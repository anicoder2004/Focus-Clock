@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.anisoft.focusclock.scheduler

import kotlin.time.Clock

object TriggerDedup {
    private val recent = mutableMapOf<String, Long>()
    private const val WINDOW_MS = 60_000L

    fun shouldTrigger(id: String): Boolean {
        val now = Clock.System.now().toEpochMilliseconds()
        val last = recent[id]
        if (last != null && now - last < WINDOW_MS) return false
        recent[id] = now
        return true
    }
}