package io.github.barqallayl.burkan.core.storage

import io.github.barqallayl.burkan.feature.setup.model.SETUP_STEPS_VERSION
import kotlinx.coroutines.flow.MutableStateFlow

class FakeDeviceStateStorage(
    paired: Boolean = false,
    setupStepsDone: Int = 0,
    setupComplete: Boolean = false,
    welcomeSeen: Boolean = false,
    setupStepsVersion: Int = SETUP_STEPS_VERSION,
) : DeviceStateStorage {
    override val isPaired = MutableStateFlow(paired)
    override val setupStepsDone = MutableStateFlow(setupStepsDone)
    override val setupStepsVersion = MutableStateFlow(setupStepsVersion)
    override val isSetupComplete = MutableStateFlow(setupComplete)
    override val isWelcomeSeen = MutableStateFlow(welcomeSeen)
    override val isFirstApplyPending = MutableStateFlow(false)
    override val switchedOnWirelessDebugging = MutableStateFlow(false)

    override suspend fun setPaired(paired: Boolean) {
        isPaired.value = paired
    }

    override suspend fun setSetupStepsDone(count: Int) {
        this.setupStepsDone.value = count
    }

    override suspend fun setSetupStepsDone(count: Int, version: Int) {
        setupStepsDone.value = count
        setupStepsVersion.value = version
    }

    override suspend fun setSetupComplete(complete: Boolean) {
        isSetupComplete.value = complete
    }

    override suspend fun setWelcomeSeen(seen: Boolean) {
        isWelcomeSeen.value = seen
    }

    override suspend fun setFirstApplyPending(pending: Boolean) {
        isFirstApplyPending.value = pending
    }

    override suspend fun setSwitchedOnWirelessDebugging(switchedOn: Boolean) {
        switchedOnWirelessDebugging.value = switchedOn
    }
}
