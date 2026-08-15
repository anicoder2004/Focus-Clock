package com.anisoft.focusclock.scheduler

import com.anisoft.focusclock.data.AlarmModel
import kotlinx.datetime.LocalDateTime

actual class PlatformAlarmScheduler {
    actual suspend fun scheduleAlarm(alarm: AlarmModel) {
    }

    actual suspend fun cancelAlarm(alarmId: String) {
    }

    actual suspend fun rescheduleAlarm(alarm: AlarmModel, newTriggerTime: LocalDateTime) {
    }

    actual fun getNextAlarmTime(alarmId: String): LocalDateTime? = null
}