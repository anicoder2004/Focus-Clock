package com.anisoft.focusclock.scheduler

import com.anisoft.focusclock.data.AlarmModel
import kotlinx.datetime.LocalDateTime

expect class PlatformAlarmScheduler {
    suspend fun scheduleAlarm(alarm: AlarmModel)
    suspend fun cancelAlarm(alarmId: String)
    suspend fun rescheduleAlarm(alarm: AlarmModel, newTriggerTime: LocalDateTime)
    fun getNextAlarmTime(alarmId: String): LocalDateTime?
}
