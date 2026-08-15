package com.anisoft.focusclock.scheduler

expect class FilePicker {
    suspend fun pickExeFile(): String?
    suspend fun pickAudioFile(): String?
    fun isAudioFormatSupported(filePath: String): Boolean
    val supportedAudioExtensions: List<String>
}
