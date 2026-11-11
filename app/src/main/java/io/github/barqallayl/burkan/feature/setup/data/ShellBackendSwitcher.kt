package io.github.barqallayl.burkan.feature.setup.data

import dev.zacsweers.metro.Inject
import io.github.barqallayl.burkan.core.model.ShellBackend
import io.github.barqallayl.burkan.core.storage.DeviceStateStorage
import io.github.barqallayl.burkan.core.storage.SettingsStorage
import io.github.barqallayl.burkan.feature.setup.model.SHARED_STEPS
import kotlinx.coroutines.flow.first

/**
 * Changes the way to a shell: Shizuku if it was wireless debugging, and wireless debugging if it was Shizuku. Each
 * has steps of its own, so setup keeps the ones the two start with and goes through the rest again, and is no longer
 * complete: `App.kt` shows Setup. The pairing is kept, for the way back.
 *
 * Settings and Setup's own screen both offer it.
 */
@Inject
class ShellBackendSwitcher(
    private val settings: SettingsStorage,
    private val deviceState: DeviceStateStorage,
) {
    suspend fun switch() {
        val target = when (settings.shellBackend.first()) {
            ShellBackend.Shizuku -> ShellBackend.WirelessDebugging
            ShellBackend.WirelessDebugging -> ShellBackend.Shizuku
        }
        choose(target)
    }

    /** Makes [target] the connection, as [switch] does; choosing the one already in use changes nothing. */
    suspend fun choose(target: ShellBackend) {
        if (settings.shellBackend.first() == target) return
        settings.setShellBackend(target)
        deviceState.setSetupStepsDone(minOf(deviceState.setupStepsDone.first(), SHARED_STEPS))
        deviceState.setSetupComplete(false)
    }
}
