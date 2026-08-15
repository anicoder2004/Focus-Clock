package com.anisoft.focusclock.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.anisoft.focusclock.appContext
import com.anisoft.focusclock.data.AlarmModel
import com.anisoft.focusclock.receiver.AlarmReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

private const val FALLBACK_WINDOW_MS = 10L * 60 * 1000

actual class PlatformAlarmScheduler {
    private val context: Context get() = appContext
    private val alarmManager: AlarmManager
        get() = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    actual suspend fun scheduleAlarm(alarm: AlarmModel) = withContext(Dispatchers.IO) {
        val triggerTime = alarm.nextTrigger()?.toInstant(TimeZone.currentSystemDefault())?.toEpochMilliseconds()
            ?: return@withContext
        
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("alarm_id", alarm.id)
            putExtra("alarm_note", alarm.note)
            putExtra("alarm_audio", alarm.audioPath ?: "")
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
        } catch (e: SecurityException) {
            alarmManager.setWindow(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                FALLBACK_WINDOW_MS,
                pendingIntent
            )
        }
    }

    actual suspend fun cancelAlarm(alarmId: String) = withContext(Dispatchers.IO) {
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    actual suspend fun rescheduleAlarm(alarm: AlarmModel, newTriggerTime: LocalDateTime) = withContext(Dispatchers.IO) {
        cancelAlarm(alarm.id)
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("alarm_id", alarm.id)
            putExtra("alarm_note", alarm.note)
            putExtra("alarm_audio", alarm.audioPath ?: "")
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val triggerMillis = newTriggerTime.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
            }
        } catch (e: SecurityException) {
            alarmManager.setWindow(
                AlarmManager.RTC_WAKEUP,
                triggerMillis,
                FALLBACK_WINDOW_MS,
                pendingIntent
            )
        }
    }

    actual fun getNextAlarmTime(alarmId: String): LocalDateTime? = null
}