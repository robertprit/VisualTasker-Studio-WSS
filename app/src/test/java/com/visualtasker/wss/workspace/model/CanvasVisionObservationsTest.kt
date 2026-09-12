package com.visualtasker.wss.workspace.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CanvasVisionObservationsTest {
    @Test
    fun createsOcrObservationForVisionTextMatch() {
        val observation = CanvasVisionObservationFactory.create(
            idSeed = "screen:login button",
            label = "Login",
            matchKind = "OCR",
            processingMode = "Grayscale",
            bounds = WorldviewRect(0.1f, 0.2f, 0.4f, 0.5f),
            score = 0.91f,
            threshold = 0.8f,
            assetId = "screenshot:login",
            referenceId = "template:login",
            observedAtEpochMs = 42L,
        )

        assertEquals("observation:vision:screen:login-button", observation.id)
        assertEquals(ObservationProvider.Ocr, observation.provider)
        assertEquals(ObservationKind.Text, observation.kind)
        assertEquals(0.91f, observation.confidence)
        assertEquals("Login", observation.properties["text"])
        assertEquals("Grayscale", observation.properties["processingMode"])
        assertEquals("template:login", observation.properties["referenceId"])
    }

    @Test
    fun createsOcvObservationForTemplateMatchAndClampsScore() {
        val observation = CanvasVisionObservationFactory.create(
            idSeed = "Template Find",
            label = "Catalog",
            matchKind = "OCV",
            processingMode = "Edge",
            bounds = WorldviewRect(0.2f, 0.3f, 0.7f, 0.8f),
            score = 1.3f,
            threshold = 2f,
            assetId = null,
            referenceId = null,
            observedAtEpochMs = 7L,
        )

        assertEquals("observation:vision:template-find", observation.id)
        assertEquals(ObservationProvider.OpenCv, observation.provider)
        assertEquals(ObservationKind.TemplateMatch, observation.kind)
        assertEquals(1.0f, observation.confidence)
        assertEquals("1.0", observation.properties["score"])
        assertEquals("1.0", observation.properties["threshold"])
        assertTrue(observation.properties.keys.none { it.isBlank() })
    }
}
