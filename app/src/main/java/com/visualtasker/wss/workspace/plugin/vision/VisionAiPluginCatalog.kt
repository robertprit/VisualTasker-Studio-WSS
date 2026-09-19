package com.visualtasker.wss.workspace.plugin.vision

import com.visualtasker.vision.api.VisionCapability

enum class VisionAiProviderState {
    AVAILABLE,
    PLANNED,
}

data class VisionAiProviderRegistration(
    val id: String,
    val displayName: String,
    val state: VisionAiProviderState,
    val capabilities: Set<VisionCapability>,
)

object VisionAiPluginCatalog {
    val providers = listOf(
        VisionAiProviderRegistration(
            id = "yolo-litert",
            displayName = "YOLO / LiteRT",
            state = VisionAiProviderState.AVAILABLE,
            capabilities = setOf(
                VisionCapability.OBJECT_DETECTION,
                VisionCapability.CLASSIFICATION,
                VisionCapability.POSE,
                VisionCapability.SEGMENTATION,
                VisionCapability.DEPTH,
            ),
        ),
        VisionAiProviderRegistration("mlkit", "ML Kit", VisionAiProviderState.PLANNED, setOf(VisionCapability.OCR)),
        VisionAiProviderRegistration("opencv", "OpenCV", VisionAiProviderState.PLANNED, setOf(VisionCapability.OBJECT_DETECTION)),
        VisionAiProviderRegistration("gemma", "Gemma", VisionAiProviderState.PLANNED, setOf(VisionCapability.LANGUAGE_MODEL)),
        VisionAiProviderRegistration("rag", "RAG", VisionAiProviderState.PLANNED, setOf(VisionCapability.RETRIEVAL)),
    )
}
