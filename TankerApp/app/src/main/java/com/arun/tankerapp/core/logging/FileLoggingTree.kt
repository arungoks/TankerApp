package com.arun.tankerapp.core.logging

import android.content.Context
import android.util.Log
import timber.log.Timber
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FileLoggingTree(private val context: Context) : Timber.DebugTree() {

    private val logFile: File by lazy {
        File(context.filesDir, "tanker_app_logs.txt")
    }

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        // Log to console as usual
        super.log(priority, tag, message, t)

        // Log to file
        try {
            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
            val priorityString = when (priority) {
                Log.VERBOSE -> "VERBOSE"
                Log.DEBUG -> "DEBUG"
                Log.INFO -> "INFO"
                Log.WARN -> "WARN"
                Log.ERROR -> "ERROR"
                Log.ASSERT -> "ASSERT"
                else -> "UNKNOWN"
            }
            
            val logEntry = "$timestamp [$priorityString] $tag: $message\n"
            val writer = FileWriter(logFile, true)
            writer.append(logEntry)
            t?.let { 
                writer.append(Log.getStackTraceString(it))
                writer.append("\n")
            }
            writer.flush()
            writer.close()
        } catch (e: IOException) {
            Log.e("FileLoggingTree", "Failed to write log to file", e)
        }
    }
}
