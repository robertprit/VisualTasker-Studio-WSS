package com.visualtasker.ime

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.KeyEvent
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Switch
import android.widget.Toast
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import com.visualtasker.ime.storage.PrefsStore

class MainActivity : AppCompatActivity() {
    private lateinit var prefs: PrefsStore
    private lateinit var keycodeOptions: List<KeyCodeItem>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        prefs = PrefsStore(this)
        keycodeOptions = buildAndroidKeycodeList()

        findViewById<Button>(R.id.openSettingsButton).setOnClickListener {
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        }

        val darkSwitch = findViewById<Switch>(R.id.darkKeyboardSwitch)
        val argbSwitch = findViewById<Switch>(R.id.colorArgbSwitch)
        val clipboardSwitch = findViewById<Switch>(R.id.clipboardHistorySwitch)
        val emscriptSwitch = findViewById<Switch>(R.id.emscriptModeSwitch)
        val visionSwitch = findViewById<Switch>(R.id.visionTaskerModeSwitch)
        val s210 = findViewById<Spinner>(R.id.spinnerSpecial210)
        val s211 = findViewById<Spinner>(R.id.spinnerSpecial211)
        val s212 = findViewById<Spinner>(R.id.spinnerSpecial212)
        val s213 = findViewById<Spinner>(R.id.spinnerSpecial213)
        val vtSlot1Label = findViewById<EditText>(R.id.vtSlot1Label)
        val vtSlot1Script = findViewById<EditText>(R.id.vtSlot1Script)
        val vtSlot2Label = findViewById<EditText>(R.id.vtSlot2Label)
        val vtSlot2Script = findViewById<EditText>(R.id.vtSlot2Script)

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            keycodeOptions.map { it.label }
        )
        listOf(s210, s211, s212, s213).forEach { it.adapter = adapter }

        darkSwitch.isChecked = prefs.isDarkKeyboardEnabled()
        argbSwitch.isChecked = prefs.isColorInsertModeArgb()
        clipboardSwitch.isChecked = prefs.isClipboardHistoryEnabled()
        emscriptSwitch.isChecked = prefs.isEmscriptContextEnabled()
        visionSwitch.isChecked = prefs.isVisionTaskerContextEnabled()

        setSpinnerSelection(s210, prefs.getSpecialKeyMapping(-210, KeyEvent.KEYCODE_F1))
        setSpinnerSelection(s211, prefs.getSpecialKeyMapping(-211, KeyEvent.KEYCODE_F2))
        setSpinnerSelection(s212, prefs.getSpecialKeyMapping(-212, KeyEvent.KEYCODE_F3))
        setSpinnerSelection(s213, prefs.getSpecialKeyMapping(-213, KeyEvent.KEYCODE_F4))
        val slot1 = prefs.getVtKeySlot(1)
        val slot2 = prefs.getVtKeySlot(2)
        vtSlot1Label.setText(slot1.label)
        vtSlot1Script.setText(slot1.script)
        vtSlot2Label.setText(slot2.label)
        vtSlot2Script.setText(slot2.script)

        findViewById<Button>(R.id.saveSettingsButton).setOnClickListener {
            prefs.setDarkKeyboardEnabled(darkSwitch.isChecked)
            prefs.setColorInsertModeArgb(argbSwitch.isChecked)
            prefs.setClipboardHistoryEnabled(clipboardSwitch.isChecked)
            prefs.setEmscriptContextEnabled(emscriptSwitch.isChecked)
            prefs.setVisionTaskerContextEnabled(visionSwitch.isChecked)
            prefs.setSpecialKeyMapping(-210, selectedKeyCode(s210))
            prefs.setSpecialKeyMapping(-211, selectedKeyCode(s211))
            prefs.setSpecialKeyMapping(-212, selectedKeyCode(s212))
            prefs.setSpecialKeyMapping(-213, selectedKeyCode(s213))
            prefs.setVtKeySlot(1, vtSlot1Label.text.toString(), vtSlot1Script.text.toString())
            prefs.setVtKeySlot(2, vtSlot2Label.text.toString(), vtSlot2Script.text.toString())
            Toast.makeText(this, "Einstellungen gespeichert", Toast.LENGTH_SHORT).show()
        }
    }

    private fun selectedKeyCode(spinner: Spinner): Int {
        return keycodeOptions.getOrNull(spinner.selectedItemPosition)?.keyCode ?: KeyEvent.KEYCODE_UNKNOWN
    }

    private fun setSpinnerSelection(spinner: Spinner, keyCode: Int) {
        val index = keycodeOptions.indexOfFirst { it.keyCode == keyCode }
        spinner.setSelection(if (index >= 0) index else 0)
    }

    private fun buildAndroidKeycodeList(): List<KeyCodeItem> {
        val items = mutableListOf<KeyCodeItem>()
        items += KeyCodeItem("KEYCODE_UNKNOWN (0)", KeyEvent.KEYCODE_UNKNOWN)

        val fields = KeyEvent::class.java.fields
            .filter { it.name.startsWith("KEYCODE_") && it.type == Int::class.javaPrimitiveType }
            .sortedBy { it.name }

        for (field in fields) {
            val code = runCatching { field.getInt(null) }.getOrNull() ?: continue
            items += KeyCodeItem("${field.name} ($code)", code)
        }
        return items.distinctBy { it.keyCode }
    }
}

data class KeyCodeItem(
    val label: String,
    val keyCode: Int
)
