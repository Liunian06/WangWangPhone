package com.WangWangPhone.core

import android.content.Context
import android.util.Log
import org.json.JSONObject
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

/**
 * 应用日志管理器
 * 使用JSONL格式记录日志，方便导出和分析
 */
object AppLogger {
    private const val LOG_FILE_NAME = "app_logs.jsonl"
    private const val MAX_LOG_SIZE = 5 * 1024 * 1024 // 5MB
    private var logFile: File? = null
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
    
    fun init(context: Context) {
        logFile = File(context.filesDir, LOG_FILE_NAME)
        
        // 如果日志文件过大，清空它
        if (logFile?.exists() == true && (logFile?.length() ?: 0) > MAX_LOG_SIZE) {
            logFile?.delete()
        }
    }
    
    fun d(tag: String, message: String) {
        log("DEBUG", tag, message)
        Log.d(tag, message)
    }
    
    fun i(tag: String, message: String) {
        log("INFO", tag, message)
        Log.i(tag, message)
    }
    
    fun w(tag: String, message: String) {
        log("WARN", tag, message)
        Log.w(tag, message)
    }
    
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        log("ERROR", tag, message, throwable)
        if (throwable != null) {
            Log.e(tag, message, throwable)
        } else {
            Log.e(tag, message)
        }
    }
    
    private fun log(level: String, tag: String, message: String, throwable: Throwable? = null) {
        try {
            val logEntry = JSONObject().apply {
                put("timestamp", dateFormat.format(Date()))
                put("level", level)
                put("tag", tag)
                put("message", message)
                if (throwable != null) {
                    put("exception", throwable.toString())
                    put("stackTrace", throwable.stackTraceToString())
                }
            }
            
            logFile?.let { file ->
                FileWriter(file, true).use { writer ->
                    writer.append(logEntry.toString())
                    writer.append("\n")
                }
            }
        } catch (e: Exception) {
            // 如果日志写入失败，只在Logcat中记录
            Log.e("AppLogger", "Failed to write log", e)
        }
    }
    
    fun getLogFile(): File? = logFile
    
    fun clearLogs() {
        logFile?.delete()
        Log.i("AppLogger", "Logs cleared")
    }
    
    fun getLogSize(): Long = logFile?.length() ?: 0
    
    fun getLogCount(): Int {
        return try {
            logFile?.readLines()?.size ?: 0
        } catch (e: Exception) {
            0
        }
    }
}
