package com.WangWangPhone.core

import android.util.Log

/**
 * JNI 桥接类，用于连接 C++ 核心聊天逻辑
 * 
 * 注意：当前 MVP 阶段，C++ 原生库（libwwj_core.so）尚未通过 Android NDK 构建。
 * 该类提供安全的降级逻辑：当原生库不可用时，所有方法返回默认值而不会导致崩溃。
 * 
 * TODO: 在 app/build.gradle 中配置 externalNativeBuild 后启用完整 JNI 功能。
 * 需要解决的依赖问题：
 *   - SQLite3：需要在 CMakeLists.txt 中为 Android 配置（使用 NDK 内置或自编译）
 *   - OpenSSL：需要通过预编译库或 CMake ExternalProject 引入
 */
object ChatManager {
    private const val TAG = "ChatManager"
    
    /** 原生库是否成功加载 */
    var isNativeLoaded: Boolean = false
        private set

    init {
        try {
            System.loadLibrary("wwj_core")
            isNativeLoaded = true
            Log.i(TAG, "C++ 原生库 libwwj_core.so 加载成功")
        } catch (e: UnsatisfiedLinkError) {
            isNativeLoaded = false
            Log.w(TAG, "C++ 原生库 libwwj_core.so 未找到，将使用纯 Kotlin 降级模式运行。" +
                    "聊天功能将使用 Mock 数据。错误: ${e.message}")
        }
    }

    // ---------- 安全调用包装 ----------

    /**
     * 初始化聊天管理器（安全版本）
     * 当原生库不可用时返回 false 但不会崩溃
     */
    fun safeInitialize(): Boolean {
        if (!isNativeLoaded) {
            Log.d(TAG, "safeInitialize: 原生库未加载，跳过初始化")
            return false
        }
        return try {
            initialize()
        } catch (e: Exception) {
            Log.e(TAG, "初始化失败: ${e.message}")
            false
        }
    }

    // ---------- Native Methods ----------

    /**
     * 初始化聊天管理器（在数据库初始化后调用）
     */
    external fun initialize(): Boolean

    // 会话操作
    external fun getAllSessionsJson(): String
    external fun saveSessionJson(sessionJson: String): Boolean
    external fun deleteSession(sessionId: String): Boolean
    external fun updateSessionLastUpdated(sessionId: String, timestamp: Long): Boolean

    // 消息操作
    external fun getMessagesJson(sessionId: String, limit: Int, offset: Int): String
    external fun insertMessageJson(messageJson: String): Boolean
    external fun deleteMessage(messageId: String): Boolean
    external fun getLastMessageJson(sessionId: String): String
    external fun getUnreadCount(sessionId: String): Int

    // 角色与人设
    external fun getAllRolesJson(): String
    external fun saveRoleJson(roleJson: String): Boolean
    external fun getRoleJson(roleId: String): String
    
    external fun getAllMeJson(): String
    external fun saveMeJson(meJson: String): Boolean
    external fun getMeJson(meId: String): String
    
    // 解析引擎接口
    /**
     * 调用 C++ 解析器解析 AI 响应文本
     * @return JSON 数组字符串，包含 ParsedItem 列表
     */
    external fun parseResponse(response: String, enableExtended: Boolean, enableEmoji: Boolean): String

    // ---------- Kotlin Wrapper (Optional) ----------
    // 这里可以封装将 JSON 转换为 Kotlin Data Class 的逻辑，使用 Gson 或 Kotlinx.serialization
}
