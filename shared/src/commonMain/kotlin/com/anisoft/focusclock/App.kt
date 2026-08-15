package com.anisoft.focusclock

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.anisoft.focusclock.data.AlarmModel
import com.anisoft.focusclock.data.AlarmRepository
import com.anisoft.focusclock.data.SettingsKeys
import com.anisoft.focusclock.data.SettingsRepository
import com.anisoft.focusclock.scheduler.AlarmSchedulerImpl
import com.anisoft.focusclock.scheduler.AudioPlayer
import com.anisoft.focusclock.scheduler.FilePicker
import com.anisoft.focusclock.scheduler.FloatingWindowManager
import com.anisoft.focusclock.scheduler.PlatformAlarmScheduler
import com.anisoft.focusclock.scheduler.ScreenManager
import com.anisoft.focusclock.ui.AlarmListScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@Composable
fun App(
    repository: AlarmRepository,
    settingsRepository: SettingsRepository,
    platformScheduler: PlatformAlarmScheduler,
    audioPlayer: AudioPlayer,
    floatingWindowManager: FloatingWindowManager,
    screenManager: ScreenManager,
    filePicker: FilePicker,
    exactAlarmEnabled: Boolean? = null,
    onRequestExactAlarmPermission: () -> Unit = {}
) {
    val scope = remember { CoroutineScope(SupervisorJob() + Dispatchers.Main) }
    val scheduler = remember { AlarmSchedulerImpl(
        repository = repository,
        platformScheduler = platformScheduler,
        audioPlayer = audioPlayer,
        floatingWindowManager = floatingWindowManager,
        screenManager = screenManager,
        scope = scope
    ) }
    
    val alarms by repository.getAllAlarms().collectAsState(initial = emptyList())

    var isDarkTheme by remember { mutableStateOf(false) }
    var defaultAudioPath by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        isDarkTheme = settingsRepository.getString(SettingsKeys.DARK_THEME) == "true"
        defaultAudioPath = settingsRepository.getString(SettingsKeys.DEFAULT_AUDIO)?.takeIf { it.isNotBlank() }
    }

    MaterialTheme(colorScheme = if (isDarkTheme) darkColorScheme() else lightColorScheme()) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            AlarmListScreen(
                alarms = alarms,
                onAlarmClick = { /* handle click */ },
                onAlarmToggle = { alarm, enabled -> scope.launch { scheduler.toggleAlarm(alarm.id, enabled) } },
                onAlarmDelete = { id -> scope.launch { scheduler.deleteAlarm(id) } },
                onAddAlarm = { /* handled by dialog */ },
                coroutineScope = scope,
                filePicker = filePicker,
                audioPlayer = audioPlayer,
                scheduler = scheduler,
                isDarkTheme = isDarkTheme,
                onToggleDarkTheme = { enabled ->
                    isDarkTheme = enabled
                    settingsRepository.setString(SettingsKeys.DARK_THEME, enabled.toString())
                },
                defaultAudioPath = defaultAudioPath,
                onSetDefaultAudio = { path ->
                    defaultAudioPath = path
                    settingsRepository.setString(SettingsKeys.DEFAULT_AUDIO, path ?: "")
                },
                exactAlarmEnabled = exactAlarmEnabled,
                onRequestExactAlarmPermission = onRequestExactAlarmPermission
            )
        }
    }
}

@Composable
fun AppPreview() {
    MaterialTheme {
        Surface {
            Text("Focus Clock Preview")
        }
    }
}