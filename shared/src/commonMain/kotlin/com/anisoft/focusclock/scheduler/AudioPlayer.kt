package com.anisoft.focusclock.scheduler

expect class AudioPlayer {
    suspend fun playAlarm(audioPath: String, maxDurationSeconds: Int = 55)
    suspend fun stopAlarm()
    fun isPlaying(): Boolean
}
