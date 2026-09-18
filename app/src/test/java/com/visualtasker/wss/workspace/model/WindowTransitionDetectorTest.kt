package com.visualtasker.wss.workspace.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WindowTransitionDetectorTest {
    @Test
    fun reducerEmitsBaselineThenSuppressesDuplicateAndEmitsChange() {
        val reducer = WindowRecordingEvidenceReducer()
        val baseline = context(activity = "LoginActivity", windows = listOf(snapshot("main", active = true)))
        val duplicate = context(activity = "LoginActivity", windows = listOf(snapshot("main", active = true)))
        val changed = context(activity = "HomeActivity", windows = listOf(snapshot("main", active = true)))

        val baselineEvidence = reducer.accept(baseline)
        val duplicateEvidence = reducer.accept(duplicate)
        val transitionEvidence = reducer.accept(changed)

        assertEquals("window.baseline", baselineEvidence?.kind)
        assertEquals("window.baseline", baselineEvidence?.attributes?.get("recording.evidence"))
        assertEquals(null, duplicateEvidence)
        assertEquals("window.transition", transitionEvidence?.kind)
        assertEquals("1", transitionEvidence?.attributes?.get("window.transitionCount"))
        assertEquals("ACTIVITY_CHANGED", transitionEvidence?.attributes?.get("window.transitionKinds"))
    }

    @Test
    fun sameStateRepeatedProducesNoTransitions() {
        val before = context(activity = "MainActivity", windows = listOf(snapshot("main", focused = true)))
        val after = context(activity = "MainActivity", windows = listOf(snapshot("main", focused = true)))

        val transitions = WindowTransitionDetector.detect(before, after)

        assertTrue(transitions.isEmpty())
    }

    @Test
    fun firstObservedStateIsBaselineOnly() {
        val current = context(activity = "MainActivity", windows = listOf(snapshot("main", active = true)))

        val transitions = WindowTransitionDetector.detect(previous = null, current = current)

        assertTrue(transitions.isEmpty())
    }

    @Test
    fun detectsWindowAppeared() {
        val before = context(activity = "MainActivity", windows = listOf(snapshot("main")))
        val after = context(activity = "MainActivity", windows = listOf(snapshot("main"), snapshot("dialog")))

        val transitions = WindowTransitionDetector.detect(before, after)

        assertEquals(listOf(WindowTransitionKind.WINDOW_APPEARED), transitions.map { it.kind })
        assertEquals("dialog", transitions.single().current?.id)
    }

    @Test
    fun detectsWindowDisappeared() {
        val before = context(activity = "MainActivity", windows = listOf(snapshot("main"), snapshot("dialog")))
        val after = context(activity = "MainActivity", windows = listOf(snapshot("main")))

        val transitions = WindowTransitionDetector.detect(before, after)

        assertEquals(listOf(WindowTransitionKind.WINDOW_DISAPPEARED), transitions.map { it.kind })
        assertEquals("dialog", transitions.single().previous?.id)
    }

    @Test
    fun detectsActivityChanged() {
        val before = context(activity = "LoginActivity", windows = listOf(snapshot("main")))
        val after = context(activity = "HomeActivity", windows = listOf(snapshot("main")))

        val transitions = WindowTransitionDetector.detect(before, after)

        assertEquals(listOf(WindowTransitionKind.ACTIVITY_CHANGED), transitions.map { it.kind })
        assertEquals("LoginActivity", transitions.single().properties["previousActivity"])
        assertEquals("HomeActivity", transitions.single().properties["currentActivity"])
    }

    @Test
    fun unknownCurrentActivityDoesNotCreateSamePackageActivityTransition() {
        val before = context(activity = "MainActivity", windows = listOf(snapshot("main", focused = true)))
        val after = context(activity = null, windows = listOf(snapshot("main", focused = true)))

        val transitions = WindowTransitionDetector.detect(before, after)

        assertTrue(transitions.isEmpty())
    }

    @Test
    fun packageChangeWithoutActivityIsLowConfidenceContextTransition() {
        val before = context(activity = "MainActivity", packageName = "com.example.old", windows = listOf(snapshot("main")))
        val after = context(activity = null, packageName = "com.example.new", windows = listOf(snapshot("main")))

        val transitions = WindowTransitionDetector.detect(before, after)

        assertEquals(listOf(WindowTransitionKind.ACTIVITY_CHANGED), transitions.map { it.kind })
        assertEquals("package-context-changed", transitions.single().reason)
        assertEquals(0.6f, transitions.single().confidence)
    }

    @Test
    fun knownActivityAfterUnknownSamePackageDoesNotCreateTransition() {
        val before = context(activity = null, windows = listOf(snapshot("main", active = true)))
        val after = context(activity = "MainActivity", windows = listOf(snapshot("main", active = true)))

        val transitions = WindowTransitionDetector.detect(before, after)

        assertTrue(transitions.isEmpty())
    }

    @Test
    fun detectsFocusChanged() {
        val before = context(
            activity = "MainActivity",
            windows = listOf(snapshot("main", focused = true), snapshot("dialog", focused = false)),
        )
        val after = context(
            activity = "MainActivity",
            windows = listOf(snapshot("main", focused = false), snapshot("dialog", focused = true)),
        )

        val transitions = WindowTransitionDetector.detect(before, after)

        assertEquals(
            listOf(WindowTransitionKind.FOCUS_CHANGED, WindowTransitionKind.FOCUS_CHANGED),
            transitions.map { it.kind },
        )
    }

    @Test
    fun multipleChangesAreReportedInDeterministicOrder() {
        val before = context(activity = "ActivityA", windows = listOf(snapshot("main"), snapshot("dialog")))
        val after = context(activity = "ActivityB", windows = listOf(snapshot("main"), snapshot("sheet")))

        val transitions = WindowTransitionDetector.detect(before, after)

        assertEquals(
            listOf(
                WindowTransitionKind.ACTIVITY_CHANGED,
                WindowTransitionKind.WINDOW_APPEARED,
                WindowTransitionKind.WINDOW_DISAPPEARED,
            ),
            transitions.map { it.kind },
        )
        assertEquals("sheet", transitions[1].current?.id)
        assertEquals("dialog", transitions[2].previous?.id)
    }

    private fun context(
        activity: String?,
        packageName: String = "com.example",
        windows: List<WindowSnapshot>,
        timestampMs: Long = 1_000L,
    ): WindowContext =
        WindowContext(
            packageName = packageName,
            activityName = activity,
            windows = windows,
            timestampMs = timestampMs,
        )

    private fun snapshot(
        id: String,
        focused: Boolean = false,
        active: Boolean = false,
    ): WindowSnapshot =
        WindowSnapshot(
            id = id,
            packageName = "com.example",
            activityName = "MainActivity",
            focused = focused,
            active = active,
            timestampMs = 1_000L,
        )
}
