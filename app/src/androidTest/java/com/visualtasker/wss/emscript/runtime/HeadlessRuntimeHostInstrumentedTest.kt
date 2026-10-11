package com.visualtasker.wss.emscript.runtime

import android.content.ComponentName
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.visualtasker.wss.emscript.parser.EmscriptWorkspaceImporter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HeadlessRuntimeHostInstrumentedTest {
    @Test
    fun processHostIsStableAcrossClientReattachment() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        assertSame(RuntimeProcessHost.get(context), RuntimeProcessHost.get(context))
    }

    @Test
    fun headlessServiceIsPrivateNonStickyAndDeclaresSpecialUse() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val component = ComponentName(context, HeadlessRuntimeService::class.java)
        val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getServiceInfo(component, PackageManager.ComponentInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getServiceInfo(component, 0)
        }

        assertFalse(info.exported)
        assertEquals(HeadlessRuntimeService::class.java.name, info.name)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            assertTrue(info.foregroundServiceType and ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE != 0)
        }
    }

    @Test
    fun foregroundWaitSurvivesClientReattachment() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val host = RuntimeProcessHost.get(context)
        stopActiveSession(host)
        val document = import("wait(750)", "m5-h-device-complete")
        val sessionId = requireNotNull(
            host.reserve(
                document = document,
                workflowId = "m5-h-device-complete",
                workspaceRevision = 51L,
            ),
        )

        ContextCompat.startForegroundService(context, HeadlessRuntimeService.startIntent(context, sessionId))
        val running = awaitSession(host) { it.sessionId == sessionId && it.state == ExecutionSessionState.RUNNING }
        val reattachedClient = RuntimeClient(context)

        assertEquals(sessionId, running.sessionId)
        assertEquals(sessionId, reattachedClient.session.value?.sessionId)
        val completed = awaitSession(host) { it.sessionId == sessionId && it.state == ExecutionSessionState.COMPLETED }
        assertEquals(ExecutionState.COMPLETED, completed.result?.state)
        assertTrue(completed.trace.any { it.kind == ExecutionTraceEventKind.PROVIDER_FINISHED })
    }

    @Test
    fun notificationStopIntentCancelsHeadlessWaitWithStableReason() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val host = RuntimeProcessHost.get(context)
        stopActiveSession(host)
        val document = import("wait(10000)", "m5-h-device-stop")
        val sessionId = requireNotNull(
            host.reserve(
                document = document,
                workflowId = "m5-h-device-stop",
                workspaceRevision = 52L,
            ),
        )

        ContextCompat.startForegroundService(context, HeadlessRuntimeService.startIntent(context, sessionId))
        awaitSession(host) { it.sessionId == sessionId && it.state == ExecutionSessionState.RUNNING }
        context.startService(
            HeadlessRuntimeService.stopIntent(
                context,
                sessionId,
                ExecutionCancellationReason.NOTIFICATION_STOP,
            ),
        )

        val cancelled = awaitSession(host) {
            it.sessionId == sessionId && it.state == ExecutionSessionState.CANCELLED
        }
        assertEquals(ExecutionCancellationReason.NOTIFICATION_STOP, cancelled.cancellationReason)
        assertEquals(ExecutionState.CANCELLED, cancelled.result?.state)
        assertNotNull(cancelled.endedAtEpochMs)
    }

    private fun import(source: String, workspaceId: String) =
        EmscriptWorkspaceImporter().import(source, workspaceId).let { result ->
            assertTrue(result.issues.joinToString { it.message }, result.isSuccess)
            requireNotNull(result.document)
        }

    private fun stopActiveSession(host: AndroidRuntimeHost) {
        host.currentSession.value?.takeIf(ExecutionSessionSnapshot::isActive)?.let { active ->
            host.stop(active.sessionId, ExecutionCancellationReason.USER_STOP)
            awaitSession(host) { it.sessionId == active.sessionId && !it.isActive }
        }
    }

    private fun awaitSession(
        host: AndroidRuntimeHost,
        predicate: (ExecutionSessionSnapshot) -> Boolean,
    ): ExecutionSessionSnapshot {
        repeat(500) {
            host.currentSession.value?.let { snapshot ->
                if (predicate(snapshot)) return snapshot
            }
            Thread.sleep(20)
        }
        error("Runtime session timed out at ${host.currentSession.value}")
    }
}
