package com.anisoft.focusclock.scheduler

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

actual class FilePicker {
    private val desktopSupportedExtensions = listOf("wav", "aiff", "au", "aif")
    private val desktopSupportedExtensionsSet = desktopSupportedExtensions.toSet()

    actual suspend fun pickExeFile(): String? = withContext(Dispatchers.IO) {
        val chooser = JFileChooser()
        chooser.dialogTitle = "Select Application Executable"
        chooser.fileFilter = FileNameExtensionFilter("Executable Files (*.exe)", "exe")
        chooser.fileSelectionMode = JFileChooser.FILES_ONLY
        chooser.isAcceptAllFileFilterUsed = false
        
        return@withContext if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            chooser.selectedFile.absolutePath
        } else null
    }

    actual suspend fun pickAudioFile(): String? = withContext(Dispatchers.IO) {
        val chooser = JFileChooser()
        chooser.dialogTitle = "Select Alarm Audio"
        chooser.fileFilter = FileNameExtensionFilter("Audio Files (WAV, AIFF, AU)", *desktopSupportedExtensions.toTypedArray())
        chooser.fileSelectionMode = JFileChooser.FILES_ONLY
        chooser.isAcceptAllFileFilterUsed = false
        
        return@withContext if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            chooser.selectedFile.absolutePath
        } else null
    }

    actual fun isAudioFormatSupported(filePath: String): Boolean {
        val extension = File(filePath).extension?.lowercase() ?: return false
        return extension in desktopSupportedExtensionsSet
    }

    actual val supportedAudioExtensions: List<String> = desktopSupportedExtensions
}