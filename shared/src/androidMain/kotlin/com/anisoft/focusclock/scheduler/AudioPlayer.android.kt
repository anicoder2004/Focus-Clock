package com.anisoft.focusclock.scheduler

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import com.anisoft.focusclock.appContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.IOException

actual class AudioPlayer {
    private val context: Context get() = appContext
    private var mediaPlayer: MediaPlayer? = null
    private var playbackJob: Job? = null
    private var isPlaying = false

    actual suspend fun playAlarm(audioPath: String, maxDurationSeconds: Int) {
        stopAlarm()

        playbackJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                val uri = if (audioPath.isBlank() || audioPath == "default") {
                    android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI
                } else {
                    Uri.parse(audioPath)
                }
                mediaPlayer = MediaPlayer().apply {
                    val audioAttributes = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                    setAudioAttributes(audioAttributes)
                    setLooping(true)
                    if (audioPath.startsWith("/") || audioPath.startsWith("file:")) {
                        setDataSource(audioPath)
                    } else {
                        setDataSource(context, uri)
                    }
                    prepare()
                    start()
                }
                isPlaying = true

                delay((maxDurationSeconds * 1000L).toLong())
                stopAlarm()
            } catch (e: IOException) {
                e.printStackTrace()
                // Fallback to default system alarm sound
                try {
                    mediaPlayer = MediaPlayer().apply {
                        val audioAttributes = AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                        setAudioAttributes(audioAttributes)
                        setLooping(true)
                        setDataSource(context, android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI)
                        prepare()
                        start()
                    }
                    isPlaying = true
                    delay((maxDurationSeconds * 1000L).toLong())
                    stopAlarm()
                } catch (e2: IOException) {
                    e2.printStackTrace()
                }
            }
        }
    }

    actual suspend fun stopAlarm() {
        playbackJob?.cancel()
        playbackJob = null
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        isPlaying = false
    }

    actual fun isPlaying(): Boolean = isPlaying
}
