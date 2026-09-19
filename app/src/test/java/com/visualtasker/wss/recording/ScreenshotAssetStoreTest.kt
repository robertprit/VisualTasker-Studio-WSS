package com.visualtasker.wss.recording

import com.visualtasker.wss.recording.persistence.ScreenshotAssetStore
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenshotAssetStoreTest {
    @Test
    fun deduplicatesIdenticalPngBytes() {
        val root = Files.createTempDirectory("recorder-assets").toFile()
        val store = ScreenshotAssetStore(root) { 42L }
        val first = store.putPng(PNG_A)
        val second = store.putPng(PNG_A)

        assertTrue(first.created)
        assertFalse(second.created)
        assertEquals(first.asset.assetHash, second.asset.assetHash)
        assertEquals(1, root.walkTopDown().count { it.isFile })
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsNonPngPayload() {
        ScreenshotAssetStore(Files.createTempDirectory("recorder-assets").toFile()).putPng(byteArrayOf(1, 2, 3))
    }

    companion object {
        val PNG_A = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 1)
        val PNG_B = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 2)
    }
}
