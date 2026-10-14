package io.github.barqallayl.burkan.feature.apply

import io.github.barqallayl.burkan.feature.apply.model.isUnattended
import android.app.Application
import android.app.ForegroundServiceStartNotAllowedException
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.android.ServiceKey
import io.github.barqallayl.burkan.R
import io.github.barqallayl.burkan.core.model.AppError
import io.github.barqallayl.burkan.core.model.messageRes
import io.github.barqallayl.burkan.core.shell.PackageName
import io.github.barqallayl.burkan.feature.apply.data.ApplyController
import io.github.barqallayl.burkan.feature.apply.data.ApplyRunState
import io.github.barqallayl.burkan.feature.apply.data.AutoApply
import io.github.barqallayl.burkan.feature.apply.data.RunAlerts
import io.github.barqallayl.burkan.feature.apply.data.RunOutcome
import io.github.barqallayl.burkan.feature.apply.model.ApplyKind
import io.github.barqallayl.burkan.feature.log.ui.runningLabel
import io.github.barqallayl.burkan.feature.apply.model.RestartScope
import io.github.barqallayl.burkan.feature.apply.model.RunTrigger
import io.github.barqallayl.burkan.feature.apply.ui.label
import io.github.barqallayl.burkan.feature.apply.ui.text
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.minutes

/**
 * Keeps the app alive while a run is in progress. A full apply relaunches other apps over Burkan, and a cached
 * process can be frozen before its restore steps run. After a restart it also stays, in the foreground, until the
 * phone next locks, when it restarts System UI. An entry point only: the work is in [ApplyController] and
 * [AutoApply].
 */
@Inject
@ServiceKey
@ContributesIntoMap(AppScope::class, binding = binding<Service>())
class ApplyService(
    private val controller: ApplyController,
    private val autoApply: AutoApply,
    private val notifications: ApplyNotifications,
) : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var run: Job? = null
    private var lockWait: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        notifications.createChannel()
        val automatic = intent?.action == ACTION_AUTOMATIC
        val kind = if (automatic) {
            ApplyKind.Light
        } else {
            intent?.getStringExtra(EXTRA_KIND)?.let { name -> ApplyKind.entries.firstOrNull { it.name == name } }
        }
        val waiting = intent?.action == ACTION_AWAIT_LOCK || (run?.isActive != true && lockWait?.isActive == true)
        startInForeground(
            if (waiting) {
                notifications.waitingForLock()
            } else {
                notifications.progress(kind ?: ApplyKind.Light, controller.state.value)
            },
        )
        when {
            intent?.action == ACTION_CANCEL -> cancel()
            intent?.action == ACTION_AWAIT_LOCK -> if (run?.isActive != true && lockWait?.isActive != true) awaitLock()
            run?.isActive == true || kind == null -> Unit
            else -> {
                // A run started by hand replaces the wait for the lock; it restarts System UI itself.
                lockWait?.cancel()
                val scope = intent?.getStringExtra(EXTRA_SCOPE)
                    ?.let { name -> RestartScope.entries.firstOrNull { it.name == name } }
                    ?: RestartScope.All
                // Only the triggers a run can be started with from outside are taken from the intent.
                val unattended = intent?.getStringExtra(EXTRA_TRIGGER)
                    ?.let { name -> RunTrigger.entries.firstOrNull { it.name == name } }
                    ?.takeIf { it.isUnattended }
                val only = intent?.getStringArrayExtra(EXTRA_PACKAGES)
                    ?.mapNotNullTo(mutableSetOf(), PackageName::parse)
                start(kind, if (automatic) RunTrigger.Boot else unattended ?: RunTrigger.Manual, scope, only)
            }
        }
        // A cancelled run is still restoring settings until it completes; it stops the service itself then.
        stopIfIdle()
        return START_NOT_STICKY
    }

    private fun start(kind: ApplyKind, trigger: RunTrigger, restartScope: RestartScope, only: Set<PackageName>?) {
        run = scope.launch {
            // A run nobody is watching may start with the screen off: keep the CPU awake until it has ended.
            val wakeLock = if (trigger.isUnattended) {
                getSystemService(PowerManager::class.java).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG)
                    .also { it.acquire(UNATTENDED_WAKE_LOCK_TIMEOUT.inWholeMilliseconds) }
            } else {
                null
            }
            try {
                // After a restart, System UI waits for the lock: restarting it now would lock a phone just unlocked.
                execute(kind, trigger, restartScope, only)?.let { autoApply.onRunFinished(trigger, it) }
            } finally {
                if (wakeLock?.isHeld == true) wakeLock.release()
            }
            if (autoApply.isWaitingForLock()) awaitLock()
        }.also { it.invokeOnCompletion { stopIfIdle() } }
    }

    private fun awaitLock() {
        lockWait = scope.launch {
            notifications.update(notifications.waitingForLock())
            autoApply.restartSystemUiAtLocks {
                // The screen is off: keep the CPU awake for the run.
                val wakeLock = getSystemService(PowerManager::class.java)
                    .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG)
                wakeLock.acquire(RUN_WAKE_LOCK_TIMEOUT.inWholeMilliseconds)
                try {
                    execute(ApplyKind.Light, RunTrigger.AtLock)
                } finally {
                    if (wakeLock.isHeld) wakeLock.release()
                    // Shown again in case the phone was unlocked first and the wait starts over.
                    notifications.update(notifications.waitingForLock())
                }
            }
        }.also { it.invokeOnCompletion { stopIfIdle() } }
    }

    /** Runs the apply, with its progress in the notification. Null when another run held the app. */
    private suspend fun execute(
        kind: ApplyKind,
        trigger: RunTrigger,
        restartScope: RestartScope = RestartScope.All,
        only: Set<PackageName>? = null,
    ): RunOutcome? {
        val progress = scope.launch {
            controller.state.collect { state ->
                if (state is ApplyRunState.Running) notifications.update(notifications.progress(kind, state))
            }
        }
        try {
            return controller.run(kind, trigger, restartScope, only)
        } finally {
            withContext(NonCancellable) { progress.cancel() }
        }
    }

    private fun cancel() {
        run?.cancel()
        lockWait?.let { wait ->
            wait.cancel()
            // Nothing waits for the lock any more, so Home must not say System UI will switch then.
            scope.launch { autoApply.cancelSystemUiAtLock() }.invokeOnCompletion { stopIfIdle() }
        }
    }

    private fun stopIfIdle() {
        if (run?.isCompleted != false && lockWait?.isCompleted != false) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun startInForeground(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                ApplyNotifications.ONGOING_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } else {
            // Android 13 has no special-use type; a plain foreground service is allowed there.
            startForeground(ApplyNotifications.ONGOING_ID, notification)
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val EXTRA_KIND = "kind"
        const val EXTRA_SCOPE = "scope"

        /** The packages an open-apps run is limited to, when it restarts one app rather than all that are behind. */
        const val EXTRA_PACKAGES = "packages"

        /** [RunTrigger.External], by name, for a run started without the app's screens. */
        const val EXTRA_TRIGGER = "trigger"
        const val ACTION_CANCEL = "io.github.barqallayl.burkan.action.CANCEL_RUN"

        /** The light apply after a restart, which skips itself when Vulkan is already in place. */
        const val ACTION_AUTOMATIC = "io.github.barqallayl.burkan.action.AUTOMATIC_RUN"

        /** Waits for the next lock to restart System UI, when a run after a restart left it for then. */
        const val ACTION_AWAIT_LOCK = "io.github.barqallayl.burkan.action.AWAIT_LOCK"

        private const val WAKE_LOCK_TAG = "Burkan:apply"
        private val RUN_WAKE_LOCK_TIMEOUT = 2.minutes

        /** A full apply was measured at 73 seconds; this is the most the CPU is held awake for one. */
        private val UNATTENDED_WAKE_LOCK_TIMEOUT = 5.minutes
    }
}

/** The run's notifications: its progress while it lasts, and a failure that outlives it. */
@Inject
@ContributesBinding(AppScope::class)
class ApplyNotifications(private val application: Application) : RunAlerts {

    private val manager = application.getSystemService(NotificationManager::class.java)

    fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            application.getString(R.string.apply_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply { description = application.getString(R.string.apply_channel_description) }
        manager.createNotificationChannel(channel)
    }

    /**
     * A channel of its own for the word that the apply after a restart is done, so that someone who does not want
     * to be told can turn that off in the phone's settings and still hear of a run that failed.
     */
    private fun createAppliedChannel() {
        val channel = NotificationChannel(
            APPLIED_CHANNEL_ID,
            application.getString(R.string.applied_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply { description = application.getString(R.string.applied_channel_description) }
        manager.createNotificationChannel(channel)
    }

    override fun showApplied(behind: Int) {
        createAppliedChannel()
        val builder = NotificationCompat.Builder(application, APPLIED_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(application.getString(R.string.applied_title))
            .setContentIntent(openApp())
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
        if (behind > 0) {
            // Apps that started before the run keep OpenGL: the notice offers to restart them, as Home does.
            val text = application.resources.getQuantityString(R.plurals.applied_behind_text, behind, behind)
            val restart = PendingIntent.getForegroundService(
                application,
                0,
                Intent(application, ApplyService::class.java).putExtra(ApplyService.EXTRA_KIND, ApplyKind.OpenApps.name),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            builder.setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .addAction(0, application.getString(R.string.home_behind_action), restart)
        } else {
            builder.setContentText(application.getString(R.string.applied_text))
        }
        val notification = builder.build()
        // A run that failed earlier in this restart has been put right: its notice gives way to this one.
        manager.cancel(FAILURE_ID)
        manager.notify(APPLIED_ID, notification)
    }

    fun progress(kind: ApplyKind, state: ApplyRunState): Notification {
        val title = kind.runningLabel
        val cancel = PendingIntent.getService(
            application,
            0,
            Intent(application, ApplyService::class.java).setAction(ApplyService.ACTION_CANCEL),
            PendingIntent.FLAG_IMMUTABLE,
        )
        return base()
            .setContentTitle(application.getString(title))
            .setContentText((state as? ApplyRunState.Running)?.let { application.resources.text(it.phase.label()) })
            .setOngoing(true)
            .setProgress(0, 0, true)
            .addAction(0, application.getString(R.string.apply_cancel_action), cancel)
            .build()
    }

    fun update(notification: Notification) {
        manager.notify(ONGOING_ID, notification)
    }

    override fun showFailure(error: AppError?) {
        createChannel()
        val text = application.getString(error?.messageRes() ?: R.string.apply_failed_text)
        val notification = base()
            .setContentTitle(application.getString(R.string.apply_failed_title))
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .build()
        manager.notify(FAILURE_ID, notification)
    }

    /** Shown while the service waits for the phone to lock, with a way to stop waiting. */
    fun waitingForLock(): Notification {
        val cancel = PendingIntent.getService(
            application,
            0,
            Intent(application, ApplyService::class.java).setAction(ApplyService.ACTION_CANCEL),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val text = application.getString(R.string.apply_waiting_for_lock_text)
        return base()
            .setContentTitle(application.getString(R.string.apply_waiting_for_lock_title))
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setOngoing(true)
            .addAction(0, application.getString(R.string.apply_cancel_action), cancel)
            .build()
    }

    override fun showReadyToApply() {
        createChannel()
        val apply = PendingIntent.getForegroundService(
            application,
            0,
            Intent(application, ApplyService::class.java).setAction(ApplyService.ACTION_AUTOMATIC),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = base()
            .setContentTitle(application.getString(R.string.apply_ready_title))
            .setContentText(application.getString(R.string.apply_ready_text))
            .addAction(0, application.getString(R.string.apply_ready_action), apply)
            .setAutoCancel(true)
            .build()
        manager.notify(FAILURE_ID, notification)
    }

    private fun base(): NotificationCompat.Builder = NotificationCompat.Builder(application, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_notification)
        .setOnlyAlertOnce(true)
        .setContentIntent(openApp())

    private fun openApp(): PendingIntent? {
        val launch = application.packageManager.getLaunchIntentForPackage(application.packageName) ?: return null
        return PendingIntent.getActivity(application, 0, launch, PendingIntent.FLAG_IMMUTABLE)
    }

    companion object {
        const val CHANNEL_ID = "apply"
        const val ONGOING_ID = 10
        const val FAILURE_ID = 11
        const val APPLIED_CHANNEL_ID = "applied"
        const val APPLIED_ID = 12
    }
}

/** Starts and cancels runs. */
interface ApplyLauncher {
    /** [scope] is how many apps a full apply restarts; [only] limits an open-apps run to the packages it names. */
    fun start(kind: ApplyKind, scope: RestartScope = RestartScope.All, only: Set<PackageName>? = null)

    /** Waits for the next lock to restart System UI, if a run after a restart left it for then. */
    fun awaitLock()

    /**
     * Starts the automatic apply. False when Android does not let the app start it from the background, which it
     * allows after a restart but not always later.
     */
    fun startAutomatic(): Boolean

    /**
     * Starts a run nobody is watching: from a shortcut, or from another app. [trigger] says which. False when Android does not let the app start it from the background.
     */
    fun startUnattended(kind: ApplyKind, trigger: RunTrigger): Boolean

    fun cancel()
}

@Inject
@ContributesBinding(AppScope::class)
class ServiceApplyLauncher(private val application: Application) : ApplyLauncher {

    override fun start(kind: ApplyKind, scope: RestartScope, only: Set<PackageName>?) {
        application.startForegroundService(
            Intent(application, ApplyService::class.java)
                .putExtra(ApplyService.EXTRA_KIND, kind.name)
                .putExtra(ApplyService.EXTRA_SCOPE, scope.name)
                .apply { if (only != null) putExtra(ApplyService.EXTRA_PACKAGES, only.map { it.value }.toTypedArray()) },
        )
    }

    override fun awaitLock() {
        try {
            application.startForegroundService(
                Intent(application, ApplyService::class.java).setAction(ApplyService.ACTION_AWAIT_LOCK),
            )
        } catch (_: ForegroundServiceStartNotAllowedException) {
            // Asked from the background: the service that left System UI for the lock is still waiting for it.
        }
    }

    override fun startAutomatic(): Boolean = try {
        application.startForegroundService(
            Intent(application, ApplyService::class.java).setAction(ApplyService.ACTION_AUTOMATIC),
        )
        true
    } catch (_: ForegroundServiceStartNotAllowedException) {
        false
    }

    override fun startUnattended(kind: ApplyKind, trigger: RunTrigger): Boolean = try {
        application.startForegroundService(
            Intent(application, ApplyService::class.java)
                .putExtra(ApplyService.EXTRA_KIND, kind.name)
                .putExtra(ApplyService.EXTRA_TRIGGER, trigger.name),
        )
        true
    } catch (_: ForegroundServiceStartNotAllowedException) {
        false
    }

    override fun cancel() {
        application.startService(Intent(application, ApplyService::class.java).setAction(ApplyService.ACTION_CANCEL))
    }
}
