package io.github.barqallayl.burkan.feature.connection

import android.app.Application
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.android.ServiceKey
import io.github.barqallayl.burkan.feature.connection.data.PairingRepository
import io.github.barqallayl.burkan.feature.connection.data.PairingStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

/**
 * Keeps the pairing notification up while a session runs. An entry point only: the work is in [PairingRepository].
 */
@Inject
@ServiceKey
@ContributesIntoMap(AppScope::class, binding = binding<Service>())
class PairingService(
    private val pairing: PairingRepository,
    private val notifications: PairingNotifications,
) : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var session: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        notifications.createChannel()
        val starting = session?.isActive != true
        val status = if (starting) PairingStatus.WaitingForCode else pairing.status.value
        val notification = notifications.forStatus(status)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                PairingNotifications.ONGOING_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } else {
            // Android 13 has no special-use type; a plain foreground service is allowed there.
            startForeground(PairingNotifications.ONGOING_ID, notification)
        }
        if (starting) startSession()
        val code = intent?.takeIf { it.action == ACTION_REPLY }?.let(PairingNotifications::codeFrom)
        // The outcome reaches the notification and the Setup screen through the status, not the return value.
        if (code != null) scope.launch { pairing.pair(code) }
        return START_NOT_STICKY
    }

    private fun startSession() {
        notifications.cancelOutcome()
        session = scope.launch {
            val updates = launch { pairing.status.drop(1).collect(notifications::update) }
            pairing.runSession()
            updates.cancel()
            stopForeground(STOP_FOREGROUND_REMOVE)
            notifications.showOutcome(pairing.status.value)
            stopSelf()
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val ACTION_REPLY = "io.github.barqallayl.burkan.action.PAIRING_REPLY"
    }
}

/** Starts a pairing session. */
interface PairingLauncher {
    fun start()
}

@Inject
@ContributesBinding(AppScope::class)
class ServicePairingLauncher(private val application: Application) : PairingLauncher {
    override fun start() {
        application.startForegroundService(Intent(application, PairingService::class.java))
    }
}
