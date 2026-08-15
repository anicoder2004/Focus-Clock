package com.anisoft.focusclock.scheduler

import android.content.Context
import android.content.Intent
import com.anisoft.focusclock.appContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

actual class ScreenManager {
    private val context: Context get() = appContext

    actual suspend fun minimizeAllWindows() = withContext(Dispatchers.IO) {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        context.startActivity(intent)
    }

    actual suspend fun launchApplication(exePath: String) = withContext(Dispatchers.IO) {
        // Not applicable on Android - apps are launched via intents, not executable paths
        // This is a no-op on Android
    }
}