#include "LicenseManager.h"
#include "DatabaseManager.h"
#include <chrono>
#include <iostream>
#include <sstream>
#include <cstring>

// 注意：实际生产中应使用如 OpenSSL, mbedTLS 或平台原生加密库
// 这里提供逻辑框架，具体 RSA 实现依赖于集成的库

namespace wwj_core {

LicenseManager& LicenseManager::getInstance() {
    static LicenseManager instance;
    return instance;
}

bool LicenseManager::initialize(const std::string& dbPath) {
    if (initialized) {
        return true;
    }

    // 初始化数据库
    if (!DatabaseManager::getInstance().initialize(dbPath)) {
        std::cerr << "LicenseManager: 数据库初始化失败" << std::endl;
        return false;
    }

    initialized = true;
    std::cout << "LicenseManager: 初始化成功" << std::endl;
    return true;
}

void LicenseManager::setPublicKey(const std::string& key) {
    this->publicKey = key;
}

bool LicenseManager::verifyLicense(const std::string& licenseKey, const std::string& currentMachineId) {
    LicensePayload payload;
    if (!decodeAndVerify(licenseKey, payload)) {
        std::cerr << "LicenseManager: 激活码解析失败" << std::endl;
        return false;
    }

    // 校验机器码
    if (payload.machine_id != currentMachineId) {
        std::cerr << "LicenseManager: 机器码不匹配" << std::endl;
        std::cerr << "  期望: " << payload.machine_id << std::endl;
        std::cerr << "  实际: " << currentMachineId << std::endl;
        return false;
    }

    // 校验过期时间
    auto now = std::chrono::system_clock::now();
    auto now_ts = std::chrono::duration_cast<std::chrono::seconds>(now.time_since_epoch()).count();
    
    if (payload.expiration_time < now_ts) {
        std::cerr << "LicenseManager: 授权已过期" << std::endl;
        return false;
    }

    this->currentPayload = payload;
    this->activated = true;

    // 保存到数据库
    if (!saveLicenseToDatabase(licenseKey, payload)) {
        std::cerr << "LicenseManager: 保存授权信息到数据库失败" << std::endl;
        // 即使保存失败，内存中的授权状态仍然有效
    }

    std::cout << "LicenseManager: 激活成功，有效期至 " << payload.expiration_time << std::endl;
    return true;
}

bool LicenseManager::restoreLicenseFromDatabase(const std::string& currentMachineId) {
    if (!initialized) {
        std::cerr << "LicenseManager: 未初始化" << std::endl;
        return false;
    }

    LicenseRecord record;
    if (!DatabaseManager::getInstance().getLicenseRecord(record)) {
        std::cout << "LicenseManager: 数据库中没有授权记录" << std::endl;
        return false;
    }

    // 校验机器码
    if (record.machine_id != currentMachineId) {
        std::cerr << "LicenseManager: 数据库中的机器码与当前设备不匹配" << std::endl;
        return false;
    }

    // 校验过期时间
    auto now = std::chrono::system_clock::now();
    auto now_ts = std::chrono::duration_cast<std::chrono::seconds>(now.time_since_epoch()).count();
    
    if (record.expiration_time < now_ts) {
        std::cerr << "LicenseManager: 数据库中的授权已过期" << std::endl;
        return false;
    }

    // 恢复授权状态
    currentPayload.machine_id = record.machine_id;
    currentPayload.expiration_time = record.expiration_time;
    currentPayload.type = record.license_type;
    currentPayload.salt = "";
    currentPayload.xhsID = record.xhsID;
    currentPayload.qqID = record.qqID;
    activated = true;

    std::cout << "LicenseManager: 从数据库恢复授权成功" << std::endl;
    std::cout << "  授权类型: " << record.license_type << std::endl;
    std::cout << "  剩余天数: " << getRemainingDays() << " 天" << std::endl;
    return true;
}

bool LicenseManager::checkLicenseDaily(const std::string& currentMachineId) {
    if (!initialized) {
        return false;
    }

    if (!activated) {
        return false;
    }

    auto now = std::chrono::system_clock::now();
    long long now_ts = std::chrono::duration_cast<std::chrono::seconds>(now.time_since_epoch()).count();

    // 每天只检查一次
    // 86400 秒 = 1 天
    if (lastCheckTime > 0 && (now_ts - lastCheckTime) < 86400) {
        return true;
    }

    std::cout << "LicenseManager: 执行每日授权检查..." << std::endl;

    // 重新从数据库读取并验证
    LicenseRecord record;
    if (!DatabaseManager::getInstance().getLicenseRecord(record)) {
        std::cerr << "LicenseManager: 每日检查失败 - 数据库无记录" << std::endl;
        clearLicense(); // 视为未授权
        return false;
    }

    // 1. 验证机器码
    if (record.machine_id != currentMachineId) {
        std::cerr << "LicenseManager: 每日检查失败 - 机器码不匹配" << std::endl;
        clearLicense();
        return false;
    }

    // 2. 验证过期时间
    if (record.expiration_time < now_ts) {
        std::cerr << "LicenseManager: 每日检查失败 - 授权已过期" << std::endl;
        clearLicense();
        return false;
    }
    
    // 3. 验证签名完整性 (重新解析 licenseKey)
    LicensePayload payload;
    if (!decodeAndVerify(record.license_key, payload)) {
         std::cerr << "LicenseManager: 每日检查失败 - 密钥验证无效" << std::endl;
         clearLicense();
         return false;
    }

    // 更新最后检查时间
    lastCheckTime = now_ts;
    std::cout << "LicenseManager: 每日检查通过" << std::endl;
    return true;
}

bool LicenseManager::isActivated() const {
    return activated;
}

bool LicenseManager::isExpired() const {
    if (!activated) return true;

    auto now = std::chrono::system_clock::now();
    auto now_ts = std::chrono::duration_cast<std::chrono::seconds>(now.time_since_epoch()).count();
    
    return currentPayload.expiration_time < now_ts;
}

LicensePayload LicenseManager::getLicensePayload() const {
    return currentPayload;
}

int LicenseManager::getRemainingDays() const {
    if (!activated) return 0;

    auto now = std::chrono::system_clock::now();
    auto now_ts = std::chrono::duration_cast<std::chrono::seconds>(now.time_since_epoch()).count();
    
    if (currentPayload.expiration_time <= now_ts) {
        return 0;
    }

    long long remaining_seconds = currentPayload.expiration_time - now_ts;
    return static_cast<int>(remaining_seconds / (24 * 60 * 60));
}

bool LicenseManager::clearLicense() {
    activated = false;
    currentPayload = LicensePayload();

    if (initialized) {
        return DatabaseManager::getInstance().clearLicenseRecord();
    }
    return true;
}

bool LicenseManager::decodeAndVerify(const std::string& licenseKey, LicensePayload& outPayload) {
    // 逻辑步骤：
    // 1. 检查激活码格式 "WANGWANG-[Payload-Base64].[Signature-Base64]"
    // 2. Base64 解码 Payload
    // 3. 使用 publicKey 验证 Signature
    // 4. 解析 JSON Payload
    
    if (licenseKey.empty()) {
        return false;
    }

    // 检查前缀
    const std::string prefix = "WANGWANG-";
    if (licenseKey.find(prefix) != 0) {
        std::cerr << "激活码格式错误：缺少 WANGWANG- 前缀" << std::endl;
        return false;
    }

    // 分离 Payload 和 Signature
    std::string rest = licenseKey.substr(prefix.length());
    size_t dotPos = rest.find('.');
    if (dotPos == std::string::npos) {
        std::cerr << "激活码格式错误：缺少签名分隔符" << std::endl;
        return false;
    }

    std::string payloadBase64 = rest.substr(0, dotPos);
    std::string signatureBase64 = rest.substr(dotPos + 1);

    // TODO: 实际的 Base64 解码和 RSA 签名验证
    // 这里需要集成加密库（如 OpenSSL）来实现
    // 以下是简化的解析逻辑，仅用于演示

    // 简单的 Base64 解码（实际应使用正规库）
    // 这里假设 payload 是 JSON 格式: {"mid":"xxx","exp":123456789,"type":"pro","salt":"xxx"}
    
    // 模拟解析 - 实际生产中需要真正解码和验签
    // 为了演示，我们直接尝试从 base64 解析
    
    // 注意：以下代码是模拟实现
    // 实际使用时需要：
    // 1. 使用 OpenSSL/mbedTLS 进行 Base64 解码
    // 2. 使用公钥验证签名
    // 3. 解析 JSON payload

    // 临时：使用简单的验证逻辑
    if (payloadBase64.empty() || signatureBase64.empty()) {
        return false;
    }

    // 模拟解析出的 Payload（实际应从解密数据中获取）
    // 这里暂时使用机器码作为标识
    outPayload.machine_id = getMachineId();  // 将在实际验证中使用解密的值
    
    // 从当前时间计算 365 天后的过期时间（仅用于演示）
    auto now = std::chrono::system_clock::now();
    auto exp_time = now + std::chrono::hours(24 * 365);
    outPayload.expiration_time = std::chrono::duration_cast<std::chrono::seconds>(
        exp_time.time_since_epoch()).count();
    
    outPayload.type = "pro";
    outPayload.salt = "generated_salt";
    outPayload.xhsID = 0; // 模拟解析出的追踪 ID
    outPayload.qqID = 0;

    // TODO: 当集成真正的加密库后，这里应该：
    // 1. Base64 解码 payloadBase64 获得 JSON 字符串
    // 2. 使用公钥和 signatureBase64 验证签名
    // 3. 从 JSON 解析出 machine_id, expiration_time, type, salt
    // 4. 返回验证结果

    std::cout << "注意: 当前使用模拟验证逻辑，需要集成加密库完成真正的 RSA 验签" << std::endl;
    return true;
}

bool LicenseManager::saveLicenseToDatabase(const std::string& licenseKey, const LicensePayload& payload) {
    if (!initialized) {
        return false;
    }

    LicenseRecord record;
    record.license_key = licenseKey;
    record.machine_id = payload.machine_id;
    record.expiration_time = payload.expiration_time;
    record.license_type = payload.type;
    record.xhsID = payload.xhsID;
    record.qqID = payload.qqID;
    
    auto now = std::chrono::system_clock::now();
    record.activation_time = std::chrono::duration_cast<std::chrono::seconds>(
        now.time_since_epoch()).count();

    return DatabaseManager::getInstance().saveLicenseRecord(record);
}

std::string LicenseManager::getMachineId() {
    // 平台相关实现：通常由原生端传入，或者在这里通过系统调用获取
    // TODO: 实现跨平台的机器码获取
    // - Windows: 使用 WMI 获取硬件 ID
    // - Android: 使用 ANDROID_ID 或设备序列号
    // - iOS: 使用 identifierForVendor
    // - Web: 使用 fingerprint 或存储的 UUID
    return "DEVICE_ID_PLACEHOLDER";
}

} // namespace wwj_core
