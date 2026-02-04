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
    LicenseManager() : activated(false) {
        // 注入生成的 RSA 公钥 (SPKI 格式 Base64)
        publicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEApcy/Am7F1r7KDKQqMpTO0jnKToPlj9KNIRcSCYXZ07mPbgxialV4snWzrFMANEC5wlZ0shUUaL+525eFSn3ZzphyrxC75Kyaw0nFeISvY6uNlZxooEu6DS51KU4w5XX0oGlEfSPrx5SsmWvous1Xf6jC3I+RVX+raNCbLH2rrbmk+phkXCTLxzzWUWxQgonN1/PLPQMeLqaaKBcxnZ4rJCw0frWCFr60NTDCSAAt0w7YxNdsaCvTIaIkh62Mdi6qbpG2Tmr7D3viZmE4heO4f5lxi+vXG5KIIKOMAvPXPs14itV8DI5EzJM2C7KUW7xejeUvx3CvVePo7BVBw/4dLwIDAQAB";
    }
    std::string publicKey;
    bool activated;
    LicensePayload currentPayload;

    // 内部解密和解析逻辑
    bool decodeAndVerify(const std::string& licenseKey, LicensePayload& outPayload);
};

} // namespace wwj_core

#endif // LICENSE_MANAGER_H
