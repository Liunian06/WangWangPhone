import Foundation
import SQLite3

// 联系人信息结构体
struct ContactInfo {
    let id: String
    let nickname: String
    let avatarFileName: String?
    let persona: String
    
    func getPinyinInitial() -> String {
        guard let firstChar = nickname.first else { return "#" }
        if firstChar.isLetter {
            return String(firstChar).uppercased()
        }
        if firstChar >= "\u{4e00}" && firstChar <= "\u{9fff}" {
            return String(getPinyinFirstLetter(firstChar))
        }
        return "#"
    }
    
    private func getPinyinFirstLetter(_ c: Character) -> Character {
        let code = c.unicodeScalars.first?.value ?? 0
        switch code {
        case 0x963F...0x9FFF: return "A"
        case 0x5DF4...0x5EF6: return "B"
        case 0x5F00...0x62FF: return "C"
        case 0x6300...0x6536: return "D"
        case 0x5384...0x5592: return "E"
        case 0x53D1...0x5926: return "F"
        case 0x7518...0x7A00: return "G"
        case 0x54C8...0x5DF3: return "H"
        case 0x673A...0x6770: return "J"
        case 0x5361...0x5494: return "K"
        case 0x5783...0x5D03: return "L"
        case 0x5988...0x5BFF: return "M"
        case 0x54EA...0x5360: return "N"
        case 0x5594...0x5783: return "O"
        case 0x556A...0x5939: return "P"
        case 0x4E03...0x5360: return "Q"
        case 0x7136...0x7518: return "R"
        case 0x4E09...0x53D0: return "S"
        case 0x584C...0x6316: return "T"
        case 0x6316...0x6770: return "W"
        case 0x5915...0x5BFF: return "X"
        case 0x538B...0x5939: return "Y"
        case 0x531D...0x5594: return "Z"
        default: return "#"
        }
    }
}

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
            letter TEXT NOT NULL,
            persona TEXT NOT NULL DEFAULT ''
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
    
    func addContact(id: String, name: String, avatar: String, letter: String, persona: String = "") {
        let insertQuery = "INSERT OR REPLACE INTO contacts (id, name, avatar, letter, persona) VALUES (?, ?, ?, ?, ?)"
        var insertStatement: OpaquePointer?
        
        if sqlite3_prepare_v2(db, insertQuery, -1, &insertStatement, nil) == SQLITE_OK {
            sqlite3_bind_text(insertStatement, 1, (id as NSString).utf8String, -1, nil)
            sqlite3_bind_text(insertStatement, 2, (name as NSString).utf8String, -1, nil)
            sqlite3_bind_text(insertStatement, 3, (avatar as NSString).utf8String, -1, nil)
            sqlite3_bind_text(insertStatement, 4, (letter as NSString).utf8String, -1, nil)
            sqlite3_bind_text(insertStatement, 5, (persona as NSString).utf8String, -1, nil)
            
            if sqlite3_step(insertStatement) == SQLITE_DONE {
                print("Contact saved successfully")
            }
        }
        sqlite3_finalize(insertStatement)
    }
    
    func getAllContacts() -> [ContactInfo] {
        var contacts: [ContactInfo] = []
        let queryString = "SELECT id, name, avatar, letter, persona FROM contacts ORDER BY letter, name"
        var queryStatement: OpaquePointer?
        
        if sqlite3_prepare_v2(db, queryString, -1, &queryStatement, nil) == SQLITE_OK {
            while sqlite3_step(queryStatement) == SQLITE_ROW {
                let id = String(cString: sqlite3_column_text(queryStatement, 0))
                let name = String(cString: sqlite3_column_text(queryStatement, 1))
                let avatar = String(cString: sqlite3_column_text(queryStatement, 2))
                let _ = String(cString: sqlite3_column_text(queryStatement, 3)) // letter
                let persona = String(cString: sqlite3_column_text(queryStatement, 4))
                contacts.append(ContactInfo(id: id, nickname: name, avatarFileName: avatar.isEmpty ? nil : avatar, persona: persona))
            }
        }
        sqlite3_finalize(queryStatement)
        return contacts
    }
    
    func getContactById(_ id: String) -> ContactInfo? {
        let queryString = "SELECT id, name, avatar, letter, persona FROM contacts WHERE id = ?"
        var queryStatement: OpaquePointer?
        
        if sqlite3_prepare_v2(db, queryString, -1, &queryStatement, nil) == SQLITE_OK {
            sqlite3_bind_text(queryStatement, 1, (id as NSString).utf8String, -1, nil)
            
            if sqlite3_step(queryStatement) == SQLITE_ROW {
                let id = String(cString: sqlite3_column_text(queryStatement, 0))
                let name = String(cString: sqlite3_column_text(queryStatement, 1))
                let avatar = String(cString: sqlite3_column_text(queryStatement, 2))
                let _ = String(cString: sqlite3_column_text(queryStatement, 3)) // letter
                let persona = String(cString: sqlite3_column_text(queryStatement, 4))
                sqlite3_finalize(queryStatement)
                return ContactInfo(id: id, nickname: name, avatarFileName: avatar.isEmpty ? nil : avatar, persona: persona)
            }
        }
        sqlite3_finalize(queryStatement)
        return nil
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
    
    func getAvatarFilePath(_ fileName: String) -> String? {
        guard !fileName.isEmpty else { return nil }
        let fileManager = FileManager.default
        guard let documentsURL = fileManager.urls(for: .documentDirectory, in: .userDomainMask).first else { return nil }
        let avatarDir = documentsURL.appendingPathComponent("contact_avatars")
        let filePath = avatarDir.appendingPathComponent(fileName).path
        return fileManager.fileExists(atPath: filePath) ? filePath : nil
    }
    
    deinit {
        if db != nil {
            sqlite3_close(db)
        }
    }
}
