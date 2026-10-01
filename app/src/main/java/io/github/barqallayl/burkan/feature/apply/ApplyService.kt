package io.github.barqallayl.burkan.feature.apply

import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.android.ServiceKey
import io.github.barqallayl.burkan.R
import io.github.barqallayl.burkan.feature.apply.data.ApplyController
import io.github.barqallayl.burkan.feature.apply.data.ApplyRunState
import io.github.barqallayl.burkan.feature.apply.model.ApplyKind
import io.github.barqallayl.burkan.feature.apply.model.RunTrigger
import io.github.barqallayl.burkan.feature.apply.ui.label
import io.github.barqallayl.burkan.feature.apply.ui.text
import io.github.barqallayl.burkan.feature.log.model.RunResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Keeps the app alive while a run is in progress. A full apply relaunches other apps over Burkan, and a cached
 * process can be frozen before its restore steps run. An entry point only: the work is in [ApplyController].
 */
@Inject
@ServiceKey
@ContributesIntoMap(AppScope::class, binding = binding<Service>())
class ApplyService(
    private val controller: ApplyController,
    private val notifications: ApplyNotifications,
) : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var run: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        notifications.createChannel()
        val kind = intent?.getStringExtra(EXTRA_KIND)?.let { name -> ApplyKind.entries.firstOrNull { it.name == name } }
        startInForeground(notifications.progress(kind ?: ApplyKind.Light, controller.state.value))
        when {
            intent?.action == ACTION_CANCEL -> run?.cancel()
            run?.isActive == true || kind == null -> Unit
            else -> run = scope.launch { execute(kind) }
        }
        // A cancelled run is still restoring settings until it completes; it stops the service itself then.
        if (run?.isCompleted != false) stopSelf()
        return START_NOT_STICKY
    }

    private suspend fun execute(kind: ApplyKind) {
        val progress = scope.launch {
            controller.state.collect { state ->
                if (state is ApplyRunState.Running) notifications.update(notifications.progress(kind, state))
            }
        }
        try {
            if (controller.run(kind, RunTrigger.Manual) == RunResult.Failed) notifications.showFailure()
        } finally {
            withContext(NonCancellable) {
                progress.cancel()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
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
    }
}

/** The run's notifications: its progress while it lasts, and a failure that outlives it. */
@Inject
class ApplyNotifications(private val application: Application) {

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

    fun showFailure() {
        val notification = base()
            .setContentTitle(application.getString(R.string.apply_failed_title))
            .setContentText(application.getString(R.string.apply_failed_text))
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

    override fun cancel() {
        application.startService(Intent(application, ApplyService::class.java).setAction(ApplyService.ACTION_CANCEL))
    }
}
