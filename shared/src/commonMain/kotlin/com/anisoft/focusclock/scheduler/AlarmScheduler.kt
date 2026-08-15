package com.anisoft.focusclock.scheduler

import com.anisoft.focusclock.data.AlarmModel

interface AlarmScheduler {
    suspend fun scheduleAlarm(alarm: AlarmModel)
    suspend fun cancelAlarm(alarmId: String)
    suspend fun rescheduleAlarm(alarm: AlarmModel, newTriggerTime: kotlinx.datetime.LocalDateTime)
    fun getNextAlarmTime(alarmId: String): kotlinx.datetime.LocalDateTime?
}