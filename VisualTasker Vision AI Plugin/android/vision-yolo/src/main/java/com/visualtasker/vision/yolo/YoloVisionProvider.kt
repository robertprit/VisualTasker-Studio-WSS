package com.visualtasker.vision.yolo

import android.content.Context
import android.graphics.Bitmap
import com.visualtasker.vision.api.VisionCapability
import com.visualtasker.vision.api.VisionProvider
import com.visualtasker.vision.api.VisionProviderDescriptor

class YoloVisionProvider(
    context: Context,
    task: YoloTask,
    confidenceThreshold: Float = 0.2f,
    iouThreshold: Float = 0.45f,
) : VisionProvider<Bitmap, FrameResult> {
    private val predictor = YoloPredictor(
        context = context.applicationContext,
        task = task,
        confThreshold = confidenceThreshold,
        iouThreshold = iouThreshold,
    )

    override val descriptor = VisionProviderDescriptor(
        id = "yolo.${task.name.lowercase()}",
        displayName = "YOLO ${task.label}",
        capabilities = setOf(task.toCapability()),
        localOnly = true,
    )

    override suspend fun process(input: Bitmap): FrameResult = predictor.predict(input)

    override fun close() = predictor.close()
}

private fun YoloTask.toCapability(): VisionCapability = when (this) {
    YoloTask.DETECT, YoloTask.OBB -> VisionCapability.OBJECT_DETECTION
    YoloTask.POSE -> VisionCapability.POSE
    YoloTask.SEGMENT, YoloTask.SEMANTIC -> VisionCapability.SEGMENTATION
    YoloTask.CLASSIFY -> VisionCapability.CLASSIFICATION
    YoloTask.DEPTH -> VisionCapability.DEPTH
}
