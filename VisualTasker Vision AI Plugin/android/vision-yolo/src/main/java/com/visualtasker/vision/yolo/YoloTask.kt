package com.visualtasker.vision.yolo

enum class YoloTask(
    val label: String,
    val asset: String,
) {
    DETECT("Detect", "yolo26n_w8a32.tflite"),
    POSE("Pose", "yolo26n-pose_w8a32.tflite"),
    SEGMENT("Seg", "yolo26n-seg_w8a32.tflite"),
    SEMANTIC("Semantik", "yolo26n-sem_w8a32.tflite"),
    OBB("OBB", "yolo26n-obb_w8a32.tflite"),
    CLASSIFY("Klasse", "yolo26n-cls_w8a32.tflite"),
    DEPTH("Tiefe", "yolo26n-depth_w8a32.tflite"),
}
