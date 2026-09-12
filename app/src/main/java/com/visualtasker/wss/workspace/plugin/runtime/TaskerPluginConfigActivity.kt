package com.visualtasker.wss.workspace.plugin.runtime

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.visualtasker.wss.ui.theme.MultiPanelTheme

class TaskerPluginConfigActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val initial = TaskerPluginBundleParser.parse(
            intent.getBundleExtra(TaskerPluginContract.EXTRA_BUNDLE),
        )
        setContent {
            MultiPanelTheme(themeMode = "dark") {
                TaskerPluginConfigScreen(
                    initial = initial,
                    onSave = { action ->
                        val bundle = TaskerPluginContract.buildBundle(
                            command = action.command,
                            eventName = action.eventName,
                            message = action.message,
                            workspace = action.workspace,
                            script = action.script,
                            runId = action.runId,
                            status = action.status,
                            eventSlot = action.eventSlot,
                        )
                        setResult(
                            Activity.RESULT_OK,
                            Intent()
                                .putExtra(TaskerPluginContract.EXTRA_BUNDLE, bundle)
                                .putExtra(TaskerPluginContract.EXTRA_BLURB, TaskerPluginContract.blurb(bundle)),
                        )
                        finish()
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskerPluginConfigScreen(
    initial: TaskerPluginAction,
    onSave: (TaskerPluginAction) -> Unit,
) {
    var command by remember { mutableStateOf(initial.command) }
    var eventName by remember { mutableStateOf(initial.eventName) }
    var message by remember { mutableStateOf(initial.message) }
    var workspace by remember { mutableStateOf(initial.workspace) }
    var script by remember { mutableStateOf(initial.script) }
    var runId by remember { mutableStateOf(initial.runId) }
    var status by remember { mutableStateOf(initial.status) }
    var eventSlot by remember { mutableStateOf(initial.eventSlot) }
    var commandExpanded by remember { mutableStateOf(false) }
    var statusExpanded by remember { mutableStateOf(false) }
    var slotExpanded by remember { mutableStateOf(false) }
    val commandLabel = TaskerPluginContract.commandLabels.firstOrNull { it.first == command }?.second ?: command
    val statusLabel = TaskerPluginContract.statusLabels.firstOrNull { it.first == status }?.second ?: status
    val slotLabel = TaskerPluginContract.eventSlotLabels.firstOrNull { it.first == eventSlot }?.second ?: eventSlot
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("VisualTasker WSS Tasker Action") },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Diese Action sendet einen strukturierten Tasker-Impuls an RailTrace/WSS.",
                style = MaterialTheme.typography.bodyMedium,
            )
            ExposedDropdownMenuBox(
                expanded = commandExpanded,
                onExpandedChange = { commandExpanded = !commandExpanded },
            ) {
                OutlinedTextField(
                    value = commandLabel,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Aktion") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(commandExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                )
                ExposedDropdownMenu(
                    expanded = commandExpanded,
                    onDismissRequest = { commandExpanded = false },
                ) {
                    TaskerPluginContract.commandLabels.forEach { (value, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                command = value
                                commandExpanded = false
                            },
                        )
                    }
                }
            }
            ExposedDropdownMenuBox(
                expanded = slotExpanded,
                onExpandedChange = { slotExpanded = !slotExpanded },
            ) {
                OutlinedTextField(
                    value = slotLabel,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Event-Slot") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(slotExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                )
                ExposedDropdownMenu(
                    expanded = slotExpanded,
                    onDismissRequest = { slotExpanded = false },
                ) {
                    TaskerPluginContract.eventSlotLabels.forEach { (value, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                eventSlot = value
                                slotExpanded = false
                            },
                        )
                    }
                }
            }
            ExposedDropdownMenuBox(
                expanded = statusExpanded,
                onExpandedChange = { statusExpanded = !statusExpanded },
            ) {
                OutlinedTextField(
                    value = statusLabel,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Status") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(statusExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                )
                ExposedDropdownMenu(
                    expanded = statusExpanded,
                    onDismissRequest = { statusExpanded = false },
                ) {
                    TaskerPluginContract.statusLabels.forEach { (value, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                status = value
                                statusExpanded = false
                            },
                        )
                    }
                }
            }
            OutlinedTextField(
                value = eventName,
                onValueChange = { eventName = it },
                label = { Text("Event-Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = message,
                onValueChange = { message = it },
                label = { Text("Nachricht") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = workspace,
                onValueChange = { workspace = it },
                label = { Text("Workspace/Projekt optional") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = runId,
                onValueChange = { runId = it },
                label = { Text("Run-ID optional") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = script,
                onValueChange = { script = it },
                label = { Text("Script-Draft optional") },
                minLines = 4,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            )
            Spacer(Modifier.height(4.dp))
            Button(
                onClick = {
                    onSave(
                        TaskerPluginAction(
                            command = command,
                            eventName = eventName.ifBlank { "Tasker Event" },
                            message = message.ifBlank { "Tasker Plugin ausgelöst" },
                            workspace = workspace,
                            script = script,
                            runId = runId,
                            status = status,
                            eventSlot = eventSlot,
                        ),
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("In Tasker speichern")
            }
        }
    }
}
