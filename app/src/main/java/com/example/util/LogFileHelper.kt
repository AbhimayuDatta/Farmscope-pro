package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.model.SensorLog
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object LogFileHelper {
    private const val AUTHORITY = "com.example.farmsense.fileprovider"

    fun getLogsDirectory(context: Context): File {
        val dir = File(context.getExternalFilesDir(null), "FarmSenseLogs")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun createNewSessionFile(context: Context, isManual: Boolean): File {
        val dir = getLogsDirectory(context)
        val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        val prefix = if (isManual) "AgroTech_Manual_" else "AgroTech_Continuous_"
        val filename = "$prefix${sdf.format(Date())}.csv"
        val file = File(dir, filename)

        // Write CSV header if file is new
        if (!file.exists()) {
            FileWriter(file, true).use { writer ->
                writer.append(SensorLog.csvHeader()).append("\n")
            }
        }
        return file
    }

    fun appendLogToCsv(file: File, log: SensorLog) {
        try {
            FileWriter(file, true).use { writer ->
                writer.append(log.toCsvRow()).append("\n")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getAllLogFiles(context: Context): List<File> {
        val dir = getLogsDirectory(context)
        return dir.listFiles { f -> f.extension.lowercase() == "csv" }?.toList()?.sortedByDescending { it.lastModified() } ?: emptyList()
    }

    fun renameFile(file: File, newName: String): File? {
        val cleanName = if (newName.endsWith(".csv", ignoreCase = true)) newName else "$newName.csv"
        val destination = File(file.parentFile, cleanName)
        return if (file.renameTo(destination)) destination else null
    }

    fun deleteFile(file: File): Boolean {
        return file.delete()
    }

    fun getShareIntent(context: Context, file: File): Intent {
        val uri: Uri = FileProvider.getUriForFile(context, AUTHORITY, file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "AgroTech Sensor Log: ${file.name}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(intent, "Share AgroTech CSV Log")
    }

    fun getOpenIntent(context: Context, file: File): Intent {
        val uri: Uri = FileProvider.getUriForFile(context, AUTHORITY, file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "text/csv")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return intent
    }
}
