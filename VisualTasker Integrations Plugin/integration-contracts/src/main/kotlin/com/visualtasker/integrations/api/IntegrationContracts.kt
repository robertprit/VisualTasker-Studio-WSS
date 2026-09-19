package com.visualtasker.integrations.api

enum class IntegrationId(val wireId: String) {
    TASKER("tasker"),
    TERMUX("termux"),
    JOIN("join"),
    CUSTOM_TABS("custom-tabs"),
    SCRCPY("scrcpy"),
    KTOR_SERVER("ktor-server"),
    VT2VT("vt2vt"),
    SHERPA("sherpa"),
}

enum class IntegrationDeployment {
    EMBEDDED_RUNTIME,
    COMPANION_APP,
    EXTERNAL_SERVICE,
    REMOTE_PEER,
}

data class IntegrationCapability(
    val id: String,
    val version: Int = 1,
) {
    init {
        require(id.isNotBlank() && id == id.trim())
        require(version > 0)
    }
}

data class IntegrationDescriptor(
    val id: IntegrationId,
    val displayName: String,
    val deployment: IntegrationDeployment,
    val capabilities: Set<IntegrationCapability>,
    val companionPackageName: String? = null,
) {
    init {
        require(displayName.isNotBlank() && displayName == displayName.trim())
        require(companionPackageName == null || companionPackageName.isNotBlank())
    }
}

object VisualTaskerIntegrationCatalog {
    val descriptors: List<IntegrationDescriptor> = listOf(
        IntegrationDescriptor(IntegrationId.TASKER, "Tasker", IntegrationDeployment.COMPANION_APP, setOf(
            IntegrationCapability("tasker.action"),
            IntegrationCapability("tasker.condition"),
            IntegrationCapability("tasker.event"),
        ), "net.dinglisch.android.taskerm"),
        IntegrationDescriptor(IntegrationId.TERMUX, "Termux", IntegrationDeployment.COMPANION_APP, setOf(
            IntegrationCapability("termux.command"),
        ), "com.termux"),
        IntegrationDescriptor(IntegrationId.JOIN, "Join", IntegrationDeployment.COMPANION_APP, setOf(
            IntegrationCapability("join.message"),
            IntegrationCapability("join.device"),
        )),
        IntegrationDescriptor(IntegrationId.CUSTOM_TABS, "Custom Tabs", IntegrationDeployment.EMBEDDED_RUNTIME, setOf(
            IntegrationCapability("browser.custom-tab"),
        )),
        IntegrationDescriptor(IntegrationId.SCRCPY, "scrcpy", IntegrationDeployment.EXTERNAL_SERVICE, setOf(
            IntegrationCapability("scrcpy.video"),
            IntegrationCapability("scrcpy.control"),
        )),
        IntegrationDescriptor(IntegrationId.KTOR_SERVER, "Ktor Server", IntegrationDeployment.EMBEDDED_RUNTIME, setOf(
            IntegrationCapability("ktor.server"),
        )),
        IntegrationDescriptor(IntegrationId.VT2VT, "VT2VT", IntegrationDeployment.REMOTE_PEER, setOf(
            IntegrationCapability("vt2vt.workspace"),
            IntegrationCapability("vt2vt.runtime-trace"),
        )),
        IntegrationDescriptor(IntegrationId.SHERPA, "Sherpa", IntegrationDeployment.EMBEDDED_RUNTIME, setOf(
            IntegrationCapability("sherpa.speech"),
        )),
    )

    fun descriptor(id: IntegrationId): IntegrationDescriptor =
        descriptors.first { it.id == id }
}
