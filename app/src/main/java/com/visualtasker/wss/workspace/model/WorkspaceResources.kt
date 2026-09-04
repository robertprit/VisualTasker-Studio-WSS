package com.visualtasker.wss.workspace.model

const val WORKSPACE_RESOURCE_SCHEMA_VERSION = 1

enum class WorkspaceResourceKind {
    Marker,
    Region,
    Template,
    Screenshot,
    Dataset,
    Overlay,
    Unknown,
}

enum class WorkspaceMarkerMode {
    Region,
    Point,
    Swipe,
    Path,
    Node,
}

data class WorkspaceRegionBounds(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    init {
        require(left in 0f..1f && right in 0f..1f && top in 0f..1f && bottom in 0f..1f) {
            "Region bounds must be normalized to 0..1."
        }
        require(left < right && top < bottom) {
            "Region bounds must describe a non-empty area."
        }
    }
}

data class WorkspacePointBounds(
    val x: Float,
    val y: Float,
) {
    init {
        require(x in 0f..1f && y in 0f..1f) {
            "Point bounds must be normalized to 0..1."
        }
    }
}

data class WorkspaceResource(
    val id: String,
    val kind: WorkspaceResourceKind,
    val label: String,
    val pluginOwner: String = "visualtasker.core",
    val uri: String? = null,
    val mimeType: String? = null,
    val packageName: String? = null,
    val activityClass: String? = null,
    val markerMode: WorkspaceMarkerMode? = null,
    val region: WorkspaceRegionBounds? = null,
    val point: WorkspacePointBounds? = null,
    val referenceWidthPx: Int? = null,
    val referenceHeightPx: Int? = null,
    val hidden: Boolean = false,
    val locked: Boolean = false,
    val tags: Set<String> = emptySet(),
    val metadata: Map<String, String> = emptyMap(),
    val createdAtEpochMs: Long = 0L,
    val updatedAtEpochMs: Long = createdAtEpochMs,
) {
    init {
        require(id.matches(RESOURCE_ID_PATTERN)) {
            "Resource id must use lowercase letters, numbers, dot, dash, underscore, or colon."
        }
        require(label.isNotBlank() && label == label.trim()) {
            "Resource label must be nonblank and trimmed."
        }
        require(pluginOwner.isNotBlank() && pluginOwner == pluginOwner.trim()) {
            "Plugin owner must be nonblank and trimmed."
        }
        require(tags.all { it.isNotBlank() && it == it.trim() }) {
            "Resource tags must be nonblank and trimmed."
        }
        require(metadata.keys.all { it.isNotBlank() && it == it.trim() }) {
            "Resource metadata keys must be nonblank and trimmed."
        }
        require(referenceWidthPx == null || referenceWidthPx > 0) {
            "Reference width must be positive."
        }
        require(referenceHeightPx == null || referenceHeightPx > 0) {
            "Reference height must be positive."
        }
    }
}

data class WorkspaceResourceBundle(
    val schemaVersion: Int = WORKSPACE_RESOURCE_SCHEMA_VERSION,
    val revision: Long = 0L,
    val resources: List<WorkspaceResource> = emptyList(),
) {
    init {
        require(schemaVersion == WORKSPACE_RESOURCE_SCHEMA_VERSION) {
            "Unsupported workspace resource schema version $schemaVersion."
        }
        require(resources.map { it.id }.toSet().size == resources.size) {
            "Workspace resources must have unique ids."
        }
    }

    fun find(id: String): WorkspaceResource? =
        resources.firstOrNull { it.id == id }

    fun byKind(kind: WorkspaceResourceKind): List<WorkspaceResource> =
        resources.filter { it.kind == kind }
}

object WorkspaceResourceReducer {
    fun upsert(bundle: WorkspaceResourceBundle, resource: WorkspaceResource): WorkspaceResourceBundle {
        val nextResources = if (bundle.resources.any { it.id == resource.id }) {
            bundle.resources.map { existing -> if (existing.id == resource.id) resource else existing }
        } else {
            bundle.resources + resource
        }
        return bundle.copy(
            revision = bundle.revision + 1,
            resources = nextResources.sortedWith(compareBy<WorkspaceResource> { it.kind.name }.thenBy { it.label }),
        )
    }

    fun remove(bundle: WorkspaceResourceBundle, resourceId: String): WorkspaceResourceBundle {
        val nextResources = bundle.resources.filterNot { it.id == resourceId }
        return if (nextResources.size == bundle.resources.size) {
            bundle
        } else {
            bundle.copy(revision = bundle.revision + 1, resources = nextResources)
        }
    }
}

fun normalizedRegionFromPixels(
    leftPx: Int,
    topPx: Int,
    rightPx: Int,
    bottomPx: Int,
    referenceWidthPx: Int,
    referenceHeightPx: Int,
): WorkspaceRegionBounds {
    require(referenceWidthPx > 0 && referenceHeightPx > 0) {
        "Reference size must be positive."
    }
    require(leftPx < rightPx && topPx < bottomPx) {
        "Pixel region must describe a non-empty area."
    }
    return WorkspaceRegionBounds(
        left = (leftPx.toFloat() / referenceWidthPx).coerceIn(0f, 1f),
        top = (topPx.toFloat() / referenceHeightPx).coerceIn(0f, 1f),
        right = (rightPx.toFloat() / referenceWidthPx).coerceIn(0f, 1f),
        bottom = (bottomPx.toFloat() / referenceHeightPx).coerceIn(0f, 1f),
    )
}

fun normalizedPointFromPixels(
    xPx: Int,
    yPx: Int,
    referenceWidthPx: Int,
    referenceHeightPx: Int,
): WorkspacePointBounds {
    require(referenceWidthPx > 0 && referenceHeightPx > 0) {
        "Reference size must be positive."
    }
    return WorkspacePointBounds(
        x = (xPx.toFloat() / referenceWidthPx).coerceIn(0f, 1f),
        y = (yPx.toFloat() / referenceHeightPx).coerceIn(0f, 1f),
    )
}

private val RESOURCE_ID_PATTERN = Regex("[a-z0-9][a-z0-9._:-]*")
