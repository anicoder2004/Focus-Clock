package com.anisoft.focusclock.data

import app.cash.sqldelight.db.SqlDriver
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object SettingsKeys {
    const val DARK_THEME = "dark_theme"
    const val DEFAULT_AUDIO = "default_audio"
}

interface SettingsRepository {
    fun getString(key: String): String?
    fun setString(key: String, value: String)
    fun observeString(key: String): Flow<String?>
}

class SettingsRepositoryImpl(private val driver: SqlDriver) : SettingsRepository {
    private val database = AlarmDatabase(driver)
    private val cache = mutableMapOf<String, MutableStateFlow<String?>>()

    override fun getString(key: String): String? {
        return database.alarmDatabaseQueries.selectSetting(key).executeAsOneOrNull()
    }

    override fun setString(key: String, value: String) {
        database.alarmDatabaseQueries.insertSetting(key, value_ = value)
        cache[key]?.value = value
    }

    override fun observeString(key: String): Flow<String?> {
        return cache.getOrPut(key) { MutableStateFlow(getString(key)) }.asStateFlow()
    }
}