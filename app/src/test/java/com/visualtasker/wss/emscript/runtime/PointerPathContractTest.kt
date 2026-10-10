package com.visualtasker.wss.emscript.runtime

import com.visualtasker.wss.emscript.apply.EmscriptApplyGuard
import com.visualtasker.wss.emscript.apply.EmscriptApplyGuardResult
import com.visualtasker.wss.emscript.parser.EmscriptWorkspaceImporter
import de.visualtasker.blockeditor.emscript.EmscriptGenerator
import de.visualtasker.workflow.serialization.WorkflowSerializer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PointerPathContractTest {
    @Test
    fun `legacy swipe becomes one deterministic linear PointerPath`() {
        val parsed = success("[10, 20, 30, 40], 2")
        val resolved = resolve(parsed.pointerPath)

        assertTrue(parsed.legacySyntax)
        assertEquals(2, resolved.repeat)
        assertEquals(250L, resolved.movementDurationMs)
        assertEquals(
            PointerPathSegment.Line(point(10.0, 20.0), point(30.0, 40.0)),
            resolved.segments.single().geometry,
        )
    }

    @Test
    fun `quadratic Bezier geometry and timing are preserved`() {
        val parsed = success(
            "pointerPath(path([quadraticBezierSegment(point(10,20),point(30,60),point(80,20))])," +
                "duration=900,startHold=40,endHold=60,delay=20,repeat=2)",
        )
        val resolved = resolve(parsed.pointerPath)

        assertEquals(
            PointerPathSegment.QuadraticBezier(
                point(10.0, 20.0),
                point(30.0, 60.0),
                point(80.0, 20.0),
            ),
            resolved.segments.single().geometry,
        )
        assertEquals(900L, resolved.movementDurationMs)
        assertEquals(1_020L, resolved.totalDispatchDurationMs)
        assertEquals(2, resolved.repeat)
    }

    @Test
    fun `cubic Bezier geometry is preserved`() {
        val parsed = success(
            "pointerPath(path([cubicBezierSegment(point(0,0),point(20,80),point(80,80),point(100,0))])," +
                "duration=1200)",
        )

        assertEquals(
            PointerPathSegment.CubicBezier(
                point(0.0, 0.0),
                point(20.0, 80.0),
                point(80.0, 80.0),
                point(100.0, 0.0),
            ),
            resolve(parsed.pointerPath).segments.single().geometry,
        )
    }

    @Test
    fun `mixed segments keep order and receive exact total duration`() {
        val parsed = success(
            "pointerPath(path([" +
                "lineSegment(point(0,0),point(100,0))," +
                "quadraticBezierSegment(point(100,0),point(150,50),point(200,0))," +
                "cubicBezierSegment(point(200,0),point(225,75),point(275,75),point(300,0))" +
                "]),duration=1800)",
        )
        val first = resolve(parsed.pointerPath)
        val second = resolve(parsed.pointerPath)

        assertEquals(3, first.segments.size)
        assertTrue(first.segments[0].geometry is PointerPathSegment.Line)
        assertTrue(first.segments[1].geometry is PointerPathSegment.QuadraticBezier)
        assertTrue(first.segments[2].geometry is PointerPathSegment.CubicBezier)
        assertEquals(1_800L, first.segments.sumOf { it.durationMs })
        assertEquals(first, second)
    }

    @Test
    fun `explicit segment durations remain exact and ordered`() {
        val parsed = success(
            "pointerPath(path([" +
                "lineSegment(point(0,0),point(100,0))," +
                "quadraticBezierSegment(point(100,0),point(150,50),point(200,0))," +
                "lineSegment(point(200,0),point(300,0))" +
                "]),segmentDurations=[100,350,200])",
        )
        val resolved = resolve(parsed.pointerPath)

        assertEquals(listOf(100L, 350L, 200L), resolved.segments.map { it.durationMs })
        assertEquals(650L, resolved.movementDurationMs)
    }

    @Test
    fun `speed is converted to duration without being treated as duration`() {
        val parsed = success(
            "pointerPath(path(point(0,0),point(300,400)),speed=250)",
        )
        val resolved = resolve(parsed.pointerPath)

        assertEquals(null, parsed.pointerPath.timing.durationMs)
        assertEquals(250.0, parsed.pointerPath.timing.speedPixelsPerSecond)
        assertEquals(2_000L, resolved.movementDurationMs)
    }

    @Test
    fun `top level typed swipe supports named timing`() {
        val parsed = success(
            "path(point(10,20),point(30,40)),duration=700,startHold=50,endHold=60,delay=70,repeat=3",
        )
        val resolved = resolve(parsed.pointerPath)

        assertEquals(700L, resolved.movementDurationMs)
        assertEquals(50L, resolved.startHoldMs)
        assertEquals(60L, resolved.endHoldMs)
        assertEquals(70L, resolved.delayMs)
        assertEquals(3, resolved.repeat)
    }

    @Test
    fun `invalid control point arity is diagnosed`() {
        assertFailure(
            "pointerPath(path([quadraticBezierSegment(point(0,0),point(10,10))]),duration=100)",
            "TOUCH_PATH_INVALID",
        )
    }

    @Test
    fun `duration speed conflict is diagnosed`() {
        assertFailure(
            "pointerPath(path(point(0,0),point(10,10)),duration=100,speed=10)",
            "TOUCH_TIMING_CONFLICT",
        )
    }

    @Test
    fun `segment duration count mismatch is diagnosed`() {
        val parsed = success(
            "pointerPath(path([" +
                "lineSegment(point(0,0),point(10,0))," +
                "lineSegment(point(10,0),point(20,0))" +
                "]),segmentDurations=[100])",
        )
        val result = PointerPathLowerer.resolve(parsed.pointerPath)

        assertTrue(result is PointerPathResolutionResult.Failure)
        assertEquals("TOUCH_SEGMENT_TIMING_INVALID", (result as PointerPathResolutionResult.Failure).code)
    }

    @Test
    fun `unknown timing is diagnosed`() {
        assertFailure(
            "pointerPath(path(point(0,0),point(10,10)),duration=100,easing=linear)",
            "TOUCH_TIMING_ARGUMENT_UNKNOWN",
        )
    }

    @Test
    fun `discontinuous segments fail lowering instead of being flattened`() {
        val parsed = success(
            "pointerPath(path([" +
                "lineSegment(point(0,0),point(10,0))," +
                "lineSegment(point(20,0),point(30,0))" +
                "]),duration=100)",
        )
        val result = PointerPathLowerer.resolve(parsed.pointerPath)

        assertTrue(result is PointerPathResolutionResult.Failure)
        assertEquals("TOUCH_PATH_DISCONTINUOUS", (result as PointerPathResolutionResult.Failure).code)
    }

    @Test
    fun `nonzero pointer id is an explicit M5-4 boundary`() {
        val parsed = success(
            "pointerPath(path(point(0,0),point(10,10)),duration=100,pointerId=1)",
        )
        val result = PointerPathLowerer.resolve(parsed.pointerPath)

        assertTrue(result is PointerPathResolutionResult.Failure)
        assertEquals("TOUCH_MULTITOUCH_UNSUPPORTED", (result as PointerPathResolutionResult.Failure).code)
    }

    @Test
    fun `typed PointerPath survives source workspace IR persistence and source roundtrip`() {
        val source = "swipe(pointerPath(path([" +
            "lineSegment(point(100,200),point(200,200))," +
            "quadraticBezierSegment(point(200,200),point(250,260),point(300,200))," +
            "cubicBezierSegment(point(300,200),point(340,140),point(380,260),point(420,200))" +
            "]),duration=1500,startHold=40,endHold=60,delay=20,repeat=2))"
        val importer = EmscriptWorkspaceImporter()
        val first = importer.import(source, workspaceId = "m5-4-pointer-path")
        assertTrue(first.issues.joinToString { it.message }, first.isSuccess)
        val firstDocument = requireNotNull(first.document)
        val persisted = WorkflowSerializer.deserialize(WorkflowSerializer.serialize(firstDocument))
        val generated = EmscriptGenerator().generate(persisted)
        val secondResult = EmscriptApplyGuard().preview(
            draft = generated,
            workspaceId = persisted.id,
            previousDocument = persisted,
        )

        assertTrue(secondResult.toString(), secondResult is EmscriptApplyGuardResult.Success)
        assertTrue(generated.contains("quadraticBezierSegment"))
        assertTrue(generated.contains("cubicBezierSegment"))
        assertTrue(generated.contains("duration=1500"))
        assertTrue(generated.contains("startHold=40"))
        val secondDocument = (secondResult as EmscriptApplyGuardResult.Success).importedDocument
        assertEquals(
            requireNotNull(firstDocument.canonical).entities.map { entity -> entity.ref.id }.toSet(),
            requireNotNull(secondDocument.canonical).entities.map { entity -> entity.ref.id }.toSet(),
        )
        assertEquals(
            requireNotNull(firstDocument.canonical).relations.map { relation -> relation.id }.toSet(),
            requireNotNull(secondDocument.canonical).relations.map { relation -> relation.id }.toSet(),
        )
        assertEquals(
            resolve(success(extractSwipeArguments(generated)).pointerPath),
            resolve(success(extractSwipeArguments(EmscriptGenerator().generate(secondDocument))).pointerPath),
        )
    }

    private fun success(arguments: String): PointerPathParseResult.Success {
        val result = PointerPathSourceCodec.parseSwipe(arguments)
        assertTrue(result.toString(), result is PointerPathParseResult.Success)
        return result as PointerPathParseResult.Success
    }

    private fun resolve(pointerPath: PointerPath): ResolvedPointerPath {
        val result = PointerPathLowerer.resolve(pointerPath)
        assertTrue(result.toString(), result is PointerPathResolutionResult.Success)
        return (result as PointerPathResolutionResult.Success).pointerPath
    }

    private fun assertFailure(arguments: String, code: String) {
        val result = PointerPathSourceCodec.parseSwipe(arguments)
        assertTrue(result.toString(), result is PointerPathParseResult.Failure)
        assertEquals(code, (result as PointerPathParseResult.Failure).code)
    }

    private fun extractSwipeArguments(source: String): String = source.lineSequence()
        .first { it.trimStart().startsWith("swipe(") }
        .trim()
        .removePrefix("swipe(")
        .removeSuffix(";")
        .removeSuffix(")")

    private fun point(x: Double, y: Double) = PointerPathPoint(x, y)
}
