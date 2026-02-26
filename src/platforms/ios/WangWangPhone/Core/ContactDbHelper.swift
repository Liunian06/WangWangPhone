import Foundation
import SQLite3
import UIKit

/// 联系人信息结构体（对齐 Android ContactInfo）
struct ContactInfo {
    let id: String
    let nickname: String
    var wechatId: String = ""
    var region: String = ""
    let persona: String
    let avatarFileName: String?
    let createdAt: Int64
    let updatedAt: Int64
    
    init(id: String, nickname: String, wechatId: String = "", region: String = "", persona: String = "", avatarFileName: String? = nil, createdAt: Int64 = 0, updatedAt: Int64 = 0) {
        self.id = id
        self.nickname = nickname
        self.wechatId = wechatId
        self.region = region
        self.persona = persona
        self.avatarFileName = avatarFileName
        self.createdAt = createdAt
        self.updatedAt = updatedAt
    }
    
    func getPinyinInitial() -> String {
        guard let firstChar = nickname.first else { return "#" }
        if firstChar.isLetter && firstChar.asciiValue != nil {
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
    private let contactImagesDir = "contact_images"
    
    private init() {
        openDatabase()
        createTable()
        migrateIfNeeded()
    }
    
    private func openDatabase() {
        let fileURL = try! FileManager.default
            .url(for: .documentDirectory, in: .userDomainMask, appropriateFor: nil, create: false)
            .appendingPathComponent("wangwang_contacts.db")
        
        if sqlite3_open(fileURL.path, &db) != SQLITE_OK {
            print("Error opening contacts database")
        }
    }
    
    private func createTable() {
        let sql = """
        CREATE TABLE IF NOT EXISTS contacts (
            id TEXT PRIMARY KEY,
            nickname TEXT NOT NULL,
            wechat_id TEXT DEFAULT '',
            region TEXT DEFAULT '',
            persona TEXT NOT NULL DEFAULT '',
            avatar_file TEXT DEFAULT '',
            created_at INTEGER DEFAULT (strftime('%s', 'now')),
            updated_at INTEGER DEFAULT (strftime('%s', 'now'))
        )
        """
        executeSQL(sql)
    }
    
    /// 迁移旧表结构（如果存在旧字段 name/avatar/letter）
    private func migrateIfNeeded() {
        let pragmaQuery = "PRAGMA table_info(contacts)"
        var stmt: OpaquePointer?
        var hasOldName = false
        var hasWechatId = false
        
        if sqlite3_prepare_v2(db, pragmaQuery, -1, &stmt, nil) == SQLITE_OK {
            while sqlite3_step(stmt) == SQLITE_ROW {
                if let namePtr = sqlite3_column_text(stmt, 1) {
                    let colName = String(cString: namePtr)
                    if colName == "name" { hasOldName = true }
                    if colName == "wechat_id" { hasWechatId = true }
                }
            }
        }
        sqlite3_finalize(stmt)
        
        if hasOldName && !hasWechatId {
            print("ContactDbHelper: Migrating old contacts table...")
            executeSQL("ALTER TABLE contacts RENAME TO contacts_old")
            createTable()
            executeSQL("""
                INSERT INTO contacts (id, nickname, persona, avatar_file, created_at, updated_at)
                SELECT id, name, persona, avatar, strftime('%s', 'now'), strftime('%s', 'now')
                FROM contacts_old
            """)
            executeSQL("DROP TABLE IF EXISTS contacts_old")
            print("ContactDbHelper: Migration complete")
        }
    }
    
    private func executeSQL(_ sql: String) {
        var stmt: OpaquePointer?
        if sqlite3_prepare_v2(db, sql, -1, &stmt, nil) == SQLITE_OK {
            if sqlite3_step(stmt) != SQLITE_DONE {
                let errmsg = String(cString: sqlite3_errmsg(db))
                print("SQL error: \(errmsg)")
            }
        } else {
            let errmsg = String(cString: sqlite3_errmsg(db))
            print("Prepare error: \(errmsg)")
        }
        sqlite3_finalize(stmt)
    }
    
    // MARK: - 图片存储
    
    private func getContactImagesDirectory() -> URL {
        let documentsURL = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first!
        let dir = documentsURL.appendingPathComponent(contactImagesDir)
        if !FileManager.default.fileExists(atPath: dir.path) {
            try? FileManager.default.createDirectory(at: dir, withIntermediateDirectories: true)
        }
        return dir
    }
    
    func copyImageToStorage(_ image: UIImage, prefix: String = "contact_") -> String? {
        guard let data = image.jpegData(compressionQuality: 0.9) else { return nil }
        let fileName = "\(prefix)\(UUID().uuidString).jpg"
        let filePath = getContactImagesDirectory().appendingPathComponent(fileName)
        do {
            try data.write(to: filePath)
            return fileName
        } catch {
            print("Error saving contact image: \(error)")
            return nil
        }
    }
    
    func getAvatarFilePath(_ fileName: String) -> String? {
        guard !fileName.isEmpty else { return nil }
        let filePath = getContactImagesDirectory().appendingPathComponent(fileName).path
        return FileManager.default.fileExists(atPath: filePath) ? filePath : nil
    }
    
    // MARK: - CRUD
    
    func addContact(nickname: String, wechatId: String = "", region: String = "", persona: String = "", avatarImage: UIImage? = nil) -> String? {
        let contactId = UUID().uuidString
        let avatarFileName = avatarImage.flatMap { copyImageToStorage($0) } ?? ""
        let now = Int64(Date().timeIntervalSince1970)
        
        let sql = "INSERT INTO contacts (id, nickname, wechat_id, region, persona, avatar_file, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)"
        var stmt: OpaquePointer?
        guard sqlite3_prepare_v2(db, sql, -1, &stmt, nil) == SQLITE_OK else { return nil }
        
        sqlite3_bind_text(stmt, 1, (contactId as NSString).utf8String, -1, nil)
        sqlite3_bind_text(stmt, 2, (nickname as NSString).utf8String, -1, nil)
        sqlite3_bind_text(stmt, 3, (wechatId as NSString).utf8String, -1, nil)
        sqlite3_bind_text(stmt, 4, (region as NSString).utf8String, -1, nil)
        sqlite3_bind_text(stmt, 5, (persona as NSString).utf8String, -1, nil)
        sqlite3_bind_text(stmt, 6, (avatarFileName as NSString).utf8String, -1, nil)
        sqlite3_bind_int64(stmt, 7, now)
        sqlite3_bind_int64(stmt, 8, now)
        
        let result = sqlite3_step(stmt)
        sqlite3_finalize(stmt)
        return result == SQLITE_DONE ? contactId : nil
    }
    
    /// 旧接口兼容
    func addContact(id: String, name: String, avatar: String, letter: String, persona: String = "") {
        let now = Int64(Date().timeIntervalSince1970)
        let sql = "INSERT OR REPLACE INTO contacts (id, nickname, persona, avatar_file, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?)"
        var stmt: OpaquePointer?
        guard sqlite3_prepare_v2(db, sql, -1, &stmt, nil) == SQLITE_OK else { return }
        sqlite3_bind_text(stmt, 1, (id as NSString).utf8String, -1, nil)
        sqlite3_bind_text(stmt, 2, (name as NSString).utf8String, -1, nil)
        sqlite3_bind_text(stmt, 3, (persona as NSString).utf8String, -1, nil)
        sqlite3_bind_text(stmt, 4, (avatar as NSString).utf8String, -1, nil)
        sqlite3_bind_int64(stmt, 5, now)
        sqlite3_bind_int64(stmt, 6, now)
        sqlite3_step(stmt)
        sqlite3_finalize(stmt)
    }
    
    func updateContact(id: String, nickname: String, wechatId: String, region: String, persona: String, avatarImage: UIImage? = nil) -> Bool {
        guard let existing = getContactById(id) else { return false }
        var avatarFileName = existing.avatarFileName ?? ""
        
        if let newImage = avatarImage {
            // 删除旧头像
            if !avatarFileName.isEmpty {
                let oldPath = getContactImagesDirectory().appendingPathComponent(avatarFileName).path
                try? FileManager.default.removeItem(atPath: oldPath)
            }
            avatarFileName = copyImageToStorage(newImage) ?? avatarFileName
        }
        
        let now = Int64(Date().timeIntervalSince1970)
        let sql = "UPDATE contacts SET nickname = ?, wechat_id = ?, region = ?, persona = ?, avatar_file = ?, updated_at = ? WHERE id = ?"
        var stmt: OpaquePointer?
        guard sqlite3_prepare_v2(db, sql, -1, &stmt, nil) == SQLITE_OK else { return false }
        
        sqlite3_bind_text(stmt, 1, (nickname as NSString).utf8String, -1, nil)
        sqlite3_bind_text(stmt, 2, (wechatId as NSString).utf8String, -1, nil)
        sqlite3_bind_text(stmt, 3, (region as NSString).utf8String, -1, nil)
        sqlite3_bind_text(stmt, 4, (persona as NSString).utf8String, -1, nil)
        sqlite3_bind_text(stmt, 5, (avatarFileName as NSString).utf8String, -1, nil)
        sqlite3_bind_int64(stmt, 6, now)
        sqlite3_bind_text(stmt, 7, (id as NSString).utf8String, -1, nil)
        
        let result = sqlite3_step(stmt)
        sqlite3_finalize(stmt)
        return result == SQLITE_DONE
    }
    
    func getAllContacts() -> [ContactInfo] {
        var contacts: [ContactInfo] = []
        let sql = "SELECT id, nickname, wechat_id, region, persona, avatar_file, created_at, updated_at FROM contacts ORDER BY nickname ASC"
        var stmt: OpaquePointer?
        
        guard sqlite3_prepare_v2(db, sql, -1, &stmt, nil) == SQLITE_OK else { return contacts }
        
        while sqlite3_step(stmt) == SQLITE_ROW {
            if let contact = parseContact(stmt) {
                contacts.append(contact)
            }
        }
        sqlite3_finalize(stmt)
        return contacts.sorted { $0.getPinyinInitial() < $1.getPinyinInitial() }
    }
    
    func getContactById(_ id: String) -> ContactInfo? {
        let sql = "SELECT id, nickname, wechat_id, region, persona, avatar_file, created_at, updated_at FROM contacts WHERE id = ? LIMIT 1"
        var stmt: OpaquePointer?
        
        guard sqlite3_prepare_v2(db, sql, -1, &stmt, nil) == SQLITE_OK else { return nil }
        sqlite3_bind_text(stmt, 1, (id as NSString).utf8String, -1, nil)
        
        var contact: ContactInfo?
        if sqlite3_step(stmt) == SQLITE_ROW {
            contact = parseContact(stmt)
        }
        sqlite3_finalize(stmt)
        return contact
    }
    
    func deleteContact(id: String) -> Bool {
        if let contact = getContactById(id), let avatarFile = contact.avatarFileName, !avatarFile.isEmpty {
            let path = getContactImagesDirectory().appendingPathComponent(avatarFile).path
            try? FileManager.default.removeItem(atPath: path)
        }
        let sql = "DELETE FROM contacts WHERE id = ?"
        var stmt: OpaquePointer?
        guard sqlite3_prepare_v2(db, sql, -1, &stmt, nil) == SQLITE_OK else { return false }
        sqlite3_bind_text(stmt, 1, (id as NSString).utf8String, -1, nil)
        let result = sqlite3_step(stmt)
        sqlite3_finalize(stmt)
        return result == SQLITE_DONE
    }
    
    // MARK: - Private
    
    private func parseContact(_ stmt: OpaquePointer?) -> ContactInfo? {
        guard let stmt = stmt,
              let idPtr = sqlite3_column_text(stmt, 0),
              let nicknamePtr = sqlite3_column_text(stmt, 1) else { return nil }
        
        let wechatIdPtr = sqlite3_column_text(stmt, 2)
        let regionPtr = sqlite3_column_text(stmt, 3)
        let personaPtr = sqlite3_column_text(stmt, 4)
        let avatarPtr = sqlite3_column_text(stmt, 5)
        
        let avatarFile = avatarPtr != nil ? String(cString: avatarPtr!) : ""
        
        return ContactInfo(
            id: String(cString: idPtr),
            nickname: String(cString: nicknamePtr),
            wechatId: wechatIdPtr != nil ? String(cString: wechatIdPtr!) : "",
            region: regionPtr != nil ? String(cString: regionPtr!) : "",
            persona: personaPtr != nil ? String(cString: personaPtr!) : "",
            avatarFileName: avatarFile.isEmpty ? nil : avatarFile,
            createdAt: sqlite3_column_int64(stmt, 6),
            updatedAt: sqlite3_column_int64(stmt, 7)
        )
    }
    
    deinit {
        if db != nil {
            sqlite3_close(db)
        }
    }
}
