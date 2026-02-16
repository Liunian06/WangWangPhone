import Foundation
import SQLite3

class ChatDbHelper {
    static let shared = ChatDbHelper()
    private var db: OpaquePointer?
    
    private init() {
        openDatabase()
        createTable()
    }
    
    private func openDatabase() {
        let fileURL = try! FileManager.default
            .url(for: .documentDirectory, in: .userDomainMask, appropriateFor: nil, create: false)
            .appendingPathComponent("chat.db")
        
        if sqlite3_open(fileURL.path, &db) != SQLITE_OK {
            print("Error opening database")
        }
    }
    
    private func createTable() {
        let createTableQuery = """
        CREATE TABLE IF NOT EXISTS messages (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            contact_id TEXT NOT NULL,
            is_sent INTEGER NOT NULL,
            message TEXT NOT NULL,
            timestamp INTEGER NOT NULL
        )
        """
        
        var createTableStatement: OpaquePointer?
        if sqlite3_prepare_v2(db, createTableQuery, -1, &createTableStatement, nil) == SQLITE_OK {
            if sqlite3_step(createTableStatement) == SQLITE_DONE {
                print("Messages table created")
            }
        }
        sqlite3_finalize(createTableStatement)
    }
    
    func addMessage(contactId: String, isSent: Bool, message: String) {
        let insertQuery = "INSERT INTO messages (contact_id, is_sent, message, timestamp) VALUES (?, ?, ?, ?)"
        var insertStatement: OpaquePointer?
        
        if sqlite3_prepare_v2(db, insertQuery, -1, &insertStatement, nil) == SQLITE_OK {
            sqlite3_bind_text(insertStatement, 1, (contactId as NSString).utf8String, -1, nil)
            sqlite3_bind_int(insertStatement, 2, isSent ? 1 : 0)
            sqlite3_bind_text(insertStatement, 3, (message as NSString).utf8String, -1, nil)
            sqlite3_bind_int64(insertStatement, 4, Int64(Date().timeIntervalSince1970 * 1000))
            
            if sqlite3_step(insertStatement) == SQLITE_DONE {
                print("Message saved successfully")
            }
        }
        sqlite3_finalize(insertStatement)
    }
    
    func getMessages(contactId: String) -> [(isSent: Bool, message: String, timestamp: Int64)] {
        var messages: [(Bool, String, Int64)] = []
        let queryString = "SELECT is_sent, message, timestamp FROM messages WHERE contact_id = ? ORDER BY timestamp ASC"
        var queryStatement: OpaquePointer?
        
        if sqlite3_prepare_v2(db, queryString, -1, &queryStatement, nil) == SQLITE_OK {
            sqlite3_bind_text(queryStatement, 1, (contactId as NSString).utf8String, -1, nil)
            
            while sqlite3_step(queryStatement) == SQLITE_ROW {
                let isSent = sqlite3_column_int(queryStatement, 0) == 1
                let message = String(cString: sqlite3_column_text(queryStatement, 1))
                let timestamp = sqlite3_column_int64(queryStatement, 2)
                messages.append((isSent, message, timestamp))
            }
        }
        sqlite3_finalize(queryStatement)
        return messages
    }
    
    deinit {
        if db != nil {
            sqlite3_close(db)
        }
    }
}
