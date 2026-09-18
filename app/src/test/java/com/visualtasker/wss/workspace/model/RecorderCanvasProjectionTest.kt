package com.visualtasker.wss.workspace.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RecorderCanvasProjectionTest {
    @Test
    fun `click keeps recorded point and bounds without inventing gesture`() {
        val bounds = WorldviewRect(10f, 20f, 60f, 80f, CoordinateSpace(CoordinateSpaceKind.Screen))
        val point = WorldviewPoint(35f, 50f, CoordinateSpace(CoordinateSpaceKind.Screen))

        val projection = RecorderStepUi(
            id = "click",
            label = "Click",
            actionType = "click",
            status = StepStatus.Recorded,
            bounds = bounds,
            point = point,
        ).toRecorderCanvasProjection()

        assertEquals(bounds, projection.bounds)
        assertEquals(point, projection.point)
        assertNull(projection.gesture)
    }

    @Test
    fun `swipe maps explicit start and end coordinates`() {
        val projection = RecorderStepUi(
            id = "swipe",
            label = "Swipe",
            actionType = "swipe",
            status = StepStatus.Recorded,
            properties = mapOf("startX" to "10", "startY" to "20", "endX" to "110", "endY" to "220"),
        ).toRecorderCanvasProjection()

        assertEquals(RecorderCanvasGestureKind.Swipe, projection.gesture?.kind)
        assertEquals(listOf(10f, 110f), projection.gesture?.points?.map { it.x })
        assertEquals(listOf(20f, 220f), projection.gesture?.points?.map { it.y })
    }

    @Test
    fun `spline maps bezier control point`() {
        val projection = RecorderStepUi(
            id = "spline",
            label = "Spline",
            actionType = "marker.spline",
            status = StepStatus.Recorded,
            properties = mapOf(
                "startX" to "5", "startY" to "10",
                "controlX" to "30", "controlY" to "2",
                "endX" to "50", "endY" to "40",
            ),
        ).toRecorderCanvasProjection()

        assertEquals(RecorderCanvasGestureKind.Spline, projection.gesture?.kind)
        assertEquals(true, projection.gesture?.curved)
        assertEquals(3, projection.gesture?.points?.size)
    }

    @Test
    fun `path accepts encoded point list`() {
        val projection = RecorderStepUi(
            id = "path",
            label = "Path",
            actionType = "draw.path",
            status = StepStatus.Recorded,
            properties = mapOf("points" to "10,20; 30,40; 50,60"),
        ).toRecorderCanvasProjection()

        assertEquals(RecorderCanvasGestureKind.Path, projection.gesture?.kind)
        assertEquals(listOf(10f, 30f, 50f), projection.gesture?.points?.map { it.x })
    }

    @Test
    fun `gesture without coordinates stays geometry free`() {
        val projection = RecorderStepUi(
            id = "legacy",
            label = "Legacy swipe",
            actionType = "swipe",
            status = StepStatus.Recorded,
        ).toRecorderCanvasProjection()

        assertNull(projection.gesture)
    }
}
