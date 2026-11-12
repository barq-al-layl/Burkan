package io.github.barqallayl.burkan

import org.junit.Test
import kotlin.test.assertContains
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/** Fails if the power-assert plugin stops rewriting the test sources, and a failed assertion goes back to saying nothing. */
class PowerAssertTest {

    @Test
    fun `a failed assertion shows the values in its expression`() {
        val steps = 6
        val done = 4

        val failure = assertFailsWith<AssertionError> { assertTrue(done == steps) }

        val message = failure.message.orEmpty()
        assertContains(message, "done == steps")
        assertContains(message, "4")
        assertContains(message, "6")
    }
}
