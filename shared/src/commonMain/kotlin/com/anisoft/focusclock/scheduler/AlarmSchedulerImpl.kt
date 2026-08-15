@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.anisoft.focusclock.scheduler

import com.anisoft.focusclock.data.AlarmModel
import com.anisoft.focusclock.data.AlarmRepository
import com.anisoft.focusclock.data.Recurrence
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

class AlarmSchedulerImpl(
    private val repository: AlarmRepository,
    private val platformScheduler: PlatformAlarmScheduler,
    private val audioPlayer: AudioPlayer,
    private val floatingWindowManager: FloatingWindowManager,
    private val screenManager: ScreenManager,
    private val scope: CoroutineScope
) {
    private val activeAlarms = mutableMapOf<String, Job>()

    fun start() {
        scope.launch {
            loadAndScheduleEnabledAlarms()
        }
    }

    fun stop() {
        activeAlarms.values.forEach { it.cancel() }
        activeAlarms.clear()
    }

    suspend fun addAlarm(alarm: AlarmModel) {
        repository.insertAlarm(alarm)
        if (alarm.isEnabled) {
            scheduleAlarmInternal(alarm)
        }
    }

    suspend fun updateAlarm(alarm: AlarmModel) {
        cancelAlarmInternal(alarm.id)
        repository.updateAlarm(alarm)
        if (alarm.isEnabled) {
            scheduleAlarmInternal(alarm)
        }
    }

    suspend fun deleteAlarm(alarmId: String) {
        cancelAlarmInternal(alarmId)
        repository.deleteAlarm(alarmId)
    }

    suspend fun toggleAlarm(alarmId: String, enabled: Boolean) {
        val alarm = repository.getAlarm(alarmId)?.copy(isEnabled = enabled)
        alarm?.let {
            if (enabled) {
                scheduleAlarmInternal(it)
            } else {
                cancelAlarmInternal(alarmId)
            }
            repository.updateAlarm(it)
        }
    }

    suspend fun snoozeAlarm(alarm: AlarmModel, minutes: Int) {
        val now = Clock.System.now()
        val snoozeUntil = now.plus(minutes.minutes)
            .toLocalDateTime(TimeZone.currentSystemDefault())
        repository.logTrigger(alarm.id, now.toLocalDateTime(TimeZone.currentSystemDefault()), snoozeUntil)

        val snoozedAlarm = alarm.copy(time = snoozeUntil, recurrence = Recurrence.None)
        scheduleAlarmInternal(snoozedAlarm)
    }

    private suspend fun loadAndScheduleEnabledAlarms() {
        val alarms = repository.getEnabledAlarms().first()
        alarms.forEach { alarm ->
            scheduleAlarmInternal(alarm)
        }
    }

    private suspend fun scheduleAlarmInternal(alarm: AlarmModel) {
        platformScheduler.scheduleAlarm(alarm)
        val job = scope.launch(Dispatchers.IO) {
            try {
                val triggerTime = alarm.nextTrigger()
                triggerTime?.let { time ->
                    val delayMs = time.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds() -
                        Clock.System.now().toEpochMilliseconds()
                    if (delayMs > 0) {
                        delay(delayMs)
                    }
                    triggerAlarm(alarm)
                }
            } catch (e: Exception) {
                println("Error scheduling alarm ${alarm.id}: ${e.message}")
            }
        }
        activeAlarms[alarm.id] = job
    }

    private suspend fun triggerAlarm(alarm: AlarmModel) {
        if (!TriggerDedup.shouldTrigger(alarm.id)) return
        println("Triggering alarm: ${alarm.id}")
        activeAlarms.remove(alarm.id)

        repository.logTrigger(alarm.id, Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()))

        audioPlayer.playAlarm(alarm.audioPath ?: "default")
        screenManager.minimizeAllWindows()
        alarm.targetExePath?.let { screenManager.launchApplication(it) }

        floatingWindowManager.showFloatingNote(
            alarm,
            onSnooze = { minutes -> scope.launch { snoozeAlarm(alarm, minutes) } },
            onDismiss = {
                scope.launch {
                    audioPlayer.stopAlarm()
                    floatingWindowManager.hideFloatingNote()
                    if (alarm.recurrence != Recurrence.None) {
                        val nextAlarm = alarm.copy(time = alarm.nextTrigger()!!)
                        repository.updateAlarm(nextAlarm)
                        scheduleAlarmInternal(nextAlarm)
                    }
                }
            }
        )
    }

    private suspend fun cancelAlarmInternal(alarmId: String) {
        activeAlarms[alarmId]?.cancel()
        activeAlarms.remove(alarmId)
        platformScheduler.cancelAlarm(alarmId)
    }
}
