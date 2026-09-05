package com.visualtasker.wss.overlay

import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.IBinder
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.visualtasker.wss.accessibility.VisualTaskerAccessibilityService
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

class StudioOverlayService : Service() {

    companion object {
        const val ACTION_SHOW_FLOATING_PANEL = "com.visualtasker.wss.overlay.SHOW_FLOATING_PANEL"
        const val ACTION_SHOW_FLOATING_TOOLBAR = "com.visualtasker.wss.overlay.SHOW_FLOATING_TOOLBAR"
        const val ACTION_SHOW_FLOATING_INSPECTOR = "com.visualtasker.wss.overlay.SHOW_FLOATING_INSPECTOR"
        const val ACTION_CAPTURE_SCREENSHOT = "com.visualtasker.wss.overlay.CAPTURE_SCREENSHOT"
    }

    private lateinit var windowManager: WindowManager
    private val overlays = linkedMapOf<String, OverlayHandle>()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var statusView: TextView? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SHOW_FLOATING_PANEL -> showFloatingPanel()
            ACTION_SHOW_FLOATING_TOOLBAR -> showFloatingToolbar()
            ACTION_SHOW_FLOATING_INSPECTOR -> showFloatingInspector()
            ACTION_CAPTURE_SCREENSHOT -> captureScreenshot()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        overlays.values.forEach { handle ->
            runCatching { windowManager.removeView(handle.root) }
        }
        overlays.clear()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun showFloatingPanel() {
        if (overlays.containsKey("panel")) return
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
            x = 120,
            y = 160,
            width = dp(360),
            height = dp(280),
            minWidth = dp(280),
            minHeight = dp(190),
        )
    }

    private fun showFloatingToolbar() {
        if (overlays.containsKey("toolbar")) return
        val shell = createOverlayShell(
            key = "toolbar",
            title = "Floating Toolbar",
            showResizeHandle = false,
        )
        val controls = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        controls.addView(toolButton("Shot") { captureScreenshot() })
        controls.addView(toolButton("Panel") { showFloatingPanel() })
        controls.addView(toolButton("Info") { showFloatingInspector() })
        controls.addView(toolButton("Hide") { removeOverlay("toolbar") })
        val status = TextView(this).apply {
            text = "Bereit fuer Screenshot"
            setTextColor(Color.argb(230, 220, 225, 235))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            setPadding(0, dp(8), 0, 0)
        }
        statusView = status
        shell.content.addView(controls)
        shell.content.addView(status)
        addOverlay(
            key = "toolbar",
            root = shell.root,
            dragHandle = shell.header,
            resizeHandle = null,
            x = 220,
            y = 70,
            width = WindowManager.LayoutParams.WRAP_CONTENT,
            height = WindowManager.LayoutParams.WRAP_CONTENT,
            minWidth = dp(180),
            minHeight = dp(80),
        )
    }

    private fun showFloatingInspector() {
        if (overlays.containsKey("inspector")) return
        var enabled = true
        val shell = createOverlayShell(
            key = "inspector",
            title = "Floating Inspector",
            showResizeHandle = false,
        )
        val content = TextView(this).apply {
            text = overlayStatusText()
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setPadding(0, 0, 0, dp(8))
        }
        statusView = content
        val capture = Button(this).apply {
            text = "Screenshot"
            textSize = 11f
            setOnClickListener { captureScreenshot() }
        }
        shell.content.addView(content)
        shell.content.addView(capture)
        addOverlay(
            key = "inspector",
            root = shell.root,
            dragHandle = shell.header,
            resizeHandle = null,
            x = 80,
            y = 460,
            width = 420,
            height = WindowManager.LayoutParams.WRAP_CONTENT,
            minWidth = dp(220),
            minHeight = dp(100),
        )
    }

    private fun toolButton(label: String, onClick: (() -> Unit)? = null): Button =
        Button(this).apply {
            text = label
            textSize = 11f
            minWidth = 0
            minimumWidth = 0
            onClick?.let { handler -> setOnClickListener { handler() } }
        }

    private fun captureScreenshot() {
        val service = VisualTaskerAccessibilityService.current()
        if (service == null) {
            setStatus("Accessibility nicht aktiv")
            showFloatingInspector()
            return
        }
        val target = File(filesDir, "emscript-runtime/screenshots/overlay-${timestamp()}.png")
        target.parentFile?.mkdirs()
        setStatus("Screenshot laeuft...")
        serviceScope.launch {
            val ok = service.takeScreenshotTo(target)
            setStatus(
                if (ok) {
                    "Gespeichert: ${target.name}"
                } else {
                    "Screenshot fehlgeschlagen"
                }
            )
        }
    }

    private fun setStatus(message: String) {
        statusView?.text = message
    }

    private fun overlayStatusText(): String =
        if (VisualTaskerAccessibilityService.current() == null) {
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
            setPadding(dp(12), dp(8), dp(8), dp(8))
            setBackgroundColor(Color.argb(210, 38, 42, 54))
        }
        val titleText = TextView(this).apply {
            text = title
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setTextColor(Color.rgb(120, 210, 255))
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val closeButton = Button(this).apply {
            text = "✕"
            textSize = 10f
            minWidth = 0
            minimumWidth = 0
            setPadding(dp(8), dp(2), dp(8), dp(2))
            setOnClickListener { removeOverlay(key) }
        }
        titleBar.addView(titleText)
        titleBar.addView(closeButton)

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(12), dp(12), dp(12))
        }

        container.addView(titleBar)
        container.addView(content)

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
            resizeHandle = resizeHandle,
        )
    }

    private fun addOverlay(
        key: String,
        root: View,
        dragHandle: View,
        resizeHandle: View?,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        minWidth: Int,
        minHeight: Int,
    ) {
        val params = WindowManager.LayoutParams(
            width,
            height,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            this.x = x
            this.y = y
        }

        installDragHandler(dragHandle, root, params)
        if (resizeHandle != null) {
            installResizeHandler(
                resizeHandle = resizeHandle,
                root = root,
                params = params,
                minWidth = minWidth,
                minHeight = minHeight,
            )
        }
        windowManager.addView(root, params)
        overlays[key] = OverlayHandle(root, params)
    }

    private fun removeOverlay(key: String) {
        val handle = overlays.remove(key) ?: return
        runCatching { windowManager.removeView(handle.root) }
        if (overlays.isEmpty()) {
            stopSelf()
        }
    }

    private fun installDragHandler(
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
                        lastX = event.rawX
                        lastY = event.rawY
                        dragging = false
                        return false
                    }

                    MotionEvent.ACTION_MOVE -> {
                        val dx = event.rawX - lastX
                        val dy = event.rawY - lastY
                        if (abs(dx) > 4f || abs(dy) > 4f) {
                            dragging = true
                            params.x += dx.toInt()
                            params.y += dy.toInt()
                            windowManager.updateViewLayout(root, params)
                            lastX = event.rawX
                            lastY = event.rawY
                            return true
                        }
                    }
                }
                return dragging
            }
        })
    }

    private fun installResizeHandler(
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
                        return true
                    }

                    MotionEvent.ACTION_UP,
                    MotionEvent.ACTION_CANCEL,
                    -> {
                        resizing = false
                        return true
                    }
                }
                return false
            }
        })
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).roundToInt()

    private data class OverlayShell(
        val root: LinearLayout,
        val header: View,
        val content: LinearLayout,
        val resizeHandle: View?,
    )

    private data class OverlayHandle(
        val root: View,
        val params: WindowManager.LayoutParams,
    )
}
