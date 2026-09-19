package com.visualtasker.vision.api

enum class VisionCapability {
    OCR,
    OBJECT_DETECTION,
    CLASSIFICATION,
    POSE,
    SEGMENTATION,
    DEPTH,
    EMBEDDING,
    LANGUAGE_MODEL,
    RETRIEVAL,
}

data class VisionProviderDescriptor(
    val id: String,
    val displayName: String,
    val capabilities: Set<VisionCapability>,
    val localOnly: Boolean,
)

interface VisionProvider<Input : Any, Output : Any> : AutoCloseable {
    val descriptor: VisionProviderDescriptor
    suspend fun process(input: Input): Output
    override fun close() = Unit
}
