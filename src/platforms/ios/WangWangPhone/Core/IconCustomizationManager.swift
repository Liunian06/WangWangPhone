import Foundation
import UIKit
import SQLite3

/// 图标自定义记录
struct IconCustomizationRecord {
    let appId: String
    let customIconPath: String
    let updatedAt: Int64
}

/// 图标自定义管理器 - iOS 平台实现
class IconCustomizationManager {
    static let shared = IconCustomizationManager()
    private var db: OpaquePointer?
    private var isInitialized = false
    private let dbName = "wangwang_icon_customization.db"
    private let iconDir = "custom_icons"
    
    private init() { _ = initialize() }
    
    @discardableResult
    func initialize() -> Bool {
        if isInitialized { return true }
        guard let dbPath = getDatabasePath() else { return false }
        if sqlite3_open(dbPath, &db) != SQLITE_OK { return false }
        if !createTables() { return false }
        _ = getIconDirectory()
        isInitialized = true
        return true
    }
    
    deinit { if db != nil { sqlite3_close(db) } }
    
    func copyImageToStorage(_ image: UIImage) -> String? {
        guard let data = image.jpegData(compressionQuality: 0.9) else { return nil }
        let fileName = "icon_\(UUID().uuidString).jpg"
        guard let dir = getIconDirectory() else { return nil }
        let filePath = dir.appendingPathComponent(fileName)
        do {
            try data.write(to: filePath)
            return fileName
        } catch {
            print("IconCustomizationManager error: \(error)")
            return nil
        }
    }
    
    func saveCustomIcon(appId: String, fileName: String) -> Bool {
        if let old = getCustomIcon(appId: appId), let dir = getIconDirectory() {
            try? FileManager.default.removeItem(at: dir.appendingPathComponent(old.customIconPath))
        }
        guard let db = db else { return false }
        let sql = "INSERT OR REPLACE INTO icon_customization (app_id, custom_icon_path, updated_at) VALUES (?, ?, strftime('%s', 'now'));"
        var stmt: OpaquePointer?
        guard sqlite3_prepare_v2(db, sql, -1, &stmt, nil) == SQLITE_OK else { return false }
        sqlite3_bind_text(stmt, 1, (appId as NSString).utf8String, -1, nil)
        sqlite3_bind_text(stmt, 2, (fileName as NSString).utf8String, -1, nil)
        let result = sqlite3_step(stmt)
        sqlite3_finalize(stmt)
        return result == SQLITE_DONE
    }
    
    func getCustomIcon(appId: String) -> IconCustomizationRecord? {
        guard let db = db else { return nil }
        let sql = "SELECT app_id, custom_icon_path, updated_at FROM icon_customization WHERE app_id = ? LIMIT 1;"
        var stmt: OpaquePointer?
        guard sqlite3_prepare_v2(db, sql, -1, &stmt, nil) == SQLITE_OK else { return nil }
        sqlite3_bind_text(stmt, 1, (appId as NSString).utf8String, -1, nil)
        defer { sqlite3_finalize(stmt) }
        if sqlite3_step(stmt) == SQLITE_ROW {
            return IconCustomizationRecord(
                appId: String(cString: sqlite3_column_text(stmt, 0)),
                customIconPath: String(cString: sqlite3_column_text(stmt, 1)),
                updatedAt: sqlite3_column_int64(stmt, 2)
            )
        }
        return nil
    }
    
    func getCustomIconImage(appId: String) -> UIImage? {
        guard let path = getCustomIconFilePath(appId: appId) else { return nil }
        return UIImage(contentsOfFile: path)
    }
    
    func getCustomIconFilePath(appId: String) -> String? {
        guard let record = getCustomIcon(appId: appId), let dir = getIconDirectory() else { return nil }
        let path = dir.appendingPathComponent(record.customIconPath).path
        return FileManager.default.fileExists(atPath: path) ? path : nil
    }
    
    func getAllCustomIcons() -> [IconCustomizationRecord] {
        guard let db = db else { return [] }
        var records: [IconCustomizationRecord] = []
        let sql = "SELECT app_id, custom_icon_path, updated_at FROM icon_customization;"
        var stmt: OpaquePointer?
        guard sqlite3_prepare_v2(db, sql, -1, &stmt, nil) == SQLITE_OK else { return [] }
        defer { sqlite3_finalize(stmt) }
        while sqlite3_step(stmt) == SQLITE_ROW {
            records.append(IconCustomizationRecord(
                appId: String(cString: sqlite3_column_text(stmt, 0)),
                customIconPath: String(cString: sqlite3_column_text(stmt, 1)),
                updatedAt: sqlite3_column_int64(stmt, 2)
            ))
        }
        return records
    }
    
    func clearCustomIcon(appId: String) -> Bool {
        if let record = getCustomIcon(appId: appId), let dir = getIconDirectory() {
            try? FileManager.default.removeItem(at: dir.appendingPathComponent(record.customIconPath))
        }
        guard let db = db else { return false }
        let sql = "DELETE FROM icon_customization WHERE app_id = ?;"
        var stmt: OpaquePointer?
        guard sqlite3_prepare_v2(db, sql, -1, &stmt, nil) == SQLITE_OK else { return false }
        sqlite3_bind_text(stmt, 1, (appId as NSString).utf8String, -1, nil)
        let result = sqlite3_step(stmt)
        sqlite3_finalize(stmt)
        return result == SQLITE_DONE
    }
    
    func clearAllCustomIcons() -> Bool {
        guard let db = db else { return false }
        
        let selectSQL = "SELECT custom_icon_path FROM icon_customization;"
        var statement: OpaquePointer?
        
        if sqlite3_prepare_v2(db, selectSQL, -1, &statement, nil) == SQLITE_OK {
            while sqlite3_step(statement) == SQLITE_ROW {
                if let cString = sqlite3_column_text(statement, 0) {
                    let fileName = String(cString: cString)
                    if let dir = getIconDirectory() {
                        try? FileManager.default.removeItem(at: dir.appendingPathComponent(fileName))
                    }
                }
            }
        }
        sqlite3_finalize(statement)
        
        return executeSQL("DELETE FROM icon_customization;")
    }
    
    private func getDatabasePath() -> String? {
        guard let docs = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first else { return nil }
        return docs.appendingPathComponent(dbName).path
    }
    
    private func getIconDirectory() -> URL? {
        guard let docs = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first else { return nil }
        let dir = docs.appendingPathComponent(iconDir)
        if !FileManager.default.fileExists(atPath: dir.path) {
            try? FileManager.default.createDirectory(at: dir, withIntermediateDirectories: true)
        }
        return dir
    }
    
    private func createTables() -> Bool {
        let sql = """
            CREATE TABLE IF NOT EXISTS icon_customization (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                app_id TEXT NOT NULL UNIQUE,
                custom_icon_path TEXT NOT NULL,
                created_at INTEGER DEFAULT (strftime('%s', 'now')),
                updated_at INTEGER DEFAULT (strftime('%s', 'now'))
            );
        """
        return executeSQL(sql)
    }
    
    @discardableResult
    private func executeSQL(_ sql: String) -> Bool {
        guard let db = db else { return false }
        var stmt: OpaquePointer?
        if sqlite3_prepare_v2(db, sql, -1, &stmt, nil) == SQLITE_OK {
            let result = sqlite3_step(stmt)
            sqlite3_finalize(stmt)
            return result == SQLITE_DONE || result == SQLITE_OK
        }
        sqlite3_finalize(stmt)
        return false
    }
}
