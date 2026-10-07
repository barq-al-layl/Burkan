package io.github.barqallayl.burkan.core.store

import androidx.compose.runtime.Immutable
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.Named
import dev.zacsweers.metro.SingleIn
import io.github.barqallayl.burkan.Appearance
import io.github.barqallayl.burkan.appearance
import io.github.barqallayl.burkan.core.di.AppBindings
import io.github.barqallayl.burkan.core.shell.PackageName
import io.github.barqallayl.burkan.core.storage.DeviceStateStorage
import io.github.barqallayl.burkan.core.storage.SettingsStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** Every setting, as one value. */
@Immutable
data class UserSettings(
    val applyOnBoot: Boolean,
    val turnOffWirelessDebugging: Boolean,
    val exclusions: Set<PackageName>,
    val appearance: Appearance,
)

/** What the phone's own state file holds. */
@Immutable
data class DeviceState(val isPaired: Boolean, val setupStepsDone: Int, val isSetupComplete: Boolean)

/**
 * The settings, read when the process starts and kept current for as long as it lives. A screen takes what is here
 * as its first state, so it opens with its content rather than a loading indicator.
 *
 * Both values are null only until the first read. Writing still goes through [SettingsStorage].
 */
@Inject
@SingleIn(AppScope::class)
class SettingsStore(
    settings: SettingsStorage,
    deviceState: DeviceStateStorage,
    @Named(AppBindings.APP_SCOPE) scope: CoroutineScope,
) {
    val settings: StateFlow<UserSettings?> = combine(
        settings.applyOnBoot,
        settings.turnOffWirelessDebugging,
        settings.userExclusions,
        settings.appearance(),
        ::UserSettings,
    ).stateIn(scope, SharingStarted.Eagerly, null)

    val device: StateFlow<DeviceState?> = combine(
        deviceState.isPaired,
        deviceState.setupStepsDone,
        deviceState.isSetupComplete,
        ::DeviceState,
    ).stateIn(scope, SharingStarted.Eagerly, null)
}
