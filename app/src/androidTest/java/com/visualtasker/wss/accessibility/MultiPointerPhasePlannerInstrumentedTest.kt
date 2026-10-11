package com.visualtasker.wss.accessibility

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MultiPointerPhasePlannerInstrumentedTest {
    @Test
    fun delayedPointersShareOneDeterministicContinuationTimeline() {
        val service = VisualTaskerAccessibilityService()
        val plan = service.inspectMultiPointerPhasePlan(
            listOf(
                pointer(
                    pointerId = 4,
                    fromX = 100f,
                    toX = 500f,
                    durationMs = 500L,
                ),
                pointer(
                    pointerId = 9,
                    fromX = 500f,
                    toX = 100f,
                    durationMs = 300L,
                    delayMs = 100L,
                ),
            ),
        )

        assertEquals(500L, plan.sumOf { it.startTimeMs + it.durationMs })
        assertEquals(listOf(4, 9), plan.first().pointerIds)
        assertEquals(listOf(0L, 100L), plan.first().strokeStartTimesMs)
        assertEquals(listOf(400L, 300L), plan.first().strokeDurationsMs)
        assertEquals(400L, plan.first().durationMs)
        assertTrue(plan.any { it.pointerIds == listOf(4, 9) })
        assertEquals(listOf(4), plan.last().pointerIds)
        assertTrue(plan.drop(1).all { it.durationMs in 1L..200L })
        assertTrue(plan.drop(1).all { it.startTimeMs == 0L })
    }

    @Test
    fun simultaneousPointersRemainInEveryContinuedPhase() {
        val service = VisualTaskerAccessibilityService()
        val plan = service.inspectMultiPointerPhasePlan(
            listOf(
                pointer(pointerId = 0, fromX = 200f, toX = 400f, durationMs = 450L),
                pointer(pointerId = 1, fromX = 600f, toX = 400f, durationMs = 450L),
            ),
        )

        assertEquals(450L, plan.sumOf { it.startTimeMs + it.durationMs })
        assertEquals(3, plan.size)
        assertTrue(plan.all { it.pointerIds == listOf(0, 1) })
        assertEquals(listOf(200L, 200L, 50L), plan.map { it.durationMs })
    }

    @Test
    fun delayedPointerAfterEarlierTimingBoundaryIsRejectedBeforeDispatch() {
        val service = VisualTaskerAccessibilityService()

        val error = assertThrows(IllegalArgumentException::class.java) {
            service.inspectMultiPointerPhasePlan(
                listOf(
                    pointer(
                        pointerId = 0,
                        fromX = 100f,
                        toX = 500f,
                        durationMs = 500L,
                        startHoldMs = 80L,
                    ),
                    pointer(
                        pointerId = 1,
                        fromX = 500f,
                        toX = 100f,
                        durationMs = 300L,
                        delayMs = 100L,
                    ),
                ),
            )
        }

        assertTrue(error.message.orEmpty().contains("timing-section boundary"))
    }

    private fun pointer(
        pointerId: Int,
        fromX: Float,
        toX: Float,
        durationMs: Long,
        delayMs: Long = 0L,
        startHoldMs: Long = 0L,
    ): AccessibilityPointerPath {
        val from = AccessibilityGesturePoint(fromX, 700f)
        val to = AccessibilityGesturePoint(toX, 700f)
        return AccessibilityPointerPath(
            pointerId = pointerId,
            startPoint = from,
            segments = listOf(AccessibilityGestureSegment.Line(from, to, durationMs)),
            startHoldMs = startHoldMs,
            endHoldMs = 0L,
            delayMs = delayMs,
        )
    }
}
