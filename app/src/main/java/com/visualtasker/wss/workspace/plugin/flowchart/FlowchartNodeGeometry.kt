package com.visualtasker.wss.workspace.plugin.flowchart

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Path
import de.visualtasker.blockeditor.registry.BlockCategories
import de.visualtasker.blockeditor.registry.BlockTypes
import de.visualtasker.blockeditor.registry.DefaultBlockRegistry
import de.visualtasker.flowchart.domain.FlowExecutionKind
import de.visualtasker.flowchart.domain.FlowGraphNode
import de.visualtasker.flowchart.domain.FlowLifecycleSemantics
import de.visualtasker.flowchart.domain.FlowNodeKind
import de.visualtasker.flowchart.domain.FlowSemanticValue
import de.visualtasker.flowchart.domain.FlowTerminatorRole
import de.visualtasker.flowchart.domain.terminatorRole
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Presentation-only M3-inspired silhouettes inside the common square node viewport. */
internal enum class FlowchartNodeShape(val paletteId: Int) {
    Circle(1), Slanted(2), Arch(3), Diamond(4), ClamShell(5), Bun(6), PuffyDiamond(7),
    Fan(8), Pentagon(9), Arrow(10), Cookie4(11), Octagon(12), Eye(13), Triangle(14),
    Gem(15), SemiCircle(16), Pill(17), Operator(18), SoftBurst(19), Sunny(20),
    VerySunny(21), Ghostish(22), Flower(23), Clover4(24), Burst(25), FunctionCall(26),
    TryStart(27), Catch(28), TryEnd(29), Default(30);

    companion object {
        fun fromPaletteId(id: Int): FlowchartNodeShape = entries.firstOrNull { it.paletteId == id } ?: Default
    }
}

internal fun flowchartPaletteShape(blockType: String, category: String): FlowchartNodeShape = when {
    blockType.startsWith("event.") -> FlowchartNodeShape.Circle
    blockType.startsWith("control.if") -> FlowchartNodeShape.Diamond
    blockType == BlockTypes.CONTROL_REPEAT -> FlowchartNodeShape.Pentagon
    blockType == BlockTypes.CONTROL_WHILE -> FlowchartNodeShape.SemiCircle
    blockType == BlockTypes.LOGIC_COMPARE -> FlowchartNodeShape.ClamShell
    blockType == BlockTypes.VARIABLE_GET -> FlowchartNodeShape.Bun
    blockType == BlockTypes.VARIABLE_SET -> FlowchartNodeShape.Pill
    blockType == BlockTypes.LOGIC_OPERATE -> FlowchartNodeShape.Operator
    blockType == BlockTypes.LOGIC_AND || blockType == BlockTypes.LOGIC_OR -> FlowchartNodeShape.PuffyDiamond
    blockType.startsWith("feedback.") -> FlowchartNodeShape.Sunny
    category == BlockCategories.DEBUG -> FlowchartNodeShape.VerySunny
    category == BlockCategories.VISION || category == BlockCategories.PERCEPTION -> FlowchartNodeShape.Eye
    category == BlockCategories.CHROME_TAB -> FlowchartNodeShape.Octagon
    category == BlockCategories.TASKER -> FlowchartNodeShape.Cookie4
    category == BlockCategories.TERMUX || category == BlockCategories.SHIZUKU || category == BlockCategories.SCRCPY -> FlowchartNodeShape.Arrow
    else -> FlowchartNodeShape.Slanted
}

internal fun flowchartNodeShape(node: FlowGraphNode): FlowchartNodeShape {
    val role = node.terminatorRole() ?: when (node.kind.standard) {
        FlowNodeKind.ENTRY -> FlowTerminatorRole.START
        FlowNodeKind.EXIT -> FlowTerminatorRole.END
        else -> null
    }
    if (role != null) {
        val executionKind = (node.properties[FlowLifecycleSemantics.EXECUTION_KIND_PROPERTY] as? FlowSemanticValue.StringValue)
            ?.value
            ?.let { value -> FlowExecutionKind.entries.firstOrNull { it.wireValue == value } }
            ?: FlowExecutionKind.WORKFLOW
        return when (executionKind to role) {
            FlowExecutionKind.WORKFLOW to FlowTerminatorRole.START -> FlowchartNodeShape.Circle
            FlowExecutionKind.WORKFLOW to FlowTerminatorRole.END -> FlowchartNodeShape.Arch
            FlowExecutionKind.RECORDING to FlowTerminatorRole.START -> FlowchartNodeShape.Sunny
            FlowExecutionKind.RECORDING to FlowTerminatorRole.END -> FlowchartNodeShape.VerySunny
            FlowExecutionKind.DRY_RUN to FlowTerminatorRole.START -> FlowchartNodeShape.SoftBurst
            FlowExecutionKind.DRY_RUN to FlowTerminatorRole.END -> FlowchartNodeShape.Burst
            else -> FlowchartNodeShape.Default
        }
    }

    val blockType = (node.properties["blockType"] as? FlowSemanticValue.StringValue)?.value.orEmpty()
    flowchartShapeForKnownBlockType(blockType)?.let { return it }

    val category = DefaultBlockRegistry.allDefinitions().firstOrNull { it.id == blockType }?.category
    if (category != null) return flowchartPaletteShape(blockType, category)

    return when {
        blockType.startsWith("control.if") || blockType.startsWith("logic.compare") -> FlowchartNodeShape.Diamond
        blockType.startsWith("variable.get") || blockType.startsWith("variables.get") -> FlowchartNodeShape.Bun
        blockType.startsWith("variable.") || blockType.startsWith("variables.") -> FlowchartNodeShape.Pill
        blockType.startsWith("logic.") -> FlowchartNodeShape.Operator
        blockType.startsWith("control.") -> FlowchartNodeShape.Pentagon
        blockType.startsWith("feedback.") -> FlowchartNodeShape.Sunny
        blockType.startsWith("vision.") || blockType.startsWith("perception.") -> FlowchartNodeShape.Eye
        blockType.startsWith("chromeTab.") -> FlowchartNodeShape.Octagon
        blockType.startsWith("tasker.") -> FlowchartNodeShape.Cookie4
        blockType.startsWith("termux.") || blockType.startsWith("shizuku.") || blockType.startsWith("scrcpy.") -> FlowchartNodeShape.Arrow
        else -> when (node.kind.standard) {
            FlowNodeKind.ENTRY -> FlowchartNodeShape.Circle
            FlowNodeKind.EXIT -> FlowchartNodeShape.Arch
            FlowNodeKind.ACTION -> FlowchartNodeShape.Slanted
            FlowNodeKind.INPUT -> FlowchartNodeShape.Fan
            FlowNodeKind.OUTPUT -> FlowchartNodeShape.Arrow
            FlowNodeKind.ASSIGNMENT -> FlowchartNodeShape.Pill
            FlowNodeKind.PROPERTY_ACCESS -> FlowchartNodeShape.Bun
            FlowNodeKind.DECISION -> FlowchartNodeShape.Diamond
            FlowNodeKind.ELSE_IF -> FlowchartNodeShape.Cookie4
            FlowNodeKind.ELSE -> FlowchartNodeShape.Ghostish
            FlowNodeKind.LOOP_START -> FlowchartNodeShape.Pentagon
            FlowNodeKind.LOOP_END -> FlowchartNodeShape.SemiCircle
            FlowNodeKind.TRY_START -> FlowchartNodeShape.TryStart
            FlowNodeKind.CATCH -> FlowchartNodeShape.Catch
            FlowNodeKind.TRY_END -> FlowchartNodeShape.TryEnd
            FlowNodeKind.FUNCTION_START -> FlowchartNodeShape.Gem
            FlowNodeKind.FUNCTION_END -> FlowchartNodeShape.Arch
            FlowNodeKind.FUNCTION_CALL -> FlowchartNodeShape.FunctionCall
            FlowNodeKind.ANNOTATION -> FlowchartNodeShape.Flower
            FlowNodeKind.SYNTHETIC -> FlowchartNodeShape.Clover4
            FlowNodeKind.UNKNOWN_SOURCE -> FlowchartNodeShape.Burst
            null -> FlowchartNodeShape.Default
        }
    }
}

private fun flowchartShapeForKnownBlockType(blockType: String): FlowchartNodeShape? = when (blockType) {
    BlockTypes.EVENT_START -> FlowchartNodeShape.Circle
    BlockTypes.ACTION_CLICK_TEXT -> FlowchartNodeShape.Slanted
    BlockTypes.ACTION_WAIT -> FlowchartNodeShape.SemiCircle
    BlockTypes.ACTION_FIND_TEMPLATE -> FlowchartNodeShape.Eye
    BlockTypes.DEBUG_LOG -> FlowchartNodeShape.SoftBurst
    BlockTypes.FEEDBACK_BEEP -> FlowchartNodeShape.Sunny
    BlockTypes.FEEDBACK_VIBRATE -> FlowchartNodeShape.VerySunny
    BlockTypes.CONTROL_REPEAT -> FlowchartNodeShape.Pentagon
    BlockTypes.CONTROL_WHILE -> FlowchartNodeShape.Arch
    BlockTypes.CONTROL_IF,
    BlockTypes.CONTROL_IF_ELSE,
    BlockTypes.CONTROL_IF_ELSEIF_ELSE -> FlowchartNodeShape.Diamond
    BlockTypes.LOGIC_SCREEN_CONTAINS -> FlowchartNodeShape.Eye
    BlockTypes.LOGIC_COMPARE -> FlowchartNodeShape.ClamShell
    BlockTypes.LOGIC_BOOLEAN,
    BlockTypes.LITERAL_BOOLEAN -> FlowchartNodeShape.Triangle
    BlockTypes.LOGIC_AND,
    BlockTypes.LOGIC_OR -> FlowchartNodeShape.PuffyDiamond
    BlockTypes.LOGIC_OPERATE -> FlowchartNodeShape.Operator
    BlockTypes.LITERAL_NUMBER -> FlowchartNodeShape.Pill
    BlockTypes.LITERAL_STRING -> FlowchartNodeShape.FunctionCall
    BlockTypes.VARIABLE_GET,
    BlockTypes.VARIABLE_REPORTER,
    BlockTypes.VARIABLE_VALUE,
    BlockTypes.VARIABLES_GET -> FlowchartNodeShape.Bun
    BlockTypes.VARIABLE_SET -> FlowchartNodeShape.Fan
    else -> when {
        blockType.startsWith(BlockTypes.VARIABLE_REPORTER_PREFIX) -> FlowchartNodeShape.Bun
        blockType.startsWith(BlockTypes.EMSCRIPT_COMMAND_PREFIX) -> FlowchartNodeShape.Slanted
        else -> null
    }
}

internal fun flowchartNodeShapePath(shape: FlowchartNodeShape, width: Float, height: Float): Path {
    val w = width.coerceAtLeast(8f)
    val h = height.coerceAtLeast(8f)
    return Path().apply {
        when (shape) {
            FlowchartNodeShape.Circle -> addOval(Rect(0f, 0f, w, h))
            FlowchartNodeShape.Slanted -> polygon(w, h, .14f to 0f, 1f to 0f, .86f to 1f, 0f to 1f)
            FlowchartNodeShape.Arch -> {
                moveTo(0f, h); lineTo(0f, h * .48f)
                cubicTo(0f, h * .16f, w * .18f, 0f, w * .5f, 0f)
                cubicTo(w * .82f, 0f, w, h * .16f, w, h * .48f)
                lineTo(w, h); close()
            }
            FlowchartNodeShape.Diamond -> polygon(w, h, .5f to 0f, 1f to .5f, .5f to 1f, 0f to .5f)
            FlowchartNodeShape.ClamShell -> polygon(w, h, .16f to 0f, .84f to 0f, 1f to .5f, .84f to 1f, .16f to 1f, 0f to .5f)
            FlowchartNodeShape.Bun -> bun(w, h)
            FlowchartNodeShape.PuffyDiamond -> puffyDiamond(w, h)
            FlowchartNodeShape.Fan -> {
                moveTo(0f, 0f); cubicTo(w * .5f, 0f, w, h * .5f, w, h); lineTo(0f, h); close()
            }
            FlowchartNodeShape.Pentagon -> polygon(w, h, .5f to 0f, 1f to .34f, .82f to 1f, .18f to 1f, 0f to .34f)
            FlowchartNodeShape.Arrow -> polygon(w, h, 0f to .18f, .62f to .18f, .62f to 0f, 1f to .5f, .62f to 1f, .62f to .82f, 0f to .82f)
            FlowchartNodeShape.Cookie4 -> cookie4(w, h)
            FlowchartNodeShape.Octagon -> polygon(w, h, .18f to 0f, .82f to 0f, 1f to .18f, 1f to .82f, .82f to 1f, .18f to 1f, 0f to .82f, 0f to .18f)
            FlowchartNodeShape.Eye -> {
                moveTo(0f, h * .5f); cubicTo(w * .2f, 0f, w * .8f, 0f, w, h * .5f)
                cubicTo(w * .8f, h, w * .2f, h, 0f, h * .5f); close()
            }
            FlowchartNodeShape.Triangle -> polygon(w, h, .5f to 0f, 1f to 1f, 0f to 1f)
            FlowchartNodeShape.Gem -> polygon(w, h, .5f to 0f, .9f to .2f, 1f to .68f, .68f to 1f, .32f to 1f, 0f to .68f, .1f to .2f)
            FlowchartNodeShape.SemiCircle -> {
                moveTo(0f, h); lineTo(0f, h * .5f)
                cubicTo(0f, h * .22f, w * .22f, 0f, w * .5f, 0f)
                cubicTo(w * .78f, 0f, w, h * .22f, w, h * .5f)
                lineTo(w, h); close()
            }
            FlowchartNodeShape.Pill -> {
                moveTo(w * .28f, 0f)
                cubicTo(w * .10f, 0f, 0f, h * .18f, 0f, h * .42f)
                cubicTo(0f, h * .68f, w * .14f, h * .90f, w * .34f, h * .96f)
                cubicTo(w * .54f, h, w * .78f, h, w * .90f, h * .84f)
                cubicTo(w, h * .70f, w, h * .42f, w * .90f, h * .26f)
                cubicTo(w * .78f, h * .06f, w * .54f, 0f, w * .28f, 0f)
                close()
            }
            FlowchartNodeShape.Operator -> {
                moveTo(w * .14f, h * .06f); lineTo(w * .86f, h * .06f)
                cubicTo(w, h * .2f, w, h * .8f, w * .86f, h * .94f); lineTo(w * .14f, h * .94f)
                cubicTo(0f, h * .8f, 0f, h * .2f, w * .14f, h * .06f); close()
            }
            FlowchartNodeShape.SoftBurst -> radialShape(w, h, 8, .78f, true)
            FlowchartNodeShape.Sunny -> radialShape(w, h, 10, .78f, true)
            FlowchartNodeShape.VerySunny -> radialShape(w, h, 14, .68f, true)
            FlowchartNodeShape.Ghostish -> ghostish(w, h)
            FlowchartNodeShape.Flower -> radialShape(w, h, 6, .72f, true)
            FlowchartNodeShape.Clover4 -> clover4(w, h)
            FlowchartNodeShape.Burst -> radialShape(w, h, 12, .56f, false)
            FlowchartNodeShape.FunctionCall -> polygon(w, h, .12f to 0f, .88f to 0f, 1f to .5f, .88f to 1f, .12f to 1f, 0f to .5f)
            FlowchartNodeShape.TryStart -> polygon(w, h, 0f to 0f, .78f to 0f, 1f to .22f, 1f to 1f, 0f to 1f)
            FlowchartNodeShape.Catch -> polygon(w, h, .22f to 0f, 1f to 0f, 1f to 1f, .22f to 1f, 0f to .5f)
            FlowchartNodeShape.TryEnd -> polygon(w, h, 0f to 0f, 1f to 0f, 1f to .78f, .78f to 1f, 0f to 1f)
            FlowchartNodeShape.Default -> addRoundRect(RoundRect(Rect(0f, 0f, w, h), CornerRadius(w * .18f, h * .18f)))
        }
    }
}

private fun Path.polygon(width: Float, height: Float, vararg points: Pair<Float, Float>) {
    points.forEachIndexed { index, point ->
        val x = width * point.first
        val y = height * point.second
        if (index == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}

private fun Path.bun(w: Float, h: Float) {
    moveTo(w * .22f, 0f)
    cubicTo(w * .02f, 0f, 0f, h * .2f, 0f, h * .5f)
    cubicTo(0f, h * .8f, w * .02f, h, w * .22f, h)
    cubicTo(w * .42f, h, w * .42f, h * .74f, w * .5f, h * .74f)
    cubicTo(w * .58f, h * .74f, w * .58f, h, w * .78f, h)
    cubicTo(w * .98f, h, w, h * .8f, w, h * .5f)
    cubicTo(w, h * .2f, w * .98f, 0f, w * .78f, 0f); close()
}

private fun Path.puffyDiamond(w: Float, h: Float) {
    moveTo(w * .5f, 0f)
    cubicTo(w * .64f, 0f, w * .67f, h * .18f, w * .78f, h * .22f)
    cubicTo(w * .9f, h * .27f, w, h * .34f, w, h * .5f)
    cubicTo(w, h * .66f, w * .9f, h * .73f, w * .78f, h * .78f)
    cubicTo(w * .67f, h * .82f, w * .64f, h, w * .5f, h)
    cubicTo(w * .36f, h, w * .33f, h * .82f, w * .22f, h * .78f)
    cubicTo(w * .1f, h * .73f, 0f, h * .66f, 0f, h * .5f)
    cubicTo(0f, h * .34f, w * .1f, h * .27f, w * .22f, h * .22f)
    cubicTo(w * .33f, h * .18f, w * .36f, 0f, w * .5f, 0f); close()
}

private fun Path.cookie4(w: Float, h: Float) {
    moveTo(w * .5f, 0f)
    cubicTo(w * .68f, 0f, w * .68f, h * .2f, w, h * .2f)
    cubicTo(w, h * .5f, w * .8f, h * .5f, w * .8f, h * .8f)
    cubicTo(w * .5f, h * .8f, w * .5f, h, w * .2f, h * .8f)
    cubicTo(w * .2f, h * .5f, 0f, h * .5f, 0f, h * .2f)
    cubicTo(w * .32f, h * .2f, w * .32f, 0f, w * .5f, 0f); close()
}

private fun Path.ghostish(w: Float, h: Float) {
    moveTo(0f, h); lineTo(0f, h * .28f)
    cubicTo(0f, h * .1f, w * .14f, 0f, w * .32f, 0f)
    cubicTo(w * .44f, 0f, w * .44f, h * .15f, w * .5f, h * .15f)
    cubicTo(w * .56f, h * .15f, w * .56f, 0f, w * .68f, 0f)
    cubicTo(w * .86f, 0f, w, h * .1f, w, h * .28f); lineTo(w, h)
    cubicTo(w * .78f, h * .84f, w * .7f, h, w * .5f, h)
    cubicTo(w * .3f, h, w * .22f, h * .84f, 0f, h); close()
}

private fun Path.clover4(w: Float, h: Float) {
    moveTo(w * .5f, h * .16f)
    cubicTo(w * .58f, 0f, w, 0f, w, h * .32f)
    cubicTo(w, h * .48f, w * .84f, h * .5f, w * .84f, h * .5f)
    cubicTo(w, h * .5f, w, h * .92f, w * .68f, h)
    cubicTo(w * .52f, h, w * .5f, h * .84f, w * .5f, h * .84f)
    cubicTo(w * .5f, h, w * .08f, h, 0f, h * .68f)
    cubicTo(0f, h * .52f, w * .16f, h * .5f, w * .16f, h * .5f)
    cubicTo(0f, h * .5f, 0f, h * .08f, w * .32f, 0f)
    cubicTo(w * .48f, 0f, w * .5f, h * .16f, w * .5f, h * .16f); close()
}

private fun Path.radialShape(w: Float, h: Float, points: Int, innerRatio: Float, rounded: Boolean) {
    val center = Offset(w / 2f, h / 2f)
    val outer = minOf(w, h) / 2f
    val vertices = List(points * 2) { index ->
        val angle = -PI / 2.0 + index * PI / points
        val radius = if (index % 2 == 0) outer else outer * innerRatio
        Offset(center.x + cos(angle).toFloat() * radius, center.y + sin(angle).toFloat() * radius)
    }
    if (!rounded) {
        vertices.forEachIndexed { index, point -> if (index == 0) moveTo(point.x, point.y) else lineTo(point.x, point.y) }
        close(); return
    }
    vertices.forEachIndexed { index, point ->
        val previous = vertices[(index - 1 + vertices.size) % vertices.size]
        val next = vertices[(index + 1) % vertices.size]
        val entry = Offset(point.x + (previous.x - point.x) * .22f, point.y + (previous.y - point.y) * .22f)
        val exit = Offset(point.x + (next.x - point.x) * .22f, point.y + (next.y - point.y) * .22f)
        if (index == 0) moveTo(entry.x, entry.y) else lineTo(entry.x, entry.y)
        quadraticTo(point.x, point.y, exit.x, exit.y)
    }
    close()
}
