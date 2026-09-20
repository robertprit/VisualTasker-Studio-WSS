package com.visualtasker.ime.ime

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.inputmethodservice.InputMethodService
import android.inputmethodservice.Keyboard
import android.inputmethodservice.KeyboardView
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.ContextThemeWrapper
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.SoundEffectConstants
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import com.visualtasker.ime.R
import com.visualtasker.ime.feature.ExpressionEvaluator
import com.visualtasker.ime.ime.actions.ActionRouter
import com.visualtasker.ime.ime.actions.ImeCommand
import com.visualtasker.ime.ime.context.ContextEngine
import com.visualtasker.ime.ime.context.EditorContext
import com.visualtasker.ime.ime.context.KeyboardLayout
import com.visualtasker.ime.ime.context.KeyboardProfile
import com.visualtasker.ime.ime.context.ResolvedKeyboardProfile
import com.visualtasker.ime.ime.gestures.GestureEngine
import com.visualtasker.ime.ime.handwriting.DigitalInkHandwritingRecognizer
import com.visualtasker.ime.ime.handwriting.HandwritingCanvasView
import com.visualtasker.ime.ime.input.InputConnectionFacade
import com.visualtasker.ime.ime.security.ImeSecurityPolicy
import com.visualtasker.ime.ime.selection.SelectionAction
import com.visualtasker.ime.ime.selection.SelectionActionEngine
import com.visualtasker.ime.ime.snippets.SnippetDefinition
import com.visualtasker.ime.ime.snippets.SnippetEngine
import com.visualtasker.ime.ime.stylus.AndroidStylusSupport
import com.visualtasker.ime.ime.stylus.StylusMode
import com.visualtasker.ime.ime.toolbar.SwipeToolbarView
import com.visualtasker.ime.ime.toolbar.ToolbarAction
import com.visualtasker.ime.ime.toolbar.ToolbarPageCatalog
import com.visualtasker.ime.ime.vtbridge.IntentVisionTaskerBridge
import com.visualtasker.ime.storage.PrefsStore
import com.visualtasker.ime.tasker.TaskerActionReceiver

@Suppress("DEPRECATION")
class KeyboardImeService : InputMethodService(), KeyboardView.OnKeyboardActionListener {
    private lateinit var keyboardView: KeyboardView
    private lateinit var qwertzKeyboard: Keyboard
    private lateinit var symbolsKeyboard: Keyboard
    private lateinit var functionKeyboard: Keyboard
    private lateinit var keypadKeyboard: Keyboard
    private lateinit var prefsStore: PrefsStore

    private var memoryValue: Double = 0.0
    private var shiftEnabled: Boolean = false
    private var capsLockEnabled: Boolean = false
    private var sheetReplacesKeyboard: Boolean = false
    private var currentEditorContext: EditorContext? = null
    private var currentProfile: ResolvedKeyboardProfile? = null
    private var manualProfileOverride: KeyboardProfile? = null
    private lateinit var inlineToolSheet: LinearLayout
    private lateinit var inlineSheetTitle: TextView
    private lateinit var inlineSheetContent: FrameLayout
    private lateinit var swipeToolbar: SwipeToolbarView
    private lateinit var rightNumpadPanel: LinearLayout
    private lateinit var inputFacade: InputConnectionFacade
    private lateinit var actionRouter: ActionRouter
    private val gestureEngine = GestureEngine()
    private val stylusSupport = AndroidStylusSupport()
    private lateinit var handwritingRecognizer: DigitalInkHandwritingRecognizer

    private val taskerReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val text = intent.getStringExtra(TaskerActionReceiver.EXTRA_TEXT) ?: return
            insertText(text)
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onCreate() {
        super.onCreate()
        prefsStore = PrefsStore(this)
        inputFacade = InputConnectionFacade(
            connectionProvider = { currentInputConnection },
            editorInfoProvider = { currentInputEditorInfo }
        )
        actionRouter = ActionRouter(
            visionTaskerBridge = IntentVisionTaskerBridge(this),
            onInput = { text -> insertText(text) },
            onUiSheet = { routeUiSheet(it) }
        )
        handwritingRecognizer = DigitalInkHandwritingRecognizer()
        val filter = IntentFilter(TaskerActionReceiver.ACTION_TASKER_INSERT_TEXT)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(taskerReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(taskerReceiver, filter)
        }
    }

    override fun onDestroy() {
        unregisterReceiver(taskerReceiver)
        if (::handwritingRecognizer.isInitialized) handwritingRecognizer.close()
        super.onDestroy()
    }

    override fun onCreateInputView(): View {
        val themedContext = ContextThemeWrapper(this, R.style.Theme_ImeKeypad)
        val root = LayoutInflater.from(themedContext).inflate(R.layout.input_view, null)
        keyboardView = root.findViewById(R.id.keyboardView)
        rightNumpadPanel = root.findViewById(R.id.rightNumpadPanel)
        inlineToolSheet = root.findViewById(R.id.inlineToolSheet)
        inlineSheetTitle = root.findViewById(R.id.inlineSheetTitle)
        inlineSheetContent = root.findViewById(R.id.inlineSheetContent)
        swipeToolbar = root.findViewById(R.id.swipeToolbar)

        qwertzKeyboard = Keyboard(this, R.xml.keyboard_qwertz)
        symbolsKeyboard = Keyboard(this, R.xml.keyboard_symbols)
        functionKeyboard = Keyboard(this, R.xml.keyboard_function)
        keypadKeyboard = Keyboard(this, R.xml.keyboard_keypad)

        keyboardView.keyboard = qwertzKeyboard
        keyboardView.setOnKeyboardActionListener(this)
        keyboardView.isPreviewEnabled = false
        applyKeyboardStyle()

        bindIconBar(root)
        bindRightNumpad(root)

        return root
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        stylusSupport.setMode(StylusMode.KEYBOARD)
        applyContextProfile(info)
        prefsStore.consumePendingTaskerText()?.let { insertText(it) }
    }

    override fun onKey(primaryCode: Int, keyCodes: IntArray?) {
        performKeyFeedback()
        when (primaryCode) {
            -5 -> inputFacade.deleteSelectionOrCharacterBeforeCursor()
            -100 -> toggleShift()
            -101 -> switchAltLayer()
            -1 -> switchKeyboard(qwertzKeyboard)
            -2 -> switchKeyboard(symbolsKeyboard)
            -3 -> switchKeyboard(functionKeyboard)
            -4 -> switchKeyboard(keypadKeyboard)
            -20 -> sendKeyDownUp(KeyEvent.KEYCODE_DPAD_UP)
            -21 -> sendKeyDownUp(KeyEvent.KEYCODE_DPAD_DOWN)
            -22 -> sendKeyDownUp(KeyEvent.KEYCODE_DPAD_LEFT)
            -23 -> sendKeyDownUp(KeyEvent.KEYCODE_DPAD_RIGHT)
            -24 -> inputFacade.moveCursorByWord(-1)
            -25 -> inputFacade.moveCursorByWord(1)
            -26 -> inputFacade.moveCursorToLineBoundary(toStart = true)
            -27 -> inputFacade.moveCursorToLineBoundary(toStart = false)
            -28 -> if (!inputFacade.shiftTabSelection()) insertText("\t")
            -200, -201, -202, -203, -204, -205 -> insertMacro(primaryCode + 200)
            -214 -> renderCommandPaletteSheet()
            -400 -> insertText("==")
            -401 -> insertText("!=")
            -402 -> insertText("&&")
            -403 -> insertText("||")
            -404 -> insertText("->")
            -405 -> insertText("?:")
            -406 -> insertText("?.")
            -407 -> insertText("::")
            -420 -> insertText("IF condition THEN\n    \nEND IF")
            -421 -> insertText("click(x, y)")
            -422 -> insertText("swipe(x1, y1, x2, y2, 250)")
            -423 -> insertText("wait(500)")
            -210 -> triggerConfiguredSpecialKey(-210) { renderCalculatorSheet() }
            -211 -> triggerConfiguredSpecialKey(-211) { renderColorSheet() }
            -212 -> triggerConfiguredSpecialKey(-212) { renderClipboardSheet() }
            -213 -> triggerConfiguredSpecialKey(-213) { renderMacrosSheet() }
            in -311..-300 -> sendFunctionKey(primaryCode)
            10 -> inputFacade.performImeActionOrEnter()
            else -> commitCharacter(primaryCode)
        }
    }

    private fun insertMacro(index: Int) {
        val macros = prefsStore.getMacros()
        insertText(macros[index])
    }

    private fun sendFunctionKey(code: Int) {
        val offset = code + 311
        val keyCode = KeyEvent.KEYCODE_F1 + offset
        sendKeyDownUp(keyCode)
    }

    private fun sendKeyDownUp(keyEventCode: Int) {
        currentInputConnection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyEventCode))
        currentInputConnection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyEventCode))
    }

    private fun insertText(text: String) {
        inputFacade.insertText(text)
        if (ImeSecurityPolicy.canPersistClipboard(currentEditorContext)) {
            prefsStore.addClipboardEntry(text)
        }
    }

    private fun switchKeyboard(keyboard: Keyboard) {
        sheetReplacesKeyboard = false
        hideSheet()
        keyboardView.keyboard = keyboard
        if (keyboard != qwertzKeyboard) {
            shiftEnabled = false
            capsLockEnabled = false
            qwertzKeyboard.isShifted = false
        }
        updateKeyboardAreaVisibility()
        keyboardView.invalidateAllKeys()
    }

    private fun applyContextProfile(info: EditorInfo?) {
        val emscriptEnabled = prefsStore.isEmscriptContextEnabled()
        val visionTaskerEnabled = prefsStore.isVisionTaskerContextEnabled()
        currentEditorContext = ContextEngine.resolveContext(info, emscriptEnabled, visionTaskerEnabled)
        val autoProfile = ContextEngine.resolveProfile(
            context = currentEditorContext!!,
            emscriptEnabled = emscriptEnabled,
            visionTaskerEnabled = visionTaskerEnabled
        )
        currentProfile = manualProfileOverride?.let { forced ->
            ResolvedKeyboardProfile(
                profile = forced,
                layout = when (forced) {
                    KeyboardProfile.NORMAL -> KeyboardLayout.QWERTZ
                    KeyboardProfile.DEVELOPER -> KeyboardLayout.FUNCTION
                    KeyboardProfile.EMSCRIPT -> KeyboardLayout.FUNCTION
                    KeyboardProfile.VISIONTASKER -> KeyboardLayout.FUNCTION
                }
            )
        } ?: autoProfile
        val targetKeyboard = when (currentProfile?.layout) {
            KeyboardLayout.KEYPAD -> keypadKeyboard
            KeyboardLayout.SYMBOLS -> symbolsKeyboard
            KeyboardLayout.FUNCTION -> functionKeyboard
            else -> qwertzKeyboard
        }
        switchKeyboard(targetKeyboard)
    }

    private fun setManualProfile(profile: KeyboardProfile?) {
        manualProfileOverride = profile
        applyContextProfile(currentInputEditorInfo)
    }

    private fun toggleShift() {
        val now = System.currentTimeMillis()
        val isDoubleTap = gestureEngine.registerTap(now)
        if (!capsLockEnabled && isDoubleTap) {
            capsLockEnabled = true
            shiftEnabled = true
        } else if (capsLockEnabled) {
            capsLockEnabled = false
            shiftEnabled = false
        } else {
            shiftEnabled = !shiftEnabled
        }
        qwertzKeyboard.isShifted = shiftEnabled
        keyboardView.invalidateAllKeys()
    }

    private fun switchAltLayer() {
        val isQwertz = keyboardView.keyboard == qwertzKeyboard
        switchKeyboard(if (isQwertz) symbolsKeyboard else qwertzKeyboard)
    }

    private fun commitCharacter(primaryCode: Int) {
        var text = primaryCode.toChar().toString()
        if (shiftEnabled && text.length == 1 && text[0].isLetter()) {
            text = text.uppercase()
            if (!capsLockEnabled) {
                shiftEnabled = false
                qwertzKeyboard.isShifted = false
                keyboardView.invalidateAllKeys()
            }
        }
        insertText(text)
    }

    private fun bindIconBar(root: View) {
        val slotLabels = listOf(
            prefsStore.getVtKeySlot(1).label,
            prefsStore.getVtKeySlot(2).label
        )
        swipeToolbar.configure(ToolbarPageCatalog.pages(slotLabels)) { action ->
            performKeyFeedback()
            handleToolbarAction(action)
        }
    }

    private fun handleToolbarAction(action: ToolbarAction) {
        when (action) {
            ToolbarAction.UNDO -> sendCtrlShortcut(KeyEvent.KEYCODE_Z)
            ToolbarAction.REDO -> sendCtrlShortcut(KeyEvent.KEYCODE_Y)
            ToolbarAction.CUT -> currentInputConnection.performContextMenuAction(android.R.id.cut)
            ToolbarAction.COPY -> currentInputConnection.performContextMenuAction(android.R.id.copy)
            ToolbarAction.PASTE -> performPaste()
            ToolbarAction.CLIPBOARD_HISTORY -> renderClipboardSheet()
            ToolbarAction.QWERTZ -> setManualProfile(KeyboardProfile.NORMAL)
            ToolbarAction.SPECIAL_1 -> {
                manualProfileOverride = KeyboardProfile.NORMAL
                switchKeyboard(symbolsKeyboard)
            }
            ToolbarAction.SPECIAL_2 -> {
                manualProfileOverride = KeyboardProfile.DEVELOPER
                switchKeyboard(functionKeyboard)
            }
            ToolbarAction.COLOR_PICKER -> renderColorSheet()
            ToolbarAction.CALCULATOR -> renderCalculatorSheet()
            ToolbarAction.HANDWRITING -> {
                stylusSupport.setMode(StylusMode.HANDWRITING)
                renderHandwritingSheet()
            }
            ToolbarAction.ESCAPE -> sendKeyDownUp(KeyEvent.KEYCODE_ESCAPE)
            ToolbarAction.TAB -> sendKeyDownUp(KeyEvent.KEYCODE_TAB)
            ToolbarAction.HOME -> sendKeyDownUp(KeyEvent.KEYCODE_MOVE_HOME)
            ToolbarAction.END -> sendKeyDownUp(KeyEvent.KEYCODE_MOVE_END)
            ToolbarAction.WORKFLOW_START -> dispatchVtAction("start_workflow", mapOf("name" to "default"))
            ToolbarAction.WORKFLOW_STOP -> dispatchVtAction("stop_workflow")
            ToolbarAction.RECORDER_TOGGLE -> dispatchVtAction("toggle_recorder")
            ToolbarAction.WATCHDOG_TOGGLE -> dispatchVtAction("toggle_watchdog")
            ToolbarAction.VT_SLOT_1 -> dispatchConfiguredVtSlot(1)
            ToolbarAction.VT_SLOT_2 -> dispatchConfiguredVtSlot(2)
            else -> {
                val functionNumber = action.name.removePrefix("F").toIntOrNull() ?: return
                sendKeyDownUp(KeyEvent.KEYCODE_F1 + functionNumber - 1)
            }
        }
    }

    private fun dispatchConfiguredVtSlot(index: Int) {
        val slot = prefsStore.getVtKeySlot(index)
        if (slot.script.isBlank()) {
            Toast.makeText(this, "${slot.label} ist nicht belegt", Toast.LENGTH_SHORT).show()
            return
        }
        dispatchVtAction(
            action = "run_script",
            payload = mapOf(
                "eventSlot" to "slot_$index",
                "message" to "${slot.label} vom VT-Keypad",
                "script" to slot.script
            )
        )
    }

    private fun dispatchVtAction(action: String, payload: Map<String, String> = emptyMap()) {
        if (!ImeSecurityPolicy.canSendToAutomation(currentEditorContext)) {
            Toast.makeText(this, "Sensitive Feld: VT-Aktion blockiert", Toast.LENGTH_SHORT).show()
            return
        }
        val sent = actionRouter.route(ImeCommand.VtCommand(action, payload))
        Toast.makeText(
            this,
            if (sent) "VT-Aktion gesendet" else "VisualTasker nicht erreichbar",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun bindRightNumpad(root: View) {
        setIconAction(root, R.id.numpadMinus) { insertText("-") }
        setIconAction(root, R.id.numpad0) { insertText("0") }
        setIconAction(root, R.id.numpad1) { insertText("1") }
        setIconAction(root, R.id.numpad2) { insertText("2") }
        setIconAction(root, R.id.numpad3) { insertText("3") }
        setIconAction(root, R.id.numpad4) { insertText("4") }
        setIconAction(root, R.id.numpad5) { insertText("5") }
        setIconAction(root, R.id.numpad6) { insertText("6") }
        setIconAction(root, R.id.numpad7) { insertText("7") }
        setIconAction(root, R.id.numpad8) { insertText("8") }
        setIconAction(root, R.id.numpad9) { insertText("9") }
        setIconAction(root, R.id.numpadPlus) { insertText("+") }
    }

    private fun setIconAction(root: View, id: Int, action: () -> Unit) {
        root.findViewById<View>(id).setOnClickListener {
            performKeyFeedback()
            action()
        }
    }

    private fun applyKeyboardStyle() {
        val dark = prefsStore.isDarkKeyboardEnabled()
        keyboardView.setBackgroundColor(if (dark) Color.parseColor("#111111") else Color.parseColor("#F2F2F2"))
    }

    private fun renderCalculatorSheet() {
        val body = verticalContainer()
        val input = EditText(this).apply { hint = "Ausdruck, z.B. (4+5)*2-3%2" }
        val output = TextView(this).apply { textSize = 16f }
        val keypadRows = listOf(
            listOf("sin(", "cos(", "tan(", "sqrt("),
            listOf("7", "8", "9", "/"),
            listOf("4", "5", "6", "*"),
            listOf("1", "2", "3", "-"),
            listOf("0", ".", "(", ")"),
            listOf("%", "^", "pi", "e"),
            listOf("C", "⌫", "=", "+")
        )
        val convertRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val decToBinButton = Button(this).apply { text = "Dec→Bin" }
        val decToHexButton = Button(this).apply { text = "Dec→Hex" }
        val binToDecButton = Button(this).apply { text = "Bin→Dec" }
        val hexToDecButton = Button(this).apply { text = "Hex→Dec" }
        convertRow.addView(decToBinButton, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        convertRow.addView(decToHexButton, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        convertRow.addView(binToDecButton, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        convertRow.addView(hexToDecButton, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        val controlRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val insertButton = Button(this).apply { text = "Einfügen" }
        val mPlusButton = Button(this).apply { text = "M+" }
        val mrButton = Button(this).apply { text = "MR" }
        val mcButton = Button(this).apply { text = "MC" }
        controlRow.addView(insertButton)
        controlRow.addView(mPlusButton)
        controlRow.addView(mrButton)
        controlRow.addView(mcButton)
        body.addView(input)
        keypadRows.forEach { labels ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            labels.forEach { label ->
                val button = Button(this).apply {
                    text = label
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    setOnClickListener {
                        when (label) {
                            "C" -> {
                                input.setText("")
                                output.text = ""
                            }
                            "⌫" -> {
                                val current = input.text?.toString().orEmpty()
                                if (current.isNotEmpty()) input.setText(current.dropLast(1))
                                input.setSelection(input.text?.length ?: 0)
                            }
                            "=" -> evaluateAndShowResult(input, output)
                            else -> input.append(label)
                        }
                    }
                }
                row.addView(button)
            }
            body.addView(row)
        }
        body.addView(convertRow)
        body.addView(controlRow)
        body.addView(output)

        decToBinButton.setOnClickListener {
            convertNumberInput(input, output, 10, 2)
        }
        decToHexButton.setOnClickListener {
            convertNumberInput(input, output, 10, 16)
        }
        binToDecButton.setOnClickListener {
            convertNumberInput(input, output, 2, 10)
        }
        hexToDecButton.setOnClickListener {
            convertNumberInput(input, output, 16, 10)
        }
        insertButton.setOnClickListener {
            val toInsert = evaluateAndShowResult(input, output)?.let { formatNumber(it) }
            if (toInsert.isNullOrBlank()) {
                Toast.makeText(this, "Kein Ergebnis zum Einfügen", Toast.LENGTH_SHORT).show()
            } else {
                insertText(toInsert)
            }
        }
        mPlusButton.setOnClickListener {
            val value = evaluateAndShowResult(input, output) ?: return@setOnClickListener
            memoryValue += value
            Toast.makeText(this, "M = $memoryValue", Toast.LENGTH_SHORT).show()
        }
        mrButton.setOnClickListener { input.append(memoryValue.toString()) }
        mcButton.setOnClickListener {
            memoryValue = 0.0
            Toast.makeText(this, "Speicher gelöscht", Toast.LENGTH_SHORT).show()
        }

        showSheet("Erweiterter Taschenrechner", body, replaceKeyboard = true)
    }

    private fun renderColorSheet() {
        val body = verticalContainer()
        val preview = TextView(this).apply {
            text = "#000000"
            textSize = 18f
            setPadding(12, 12, 12, 12)
        }
        val a = createColorSeekbar("A", body).apply { progress = 255 }
        val r = createColorSeekbar("R", body)
        val g = createColorSeekbar("G", body)
        val b = createColorSeekbar("B", body)
        val insertButton = Button(this).apply { text = "HEX einfügen" }
        body.addView(preview, 0)
        body.addView(insertButton)

        fun refresh() {
            val color = Color.argb(a.progress, r.progress, g.progress, b.progress)
            val hexArgb = String.format("#%02X%02X%02X%02X", a.progress, r.progress, g.progress, b.progress)
            val hexRgb = String.format("#%02X%02X%02X", r.progress, g.progress, b.progress)
            preview.text = "$hexArgb / $hexRgb   (argb ${a.progress}, ${r.progress}, ${g.progress}, ${b.progress})"
            preview.setBackgroundColor(color)
            preview.setTextColor(
                if ((0.299 * r.progress + 0.587 * g.progress + 0.114 * b.progress) > 186) Color.BLACK else Color.WHITE
            )
        }
        val listener = object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) = refresh()
            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        }
        a.setOnSeekBarChangeListener(listener)
        r.setOnSeekBarChangeListener(listener)
        g.setOnSeekBarChangeListener(listener)
        b.setOnSeekBarChangeListener(listener)
        refresh()
        insertButton.setOnClickListener {
            val useArgb = prefsStore.isColorInsertModeArgb()
            val value = if (useArgb) {
                String.format("#%02X%02X%02X%02X", a.progress, r.progress, g.progress, b.progress)
            } else {
                String.format("#%02X%02X%02X", r.progress, g.progress, b.progress)
            }
            insertText(value)
        }

        showSheet("Color Picker", body, replaceKeyboard = true)
    }

    private fun renderHandwritingSheet() {
        stylusSupport.setMode(StylusMode.HANDWRITING)
        val body = verticalContainer()
        val status = TextView(this).apply {
            text = "Deutsches Handschriftmodell wird vorbereitet ..."
            setTextColor(Color.LTGRAY)
        }
        val canvas = HandwritingCanvasView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (170 * resources.displayMetrics.density).toInt()
            )
        }
        val result = TextView(this).apply {
            text = "Noch kein Ergebnis"
            textSize = 18f
            setTextColor(Color.WHITE)
            setPadding(8, 8, 8, 8)
        }
        val candidates = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        val controls = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        val clearButton = Button(this).apply { text = "Löschen" }
        val recognizeButton = Button(this).apply {
            text = "Erkennen"
            isEnabled = false
        }
        val insertButton = Button(this).apply {
            text = "Einfügen"
            isEnabled = false
        }

        var selectedCandidate = ""
        clearButton.setOnClickListener {
            canvas.clearInk()
            candidates.removeAllViews()
            selectedCandidate = ""
            result.text = "Noch kein Ergebnis"
            insertButton.isEnabled = false
        }
        recognizeButton.setOnClickListener {
            if (!canvas.hasInk) {
                status.text = "Bitte zuerst schreiben"
                return@setOnClickListener
            }
            recognizeButton.isEnabled = false
            status.text = "Erkennung läuft ..."
            handwritingRecognizer.recognize(
                ink = canvas.snapshotInk(),
                width = canvas.width.toFloat(),
                height = canvas.height.toFloat(),
                preContext = inputFacade.getTextBeforeCursor(20),
                onResult = { options ->
                    candidates.removeAllViews()
                    selectedCandidate = options.firstOrNull().orEmpty()
                    result.text = selectedCandidate.ifBlank { "Nichts erkannt" }
                    options.forEach { option ->
                        candidates.addView(
                            Button(this).apply {
                                text = option
                                setOnClickListener {
                                    selectedCandidate = option
                                    result.text = option
                                    insertButton.isEnabled = true
                                }
                            },
                            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                        )
                    }
                    insertButton.isEnabled = selectedCandidate.isNotBlank()
                    recognizeButton.isEnabled = true
                    status.text = if (options.isEmpty()) "Keine Übereinstimmung" else "Erkannt"
                },
                onFailure = { error ->
                    recognizeButton.isEnabled = true
                    status.text = "Erkennung fehlgeschlagen: ${error.message ?: "unbekannter Fehler"}"
                }
            )
        }
        insertButton.setOnClickListener {
            if (selectedCandidate.isNotBlank()) {
                insertText(selectedCandidate)
                canvas.clearInk()
                hideSheet()
                stylusSupport.setMode(StylusMode.KEYBOARD)
            }
        }

        controls.addView(clearButton, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        controls.addView(recognizeButton, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        controls.addView(insertButton, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        body.addView(status)
        body.addView(canvas)
        body.addView(result)
        body.addView(candidates)
        body.addView(controls)
        showSheet("Handschrift", body, replaceKeyboard = true)

        handwritingRecognizer.prepare(
            onReady = {
                status.text = "Bereit - mit Finger oder Stift schreiben"
                recognizeButton.isEnabled = true
            },
            onFailure = { error ->
                status.text = "Modell nicht verfügbar: ${error.message ?: "Download fehlgeschlagen"}"
            }
        )
    }

    private fun renderClipboardSheet() {
        val values = prefsStore.getClipboardHistory()
        if (values.isEmpty()) {
            Toast.makeText(this, "Zwischenablage leer", Toast.LENGTH_SHORT).show()
            return
        }
        val body = verticalContainer()
        values.take(12).forEach { value ->
            val button = Button(this).apply {
                text = if (value.length > 48) "${value.take(48)}..." else value
                setOnClickListener { insertText(value) }
            }
            body.addView(button)
        }
        showSheet("Zwischenablage-Verlauf", ScrollView(this).apply { addView(body) })
    }

    private fun renderMacrosSheet() {
        val macros = prefsStore.getMacros()
        val body = verticalContainer()
        val fields = mutableListOf<EditText>()
        repeat(6) { index ->
            val field = EditText(this).apply {
                hint = "Makro M${index + 1}"
                setText(macros[index])
            }
            fields.add(field)
            body.addView(field)
        }
        val saveButton = Button(this).apply {
            text = "Speichern"
            setOnClickListener {
                fields.forEachIndexed { index, editText ->
                    prefsStore.setMacro(index, editText.text?.toString().orEmpty())
                }
                Toast.makeText(this@KeyboardImeService, "Makros gespeichert", Toast.LENGTH_SHORT).show()
            }
        }
        body.addView(saveButton)
        showSheet("Makrotasten konfigurieren", ScrollView(this).apply { addView(body) })
    }

    private fun renderCommandPaletteSheet() {
        val body = verticalContainer()
        val profile = currentProfile?.profile ?: KeyboardProfile.NORMAL
        val profileInfo = TextView(this).apply {
            text = "Profil: ${profile.name}"
            setTextColor(Color.WHITE)
        }
        body.addView(profileInfo)

        val snippetTitle = TextView(this).apply {
            text = "Snippets"
            setTextColor(Color.WHITE)
            textSize = 15f
        }
        body.addView(snippetTitle)
        SnippetEngine.snippetsFor(profile).forEach { snippet ->
            val button = Button(this).apply {
                text = "${snippet.category}: ${snippet.label}"
                setOnClickListener { insertSnippet(snippet) }
            }
            body.addView(button)
        }

        val transformTitle = TextView(this).apply {
            text = "Selection Transform"
            setTextColor(Color.WHITE)
            textSize = 15f
        }
        body.addView(transformTitle)
        val selection = inputFacade.selectedText()
        val selectionActions = if (ImeSecurityPolicy.canAnalyzeSelection(currentEditorContext)) {
            SelectionActionEngine.actionsForSelection(selection, profile)
        } else {
            emptyList()
        }
        if (selectionActions.isEmpty()) {
            body.addView(TextView(this).apply {
                text = "Keine Aktionen. Markiere Text oder wechsle ins Developer/EMScript-Profil."
                setTextColor(Color.LTGRAY)
            })
        } else {
            selectionActions.forEach { action ->
                val button = Button(this).apply {
                    text = action.label
                    setOnClickListener { applySelectionAction(action) }
                }
                body.addView(button)
            }
        }

        val utilityTitle = TextView(this).apply {
            text = "Tools / Bridge"
            setTextColor(Color.WHITE)
            textSize = 15f
        }
        body.addView(utilityTitle)
        body.addView(TextView(this).apply {
            text = "Stylus-Handwriting API: ${if (stylusSupport.supportsHandwriting()) "verfügbar" else "nicht verfügbar"}"
            setTextColor(Color.LTGRAY)
        })
        body.addView(Button(this).apply {
            text = "Stylus-Modus: Handwriting"
            setOnClickListener { renderHandwritingSheet() }
        })
        body.addView(Button(this).apply {
            text = "Stylus-Modus: Keyboard"
            setOnClickListener { stylusSupport.setMode(StylusMode.KEYBOARD) }
        })

        body.addView(Button(this).apply {
            text = "Clipboard öffnen"
            setOnClickListener { actionRouter.route(ImeCommand.UiCommand("clipboard")) }
        })
        body.addView(Button(this).apply {
            text = "IME Action: Next"
            setOnClickListener { inputFacade.performImeAction(EditorInfo.IME_ACTION_NEXT) }
        })
        body.addView(Button(this).apply {
            text = "IME Action: Done"
            setOnClickListener { inputFacade.performImeAction(EditorInfo.IME_ACTION_DONE) }
        })
        body.addView(Button(this).apply {
            text = "IME Action: Search"
            setOnClickListener { inputFacade.performImeAction(EditorInfo.IME_ACTION_SEARCH) }
        })
        body.addView(Button(this).apply {
            text = "IME Action: Send"
            setOnClickListener { inputFacade.performImeAction(EditorInfo.IME_ACTION_SEND) }
        })
        body.addView(Button(this).apply {
            text = "EMScript-Snippets anzeigen"
            setOnClickListener { setManualProfile(KeyboardProfile.EMSCRIPT) }
        })
        body.addView(Button(this).apply {
            text = "Variable einfügen"
            setOnClickListener { actionRouter.route(ImeCommand.InputCommand("\${VAR_NAME}")) }
        })
        body.addView(Button(this).apply {
            text = "Gespeicherten Point wählen (Bridge)"
            setOnClickListener {
                if (!ImeSecurityPolicy.canSendToAutomation(currentEditorContext)) {
                    Toast.makeText(this@KeyboardImeService, "Sensitive Feld: Bridge blockiert", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val sent = actionRouter.route(ImeCommand.VtCommand("select_point", emptyMap()))
                Toast.makeText(
                    this@KeyboardImeService,
                    if (sent) "Bridge-Aufruf gesendet" else "WSS nicht installiert/erreichbar",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
        body.addView(Button(this).apply {
            text = "Gespeicherte Region wählen (Bridge)"
            setOnClickListener {
                if (!ImeSecurityPolicy.canSendToAutomation(currentEditorContext)) {
                    Toast.makeText(this@KeyboardImeService, "Sensitive Feld: Bridge blockiert", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val sent = actionRouter.route(ImeCommand.VtCommand("select_region", emptyMap()))
                Toast.makeText(
                    this@KeyboardImeService,
                    if (sent) "Bridge-Aufruf gesendet" else "WSS nicht installiert/erreichbar",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
        body.addView(Button(this).apply {
            text = "VisionTasker Aktion: OCR (Bridge)"
            setOnClickListener {
                if (!ImeSecurityPolicy.canSendToAutomation(currentEditorContext)) {
                    Toast.makeText(this@KeyboardImeService, "Sensitive Feld: Bridge blockiert", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val sent = actionRouter.route(ImeCommand.VtCommand("ocr", emptyMap()))
                Toast.makeText(
                    this@KeyboardImeService,
                    if (sent) "Bridge-Aufruf gesendet" else "WSS nicht installiert/erreichbar",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
        body.addView(Button(this).apply {
            text = "VisionTasker Aktion: Screenshot (Bridge)"
            setOnClickListener {
                if (!ImeSecurityPolicy.canSendToAutomation(currentEditorContext)) {
                    Toast.makeText(this@KeyboardImeService, "Sensitive Feld: Bridge blockiert", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val sent = actionRouter.route(ImeCommand.VtCommand("screenshot", emptyMap()))
                Toast.makeText(
                    this@KeyboardImeService,
                    if (sent) "Bridge-Aufruf gesendet" else "WSS nicht installiert/erreichbar",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
        body.addView(Button(this).apply {
            text = "VisionTasker Aktion: Recorder Start/Stop"
            setOnClickListener {
                if (!ImeSecurityPolicy.canSendToAutomation(currentEditorContext)) {
                    Toast.makeText(this@KeyboardImeService, "Sensitive Feld: Bridge blockiert", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val sent = actionRouter.route(ImeCommand.VtCommand("toggle_recorder", emptyMap()))
                Toast.makeText(
                    this@KeyboardImeService,
                    if (sent) "Bridge-Aufruf gesendet" else "WSS nicht installiert/erreichbar",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
        body.addView(Button(this).apply {
            text = "VisionTasker Aktion: Workflow starten"
            setOnClickListener {
                if (!ImeSecurityPolicy.canSendToAutomation(currentEditorContext)) {
                    Toast.makeText(this@KeyboardImeService, "Sensitive Feld: Bridge blockiert", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val sent = actionRouter.route(ImeCommand.VtCommand("start_workflow", mapOf("name" to "default")))
                Toast.makeText(
                    this@KeyboardImeService,
                    if (sent) "Bridge-Aufruf gesendet" else "WSS nicht installiert/erreichbar",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })

        showSheet("Command Palette", ScrollView(this).apply { addView(body) }, replaceKeyboard = true)
    }

    private fun insertSnippet(snippet: SnippetDefinition) {
        val selected = inputFacade.selectedText()
        val primaryPlaceholder = snippet.placeholders.firstOrNull()?.key
        val values = if (!primaryPlaceholder.isNullOrBlank() && selected.isNotBlank()) {
            mapOf(primaryPlaceholder to selected)
        } else {
            emptyMap()
        }
        val rendered = SnippetEngine.render(snippet, values)
        if (rendered.selectionStart != null && rendered.selectionEnd != null) {
            inputFacade.replaceSelectionAndSelect(rendered.text, rendered.selectionStart, rendered.selectionEnd)
        } else {
            inputFacade.replaceSelection(rendered.text)
        }
        hideSheet()
    }

    private fun applySelectionAction(action: SelectionAction) {
        if (!ImeSecurityPolicy.canAnalyzeSelection(currentEditorContext)) {
            Toast.makeText(this, "Sensitive Selection ist gesperrt", Toast.LENGTH_SHORT).show()
            return
        }
        val selected = inputFacade.selectedText()
        if (selected.isBlank()) {
            Toast.makeText(this, "Kein Text markiert", Toast.LENGTH_SHORT).show()
            return
        }
        inputFacade.replaceSelection(action.replacement)
        hideSheet()
    }

    private fun routeUiSheet(sheet: String) {
        when (sheet) {
            "clipboard" -> renderClipboardSheet()
            "macros" -> renderMacrosSheet()
            "calculator" -> renderCalculatorSheet()
            "color" -> renderColorSheet()
            else -> renderCommandPaletteSheet()
        }
    }

    private fun showSheet(title: String, content: View, replaceKeyboard: Boolean = false) {
        sheetReplacesKeyboard = replaceKeyboard
        inlineSheetTitle.text = title
        inlineSheetContent.removeAllViews()
        inlineSheetContent.addView(content)
        inlineToolSheet.visibility = View.VISIBLE
        updateKeyboardAreaVisibility()
    }

    private fun hideSheet() {
        if (::inlineToolSheet.isInitialized) {
            inlineToolSheet.visibility = View.GONE
            inlineSheetContent.removeAllViews()
        }
        updateKeyboardAreaVisibility()
    }

    private fun updateKeyboardAreaVisibility() {
        if (!::keyboardView.isInitialized || !::rightNumpadPanel.isInitialized) return
        if (sheetReplacesKeyboard && inlineToolSheet.visibility == View.VISIBLE) {
            keyboardView.visibility = View.GONE
            rightNumpadPanel.visibility = View.GONE
            return
        }
        keyboardView.visibility = View.VISIBLE
        val showNumpad = keyboardView.keyboard == qwertzKeyboard
        rightNumpadPanel.visibility = if (showNumpad) View.VISIBLE else View.GONE
    }

    private fun evaluateAndShowResult(input: EditText, output: TextView): Double? {
        val expression = input.text?.toString().orEmpty()
        if (expression.isBlank()) {
            output.text = "Bitte Ausdruck eingeben"
            return null
        }
        return runCatching { ExpressionEvaluator.evaluate(expression) }
            .onSuccess { output.text = "Ergebnis: ${formatNumber(it)}" }
            .onFailure { output.text = "Ungültiger Ausdruck" }
            .getOrNull()
    }

    private fun convertNumberInput(input: EditText, output: TextView, fromBase: Int, toBase: Int) {
        val raw = input.text?.toString().orEmpty().trim().lowercase()
        if (raw.isBlank()) {
            output.text = "Bitte Zahl eingeben"
            return
        }
        val normalized = when (fromBase) {
            2 -> raw.removePrefix("0b")
            16 -> raw.removePrefix("0x")
            else -> raw
        }
        val value = normalized.toLongOrNull(fromBase)
        if (value == null) {
            output.text = "Ungültige Basis-$fromBase Zahl"
            return
        }
        val result = when (toBase) {
            2 -> value.toString(2)
            16 -> value.toString(16).uppercase()
            else -> value.toString()
        }
        input.setText(result)
        input.setSelection(result.length)
        output.text = "Konvertiert: $result"
    }

    private fun formatNumber(value: Double): String {
        val longValue = value.toLong()
        return if (value == longValue.toDouble()) longValue.toString() else value.toString()
    }

    private fun verticalContainer(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 8, 16, 8)
        }
    }

    private fun createColorSeekbar(label: String, parent: LinearLayout): SeekBar {
        val wrapper = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val title = TextView(this).apply { text = label }
        val seek = SeekBar(this).apply { max = 255 }
        wrapper.addView(title)
        wrapper.addView(seek)
        parent.addView(wrapper)
        return seek
    }

    private fun triggerConfiguredSpecialKey(specialCode: Int, fallback: () -> Unit) {
        val defaultKey = when (specialCode) {
            -210 -> KeyEvent.KEYCODE_F1
            -211 -> KeyEvent.KEYCODE_F2
            -212 -> KeyEvent.KEYCODE_F3
            -213 -> KeyEvent.KEYCODE_F4
            else -> KeyEvent.KEYCODE_UNKNOWN
        }
        val mapped = prefsStore.getSpecialKeyMapping(specialCode, defaultKey)
        if (mapped == KeyEvent.KEYCODE_UNKNOWN) {
            fallback()
        } else {
            sendKeyDownUp(mapped)
        }
    }

    private fun sendCtrlShortcut(keyCode: Int) {
        val down = KeyEvent(0L, 0L, KeyEvent.ACTION_DOWN, keyCode, 0, KeyEvent.META_CTRL_ON)
        val up = KeyEvent(0L, 0L, KeyEvent.ACTION_UP, keyCode, 0, KeyEvent.META_CTRL_ON)
        currentInputConnection.sendKeyEvent(down)
        currentInputConnection.sendKeyEvent(up)
    }

    private fun performPaste() {
        val pastedFromAction = currentInputConnection.performContextMenuAction(android.R.id.paste)
        if (pastedFromAction) return
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val text = clipboard.primaryClip?.getItemAt(0)?.coerceToText(this)?.toString()
        if (!text.isNullOrBlank()) {
            insertText(text)
        }
    }

    private fun performKeyFeedback() {
        if (::keyboardView.isInitialized) {
            keyboardView.playSoundEffect(SoundEffectConstants.CLICK)
        }
        val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager
            manager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }
        vibrator?.let {
            if (it.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    it.vibrate(VibrationEffect.createOneShot(18, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    it.vibrate(18)
                }
            }
        }
    }

    override fun onPress(primaryCode: Int) = Unit
    override fun onRelease(primaryCode: Int) = Unit
    override fun onText(text: CharSequence?) = Unit
    override fun swipeLeft() = Unit
    override fun swipeRight() = Unit
    override fun swipeDown() {
        sheetReplacesKeyboard = false
        hideSheet()
    }
    override fun swipeUp() = Unit
}
