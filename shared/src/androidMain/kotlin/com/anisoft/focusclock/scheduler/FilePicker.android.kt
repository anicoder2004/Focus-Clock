package com.anisoft.focusclock.scheduler

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.result.ActivityResultLauncher
import com.anisoft.focusclock.appContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object FilePickerBridge {
    var audioLauncher: ActivityResultLauncher<String>? = null
    var exeLauncher: ActivityResultLauncher<String>? = null
    var audioResult: CompletableDeferred<String?>? = null
    var exeResult: CompletableDeferred<String?>? = null

    fun onAudioPicked(uri: Uri?) {
        audioResult?.complete(uri?.toString())
    }

    fun onExePicked(uri: Uri?) {
        exeResult?.complete(uri?.toString())
    }
}

actual class FilePicker {
    private val androidSupportedExtensions = listOf("wav", "mp3", "ogg", "m4a", "aac", "flac", "amr")
    private val androidSupportedExtensionsSet = androidSupportedExtensions.toSet()

    actual suspend fun pickExeFile(): String? = withContext(Dispatchers.Main) {
        val launcher = FilePickerBridge.exeLauncher ?: return@withContext null
        val deferred = CompletableDeferred<String?>()
        FilePickerBridge.exeResult = deferred
        launcher.launch("*/*")
        deferred.await()
    }

    actual suspend fun pickAudioFile(): String? {
        val uriString = withContext(Dispatchers.Main) {
            val launcher = FilePickerBridge.audioLauncher ?: return@withContext null
            val deferred = CompletableDeferred<String?>()
            FilePickerBridge.audioResult = deferred
            launcher.launch("audio/*")
            deferred.await()
        } ?: return null

        return withContext(Dispatchers.IO) {
            try {
                val uri = Uri.parse(uriString)
                val resolver = appContext.contentResolver
                val displayName = resolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0 && cursor.moveToFirst()) cursor.getString(nameIndex) else null
                }
                val ext = displayName?.substringAfterLast('.', "")?.lowercase()
                val safeExt = if (!ext.isNullOrBlank() && ext.length <= 5) ext else "bin"
                val file = File(appContext.filesDir, "alarm_${System.currentTimeMillis()}.$safeExt")
                resolver.openInputStream(uri)?.use { input ->
                    file.outputStream().use { output -> input.copyTo(output) }
                }
                file.absolutePath
            } catch (e: Exception) {
                null
            }
        }
    }

    actual fun isAudioFormatSupported(filePath: String): Boolean {
        val extension = File(filePath).extension.lowercase()
        return extension in androidSupportedExtensionsSet
    }

    actual val supportedAudioExtensions: List<String> = androidSupportedExtensions
}