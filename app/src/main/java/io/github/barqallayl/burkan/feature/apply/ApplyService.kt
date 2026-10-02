package io.github.barqallayl.burkan.feature.apply

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
import io.github.barqallayl.burkan.feature.apply.data.ApplyController
import io.github.barqallayl.burkan.feature.apply.data.ApplyRunState
import io.github.barqallayl.burkan.feature.apply.data.AutoApply
import io.github.barqallayl.burkan.feature.apply.data.RunAlerts
import io.github.barqallayl.burkan.feature.apply.model.ApplyKind
import io.github.barqallayl.burkan.feature.apply.model.RunTrigger
import io.github.barqallayl.burkan.feature.apply.ui.label
import io.github.barqallayl.burkan.feature.apply.ui.text
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

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
                start(kind, if (automatic) RunTrigger.Boot else RunTrigger.Manual)
            }
        }
        // A cancelled run is still restoring settings until it completes; it stops the service itself then.
        stopIfIdle()
        return START_NOT_STICKY
    }

    private fun start(kind: ApplyKind, trigger: RunTrigger) {
        run = scope.launch {
            // After a restart, System UI waits for the lock: restarting it now would lock a phone just unlocked.
            execute(kind, trigger, deferSystemUi = trigger == RunTrigger.Boot)
            if (autoApply.isWaitingForLock()) awaitLock()
        }.also { it.invokeOnCompletion { stopIfIdle() } }
    }

    private fun awaitLock() {
        lockWait = scope.launch {
            notifications.update(notifications.waitingForLock())
            if (!autoApply.awaitLockWhilePending()) return@launch
            // The screen is off: keep the CPU awake for the run.
            val wakeLock = getSystemService(PowerManager::class.java)
                .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG)
            wakeLock.acquire(RUN_WAKE_LOCK_TIMEOUT.inWholeMilliseconds)
            try {
                // Locking restarted adbd; it takes about a second to accept connections again.
                delay(ADBD_SETTLE)
                execute(ApplyKind.Light, RunTrigger.Boot, deferSystemUi = false)
            } finally {
                if (wakeLock.isHeld) wakeLock.release()
            }
        }.also { it.invokeOnCompletion { stopIfIdle() } }
    }

    private suspend fun execute(kind: ApplyKind, trigger: RunTrigger, deferSystemUi: Boolean) {
        val progress = scope.launch {
            controller.state.collect { state ->
                if (state is ApplyRunState.Running) notifications.update(notifications.progress(kind, state))
            }
        }
        try {
            val outcome = controller.run(kind, trigger, deferSystemUi)
            if (outcome != null) autoApply.onRunFinished(trigger, outcome)
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
        const val ACTION_CANCEL = "io.github.barqallayl.burkan.action.CANCEL_RUN"

        /** The light apply after a restart, which skips itself when Vulkan is already in place. */
        const val ACTION_AUTOMATIC = "io.github.barqallayl.burkan.action.AUTOMATIC_RUN"

        /** Waits for the next lock to restart System UI, when a run after a restart left it for then. */
        const val ACTION_AWAIT_LOCK = "io.github.barqallayl.burkan.action.AWAIT_LOCK"

        private const val WAKE_LOCK_TAG = "Burkan:apply"
        private val ADBD_SETTLE = 3.seconds
        private val RUN_WAKE_LOCK_TIMEOUT = 2.minutes
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

    fun progress(kind: ApplyKind, state: ApplyRunState): Notification {
        val title = when (kind) {
            ApplyKind.Light -> R.string.apply_running_light_title
            ApplyKind.Full -> R.string.apply_running_full_title
        }
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
    }
}

/** Starts and cancels runs. */
interface ApplyLauncher {
    fun start(kind: ApplyKind)

    /** Waits for the next lock to restart System UI, if a run after a restart left it for then. */
    fun awaitLock()

    /**
     * Starts the automatic apply. False when Android does not let the app start it from the background, which it
     * allows after a restart but not always later.
     */
    fun startAutomatic(): Boolean

    fun cancel()
}

@Inject
@ContributesBinding(AppScope::class)
class ServiceApplyLauncher(private val application: Application) : ApplyLauncher {

    override fun start(kind: ApplyKind) {
        application.startForegroundService(
            Intent(application, ApplyService::class.java).putExtra(ApplyService.EXTRA_KIND, kind.name),
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

    override fun cancel() {
        application.startService(Intent(application, ApplyService::class.java).setAction(ApplyService.ACTION_CANCEL))
    }
}
