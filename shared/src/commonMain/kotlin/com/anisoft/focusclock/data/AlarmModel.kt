@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.anisoft.focusclock.data

import kotlin.random.Random
import kotlin.time.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable

@Serializable
data class AlarmModel(
    val id: String = generateAlarmId(),
    val time: LocalDateTime,
    val recurrence: Recurrence = Recurrence.None,
    val note: String = "",
    val audioPath: String? = null,
    val targetExePath: String? = null,
    val isEnabled: Boolean = true,
    val createdAt: LocalDateTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
) {
    fun nextTrigger(after: LocalDateTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())): LocalDateTime? {
        return recurrence.nextTrigger(time, after)
    }
}

fun generateAlarmId(): String {
    val random = Random.nextBytes(16)
    val sb = StringBuilder()
    for (byte in random) {
        sb.append("%02x".format(byte.toInt() and 0xFF))
    }
    return sb.toString()
}

private fun LocalDateTime.plusDays(n: Int): LocalDateTime =
    LocalDate.fromEpochDays(date.toEpochDays() + n).atTime(time)

@Serializable
sealed interface Recurrence {
    fun nextTrigger(baseTime: LocalDateTime, after: LocalDateTime): LocalDateTime?

    @Serializable
    data object None : Recurrence {
        override fun nextTrigger(baseTime: LocalDateTime, after: LocalDateTime): LocalDateTime? {
            val candidate = if (baseTime > after) baseTime else baseTime.plusDays(1)
            return if (candidate > after) candidate else null
        }
    }

    @Serializable
    object Daily : Recurrence {
        override fun nextTrigger(baseTime: LocalDateTime, after: LocalDateTime): LocalDateTime? {
            var candidate = baseTime
            while (candidate <= after) {
                candidate = candidate.plusDays(1)
            }
            return candidate
        }
    }

    @Serializable
    object Weekdays : Recurrence {
        override fun nextTrigger(baseTime: LocalDateTime, after: LocalDateTime): LocalDateTime? {
            var candidate = baseTime
            while (candidate <= after || candidate.dayOfWeek.ordinal >= 5) {
                candidate = candidate.plusDays(1)
            }
            return candidate
        }
    }

    @Serializable
    data class CustomDays(val days: Set<Int>) : Recurrence {
        override fun nextTrigger(baseTime: LocalDateTime, after: LocalDateTime): LocalDateTime? {
            var candidate = baseTime
            val maxIterations = 14
            var iterations = 0
            while (candidate <= after || candidate.dayOfWeek.ordinal !in days) {
                candidate = candidate.plusDays(1)
                iterations++
                if (iterations > maxIterations) return null
            }
            return candidate
        }
    }
}
