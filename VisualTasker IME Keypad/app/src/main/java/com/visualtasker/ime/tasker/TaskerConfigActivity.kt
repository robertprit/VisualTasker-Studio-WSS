package com.visualtasker.ime.tasker

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast

class TaskerConfigActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
        }

        val input = EditText(this).apply {
            hint = "Text für Tasker Aktion"
        }
        val save = Button(this).apply { text = "Speichern" }

        container.addView(input)
        container.addView(save)
        setContentView(container)

        save.setOnClickListener {
            val text = input.text?.toString()?.trim().orEmpty()
            if (text.isBlank()) {
                Toast.makeText(this, "Text darf nicht leer sein", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val result = Intent().apply {
                putExtra(
                    "com.twofortyfouram.locale.intent.extra.BUNDLE",
                    Bundle().apply { putString(TaskerActionReceiver.EXTRA_TEXT, text) }
                )
                putExtra("com.twofortyfouram.locale.intent.extra.BLURB", text.take(30))
            }
            setResult(RESULT_OK, result)
            finish()
        }
    }
}
