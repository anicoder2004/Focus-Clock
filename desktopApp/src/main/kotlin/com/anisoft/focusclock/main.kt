package com.anisoft.focusclock

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.anisoft.focusclock.data.AlarmDatabase
import com.anisoft.focusclock.data.AlarmRepository
import com.anisoft.focusclock.data.AlarmRepositoryImpl
import com.anisoft.focusclock.data.SettingsRepository
import com.anisoft.focusclock.data.SettingsRepositoryImpl
import com.anisoft.focusclock.scheduler.AlarmSchedulerImpl
import com.anisoft.focusclock.scheduler.AudioPlayer
import com.anisoft.focusclock.scheduler.FilePicker
import com.anisoft.focusclock.scheduler.FloatingWindowManager
import com.anisoft.focusclock.scheduler.PlatformAlarmScheduler
import com.anisoft.focusclock.scheduler.ScreenManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.awt.AWTException
import java.awt.MenuItem
import java.awt.PopupMenu
import java.awt.SystemTray
import java.awt.Toolkit
import java.awt.TrayIcon

class DesktopApp {
    private var trayIcon: TrayIcon? = null
    var onShowRequested: () -> Unit = {}

    @Composable
    fun Content() {
        val scope = remember { CoroutineScope(SupervisorJob() + Dispatchers.Main) }

        val driver: SqlDriver = remember {
            JdbcSqliteDriver("jdbc:sqlite:focus_alarm.db").apply {
                AlarmDatabase.Schema.create(this)
            }
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
            exactAlarmEnabled = null,
            onRequestExactAlarmPermission = {}
        )
    }

    fun setupSystemTray() {
        if (!SystemTray.isSupported()) return

        val tray = SystemTray.getSystemTray()
        val image = Toolkit.getDefaultToolkit().createImage(this::class.java.getResource("/icons/tray_icon.png"))
        trayIcon = TrayIcon(image, "Focus Clock", createTrayMenu())
        trayIcon?.isImageAutoSize = true

        try {
            tray.add(trayIcon!!)
        } catch (e: AWTException) {
            e.printStackTrace()
        }
    }

    private fun createTrayMenu(): PopupMenu {
        val menu = PopupMenu()

        val showItem = MenuItem("Show")
        showItem.addActionListener { onShowRequested() }
        menu.add(showItem)

        val exitItem = MenuItem("Exit")
        exitItem.addActionListener { System.exit(0) }
        menu.add(exitItem)

        return menu
    }
}

fun main() = application {
    val app = DesktopApp()
    var isVisible by remember { mutableStateOf(true) }
    app.onShowRequested = { isVisible = true }

    app.setupSystemTray()

    Window(
        onCloseRequest = {
            isVisible = false
        },
        title = "Focus Clock",
        state = rememberWindowState(width = 420.dp, height = 640.dp),
        visible = isVisible
    ) {
        app.Content()
    }
}