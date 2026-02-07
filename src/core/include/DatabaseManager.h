#ifndef DATABASE_MANAGER_H
#define DATABASE_MANAGER_H

#include <string>
#include <vector>
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

// 布局项结构 - 记录每个应用图标在网格中的位置
struct LayoutItem {
    std::string app_id;           // 应用唯一标识（如 "phone", "settings"）
    int position;                 // 在网格中的位置索引（0-based）
    std::string area;             // 区域标识: "grid" = 主网格, "dock" = 底部 Dock 栏
};

// 壁纸记录结构
struct WallpaperRecord {
    std::string wallpaper_type;   // 壁纸类型: "lock" = 锁屏壁纸, "home" = 桌面壁纸
    std::string file_name;        // 持久化存储的文件名
    long long updated_at;         // 更新时间戳
};

// 天气缓存记录结构
struct WeatherCacheRecord {
    std::string city;             // 城市名
    std::string temp;             // 温度
    std::string description;      // 天气描述
    std::string icon;             // 天气图标
    std::string range;            // 温度范围
    std::string request_date;     // 请求日期 (yyyy-MM-dd)
    long long updated_at;         // 更新时间戳
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
    bool saveLayout(const std::vector<LayoutItem>& items);
    std::vector<LayoutItem> getLayout();
    bool clearLayout();

    // 壁纸相关操作
    bool saveWallpaperRecord(const WallpaperRecord& record);
    bool getWallpaperRecord(const std::string& wallpaperType, WallpaperRecord& outRecord);
    bool clearWallpaperRecord(const std::string& wallpaperType);

    // 天气缓存相关操作
    bool saveWeatherCache(const WeatherCacheRecord& record);
    bool getWeatherCache(const std::string& city, const std::string& date, WeatherCacheRecord& outRecord);
    bool clearExpiredWeatherCache(const std::string& todayDate);
    bool clearAllWeatherCache();

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