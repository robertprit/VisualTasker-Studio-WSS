package com.visualtasker.wss.recording

import com.visualtasker.wss.targeting.ElementBoundsPx
import com.visualtasker.wss.targeting.ElementQuery
import com.visualtasker.wss.targeting.ElementRef

fun CandidateTarget.A11y.toElementRef(displayId: Int): ElementRef = ElementRef(
    snapshotNodeId = nodeId,
    packageName = packageName,
    className = className,
    viewId = resourceId,
    bounds = ElementBoundsPx(left, top, right, bottom),
    windowId = windowId,
    displayId = displayId,
)

fun CandidateTarget.A11y.toElementQuery(displayId: Int): ElementQuery = ElementQuery(
    text = text,
    contentDescription = contentDescription,
    viewId = resourceId,
    className = className,
    packageName = packageName,
    bounds = ElementBoundsPx(left, top, right, bottom),
    enabled = enabled,
    visible = visible,
    displayId = displayId,
)
