package io.github.barqallayl.burkan.feature.welcome.ui

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import io.github.barqallayl.burkan.core.storage.DeviceStateStorage
import io.github.barqallayl.burkan.feature.connection.data.ShizukuClient
import io.github.barqallayl.burkan.feature.connection.data.WirelessDebugging
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer

/** [isOnWifi] is true until checked, so a phone on Wi-Fi never sees the line about it flash up. */
@Immutable
data class WelcomeState(val isOnWifi: Boolean = true)

sealed interface WelcomeSideEffect {
    /** On to the choice of connection, offered because Shizuku's app is on the phone. */
    data object ChooseConnection : WelcomeSideEffect

    /** Straight on to Setup, with wireless debugging, the default. */
    data object StartSetup : WelcomeSideEffect
}

@Inject
@ViewModelKey
@ContributesIntoMap(AppScope::class, binding = binding<ViewModel>())
class WelcomeViewModel(
    private val deviceState: DeviceStateStorage,
    private val wirelessDebugging: WirelessDebugging,
    private val shizuku: ShizukuClient,
) : OrbitContainerHost<WelcomeState, WelcomeState, WelcomeSideEffect>, ViewModel() {

    override val container = orbitContainer<WelcomeState, WelcomeSideEffect>(WelcomeState())

    /** Checked on every return to the screen: the user may have gone to connect to Wi-Fi. */
    fun refresh() = intent {
        reduce { state.copy(isOnWifi = wirelessDebugging.isWifiConnected()) }
    }

    /**
     * Get started. Welcome is not shown again, and the choice of connection is offered only to a phone that has
     * Shizuku's app: anyone else has nothing to choose between.
     */
    fun getStarted() = intent {
        deviceState.setWelcomeSeen(true)
        postSideEffect(
            if (shizuku.appPackage() != null) WelcomeSideEffect.ChooseConnection else WelcomeSideEffect.StartSetup,
        )
    }
}
