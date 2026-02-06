import Foundation
import SQLite3
import UIKit

/// 授权记录结构
struct LicenseRecord: Codable {
    let licenseKey: String
    let machineId: String
    let expirationTime: Int64
    let licenseType: String
    let activationTime: Int64
}

/// 布局记录结构
struct AppLayoutRecord {
    let appId: String
    let col: Int
    let row: Int
    let spanX: Int
    let spanY: Int
}

/// 授权载荷结构
struct LicensePayload {
    let machineId: String
    let expirationTime: Int64
    let type: String
    let salt: String
}

/// 授权操作结果
enum LicenseResult {
    case success(LicenseRecord)
    case error(String)
}

/// 授权管理器 - iOS 平台实现
/// 
/// 该类封装了与 C++ Core LicenseManager 的交互。
/// 当前版本使用 SQLite 进行本地持久化。
class LicenseManager {
    
    // MARK: - 单例
    static let shared = LicenseManager()
    
    // MARK: - 属性
    private var db: OpaquePointer?
    private var cachedLicense: LicenseRecord?
    private var isInitialized = false
    
    private let dbName = "wangwang_license.db"
    
    // MARK: - 初始化
    private init() {}
    
    /// 初始化数据库
    func initialize() -> Bool {
        if isInitialized { return true }
        
        guard let dbPath = getDatabasePath() else {
            print("LicenseManager: 无法获取数据库路径")
            return false
        }
        
        if sqlite3_open(dbPath, &db) != SQLITE_OK {
            print("LicenseManager: 无法打开数据库")
            return false
        }
        
        if !createTables() {
            print("LicenseManager: 创建表失败")
            return false
        }
        
        // 尝试从数据库恢复授权
        cachedLicense = getLicenseRecord()
        isInitialized = true
        
        print("LicenseManager: 初始化成功")
        return true
    }
    
    deinit {
        if db != nil {
            sqlite3_close(db)
        }
    }
    
    // MARK: - 公共方法
    
    /// 获取设备机器码
    func getMachineId() -> String {
        if let identifierForVendor = UIDevice.current.identifierForVendor {
            return "IOS-\(identifierForVendor.uuidString)"
        }
        return "IOS-UNKNOWN"
    }
    
    /// 验证并激活授权码
    func verifyLicense(_ licenseKey: String, completion: @escaping (LicenseResult) -> Void) {
        DispatchQueue.global(qos: .userInitiated).async { [weak self] in
            guard let self = self else { return }
            
            // 检查格式
            guard licenseKey.hasPrefix("WANGWANG-") else {
                DispatchQueue.main.async {
                    completion(.error("激活码格式无效"))
                }
                return
            }
            
            // 解析激活码
            guard let payload = self.parseLicenseKey(licenseKey) else {
                DispatchQueue.main.async {
                    completion(.error("激活码解析失败"))
                }
                return
            }
            
            // 验证机器码
            let currentMachineId = self.getMachineId()
            guard payload.machineId == currentMachineId else {
                DispatchQueue.main.async {
                    completion(.error("机器码不匹配"))
                }
                return
            }
            
            // 验证过期时间
            let now = Int64(Date().timeIntervalSince1970)
            guard payload.expirationTime > now else {
                DispatchQueue.main.async {
                    completion(.error("授权已过期"))
                }
                return
            }
            
            // 保存到数据库
            let record = LicenseRecord(
                licenseKey: licenseKey,
                machineId: payload.machineId,
                expirationTime: payload.expirationTime,
                licenseType: payload.type,
                activationTime: now
            )
            
            if self.saveLicenseRecord(record) {
                self.cachedLicense = record
                DispatchQueue.main.async {
                    completion(.success(record))
                }
            } else {
                DispatchQueue.main.async {
                    completion(.error("保存授权信息失败"))
                }
            }
        }
    }
    
    /// 检查是否已激活
    func isActivated() -> Bool {
        guard let license = cachedLicense ?? getLicenseRecord() else {
            return false
        }
        let now = Int64(Date().timeIntervalSince1970)
        return license.expirationTime > now
    }
    
    /// 检查授权是否过期
    func isExpired() -> Bool {
        guard let license = cachedLicense else { return true }
        let now = Int64(Date().timeIntervalSince1970)
        return license.expirationTime <= now
    }
    
    /// 获取剩余天数
    func getRemainingDays() -> Int {
        guard let license = cachedLicense else { return 0 }
        let now = Int64(Date().timeIntervalSince1970)
        let remaining = license.expirationTime - now
        return remaining > 0 ? Int(remaining / (24 * 60 * 60)) : 0
    }
    
    /// 获取过期日期字符串
    func getExpirationDateString() -> String {
        guard let license = cachedLicense else { return "未激活" }
        let date = Date(timeIntervalSince1970: TimeInterval(license.expirationTime))
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd"
        return formatter.string(from: date)
    }
    
    /// 获取授权类型
    func getLicenseType() -> String {
        return cachedLicense?.licenseType ?? "free"
    }
    
    /// 清除授权信息
    func clearLicense() -> Bool {
        cachedLicense = nil
        return clearLicenseRecord()
    }
    
    /// 保存布局信息
    func saveAppLayout(appId: String, col: Int, row: Int, spanX: Int = 1, spanY: Int = 1) -> Bool {
        let replaceSQL = """
            INSERT OR REPLACE INTO app_layout (app_id, col, row, span_x, span_y, updated_at)
            VALUES (?, ?, ?, ?, ?, strftime('%s', 'now'));
        """
        
        var statement: OpaquePointer?
        guard sqlite3_prepare_v2(db, replaceSQL, -1, &statement, nil) == SQLITE_OK else {
            return false
        }
        
        sqlite3_bind_text(statement, 1, (appId as NSString).utf8String, -1, nil)
        sqlite3_bind_int(statement, 2, Int32(col))
        sqlite3_bind_int(statement, 3, Int32(row))
        sqlite3_bind_int(statement, 4, Int32(spanX))
        sqlite3_bind_int(statement, 5, Int32(spanY))
        
        let result = sqlite3_step(statement)
        sqlite3_finalize(statement)
        
        return result == SQLITE_DONE
    }
    
    /// 获取所有布局信息
    func getAppLayouts() -> [AppLayoutRecord] {
        let selectSQL = "SELECT app_id, col, row, span_x, span_y FROM app_layout;"
        var layouts: [AppLayoutRecord] = []
        
        var statement: OpaquePointer?
        guard sqlite3_prepare_v2(db, selectSQL, -1, &statement, nil) == SQLITE_OK else {
            return []
        }
        
        defer { sqlite3_finalize(statement) }
        
        while sqlite3_step(statement) == SQLITE_ROW {
            let appId = String(cString: sqlite3_column_text(statement, 0))
            let col = Int(sqlite3_column_int(statement, 1))
            let row = Int(sqlite3_column_int(statement, 2))
            let spanX = Int(sqlite3_column_int(statement, 3))
            let spanY = Int(sqlite3_column_int(statement, 4))
            
            layouts.append(AppLayoutRecord(appId: appId, col: col, row: row, spanX: spanX, spanY: spanY))
        }
        
        return layouts
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
            CREATE TABLE IF NOT EXISTS license (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                license_key TEXT NOT NULL,
                machine_id TEXT NOT NULL,
                expiration_time INTEGER NOT NULL,
                license_type TEXT DEFAULT 'standard',
                activation_time INTEGER NOT NULL,
                created_at INTEGER DEFAULT (strftime('%s', 'now')),
                updated_at INTEGER DEFAULT (strftime('%s', 'now'))
            );
        """
        
        let createLayoutTableSQL = """
            CREATE TABLE IF NOT EXISTS app_layout (
                app_id TEXT PRIMARY KEY,
                col INTEGER NOT NULL,
                row INTEGER NOT NULL,
                span_x INTEGER DEFAULT 1,
                span_y INTEGER DEFAULT 1,
                updated_at INTEGER DEFAULT (strftime('%s', 'now'))
            );
        """
        
        var statement: OpaquePointer?
        if sqlite3_prepare_v2(db, createTableSQL, -1, &statement, nil) == SQLITE_OK {
            if sqlite3_step(statement) == SQLITE_DONE {
                sqlite3_finalize(statement)
                sqlite3_finalize(statement)
            } else {
                sqlite3_finalize(statement)
                return false
            }
        }
        
        if sqlite3_prepare_v2(db, createLayoutTableSQL, -1, &statement, nil) == SQLITE_OK {
            if sqlite3_step(statement) == SQLITE_DONE {
                sqlite3_finalize(statement)
                return true
            }
        }
        sqlite3_finalize(statement)
        return false
    }
    
    private func saveLicenseRecord(_ record: LicenseRecord) -> Bool {
        // 先清除旧记录
        _ = clearLicenseRecord()
        
        let insertSQL = """
            INSERT INTO license (license_key, machine_id, expiration_time, license_type, activation_time)
            VALUES (?, ?, ?, ?, ?);
        """
        
        var statement: OpaquePointer?
        guard sqlite3_prepare_v2(db, insertSQL, -1, &statement, nil) == SQLITE_OK else {
            return false
        }
        
        sqlite3_bind_text(statement, 1, (record.licenseKey as NSString).utf8String, -1, nil)
        sqlite3_bind_text(statement, 2, (record.machineId as NSString).utf8String, -1, nil)
        sqlite3_bind_int64(statement, 3, record.expirationTime)
        sqlite3_bind_text(statement, 4, (record.licenseType as NSString).utf8String, -1, nil)
        sqlite3_bind_int64(statement, 5, record.activationTime)
        
        let result = sqlite3_step(statement)
        sqlite3_finalize(statement)
        
        return result == SQLITE_DONE
    }
    
    private func getLicenseRecord() -> LicenseRecord? {
        let selectSQL = """
            SELECT license_key, machine_id, expiration_time, license_type, activation_time
            FROM license
            ORDER BY id DESC
            LIMIT 1;
        """
        
        var statement: OpaquePointer?
        guard sqlite3_prepare_v2(db, selectSQL, -1, &statement, nil) == SQLITE_OK else {
            return nil
        }
        
        defer { sqlite3_finalize(statement) }
        
        if sqlite3_step(statement) == SQLITE_ROW {
            let licenseKey = String(cString: sqlite3_column_text(statement, 0))
            let machineId = String(cString: sqlite3_column_text(statement, 1))
            let expirationTime = sqlite3_column_int64(statement, 2)
            let licenseType = String(cString: sqlite3_column_text(statement, 3))
            let activationTime = sqlite3_column_int64(statement, 4)
            
            return LicenseRecord(
                licenseKey: licenseKey,
                machineId: machineId,
                expirationTime: expirationTime,
                licenseType: licenseType,
                activationTime: activationTime
            )
        }
        
        return nil
    }
    
    private func clearLicenseRecord() -> Bool {
        let deleteSQL = "DELETE FROM license;"
        var statement: OpaquePointer?
        
        guard sqlite3_prepare_v2(db, deleteSQL, -1, &statement, nil) == SQLITE_OK else {
            return false
        }
        
        let result = sqlite3_step(statement)
        sqlite3_finalize(statement)
        
        return result == SQLITE_DONE
    }
    
    /// 解析激活码 (模拟实现)
    /// TODO: 当 C++ Core 就绪后，通过桥接调用真正的 RSA 验签
    private func parseLicenseKey(_ licenseKey: String) -> LicensePayload? {
        // 格式: WANGWANG-[Payload-Base64].[Signature-Base64]
        let rest = String(licenseKey.dropFirst("WANGWANG-".count))
        let parts = rest.split(separator: ".")
        
        guard parts.count == 2 else { return nil }
        
        // TODO: 验证签名
        // TODO: Base64 解码并解析 JSON
        
        // 模拟解析 - 使用当前时间 + 365天作为过期时间
        let now = Int64(Date().timeIntervalSince1970)
        let expiration = now + (365 * 24 * 60 * 60)
        
        return LicensePayload(
            machineId: getMachineId(),
            expirationTime: expiration,
            type: "pro",
            salt: "generated"
        )
    }
}