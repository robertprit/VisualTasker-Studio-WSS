package com.visualtasker.wss.targeting

import com.visualtasker.wss.recording.CandidateTarget
import com.visualtasker.wss.recording.toElementQuery
import com.visualtasker.wss.recording.toElementRef
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AccessibilityTargetResolverTest {
    @Test
    fun resolvesUniqueTextViewIdAndContentDescription() {
        val target = node(
            id = "node-0.0",
            text = "Login",
            description = "Submit login",
            viewId = "app:id/login",
            clickable = true,
        )
        val snapshot = snapshot(target)

        listOf(
            ElementQuery(text = "login", displayId = 0),
            ElementQuery(viewId = "app:id/login", displayId = 0),
            ElementQuery(contentDescription = "Submit login", displayId = 0),
        ).forEach { query ->
            val result = AccessibilityTargetResolver.resolve(snapshot, query, policy())
            assertEquals(TargetResolutionStatus.FOUND, result.status)
            assertEquals(target.ref, result.match?.actionTarget)
        }
    }

    @Test
    fun reportsNotFoundAndAmbiguousWithoutSelectingFirstCandidate() {
        val first = node("node-0.0", text = "Login", clickable = true)
        val second = node("node-0.1", text = "Login", clickable = true, order = 1)
        val snapshot = snapshot(first, second)

        assertEquals(
            TargetResolutionStatus.NOT_FOUND,
            AccessibilityTargetResolver.resolve(
                snapshot,
                ElementQuery(text = "Missing", displayId = 0),
                policy(),
            ).status,
        )
        val ambiguous = AccessibilityTargetResolver.resolve(
            snapshot,
            ElementQuery(text = "Login", displayId = 0),
            policy(),
        )
        assertEquals(TargetResolutionStatus.AMBIGUOUS, ambiguous.status)
        assertEquals(listOf(first.ref, second.ref), ambiguous.candidateRefs)
    }

    @Test
    fun disabledElementIsExcludedAndNonClickableChildUsesClickableParent() {
        val disabled = node("node-0.0", text = "Disabled", clickable = true, enabled = false)
        val child = node(
            id = "node-0.1.0",
            parentId = "node-0.1",
            text = "Child label",
            clickable = false,
        )
        val parent = node("node-0.1", clickable = true, order = 1, children = listOf(child))
        val snapshot = snapshot(disabled, parent)

        assertEquals(
            TargetResolutionStatus.NOT_FOUND,
            AccessibilityTargetResolver.resolve(
                snapshot,
                ElementQuery(text = "Disabled", displayId = 0),
                policy(),
            ).status,
        )
        val result = AccessibilityTargetResolver.resolve(
            snapshot,
            ElementQuery(text = "Child label", displayId = 0),
            policy(),
        )
        assertEquals(TargetResolutionStatus.FOUND, result.status)
        assertEquals(parent.ref, result.match?.actionTarget)
        assertTrue(result.match?.usedClickableAncestor == true)
    }

    @Test
    fun nonActionableAndWrongDisplayAreExplicit() {
        val passive = node("node-0.0", text = "Passive")
        val snapshot = snapshot(passive)

        assertEquals(
            TargetResolutionStatus.NOT_ACTIONABLE,
            AccessibilityTargetResolver.resolve(
                snapshot,
                ElementQuery(text = "Passive", displayId = 0),
                policy(allowAncestor = false),
            ).status,
        )
        assertEquals(
            TargetResolutionStatus.DISPLAY_UNSUPPORTED,
            AccessibilityTargetResolver.resolve(
                snapshot,
                ElementQuery(text = "Passive", displayId = 7),
                TargetResolutionPolicy(displayId = 7),
            ).status,
        )
    }

    @Test
    fun resolverIsDeterministicAcrossRepeatedRuns() {
        val snapshot = snapshot(
            node("node-0.0", text = "Same", clickable = true),
            node("node-0.1", text = "Same", clickable = true, order = 1),
        )
        val query = ElementQuery(text = "Same", displayId = 0)

        val fingerprints = List(20) {
            AccessibilityTargetResolver.resolve(snapshot, query, policy()).candidateRefs
                .map(ElementRef::snapshotNodeId)
        }

        assertEquals(1, fingerprints.distinct().size)
    }

    @Test
    fun recorderCandidateUsesSharedBoundsAndTargetContracts() {
        val candidate = CandidateTarget.A11y(
            nodeId = "node-0.2",
            resourceId = "app:id/login",
            text = "Login",
            contentDescription = null,
            className = "android.widget.Button",
            left = 10,
            top = 20,
            right = 110,
            bottom = 70,
            clickable = true,
            visible = true,
            enabled = true,
            packageName = "app",
            windowId = "window",
            xPx = 60,
            yPx = 45,
        )

        assertEquals(candidate.toElementRef(4).bounds, candidate.toElementQuery(4).bounds)
        assertEquals(4, candidate.toElementRef(4).displayId)
        assertEquals("app:id/login", candidate.toElementQuery(4).viewId)
    }

    private fun snapshot(vararg children: ObservedAccessibilityElement): AccessibilityTreeSnapshot {
        val root = node("node-0", clickable = false, children = children.toList())
        return AccessibilityTreeSnapshot("snapshot", 1L, 0, "window", "app", root)
    }

    private fun policy(allowAncestor: Boolean = true) = TargetResolutionPolicy(
        displayId = 0,
        allowClickableAncestor = allowAncestor,
    )

    private fun node(
        id: String,
        parentId: String? = "node-0",
        text: String? = null,
        description: String? = null,
        viewId: String? = null,
        clickable: Boolean = false,
        enabled: Boolean = true,
        visible: Boolean = true,
        order: Int = 0,
        children: List<ObservedAccessibilityElement> = emptyList(),
    ) = ObservedAccessibilityElement(
        ref = ElementRef(
            snapshotNodeId = id,
            packageName = "app",
            className = "android.view.View",
            viewId = viewId,
            bounds = ElementBoundsPx(10 + order * 100, 20, 90 + order * 100, 80),
            windowId = "window",
            displayId = 0,
        ),
        parentNodeId = if (id == "node-0") null else parentId,
        childOrder = order,
        text = text,
        contentDescription = description,
        clickable = clickable,
        enabled = enabled,
        visible = visible,
        children = children,
    )
}
