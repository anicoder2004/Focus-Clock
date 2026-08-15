package com.anisoft.focusclock.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anisoft.focusclock.scheduler.FilePicker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun SettingsDialog(
    isDarkTheme: Boolean,
    onToggleDarkTheme: (Boolean) -> Unit,
    defaultAudioPath: String?,
    onSetDefaultAudio: (String?) -> Unit,
    filePicker: FilePicker,
    coroutineScope: CoroutineScope,
    onDismiss: () -> Unit,
    exactAlarmEnabled: Boolean? = null,
    onRequestExactAlarmPermission: () -> Unit = {}
) {
    var showAudioError by remember { mutableStateOf(false) }
    var audioErrorMessage by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Settings", fontSize = 20.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Dark Theme", fontSize = 16.sp)
                    Switch(checked = isDarkTheme, onCheckedChange = onToggleDarkTheme)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Default Alarm Sound", fontSize = 16.sp)
                    Button(onClick = {
                        coroutineScope.launch(Dispatchers.IO) {
                            val path = filePicker.pickAudioFile()
                            if (path != null) {
                                if (filePicker.isAudioFormatSupported(path)) {
                                    onSetDefaultAudio(path)
                                } else {
                                    val supported = filePicker.supportedAudioExtensions.joinToString(", ").uppercase()
                                    audioErrorMessage = "Invalid audio type. Please select $supported audio files only."
                                    showAudioError = true
                                }
                            }
                        }
                    }) {
                        Text(
                            if (defaultAudioPath.isNullOrBlank()) "Select Audio" else "Change Audio",
                            fontSize = 14.sp
                        )
                    }
                }

                Text(
                    text = "Plays for max 55 seconds — longer audio is cut off at 55s.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                )

                val currentAudio = defaultAudioPath
                if (currentAudio?.isNotBlank() == true) {
                    Text(currentAudio, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onClick = { onSetDefaultAudio(null) }) { Text("Clear default sound") }
                }

                if (showAudioError) {
                    Text(audioErrorMessage, fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                }

                exactAlarmEnabled?.let { enabled ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Exact alarms", fontSize = 16.sp)
                        if (enabled) {
                            Text("On", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            Button(onClick = onRequestExactAlarmPermission) {
                                Text("Enable", fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}