package com.visualtasker.wss.workspace.ui

import com.visualtasker.wss.workspace.model.RailMode
import com.visualtasker.wss.workspace.model.RailScaleMode
import org.junit.Assert.assertEquals
import org.junit.Test

class StepperPanelStateTest {
    @Test
    fun encodeDecodeKeepsRailTraceSettingsAndReplayPosition() {
        val state = StepperPanelState(
            selectedStepId = "step-42",
            replayIndex = 7,
            replayPositionMs = 12_345L,
            speed = 2f,
            railMode = RailMode.Curate,
            scaleMode = RailScaleMode.Logical,
            timelineZoom = 3.25f,
        )

        assertEquals(state, StepperPanelState.decode(state.encode()))
    }

    @Test
    fun decodeFallsBackForInvalidPayload() {
        assertEquals(StepperPanelState(), StepperPanelState.decode("not-json"))
        assertEquals(StepperPanelState(), StepperPanelState.decode(null))
    }

    @Test
    fun decodeClampsPersistedNumericValues() {
        val decoded = StepperPanelState.decode(
            """{"replayIndex":-5,"replayPositionMs":-7,"speed":99,"timelineZoom":0.1}""",
        )

        assertEquals(0, decoded.replayIndex)
        assertEquals(0L, decoded.replayPositionMs)
        assertEquals(4f, decoded.speed)
        assertEquals(0.5f, decoded.timelineZoom)
    }
}
