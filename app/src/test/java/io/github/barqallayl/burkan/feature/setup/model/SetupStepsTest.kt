package io.github.barqallayl.burkan.feature.setup.model

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SetupStepsTest {

    private val nothing = SetupFacts(
        notificationsAllowed = false,
        developerOptionsEnabled = false,
        wirelessDebuggingOn = false,
        paired = false,
        connected = false,
        permissionHeld = false,
        batteryExempt = false,
        batteryStepSkipped = false,
    )

    @Test
    fun `a fresh phone starts at the first step`() {
        assertEquals(SetupStep.Notifications, nothing.doneSteps().currentStep())
    }

    @Test
    fun `the first step not done is current, even with later steps done`() {
        val facts = nothing.copy(notificationsAllowed = true, wirelessDebuggingOn = true, batteryExempt = true)

        assertEquals(SetupStep.DeveloperOptions, facts.doneSteps().currentStep())
    }

    @Test
    fun `holding the permission covers wireless debugging and the connection`() {
        val done = nothing.copy(permissionHeld = true).doneSteps()

        assertTrue(SetupStep.WirelessDebugging in done)
        assertTrue(SetupStep.Connect in done)
        assertTrue(SetupStep.Permission in done)
    }

    @Test
    fun `a pairing the phone dropped is asked for again, permission or not`() {
        val facts = nothing.copy(
            notificationsAllowed = true,
            developerOptionsEnabled = true,
            permissionHeld = true,
            batteryExempt = true,
        )

        assertEquals(SetupStep.Pair, facts.doneSteps().currentStep())
    }

    @Test
    fun `skipping the battery step finishes setup`() {
        val facts = nothing.copy(
            notificationsAllowed = true,
            developerOptionsEnabled = true,
            paired = true,
            permissionHeld = true,
            batteryStepSkipped = true,
        )

        assertNull(facts.doneSteps().currentStep())
    }

    @Test
    fun `only the S23 family is the tested model`() {
        listOf("SM-S911B", "SM-S916U1", "SM-S918N").forEach { assertTrue(isTestedModel(it), it) }
        listOf("SM-S921B", "SM-A556B", "Pixel 9").forEach { assertFalse(isTestedModel(it), it) }
    }
}
