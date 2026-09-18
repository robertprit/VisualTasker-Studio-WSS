package com.visualtasker.wss.workspace.model

enum class DatastoreDomain(val label: String) {
    Human("Human"),
    MachineVision("Machine Vision"),
    Runtime("Runtime"),
    Resources("Resources"),
    Plugins("Plugins"),
    Junktor("Junktor"),
}

data class DatastoreDomainGroup(
    val domain: DatastoreDomain,
    val resources: List<WorkspaceResource>,
    val suggestionCount: Int = 0,
) {
    val itemCount: Int
        get() = resources.size + suggestionCount
}

data class DatastoreDomainProjection(
    val groups: List<DatastoreDomainGroup>,
) {
    fun group(domain: DatastoreDomain): DatastoreDomainGroup =
        groups.single { it.domain == domain }
}

object DatastoreDomainProjector {
    fun project(
        resources: WorkspaceResourceBundle,
        junktorSuggestionCount: Int = 0,
    ): DatastoreDomainProjection {
        require(junktorSuggestionCount >= 0) { "Junktor suggestion count must be non-negative." }
        val classified = resources.resources.groupBy(::classify)
        return DatastoreDomainProjection(
            groups = DatastoreDomain.entries.map { domain ->
                DatastoreDomainGroup(
                    domain = domain,
                    resources = classified[domain]
                        .orEmpty()
                        .sortedWith(compareBy<WorkspaceResource> { it.kind.name }.thenBy { it.label }.thenBy { it.id }),
                    suggestionCount = junktorSuggestionCount.takeIf { domain == DatastoreDomain.Junktor } ?: 0,
                )
            },
        )
    }

    private fun classify(resource: WorkspaceResource): DatastoreDomain =
        when {
            "junktor" in resource.tags || resource.pluginOwner == "visualtasker.junktor" -> DatastoreDomain.Junktor
            "runtime" in resource.tags || resource.pluginOwner == "visualtasker.runtime" -> DatastoreDomain.Runtime
            "vision" in resource.tags || resource.pluginOwner == "visualtasker.vision" ||
                resource.tags.any { it in MACHINE_VISION_TAGS } -> DatastoreDomain.MachineVision
            resource.kind in HUMAN_RESOURCE_KINDS -> DatastoreDomain.Human
            resource.pluginOwner !in CORE_PLUGIN_OWNERS -> DatastoreDomain.Plugins
            else -> DatastoreDomain.Resources
        }
}

private val HUMAN_RESOURCE_KINDS = setOf(
    WorkspaceResourceKind.Marker,
    WorkspaceResourceKind.Region,
    WorkspaceResourceKind.Template,
    WorkspaceResourceKind.Screenshot,
)

private val MACHINE_VISION_TAGS = setOf("a11y", "ocr", "ocv", "opencv", "yolo", "dom")

private val CORE_PLUGIN_OWNERS = setOf(
    "visualtasker.core",
    "visualtasker.canvas",
    "visualtasker.recorder",
    "visualtasker.runtime",
    "visualtasker.vision",
    "visualtasker.junktor",
)
