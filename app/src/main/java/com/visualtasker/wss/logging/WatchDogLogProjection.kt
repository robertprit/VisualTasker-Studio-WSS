package com.visualtasker.wss.logging

import com.visualtasker.wss.workspace.model.RecorderStepUi
import com.visualtasker.wss.workspace.model.StepStatus
import com.visualtasker.wss.workspace.model.watchDogSteps

data class WatchDogLogProjection(
    val level: StudioLogLevel,
    val source: String,
    val message: String,
    val details: String,
    val groupKey: String,
    val sourceTarget: StudioLogSourceTarget,
)

fun List<RecorderStepUi>.toWatchDogLogProjections(): List<WatchDogLogProjection> =
    watchDogSteps().map { step ->
        WatchDogLogProjection(
            level = when (step.status) {
                StepStatus.Invalid -> StudioLogLevel.ERROR
                StepStatus.Edited -> StudioLogLevel.WARNING
                StepStatus.Recorded,
                StepStatus.Executed,
                -> StudioLogLevel.INFO
            },
            source = "WATCHDOG",
            message = step.label,
            details = buildString {
                append("Typ=${step.actionType}")
                step.activityName?.let { append(" | Activity=$it") }
                step.properties["pluginOwner"]?.let { append(" | Plugin=$it") }
                step.detail?.takeIf { it.isNotBlank() }?.let { append("\n$it") }
            },
            groupKey = "watchdog:${step.id}:${step.status.name}:${step.detail.orEmpty()}",
            sourceTarget = StudioLogSourceTarget(
                railStepId = step.id,
                railSurface = StudioLogRailSurface.WATCHDOG,
                preferredSurface = StudioLogSourceSurface.RAILTRACE,
            ),
        )
    }
