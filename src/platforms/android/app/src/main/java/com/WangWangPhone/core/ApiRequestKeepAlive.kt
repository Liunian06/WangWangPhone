package com.WangWangPhone.core

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import java.util.concurrent.atomic.AtomicInteger

/**
 * 基于引用计数的请求保活管理器。
 * 只要还有进行中的 API 请求，就保持前台 Service 存活，避免切后台时请求被系统中断。
 */
object ApiRequestKeepAlive {
    private const val TAG = "ApiRequestKeepAlive"

    private val activeRequests = AtomicInteger(0)

    @Volatile
    private var appContext: Context? = null

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    fun onRequestStarted() {
        val count = activeRequests.incrementAndGet()
        if (count == 1) {
            startKeepAliveService()
        }
    }

    fun onRequestFinished() {
        val count = activeRequests.updateAndGet { current ->
            if (current <= 0) 0 else current - 1
        }
        if (count == 0) {
            stopKeepAliveService()
        }
    }

    private fun startKeepAliveService() {
        val context = appContext ?: return
        runCatching {
            ContextCompat.startForegroundService(
                context,
                Intent(context, ApiKeepAliveService::class.java)
            )
        }.onFailure { e ->
            Log.w(TAG, "Failed to start keep-alive service", e)
        }
    }

    private fun stopKeepAliveService() {
        val context = appContext ?: return
        runCatching {
            context.stopService(Intent(context, ApiKeepAliveService::class.java))
        }.onFailure { e ->
            Log.w(TAG, "Failed to stop keep-alive service", e)
        }
    }
}
