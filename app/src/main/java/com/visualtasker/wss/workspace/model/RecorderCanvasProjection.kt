package com.visualtasker.wss.workspace.model

enum class RecorderCanvasGestureKind {
    Swipe,
    Path,
    Spline,
}

data class RecorderCanvasGesture(
    val kind: RecorderCanvasGestureKind,
    val points: List<WorldviewPoint>,
    val curved: Boolean = false,
) {
    init {
        require(points.size >= 2) { "Recorder canvas gestures need at least two points." }
    }
}

data class RecorderCanvasProjection(
    val bounds: WorldviewRect? = null,
    val point: WorldviewPoint? = null,
    val gesture: RecorderCanvasGesture? = null,
)

fun RecorderStepUi.toRecorderCanvasProjection(): RecorderCanvasProjection {
    val normalizedAction = actionType.trim().lowercase()
    val coordinateSpace = point?.coordinateSpace
        ?: bounds?.coordinateSpace
        ?: properties.coordinateSpace()
    val explicitPoints = properties.encodedGesturePoints(coordinateSpace)
    val start = properties.coordinatePoint(
        coordinateSpace = coordinateSpace,
        xKeys = listOf("startX", "fromX", "x1"),
        yKeys = listOf("startY", "fromY", "y1"),
    )
    val control = properties.coordinatePoint(
        coordinateSpace = coordinateSpace,
        xKeys = listOf("controlX", "bezierX", "cx"),
        yKeys = listOf("controlY", "bezierY", "cy"),
    )
    val end = properties.coordinatePoint(
        coordinateSpace = coordinateSpace,
        xKeys = listOf("endX", "toX", "x2"),
        yKeys = listOf("endY", "toY", "y2"),
    )
    val gestureKind = when {
        "swipe" in normalizedAction -> RecorderCanvasGestureKind.Swipe
        "spline" in normalizedAction || "curve" in normalizedAction -> RecorderCanvasGestureKind.Spline
        "path" in normalizedAction || "draw" in normalizedAction || "gesture" in normalizedAction -> RecorderCanvasGestureKind.Path
        else -> null
    }
    val gesturePoints = when {
        explicitPoints.size >= 2 -> explicitPoints
        start != null && control != null && end != null -> listOf(start, control, end)
        start != null && end != null -> listOf(start, end)
        point != null && end != null -> listOf(point, end)
        else -> emptyList()
    }
    return RecorderCanvasProjection(
        bounds = bounds,
        point = point,
        gesture = if (gestureKind != null && gesturePoints.size >= 2) {
            RecorderCanvasGesture(
                kind = gestureKind,
                points = gesturePoints,
                curved = control != null || gestureKind == RecorderCanvasGestureKind.Spline,
            )
        } else {
            null
        },
    )
}

private fun Map<String, String>.coordinateSpace(): CoordinateSpace {
    val kind = when (this["coordinateSpace"]?.trim()?.lowercase()) {
        "normalized" -> CoordinateSpaceKind.Normalized
        "image" -> CoordinateSpaceKind.Image
        "viewport" -> CoordinateSpaceKind.Viewport
        "window" -> CoordinateSpaceKind.Window
        else -> CoordinateSpaceKind.Screen
    }
    return CoordinateSpace(kind)
}

private fun Map<String, String>.coordinatePoint(
    coordinateSpace: CoordinateSpace,
    xKeys: List<String>,
    yKeys: List<String>,
): WorldviewPoint? {
    val x = xKeys.firstNotNullOfOrNull { key -> this[key]?.toFloatOrNull() }
    val y = yKeys.firstNotNullOfOrNull { key -> this[key]?.toFloatOrNull() }
    if (x == null || y == null) return null
    return runCatching { WorldviewPoint(x, y, coordinateSpace) }.getOrNull()
}

private fun Map<String, String>.encodedGesturePoints(
    coordinateSpace: CoordinateSpace,
): List<WorldviewPoint> {
    val encoded = listOf("points", "pathPoints", "gesturePoints", "coordinates")
        .firstNotNullOfOrNull { key -> this[key]?.takeIf(String::isNotBlank) }
        ?: return emptyList()
    return NumberPattern.findAll(encoded)
        .mapNotNull { match -> match.value.toFloatOrNull() }
        .chunked(2)
        .mapNotNull { pair ->
            if (pair.size != 2) null else runCatching {
                WorldviewPoint(pair[0], pair[1], coordinateSpace)
            }.getOrNull()
        }
        .toList()
}

private val NumberPattern = Regex("-?\\d+(?:\\.\\d+)?")
