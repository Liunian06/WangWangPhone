import Foundation
import SQLite3

/// 天气缓存记录
struct WeatherCacheRecord {
    let city: String           // 城市名
    let temp: String           // 温度
    let description: String    // 天气描述
    let icon: String           // 天气图标
    let range: String          // 温度范围
    let requestDate: String    // 请求日期 (yyyy-MM-dd)
    let updatedAt: Int64       // 更新时间戳
}

/// 天气缓存管理器 - iOS 平台实现
/// 使用 SQLite 缓存天气数据，每天只请求一次
class WeatherCacheManager {
    
    // MARK: - 单例
    static let shared = WeatherCacheManager()
    
    // MARK: - 属性
    private var db: OpaquePointer?
    private var isInitialized = false
    private let dbName = "wangwang_weather_cache.db"
    
    // MARK: - 初始化
    private init() {
        _ = initialize()
    }
    
    @discardableResult
    func initialize() -> Bool {
        if isInitialized { return true }
        guard let dbPath = getDatabasePath() else {
            print("WeatherCacheManager: 无法获取数据库路径")
            return false
        }
        if sqlite3_open(dbPath, &db) != SQLITE_OK {
            print("WeatherCacheManager: 无法打开数据库")
            return false
        }
        if !createTables() {
            print("WeatherCacheManager: 创建表失败")
            return false
        }
        isInitialized = true
        print("WeatherCacheManager: 初始化成功")
        return true
    }
    
    deinit {
        if db != nil {
            sqlite3_close(db)
        }
    }
    
    // MARK: - 公共方法
    
    /// 获取今天的日期字符串 (yyyy-MM-dd)
    static func getTodayDateString() -> String {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd"
        return formatter.string(from: Date())
    }
    
    /// 保存天气缓存
    func saveWeatherCache(_ record: WeatherCacheRecord) -> Bool {
        guard let db = db else { return false }
        
        let sql = """
            INSERT OR REPLACE INTO weather_cache (city, temp, description, icon, range_info, request_date, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, strftime('%s', 'now'));
        """
        
        var stmt: OpaquePointer?
        guard sqlite3_prepare_v2(db, sql, -1, &stmt, nil) == SQLITE_OK else { return false }
        
        sqlite3_bind_text(stmt, 1, (record.city as NSString).utf8String, -1, nil)
        sqlite3_bind_text(stmt, 2, (record.temp as NSString).utf8String, -1, nil)
        sqlite3_bind_text(stmt, 3, (record.description as NSString).utf8String, -1, nil)
        sqlite3_bind_text(stmt, 4, (record.icon as NSString).utf8String, -1, nil)
        sqlite3_bind_text(stmt, 5, (record.range as NSString).utf8String, -1, nil)
        sqlite3_bind_text(stmt, 6, (record.requestDate as NSString).utf8String, -1, nil)
        
        let result = sqlite3_step(stmt)
        sqlite3_finalize(stmt)
        
        if result == SQLITE_DONE {
            print("WeatherCacheManager: 天气缓存已保存 - \(record.city)")
            return true
        }
        return false
    }
    
    /// 获取今天的天气缓存
    /// - Parameter city: 城市名
    /// - Returns: 如果今天已经请求过，返回缓存数据；否则返回 nil
    func getTodayWeatherCache(city: String) -> WeatherCacheRecord? {
        guard let db = db else { return nil }
        
        let today = WeatherCacheManager.getTodayDateString()
        let sql = """
            SELECT city, temp, description, icon, range_info, request_date, updated_at
            FROM weather_cache
            WHERE city = ? AND request_date = ?
            LIMIT 1;
        """
        
        var stmt: OpaquePointer?
        guard sqlite3_prepare_v2(db, sql, -1, &stmt, nil) == SQLITE_OK else { return nil }
        
        sqlite3_bind_text(stmt, 1, (city as NSString).utf8String, -1, nil)
        sqlite3_bind_text(stmt, 2, (today as NSString).utf8String, -1, nil)
        
        defer { sqlite3_finalize(stmt) }
        
        if sqlite3_step(stmt) == SQLITE_ROW {
            return WeatherCacheRecord(
                city: String(cString: sqlite3_column_text(stmt, 0)),
                temp: String(cString: sqlite3_column_text(stmt, 1)),
                description: String(cString: sqlite3_column_text(stmt, 2)),
                icon: String(cString: sqlite3_column_text(stmt, 3)),
                range: String(cString: sqlite3_column_text(stmt, 4)),
                requestDate: String(cString: sqlite3_column_text(stmt, 5)),
                updatedAt: sqlite3_column_int64(stmt, 6)
            )
        }
        return nil
    }
    
    /// 清除过期缓存（非今天的数据）
    @discardableResult
    func clearExpiredCache() -> Bool {
        guard let db = db else { return false }
        
        let today = WeatherCacheManager.getTodayDateString()
        let sql = "DELETE FROM weather_cache WHERE request_date != ?;"
        
        var stmt: OpaquePointer?
        guard sqlite3_prepare_v2(db, sql, -1, &stmt, nil) == SQLITE_OK else { return false }
        sqlite3_bind_text(stmt, 1, (today as NSString).utf8String, -1, nil)
        let result = sqlite3_step(stmt)
        sqlite3_finalize(stmt)
        return result == SQLITE_DONE
    }
    
    /// 清除所有缓存
    @discardableResult
    func clearAllCache() -> Bool {
        return executeSQL("DELETE FROM weather_cache;")
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
        let sql = """
            CREATE TABLE IF NOT EXISTS weather_cache (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                city TEXT NOT NULL,
                temp TEXT NOT NULL,
                description TEXT NOT NULL,
                icon TEXT NOT NULL,
                range_info TEXT NOT NULL,
                request_date TEXT NOT NULL,
                updated_at INTEGER DEFAULT (strftime('%s', 'now')),
                UNIQUE(city, request_date)
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