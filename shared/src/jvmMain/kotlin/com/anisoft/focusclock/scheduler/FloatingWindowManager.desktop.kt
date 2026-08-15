package com.anisoft.focusclock.scheduler

import com.anisoft.focusclock.data.AlarmModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.Font
import java.awt.GridLayout
import java.awt.Point
import java.awt.Toolkit
import javax.swing.JButton
import javax.swing.JComboBox
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants

actual class FloatingWindowManager {
    private var currentWindow: java.awt.Frame? = null
    private var isShowing = false

    actual suspend fun showFloatingNote(
        alarm: AlarmModel,
        onSnooze: (Int) -> Unit,
        onDismiss: () -> Unit
    ) = withContext(Dispatchers.IO) {
        if (isShowing) return@withContext
        isShowing = true

        val frame = java.awt.Frame().apply {
            isUndecorated = true
            isAlwaysOnTop = true
            title = "Focus Clock"
            layout = BorderLayout()
            size = Dimension(420, 200)
            isResizable = false
        }

        val panel = JPanel().apply {
            layout = GridLayout(4, 1)
            background = Color.DARK_GRAY
        }

        val titleLabel = JLabel("Focus Clock", SwingConstants.CENTER).apply {
            foreground = Color.WHITE
            font = Font("SansSerif", Font.BOLD, 20)
        }

        val noteLabel = JLabel(alarm.note.ifEmpty { "Time to focus!" }, SwingConstants.CENTER).apply {
            foreground = Color.WHITE
            font = Font("SansSerif", Font.PLAIN, 18)
        }

        val snoozeOptions = arrayOf("5 min", "10 min", "15 min", "30 min")
        val snoozeCombo = JComboBox(snoozeOptions)
        val snoozeButton = JButton("Snooze").apply {
            addActionListener {
                val minutes = when (snoozeCombo.selectedIndex) {
                    1 -> 10
                    2 -> 15
                    3 -> 30
                    else -> 5
                }
                onSnooze(minutes)
                frame.dispose()
                this@FloatingWindowManager.isShowing = false
            }
        }

        val snoozeRow = JPanel().apply {
            layout = java.awt.FlowLayout()
            background = Color.DARK_GRAY
            add(snoozeCombo)
            add(snoozeButton)
        }

        val dismissButton = JButton("Dismiss").apply {
            addActionListener {
                onDismiss()
                frame.dispose()
                this@FloatingWindowManager.isShowing = false
            }
        }

        panel.add(titleLabel)
        panel.add(noteLabel)
        panel.add(snoozeRow)
        panel.add(dismissButton)
        frame.add(panel, BorderLayout.CENTER)

        val screenSize = Toolkit.getDefaultToolkit().screenSize
        frame.setLocation(Point((screenSize.width - frame.width) / 2, 0))

        frame.isVisible = true
        currentWindow = frame
    }

    actual suspend fun hideFloatingNote() {
        currentWindow?.dispose()
        currentWindow = null
        isShowing = false
    }

    actual fun isShowing(): Boolean = isShowing
}