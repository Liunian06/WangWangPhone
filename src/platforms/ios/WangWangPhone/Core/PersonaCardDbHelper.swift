import Foundation
import SQLite3

struct PersonaCard: Identifiable {
    let id: Int64
    let name: String
    let apiPresetId: Int64
    let createdAt: Int64
    let updatedAt: Int64
}

struct PersonaMessage {
    let id: Int64
    let cardId: Int64
    let role: String
    let content: String
    let timestamp: Int64
}

class PersonaCardDbHelper {
    private var db: OpaquePointer?
    
    init() {
        let fileURL = try! FileManager.default
            .url(for: .documentDirectory, in: .userDomainMask, appropriateFor: nil, create: false)
            .appendingPathComponent("WangWangPhone.db")
        
        if sqlite3_open(fileURL.path, &db) != SQLITE_OK {
            print("Error opening database")
        }
        
        createTables()
    }
    
    deinit {
        sqlite3_close(db)
    }
    
    private func createTables() {
        let createCardTable = """
        CREATE TABLE IF NOT EXISTS persona_cards (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            name TEXT NOT NULL,
            api_preset_id INTEGER NOT NULL,
            created_at INTEGER NOT NULL,
            updated_at INTEGER NOT NULL
        )
        """
        
        let createMessageTable = """
        CREATE TABLE IF NOT EXISTS persona_messages (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            card_id INTEGER NOT NULL,
            role TEXT NOT NULL,
            content TEXT NOT NULL,
            timestamp INTEGER NOT NULL,
            FOREIGN KEY (card_id) REFERENCES persona_cards(id) ON DELETE CASCADE
        )
        """
        
        if sqlite3_exec(db, createCardTable, nil, nil, nil) != SQLITE_OK {
            print("Error creating persona_cards table")
        }
        
        if sqlite3_exec(db, createMessageTable, nil, nil, nil) != SQLITE_OK {
            print("Error creating persona_messages table")
        }
    }
    
    func createCard(name: String, apiPresetId: Int64) -> Int64 {
        let now = Int64(Date().timeIntervalSince1970 * 1000)
        let query = "INSERT INTO persona_cards (name, api_preset_id, created_at, updated_at) VALUES (?, ?, ?, ?)"
        var statement: OpaquePointer?
        
        if sqlite3_prepare_v2(db, query, -1, &statement, nil) == SQLITE_OK {
            sqlite3_bind_text(statement, 1, (name as NSString).utf8String, -1, nil)
            sqlite3_bind_int64(statement, 2, apiPresetId)
            sqlite3_bind_int64(statement, 3, now)
            sqlite3_bind_int64(statement, 4, now)
            
            if sqlite3_step(statement) == SQLITE_DONE {
                let id = sqlite3_last_insert_rowid(db)
                sqlite3_finalize(statement)
                return id
            }
        }
        
        sqlite3_finalize(statement)
        return -1
    }
    
    func getAllCards() -> [PersonaCard] {
        let query = "SELECT id, name, api_preset_id, created_at, updated_at FROM persona_cards ORDER BY updated_at DESC"
        var statement: OpaquePointer?
        var cards: [PersonaCard] = []
        
        if sqlite3_prepare_v2(db, query, -1, &statement, nil) == SQLITE_OK {
            while sqlite3_step(statement) == SQLITE_ROW {
                let id = sqlite3_column_int64(statement, 0)
                let name = String(cString: sqlite3_column_text(statement, 1))
                let apiPresetId = sqlite3_column_int64(statement, 2)
                let createdAt = sqlite3_column_int64(statement, 3)
                let updatedAt = sqlite3_column_int64(statement, 4)
                
                cards.append(PersonaCard(
                    id: id,
                    name: name,
                    apiPresetId: apiPresetId,
                    createdAt: createdAt,
                    updatedAt: updatedAt
                ))
            }
        }
        
        sqlite3_finalize(statement)
        return cards
    }
    
    func getCardById(_ id: Int64) -> PersonaCard? {
        let query = "SELECT id, name, api_preset_id, created_at, updated_at FROM persona_cards WHERE id = ?"
        var statement: OpaquePointer?
        
        if sqlite3_prepare_v2(db, query, -1, &statement, nil) == SQLITE_OK {
            sqlite3_bind_int64(statement, 1, id)
            
            if sqlite3_step(statement) == SQLITE_ROW {
                let id = sqlite3_column_int64(statement, 0)
                let name = String(cString: sqlite3_column_text(statement, 1))
                let apiPresetId = sqlite3_column_int64(statement, 2)
                let createdAt = sqlite3_column_int64(statement, 3)
                let updatedAt = sqlite3_column_int64(statement, 4)
                
                sqlite3_finalize(statement)
                return PersonaCard(
                    id: id,
                    name: name,
                    apiPresetId: apiPresetId,
                    createdAt: createdAt,
                    updatedAt: updatedAt
                )
            }
        }
        
        sqlite3_finalize(statement)
        return nil
    }
    
    func deleteCard(_ id: Int64) {
        let query = "DELETE FROM persona_cards WHERE id = ?"
        var statement: OpaquePointer?
        
        if sqlite3_prepare_v2(db, query, -1, &statement, nil) == SQLITE_OK {
            sqlite3_bind_int64(statement, 1, id)
            sqlite3_step(statement)
        }
        
        sqlite3_finalize(statement)
    }
    
    func addMessage(_ message: PersonaMessage) -> Int64 {
        let query = "INSERT INTO persona_messages (card_id, role, content, timestamp) VALUES (?, ?, ?, ?)"
        var statement: OpaquePointer?
        
        if sqlite3_prepare_v2(db, query, -1, &statement, nil) == SQLITE_OK {
            sqlite3_bind_int64(statement, 1, message.cardId)
            sqlite3_bind_text(statement, 2, (message.role as NSString).utf8String, -1, nil)
            sqlite3_bind_text(statement, 3, (message.content as NSString).utf8String, -1, nil)
            sqlite3_bind_int64(statement, 4, message.timestamp)
            
            if sqlite3_step(statement) == SQLITE_DONE {
                let id = sqlite3_last_insert_rowid(db)
                sqlite3_finalize(statement)
                return id
            }
        }
        
        sqlite3_finalize(statement)
        return -1
    }
    
    func getMessages(cardId: Int64) -> [PersonaMessage] {
        let query = "SELECT id, card_id, role, content, timestamp FROM persona_messages WHERE card_id = ? ORDER BY timestamp ASC"
        var statement: OpaquePointer?
        var messages: [PersonaMessage] = []
        
        if sqlite3_prepare_v2(db, query, -1, &statement, nil) == SQLITE_OK {
            sqlite3_bind_int64(statement, 1, cardId)
            
            while sqlite3_step(statement) == SQLITE_ROW {
                let id = sqlite3_column_int64(statement, 0)
                let cardId = sqlite3_column_int64(statement, 1)
                let role = String(cString: sqlite3_column_text(statement, 2))
                let content = String(cString: sqlite3_column_text(statement, 3))
                let timestamp = sqlite3_column_int64(statement, 4)
                
                messages.append(PersonaMessage(
                    id: id,
                    cardId: cardId,
                    role: role,
                    content: content,
                    timestamp: timestamp
                ))
            }
        }
        
        sqlite3_finalize(statement)
        return messages
    }
    
    func updateCardTimestamp(_ id: Int64) {
        let now = Int64(Date().timeIntervalSince1970 * 1000)
        let query = "UPDATE persona_cards SET updated_at = ? WHERE id = ?"
        var statement: OpaquePointer?
        
        if sqlite3_prepare_v2(db, query, -1, &statement, nil) == SQLITE_OK {
            sqlite3_bind_int64(statement, 1, now)
            sqlite3_bind_int64(statement, 2, id)
            sqlite3_step(statement)
        }
        
        sqlite3_finalize(statement)
    }
    
    func updateMessageContent(cardId: Int64, messageId: Int64, newContent: String) -> Bool {
        let query = "UPDATE persona_messages SET content = ? WHERE card_id = ? AND id = ?"
        var statement: OpaquePointer?
        
        if sqlite3_prepare_v2(db, query, -1, &statement, nil) == SQLITE_OK {
            sqlite3_bind_text(statement, 1, (newContent as NSString).utf8String, -1, nil)
            sqlite3_bind_int64(statement, 2, cardId)
            sqlite3_bind_int64(statement, 3, messageId)
            
            let result = sqlite3_step(statement) == SQLITE_DONE
            sqlite3_finalize(statement)
            return result
        }
        
        sqlite3_finalize(statement)
        return false
    }
    
    func deleteMessagesAfter(cardId: Int64, messageId: Int64) {
        let query = "DELETE FROM persona_messages WHERE card_id = ? AND id > ?"
        var statement: OpaquePointer?
        
        if sqlite3_prepare_v2(db, query, -1, &statement, nil) == SQLITE_OK {
            sqlite3_bind_int64(statement, 1, cardId)
            sqlite3_bind_int64(statement, 2, messageId)
            sqlite3_step(statement)
        }
        
        sqlite3_finalize(statement)
    }
    
    /// 更新人设卡（对齐 Android updateCard）
    func updateCard(id: Int64, name: String, apiPresetId: Int64) -> Bool {
        let now = Int64(Date().timeIntervalSince1970 * 1000)
        let query = "UPDATE persona_cards SET name = ?, api_preset_id = ?, updated_at = ? WHERE id = ?"
        var statement: OpaquePointer?
        
        if sqlite3_prepare_v2(db, query, -1, &statement, nil) == SQLITE_OK {
            sqlite3_bind_text(statement, 1, (name as NSString).utf8String, -1, nil)
            sqlite3_bind_int64(statement, 2, apiPresetId)
            sqlite3_bind_int64(statement, 3, now)
            sqlite3_bind_int64(statement, 4, id)
            
            let result = sqlite3_step(statement) == SQLITE_DONE
            sqlite3_finalize(statement)
            return result
        }
        
        sqlite3_finalize(statement)
        return false
    }
    
    /// 清空某张卡的所有消息（对齐 Android clearMessages）
    func clearMessages(cardId: Int64) -> Bool {
        let query = "DELETE FROM persona_messages WHERE card_id = ?"
        var statement: OpaquePointer?
        
        if sqlite3_prepare_v2(db, query, -1, &statement, nil) == SQLITE_OK {
            sqlite3_bind_int64(statement, 1, cardId)
            let result = sqlite3_step(statement) == SQLITE_DONE
            sqlite3_finalize(statement)
            return result
        }
        
        sqlite3_finalize(statement)
        return false
    }
}
