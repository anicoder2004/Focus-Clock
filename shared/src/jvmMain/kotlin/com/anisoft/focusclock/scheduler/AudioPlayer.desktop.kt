package com.anisoft.focusclock.scheduler

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import javax.sound.sampled.FloatControl
import java.io.File

actual class AudioPlayer {
    private var clip: Clip? = null
    private var isPlaying = false

    actual suspend fun playAlarm(audioPath: String, maxDurationSeconds: Int) = withContext(Dispatchers.IO) {
        stopAlarm()
        try {
            val file = if (audioPath == "default") {
                AudioSystem.getAudioInputStream(this::class.java.getResource("/sounds/alarm.wav"))
            } else {
                AudioSystem.getAudioInputStream(File(audioPath))
            }
            clip = AudioSystem.getClip()
            clip?.open(file)
            clip?.loop(Clip.LOOP_CONTINUOUSLY)
            
            val gainControl = clip?.getControl(FloatControl.Type.MASTER_GAIN) as? FloatControl
            gainControl?.value = 6.0f
            
            clip?.start()
            isPlaying = true
            
            Thread.sleep((maxDurationSeconds * 1000L).toLong())
            stopAlarm()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    actual suspend fun stopAlarm() = withContext(Dispatchers.IO) {
        clip?.stop()
        clip?.close()
        clip = null
        isPlaying = false
    }

    actual fun isPlaying(): Boolean = isPlaying
}