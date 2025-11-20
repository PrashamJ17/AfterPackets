package com.packethunter.mobile

import android.content.Context
import android.util.Log
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.*

/**
 * Global uncaught exception handler that logs crashes to file
 */
class CrashHandler private constructor(private val context: Context) : Thread.UncaughtExceptionHandler {
    
    private val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
    
    companion object {
        private const val TAG = "AfterPacketsUncaught"
        private const val CRASH_LOG_FILE = "crash_log.txt"
        
        @Volatile
        private var instance: CrashHandler? = null
        
        fun install(context: Context) {
            if (instance == null) {
                synchronized(this) {
                    if (instance == null) {
                        val handler = CrashHandler(context.applicationContext)
                        Thread.setDefaultUncaughtExceptionHandler(handler)
                        instance = handler
                        Log.i(TAG, "✅ Global crash handler installed")
                    }
                }
            }
        }
        
        fun getCrashLogFile(context: Context): File {
            return File(context.filesDir, CRASH_LOG_FILE)
        }
    }
    
    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            // Log to logcat
            Log.e(TAG, "💥 UNCAUGHT EXCEPTION in thread: ${thread.name}", throwable)
            
            // Write to crash log file
            writeCrashToFile(thread, throwable)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write crash log", e)
        } finally {
            // Call original handler to perform system crash handling
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
    
    private fun writeCrashToFile(thread: Thread, throwable: Throwable) {
        val crashLogFile = getCrashLogFile(context)
        
        try {
            // Get full stacktrace
            val sw = StringWriter()
            val pw = PrintWriter(sw)
            throwable.printStackTrace(pw)
            val stackTrace = sw.toString()
            
            // Build crash entry
            val timestamp = dateFormat.format(Date())
            val crashEntry = buildString {
                // Create separator using string concatenation
                append("\n")
                append("================================================================================\n")
                append("CRASH REPORT - $timestamp\n")
                append("================================================================================\n")
                append("Thread: ${thread.name}\n")
                append("Exception: ${throwable.javaClass.name}\n")
                append("Message: ${throwable.message ?: "No message"}\n")
                append("\n")
                append("Stack Trace:\n")
                append(stackTrace)
                append("\n")
                
                // Include cause if present
                var cause = throwable.cause
                var level = 1
                while (cause != null && level <= 3) {
                    append("\nCaused by (level $level):\n")
                    append("${cause.javaClass.name}: ${cause.message}\n")
                    val causeSw = StringWriter()
                    val causePw = PrintWriter(causeSw)
                    cause.printStackTrace(causePw)
                    append(causeSw.toString())
                    cause = cause.cause
                    level++
                }
                
                append("\n")
            }
            
            // Append to file (atomic write via temp file)
            val tempFile = File(crashLogFile.parentFile, "${CRASH_LOG_FILE}.tmp")
            
            // Read existing content
            val existingContent = if (crashLogFile.exists()) {
                crashLogFile.readText()
            } else {
                ""
            }
            
            // Write combined content to temp file
            tempFile.writeText(existingContent + crashEntry)
            
            // Rename temp to final (atomic on most filesystems)
            tempFile.renameTo(crashLogFile)
            
            Log.i(TAG, "📝 Crash written to: ${crashLogFile.absolutePath} (${crashLogFile.length()} bytes)")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write crash to file", e)
        }
    }
}
