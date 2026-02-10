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

    // 每日检查授权逻辑
    // 如果过期或验证失败，将重置授权状态
    // 返回 true 表示授权有效，false 表示已失效
    bool checkLicenseDaily(const std::string& currentMachineId);

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
    LicenseManager() : activated(false), initialized(false), lastCheckTime(0) {
        // RSA 公钥通过 CMake 编译时从 tools/keys/public.pem 注入
        // 更新公钥只需替换 public.pem 文件并重新编译
#ifdef RSA_PUBLIC_KEY_BASE64
        publicKey = RSA_PUBLIC_KEY_BASE64;
#else
        publicKey = "";
#endif
    }
    std::string publicKey;
    bool activated;
    bool initialized;
    long long lastCheckTime; // 上次检查时间戳
    LicensePayload currentPayload;

    // 内部解密和解析逻辑
    bool decodeAndVerify(const std::string& licenseKey, LicensePayload& outPayload);

    // 保存授权信息到数据库
    bool saveLicenseToDatabase(const std::string& licenseKey, const LicensePayload& payload);
};

} // namespace wwj_core

#endif // LICENSE_MANAGER_H
