@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.anisoft.focusclock.scheduler

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import com.anisoft.focusclock.appContext
import com.anisoft.focusclock.data.AlarmModel
import com.anisoft.focusclock.receiver.AlarmReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

actual class FloatingWindowManager {
    private val context: Context get() = appContext
    private val scope = CoroutineScope(Dispatchers.Main)
    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var isShowing = false

    actual suspend fun showFloatingNote(
        alarm: AlarmModel,
        onSnooze: (Int) -> Unit,
        onDismiss: () -> Unit
    ) {
        if (isShowing) return
        isShowing = true

        windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
                or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                or WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }

        overlayView = buildNoteView(
            alarm = alarm,
            onSnooze = {
                onSnooze(it)
                scope.launch { hideFloatingNote() }
            },
            onDismiss = {
                onDismiss()
                scope.launch { hideFloatingNote() }
            }
        )
        windowManager?.addView(overlayView!!, layoutParams)
    }

    private fun buildNoteView(
        alarm: AlarmModel,
        onSnooze: (Int) -> Unit,
        onDismiss: () -> Unit
    ): View {
        val dp = context.resources.displayMetrics.density
        fun px(value: Int): Int = (value * dp).toInt()

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(px(16), px(16), px(16), px(16))
            setBackgroundColor(Color.DKGRAY)
            gravity = Gravity.CENTER_HORIZONTAL
        }

        val title = TextView(context).apply {
            text = "Focus Clock"
            setTextColor(Color.WHITE)
            textSize = 20f
            setTypeface(typeface, Typeface.BOLD)
        }

        val note = TextView(context).apply {
            text = alarm.note.ifEmpty { "Time to focus!" }
            setTextColor(Color.WHITE)
            textSize = 18f
        }

        val snoozeOptions = arrayOf("5 min", "10 min", "15 min", "30 min")
        val snoozeSpinner = Spinner(context).apply {
            adapter = object : ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, snoozeOptions) {
                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val view = super.getView(position, convertView, parent)
                    (view as? TextView)?.setTextColor(Color.WHITE)
                    return view
                }
            }.also {
                it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
        }

        val snoozeButton = Button(context).apply {
            text = "Snooze"
            setOnClickListener {
                val minutes = when (snoozeSpinner.selectedItemPosition) {
                    1 -> 10
                    2 -> 15
                    3 -> 30
                    else -> 5
                }
                onSnooze(minutes)
            }
        }

        val dismissButton = Button(context).apply {
            text = "Dismiss"
            setOnClickListener { onDismiss() }
        }

        val buttons = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_HORIZONTAL
            addView(snoozeSpinner)
            addView(snoozeButton)
            addView(dismissButton)
        }

        container.addView(title)
        container.addView(note)
        container.addView(buttons)
        return container
    }

    actual suspend fun hideFloatingNote() {
        windowManager?.let { wm ->
            overlayView?.let { view ->
                try {
                    wm.removeView(view)
                } catch (e: IllegalArgumentException) {
                    // View was not attached
                }
            }
        }
        overlayView = null
        windowManager = null
        isShowing = false
    }

    actual fun isShowing(): Boolean = isShowing
}

class OverlayService : Service() {
    private var windowManager: FloatingWindowManager? = null
    private val scope = CoroutineScope(Dispatchers.Main)
    private var audioPlayer: AudioPlayer? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = FloatingWindowManager()
        audioPlayer = AudioPlayer()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) return START_NOT_STICKY
        val alarmId = intent.getStringExtra("alarm_id") ?: run { stopSelf(); return START_NOT_STICKY }
        val note = intent.getStringExtra("alarm_note") ?: ""
        val audioPath = intent.getStringExtra("alarm_audio") ?: ""

        acquireWakeLock()
        startForeground(1, buildNotification("Focus Clock", note.ifEmpty { "Time to focus!" }))

        val alarm = AlarmModel(
            id = alarmId,
            time = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
            note = note,
            audioPath = audioPath.ifBlank { null }
        )

        scope.launch {
            audioPlayer?.playAlarm(audioPath.ifBlank { "default" })
        }
        scope.launch {
            try {
                ScreenManager().minimizeAllWindows()
            } catch (e: Exception) {
                // ignore - overlay still shows
            }
        }

        scope.launch {
            windowManager?.showFloatingNote(
                alarm,
                onSnooze = { minutes ->
                    val snoozeIntent = Intent(this@OverlayService, AlarmReceiver::class.java).apply {
                        action = "SNOOZE"
                        putExtra("alarm_id", alarmId)
                        putExtra("snooze_minutes", minutes)
                    }
                    sendBroadcast(snoozeIntent)
                    stopSelf()
                },
                onDismiss = {
                    stopSelf()
                }
            )
        }

        return START_NOT_STICKY
    }

    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "focus_clock:alarm").apply {
            setReferenceCounted(false)
            acquire(10L * 60 * 1000)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Alarm Notifications",
                NotificationManager.IMPORTANCE_HIGH
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(title: String, text: String): Notification {
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            Notification.Builder(this)
        }
        return builder
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        scope.launch { windowManager?.hideFloatingNote() }
        scope.launch { audioPlayer?.stopAlarm() }
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            stopForeground(Service.STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private companion object {
        const val CHANNEL_ID = "focus_clock_alarm"
    }
}
