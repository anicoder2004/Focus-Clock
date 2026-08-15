package com.anisoft.focusclock.scheduler

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.Robot
import java.awt.event.KeyEvent
import java.lang.ProcessBuilder

actual class ScreenManager {
    actual suspend fun minimizeAllWindows() = withContext(Dispatchers.IO) {
        try {
            val robot = Robot()
            robot.keyPress(KeyEvent.VK_WINDOWS)
            robot.keyPress(KeyEvent.VK_D)
            robot.keyRelease(KeyEvent.VK_D)
            robot.keyRelease(KeyEvent.VK_WINDOWS)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    actual suspend fun launchApplication(exePath: String) {
        withContext(Dispatchers.IO) {
            try {
                ProcessBuilder(exePath).start()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}