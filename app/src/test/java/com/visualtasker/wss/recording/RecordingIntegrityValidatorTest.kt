package com.visualtasker.wss.recording

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecordingIntegrityValidatorTest {
    @Test
    fun completeRecordIsValidAndTraversable() {
        val fixture = fixture()

        val report = RecordingIntegrityValidator.validate(fixture.record, fixture.snapshot)

        assertTrue(report.isValid)
        assertTrue(report.issues.isEmpty())
        val step = report.steps.single()
        assertTrue(step.isValid)
        assertEquals("interaction-1", step.provenance.interactionId)
        assertEquals(listOf("raw-1"), step.provenance.rawEventIds)
        assertEquals("evidence-1", step.provenance.evidence.single().evidence.evidenceId)
    }

    @Test
    fun missingRawEventIsReported() {
        val fixture = fixture()

        val report = RecordingIntegrityValidator.validate(fixture.record.copy(rawEvents = emptyList()), fixture.snapshot)

        assertIssue<RecordingIntegrityIssue.MissingRawEvent>(report, "MISSING_RAW_EVENT")
    }

    @Test
    fun missingInteractionEvidenceIsReported() {
        val fixture = fixture()
        val interaction = fixture.record.interactions.single().copy(evidenceRefs = listOf("evidence-missing"))

        val report = RecordingIntegrityValidator.validate(
            fixture.record.copy(interactions = listOf(interaction)),
            fixture.snapshot,
        )

        assertIssue<RecordingIntegrityIssue.MissingEvidence>(report, "MISSING_EVIDENCE")
    }

    @Test
    fun missingResourceIsReported() {
        val fixture = fixture()
        val missing = fixture.snapshot.evidence.single().copy(
            resources = listOf(RecordingResourceRef(RecordingResourceKind.SCREENSHOT_ASSET, "asset-missing")),
        )

        val report = RecordingIntegrityValidator.validate(
            fixture.record,
            fixture.snapshot.copy(evidence = listOf(missing)),
        )

        assertIssue<RecordingIntegrityIssue.MissingResource>(report, "MISSING_RESOURCE")
    }

    @Test
    fun brokenStepEvidenceReferenceIsReported() {
        val fixture = fixture()
        val brokenStep = fixture.snapshot.steps.single().copy(evidenceIds = listOf("evidence-missing"))

        val report = RecordingIntegrityValidator.validate(
            fixture.record,
            fixture.snapshot.copy(steps = listOf(brokenStep)),
        )

        assertIssue<RecordingIntegrityIssue.BrokenStepEvidenceRef>(report, "BROKEN_STEP_EVIDENCE_REF")
    }

    @Test
    fun brokenStepInteractionReferenceIsReported() {
        val fixture = fixture()
        val brokenStep = fixture.snapshot.steps.single().copy(interactionId = "interaction-missing")

        val report = RecordingIntegrityValidator.validate(
            fixture.record,
            fixture.snapshot.copy(steps = listOf(brokenStep)),
        )

        assertIssue<RecordingIntegrityIssue.BrokenStepInteractionRef>(report, "BROKEN_STEP_INTERACTION_REF")
    }

    @Test
    fun orphanEvidenceAndInteractionAreReportedAsWarnings() {
        val fixture = fixture()
        val orphanEvidence = fixture.snapshot.evidence.single().copy(evidenceId = "evidence-orphan")
        val orphanInteraction = fixture.record.interactions.single().copy(
            interactionId = "interaction-orphan",
            sequence = 2L,
            evidenceRefs = emptyList(),
        )

        val report = RecordingIntegrityValidator.validate(
            fixture.record.copy(interactions = fixture.record.interactions + orphanInteraction),
            fixture.snapshot.copy(evidence = fixture.snapshot.evidence + orphanEvidence),
        )

        assertTrue(report.issues.any { it is RecordingIntegrityIssue.OrphanEvidence && it.code == "ORPHAN_EVIDENCE" })
        assertTrue(report.issues.any { it is RecordingIntegrityIssue.OrphanInteraction && it.code == "ORPHAN_INTERACTION" })
        assertTrue(report.isValid)
    }

    @Test
    fun validationDoesNotMutateInput() {
        val fixture = fixture()
        val recordBefore = fixture.record.copy()
        val snapshotBefore = fixture.snapshot.copy()

        RecordingIntegrityValidator.validate(fixture.record, fixture.snapshot)

        assertEquals(recordBefore, fixture.record)
        assertEquals(snapshotBefore, fixture.snapshot)
    }

    private inline fun <reified T : RecordingIntegrityIssue> assertIssue(
        report: RecordingIntegrityReport,
        code: String,
    ) {
        assertFalse(report.isValid)
        assertTrue(report.issues.any { it is T && it.code == code })
    }

    private fun fixture(): IntegrityFixture {
        val session = RecordingSession(
            sessionId = "recording-1",
            status = RecordingSessionStatus.COMPLETED,
            startedAtEpochMs = 1_000L,
            startedAtElapsedRealtimeNanos = 1_000_000_000L,
            stoppedAtEpochMs = 1_100L,
            createdBy = "test",
            recordingPolicySnapshot = RecordingPolicySnapshot(),
        )
        val rawEvent = RawRecordingEvent(
            rawEventId = "raw-1",
            sessionId = session.sessionId,
            sequence = 1L,
            occurredAtEpochMs = 1_050L,
            occurredAtElapsedRealtimeNanos = 1_050_000_000L,
            kind = "tap",
            payload = mapOf("x" to "10", "y" to "20"),
        )
        val evidence = RecordingEvidenceRef(
            evidenceId = "evidence-1",
            sessionId = session.sessionId,
            stepId = "step-1",
            sequence = 1L,
            occurredAtEpochMs = rawEvent.occurredAtEpochMs,
            occurredAtElapsedRealtimeNanos = rawEvent.occurredAtElapsedRealtimeNanos,
            kind = RecordingEvidenceKind.INPUT,
            resources = listOf(RecordingResourceRef(RecordingResourceKind.RAW_EVENT, rawEvent.rawEventId)),
        )
        val interaction = RecordingInteraction(
            interactionId = "interaction-1",
            sessionId = session.sessionId,
            sequence = 1L,
            occurredAtEpochMs = rawEvent.occurredAtEpochMs,
            occurredAtElapsedRealtimeNanos = rawEvent.occurredAtElapsedRealtimeNanos,
            source = RecordingInteractionSource.RECORDER,
            rawEventIds = listOf(rawEvent.rawEventId),
            evidenceRefs = listOf(evidence.evidenceId),
            status = RecordingInteractionStatus.UNCHANGED,
            payload = RecordingInteractionPayload.Tap(RecordingPoint(10, 20)),
        )
        return IntegrityFixture(
            record = PersistedRecordingSession(
                session = session,
                scenes = emptyList(),
                frames = emptyList(),
                assets = emptyList(),
                snapshots = emptyList(),
                rawEvents = listOf(rawEvent),
                interactions = listOf(interaction),
            ),
            snapshot = RecordingIntegritySnapshot(
                steps = listOf(RecordingStepEvidenceRef("step-1", interaction.interactionId, listOf(evidence.evidenceId))),
                evidence = listOf(evidence),
            ),
        )
    }
}

private data class IntegrityFixture(
    val record: PersistedRecordingSession,
    val snapshot: RecordingIntegritySnapshot,
)
