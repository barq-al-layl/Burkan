package io.github.barqallayl.burkan.feature.apply.data

import android.app.Application
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.PowerManager
import androidx.core.content.ContextCompat
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

/** Tells when the phone locks. */
interface LockEvents {
    /** Emits each time the screen is off and the keyguard is up, and at once if that is already so when collected. */
    val locks: Flow<Unit>
}

/**
 * Watches the screen going off, then asks the keyguard whether it is up: a phone can wait a while after the screen
 * goes off before it locks. A collector must keep the process alive; `ApplyService` does, in the foreground.
 */
@Inject
@ContributesBinding(AppScope::class)
class KeyguardLockEvents(private val application: Application) : LockEvents {

    private val keyguard: KeyguardManager get() = application.getSystemService(KeyguardManager::class.java)
    private val power: PowerManager get() = application.getSystemService(PowerManager::class.java)

    override val locks: Flow<Unit> = callbackFlow {
        var watching: Job? = null
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                watching?.cancel()
                if (intent.action == Intent.ACTION_SCREEN_OFF) watching = launch { watchForLock() }
            }
        }
        // Screen on and off are protected broadcasts: only the system sends them, so the receiver need not be exported.
        ContextCompat.registerReceiver(
            application,
            receiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_SCREEN_ON)
            },
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        if (!power.isInteractive && keyguard.isKeyguardLocked) send(Unit)
        awaitClose { application.unregisterReceiver(receiver) }
    }

    /**
     * With the screen off the CPU may sleep, so a short wake lock covers the wait for the keyguard. A phone set to lock
     * later than [LOCK_WAIT] after the screen goes off is caught the next time the screen goes off.
     */
    private suspend fun ProducerScope<Unit>.watchForLock() {
        val wakeLock = power.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG)
        wakeLock.acquire(LOCK_WAIT.inWholeMilliseconds)
        try {
            repeat(LOCK_WAIT.inWholeSeconds.toInt()) {
                if (keyguard.isKeyguardLocked) {
                    send(Unit)
                    return
                }
                delay(1.seconds)
            }
        } finally {
            if (wakeLock.isHeld) wakeLock.release()
        }
    }

    private companion object {
        val LOCK_WAIT = 15.seconds
        const val WAKE_LOCK_TAG = "Burkan:lock"
    }
}
