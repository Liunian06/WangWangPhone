#include "LicenseManager.h"
#include <chrono>
#include <iostream>

// 注意：实际生产中应使用如 OpenSSL, mbedTLS 或平台原生加密库
// 这里提供逻辑框架，具体 RSA 实现依赖于集成的库

namespace wwj_core {

LicenseManager& LicenseManager::getInstance() {
    static LicenseManager instance;
    return instance;
}

void LicenseManager::setPublicKey(const std::string& key) {
    this->publicKey = key;
}

bool LicenseManager::verifyLicense(const std::string& licenseKey, const std::string& currentMachineId) {
    LicensePayload payload;
    if (!decodeAndVerify(licenseKey, payload)) {
        return false;
    }

    // 校验机器码
    if (payload.machine_id != currentMachineId) {
        return false;
    }

    // 校验过期时间
    auto now = std::chrono::system_clock::now();
    auto now_ts = std::chrono::duration_cast<std::chrono::seconds>(now.time_since_epoch()).count();
    
    if (payload.expiration_time < now_ts) {
        return false;
    }

    this->currentPayload = payload;
    this->activated = true;
    return true;
}

bool LicenseManager::isActivated() const {
    return activated;
}

bool LicenseManager::decodeAndVerify(const std::string& licenseKey, LicensePayload& outPayload) {
    // 逻辑步骤：
    // 1. Base64 解码 licenseKey
    // 2. 分离 Payload 和 Signature
    // 3. 使用 publicKey 验证 Signature
    // 4. 解析 JSON Payload
    
    // 模拟实现：为了演示具体的授权逻辑，我们假设激活码包含特定前缀 "WANGWANG-"
    // 且后面跟着一段模拟的 JSON Payload 数据（这里简单模拟解析）
    if (licenseKey.empty() || licenseKey.find("WANGWANG-") != 0) return false;
    
    // 模拟解析出的 Payload (实际上应该从解密后的字符串中反序列化)
    outPayload.machine_id = "MOCK_DEVICE_ID_12345";
    outPayload.expiration_time = 1893456000; // 2030-01-01
    outPayload.type = "pro";
    outPayload.salt = "random_salt";

    return true;
}

std::string LicenseManager::getMachineId() {
    // 平台相关实现：通常由原生端传入，或者在这里通过系统调用获取
    return "MOCK_DEVICE_ID_12345";
}

} // namespace wwj_core
