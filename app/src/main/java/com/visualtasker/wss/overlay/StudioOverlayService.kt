package com.visualtasker.wss.overlay

import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.IBinder
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import com.visualtasker.wss.MainActivity
import com.visualtasker.wss.accessibility.VisualTaskerAccessibilityService
import com.visualtasker.wss.accessibility.toInspectorText
import com.visualtasker.wss.recording.RecordingOperationResult
import com.visualtasker.wss.recording.RecordingSessionRuntime
import com.visualtasker.wss.workspace.model.RecordingEventStore
import com.visualtasker.wss.window.AndroidDisplayContextFactory
import com.visualtasker.wss.window.OverlayWindowHost
import com.visualtasker.wss.window.WindowBounds
import com.visualtasker.wss.window.WindowCoordinateSpace
import com.visualtasker.wss.window.WindowDiagnostic
import com.visualtasker.wss.window.WindowDiagnosticCode
import com.visualtasker.wss.window.WindowHostType
import com.visualtasker.wss.window.WindowSessionProcessRegistry
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

class StudioOverlayService : Service() {

    companion object {
        const val ACTION_SHOW_FLOATING_PANEL = "com.visualtasker.wss.overlay.SHOW_FLOATING_PANEL"
        const val ACTION_SHOW_FLOATING_TOOLBAR = "com.visualtasker.wss.overlay.SHOW_FLOATING_TOOLBAR"
        const val ACTION_SHOW_FLOATING_INSPECTOR = "com.visualtasker.wss.overlay.SHOW_FLOATING_INSPECTOR"
        const val ACTION_CAPTURE_SCREENSHOT = "com.visualtasker.wss.overlay.CAPTURE_SCREENSHOT"
        const val ACTION_TOGGLE_RECORDING = "com.visualtasker.wss.overlay.TOGGLE_RECORDING"

    }

    private lateinit var windowManager: WindowManager
    private lateinit var stateStore: OverlayStateStore
    private lateinit var overlayWindowHost: OverlayWindowHost
    private val overlays = linkedMapOf<String, OverlayHandle>()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var statusView: TextView? = null
    private var recordingStatusView: TextView? = null
    private var recordingButton: TextView? = null
    private var recordingTickerJob: Job? = null
    private var inspectorTickerJob: Job? = null
    private var inspectorContentView: TextView? = null
    private var watchdogRunning = false
    private var toolbarMinimized = false
    private var overlayStatusMessage: String? = null
    private var liveMarkerMultiGroupId: String? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        stateStore = OverlayStateStore(this)
        overlayWindowHost = OverlayWindowHost(
            registry = WindowSessionProcessRegistry.get(this),
            displayContext = AndroidDisplayContextFactory.from(this),
        )
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            restoreVisibleOverlays()
            return START_STICKY
        }
        when (intent?.action) {
            ACTION_SHOW_FLOATING_PANEL -> showFloatingPanel()
            ACTION_SHOW_FLOATING_TOOLBAR -> showFloatingToolbar()
            ACTION_SHOW_FLOATING_INSPECTOR -> showFloatingInspector()
            ACTION_CAPTURE_SCREENSHOT -> captureScreenshot()
            ACTION_TOGGLE_RECORDING -> toggleRecording()
        }
        return START_STICKY
    }

    private fun restoreVisibleOverlays() {
        stateStore.visibleKeys().forEach { key ->
            when (key) {
                "panel" -> showFloatingPanel()
                "toolbar" -> showFloatingToolbar()
                "toolbar-mini" -> showMinimizedToolbarHandle()
                "inspector" -> showFloatingInspector()
            }
        }
    }

    override fun onDestroy() {
        overlays.forEach { (key, handle) ->
            overlayWindowHost.detached(key, hidden = false)
            runCatching { windowManager.removeView(handle.root) }
        }
        overlays.clear()
        overlayWindowHost.dispose()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun showFloatingPanel() {
        if (overlays.containsKey("panel")) return
        recordOverlayEvent("overlay.show", "panel")
        val modes = listOf("TextEditor", "BlockEditor", "Flowchart", "Debug")
        var modeIndex = 0
        val shell = createOverlayShell(
            key = "panel",
            title = "Floating Panel",
            showResizeHandle = true,
        )
        val title = TextView(this).apply {
            text = "Ansicht: ${modes[modeIndex]}"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setPadding(0, 0, 0, dp(8))
        }
        val hint = TextView(this).apply {
            text = "Panel-Look + Resize aktiv (unten rechts ziehen)"
            setTextColor(Color.argb(220, 160, 170, 185))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            setPadding(0, 0, 0, dp(10))
        }
        val switchButton = Button(this).apply {
            text = "Ansicht wechseln"
            textSize = 11f
            setOnClickListener {
                modeIndex = (modeIndex + 1) % modes.size
                title.text = "Ansicht: ${modes[modeIndex]}"
            }
        }
        shell.content.addView(title)
        shell.content.addView(hint)
        shell.content.addView(switchButton)
        addOverlay(
            key = "panel",
            root = shell.root,
            dragHandle = shell.header,
            resizeHandle = shell.resizeHandle,
            recordingStatus = shell.recordingStatus,
            x = 120,
            y = 160,
            width = dp(360),
            height = dp(280),
            minWidth = dp(280),
            minHeight = dp(190),
        )
    }

    private fun showFloatingToolbar() {
        removeOverlay("toolbar-mini", stopWhenEmpty = false)
        toolbarMinimized = false
        if (overlays.containsKey("toolbar")) return
        recordOverlayEvent("overlay.show", "toolbar")
        val shell = createOverlayShell(
            key = "toolbar",
            title = "",
            showResizeHandle = false,
        )
        val controls = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        controls.addView(
            splitButton(
                label = "▣",
                onPrimary = { captureScreenshot() },
                menuItems = listOf(
                    "Screenshot" to { captureScreenshot() },
                    "Screenshot + A11y" to { captureScreenshotWithScan("A11y") },
                    "Screenshot + OCR" to { captureScreenshotWithScan("OCR") },
                    "Screenshot + YOLO" to { captureScreenshotWithScan("YOLO") },
                )
            )
        )
        controls.addView(
            splitButton(
                label = if (RecordingEventStore.isRecording()) "■" else "●",
                onPrimary = { toggleRecording() },
                menuItems = listOf(
                    "Record / Stop" to { toggleRecording() },
                    "Watchdog / Stop" to { toggleWatchdog() },
                )
            ).also { group -> recordingButton = group.getChildAt(0) as? TextView }
        )
        controls.addView(
            splitButton(
                label = "⌂",
                onPrimary = { switchToWorkspace() },
                menuItems = listOf(
                    "Workspace öffnen" to { switchToWorkspace() },
                    "Toolbar behalten" to { setStatus("Workspace bleibt aktiv") },
                )
            )
        )
        controls.addView(
            splitButton(
                label = "▤",
                onPrimary = { showFloatingPanel() },
                menuItems = listOf(
                    "Floating Panel öffnen" to { showFloatingPanel() },
                    "Floating Inspector öffnen" to { showFloatingInspector() },
                    "Beide Panels öffnen" to {
                        showFloatingPanel()
                        showFloatingInspector()
                    },
                )
            )
        )
        controls.addView(
            splitButton(
                label = "▶",
                onPrimary = { playRecording() },
                menuItems = listOf(
                    "Aufnahme wiedergeben" to { playRecording() },
                    "Workflow abspielen" to { playWorkflow() },
                    "Wiedergabe stoppen" to { stopPlayback() },
                )
            )
        )
        controls.addView(
            splitButton(
                label = "✚",
                onPrimary = { startMarkerMode("Point") },
                menuItems = listOf(
                    "Point markieren" to { startMarkerMode("Point") },
                    "Region markieren" to { startMarkerMode("Region") },
                    "Swipe markieren" to { startMarkerMode("Swipe") },
                    "Spline freihand" to { startMarkerMode("Spline") },
                    "Path zeichnen" to { startMarkerMode("Path") },
                    "Multi umschalten" to { toggleLiveMarkerMulti() },
                )
            )
        )
        shell.headerActions.addView(controls)
        shell.headerActions.addView(headerIcon("−") { minimizeToolbar() })
        statusView = shell.recordingStatus
        recordingStatusView = shell.recordingStatus
        shell.content.visibility = View.GONE
        updateRecordingUi()
        addOverlay(
            key = "toolbar",
            root = shell.root,
            dragHandle = shell.header,
            resizeHandle = null,
            recordingStatus = shell.recordingStatus,
            x = 220,
            y = 70,
            width = WindowManager.LayoutParams.WRAP_CONTENT,
            height = WindowManager.LayoutParams.WRAP_CONTENT,
            minWidth = dp(120),
            minHeight = dp(42),
        )
    }

    private fun showFloatingInspector() {
        if (overlays.containsKey("inspector")) return
        recordOverlayEvent("overlay.show", "inspector")
        val shell = createOverlayShell(
            key = "inspector",
            title = "Floating Inspector",
            showResizeHandle = false,
        )
        val content = TextView(this).apply {
            text = overlayInspectorText()
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setPadding(0, 0, 0, dp(8))
        }
        inspectorContentView = content
        statusView = content
        val capture = Button(this).apply {
            text = "Screenshot"
            textSize = 11f
            setOnClickListener { captureScreenshot() }
        }
        val record = Button(this).apply {
            text = if (RecordingEventStore.isRecording()) "Aufnahme stoppen" else "Aufnahme starten"
            textSize = 11f
            setOnClickListener { toggleRecording() }
        }
        shell.content.addView(content)
        shell.content.addView(capture)
        shell.content.addView(record)
        addOverlay(
            key = "inspector",
            root = shell.root,
            dragHandle = shell.header,
            resizeHandle = null,
            recordingStatus = shell.recordingStatus,
            x = 80,
            y = 460,
            width = 420,
            height = WindowManager.LayoutParams.WRAP_CONTENT,
            minWidth = dp(220),
            minHeight = dp(100),
        )
        startInspectorTicker()
    }

    private fun toolButton(label: String, onClick: (() -> Unit)? = null): Button =
        Button(this).apply {
            text = label
            textSize = 11f
            minWidth = 0
            minimumWidth = 0
            onClick?.let { handler -> setOnClickListener { handler() } }
        }

    private fun splitButton(
        label: String,
        onPrimary: () -> Unit,
        menuItems: List<Pair<String, () -> Unit>>,
    ): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = segmentedBackground()
            setPadding(dp(2), dp(2), dp(2), dp(2))
            val primary = segmentText(label, primary = true, onClick = onPrimary)
            addView(primary)
            addView(segmentDivider())
            val arrow = segmentText("⌄", primary = false)
            addView(arrow)
            arrow.setOnClickListener { showSplitMenu(anchor = arrow, items = menuItems) }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
                setMargins(0, 0, dp(3), 0)
            }
        }

    private fun segmentText(
        label: String,
        primary: Boolean,
        onClick: (() -> Unit)? = null,
    ): TextView =
        TextView(this).apply {
            text = label
            gravity = Gravity.CENTER
            typeface = if (primary) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            setTextSize(TypedValue.COMPLEX_UNIT_SP, if (primary) 15f else 13f)
            setTextColor(if (primary) Color.WHITE else Color.argb(235, 210, 214, 225))
            minWidth = if (primary) dp(26) else dp(20)
            minHeight = dp(30)
            setPadding(dp(6), dp(4), dp(6), dp(4))
            background = segmentBackground(primary)
            isClickable = true
            isFocusable = true
            onClick?.let { handler -> setOnClickListener { handler() } }
        }

    private fun headerIcon(label: String, onClick: () -> Unit): TextView =
        TextView(this).apply {
            text = label
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setTextColor(Color.argb(245, 235, 238, 245))
            minWidth = dp(32)
            minHeight = dp(32)
            setPadding(dp(8), dp(4), dp(8), dp(4))
            background = segmentBackground(primary = false)
            isClickable = true
            isFocusable = true
            setOnClickListener { onClick() }
        }

    private fun segmentDivider(): View =
        View(this).apply {
            setBackgroundColor(Color.argb(95, 180, 185, 205))
            layoutParams = LinearLayout.LayoutParams(dp(1), dp(22)).apply {
                gravity = Gravity.CENTER_VERTICAL
            }
        }

    private fun segmentedBackground(): GradientDrawable =
        GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(18).toFloat()
            setColor(Color.argb(235, 39, 35, 52))
            setStroke(dp(1), Color.argb(210, 135, 120, 170))
        }

    private fun segmentBackground(primary: Boolean): GradientDrawable =
        GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(16).toFloat()
            setColor(if (primary) Color.argb(210, 92, 70, 132) else Color.TRANSPARENT)
        }

    private fun showSplitMenu(anchor: View, items: List<Pair<String, () -> Unit>>) {
        val menu = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val bg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(10).toFloat()
                setColor(Color.argb(248, 24, 27, 35))
                setStroke(dp(1), Color.argb(255, 80, 90, 110))
            }
            background = bg
            setPadding(dp(6), dp(6), dp(6), dp(6))
        }
        val popup = PopupWindow(menu, WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT, true)
        items.forEach { (label, action) ->
            menu.addView(
                popupMenuItem(label) {
                    popup.dismiss()
                    action()
                }
            )
        }
        popup.isOutsideTouchable = true
        popup.showAsDropDown(anchor)
    }

    private fun popupMenuItem(label: String, onClick: () -> Unit): TextView =
        TextView(this).apply {
            text = label
            setTextColor(Color.argb(245, 235, 238, 245))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setPadding(dp(12), dp(9), dp(18), dp(9))
            minWidth = dp(180)
            background = segmentBackground(primary = false)
            isClickable = true
            isFocusable = true
            setOnClickListener { onClick() }
        }

    private fun captureScreenshot() {
        val service = VisualTaskerAccessibilityService.current()
        if (service == null) {
            setStatus("Accessibility nicht aktiv")
            recordOverlayEvent("screenshot.blocked", "accessibility inactive")
            showFloatingInspector()
            return
        }
        val target = File(filesDir, "emscript-runtime/screenshots/overlay-${timestamp()}.png")
        target.parentFile?.mkdirs()
        setStatus("Screenshot laeuft...")
        val screenshotAttributes = mapOf(
            "screenshotPath" to target.absolutePath,
            "screenshotFile" to target.name,
        )
        recordOverlayEvent("screenshot.requested", "target=${target.name}", screenshotAttributes)
        serviceScope.launch {
            setOverlaysVisible(false)
            delay(180)
            val ok = try {
                service.takeScreenshotTo(target)
            } finally {
                setOverlaysVisible(true)
            }
            setStatus(if (ok) "Gespeichert: ${target.name}" else "Screenshot fehlgeschlagen")
            recordOverlayEvent(
                kind = if (ok) "screenshot.saved" else "screenshot.failed",
                message = "target=${target.name}",
                attributes = screenshotAttributes,
            )
        }
    }

    private fun captureScreenshotWithScan(scanMode: String) {
        setStatus("Screenshot + $scanMode Scan angefragt")
        recordOverlayEvent("screenshot.scan.requested", "mode=$scanMode", mapOf("scanMode" to scanMode))
        captureScreenshot()
    }

    private fun toggleWatchdog() {
        watchdogRunning = !watchdogRunning
        val state = if (watchdogRunning) "gestartet" else "gestoppt"
        setStatus("Watchdog $state")
        recordOverlayEvent("watchdog.$state", "Watchdog $state")
        updateRecordingUi()
    }

    private fun switchToWorkspace() {
        getSharedPreferences("panel_ui_options", MODE_PRIVATE)
            .edit()
            .putString("startup_screen", "WORKSPACE")
            .apply()
        startActivity(
            Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
        )
        setStatus("Workspace geoeffnet")
        recordOverlayEvent("workspace.open", "Workspace geoeffnet")
    }

    private fun playRecording() {
        val latest = RecordingEventStore.latestRecordingFile(this)
        if (latest == null) {
            setStatus("Keine Aufnahme zum Wiedergeben")
            recordOverlayEvent("recording.play.blocked", "Keine Aufnahme vorhanden")
            return
        }
        setStatus("Aufnahme wiedergeben: ${latest.name}")
        recordOverlayEvent("recording.play.requested", "file=${latest.name}")
    }

    private fun playWorkflow() {
        setStatus("Workflow-Abspielung angefragt")
        recordOverlayEvent("workflow.play.requested", "Workflow-Abspielung angefragt")
    }

    private fun stopPlayback() {
        setStatus("Wiedergabe gestoppt")
        recordOverlayEvent("playback.stopped", "Wiedergabe gestoppt")
    }

    private fun startMarkerMode(mode: String) {
        val markerMode = runCatching { LiveMarkerMode.valueOf(mode) }.getOrNull() ?: return
        removeOverlay("live-marker", stopWhenEmpty = false)
        val view = LiveMarkerOverlayView(
            context = this,
            mode = markerMode,
            multiGroupId = liveMarkerMultiGroupId,
            onMarker = { marker -> completeLiveMarker(marker) },
            onCancel = {
                removeOverlay("live-marker", stopWhenEmpty = false)
                setStatus("Marker abgebrochen")
            },
        )
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }
        if (!Settings.canDrawOverlays(this)) {
            reportWindowFailure(
                code = WindowDiagnosticCode.OVERLAY_PERMISSION_MISSING,
                key = "live-marker",
                message = "System overlay permission is missing",
            )
            return
        }
        val attached = runCatching { windowManager.addView(view, params) }
        if (attached.isFailure) {
            reportWindowFailure(
                code = WindowDiagnosticCode.WINDOW_MANAGER_FAILURE,
                key = "live-marker",
                message = attached.exceptionOrNull()?.message ?: "WindowManager rejected live marker",
            )
            return
        }
        overlays["live-marker"] = OverlayHandle(view, params, null)
        val display = AndroidDisplayContextFactory.from(this)
        overlayWindowHost.attached(
            panelId = "live-marker",
            bounds = WindowBounds(
                left = 0f,
                top = 0f,
                width = display.physicalWidthPx.toFloat(),
                height = display.physicalHeightPx.toFloat(),
                space = WindowCoordinateSpace.DISPLAY_PX,
            ),
            passThrough = false,
        )
        setStatus("$mode markieren${if (liveMarkerMultiGroupId != null) " · Multi" else ""}")
        recordOverlayEvent("marker.mode", "mode=$mode", mapOf("multi" to (liveMarkerMultiGroupId != null).toString()))
        showFloatingInspector()
    }

    private fun toggleLiveMarkerMulti() {
        liveMarkerMultiGroupId = if (liveMarkerMultiGroupId == null) {
            "multi:${System.currentTimeMillis()}"
        } else {
            null
        }
        setStatus(if (liveMarkerMultiGroupId == null) "Marker Multi aus" else "Marker Multi aktiv")
    }

    private fun completeLiveMarker(marker: LiveMarkerResult) {
        val target = LiveMarkerStore.append(this, marker)
        val points = marker.points.joinToString(";") { "${it.x.roundToInt()},${it.y.roundToInt()}" }
        val summary = "${marker.mode.name} ${marker.left.roundToInt()},${marker.top.roundToInt()} ${
            (marker.right - marker.left).roundToInt()
        }x${(marker.bottom - marker.top).roundToInt()}"
        recordOverlayEvent(
            kind = "marker.saved",
            message = summary,
            attributes = mapOf(
                "markerId" to marker.id,
                "markerMode" to marker.mode.name,
                "points" to points,
                "multiGroupId" to marker.multiGroupId.orEmpty(),
                "markerStore" to target.absolutePath,
            ),
        )
        setStatus("Marker gespeichert: $summary")
        showFloatingInspector()
        if (marker.multiGroupId == null) {
            removeOverlay("live-marker", stopWhenEmpty = false)
        }
    }

    private fun minimizeToolbar() {
        if (toolbarMinimized) return
        toolbarMinimized = true
        recordOverlayEvent("overlay.minimize", "toolbar")
        showMinimizedToolbarHandle()
        removeOverlay("toolbar", stopWhenEmpty = false)
    }

    private fun showMinimizedToolbarHandle() {
        if (overlays.containsKey("toolbar-mini")) return
        var lastTapMs = 0L
        val handle = TextView(this).apply {
            text = "VT"
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setTextColor(Color.WHITE)
            background = segmentedBackground()
            setPadding(dp(10), dp(8), dp(10), dp(8))
            isClickable = true
            isFocusable = true
            setOnClickListener {
                val now = System.currentTimeMillis()
                if (now - lastTapMs <= 320L) {
                    removeOverlay("toolbar-mini", stopWhenEmpty = false)
                    showFloatingToolbar()
                    recordOverlayEvent("overlay.maximize.doubletap", "toolbar")
                } else {
                    lastTapMs = now
                    setStatus("Doppeltippen zum Maximieren")
                }
            }
        }
        val screenWidth = resources.displayMetrics.widthPixels
        addOverlay(
            key = "toolbar-mini",
            root = handle,
            dragHandle = handle,
            resizeHandle = null,
            recordingStatus = null,
            x = (screenWidth - dp(58)).coerceAtLeast(0),
            y = dp(84),
            width = WindowManager.LayoutParams.WRAP_CONTENT,
            height = WindowManager.LayoutParams.WRAP_CONTENT,
            minWidth = dp(42),
            minHeight = dp(36),
        )
    }

    private fun setStatus(message: String) {
        overlayStatusMessage = message
        statusView?.text = if (statusView === inspectorContentView) overlayInspectorText() else message
        statusView?.visibility = View.VISIBLE
    }

    private fun toggleRecording() {
        if (!RecordingEventStore.isRecording() && !RecordingSessionRuntime.isRecording()) {
            startOverlayRecording()
        } else {
            stopOverlayRecording()
        }
    }

    private fun startOverlayRecording() {
        val baseline = VisualTaskerAccessibilityService.current()?.currentWindowContext()
        setStatus("Initiale Scene wird erfasst...")
        serviceScope.launch {
            setOverlaysVisible(false)
            delay(180)
            val result = try {
                RecordingSessionRuntime.start(this@StudioOverlayService)
            } finally {
                setOverlaysVisible(true)
            }
            when (result) {
                is RecordingOperationResult.Success -> {
                    val target = RecordingEventStore.start(
                        this@StudioOverlayService,
                        baselineWindowContext = baseline,
                        canonicalSessionId = result.value.sessionId,
                    )
                    setStatus("Aufnahme laeuft: ${target.name}")
                    startRecordingTicker()
                }
                is RecordingOperationResult.Failure -> setStatus("Aufnahme fehlgeschlagen: ${result.code}")
            }
            updateRecordingUi()
        }
    }

    private fun stopOverlayRecording() {
        serviceScope.launch {
            val canonical = RecordingSessionRuntime.stop()
            val target = RecordingEventStore.stop()
            setStatus(
                when (canonical) {
                    is RecordingOperationResult.Success -> "Aufnahme gespeichert: ${target?.name ?: canonical.value.sessionId}"
                    is RecordingOperationResult.Failure -> "Aufnahme nur teilweise gespeichert: ${canonical.code}"
                }
            )
            recordingTickerJob?.cancel()
            recordingTickerJob = null
            updateRecordingUi()
        }
    }

    private fun recordOverlayEvent(
        kind: String,
        message: String,
        attributes: Map<String, String> = emptyMap(),
    ) {
        RecordingEventStore.recordOverlayEvent(kind, message, mapOf("message" to message) + attributes)
    }

    private fun updateRecordingUi() {
        val running = RecordingEventStore.isRecording() || RecordingSessionRuntime.isRecording()
        recordingButton?.text = if (running) "■" else "●"
        val status = RecordingEventStore.activeStatus()
        val text = if (running) {
            val elapsed = "%.1fs".format(Locale.US, status.elapsedMs / 1000f)
            "REC ${status.eventCount} Events · $elapsed · ${status.lastLabel ?: status.fileName.orEmpty()}"
        } else if (watchdogRunning) {
            "WATCHDOG aktiv · ${overlayStatusMessage ?: "warte auf Ereignisse"}"
        } else {
            overlayStatusMessage ?: ""
        }
        val showStatus = text.isNotBlank()
        recordingStatusView?.text = text
        recordingStatusView?.visibility = if (showStatus) View.VISIBLE else View.GONE
        overlays.values.forEach { handle ->
            handle.recordingStatus?.text = text
            handle.recordingStatus?.visibility = if (showStatus) View.VISIBLE else View.GONE
        }
    }

    private fun startRecordingTicker() {
        recordingTickerJob?.cancel()
        recordingTickerJob = serviceScope.launch {
            while (isActive && RecordingEventStore.isRecording()) {
                updateRecordingUi()
                delay(500)
            }
            updateRecordingUi()
        }
    }

    private fun startInspectorTicker() {
        inspectorTickerJob?.cancel()
        inspectorTickerJob = serviceScope.launch {
            while (isActive && overlays.containsKey("inspector")) {
                inspectorContentView?.text = overlayInspectorText()
                delay(500)
            }
        }
    }

    private fun overlayInspectorText(): String {
        val snapshotText = VisualTaskerAccessibilityService.current()
            ?.currentInspectorSnapshot()
            ?.toInspectorText()
            ?: "Accessibility: nicht aktiv\nUI-Element: nicht verfügbar"
        return overlayStatusMessage
            ?.takeIf(String::isNotBlank)
            ?.let { "$it\n\n$snapshotText" }
            ?: snapshotText
    }

    private fun overlayStatusText(): String =
        overlayStatusMessage ?: if (VisualTaskerAccessibilityService.current() == null) {
            "Accessibility: nicht aktiv\nScreenshot: blockiert"
        } else {
            "Accessibility: aktiv\nScreenshot: bereit"
        }

    private fun timestamp(): String =
        SimpleDateFormat("yyyyMMdd-HHmmss-SSS", Locale.US).format(Date())

    private fun createOverlayShell(
        key: String,
        title: String,
        showResizeHandle: Boolean,
    ): OverlayShell {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val bg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(14).toFloat()
                setColor(Color.argb(242, 20, 22, 28))
                setStroke(dp(1), Color.argb(255, 65, 72, 88))
            }
            background = bg
            clipToOutline = true
        }
        val titleBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(4), dp(4), dp(4))
            setBackgroundColor(Color.argb(210, 38, 42, 54))
        }
        val dragGrip = TextView(this).apply {
            text = "☰"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setTextColor(Color.argb(235, 220, 225, 235))
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 0, dp(10), 0)
        }
        val titleText = TextView(this).apply {
            text = title
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setTextColor(Color.rgb(120, 210, 255))
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(
                if (title.isBlank()) LinearLayout.LayoutParams.WRAP_CONTENT else 0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                if (title.isBlank()) 0f else 1f,
            )
        }
        val headerActions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        val closeButton = headerIcon("×") { removeOverlay(key) }
        titleBar.addView(dragGrip)
        titleBar.addView(titleText)
        titleBar.addView(headerActions)
        titleBar.addView(closeButton)

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(12), dp(12), dp(12))
        }

        container.addView(titleBar)
        container.addView(content)

        val recordingStatus = TextView(this).apply {
            setTextColor(Color.argb(235, 255, 210, 120))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            setPadding(dp(12), dp(6), dp(12), dp(8))
            visibility = View.GONE
        }
        container.addView(recordingStatus)

        var resizeHandle: View? = null
        if (showResizeHandle) {
            val footer = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.END or Gravity.CENTER_VERTICAL
                setPadding(dp(8), 0, dp(8), dp(6))
            }
            val handle = TextView(this).apply {
                text = "◢"
                setTextColor(Color.rgb(120, 210, 255))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                setPadding(dp(6), dp(2), dp(2), dp(2))
            }
            footer.addView(handle)
            container.addView(footer)
            resizeHandle = handle
        }

        return OverlayShell(
            root = container,
            header = titleBar,
            content = content,
            headerActions = headerActions,
            recordingStatus = recordingStatus,
            resizeHandle = resizeHandle,
        )
    }

    private fun addOverlay(
        key: String,
        root: View,
        dragHandle: View,
        resizeHandle: View?,
        recordingStatus: TextView?,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        minWidth: Int,
        minHeight: Int,
    ) {
        if (!Settings.canDrawOverlays(this)) {
            reportWindowFailure(
                code = WindowDiagnosticCode.OVERLAY_PERMISSION_MISSING,
                key = key,
                message = "System overlay permission is missing",
            )
            return
        }
        val saved = stateStore.load(key)
        val registry = WindowSessionProcessRegistry.get(this)
        val display = AndroidDisplayContextFactory.from(this)
        val persistedSession = registry.find(WindowHostType.OVERLAY, key)
        if (persistedSession != null && persistedSession.displayContext.displayId != display.displayId) {
            reportWindowFailure(
                code = WindowDiagnosticCode.DISPLAY_UNAVAILABLE,
                key = key,
                message = "Saved overlay display ${persistedSession.displayContext.displayId.value} is unavailable",
            )
            return
        }
        val persistedBounds = persistedSession?.placement?.bounds
            ?.takeIf { it.space == WindowCoordinateSpace.DISPLAY_PX }
        val params = WindowManager.LayoutParams(
            persistedBounds?.width?.toInt()?.takeIf { it > 0 } ?: saved?.width?.takeIf { it > 0 } ?: width,
            persistedBounds?.height?.toInt()?.takeIf { it > 0 } ?: saved?.height?.takeIf { it > 0 } ?: height,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            this.x = persistedBounds?.left?.toInt() ?: saved?.x ?: x
            this.y = persistedBounds?.top?.toInt() ?: saved?.y ?: y
        }

        installDragHandler(key, dragHandle, root, params)
        if (resizeHandle != null) {
            installResizeHandler(
                key = key,
                resizeHandle = resizeHandle,
                root = root,
                params = params,
                minWidth = minWidth,
                minHeight = minHeight,
            )
        }
        val attached = runCatching { windowManager.addView(root, params) }
        if (attached.isFailure) {
            reportWindowFailure(
                code = WindowDiagnosticCode.WINDOW_MANAGER_FAILURE,
                key = key,
                message = attached.exceptionOrNull()?.message ?: "WindowManager rejected overlay",
            )
            return
        }
        overlays[key] = OverlayHandle(root, params, recordingStatus)
        overlayWindowHost.attached(
            panelId = key,
            bounds = params.registryBounds(root, minWidth, minHeight),
            passThrough = false,
        )
        root.post {
            if (overlays[key]?.root === root && root.width > 0 && root.height > 0) {
                overlayWindowHost.resize(key, root.width.toFloat(), root.height.toFloat())
            }
        }
        savePlacement(key, params, visible = true)
        updateRecordingUi()
    }

    private fun removeOverlay(key: String, stopWhenEmpty: Boolean = true) {
        val handle = overlays.remove(key) ?: return
        if (key == "inspector") {
            inspectorTickerJob?.cancel()
            inspectorTickerJob = null
            inspectorContentView = null
            statusView = overlays["toolbar"]?.recordingStatus
        }
        savePlacement(key, handle.params, visible = false)
        overlayWindowHost.detached(key, hidden = true)
        runCatching { windowManager.removeView(handle.root) }
        if (stopWhenEmpty && overlays.isEmpty()) {
            stopSelf()
        }
    }

    private fun setOverlaysVisible(visible: Boolean) {
        overlays.values.forEach { handle ->
            handle.root.visibility = if (visible) View.VISIBLE else View.INVISIBLE
        }
    }

    private fun installDragHandler(
        key: String,
        dragHandle: View,
        root: View,
        params: WindowManager.LayoutParams,
    ) {
        dragHandle.setOnTouchListener(object : View.OnTouchListener {
            var lastX = 0f
            var lastY = 0f
            var dragging = false

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        overlayWindowHost.beginDrag(key)
                        lastX = event.rawX
                        lastY = event.rawY
                        dragging = false
                        return true
                    }

                    MotionEvent.ACTION_MOVE -> {
                        val dx = event.rawX - lastX
                        val dy = event.rawY - lastY
                        if (abs(dx) > 4f || abs(dy) > 4f) {
                            dragging = true
                            params.x += dx.toInt()
                            params.y += dy.toInt()
                            clampOverlayPosition(params, root)
                            windowManager.updateViewLayout(root, params)
                            overlayWindowHost.move(key, params.x.toFloat(), params.y.toFloat())
                            lastX = event.rawX
                            lastY = event.rawY
                            return true
                        }
                    }

                    MotionEvent.ACTION_UP -> {
                        if (dragging) {
                            savePlacement(key, params, visible = true)
                        } else {
                            v.performClick()
                        }
                        overlayWindowHost.endInteraction(key)
                        return true
                    }

                    MotionEvent.ACTION_CANCEL -> {
                        if (dragging) savePlacement(key, params, visible = true)
                        overlayWindowHost.endInteraction(key)
                        return true
                    }
                }
                return dragging
            }
        })
    }

    private fun installResizeHandler(
        key: String,
        resizeHandle: View,
        root: View,
        params: WindowManager.LayoutParams,
        minWidth: Int,
        minHeight: Int,
    ) {
        resizeHandle.setOnTouchListener(object : View.OnTouchListener {
            var lastX = 0f
            var lastY = 0f
            var baseWidth = 0
            var baseHeight = 0
            var resizing = false

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        overlayWindowHost.beginResize(key)
                        lastX = event.rawX
                        lastY = event.rawY
                        baseWidth = params.width
                        baseHeight = params.height
                        resizing = true
                        return true
                    }

                    MotionEvent.ACTION_MOVE -> {
                        if (!resizing) return false
                        val dx = (event.rawX - lastX).roundToInt()
                        val dy = (event.rawY - lastY).roundToInt()
                        params.width = (baseWidth + dx).coerceAtLeast(minWidth)
                        params.height = (baseHeight + dy).coerceAtLeast(minHeight)
                        windowManager.updateViewLayout(root, params)
                        overlayWindowHost.resize(key, params.width.toFloat(), params.height.toFloat())
                        return true
                    }

                    MotionEvent.ACTION_UP,
                    MotionEvent.ACTION_CANCEL,
                    -> {
                        resizing = false
                        savePlacement(key, params, visible = true)
                        overlayWindowHost.endInteraction(key)
                        return true
                    }
                }
                return false
            }
        })
    }

    private fun savePlacement(
        key: String,
        params: WindowManager.LayoutParams,
        visible: Boolean,
    ) {
        stateStore.save(
            key,
            OverlayPlacement(
                x = params.x,
                y = params.y,
                width = params.width,
                height = params.height,
                visible = visible,
            ),
        )
    }

    private fun WindowManager.LayoutParams.registryBounds(root: View, minWidth: Int, minHeight: Int): WindowBounds =
        WindowBounds(
            left = x.toFloat(),
            top = y.toFloat(),
            width = width.takeIf { it > 0 }?.toFloat() ?: root.width.takeIf { it > 0 }?.toFloat() ?: minWidth.toFloat(),
            height = height.takeIf { it > 0 }?.toFloat() ?: root.height.takeIf { it > 0 }?.toFloat() ?: minHeight.toFloat(),
            space = WindowCoordinateSpace.DISPLAY_PX,
        )

    private fun clampOverlayPosition(params: WindowManager.LayoutParams, root: View) {
        val display = AndroidDisplayContextFactory.from(this)
        val width = params.width.takeIf { it > 0 } ?: root.width.coerceAtLeast(1)
        val height = params.height.takeIf { it > 0 } ?: root.height.coerceAtLeast(1)
        val clamped = display.clamp(
            WindowBounds(
                left = params.x.toFloat(),
                top = params.y.toFloat(),
                width = width.toFloat(),
                height = height.toFloat(),
                space = WindowCoordinateSpace.DISPLAY_PX,
            )
        )
        params.x = clamped.left.toInt()
        params.y = clamped.top.toInt()
    }

    private fun reportWindowFailure(code: WindowDiagnosticCode, key: String, message: String) {
        WindowSessionProcessRegistry.get(this).report(
            WindowDiagnostic(
                code = code,
                message = message,
                sessionId = WindowSessionProcessRegistry.get(this).find(WindowHostType.OVERLAY, key)?.id,
                hostId = overlayWindowHost.id,
            )
        )
        overlayStatusMessage = message
        updateRecordingUi()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).roundToInt()

    private data class OverlayShell(
        val root: LinearLayout,
        val header: View,
        val content: LinearLayout,
        val headerActions: LinearLayout,
        val recordingStatus: TextView,
        val resizeHandle: View?,
    )

    private data class OverlayHandle(
        val root: View,
        val params: WindowManager.LayoutParams,
        val recordingStatus: TextView?,
    )
}
