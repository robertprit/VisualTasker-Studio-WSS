package com.visualtasker.ime.ime.vtbridge

interface VisionTaskerBridge {
    fun dispatch(action: String, payload: Map<String, String>): Boolean
}

class NoOpVisionTaskerBridge : VisionTaskerBridge {
    override fun dispatch(action: String, payload: Map<String, String>): Boolean = false
}
