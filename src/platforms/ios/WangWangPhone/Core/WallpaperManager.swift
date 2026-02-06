import Foundation
import UIKit
import SQLite3

/// 壁纸类型
enum WallpaperType: String {
    case lock = "lock"
    case home = "home"
}

/// 壁纸记录
struct WallpaperRecord {
    let wallpaperType: String
    let fileName: String
    let updatedAt: Int64
}

/// 壁纸管理器 - iOS 平台实现
class WallpaperManager {
    static let shared = WallpaperManager()
    private var db: OpaquePointer?
    private var isInitialized = false
    private let dbName = "wangwang_wallpaper.db"
    private let wallpaperDir = "wallpapers"
    private init() { _ = initialize() }
    @discardableResult
    func initialize() -> Bool {
        if isInitialized { return true }
        guard let dbPath = getDatabasePath() else { return false }
        if sqlite3_open(dbPath, &db) != SQLITE_OK { return false }
        if !createTables() { return false }
        _ = getWallpaperDirectory()
        isInitialized = true
        return true
    }
    deinit { if db != nil { sqlite3_close(db) } }
    func copyImageToStorage(_ image: UIImage) -> String? {
        guard let data = image.jpegData(compressionQuality: 0.9) else { return nil }
        let fileName = "wp_\(UUID().uuidString).jpg"
        guard let dir = getWallpaperDirectory() else { return nil }
        let filePath = dir.appendingPathComponent(fileName)
        do {
            try data.write(to: filePath)
            return fileName
        } catch {
            print("WallpaperManager error: \(error)")
            return nil
        }
    }
    func saveWallpaper(type: WallpaperType, fileName: String) -> Bool {
        if let old = getWallpaper(type: type), let dir = getWallpaperDirectory() {
            try? FileManager.default.removeItem(at: dir.appendingPathComponent(old.fileName))
        }
        guard let db = db else { return false }
        let sql = "INSERT OR REPLACE INTO wallpaper (wallpaper_type, file_name, updated_at) VALUES (?, ?, strftime('%s', 'now'));"
        var stmt: OpaquePointer?
        guard sqlite3_prepare_v2(db, sql, -1, &stmt, nil) == SQLITE_OK else { return false }
        sqlite3_bind_text(stmt, 1, (type.rawValue as NSString).utf8String, -1, nil)
        sqlite3_bind_text(stmt, 2, (fileName as NSString).utf8String, -1, nil)
        let result = sqlite3_step(stmt)
        sqlite3_finalize(stmt)
        return result == SQLITE_DONE
    }
    func getWallpaper(type: WallpaperType) -> WallpaperRecord? {
        guard let db = db else { return nil }
        let sql = "SELECT wallpaper_type, file_name, updated_at FROM wallpaper WHERE wallpaper_type = ? LIMIT 1;"
        var stmt: OpaquePointer?
        guard sqlite3_prepare_v2(db, sql, -1, &stmt, nil) == SQLITE_OK else { return nil }
        sqlite3_bind_text(stmt, 1, (type.rawValue as NSString).utf8String, -1, nil)
        defer { sqlite3_finalize(stmt) }
        if sqlite3_step(stmt) == SQLITE_ROW {
            return WallpaperRecord(
                wallpaperType: String(cString: sqlite3_column_text(stmt, 0)),
                fileName: String(cString: sqlite3_column_text(stmt, 1)),
                updatedAt: sqlite3_column_int64(stmt, 2)
            )
        }
        return nil
    }
    func getWallpaperImage(type: WallpaperType) -> UIImage? {
        guard let path = getWallpaperFilePath(type: type) else { return nil }
        return UIImage(contentsOfFile: path)
    }
    func getWallpaperFilePath(type: WallpaperType) -> String? {
        guard let record = getWallpaper(type: type), let dir = getWallpaperDirectory() else { return nil }
        let path = dir.appendingPathComponent(record.fileName).path
        return FileManager.default.fileExists(atPath: path) ? path : nil
    }
    private func getDatabasePath() -> String? {
        guard let docs = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first else { return nil }
        return docs.appendingPathComponent(dbName).path
    }
    private func getWallpaperDirectory() -> URL? {
        guard let docs = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first else { return nil }
        let dir = docs.appendingPathComponent(wallpaperDir)
        if !FileManager.default.fileExists(atPath: dir.path) {
            try? FileManager.default.createDirectory(at: dir, withIntermediateDirectories: true)
        }
        return dir
    }
    private func createTables() -> Bool {
        let sql = """
            CREATE TABLE IF NOT EXISTS wallpaper (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                wallpaper_type TEXT NOT NULL UNIQUE,
                file_name TEXT NOT NULL,
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
