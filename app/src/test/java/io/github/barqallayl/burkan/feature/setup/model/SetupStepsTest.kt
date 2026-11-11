package io.github.barqallayl.burkan.feature.setup.model

import io.github.barqallayl.burkan.core.model.ShellBackend
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SetupStepsTest {

    private val wirelessDebugging = stepsFor(ShellBackend.WirelessDebugging)
    private val shizuku = stepsFor(ShellBackend.Shizuku)

    @Test
    fun `nothing is done before the user finishes a step`() {
        assertEquals(
            SetupStep.Notifications,
            doneSteps(wirelessDebugging, stepsDone = 0, paired = false).currentStep(wirelessDebugging),
        )
    }

    @Test
    fun `the steps done are the first ones, in order`() {
        val done = doneSteps(wirelessDebugging, stepsDone = 3, paired = false)

        assertEquals(setOf(SetupStep.Notifications, SetupStep.DeveloperOptions, SetupStep.WirelessDebugging), done)
        assertEquals(SetupStep.Pair, done.currentStep(wirelessDebugging))
    }

    @Test
    fun `a pairing the phone dropped is asked for again, however far setup had got`() {
        assertEquals(
            SetupStep.Pair,
            doneSteps(wirelessDebugging, stepsDone = 6, paired = false).currentStep(wirelessDebugging),
        )
    }

    @Test
    fun `finishing every step finishes setup`() {
        assertNull(
            doneSteps(wirelessDebugging, stepsDone = wirelessDebugging.size, paired = true)
                .currentStep(wirelessDebugging),
        )
    }

    @Test
    fun `wireless debugging's steps, in the order they are done`() {
        assertEquals(
            listOf(
                SetupStep.Notifications,
                SetupStep.DeveloperOptions,
                SetupStep.WirelessDebugging,
                SetupStep.Pair,
                SetupStep.Finishing,
                SetupStep.Battery,
            ),
            wirelessDebugging,
        )
    }

    @Test
    fun `Shizuku has no Developer options, wireless debugging or pairing to go through`() {
        assertEquals(
            listOf(
                SetupStep.Notifications,
                SetupStep.Shizuku,
                SetupStep.Finishing,
                SetupStep.Battery,
            ),
            shizuku,
        )
    }

    @Test
    fun `with Shizuku a missing pairing sends setup back nowhere`() {
        val done = doneSteps(shizuku, stepsDone = 3, paired = false)

        assertEquals(setOf(SetupStep.Notifications, SetupStep.Shizuku, SetupStep.Finishing), done)
        assertEquals(SetupStep.Battery, done.currentStep(shizuku))
    }

    @Test
    fun `finishing every one of Shizuku's steps finishes setup`() {
        assertNull(doneSteps(shizuku, stepsDone = shizuku.size, paired = false).currentStep(shizuku))
    }

    @Test
    fun `the two ways to a shell start with the notifications and then part`() {
        assertEquals(1, SHARED_STEPS)
    }

    @Test
    fun `only the S23 family is the tested model`() {
        listOf("SM-S911B", "SM-S916U1", "SM-S918N").forEach { assertTrue(isTestedModel(it), it) }
        listOf("SM-S921B", "SM-A556B", "Pixel 9").forEach { assertFalse(isTestedModel(it), it) }
    }

    @Test
    fun `a count from the old steps lands on finishing setup, or keeps its place`() {
        // Version 1 for wireless debugging: Notifications, Developer options, Wireless debugging, Pair, Connect,
        // Permission, Battery.
        val old = 1
        assertEquals(4, stepsDoneNow(4, old, ShellBackend.WirelessDebugging), "paired: Finishing is next")
        assertEquals(4, stepsDoneNow(5, old, ShellBackend.WirelessDebugging), "connected: Finishing does both")
        assertEquals(5, stepsDoneNow(6, old, ShellBackend.WirelessDebugging), "permission granted: Battery is next")
        assertEquals(6, stepsDoneNow(7, old, ShellBackend.WirelessDebugging), "all done")
        assertEquals(2, stepsDoneNow(2, old, ShellBackend.WirelessDebugging))
        // Version 1 for Shizuku: Notifications, Shizuku, Connect, Permission, Battery.
        assertEquals(2, stepsDoneNow(3, old, ShellBackend.Shizuku))
        assertEquals(3, stepsDoneNow(4, old, ShellBackend.Shizuku))
        assertEquals(4, stepsDoneNow(5, old, ShellBackend.Shizuku))
    }

    @Test
    fun `a count in today's steps is left as it is`() {
        assertEquals(5, stepsDoneNow(5, SETUP_STEPS_VERSION, ShellBackend.WirelessDebugging))
    }
}
