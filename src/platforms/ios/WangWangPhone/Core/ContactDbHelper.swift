import Foundation
import SQLite3

class ContactDbHelper {
    static let shared = ContactDbHelper()
    private var db: OpaquePointer?
    
    private init() {
        openDatabase()
        createTable()
    }
    
    private func openDatabase() {
        let fileURL = try! FileManager.default
            .url(for: .documentDirectory, in: .userDomainMask, appropriateFor: nil, create: false)
            .appendingPathComponent("contacts.db")
        
        if sqlite3_open(fileURL.path, &db) != SQLITE_OK {
            print("Error opening database")
        }
    }
    
    private func createTable() {
        let createTableQuery = """
        CREATE TABLE IF NOT EXISTS contacts (
            id TEXT PRIMARY KEY,
            name TEXT NOT NULL,
            avatar TEXT NOT NULL,
            letter TEXT NOT NULL
        )
        """
        
        var createTableStatement: OpaquePointer?
        if sqlite3_prepare_v2(db, createTableQuery, -1, &createTableStatement, nil) == SQLITE_OK {
            if sqlite3_step(createTableStatement) == SQLITE_DONE {
                print("Contacts table created")
            }
        }
        sqlite3_finalize(createTableStatement)
    }
    
    func addContact(id: String, name: String, avatar: String, letter: String) {
        let insertQuery = "INSERT OR REPLACE INTO contacts (id, name, avatar, letter) VALUES (?, ?, ?, ?)"
        var insertStatement: OpaquePointer?
        
        if sqlite3_prepare_v2(db, insertQuery, -1, &insertStatement, nil) == SQLITE_OK {
            sqlite3_bind_text(insertStatement, 1, (id as NSString).utf8String, -1, nil)
            sqlite3_bind_text(insertStatement, 2, (name as NSString).utf8String, -1, nil)
            sqlite3_bind_text(insertStatement, 3, (avatar as NSString).utf8String, -1, nil)
            sqlite3_bind_text(insertStatement, 4, (letter as NSString).utf8String, -1, nil)
            
            if sqlite3_step(insertStatement) == SQLITE_DONE {
                print("Contact saved successfully")
            }
        }
        sqlite3_finalize(insertStatement)
    }
    
    func getAllContacts() -> [(id: String, name: String, avatar: String, letter: String)] {
        var contacts: [(String, String, String, String)] = []
        let queryString = "SELECT id, name, avatar, letter FROM contacts ORDER BY letter, name"
        var queryStatement: OpaquePointer?
        
        if sqlite3_prepare_v2(db, queryString, -1, &queryStatement, nil) == SQLITE_OK {
            while sqlite3_step(queryStatement) == SQLITE_ROW {
                let id = String(cString: sqlite3_column_text(queryStatement, 0))
                let name = String(cString: sqlite3_column_text(queryStatement, 1))
                let avatar = String(cString: sqlite3_column_text(queryStatement, 2))
                let letter = String(cString: sqlite3_column_text(queryStatement, 3))
                contacts.append((id, name, avatar, letter))
            }
        }
        sqlite3_finalize(queryStatement)
        return contacts
    }
    
    func deleteContact(id: String) {
        let deleteQuery = "DELETE FROM contacts WHERE id = ?"
        var deleteStatement: OpaquePointer?
        
        if sqlite3_prepare_v2(db, deleteQuery, -1, &deleteStatement, nil) == SQLITE_OK {
            sqlite3_bind_text(deleteStatement, 1, (id as NSString).utf8String, -1, nil)
            
            if sqlite3_step(deleteStatement) == SQLITE_DONE {
                print("Contact deleted successfully")
            }
        }
        sqlite3_finalize(deleteStatement)
    }
    
    deinit {
        if db != nil {
            sqlite3_close(db)
        }
    }
}
