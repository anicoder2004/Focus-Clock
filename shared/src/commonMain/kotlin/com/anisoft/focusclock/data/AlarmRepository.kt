package com.anisoft.focusclock.data

import app.cash.sqldelight.Query
import app.cash.sqldelight.db.SqlDriver
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

interface AlarmRepository {
    suspend fun insertAlarm(alarm: AlarmModel)
    suspend fun updateAlarm(alarm: AlarmModel)
    suspend fun deleteAlarm(id: String)
    suspend fun getAlarm(id: String): AlarmModel?
    fun getAllAlarms(): Flow<List<AlarmModel>>
    fun getEnabledAlarms(): Flow<List<AlarmModel>>
    suspend fun logTrigger(alarmId: String, triggeredAt: LocalDateTime, snoozedUntil: LocalDateTime? = null, dismissed: Boolean = false)
}

class AlarmRepositoryImpl(private val driver: SqlDriver) : AlarmRepository {
    private val database = AlarmDatabase(driver)
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun insertAlarm(alarm: AlarmModel) {
        database.alarmDatabaseQueries.insertAlarm(
            id = alarm.id,
            time = alarm.time.toString(),
            recurrence_type = recurrenceType(alarm.recurrence),
            recurrence_data = recurrenceData(alarm.recurrence, json),
            note = alarm.note,
            audio_path = alarm.audioPath,
            target_exe_path = alarm.targetExePath,
            is_enabled = if (alarm.isEnabled) 1L else 0L,
            created_at = alarm.createdAt.toString()
        )
    }

    override suspend fun updateAlarm(alarm: AlarmModel) {
        database.alarmDatabaseQueries.updateAlarm(
            time = alarm.time.toString(),
            recurrence_type = recurrenceType(alarm.recurrence),
            recurrence_data = recurrenceData(alarm.recurrence, json),
            note = alarm.note,
            audio_path = alarm.audioPath,
            target_exe_path = alarm.targetExePath,
            is_enabled = if (alarm.isEnabled) 1L else 0L,
            id = alarm.id
        )
    }

    override suspend fun deleteAlarm(id: String) {
        database.alarmDatabaseQueries.deleteAlarmById(id)
    }

    override suspend fun getAlarm(id: String): AlarmModel? {
        return database.alarmDatabaseQueries.selectAlarmById(id).executeAsOneOrNull()?.toModel(json)
    }

    override fun getAllAlarms(): Flow<List<AlarmModel>> = callbackFlow {
        val query = database.alarmDatabaseQueries.selectAllAlarms()
        val listener = object : Query.Listener {
            override fun queryResultsChanged() {
                trySend(query.executeAsList().map { it.toModel(json) })
            }
        }
        query.addListener(listener)
        trySend(query.executeAsList().map { it.toModel(json) })
        awaitClose { query.removeListener(listener) }
    }

    override fun getEnabledAlarms(): Flow<List<AlarmModel>> = callbackFlow {
        val query = database.alarmDatabaseQueries.selectEnabledAlarms()
        val listener = object : Query.Listener {
            override fun queryResultsChanged() {
                trySend(query.executeAsList().map { it.toModel(json) })
            }
        }
        query.addListener(listener)
        trySend(query.executeAsList().map { it.toModel(json) })
        awaitClose { query.removeListener(listener) }
    }

    override suspend fun logTrigger(alarmId: String, triggeredAt: LocalDateTime, snoozedUntil: LocalDateTime?, dismissed: Boolean) {
        database.alarmDatabaseQueries.insertTriggerHistory(
            alarm_id = alarmId,
            triggered_at = triggeredAt.toString(),
            snoozed_until = snoozedUntil?.toString(),
            dismissed = if (dismissed) 1L else 0L
        )
    }
}

private fun AlarmEntity.toModel(json: Json): AlarmModel {
    val recurrence = when (recurrence_type) {
        "NONE" -> Recurrence.None
        "DAILY" -> Recurrence.Daily
        "WEEKDAYS" -> Recurrence.Weekdays
        "CUSTOM_DAYS" -> Recurrence.CustomDays(json.decodeFromString(recurrence_data!!))
        else -> Recurrence.None
    }
    return AlarmModel(
        id = id,
        time = LocalDateTime.parse(time),
        recurrence = recurrence,
        note = note,
        audioPath = audio_path,
        targetExePath = target_exe_path,
        isEnabled = is_enabled == 1L,
        createdAt = LocalDateTime.parse(created_at)
    )
}

private fun recurrenceType(recurrence: Recurrence): String = when (recurrence) {
    is Recurrence.None -> "NONE"
    is Recurrence.Daily -> "DAILY"
    is Recurrence.Weekdays -> "WEEKDAYS"
    is Recurrence.CustomDays -> "CUSTOM_DAYS"
}

private fun recurrenceData(recurrence: Recurrence, json: Json): String? = when (recurrence) {
    is Recurrence.CustomDays -> json.encodeToString(recurrence.days)
    else -> null
}
