package com.anisoft.focusclock.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anisoft.focusclock.data.AlarmModel
import com.anisoft.focusclock.scheduler.AudioPlayer
import com.anisoft.focusclock.scheduler.FilePicker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmListScreen(
    alarms: List<AlarmModel>,
    onAlarmClick: (AlarmModel) -> Unit,
    onAlarmToggle: (AlarmModel, Boolean) -> Unit,
    onAlarmDelete: (String) -> Unit,
    onAddAlarm: () -> Unit,
    coroutineScope: CoroutineScope,
    filePicker: FilePicker,
    audioPlayer: AudioPlayer,
    scheduler: com.anisoft.focusclock.scheduler.AlarmSchedulerImpl,
    isDarkTheme: Boolean,
    onToggleDarkTheme: (Boolean) -> Unit,
    defaultAudioPath: String?,
    onSetDefaultAudio: (String?) -> Unit,
    exactAlarmEnabled: Boolean? = null,
    onRequestExactAlarmPermission: () -> Unit = {}
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editingAlarm by remember { mutableStateOf<AlarmModel?>(null) }
    var showSettings by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Focus Clock",
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    IconButton(onClick = { showSettings = true }) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true; onAddAlarm() }) {
                Icon(Icons.Default.Add, contentDescription = "Add Alarm")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (alarms.isEmpty()) {
                Text(
                    text = "No alarms set. Tap + to add one.",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    textAlign = TextAlign.Center
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(alarms) { alarm ->
                        AlarmItemCard(
                            alarm = alarm,
                            onToggle = { enabled -> onAlarmToggle(alarm, enabled) },
                            onEdit = { editingAlarm = alarm },
                            onDelete = { onAlarmDelete(alarm.id) },
                            onTest = { coroutineScope.launch { audioPlayer.playAlarm(alarm.audioPath ?: "default") } }
                        )
                    }
                }
            }
        }
    }

    if (editingAlarm != null || showAddDialog) {
        AddAlarmDialog(
            onDismiss = {
                editingAlarm = null
                showAddDialog = false
            },
            onSave = { alarm ->
                coroutineScope.launch {
                    if (editingAlarm != null) {
                        scheduler.updateAlarm(alarm)
                    } else {
                        scheduler.addAlarm(alarm)
                    }
                }
                editingAlarm = null
                showAddDialog = false
            },
            existingAlarm = editingAlarm,
            filePicker = filePicker,
            coroutineScope = coroutineScope,
            defaultAudioPath = defaultAudioPath
        )
    }

    if (showSettings) {
        SettingsDialog(
            isDarkTheme = isDarkTheme,
            onToggleDarkTheme = onToggleDarkTheme,
            defaultAudioPath = defaultAudioPath,
            onSetDefaultAudio = onSetDefaultAudio,
            filePicker = filePicker,
            coroutineScope = coroutineScope,
            onDismiss = { showSettings = false },
            exactAlarmEnabled = exactAlarmEnabled,
            onRequestExactAlarmPermission = onRequestExactAlarmPermission
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmItemCard(
    alarm: AlarmModel,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onTest: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = alarm.time.time.toString().substring(0, 5),
                        fontSize = 28.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                    Text(
                        text = recurrenceDisplayName(alarm.recurrence),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (alarm.note.isNotBlank()) {
                        Text(
                            text = alarm.note,
                            fontSize = 14.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Switch(
                        checked = alarm.isEnabled,
                        onCheckedChange = onToggle
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(onClick = onTest) {
                            Icon(Icons.Default.Edit, contentDescription = "Test Sound")
                        }
                        IconButton(onClick = onEdit) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit")
                        }
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

