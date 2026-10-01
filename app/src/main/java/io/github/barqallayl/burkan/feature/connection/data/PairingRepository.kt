package io.github.barqallayl.burkan.feature.connection.data

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.github.barqallayl.burkan.core.storage.DeviceStateStorage
import io.github.barqallayl.burkan.feature.connection.model.PairingError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/** Where a pairing session stands, for the notification and the Setup screen. */
sealed interface PairingStatus {
    data object Idle : PairingStatus
    data object WaitingForCode : PairingStatus
    data object Pairing : PairingStatus
    data object Paired : PairingStatus
    data class Failed(val error: PairingError) : PairingStatus
}

/**
 * Pairs the app's key with the phone's wireless debugging. A session lasts while the pairing notification is up;
 * the user can type the code again after a failure.
 */
@Inject
@SingleIn(AppScope::class)
class PairingRepository(
    private val client: AdbClient,
    private val discovery: AdbDiscovery,
    private val deviceState: DeviceStateStorage,
) {

    private val mutableStatus = MutableStateFlow<PairingStatus>(PairingStatus.Idle)
    val status: StateFlow<PairingStatus> = mutableStatus.asStateFlow()

    /** Waits for a successful pairing, giving up after [SESSION_TIMEOUT]. Returns once the session is over. */
    suspend fun runSession() {
        mutableStatus.value = PairingStatus.WaitingForCode
        val paired = withTimeoutOrNull(SESSION_TIMEOUT) { status.first { it == PairingStatus.Paired } }
        if (paired == null) mutableStatus.value = PairingStatus.Failed(PairingError.TimedOut)
    }

    /** Pairs with [code] as typed: spaces are ignored, anything else must be six digits. */
    suspend fun pair(code: String): Either<PairingError, Unit> {
        val result = either {
            val digits = code.filterNot { it.isWhitespace() }
            ensure(CODE.matches(digits)) { PairingError.InvalidCode }
            mutableStatus.value = PairingStatus.Pairing
            val endpoint = discovery.find(AdbService.Pairing, DISCOVERY_TIMEOUT) ?: raise(PairingError.DialogClosed)
            withTimeoutOrNull(EXCHANGE_TIMEOUT) { exchange(endpoint, digits) }?.bind() ?: raise(PairingError.TimedOut)
            deviceState.setPaired(true)
        }
        mutableStatus.value = result.fold({ PairingStatus.Failed(it) }, { PairingStatus.Paired })
        return result
    }

    private suspend fun exchange(endpoint: AdbEndpoint, code: String): Either<PairingError, Unit> {
        val loopback = client.pair(AdbShellAccess.LOOPBACK, endpoint.port, code)
        return if (loopback == Either.Left(PairingError.DialogClosed)) {
            client.pair(endpoint.host, endpoint.port, code)
        } else {
            loopback
        }
    }

    companion object {
        private val CODE = Regex("^[0-9]{6}$")

        /** How long the pairing notification waits for a code. */
        val SESSION_TIMEOUT = 5.minutes

        /** The pairing service is advertised only while the dialog is open, so it is there at once or not at all. */
        val DISCOVERY_TIMEOUT = 10.seconds
        val EXCHANGE_TIMEOUT = 30.seconds
    }
}
