package io.github.barqallayl.burkan.core.storage

import kotlinx.coroutines.flow.MutableStateFlow

class FakeDeviceStateStorage(
    paired: Boolean = false,
    batteryStepSkipped: Boolean = false,
    setupComplete: Boolean = false,
) : DeviceStateStorage {
    override val isPaired = MutableStateFlow(paired)
    override val isBatteryStepSkipped = MutableStateFlow(batteryStepSkipped)
    override val isSetupComplete = MutableStateFlow(setupComplete)

    override suspend fun setPaired(paired: Boolean) {
        isPaired.value = paired
    }

    override suspend fun setBatteryStepSkipped(skipped: Boolean) {
        isBatteryStepSkipped.value = skipped
    }

    override suspend fun setSetupComplete(complete: Boolean) {
        isSetupComplete.value = complete
    }
}
