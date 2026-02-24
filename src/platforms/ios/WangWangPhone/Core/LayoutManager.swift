import Foundation
import SQLite3

/// 布局项结构 - 记录每个应用图标在网格中的位置
struct LayoutItem: Codable {
    let appId: String       // 应用唯一标识
    let position: Int        // 在网格中的位置索引（0-based）
    let area: String         // 区域标识: "grid" = 主网格, "dock" = Dock 栏
}

/// 布局管理器 - iOS 平台实现
/// 使用 SQLite 持久化存储用户自定义的主屏幕布局
class LayoutManager {
    
    // MARK: - 单例
    static let shared = LayoutManager()
    
    // MARK: - 属性
    private var db: OpaquePointer?
    private var isInitialized = false
    private let dbName = "wangwang_layout.db"
    
    // MARK: - 初始化
    private init() {
        _ = initialize()
    }
    
    /// 初始化数据库
    @discardableResult
    func initialize() -> Bool {
        if isInitialized { return true }
        
        guard let dbPath = getDatabasePath() else {
            print("LayoutManager: 无法获取数据库路径")
            return false
        }
        
        if sqlite3_open(dbPath, &db) != SQLITE_OK {
            print("LayoutManager: 无法打开数据库")
            return false
        }
        
        if !createTables() {
            print("LayoutManager: 创建表失败")
            return false
        }
        
        isInitialized = true
        print("LayoutManager: 初始化成功")
        return true
    }
    
    deinit {
        if db != nil {
            sqlite3_close(db)
        }
    }
    
    // MARK: - 公共方法
    
    /// 保存整个布局（事务操作）
    func saveLayout(_ items: [LayoutItem]) -> Bool {
        guard let db = db else { return false }
        
        // 开启事务
        guard executeSQL("BEGIN TRANSACTION;") else { return false }
        
        // 先清除旧布局
        guard executeSQL("DELETE FROM app_layout;") else {
            executeSQL("ROLLBACK;")
            return false
        }
        
        let insertSQL = """
            INSERT INTO app_layout (app_id, position, area)
            VALUES (?, ?, ?);
        """
        
        var statement: OpaquePointer?
        guard sqlite3_prepare_v2(db, insertSQL, -1, &statement, nil) == SQLITE_OK else {
            executeSQL("ROLLBACK;")
            return false
        }
        
        for item in items {
            sqlite3_reset(statement)
            sqlite3_bind_text(statement, 1, (item.appId as NSString).utf8String, -1, nil)
            sqlite3_bind_int(statement, 2, Int32(item.position))
            sqlite3_bind_text(statement, 3, (item.area as NSString).utf8String, -1, nil)
            
            if sqlite3_step(statement) != SQLITE_DONE {
                sqlite3_finalize(statement)
                executeSQL("ROLLBACK;")
                return false
            }
        }
        
        sqlite3_finalize(statement)
        
        guard executeSQL("COMMIT;") else {
            executeSQL("ROLLBACK;")
            return false
        }
        
        print("LayoutManager: 布局已保存 (\(items.count) 项)")
        return true
    }
    
    /// 获取保存的布局
    func getLayout() -> [LayoutItem] {
        guard let db = db else { return [] }
        
        let selectSQL = """
            SELECT app_id, position, area
            FROM app_layout
            ORDER BY area ASC, position ASC;
        """
        
        var statement: OpaquePointer?
        guard sqlite3_prepare_v2(db, selectSQL, -1, &statement, nil) == SQLITE_OK else {
            return []
        }
        
        defer { sqlite3_finalize(statement) }
        
        var items: [LayoutItem] = []
        while sqlite3_step(statement) == SQLITE_ROW {
            let appId = String(cString: sqlite3_column_text(statement, 0))
            let position = Int(sqlite3_column_int(statement, 1))
            let area = String(cString: sqlite3_column_text(statement, 2))
            
            items.append(LayoutItem(appId: appId, position: position, area: area))
        }
        
        return items
    }
    
    /// 清除所有布局
    @discardableResult
    func clearLayout() -> Bool {
        return executeSQL("DELETE FROM app_layout;")
    }
    
    /// 检查是否有已保存的布局
    func hasLayout() -> Bool {
        return !getLayout().isEmpty
    }
    
    /// 恢复默认设置
    func resetToDefaultSettings() -> Bool {
        // 1. 清除布局
        _ = clearLayout()
        
        // 2. 清除壁纸
        _ = WallpaperManager.shared.clearAllWallpapers()
        
        // 3. 清除天气缓存
        _ = WeatherCacheManager.shared.clearAllCache()
        _ = WeatherCacheManager.shared.saveManualLocation(nil)
        
        // 4. 重置用户资料
        _ = UserProfileManager.shared.resetToDefault()
        
        return true
    }
    
    // MARK: - 私有方法
    
    private func getDatabasePath() -> String? {
        guard let documentsPath = FileManager.default.urls(
            for: .documentDirectory,
            in: .userDomainMask
        ).first else {
            return nil
        }
        return documentsPath.appendingPathComponent(dbName).path
    }
    
    private func createTables() -> Bool {
        let createTableSQL = """
            CREATE TABLE IF NOT EXISTS app_layout (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                app_id TEXT NOT NULL,
                position INTEGER NOT NULL,
                area TEXT NOT NULL DEFAULT 'grid',
                created_at INTEGER DEFAULT (strftime('%s', 'now')),
                updated_at INTEGER DEFAULT (strftime('%s', 'now'))
            );
        """
        
        let createIndexSQL = """
            CREATE UNIQUE INDEX IF NOT EXISTS idx_layout_app ON app_layout(app_id, area);
        """
        
        guard executeSQL(createTableSQL) else { return false }
        guard executeSQL(createIndexSQL) else { return false }
        return true
    }
    
    @discardableResult
    private func executeSQL(_ sql: String) -> Bool {
        guard let db = db else { return false }
        
        var statement: OpaquePointer?
        if sqlite3_prepare_v2(db, sql, -1, &statement, nil) == SQLITE_OK {
            let result = sqlite3_step(statement)
            sqlite3_finalize(statement)
            return result == SQLITE_DONE || result == SQLITE_OK
        }
        sqlite3_finalize(statement)
        return false
    }
}
