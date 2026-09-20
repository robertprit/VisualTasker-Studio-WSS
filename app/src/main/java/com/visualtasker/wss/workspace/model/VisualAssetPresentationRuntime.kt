package com.visualtasker.wss.workspace.model

import androidx.compose.ui.graphics.Path
import com.compose.canvas.asset.EmaCodec as ShapeMakerEmaCodec
import com.compose.canvas.asset.PathMorpher
import com.compose.canvas.geometry.PathGeometry
import com.compose.canvas.geometry.PathSegment
import java.io.File
import kotlin.math.min

data class VisualAssetPortProjection(
    val name: String,
    val xFraction: Float,
    val yFraction: Float,
    val connectionType: String,
    val valueType: String,
    val snapRadius: Float,
)

data class VisualAssetContentAreaProjection(
    val name: String,
    val leftFraction: Float,
    val topFraction: Float,
    val rightFraction: Float,
    val bottomFraction: Float,
    val contentType: String,
)

data class VisualAssetSemanticProjection(
    val assetId: String,
    val ports: List<VisualAssetPortProjection>,
    val contentAreas: List<VisualAssetContentAreaProjection>,
)

class VisualAssetPresentationRuntime private constructor(
    private val catalog: VisualAssetCatalog,
    private val assetsById: Map<String, StoredEmaVisualAsset>,
) {
    fun pathFor(
        target: EmaVisualAssetBindingTarget,
        targetId: String,
        width: Float,
        height: Float,
    ): Path? {
        if (width <= 0f || height <= 0f) return null
        val geometry = geometryFor(target, targetId) ?: return null
        return geometry.fitInto(width, height)
    }

    fun geometryFor(target: EmaVisualAssetBindingTarget, targetId: String): PathGeometry? {
        val asset = boundAsset(target, targetId) ?: return null
        val decoded = runCatching { ShapeMakerEmaCodec.decode(asset.file.readText()) }.getOrNull() ?: return null
        return PathMorpher.previewPath(decoded.design)
    }

    fun semanticsFor(target: EmaVisualAssetBindingTarget, targetId: String): VisualAssetSemanticProjection? {
        val asset = boundAsset(target, targetId) ?: return null
        val decoded = runCatching { ShapeMakerEmaCodec.decode(asset.file.readText()) }.getOrNull() ?: return null
        val canvas = decoded.design.canvas
        val width = canvas.width.coerceAtLeast(1f)
        val height = canvas.height.coerceAtLeast(1f)
        val ports = (decoded.design.metadata.ports + decoded.design.allObjects().flatMap { it.semantics.ports })
            .distinctBy { it.id }
            .map { port ->
                VisualAssetPortProjection(
                    name = port.name,
                    xFraction = (port.x / width).coerceIn(0f, 1f),
                    yFraction = (port.y / height).coerceIn(0f, 1f),
                    connectionType = port.connectionType,
                    valueType = port.valueType,
                    snapRadius = port.snapRadius,
                )
            }
        val contentAreas = (decoded.design.metadata.contentAreas + decoded.design.allObjects().flatMap { it.semantics.contentAreas })
            .distinctBy { it.id }
            .map { area ->
                VisualAssetContentAreaProjection(
                    name = area.name,
                    leftFraction = (area.bounds.left / width).coerceIn(0f, 1f),
                    topFraction = (area.bounds.top / height).coerceIn(0f, 1f),
                    rightFraction = (area.bounds.right / width).coerceIn(0f, 1f),
                    bottomFraction = (area.bounds.bottom / height).coerceIn(0f, 1f),
                    contentType = area.contentType,
                )
            }
        return VisualAssetSemanticProjection(asset.descriptor.assetId, ports, contentAreas)
    }

    fun toolboxAssets(setId: String): List<StoredEmaVisualAsset> {
        val ids = catalog.toolboxSets.firstOrNull { it.id == setId }?.assetIds.orEmpty()
        return ids.mapNotNull(assetsById::get).sortedBy { it.descriptor.name }
    }

    private fun boundAsset(target: EmaVisualAssetBindingTarget, targetId: String): StoredEmaVisualAsset? {
        val binding = catalog.bindings.firstOrNull { it.target == target && it.targetId == targetId }
            ?: catalog.bindings.firstOrNull { it.target == target && it.targetId == "*" }
            ?: return null
        return assetsById[binding.assetId]
    }

    companion object {
        fun load(directory: File): VisualAssetPresentationRuntime = VisualAssetPresentationRuntime(
            catalog = VisualAssetCatalogStore.load(File(directory, "catalog.json")),
            assetsById = EmaVisualAssetStore.latestByAssetId(directory).associateBy { it.descriptor.assetId },
        )
    }
}

private fun PathGeometry.fitInto(width: Float, height: Float): Path {
    val bounds = boundingRect()
    if (bounds.width <= 0f || bounds.height <= 0f) return Path()
    val padding = min(width, height) * 0.06f
    val availableWidth = (width - padding * 2f).coerceAtLeast(1f)
    val availableHeight = (height - padding * 2f).coerceAtLeast(1f)
    val scale = min(availableWidth / bounds.width, availableHeight / bounds.height)
    val offsetX = (width - bounds.width * scale) / 2f - bounds.left * scale
    val offsetY = (height - bounds.height * scale) / 2f - bounds.top * scale
    fun x(value: Float) = value * scale + offsetX
    fun y(value: Float) = value * scale + offsetY
    return Path().apply {
        segments.forEach { segment ->
            when (segment) {
                is PathSegment.MoveTo -> moveTo(x(segment.x), y(segment.y))
                is PathSegment.LineTo -> lineTo(x(segment.x), y(segment.y))
                is PathSegment.QuadraticBezierTo -> quadraticTo(
                    x(segment.cx), y(segment.cy), x(segment.x), y(segment.y),
                )
                is PathSegment.CubicBezierTo -> cubicTo(
                    x(segment.cx1), y(segment.cy1),
                    x(segment.cx2), y(segment.cy2),
                    x(segment.x), y(segment.y),
                )
                PathSegment.Close -> close()
            }
        }
        if (closed && segments.none { it is PathSegment.Close }) close()
    }
}
