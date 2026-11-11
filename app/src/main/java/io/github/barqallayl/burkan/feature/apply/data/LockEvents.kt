package io.github.barqallayl.burkan.feature.apply.data

import android.app.Application
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.PowerManager
import android.provider.Settings
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
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

/** Tells when the phone locks. */
interface LockEvents {
    /**
     * Emits each time the phone is locked with its screen off, and at once if that is already so when collected. A
     * phone with no secure lock counts as locked once its screen is off.
     */
    val locks: Flow<Unit>

    /**
     * True while the keyguard is up: false once the user has unlocked. A phone with no secure lock is locked while its
     * screen is off.
     */
    fun isLocked(): Boolean
}

/**
 * Watches the screen going off, then asks the keyguard whether it is up: a phone can wait a while after the screen
 * goes off before it locks. A collector must keep the process alive; `ApplyService` does, in the foreground.
 *
 * A phone whose screen lock is "None" never shows the keyguard, so for it the screen going off is the lock. Waiting
 * for the keyguard there would leave System UI for a lock that never comes. "Swipe" shows the keyguard and is no
 * more secure, so it is taken the same way.
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
                if (intent.action != Intent.ACTION_SCREEN_OFF) return
                watching = launch { if (keyguard.isDeviceSecure) watchForLock() else handOver() }
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
        if (!power.isInteractive && isLocked()) handOver()
        awaitClose { application.unregisterReceiver(receiver) }
    }

    override fun isLocked(): Boolean =
        keyguard.isKeyguardLocked || (!keyguard.isDeviceSecure && !power.isInteractive)

    /**
     * With the screen off the CPU may sleep, so a wake lock covers the wait for the keyguard. Asked every
     * [LOCK_POLL], a cheap call, so that the lock is seen at once: the user may unlock again within seconds. The
     * wait is as long as the phone is set to take before it locks ([lockWait]). It is collected only while System
     * UI is left for a lock, so the wake lock is held for one screen going off, not for every one.
     */
    private suspend fun ProducerScope<Unit>.watchForLock() {
        val wait = lockWait(lockDelay())
        val wakeLock = power.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG)
        wakeLock.acquire((wait + HAND_OVER).inWholeMilliseconds)
        try {
            val waited = TimeSource.Monotonic.markNow()
            while (waited.elapsedNow() <= wait) {
                if (keyguard.isKeyguardLocked) {
                    send(Unit)
                    // Held a moment longer: whoever hears of the lock takes a wake lock of its own, and the CPU
                    // must not fall asleep in between.
                    delay(HAND_OVER)
                    return
                }
                delay(LOCK_POLL)
            }
        } finally {
            if (wakeLock.isHeld) wakeLock.release()
        }
    }

    /** Says the phone has locked, keeping the CPU awake until whoever hears it has a wake lock of its own. */
    private suspend fun ProducerScope<Unit>.handOver() {
        val wakeLock = power.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG)
        wakeLock.acquire(HAND_OVER.inWholeMilliseconds)
        try {
            send(Unit)
            delay(HAND_OVER)
        } finally {
            if (wakeLock.isHeld) wakeLock.release()
        }
    }

    /**
     * How long after the screen goes off the phone is set to lock, or null when Android will not say. The key is
     * hidden from the SDK, and whether an app may read it on One UI has not been tried on a phone.
     */
    private fun lockDelay(): Duration? = try {
        Settings.Secure.getLong(application.contentResolver, LOCK_AFTER_TIMEOUT, UNKNOWN).takeIf { it >= 0 }?.milliseconds
    } catch (_: SecurityException) {
        null
    }

    private companion object {
        val LOCK_POLL = 200.milliseconds
        val HAND_OVER = 2.seconds
        const val WAKE_LOCK_TAG = "Burkan:lock"

        /** `Settings.Secure.LOCK_SCREEN_LOCK_AFTER_TIMEOUT`, in milliseconds. */
        const val LOCK_AFTER_TIMEOUT = "lock_screen_lock_after_timeout"
        const val UNKNOWN = -1L
    }
}

/**
 * How long to watch for the keyguard after the screen has gone off, on a phone set to lock [delay] after that; null
 * is a phone that will not say. A little longer than the delay, as the lock is not to the millisecond, and never
 * past [LONGEST_LOCK_WAIT]: a phone set to lock half an hour later is not kept awake for it, and is caught when its
 * screen next goes off with the keyguard up.
 */
fun lockWait(delay: Duration?): Duration =
    ((delay ?: UNKNOWN_LOCK_DELAY) + LOCK_MARGIN).coerceIn(SHORTEST_LOCK_WAIT, LONGEST_LOCK_WAIT)

val SHORTEST_LOCK_WAIT = 15.seconds
val LONGEST_LOCK_WAIT = 5.minutes + 5.seconds
private val LOCK_MARGIN = 5.seconds

/** Taken for a phone that will not say: long enough for a lock set a minute after the screen goes off. */
private val UNKNOWN_LOCK_DELAY = 1.minutes
