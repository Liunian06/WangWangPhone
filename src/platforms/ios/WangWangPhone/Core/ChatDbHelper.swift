import Foundation
import SQLite3

/// 会话数据结构（对齐 Android ConversationData）
struct ConversationData {
    let id: String
    let aiRoleId: String
    let userPersonaId: String
    var apiPresetId: Int64
    var lastMessage: String
    var lastMessageTime: Int64
    var unreadCount: Int
    var isMuted: Bool
    let createdAt: Int64
    var updatedAt: Int64
}

/// 消息数据结构（对齐 Android MessageData）
struct MessageData {
    let id: String
    let conversationId: String
    let isFromUser: Bool
    let content: String
    let messageType: String
    let createdAt: Int64
}

class ChatDbHelper {
    static let shared = ChatDbHelper()
    private var db: OpaquePointer?
    
    private init() {
        openDatabase()
        createTables()
    }
    
    private func openDatabase() {
        let fileURL = try! FileManager.default
            .url(for: .documentDirectory, in: .userDomainMask, appropriateFor: nil, create: false)
            .appendingPathComponent("wangwang_chat.db")
        
        if sqlite3_open(fileURL.path, &db) != SQLITE_OK {
            print("Error opening chat database")
        }
    }
    
    private func createTables() {
        // 创建会话表
        let createConversationsTable = """
        CREATE TABLE IF NOT EXISTS conversations (
            id TEXT PRIMARY KEY,
            ai_role_id TEXT NOT NULL,
            user_persona_id TEXT NOT NULL,
            api_preset_id INTEGER DEFAULT -1,
            last_message TEXT DEFAULT '',
            last_message_time INTEGER DEFAULT 0,
            unread_count INTEGER DEFAULT 0,
            is_muted INTEGER DEFAULT 0,
            created_at INTEGER DEFAULT (strftime('%s', 'now')),
            updated_at INTEGER DEFAULT (strftime('%s', 'now'))
        )
        """
        executeSQL(createConversationsTable)
        
        // 创建消息表（关联 conversation_id）
        let createMessagesTable = """
        CREATE TABLE IF NOT EXISTS messages (
            id TEXT PRIMARY KEY,
            conversation_id TEXT NOT NULL,
            is_from_user INTEGER NOT NULL,
            content TEXT NOT NULL,
            message_type TEXT DEFAULT 'text',
            created_at INTEGER DEFAULT (strftime('%s', 'now'))
        )
        """
        executeSQL(createMessagesTable)
        
        // 创建索引
        executeSQL("CREATE INDEX IF NOT EXISTS idx_messages_conversation ON messages(conversation_id)")
        executeSQL("CREATE INDEX IF NOT EXISTS idx_messages_created_at ON messages(created_at)")
        
        // 迁移兼容：如果旧 messages 表存在 contact_id 列，进行数据迁移
        migrateOldMessagesIfNeeded()
    }
    
    private func migrateOldMessagesIfNeeded() {
        // 检查旧表是否有 contact_id 列
        let pragmaQuery = "PRAGMA table_info(messages)"
        var stmt: OpaquePointer?
        var hasContactId = false
        var hasConversationId = false
        
        if sqlite3_prepare_v2(db, pragmaQuery, -1, &stmt, nil) == SQLITE_OK {
            while sqlite3_step(stmt) == SQLITE_ROW {
                if let name = sqlite3_column_text(stmt, 1) {
                    let colName = String(cString: name)
                    if colName == "contact_id" { hasContactId = true }
                    if colName == "conversation_id" { hasConversationId = true }
                }
            }
        }
        sqlite3_finalize(stmt)
        
        // 如果有旧的 contact_id 列但没有 conversation_id，说明是旧表结构
        if hasContactId && !hasConversationId {
            print("ChatDbHelper: Migrating old messages table...")
            // 重命名旧表
            executeSQL("ALTER TABLE messages RENAME TO messages_old")
            // 重新创建新表
            let createNew = """
            CREATE TABLE IF NOT EXISTS messages (
                id TEXT PRIMARY KEY,
                conversation_id TEXT NOT NULL,
                is_from_user INTEGER NOT NULL,
                content TEXT NOT NULL,
                message_type TEXT DEFAULT 'text',
                created_at INTEGER DEFAULT (strftime('%s', 'now'))
            )
            """
            executeSQL(createNew)
            executeSQL("CREATE INDEX IF NOT EXISTS idx_messages_conversation ON messages(conversation_id)")
            executeSQL("CREATE INDEX IF NOT EXISTS idx_messages_created_at ON messages(created_at)")
            // 旧数据迁移：contact_id -> conversation_id, is_sent -> is_from_user
            executeSQL("""
                INSERT INTO messages (id, conversation_id, is_from_user, content, message_type, created_at)
                SELECT CAST(id AS TEXT), contact_id, is_sent, message, 'text', timestamp / 1000
                FROM messages_old
            """)
            executeSQL("DROP TABLE IF EXISTS messages_old")
            print("ChatDbHelper: Migration complete")
        }
    }
    
    private func executeSQL(_ sql: String) {
        var stmt: OpaquePointer?
        if sqlite3_prepare_v2(db, sql, -1, &stmt, nil) == SQLITE_OK {
            if sqlite3_step(stmt) != SQLITE_DONE {
                let errmsg = String(cString: sqlite3_errmsg(db))
                print("SQL error: \(errmsg) for: \(sql.prefix(80))")
            }
        } else {
            let errmsg = String(cString: sqlite3_errmsg(db))
            print("Prepare error: \(errmsg) for: \(sql.prefix(80))")
        }
        sqlite3_finalize(stmt)
    }
    
    // MARK: - 会话 CRUD
    
    /// 创建新会话
    func createConversation(aiRoleId: String, userPersonaId: String, apiPresetId: Int64 = -1) -> String? {
        let conversationId = UUID().uuidString
        let now = Int64(Date().timeIntervalSince1970)
        let sql = """
        INSERT INTO conversations (id, ai_role_id, user_persona_id, api_preset_id, created_at, updated_at)
        VALUES (?, ?, ?, ?, ?, ?)
        """
        var stmt: OpaquePointer?
        guard sqlite3_prepare_v2(db, sql, -1, &stmt, nil) == SQLITE_OK else { return nil }
        
        sqlite3_bind_text(stmt, 1, (conversationId as NSString).utf8String, -1, nil)
        sqlite3_bind_text(stmt, 2, (aiRoleId as NSString).utf8String, -1, nil)
        sqlite3_bind_text(stmt, 3, (userPersonaId as NSString).utf8String, -1, nil)
        sqlite3_bind_int64(stmt, 4, apiPresetId)
        sqlite3_bind_int64(stmt, 5, now)
        sqlite3_bind_int64(stmt, 6, now)
        
        let result = sqlite3_step(stmt)
        sqlite3_finalize(stmt)
        return result == SQLITE_DONE ? conversationId : nil
    }
    
    /// 获取所有会话
    func getAllConversations() -> [ConversationData] {
        var conversations: [ConversationData] = []
        let sql = "SELECT * FROM conversations ORDER BY last_message_time DESC, created_at DESC"
        var stmt: OpaquePointer?
        
        guard sqlite3_prepare_v2(db, sql, -1, &stmt, nil) == SQLITE_OK else { return conversations }
        
        while sqlite3_step(stmt) == SQLITE_ROW {
            if let conv = parseConversation(stmt) {
                conversations.append(conv)
            }
        }
        sqlite3_finalize(stmt)
        return conversations
    }
    
    /// 根据 ID 获取会话
    func getConversationById(_ conversationId: String) -> ConversationData? {
        let sql = "SELECT * FROM conversations WHERE id = ? LIMIT 1"
        var stmt: OpaquePointer?
        
        guard sqlite3_prepare_v2(db, sql, -1, &stmt, nil) == SQLITE_OK else { return nil }
        sqlite3_bind_text(stmt, 1, (conversationId as NSString).utf8String, -1, nil)
        
        var conv: ConversationData?
        if sqlite3_step(stmt) == SQLITE_ROW {
            conv = parseConversation(stmt)
        }
        sqlite3_finalize(stmt)
        return conv
    }
    
    /// 删除会话（包括所有消息）
    func deleteConversation(_ conversationId: String) -> Bool {
        executeSQL("DELETE FROM messages WHERE conversation_id = '\(conversationId)'")
        let sql = "DELETE FROM conversations WHERE id = ?"
        var stmt: OpaquePointer?
        guard sqlite3_prepare_v2(db, sql, -1, &stmt, nil) == SQLITE_OK else { return false }
        sqlite3_bind_text(stmt, 1, (conversationId as NSString).utf8String, -1, nil)
        let result = sqlite3_step(stmt)
        sqlite3_finalize(stmt)
        return result == SQLITE_DONE
    }
    
    /// 设置静音状态
    func setMuted(conversationId: String, isMuted: Bool) -> Bool {
        let sql = "UPDATE conversations SET is_muted = ? WHERE id = ?"
        var stmt: OpaquePointer?
        guard sqlite3_prepare_v2(db, sql, -1, &stmt, nil) == SQLITE_OK else { return false }
        sqlite3_bind_int(stmt, 1, isMuted ? 1 : 0)
        sqlite3_bind_text(stmt, 2, (conversationId as NSString).utf8String, -1, nil)
        let result = sqlite3_step(stmt)
        sqlite3_finalize(stmt)
        return result == SQLITE_DONE
    }
    
    /// 更新会话的 API 预设 ID
    func updateApiPresetId(conversationId: String, apiPresetId: Int64) -> Bool {
        let now = Int64(Date().timeIntervalSince1970)
        let sql = "UPDATE conversations SET api_preset_id = ?, updated_at = ? WHERE id = ?"
        var stmt: OpaquePointer?
        guard sqlite3_prepare_v2(db, sql, -1, &stmt, nil) == SQLITE_OK else { return false }
        sqlite3_bind_int64(stmt, 1, apiPresetId)
        sqlite3_bind_int64(stmt, 2, now)
        sqlite3_bind_text(stmt, 3, (conversationId as NSString).utf8String, -1, nil)
        let result = sqlite3_step(stmt)
        sqlite3_finalize(stmt)
        return result == SQLITE_DONE
    }
    
    /// 清空未读数
    func clearUnreadCount(conversationId: String) -> Bool {
        let sql = "UPDATE conversations SET unread_count = 0 WHERE id = ?"
        var stmt: OpaquePointer?
        guard sqlite3_prepare_v2(db, sql, -1, &stmt, nil) == SQLITE_OK else { return false }
        sqlite3_bind_text(stmt, 1, (conversationId as NSString).utf8String, -1, nil)
        let result = sqlite3_step(stmt)
        sqlite3_finalize(stmt)
        return result == SQLITE_DONE
    }
    
    // MARK: - 消息 CRUD
    
    /// 添加消息（同时更新会话的 lastMessage）
    func addMessage(conversationId: String, isFromUser: Bool, content: String, messageType: String = "text") -> String? {
        let messageId = UUID().uuidString
        let now = Int64(Date().timeIntervalSince1970)
        
        // 插入消息
        let insertSql = """
        INSERT INTO messages (id, conversation_id, is_from_user, content, message_type, created_at)
        VALUES (?, ?, ?, ?, ?, ?)
        """
        var stmt: OpaquePointer?
        guard sqlite3_prepare_v2(db, insertSql, -1, &stmt, nil) == SQLITE_OK else { return nil }
        sqlite3_bind_text(stmt, 1, (messageId as NSString).utf8String, -1, nil)
        sqlite3_bind_text(stmt, 2, (conversationId as NSString).utf8String, -1, nil)
        sqlite3_bind_int(stmt, 3, isFromUser ? 1 : 0)
        sqlite3_bind_text(stmt, 4, (content as NSString).utf8String, -1, nil)
        sqlite3_bind_text(stmt, 5, (messageType as NSString).utf8String, -1, nil)
        sqlite3_bind_int64(stmt, 6, now)
        
        let insertResult = sqlite3_step(stmt)
        sqlite3_finalize(stmt)
        guard insertResult == SQLITE_DONE else { return nil }
        
        // 更新会话的最后消息
        let updateSql = "UPDATE conversations SET last_message = ?, last_message_time = ?, updated_at = ? WHERE id = ?"
        var updateStmt: OpaquePointer?
        if sqlite3_prepare_v2(db, updateSql, -1, &updateStmt, nil) == SQLITE_OK {
            sqlite3_bind_text(updateStmt, 1, (content as NSString).utf8String, -1, nil)
            sqlite3_bind_int64(updateStmt, 2, now)
            sqlite3_bind_int64(updateStmt, 3, now)
            sqlite3_bind_text(updateStmt, 4, (conversationId as NSString).utf8String, -1, nil)
            sqlite3_step(updateStmt)
        }
        sqlite3_finalize(updateStmt)
        
        return messageId
    }
    
    /// 获取会话的所有消息
    func getMessages(conversationId: String) -> [MessageData] {
        var messages: [MessageData] = []
        let sql = "SELECT * FROM messages WHERE conversation_id = ? ORDER BY created_at ASC"
        var stmt: OpaquePointer?
        
        guard sqlite3_prepare_v2(db, sql, -1, &stmt, nil) == SQLITE_OK else { return messages }
        sqlite3_bind_text(stmt, 1, (conversationId as NSString).utf8String, -1, nil)
        
        while sqlite3_step(stmt) == SQLITE_ROW {
            if let msg = parseMessage(stmt) {
                messages.append(msg)
            }
        }
        sqlite3_finalize(stmt)
        return messages
    }
    
    /// 删除单条消息
    func deleteMessage(_ messageId: String) -> Bool {
        let sql = "DELETE FROM messages WHERE id = ?"
        var stmt: OpaquePointer?
        guard sqlite3_prepare_v2(db, sql, -1, &stmt, nil) == SQLITE_OK else { return false }
        sqlite3_bind_text(stmt, 1, (messageId as NSString).utf8String, -1, nil)
        let result = sqlite3_step(stmt)
        sqlite3_finalize(stmt)
        return result == SQLITE_DONE
    }
    
    // MARK: - 兼容旧接口（过渡期保留）
    
    /// 旧接口兼容：通过 contactId 添加消息（内部映射到 conversationId）
    func addMessage(contactId: String, isSent: Bool, message: String) {
        _ = addMessage(conversationId: contactId, isFromUser: isSent, content: message)
    }
    
    /// 旧接口兼容：通过 contactId 获取消息
    func getMessages(contactId: String) -> [(isSent: Bool, message: String, timestamp: Int64)] {
        let msgs = getMessages(conversationId: contactId)
        return msgs.map { (isSent: $0.isFromUser, message: $0.content, timestamp: $0.createdAt) }
    }
    
    // MARK: - Private Helpers
    
    private func parseConversation(_ stmt: OpaquePointer?) -> ConversationData? {
        guard let stmt = stmt else { return nil }
        
        guard let idPtr = sqlite3_column_text(stmt, 0),
              let aiRolePtr = sqlite3_column_text(stmt, 1),
              let userPersonaPtr = sqlite3_column_text(stmt, 2) else { return nil }
        
        let lastMsgPtr = sqlite3_column_text(stmt, 4)
        let lastMessage = lastMsgPtr != nil ? String(cString: lastMsgPtr!) : ""
        
        return ConversationData(
            id: String(cString: idPtr),
            aiRoleId: String(cString: aiRolePtr),
            userPersonaId: String(cString: userPersonaPtr),
            apiPresetId: sqlite3_column_int64(stmt, 3),
            lastMessage: lastMessage,
            lastMessageTime: sqlite3_column_int64(stmt, 5),
            unreadCount: Int(sqlite3_column_int(stmt, 6)),
            isMuted: sqlite3_column_int(stmt, 7) == 1,
            createdAt: sqlite3_column_int64(stmt, 8),
            updatedAt: sqlite3_column_int64(stmt, 9)
        )
    }
    
    private func parseMessage(_ stmt: OpaquePointer?) -> MessageData? {
        guard let stmt = stmt else { return nil }
        
        guard let idPtr = sqlite3_column_text(stmt, 0),
              let convIdPtr = sqlite3_column_text(stmt, 1),
              let contentPtr = sqlite3_column_text(stmt, 3) else { return nil }
        
        let typePtr = sqlite3_column_text(stmt, 4)
        let messageType = typePtr != nil ? String(cString: typePtr!) : "text"
        
        return MessageData(
            id: String(cString: idPtr),
            conversationId: String(cString: convIdPtr),
            isFromUser: sqlite3_column_int(stmt, 2) == 1,
            content: String(cString: contentPtr),
            messageType: messageType,
            createdAt: sqlite3_column_int64(stmt, 5)
        )
    }
    
    deinit {
        if db != nil {
            sqlite3_close(db)
        }
    }
}
