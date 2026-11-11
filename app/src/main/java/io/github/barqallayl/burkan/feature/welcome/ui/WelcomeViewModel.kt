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

/**
 * [isOnWifi] is true until checked, so a phone on Wi-Fi never sees the line about it flash up. [hasShizuku] is whether
 * Shizuku's app is on the phone, which is then offered as a way that needs no Wi-Fi.
 */
@Immutable
data class WelcomeState(val isOnWifi: Boolean = true, val hasShizuku: Boolean = false) {
    /**
     * The line saying the phone is not on Wi-Fi. Only for a phone without Shizuku's app, which can only use wireless
     * debugging: to anyone else it would not be true, and the choice of connection says what each way needs.
     */
    val showsNoWifi: Boolean get() = !isOnWifi && !hasShizuku
}

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

    // Wi-Fi is followed for as long as the screen is there: the user may connect from the notification shade, which
    // does not take the app out of the foreground.
    override val container = orbitContainer<WelcomeState, WelcomeSideEffect>(WelcomeState()) {
        val hasShizuku = shizuku.appPackage() != null
        reduce { state.copy(hasShizuku = hasShizuku) }
        wirelessDebugging.wifiConnected.collect { onWifi -> reduce { state.copy(isOnWifi = onWifi) } }
    }

    /**
     * Get started. The choice of connection is offered only to a phone that has Shizuku's app: anyone else has nothing
     * to choose between, and leaves Welcome for good here. A phone with Shizuku leaves it once the choice is made, so
     * backing out of the choice and closing the app opens on Welcome again, with the choice still ahead.
     */
    fun getStarted() = intent {
        if (shizuku.appPackage() != null) {
            postSideEffect(WelcomeSideEffect.ChooseConnection)
        } else {
            deviceState.setWelcomeSeen(true)
            postSideEffect(WelcomeSideEffect.StartSetup)
        }
    }
}
