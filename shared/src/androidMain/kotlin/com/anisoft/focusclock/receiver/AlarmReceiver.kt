@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.anisoft.focusclock.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.anisoft.focusclock.data.AlarmDatabase
import com.anisoft.focusclock.data.AlarmRepository
import com.anisoft.focusclock.data.AlarmRepositoryImpl
import com.anisoft.focusclock.data.Recurrence
import com.anisoft.focusclock.scheduler.OverlayService
import com.anisoft.focusclock.scheduler.PlatformAlarmScheduler
import com.anisoft.focusclock.scheduler.TriggerDedup
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration.Companion.minutes

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d("AlarmReceiver", "Received action: $action")

        if (action == "SNOOZE") {
            handleSnooze(context, intent)
            return
        }

        val alarmId = intent.getStringExtra("alarm_id") ?: return
        if (!TriggerDedup.shouldTrigger(alarmId)) return

        val note = intent.getStringExtra("alarm_note") ?: ""
        val audioPath = intent.getStringExtra("alarm_audio") ?: ""

        val serviceIntent = Intent(context, OverlayService::class.java).apply {
            putExtra("alarm_id", alarmId)
            putExtra("alarm_note", note)
            putExtra("alarm_audio", audioPath)
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }

    private fun handleSnooze(context: Context, intent: Intent) {
        val alarmId = intent.getStringExtra("alarm_id") ?: return
        val snoozeMinutes = intent.getIntExtra("snooze_minutes", 5)
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val driver = AndroidSqliteDriver(AlarmDatabase.Schema, context, "focus_alarm.db")
                val repository: AlarmRepository = AlarmRepositoryImpl(driver)
                val now = Clock.System.now()
                val snoozeUntil = now.plus(snoozeMinutes.minutes)
                    .toLocalDateTime(TimeZone.currentSystemDefault())
                val alarm = repository.getAlarm(alarmId)
                if (alarm != null) {
                    val snoozed = alarm.copy(time = snoozeUntil, recurrence = Recurrence.None)
                    PlatformAlarmScheduler().scheduleAlarm(snoozed)
                    repository.logTrigger(alarm.id, now.toLocalDateTime(TimeZone.currentSystemDefault()), snoozeUntil)
                }
            } catch (e: Exception) {
                Log.e("AlarmReceiver", "Snooze reschedule failed", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_LOCKED_BOOT_COMPLETED
        ) {
            return
        }
        Log.d("BootReceiver", "Boot completed - rescheduling enabled alarms")
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val driver = AndroidSqliteDriver(AlarmDatabase.Schema, context, "focus_alarm.db")
                val repository: AlarmRepository = AlarmRepositoryImpl(driver)
                val enabledAlarms = repository.getEnabledAlarms().first()
                val scheduler = PlatformAlarmScheduler()
                enabledAlarms.forEach { alarm -> scheduler.scheduleAlarm(alarm) }
            } catch (e: Exception) {
                Log.e("BootReceiver", "Reschedule failed", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}