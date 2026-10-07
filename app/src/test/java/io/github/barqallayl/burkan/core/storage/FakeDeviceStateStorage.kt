package io.github.barqallayl.burkan.core.storage

import kotlinx.coroutines.flow.MutableStateFlow

class FakeDeviceStateStorage(
    paired: Boolean = false,
    setupStepsDone: Int = 0,
    setupComplete: Boolean = false,
) : DeviceStateStorage {
    override val isPaired = MutableStateFlow(paired)
    override val setupStepsDone = MutableStateFlow(setupStepsDone)
    override val isSetupComplete = MutableStateFlow(setupComplete)

    override suspend fun setPaired(paired: Boolean) {
        isPaired.value = paired
    }

    override suspend fun setSetupStepsDone(count: Int) {
        this.setupStepsDone.value = count
    }

    override suspend fun setSetupComplete(complete: Boolean) {
        isSetupComplete.value = complete
    }
}
