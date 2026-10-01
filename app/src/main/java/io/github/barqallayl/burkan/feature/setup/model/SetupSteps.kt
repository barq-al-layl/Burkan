package io.github.barqallayl.burkan.feature.setup.model

import io.github.barqallayl.burkan.core.model.AppError

/** The setup checklist, in the order the user works down it. */
enum class SetupStep {
    Notifications,
    DeveloperOptions,
    WirelessDebugging,
    Pair,
    Connect,
    Permission,
    Battery,
}

/** What the app can detect about the phone. Nothing here is self-declared by the user. */
data class SetupFacts(
    val notificationsAllowed: Boolean,
    val developerOptionsEnabled: Boolean,
    val wirelessDebuggingOn: Boolean,
    val paired: Boolean,
    /** A shell command came back in this session. */
    val connected: Boolean,
    val permissionHeld: Boolean,
    val batteryExempt: Boolean,
    val batteryStepSkipped: Boolean,
)

/**
 * The steps that are done. Holding the permission implies the connection worked and lets the app switch wireless
 * debugging itself, so those steps count as done too; pairing is tracked on its own, because a key the phone stops
 * accepting sends setup back to it.
 */
fun SetupFacts.doneSteps(): Set<SetupStep> = buildSet {
    if (notificationsAllowed) add(SetupStep.Notifications)
    if (developerOptionsEnabled) add(SetupStep.DeveloperOptions)
    if (wirelessDebuggingOn || permissionHeld) add(SetupStep.WirelessDebugging)
    if (paired) add(SetupStep.Pair)
    if (connected || permissionHeld) add(SetupStep.Connect)
    if (permissionHeld) add(SetupStep.Permission)
    if (batteryExempt || batteryStepSkipped) add(SetupStep.Battery)
}

/** The first step not done, which is the one the user acts on; null once setup is complete. */
fun Set<SetupStep>.currentStep(): SetupStep? = SetupStep.entries.firstOrNull { it !in this }

/** The models setup is tested on: the Galaxy S23, S23+ and S23 Ultra. */
fun isTestedModel(model: String): Boolean = TESTED_MODELS.any { model.startsWith(it) }

private val TESTED_MODELS = listOf("SM-S911", "SM-S916", "SM-S918")

sealed interface SetupError : AppError {
    /** `pm grant` ran but did not grant the permission. */
    data object GrantFailed : SetupError
}
