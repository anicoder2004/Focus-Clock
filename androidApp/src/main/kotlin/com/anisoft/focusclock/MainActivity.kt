package com.anisoft.focusclock

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.anisoft.focusclock.data.AlarmDatabase
import com.anisoft.focusclock.data.AlarmRepository
import com.anisoft.focusclock.data.AlarmRepositoryImpl
import com.anisoft.focusclock.data.SettingsRepository
import com.anisoft.focusclock.data.SettingsRepositoryImpl
import com.anisoft.focusclock.scheduler.AlarmSchedulerImpl
import com.anisoft.focusclock.scheduler.AudioPlayer
import com.anisoft.focusclock.scheduler.FilePicker
import com.anisoft.focusclock.scheduler.FilePickerBridge
import com.anisoft.focusclock.scheduler.FloatingWindowManager
import com.anisoft.focusclock.scheduler.PlatformAlarmScheduler
import com.anisoft.focusclock.scheduler.ScreenManager
import com.anisoft.focusclock.util.PermissionBridge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class MainActivity : ComponentActivity() {
    private val requestOverlayPermissionLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        // Handle overlay permission result
    }

    private val pickAudioLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        FilePickerBridge.onAudioPicked(uri)
    }

    private val pickExeLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        FilePickerBridge.onExePicked(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        appContext = applicationContext

        FilePickerBridge.audioLauncher = pickAudioLauncher
        FilePickerBridge.exeLauncher = pickExeLauncher

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            requestOverlayPermissionLauncher.launch(intent)
        }

        setContent {
            val scope = remember { CoroutineScope(SupervisorJob() + Dispatchers.Main) }

            val driver: SqlDriver = remember {
                AndroidSqliteDriver(AlarmDatabase.Schema, this, "focus_alarm.db")
            }

            val repository: AlarmRepository = remember { AlarmRepositoryImpl(driver) }
            val settingsRepository: SettingsRepository = remember { SettingsRepositoryImpl(driver) }
            val platformScheduler = remember { PlatformAlarmScheduler() }
            val audioPlayer = remember { AudioPlayer() }
            val floatingWindowManager = remember { FloatingWindowManager() }
            val screenManager = remember { ScreenManager() }
            val filePicker = remember { FilePicker() }

            val scheduler = remember {
                AlarmSchedulerImpl(
                    repository = repository,
                    platformScheduler = platformScheduler,
                    audioPlayer = audioPlayer,
                    floatingWindowManager = floatingWindowManager,
                    screenManager = screenManager,
                    scope = scope
                )
            }

            LaunchedEffect(Unit) {
                scheduler.start()
            }

            App(
                repository = repository,
                settingsRepository = settingsRepository,
                platformScheduler = platformScheduler,
                audioPlayer = audioPlayer,
                floatingWindowManager = floatingWindowManager,
                screenManager = screenManager,
                filePicker = filePicker,
                exactAlarmEnabled = PermissionBridge.canScheduleExactAlarms(),
                onRequestExactAlarmPermission = { PermissionBridge.requestExactAlarmPermission() }
            )
        }
    }
}
