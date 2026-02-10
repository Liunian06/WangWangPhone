package com.WangWangPhone.core

/**
 * JNI 桥接类，用于连接 C++ 核心聊天逻辑
 */
object ChatManager {
    init {
        System.loadLibrary("wwj_core")
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
