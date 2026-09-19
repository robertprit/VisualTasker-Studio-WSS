package com.visualtasker.vision.yolo

import android.graphics.Bitmap
import android.graphics.PointF
import android.graphics.RectF

data class Keypoint(
    val x: Float,
    val y: Float,
    val confidence: Float,
)

data class Detection(
    val classId: Int,
    val label: String,
    val confidence: Float,
    val box: RectF,
    val keypoints: List<Keypoint> = emptyList(),
    val polygon: List<PointF> = emptyList(),
)

data class ClassScore(
    val label: String,
    val score: Float,
)

data class FrameResult(
    val task: YoloTask,
    val imageWidth: Int,
    val imageHeight: Int,
    val detections: List<Detection> = emptyList(),
    val overlay: Bitmap? = null,
    val topClasses: List<ClassScore> = emptyList(),
    val summary: String = "",
)
