import Foundation
import UIKit
import SQLite3

/// 用户资料数据
struct UserProfile {
    var nickname: String = "我的昵称"
    var signature: String = "游荡的孤高灵魂不需要栖身之地"
    var avatarFileName: String = ""
    var coverFileName: String = ""
    var updatedAt: Int64 = 0
}

/// 用户资料管理器 - iOS 平台实现
/// 负责用户昵称、签名、头像、朋友圈封面的持久化存储
/// 图片文件会被复制到应用内部存储目录，确保持久化不丢失
class UserProfileManager {
    static let shared = UserProfileManager()
    private var db: OpaquePointer?
    private var isInitialized = false
    private let dbName = "wangwang_user_profile.db"
    private let profileImagesDir = "profile_images"

    private init() { _ = initialize() }

    @discardableResult
    func initialize() -> Bool {
        if isInitialized { return true }
        guard let dbPath = getDatabasePath() else { return false }
        if sqlite3_open(dbPath, &db) != SQLITE_OK { return false }
        if !createTables() { return false }
        _ = getProfileImagesDirectory()
        isInitialized = true
        return true
    }

    deinit {
        if db != nil { sqlite3_close(db) }
    }

    // MARK: - 图片持久化

    /// 将 UIImage 复制到持久化目录
    /// - Parameters:
    ///   - image: 要保存的图片
    ///   - prefix: 文件名前缀（如 "avatar_" 或 "cover_"）
    /// - Returns: 持久化后的文件名，失败返回 nil
    func copyImageToStorage(_ image: UIImage, prefix: String) -> String? {
        guard let data = image.jpegData(compressionQuality: 0.9) else { return nil }
        let fileName = "\(prefix)\(UUID().uuidString).jpg"
        guard let dir = getProfileImagesDirectory() else { return nil }
        let filePath = dir.appendingPathComponent(fileName)
        do {
            try data.write(to: filePath)
            return fileName
        } catch {
            print("UserProfileManager copyImageToStorage error: \(error)")
            return nil
        }
    }

    // MARK: - 用户资料 CRUD

    /// 获取用户资料
    func getUserProfile() -> UserProfile {
        guard let db = db else { return UserProfile() }
        let sql = "SELECT nickname, signature, avatar_file, cover_file, updated_at FROM user_profile WHERE id = 1 LIMIT 1;"
        var stmt: OpaquePointer?
        guard sqlite3_prepare_v2(db, sql, -1, &stmt, nil) == SQLITE_OK else { return UserProfile() }
        defer { sqlite3_finalize(stmt) }
        if sqlite3_step(stmt) == SQLITE_ROW {
            let nickname = sqlite3_column_text(stmt, 0).map { String(cString: $0) } ?? "我的昵称"
            let signature = sqlite3_column_text(stmt, 1).map { String(cString: $0) } ?? "游荡的孤高灵魂不需要栖身之地"
            let avatarFile = sqlite3_column_text(stmt, 2).map { String(cString: $0) } ?? ""
            let coverFile = sqlite3_column_text(stmt, 3).map { String(cString: $0) } ?? ""
            let updatedAt = sqlite3_column_int64(stmt, 4)
            return UserProfile(
                nickname: nickname,
                signature: signature,
                avatarFileName: avatarFile,
                coverFileName: coverFile,
                updatedAt: updatedAt
            )
        }
        return UserProfile()
    }

    /// 更新昵称
    @discardableResult
    func updateNickname(_ nickname: String) -> Bool {
        guard let db = db else { return false }
        let sql = "UPDATE user_profile SET nickname = ?, updated_at = strftime('%s', 'now') WHERE id = 1;"
        var stmt: OpaquePointer?
        guard sqlite3_prepare_v2(db, sql, -1, &stmt, nil) == SQLITE_OK else { return false }
        sqlite3_bind_text(stmt, 1, (nickname as NSString).utf8String, -1, nil)
        let result = sqlite3_step(stmt)
        sqlite3_finalize(stmt)
        return result == SQLITE_DONE
    }

    /// 更新签名
    @discardableResult
    func updateSignature(_ signature: String) -> Bool {
        guard let db = db else { return false }
        let sql = "UPDATE user_profile SET signature = ?, updated_at = strftime('%s', 'now') WHERE id = 1;"
        var stmt: OpaquePointer?
        guard sqlite3_prepare_v2(db, sql, -1, &stmt, nil) == SQLITE_OK else { return false }
        sqlite3_bind_text(stmt, 1, (signature as NSString).utf8String, -1, nil)
        let result = sqlite3_step(stmt)
        sqlite3_finalize(stmt)
        return result == SQLITE_DONE
    }

    /// 更新头像（同时删除旧头像文件）
    /// - Parameter image: 新头像图片
    /// - Returns: 新文件名，失败返回 nil
    @discardableResult
    func updateAvatar(_ image: UIImage) -> String? {
        // 删除旧头像文件
        let oldProfile = getUserProfile()
        if !oldProfile.avatarFileName.isEmpty, let dir = getProfileImagesDirectory() {
            let oldPath = dir.appendingPathComponent(oldProfile.avatarFileName)
            try? FileManager.default.removeItem(at: oldPath)
        }

        // 复制新图片
        guard let newFileName = copyImageToStorage(image, prefix: "avatar_") else { return nil }

        // 更新数据库
        guard let db = db else { return nil }
        let sql = "UPDATE user_profile SET avatar_file = ?, updated_at = strftime('%s', 'now') WHERE id = 1;"
        var stmt: OpaquePointer?
        guard sqlite3_prepare_v2(db, sql, -1, &stmt, nil) == SQLITE_OK else { return nil }
        sqlite3_bind_text(stmt, 1, (newFileName as NSString).utf8String, -1, nil)
        let result = sqlite3_step(stmt)
        sqlite3_finalize(stmt)
        return result == SQLITE_DONE ? newFileName : nil
    }

    /// 更新朋友圈封面（同时删除旧封面文件）
    /// - Parameter image: 新封面图片
    /// - Returns: 新文件名，失败返回 nil
    @discardableResult
    func updateCover(_ image: UIImage) -> String? {
        // 删除旧封面文件
        let oldProfile = getUserProfile()
        if !oldProfile.coverFileName.isEmpty, let dir = getProfileImagesDirectory() {
            let oldPath = dir.appendingPathComponent(oldProfile.coverFileName)
            try? FileManager.default.removeItem(at: oldPath)
        }

        // 复制新图片
        guard let newFileName = copyImageToStorage(image, prefix: "cover_") else { return nil }

        // 更新数据库
        guard let db = db else { return nil }
        let sql = "UPDATE user_profile SET cover_file = ?, updated_at = strftime('%s', 'now') WHERE id = 1;"
        var stmt: OpaquePointer?
        guard sqlite3_prepare_v2(db, sql, -1, &stmt, nil) == SQLITE_OK else { return nil }
        sqlite3_bind_text(stmt, 1, (newFileName as NSString).utf8String, -1, nil)
        let result = sqlite3_step(stmt)
        sqlite3_finalize(stmt)
        return result == SQLITE_DONE ? newFileName : nil
    }

    // MARK: - 图片路径获取

    /// 获取头像 UIImage
    func getAvatarImage() -> UIImage? {
        guard let path = getAvatarFilePath() else { return nil }
        return UIImage(contentsOfFile: path)
    }

    /// 获取封面 UIImage
    func getCoverImage() -> UIImage? {
        guard let path = getCoverFilePath() else { return nil }
        return UIImage(contentsOfFile: path)
    }

    /// 获取头像文件路径
    func getAvatarFilePath() -> String? {
        let profile = getUserProfile()
        guard !profile.avatarFileName.isEmpty, let dir = getProfileImagesDirectory() else { return nil }
        let path = dir.appendingPathComponent(profile.avatarFileName).path
        return FileManager.default.fileExists(atPath: path) ? path : nil
    }

    /// 获取封面文件路径
    func getCoverFilePath() -> String? {
        let profile = getUserProfile()
        guard !profile.coverFileName.isEmpty, let dir = getProfileImagesDirectory() else { return nil }
        let path = dir.appendingPathComponent(profile.coverFileName).path
        return FileManager.default.fileExists(atPath: path) ? path : nil
    }

    // MARK: - Private helpers

    private func getDatabasePath() -> String? {
        guard let docs = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first else { return nil }
        return docs.appendingPathComponent(dbName).path
    }

    private func getProfileImagesDirectory() -> URL? {
        guard let docs = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first else { return nil }
        let dir = docs.appendingPathComponent(profileImagesDir)
        if !FileManager.default.fileExists(atPath: dir.path) {
            try? FileManager.default.createDirectory(at: dir, withIntermediateDirectories: true)
        }
        return dir
    }

    private func createTables() -> Bool {
        let createSQL = """
            CREATE TABLE IF NOT EXISTS user_profile (
                id INTEGER PRIMARY KEY CHECK (id = 1),
                nickname TEXT NOT NULL DEFAULT '我的昵称',
                signature TEXT NOT NULL DEFAULT '游荡的孤高灵魂不需要栖身之地',
                avatar_file TEXT DEFAULT '',
                cover_file TEXT DEFAULT '',
                created_at INTEGER DEFAULT (strftime('%s', 'now')),
                updated_at INTEGER DEFAULT (strftime('%s', 'now'))
            );
        """
        if !executeSQL(createSQL) { return false }

        let insertSQL = "INSERT OR IGNORE INTO user_profile (id, nickname, signature, avatar_file, cover_file) VALUES (1, '我的昵称', '游荡的孤高灵魂不需要栖身之地', '', '');"
        return executeSQL(insertSQL)
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
    
    /// 重置资料为默认值
    func resetToDefault() -> Bool {
        // 删除旧头像和封面文件
        let profile = getUserProfile()
        if let dir = getProfileImagesDirectory() {
            if !profile.avatarFileName.isEmpty {
                try? FileManager.default.removeItem(at: dir.appendingPathComponent(profile.avatarFileName))
            }
            if !profile.coverFileName.isEmpty {
                try? FileManager.default.removeItem(at: dir.appendingPathComponent(profile.coverFileName))
            }
        }
        
        let sql = """
            UPDATE user_profile
            SET nickname = '我的昵称',
                signature = '游荡的孤高灵魂不需要栖身之地',
                avatar_file = '',
                cover_file = '',
                updated_at = strftime('%s', 'now')
            WHERE id = 1;
        """
        return executeSQL(sql)
    }
}