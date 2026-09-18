package com.visualtasker.wss.logging

import com.visualtasker.wss.workspace.model.RecorderStepUi
import com.visualtasker.wss.workspace.model.StepStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class WatchDogLogProjectionTest {
    @Test
    fun watchDogEventsBecomeDedicatedNavigableLogEntries() {
        val entries = listOf(
            RecorderStepUi(
                id = "watchdog-1",
                label = "Tasker event",
                actionType = "tasker.event",
                status = StepStatus.Recorded,
                activityName = "DashboardActivity",
                detail = "profile=Night",
                properties = mapOf("pluginOwner" to "tasker"),
            ),
            RecorderStepUi(
                id = "workflow-1",
                label = "Wait",
                actionType = "wait",
                status = StepStatus.Executed,
            ),
        ).toWatchDogLogProjections()

        assertEquals(1, entries.size)
        assertEquals("WATCHDOG", entries.single().source)
        assertEquals(StudioLogLevel.INFO, entries.single().level)
        assertEquals("watchdog-1", entries.single().sourceTarget.railStepId)
        assertEquals(StudioLogRailSurface.WATCHDOG, entries.single().sourceTarget.railSurface)
        assertEquals(StudioLogSourceSurface.RAILTRACE, entries.single().sourceTarget.preferredSurface)
    }

    @Test
    fun invalidProviderEventBecomesWatchDogError() {
        val entry = listOf(
            RecorderStepUi(
                id = "watchdog-error",
                label = "Provider failed",
                actionType = "watchdog.provider",
                status = StepStatus.Invalid,
            )
        ).toWatchDogLogProjections().single()

        assertEquals(StudioLogLevel.ERROR, entry.level)
    }
}
