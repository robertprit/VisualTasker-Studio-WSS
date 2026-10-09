package com.visualtasker.wss.emscript.runtime

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.visualtasker.wss.emscript.parser.EmscriptWorkspaceImporter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AndroidRuntimeBridgeInstrumentedTest {
    @Test
    fun nativeWaitCompletesOnAndroidWorkerDispatcher() {
        val fixture = fixture("wait(30)")

        assertTrue(fixture.bridge.start(fixture.document, fixture.document.id))

        assertEquals(ExecutionState.COMPLETED, fixture.awaitTerminal().state)
        fixture.close()
    }

    @Test
    fun activeNativeWaitCanBeCancelledOnAndroid() {
        val fixture = fixture("wait(2000)")
        assertTrue(fixture.bridge.start(fixture.document, fixture.document.id))
        fixture.awaitRunning()

        assertTrue(fixture.bridge.stop())

        assertEquals(ExecutionState.CANCELLED, fixture.awaitTerminal().state)
        fixture.close()
    }

    private fun fixture(script: String): Fixture {
        val imported = EmscriptWorkspaceImporter().import(script, "m5-1-device")
        require(imported.isSuccess) { imported.issues.joinToString { it.message } }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        return Fixture(
            scope = scope,
            document = requireNotNull(imported.document),
            bridge = AndroidRuntimeBridge(scope),
        )
    }

    private data class Fixture(
        val scope: CoroutineScope,
        val document: de.visualtasker.workflow.core.WorkspaceDocument,
        val bridge: AndroidRuntimeBridge,
    ) {
        fun awaitRunning(): AndroidRuntimeSnapshot = await { it.state == ExecutionState.RUNNING }

        fun awaitTerminal(): AndroidRuntimeSnapshot = await {
            it.state in setOf(
                ExecutionState.COMPLETED,
                ExecutionState.FAILED,
                ExecutionState.CANCELLED,
                ExecutionState.REJECTED,
            )
        }

        fun close() {
            bridge.close()
            scope.cancel()
        }

        private fun await(predicate: (AndroidRuntimeSnapshot) -> Boolean): AndroidRuntimeSnapshot {
            repeat(500) {
                bridge.state.value.let { snapshot -> if (predicate(snapshot)) return snapshot }
                Thread.sleep(10)
            }
            error("Android runtime bridge timed out at ${bridge.state.value.state}")
        }
    }
}
