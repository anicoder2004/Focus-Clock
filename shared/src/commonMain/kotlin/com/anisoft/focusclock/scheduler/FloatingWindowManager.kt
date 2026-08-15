package com.anisoft.focusclock.scheduler

import com.anisoft.focusclock.data.AlarmModel

expect class FloatingWindowManager {
    suspend fun showFloatingNote(
        alarm: AlarmModel,
        onSnooze: (Int) -> Unit,
        onDismiss: () -> Unit
    )
    suspend fun hideFloatingNote()
    fun isShowing(): Boolean
}
