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

/// 授权载荷结构
struct LicensePayload {
    let machineId: String
    let expirationTime: Int64
    let type: String
    let salt: String
}

/// 授权操作结果
enum LicenseResult {
    case success(LicenseRecord, needsRestart: Bool)
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
    private var lastCheckTime: Int64 = 0
    
    private let dbName = "wangwang_license.db"
    
    // MARK: - 初始化
    private init() {}
    
    /// 初始化数据库
    /// 每次启动都会从数据库恢复授权并强制验证 RSA 签名
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
        
        isInitialized = true
        
        // 尝试从数据库恢复授权记录
        if let record = getLicenseRecord() {
            // 强制验证：每次启动都重新验证 RSA 签名
            let isValid = validateLicenseOnStartup(record)
            if !isValid {
                // RSA 签名验证失败，清除无效的授权记录
                print("LicenseManager: 启动验证失败：RSA签名无效或授权已过期，清除授权")
                _ = clearLicense()
            }
        } else {
            cachedLicense = nil
        }
        
        print("LicenseManager: 初始化成功")
        return true
    }
    
    /// 启动时验证授权记录的完整性
    /// 包括：RSA 签名验证 + 机器码匹配 + 过期时间检查
    ///
    /// - Parameter record: 数据库中的授权记录
    /// - Returns: true 验证通过，false 验证失败
    private func validateLicenseOnStartup(_ record: LicenseRecord) -> Bool {
        // 1. 验证机器码
        let currentMachineId = getMachineId()
        if record.machineId != currentMachineId {
            print("LicenseManager: 启动验证失败：机器码不匹配")
            return false
        }
        
        // 2. 验证过期时间
        let now = Int64(Date().timeIntervalSince1970)
        if record.expirationTime < now {
            print("LicenseManager: 启动验证失败：授权已过期")
            return false
        }
        
        // 3. 强制验证 RSA 签名（核心安全检查）
        guard let payload = parseLicenseKey(record.licenseKey) else {
            print("LicenseManager: 启动验证失败：RSA签名验证无效")
            return false
        }
        
        // 4. 验证签名中的载荷与数据库记录是否一致
        if payload.machineId != record.machineId || payload.expirationTime != record.expirationTime {
            print("LicenseManager: 启动验证失败：载荷数据与数据库记录不一致")
            return false
        }
        
        // 验证通过，更新缓存
        cachedLicense = record
        print("LicenseManager: 启动验证通过，授权有效，剩余 \(getRemainingDays()) 天")
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
            
            // 清理激活码中的所有空白字符（换行、空格、制表符等）
            let cleanedKey = licenseKey.components(separatedBy: .whitespacesAndNewlines).joined()
            print("LicenseManager: 清理后的激活码长度: \(cleanedKey.count)")
            
            // 检查格式
            guard cleanedKey.hasPrefix("WANGWANG-") else {
                DispatchQueue.main.async {
                    completion(.error("激活码格式无效"))
                }
                return
            }
            
            // 解析激活码
            guard let payload = self.parseLicenseKey(cleanedKey) else {
                DispatchQueue.main.async {
                    completion(.error("激活码解析失败，请检查激活码是否正确"))
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
            
            // 保存到数据库（使用清理后的激活码）
            let record = LicenseRecord(
                licenseKey: cleanedKey,
                machineId: payload.machineId,
                expirationTime: payload.expirationTime,
                licenseType: payload.type,
                activationTime: now
            )
            
            if self.saveLicenseRecord(record) {
                self.cachedLicense = record
                DispatchQueue.main.async {
                    completion(.success(record, needsRestart: true))
                }
            } else {
                DispatchQueue.main.async {
                    completion(.error("保存授权信息失败"))
                }
            }
        }
    }
    
    /// 检查是否已激活
    /// 注意：仅依赖缓存状态，不直接读取数据库
    /// 缓存仅在 initialize() 或 verifyLicense() 中通过 RSA 签名验证后设置
    /// 这确保了更换公钥后旧授权无法通过验证，从而正确失效
    func isActivated() -> Bool {
        guard let license = cachedLicense else {
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
    
    /// 每日检查授权逻辑
    /// 如果过期或验证失败，将重置授权状态
    func checkLicenseDaily() -> Bool {
        guard let license = cachedLicense else { return false }
        let now = Int64(Date().timeIntervalSince1970)
        
        // 每天只检查一次 (86400秒)
        if lastCheckTime > 0 && (now - lastCheckTime) < 86400 {
            return true
        }
        
        // 1. 验证机器码
        let currentMachineId = getMachineId()
        if license.machineId != currentMachineId {
            _ = clearLicense()
            return false
        }
        
        // 2. 验证过期时间
        if license.expirationTime < now {
            _ = clearLicense()
            return false
        }
        
        // 3. 验证签名 (重新解析)
        // 在实际生产中，这里应该调用 C++ 层或原生 RSA 验证
        // 这里仅做简单模拟
        if parseLicenseKey(license.licenseKey) == nil {
            _ = clearLicense()
            return false
        }
        
        lastCheckTime = now
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
        
        var statement: OpaquePointer?
        if sqlite3_prepare_v2(db, createTableSQL, -1, &statement, nil) == SQLITE_OK {
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
    
    /// RSA 公钥 (SPKI 格式 Base64)
    /// 2026-02-10: 更新公钥
    private let publicKeyBase64 = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAxQxw4O380suUJS1ibRjKiX59SVqfUh4ao7/t+lXFaHEPDfL19vgmNaZGFY6pBkLuRZdGqkyiFmNFyWLH6VQf9kmhwL6HO3Qie//9jGIJMMohcPcNVz/cFOfnT1ojYrh+6Q2tODzLDm9EQG669ketzCdC3TynjtbzzyXY+JoL85L1MIhtsqAUFbBd4uAEG16z+OmT4BPi1UdPIKgVt7PdxqLtww2v7t60XwB1MiNo0GIDjhZHH9k1Mbu/IWZcW6pXgCaE+5rxG47gADN384n3zhLot/CbR5aYA0vnheQipjRG8oe4YTApGQ2rFvF+yUYXzcGOJFYkl8CvvPXXw8rFLQIDAQAB"
    
    /// 解析并验证激活码
    /// 使用 RSA 公钥验证签名，确保激活码的真实性
    private func parseLicenseKey(_ licenseKey: String) -> LicensePayload? {
        // 清理激活码中可能包含的空白字符
        let cleanedLicenseKey = licenseKey.components(separatedBy: .whitespacesAndNewlines).joined()
        
        // 格式: WANGWANG-[Payload-Base64].[Signature-Base64]
        let rest = String(cleanedLicenseKey.dropFirst("WANGWANG-".count))
        let parts = rest.split(separator: ".")
        
        guard parts.count == 2 else {
            print("LicenseManager: parseLicenseKey: 格式错误，分割后部分数量=\(parts.count)，期望2")
            return nil
        }
        
        let payloadBase64 = String(parts[0])
        let signatureBase64 = String(parts[1])
        
        print("LicenseManager: parseLicenseKey: payloadBase64长度=\(payloadBase64.count), signatureBase64长度=\(signatureBase64.count)")
        
        // 1. Base64 解码 Payload
        // 修复 Base64 解码兼容性问题，添加 Padding
        var paddedPayloadBase64 = payloadBase64
        while paddedPayloadBase64.count % 4 != 0 {
            paddedPayloadBase64.append("=")
        }
        
        guard let payloadData = Data(base64Encoded: paddedPayloadBase64),
              let payloadJson = String(data: payloadData, encoding: .utf8) else {
            print("LicenseManager: Payload Base64 解码失败")
            return nil
        }
        
        print("LicenseManager: parseLicenseKey: payloadJson=\(payloadJson)")
        
        // 2. 验证 RSA 签名
        guard let publicKeyData = Data(base64Encoded: publicKeyBase64) else {
            print("LicenseManager: 公钥 Base64 解码失败")
            return nil
        }
        
        // 创建公钥
        let keyDict: [CFString: Any] = [
            kSecAttrKeyType: kSecAttrKeyTypeRSA,
            kSecAttrKeyClass: kSecAttrKeyClassPublic,
            kSecAttrKeySizeInBits: 2048
        ]
        
        var error: Unmanaged<CFError>?
        guard let publicKey = SecKeyCreateWithData(publicKeyData as CFData, keyDict as CFDictionary, &error) else {
            print("LicenseManager: 无法创建公钥 - \(error?.takeRetainedValue().localizedDescription ?? "未知错误")")
            return nil
        }
        
        // 验证签名
        var paddedSignatureBase64 = signatureBase64
        while paddedSignatureBase64.count % 4 != 0 {
            paddedSignatureBase64.append("=")
        }
        
        guard let signatureData = Data(base64Encoded: paddedSignatureBase64),
              let payloadBytes = payloadJson.data(using: .utf8) else {
            print("LicenseManager: Signature Base64 解码失败")
            return nil
        }
        
        print("LicenseManager: parseLicenseKey: signatureBytes长度=\(signatureData.count)")
        
        let algorithm: SecKeyAlgorithm = .rsaSignatureMessagePKCS1v15SHA256
        
        guard SecKeyIsAlgorithmSupported(publicKey, .verify, algorithm) else {
            print("LicenseManager: 不支持的签名算法")
            return nil
        }
        
        let isValid = SecKeyVerifySignature(publicKey, algorithm, payloadBytes as CFData, signatureData as CFData, &error)
        
        if !isValid {
            print("LicenseManager: RSA签名验证失败 - \(error?.takeRetainedValue().localizedDescription ?? "签名无效")")
            return nil
        }
        
        print("LicenseManager: parseLicenseKey: RSA签名验证通过")
        
        // 3. 解析 JSON Payload
        guard let jsonData = payloadJson.data(using: .utf8),
              let json = try? JSONSerialization.jsonObject(with: jsonData) as? [String: Any] else {
            print("LicenseManager: JSON 解析失败")
            return nil
        }
        
        guard let machineId = json["mid"] as? String else {
            print("LicenseManager: JSON 中缺少 mid 字段")
            return nil
        }
        
        // 兼容 exp 字段的多种类型（Int, Int64, Double 等）
        let expirationTime: Int64
        if let expInt64 = json["exp"] as? Int64 {
            expirationTime = expInt64
        } else if let expInt = json["exp"] as? Int {
            expirationTime = Int64(expInt)
        } else if let expDouble = json["exp"] as? Double {
            expirationTime = Int64(expDouble)
        } else if let expNSNumber = json["exp"] as? NSNumber {
            expirationTime = expNSNumber.int64Value
        } else {
            print("LicenseManager: JSON 中缺少或无效的 exp 字段, 实际类型: \(type(of: json["exp"]))")
            return nil
        }
        
        let type = json["type"] as? String ?? "standard"
        let salt = json["salt"] as? String ?? ""
        
        return LicensePayload(
            machineId: machineId,
            expirationTime: expirationTime,
            type: type,
            salt: salt
        )
    }
}