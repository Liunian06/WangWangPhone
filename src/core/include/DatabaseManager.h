#ifndef DATABASE_MANAGER_H
#define DATABASE_MANAGER_H

#include <string>
#include <functional>

namespace wwj_core {

// 授权记录结构
struct LicenseRecord {
    std::string license_key;      // 原始激活码
    std::string machine_id;       // 机器码
    long long expiration_time;    // 过期时间戳
    std::string license_type;     // 授权类型
    long long activation_time;    // 激活时间戳
    long long xhsID;              // 小红书 ID
    long long qqID;               // QQ ID
};

// 软件/组件布局信息
struct AppLayout {
    std::string app_id;    // 软件唯一标识
    int column;            // 所在列
    int row;               // 所在行
    int span_x;            // 跨列数
    int span_y;            // 跨行数
};

class DatabaseManager {
public:
    static DatabaseManager& getInstance();

    // 初始化数据库（需要传入数据库文件路径）
    bool initialize(const std::string& dbPath);

    // 关闭数据库
    void close();

    // 检查数据库是否已初始化
    bool isInitialized() const;

    // 授权相关操作
    bool saveLicenseRecord(const LicenseRecord& record);
    bool getLicenseRecord(LicenseRecord& outRecord);
    bool clearLicenseRecord();

    // 布局相关操作
    bool saveAppLayout(const AppLayout& layout);
    bool getAppLayouts(std::vector<AppLayout>& outLayouts);
    bool clearAppLayouts();

    // 获取数据库路径
    std::string getDatabasePath() const;

private:
    DatabaseManager() : initialized(false), db(nullptr) {}
    ~DatabaseManager();

    // 禁止拷贝
    DatabaseManager(const DatabaseManager&) = delete;
    DatabaseManager& operator=(const DatabaseManager&) = delete;

    // 创建表结构
    bool createTables();

    // 执行 SQL
    bool executeSQL(const std::string& sql);

    bool initialized;
    void* db;  // sqlite3* 类型，使用 void* 避免头文件依赖
    std::string dbPath;
};

} // namespace wwj_core

#endif // DATABASE_MANAGER_H