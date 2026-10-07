package io.github.barqallayl.burkan.feature.setup.model

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SetupStepsTest {

    @Test
    fun `nothing is done before the user finishes a step`() {
        assertEquals(SetupStep.Notifications, doneSteps(stepsDone = 0, paired = false).currentStep())
    }

    @Test
    fun `the steps done are the first ones, in order`() {
        val done = doneSteps(stepsDone = 3, paired = false)

        assertEquals(setOf(SetupStep.Notifications, SetupStep.DeveloperOptions, SetupStep.WirelessDebugging), done)
        assertEquals(SetupStep.Pair, done.currentStep())
    }

    @Test
    fun `a pairing the phone dropped is asked for again, however far setup had got`() {
        assertEquals(SetupStep.Pair, doneSteps(stepsDone = 6, paired = false).currentStep())
    }

    @Test
    fun `finishing every step finishes setup`() {
        assertNull(doneSteps(stepsDone = SetupStep.entries.size, paired = true).currentStep())
    }

    @Test
    fun `only the S23 family is the tested model`() {
        listOf("SM-S911B", "SM-S916U1", "SM-S918N").forEach { assertTrue(isTestedModel(it), it) }
        listOf("SM-S921B", "SM-A556B", "Pixel 9").forEach { assertFalse(isTestedModel(it), it) }
    }
}
