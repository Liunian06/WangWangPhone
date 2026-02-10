#include "LicenseManager.h"
#include "DatabaseManager.h"
#include <chrono>
#include <iostream>
#include <sstream>
#include <cstring>
#include <vector>
#include <algorithm>

// OpenSSL 头文件用于 RSA 签名验证
#include <openssl/rsa.h>
#include <openssl/pem.h>
#include <openssl/bio.h>
#include <openssl/evp.h>
#include <openssl/err.h>

namespace wwj_core {

// ============================================================================
// Base64 解码实现
// ============================================================================
static const std::string base64_chars =
    "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    "abcdefghijklmnopqrstuvwxyz"
    "0123456789+/";

static inline bool is_base64(unsigned char c) {
    return (isalnum(c) || (c == '+') || (c == '/'));
}

static std::vector<unsigned char> base64_decode(const std::string& encoded_string) {
    size_t in_len = encoded_string.size();
    int i = 0;
    int j = 0;
    int in_ = 0;
    unsigned char char_array_4[4], char_array_3[3];
    std::vector<unsigned char> ret;

    while (in_len-- && (encoded_string[in_] != '=') && is_base64(encoded_string[in_])) {
        char_array_4[i++] = encoded_string[in_]; in_++;
        if (i == 4) {
            for (i = 0; i < 4; i++)
                char_array_4[i] = static_cast<unsigned char>(base64_chars.find(char_array_4[i]));

            char_array_3[0] = (char_array_4[0] << 2) + ((char_array_4[1] & 0x30) >> 4);
            char_array_3[1] = ((char_array_4[1] & 0xf) << 4) + ((char_array_4[2] & 0x3c) >> 2);
            char_array_3[2] = ((char_array_4[2] & 0x3) << 6) + char_array_4[3];

            for (i = 0; i < 3; i++)
                ret.push_back(char_array_3[i]);
            i = 0;
        }
    }

    if (i) {
        for (j = i; j < 4; j++)
            char_array_4[j] = 0;

        for (j = 0; j < 4; j++)
            char_array_4[j] = static_cast<unsigned char>(base64_chars.find(char_array_4[j]));

        char_array_3[0] = (char_array_4[0] << 2) + ((char_array_4[1] & 0x30) >> 4);
        char_array_3[1] = ((char_array_4[1] & 0xf) << 4) + ((char_array_4[2] & 0x3c) >> 2);
        char_array_3[2] = ((char_array_4[2] & 0x3) << 6) + char_array_4[3];

        for (j = 0; j < i - 1; j++)
            ret.push_back(char_array_3[j]);
    }

    return ret;
}

// ============================================================================
// 简单 JSON 解析器（用于解析 License Payload）
// ============================================================================
static std::string extractJsonString(const std::string& json, const std::string& key) {
    std::string searchKey = "\"" + key + "\"";
    size_t keyPos = json.find(searchKey);
    if (keyPos == std::string::npos) return "";
    
    size_t colonPos = json.find(':', keyPos);
    if (colonPos == std::string::npos) return "";
    
    size_t startQuote = json.find('"', colonPos);
    if (startQuote == std::string::npos) return "";
    
    size_t endQuote = json.find('"', startQuote + 1);
    if (endQuote == std::string::npos) return "";
    
    return json.substr(startQuote + 1, endQuote - startQuote - 1);
}

static long long extractJsonLong(const std::string& json, const std::string& key) {
    std::string searchKey = "\"" + key + "\"";
    size_t keyPos = json.find(searchKey);
    if (keyPos == std::string::npos) return 0;
    
    size_t colonPos = json.find(':', keyPos);
    if (colonPos == std::string::npos) return 0;
    
    // 跳过空白字符
    size_t numStart = colonPos + 1;
    while (numStart < json.size() && (json[numStart] == ' ' || json[numStart] == '\t')) {
        numStart++;
    }
    
    // 找到数字结束位置
    size_t numEnd = numStart;
    while (numEnd < json.size() && (isdigit(json[numEnd]) || json[numEnd] == '-')) {
        numEnd++;
    }
    
    if (numEnd == numStart) return 0;
    
    try {
        return std::stoll(json.substr(numStart, numEnd - numStart));
    } catch (...) {
        return 0;
    }
}

// ============================================================================
// RSA 签名验证
// ============================================================================
static bool verifyRSASignature(const std::string& publicKeyBase64,
                                const std::string& message,
                                const std::vector<unsigned char>& signature) {
    // 1. Base64 解码公钥
    std::vector<unsigned char> publicKeyDer = base64_decode(publicKeyBase64);
    if (publicKeyDer.empty()) {
        std::cerr << "RSA验证: 公钥Base64解码失败" << std::endl;
        return false;
    }
    
    // 2. 从 DER 格式创建公钥
    const unsigned char* keyData = publicKeyDer.data();
    EVP_PKEY* pkey = d2i_PUBKEY(nullptr, &keyData, static_cast<long>(publicKeyDer.size()));
    if (!pkey) {
        std::cerr << "RSA验证: 无法解析公钥 - " << ERR_error_string(ERR_get_error(), nullptr) << std::endl;
        return false;
    }
    
    // 3. 创建验证上下文
    EVP_MD_CTX* mdctx = EVP_MD_CTX_new();
    if (!mdctx) {
        EVP_PKEY_free(pkey);
        std::cerr << "RSA验证: 无法创建MD上下文" << std::endl;
        return false;
    }
    
    bool result = false;
    
    // 4. 初始化验证操作 (SHA256withRSA)
    if (EVP_DigestVerifyInit(mdctx, nullptr, EVP_sha256(), nullptr, pkey) != 1) {
        std::cerr << "RSA验证: 初始化失败" << std::endl;
        goto cleanup;
    }
    
    // 5. 更新消息数据
    if (EVP_DigestVerifyUpdate(mdctx, message.c_str(), message.size()) != 1) {
        std::cerr << "RSA验证: 更新数据失败" << std::endl;
        goto cleanup;
    }
    
    // 6. 验证签名
    if (EVP_DigestVerifyFinal(mdctx, signature.data(), signature.size()) == 1) {
        result = true;
    } else {
        std::cerr << "RSA验证: 签名验证失败 - " << ERR_error_string(ERR_get_error(), nullptr) << std::endl;
    }
    
cleanup:
    EVP_MD_CTX_free(mdctx);
    EVP_PKEY_free(pkey);
    return result;
}

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

    // 【关键安全检查】强制重新验证 RSA 签名
    // 当公钥更新后，旧私钥签发的 license 将无法通过新公钥验证
    // 从而确保更换公私钥对后，所有旧授权自动失效
    LicensePayload verifiedPayload;
    if (!decodeAndVerify(record.license_key, verifiedPayload)) {
        std::cerr << "LicenseManager: 数据库中的授权密钥 RSA 签名验证失败（公钥可能已更新）" << std::endl;
        clearLicense();
        return false;
    }

    // 【新增】安全阈值检查：废除所有旧版本的授权
    // 2026-02-10 19:30:00 (UTC+8) 之前的所有授权均视为无效（对应公钥更新时间）
    // 阈值：1770723000 (2026-02-10 19:30:00 UTC+8)
    if (record.activation_time < 1770723000) {
        std::cerr << "LicenseManager: 授权记录早于安全阈值，强制失效" << std::endl;
        clearLicense();
        return false;
    }

    // 验证签名中的载荷与数据库记录是否一致（防篡改）
    if (verifiedPayload.machine_id != record.machine_id ||
        verifiedPayload.expiration_time != record.expiration_time) {
        std::cerr << "LicenseManager: 载荷数据与数据库记录不一致，授权无效" << std::endl;
        clearLicense();
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

    std::cout << "LicenseManager: 从数据库恢复授权成功（RSA签名验证通过）" << std::endl;
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
    // 3. 使用 publicKey 验证 Signature (RSA SHA256)
    // 4. 解析 JSON Payload
    
    if (licenseKey.empty()) {
        std::cerr << "激活码为空" << std::endl;
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

    if (payloadBase64.empty() || signatureBase64.empty()) {
        std::cerr << "激活码格式错误：Payload或签名为空" << std::endl;
        return false;
    }

    // 1. Base64 解码 Payload 获得 JSON 字符串
    // 确保 Payload 长度是 4 的倍数
    std::string paddedPayloadBase64 = payloadBase64;
    while (paddedPayloadBase64.length() % 4 != 0) {
        paddedPayloadBase64 += '=';
    }
    std::vector<unsigned char> payloadBytes = base64_decode(paddedPayloadBase64);
    if (payloadBytes.empty()) {
        std::cerr << "激活码解析失败：Payload Base64解码失败" << std::endl;
        return false;
    }
    std::string payloadJson(payloadBytes.begin(), payloadBytes.end());
    
    // 2. Base64 解码签名
    // 确保签名长度是 4 的倍数
    std::string paddedSignatureBase64 = signatureBase64;
    while (paddedSignatureBase64.length() % 4 != 0) {
        paddedSignatureBase64 += '=';
    }
    std::vector<unsigned char> signatureBytes = base64_decode(paddedSignatureBase64);
    if (signatureBytes.empty()) {
        std::cerr << "激活码解析失败：签名 Base64解码失败" << std::endl;
        return false;
    }
    
    // 3. 使用 RSA 公钥验证签名 (SHA256withRSA)
    if (!verifyRSASignature(publicKey, payloadJson, signatureBytes)) {
        std::cerr << "激活码验证失败：RSA签名无效" << std::endl;
        return false;
    }
    
    std::cout << "LicenseManager: RSA签名验证通过" << std::endl;
    
    // 4. 解析 JSON Payload
    // 格式: {"mid":"xxx","exp":123456789,"type":"pro","salt":"xxx","xhsID":0,"qqID":0}
    outPayload.machine_id = extractJsonString(payloadJson, "mid");
    outPayload.expiration_time = extractJsonLong(payloadJson, "exp");
    outPayload.type = extractJsonString(payloadJson, "type");
    outPayload.salt = extractJsonString(payloadJson, "salt");
    outPayload.xhsID = extractJsonLong(payloadJson, "xhsID");
    outPayload.qqID = extractJsonLong(payloadJson, "qqID");
    
    // 验证必要字段
    if (outPayload.machine_id.empty()) {
        std::cerr << "激活码解析失败：缺少机器码字段(mid)" << std::endl;
        return false;
    }
    
    if (outPayload.expiration_time <= 0) {
        std::cerr << "激活码解析失败：缺少或无效的过期时间字段(exp)" << std::endl;
        return false;
    }
    
    // 设置默认值
    if (outPayload.type.empty()) {
        outPayload.type = "standard";
    }
    
    std::cout << "LicenseManager: 激活码解析成功" << std::endl;
    std::cout << "  机器码: " << outPayload.machine_id << std::endl;
    std::cout << "  过期时间: " << outPayload.expiration_time << std::endl;
    std::cout << "  授权类型: " << outPayload.type << std::endl;
    
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
