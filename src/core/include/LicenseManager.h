#ifndef LICENSE_MANAGER_H
#define LICENSE_MANAGER_H

#include <string>
#include <vector>

namespace wwj_core {

struct LicensePayload {
    std::string machine_id;
    long long expiration_time;
    std::string type;
    std::string salt;
    long long xhsID; // 小红书 ID (溯源)
    long long qqID;  // QQ ID (溯源)
};

class LicenseManager {
public:
    static LicenseManager& getInstance();

    // 初始化（需要传入数据库路径）
    bool initialize(const std::string& dbPath);

    // 初始化公钥
    void setPublicKey(const std::string& publicKey);

    // 验证激活码并持久化
    bool verifyLicense(const std::string& licenseKey, const std::string& currentMachineId);

    // 从数据库恢复授权状态（应用启动时调用）
    bool restoreLicenseFromDatabase(const std::string& currentMachineId);

    // 获取当前授权状态
    bool isActivated() const;

    // 检查授权是否过期
    bool isExpired() const;

    // 获取当前授权载荷
    LicensePayload getLicensePayload() const;

    // 获取剩余天数
    int getRemainingDays() const;

    // 获取机器码 (平台相关实现)
    std::string getMachineId();

    // 清除授权信息
    bool clearLicense();

private:
    LicenseManager() : activated(false), initialized(false) {
        // 注入生成的 RSA 公钥 (SPKI 格式 Base64)
        publicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEApcy/Am7F1r7KDKQqMpTO0jnKToPlj9KNIRcSCYXZ07mPbgxialV4snWzrFMANEC5wlZ0shUUaL+525eFSn3ZzphyrxC75Kyaw0nFeISvY6uNlZxooEu6DS51KU4w5XX0oGlEfSPrx5SsmWvous1Xf6jC3I+RVX+raNCbLH2rrbmk+phkXCTLxzzWUWxQgonN1/PLPQMeLqaaKBcxnZ4rJCw0frWCFr60NTDCSAAt0w7YxNdsaCvTIaIkh62Mdi6qbpG2Tmr7D3viZmE4heO4f5lxi+vXG5KIIKOMAvPXPs14itV8DI5EzJM2C7KUW7xejeUvx3CvVePo7BVBw/4dLwIDAQAB";
    }
    std::string publicKey;
    bool activated;
    bool initialized;
    LicensePayload currentPayload;

    // 内部解密和解析逻辑
    bool decodeAndVerify(const std::string& licenseKey, LicensePayload& outPayload);

    // 保存授权信息到数据库
    bool saveLicenseToDatabase(const std::string& licenseKey, const LicensePayload& payload);
};

} // namespace wwj_core

#endif // LICENSE_MANAGER_H
