@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.anisoft.focusclock.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anisoft.focusclock.data.AlarmModel
import com.anisoft.focusclock.data.Recurrence
import com.anisoft.focusclock.data.generateAlarmId
import com.anisoft.focusclock.scheduler.FilePicker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAlarmDialog(
    onDismiss: () -> Unit,
    onSave: (AlarmModel) -> Unit,
    existingAlarm: AlarmModel? = null,
    filePicker: FilePicker,
    coroutineScope: CoroutineScope,
    defaultAudioPath: String? = null
) {
    var time by remember { mutableStateOf(existingAlarm?.time?.time ?: Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).time) }
    var recurrence by remember { mutableStateOf(existingAlarm?.recurrence ?: Recurrence.None) }
    var note by remember { mutableStateOf(existingAlarm?.note ?: "") }
    var audioPath by remember {
        mutableStateOf(if (existingAlarm != null) existingAlarm.audioPath ?: "" else defaultAudioPath ?: "")
    }
    var exePath by remember { mutableStateOf(existingAlarm?.targetExePath ?: "") }
    var showRecurrenceMenu by remember { mutableStateOf(false) }
    var showAudioPicker by remember { mutableStateOf(false) }
    var showExePicker by remember { mutableStateOf(false) }
    var showAudioError by remember { mutableStateOf(false) }
    var audioErrorMessage by remember { mutableStateOf("") }

    val recurrenceOptions = listOf(
        Recurrence.None,
        Recurrence.Daily,
        Recurrence.Weekdays,
        Recurrence.CustomDays(setOf(1, 2, 3, 4, 5))
    )

    val supportedFormatsText = filePicker.supportedAudioExtensions.joinToString(", ").uppercase()

    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = if (existingAlarm != null) "Edit Alarm" else "Add Alarm",
                fontSize = 24.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )

            // Time Picker (simplified - using text field for now)
            OutlinedTextField(
                value = time.toString(),
                onValueChange = { time = kotlinx.datetime.LocalTime.parse(it) },
                label = { Text("Alarm Time (HH:mm)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                visualTransformation = VisualTransformation { text -> TransformedText(text, OffsetMapping.Identity) }
            )

            // Recurrence Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Recurrence", fontSize = 16.sp)
                Box {
                    Button(
                        onClick = { showRecurrenceMenu = true },
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(recurrenceDisplayName(recurrence), fontSize = 16.sp)
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
                        }
                    }
                    DropdownMenu(
                        expanded = showRecurrenceMenu,
                        onDismissRequest = { showRecurrenceMenu = false }
                    ) {
                        recurrenceOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(recurrenceDisplayName(option), fontSize = 16.sp) },
                                onClick = {
                                    recurrence = option
                                    showRecurrenceMenu = false
                                }
                            )
                        }
                    }
                }
            }

            // Note Input
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Note (optional)") },
                modifier = Modifier.fillMaxWidth()
            )

            // Audio Picker
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Alarm Sound", fontSize = 16.sp)
                Button(onClick = { 
                    showAudioPicker = true
                    showAudioError = false
                }) {
                    Text(if (audioPath.isBlank()) "Select Audio" else "Change Audio", fontSize = 14.sp)
                }
            }

            Text(
                text = "Plays for max 55 seconds — longer audio is cut off at 55s.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )

            if (audioPath.isNotBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(audioPath, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
                    if (showAudioError) {
                        Icon(Icons.Default.Warning, contentDescription = "Error", tint = MaterialTheme.colorScheme.error, modifier = Modifier.padding(start = 8.dp))
                    }
                }
                
                if (showAudioError) {
                    Text(
                        text = audioErrorMessage,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                    )
                }
            }

            // Exe Picker (Desktop only)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Launch App (.exe)", fontSize = 16.sp)
                Button(onClick = { showExePicker = true }) {
                    Text(if (exePath.isBlank()) "Select .exe" else "Change App", fontSize = 14.sp)
                }
            }

            if (exePath.isNotBlank()) {
                Text(exePath, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(onClick = onDismiss) {
                    Text("Cancel")
                }
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(8.dp))
                Button(onClick = {
                    val alarm = AlarmModel(
                        id = existingAlarm?.id ?: generateAlarmId(),
                        time = LocalDateTime(
                            Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date,
                            time
                        ),
                        recurrence = recurrence,
                        note = note,
                        audioPath = audioPath.ifBlank { null },
                        targetExePath = exePath.ifBlank { null },
                        isEnabled = true
                    )
                    onSave(alarm)
                }, colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )) {
                    Text("Save")
                }
            }
        }
    }

    // Audio Error Dialog
    if (showAudioError) {
        AlertDialog(
            onDismissRequest = { 
                showAudioError = false
                audioPath = ""
            },
            confirmButton = {
                Button(onClick = { 
                    showAudioError = false
                    audioPath = ""
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                Button(onClick = { 
                    showAudioError = false
                    audioPath = ""
                }) {
                    Text("Cancel")
                }
            },
            title = { Text("Invalid Audio Format") },
            text = { Text(audioErrorMessage) }
        )
    }

    // Handle audio picker
    if (showAudioPicker) {
        coroutineScope.launch(Dispatchers.IO) {
            val path = filePicker.pickAudioFile()
            kotlinx.coroutines.runBlocking {
                if (path != null) {
                    if (filePicker.isAudioFormatSupported(path)) {
                        audioPath = path
                        showAudioError = false
                    } else {
                        val supportedFormats = filePicker.supportedAudioExtensions.joinToString(", ").uppercase()
                        audioErrorMessage = "Invalid audio type. Please select $supportedFormats audio files only."
                        showAudioError = true
                        audioPath = ""
                    }
                }
                showAudioPicker = false
            }
        }
    }

    // Handle exe picker
    if (showExePicker) {
        coroutineScope.launch(Dispatchers.IO) {
            val path = filePicker.pickExeFile()
            kotlinx.coroutines.runBlocking {
                exePath = path ?: ""
                showExePicker = false
            }
        }
    }
}

fun recurrenceDisplayName(recurrence: Recurrence): String = when (recurrence) {
    is Recurrence.None -> "Once"
    is Recurrence.Daily -> "Daily"
    is Recurrence.Weekdays -> "Weekdays"
    is Recurrence.CustomDays -> "Custom: ${recurrence.days.toList().sorted().joinToString(", ")}"
}