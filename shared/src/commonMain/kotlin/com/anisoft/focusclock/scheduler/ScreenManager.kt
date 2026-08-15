package com.anisoft.focusclock.scheduler

expect class ScreenManager {
    suspend fun minimizeAllWindows()
    suspend fun launchApplication(exePath: String)
}
