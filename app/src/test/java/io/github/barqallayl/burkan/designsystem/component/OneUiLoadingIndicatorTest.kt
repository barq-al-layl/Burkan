package io.github.barqallayl.burkan.designsystem.component

import org.junit.Test
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OneUiLoadingIndicatorTest {

    @Test
    fun `the dots start and end a cycle spread out, so the loop has no seam`() {
        assertEquals(DotsPose(REST_ORBIT, REST_DOT), dotsPoseAt(0f))
        assertEquals(DotsPose(REST_ORBIT, REST_DOT), dotsPoseAt(CYCLE_MILLIS - 1f))
        assertEquals(dotsPoseAt(300f), dotsPoseAt(CYCLE_MILLIS + 300f))
    }

    @Test
    fun `no step jumps, at the edges of the phases or anywhere else`() {
        var previous = dotsPoseAt(0f)
        var millis = 1f
        while (millis <= CYCLE_MILLIS) {
            val pose = dotsPoseAt(millis)
            assertTrue(abs(pose.orbit - previous.orbit) < 0.01f, "orbit jumped at $millis ms: $previous to $pose")
            assertTrue(abs(pose.dot - previous.dot) < 0.01f, "dot jumped at $millis ms: $previous to $pose")
            previous = pose
            millis += 1f
        }
    }

    @Test
    fun `the dots merge at 900 ms, twice their size and almost on one spot`() {
        assertEquals(DotsPose(MERGED_ORBIT, MERGED_DOT), dotsPoseAt(900f))
    }

    @Test
    fun `the poses follow the recording to within a few percent`() {
        // Measured from One UI's indicator, as shares of its outer radius (88 px in the recording).
        val measured = mapOf(
            233f to DotsPose(52f / 88, 22f / 88),
            400f to DotsPose(34f / 88, 22f / 88),
            700f to DotsPose(8f / 88, 31f / 88),
            1033f to DotsPose(40f / 88, 30f / 88),
            1133f to DotsPose(54f / 88, 26f / 88),
            1600f to DotsPose(66f / 88, 22f / 88),
        )
        for ((millis, expected) in measured) {
            val pose = dotsPoseAt(millis)
            assertTrue(abs(pose.orbit - expected.orbit) < 0.06f, "orbit at $millis ms: $pose, measured $expected")
            assertTrue(abs(pose.dot - expected.dot) < 0.06f, "dot at $millis ms: $pose, measured $expected")
        }
    }

    @Test
    fun `merged, the dots stay inside the indicator`() {
        var millis = 0f
        while (millis < CYCLE_MILLIS) {
            val pose = dotsPoseAt(millis)
            assertTrue(pose.orbit + pose.dot <= 1f, "outside at $millis ms: $pose")
            millis += 5f
        }
    }
}
