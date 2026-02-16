package com.WangWangPhone.core

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * 会话数据类
 */
data class ConversationData(
    val id: String,
    val aiRoleId: String,
    val userPersonaId: String,
    val lastMessage: String = "",
    val lastMessageTime: Long = 0,
    val unreadCount: Int = 0,
    val isMuted: Boolean = false,
    val createdAt: Long = 0,
    val updatedAt: Long = 0
)

/**
 * 消息数据类
 */
data class MessageData(
    val id: String,
    val conversationId: String,
    val isFromUser: Boolean,
    val content: String,
    val messageType: String = "text",
    val createdAt: Long = 0
)

/**
 * 聊天数据库帮助类
 * 负责会话和消息的持久化存储
 */
class ChatDbHelper(private val context: Context) : SQLiteOpenHelper(
    context,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {
    companion object {
        private const val DATABASE_NAME = "wangwang_chat.db"
        private const val DATABASE_VERSION = 1

        // 会话表
        private const val TABLE_CONVERSATIONS = "conversations"
        private const val COL_CONV_ID = "id"
        private const val COL_CONV_AI_ROLE_ID = "ai_role_id"
        private const val COL_CONV_USER_PERSONA_ID = "user_persona_id"
        private const val COL_CONV_LAST_MESSAGE = "last_message"
        private const val COL_CONV_LAST_MESSAGE_TIME = "last_message_time"
        private const val COL_CONV_UNREAD_COUNT = "unread_count"
        private const val COL_CONV_IS_MUTED = "is_muted"
        private const val COL_CONV_CREATED_AT = "created_at"
        private const val COL_CONV_UPDATED_AT = "updated_at"

        // 消息表
        private const val TABLE_MESSAGES = "messages"
        private const val COL_MSG_ID = "id"
        private const val COL_MSG_CONVERSATION_ID = "conversation_id"
        private const val COL_MSG_IS_FROM_USER = "is_from_user"
        private const val COL_MSG_CONTENT = "content"
        private const val COL_MSG_TYPE = "message_type"
        private const val COL_MSG_CREATED_AT = "created_at"
    }

    override fun onCreate(db: SQLiteDatabase) {
        // 创建会话表
        val createConversationsTable = """
            CREATE TABLE $TABLE_CONVERSATIONS (
                $COL_CONV_ID TEXT PRIMARY KEY,
                $COL_CONV_AI_ROLE_ID TEXT NOT NULL,
                $COL_CONV_USER_PERSONA_ID TEXT NOT NULL,
                $COL_CONV_LAST_MESSAGE TEXT DEFAULT '',
                $COL_CONV_LAST_MESSAGE_TIME INTEGER DEFAULT 0,
                $COL_CONV_UNREAD_COUNT INTEGER DEFAULT 0,
                $COL_CONV_IS_MUTED INTEGER DEFAULT 0,
                $COL_CONV_CREATED_AT INTEGER DEFAULT (strftime('%s', 'now')),
                $COL_CONV_UPDATED_AT INTEGER DEFAULT (strftime('%s', 'now'))
            )
        """.trimIndent()
        db.execSQL(createConversationsTable)

        // 创建消息表
        val createMessagesTable = """
            CREATE TABLE $TABLE_MESSAGES (
                $COL_MSG_ID TEXT PRIMARY KEY,
                $COL_MSG_CONVERSATION_ID TEXT NOT NULL,
                $COL_MSG_IS_FROM_USER INTEGER NOT NULL,
                $COL_MSG_CONTENT TEXT NOT NULL,
                $COL_MSG_TYPE TEXT DEFAULT 'text',
                $COL_MSG_CREATED_AT INTEGER DEFAULT (strftime('%s', 'now'))
            )
        """.trimIndent()
        db.execSQL(createMessagesTable)

        // 创建索引
        db.execSQL("CREATE INDEX idx_messages_conversation ON $TABLE_MESSAGES($COL_MSG_CONVERSATION_ID)")
        db.execSQL("CREATE INDEX idx_messages_created_at ON $TABLE_MESSAGES($COL_MSG_CREATED_AT)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // 未来版本升级逻辑
    }

    /**
     * 创建新会话
     */
    fun createConversation(aiRoleId: String, userPersonaId: String): String? {
        return try {
            val conversationId = java.util.UUID.randomUUID().toString()
            val db = writableDatabase
            val values = ContentValues().apply {
                put(COL_CONV_ID, conversationId)
                put(COL_CONV_AI_ROLE_ID, aiRoleId)
                put(COL_CONV_USER_PERSONA_ID, userPersonaId)
                put(COL_CONV_CREATED_AT, System.currentTimeMillis() / 1000)
                put(COL_CONV_UPDATED_AT, System.currentTimeMillis() / 1000)
            }
            db.insert(TABLE_CONVERSATIONS, null, values)
            conversationId
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 获取所有会话
     */
    fun getAllConversations(): List<ConversationData> {
        val conversations = mutableListOf<ConversationData>()
        try {
            val db = readableDatabase
            val cursor = db.query(
                TABLE_CONVERSATIONS,
                null,
                null,
                null,
                null,
                null,
                "$COL_CONV_LAST_MESSAGE_TIME DESC, $COL_CONV_CREATED_AT DESC"
            )
            cursor.use {
                while (it.moveToNext()) {
                    conversations.add(
                        ConversationData(
                            id = it.getString(it.getColumnIndexOrThrow(COL_CONV_ID)),
                            aiRoleId = it.getString(it.getColumnIndexOrThrow(COL_CONV_AI_ROLE_ID)),
                            userPersonaId = it.getString(it.getColumnIndexOrThrow(COL_CONV_USER_PERSONA_ID)),
                            lastMessage = it.getString(it.getColumnIndexOrThrow(COL_CONV_LAST_MESSAGE)) ?: "",
                            lastMessageTime = it.getLong(it.getColumnIndexOrThrow(COL_CONV_LAST_MESSAGE_TIME)),
                            unreadCount = it.getInt(it.getColumnIndexOrThrow(COL_CONV_UNREAD_COUNT)),
                            isMuted = it.getInt(it.getColumnIndexOrThrow(COL_CONV_IS_MUTED)) == 1,
                            createdAt = it.getLong(it.getColumnIndexOrThrow(COL_CONV_CREATED_AT)),
                            updatedAt = it.getLong(it.getColumnIndexOrThrow(COL_CONV_UPDATED_AT))
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return conversations
    }

    /**
     * 根据 ID 获取会话
     */
    fun getConversationById(conversationId: String): ConversationData? {
        return try {
            val db = readableDatabase
            val cursor = db.query(
                TABLE_CONVERSATIONS,
                null,
                "$COL_CONV_ID = ?",
                arrayOf(conversationId),
                null,
                null,
                null,
                "1"
            )
            cursor.use {
                if (it.moveToFirst()) {
                    ConversationData(
                        id = it.getString(it.getColumnIndexOrThrow(COL_CONV_ID)),
                        aiRoleId = it.getString(it.getColumnIndexOrThrow(COL_CONV_AI_ROLE_ID)),
                        userPersonaId = it.getString(it.getColumnIndexOrThrow(COL_CONV_USER_PERSONA_ID)),
                        lastMessage = it.getString(it.getColumnIndexOrThrow(COL_CONV_LAST_MESSAGE)) ?: "",
                        lastMessageTime = it.getLong(it.getColumnIndexOrThrow(COL_CONV_LAST_MESSAGE_TIME)),
                        unreadCount = it.getInt(it.getColumnIndexOrThrow(COL_CONV_UNREAD_COUNT)),
                        isMuted = it.getInt(it.getColumnIndexOrThrow(COL_CONV_IS_MUTED)) == 1,
                        createdAt = it.getLong(it.getColumnIndexOrThrow(COL_CONV_CREATED_AT)),
                        updatedAt = it.getLong(it.getColumnIndexOrThrow(COL_CONV_UPDATED_AT))
                    )
                } else null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 添加消息
     */
    fun addMessage(conversationId: String, isFromUser: Boolean, content: String, messageType: String = "text"): String? {
        return try {
            val messageId = java.util.UUID.randomUUID().toString()
            val db = writableDatabase
            val timestamp = System.currentTimeMillis() / 1000
            
            // 插入消息
            val messageValues = ContentValues().apply {
                put(COL_MSG_ID, messageId)
                put(COL_MSG_CONVERSATION_ID, conversationId)
                put(COL_MSG_IS_FROM_USER, if (isFromUser) 1 else 0)
                put(COL_MSG_CONTENT, content)
                put(COL_MSG_TYPE, messageType)
                put(COL_MSG_CREATED_AT, timestamp)
            }
            db.insert(TABLE_MESSAGES, null, messageValues)
            
            // 更新会话的最后消息
            val conversationValues = ContentValues().apply {
                put(COL_CONV_LAST_MESSAGE, content)
                put(COL_CONV_LAST_MESSAGE_TIME, timestamp)
                put(COL_CONV_UPDATED_AT, timestamp)
            }
            db.update(TABLE_CONVERSATIONS, conversationValues, "$COL_CONV_ID = ?", arrayOf(conversationId))
            
            messageId
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 获取会话的所有消息
     */
    fun getMessages(conversationId: String): List<MessageData> {
        val messages = mutableListOf<MessageData>()
        try {
            val db = readableDatabase
            val cursor = db.query(
                TABLE_MESSAGES,
                null,
                "$COL_MSG_CONVERSATION_ID = ?",
                arrayOf(conversationId),
                null,
                null,
                "$COL_MSG_CREATED_AT ASC"
            )
            cursor.use {
                while (it.moveToNext()) {
                    messages.add(
                        MessageData(
                            id = it.getString(it.getColumnIndexOrThrow(COL_MSG_ID)),
                            conversationId = it.getString(it.getColumnIndexOrThrow(COL_MSG_CONVERSATION_ID)),
                            isFromUser = it.getInt(it.getColumnIndexOrThrow(COL_MSG_IS_FROM_USER)) == 1,
                            content = it.getString(it.getColumnIndexOrThrow(COL_MSG_CONTENT)),
                            messageType = it.getString(it.getColumnIndexOrThrow(COL_MSG_TYPE)) ?: "text",
                            createdAt = it.getLong(it.getColumnIndexOrThrow(COL_MSG_CREATED_AT))
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return messages
    }

    /**
     * 删除会话（包括所有消息）
     */
    fun deleteConversation(conversationId: String): Boolean {
        return try {
            val db = writableDatabase
            db.delete(TABLE_MESSAGES, "$COL_MSG_CONVERSATION_ID = ?", arrayOf(conversationId))
            db.delete(TABLE_CONVERSATIONS, "$COL_CONV_ID = ?", arrayOf(conversationId))
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 清空未读数
     */
    fun clearUnreadCount(conversationId: String): Boolean {
        return try {
            val db = writableDatabase
            val values = ContentValues().apply {
                put(COL_CONV_UNREAD_COUNT, 0)
            }
            db.update(TABLE_CONVERSATIONS, values, "$COL_CONV_ID = ?", arrayOf(conversationId))
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 设置静音状态
     */
    fun setMuted(conversationId: String, isMuted: Boolean): Boolean {
        return try {
            val db = writableDatabase
            val values = ContentValues().apply {
                put(COL_CONV_IS_MUTED, if (isMuted) 1 else 0)
            }
            db.update(TABLE_CONVERSATIONS, values, "$COL_CONV_ID = ?", arrayOf(conversationId))
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
