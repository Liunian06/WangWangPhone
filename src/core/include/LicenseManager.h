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
};

class LicenseManager {
public:
    static LicenseManager& getInstance();

    // 初始化公钥
    void setPublicKey(const std::string& publicKey);

    // 验证激活码
    bool verifyLicense(const std::string& licenseKey, const std::string& currentMachineId);

    // 获取当前授权状态
    bool isActivated() const;

    // 获取机器码 (平台相关实现)
    std::string getMachineId();

private:
    LicenseManager() : activated(false) {}
    std::string publicKey;
    bool activated;
    LicensePayload currentPayload;

    // 内部解密和解析逻辑
    bool decodeAndVerify(const std::string& licenseKey, LicensePayload& outPayload);
};

} // namespace wwj_core

#endif // LICENSE_MANAGER_H
