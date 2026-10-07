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

/**
 * The steps that are done: the first [stepsDone] of them, each finished by the user in its turn. A step is looked
 * at only once the user is on it, so nothing further down is done ahead of time. Pairing is the exception that can
 * be undone: a key the phone stops accepting sends setup back to it.
 */
fun doneSteps(stepsDone: Int, paired: Boolean): Set<SetupStep> {
    val reached = if (paired) stepsDone else minOf(stepsDone, SetupStep.Pair.ordinal)
    return SetupStep.entries.take(reached).toSet()
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
