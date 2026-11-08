package io.github.barqallayl.burkan.feature.setup.model

import io.github.barqallayl.burkan.core.model.AppError
import io.github.barqallayl.burkan.core.model.ShellBackend

/**
 * Every step setup has. The order they are declared in is not the order the user does them in: that is [stepsFor],
 * and differs with the way to a shell.
 */
enum class SetupStep {
    Notifications,
    DeveloperOptions,
    WirelessDebugging,
    Pair,
    Connect,
    Permission,
    Battery,
    Shizuku,
}

/** The setup checklist for [backend], in the order the user works down it. */
fun stepsFor(backend: ShellBackend): List<SetupStep> = when (backend) {
    ShellBackend.WirelessDebugging -> listOf(
        SetupStep.Notifications,
        SetupStep.DeveloperOptions,
        SetupStep.WirelessDebugging,
        SetupStep.Pair,
        SetupStep.Connect,
        SetupStep.Permission,
        SetupStep.Battery,
    )

    ShellBackend.Shizuku -> listOf(
        SetupStep.Notifications,
        SetupStep.Shizuku,
        SetupStep.Connect,
        SetupStep.Permission,
        SetupStep.Battery,
    )
}

/**
 * How many steps both ways to a shell start with. Changing from one to the other keeps these done and goes through
 * the rest again.
 */
val SHARED_STEPS: Int = stepsFor(ShellBackend.WirelessDebugging)
    .zip(stepsFor(ShellBackend.Shizuku))
    .takeWhile { (wirelessDebugging, shizuku) -> wirelessDebugging == shizuku }
    .size

/**
 * The steps of [steps] that are done: the first [stepsDone] of them, each finished by the user in its turn. A step is
 * looked at only once the user is on it, so nothing further down is done ahead of time. Pairing is the exception that
 * can be undone: a key the phone stops accepting sends setup back to it. Without pairing in [steps], there is none.
 */
fun doneSteps(steps: List<SetupStep>, stepsDone: Int, paired: Boolean): Set<SetupStep> {
    val pairAt = steps.indexOf(SetupStep.Pair)
    val reached = if (paired || pairAt < 0) stepsDone else minOf(stepsDone, pairAt)
    return steps.take(reached).toSet()
}

/** The first of [steps] not done, which is the one the user acts on; null once setup is complete. */
fun Set<SetupStep>.currentStep(steps: List<SetupStep>): SetupStep? = steps.firstOrNull { it !in this }

/** The models setup is tested on: the Galaxy S23, S23+ and S23 Ultra. */
fun isTestedModel(model: String): Boolean = TESTED_MODELS.any { model.startsWith(it) }

private val TESTED_MODELS = listOf("SM-S911", "SM-S916", "SM-S918")

sealed interface SetupError : AppError {
    /** `pm grant` ran but did not grant the permission. */
    data object GrantFailed : SetupError
}
