package com.visualtasker.wss.accessibility

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccessibilityInspectorSnapshotTest {
    @Test
    fun formatsIdentityBoundsAndInteractionState() {
        val text = AccessibilityInspectorSnapshot(
            packageName = "com.example.demo",
            activityName = "com.example.demo.MainActivity",
            className = "android.widget.Button",
            text = "Login",
            contentDescription = "Anmelden",
            viewId = "com.example.demo:id/login",
            left = 10,
            top = 20,
            right = 210,
            bottom = 90,
            clickable = true,
            focusable = true,
            focused = false,
            enabled = true,
            visible = true,
            timestampMs = 42L,
        ).toInspectorText()

        assertTrue(text.contains("App: com.example.demo"))
        assertTrue(text.contains("Element: Button"))
        assertTrue(text.contains("Text: Login"))
        assertTrue(text.contains("Bounds: [10,20 - 210,90]"))
        assertTrue(text.contains("clickbar"))
        assertFalse(text.contains("fokussiert"))
    }

    @Test
    fun reportsPassiveNodeWhenNoInteractionFlagsAreSet() {
        val text = AccessibilityInspectorSnapshot(
            packageName = null,
            activityName = null,
            className = null,
            text = null,
            contentDescription = null,
            viewId = null,
            left = 0,
            top = 0,
            right = 0,
            bottom = 0,
            clickable = false,
            focusable = false,
            focused = false,
            enabled = false,
            visible = false,
            timestampMs = 0L,
        ).toInspectorText()

        assertTrue(text.contains("Status: passiv"))
    }
}
