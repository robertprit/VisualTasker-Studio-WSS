package com.visualtasker.wss.workspace.model

import org.junit.Assert.assertEquals
import org.junit.Test

class DatastoreDomainProjectionTest {
    @Test
    fun resourcesAreAssignedToExactlyOneStableDomain() {
        val resources = listOf(
            WorkspaceResource("marker:one", WorkspaceResourceKind.Marker, "Marker"),
            WorkspaceResource("vision:one", WorkspaceResourceKind.Dataset, "Detection", pluginOwner = "visualtasker.vision", tags = setOf("vision", "yolo")),
            WorkspaceResource("runtime:one", WorkspaceResourceKind.Dataset, "Counter", pluginOwner = "visualtasker.runtime", tags = setOf("runtime")),
            WorkspaceResource("asset:one", WorkspaceResourceKind.VisualAsset, "Shape"),
            WorkspaceResource("plugin:one", WorkspaceResourceKind.Dataset, "Tasker", pluginOwner = "visualtasker.tasker"),
            WorkspaceResource("junktor:one", WorkspaceResourceKind.Dataset, "Candidate", pluginOwner = "visualtasker.junktor", tags = setOf("junktor")),
        )

        val projection = DatastoreDomainProjector.project(
            WorkspaceResourceBundle(resources = resources),
            junktorSuggestionCount = 3,
        )

        assertEquals(listOf("marker:one"), projection.group(DatastoreDomain.Human).resources.map { it.id })
        assertEquals(listOf("vision:one"), projection.group(DatastoreDomain.MachineVision).resources.map { it.id })
        assertEquals(listOf("runtime:one"), projection.group(DatastoreDomain.Runtime).resources.map { it.id })
        assertEquals(listOf("asset:one"), projection.group(DatastoreDomain.Resources).resources.map { it.id })
        assertEquals(listOf("plugin:one"), projection.group(DatastoreDomain.Plugins).resources.map { it.id })
        assertEquals(4, projection.group(DatastoreDomain.Junktor).itemCount)
        assertEquals(resources.size, projection.groups.sumOf { it.resources.size })
    }
}
