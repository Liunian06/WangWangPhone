#include "DatabaseManager.h"
#include <sqlite3.h>
#include <iostream>
#include <chrono>
#include <vector>

namespace wwj_core {

DatabaseManager& DatabaseManager::getInstance() {
    static DatabaseManager instance;
    return instance;
}

DatabaseManager::~DatabaseManager() {
    close();
}

bool DatabaseManager::initialize(const std::string& path) {
    if (initialized) {
        return true;
    }

    dbPath = path;
    sqlite3* sqliteDb = nullptr;
    int result = sqlite3_open(path.c_str(), &sqliteDb);
    
    if (result != SQLITE_OK) {
        std::cerr << "无法打开数据库: " << sqlite3_errmsg(sqliteDb) << std::endl;
        sqlite3_close(sqliteDb);
        return false;
    }

    db = sqliteDb;
    
    if (!createTables()) {
        close();
        return false;
    }

    initialized = true;
    std::cout << "数据库初始化成功: " << path << std::endl;
    return true;
}

void DatabaseManager::close() {
    if (db) {
        sqlite3_close(static_cast<sqlite3*>(db));
        db = nullptr;
    }
    initialized = false;
}

bool DatabaseManager::isInitialized() const {
    return initialized;
}

std::string DatabaseManager::getDatabasePath() const {
    return dbPath;
}

bool DatabaseManager::createTables() {
    // 创建授权信息表
    const char* createLicenseTableSQL = R"(
        CREATE TABLE IF NOT EXISTS license (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            license_key TEXT NOT NULL,
            machine_id TEXT NOT NULL,
            expiration_time INTEGER NOT NULL,
            license_type TEXT DEFAULT 'standard',
            activation_time INTEGER NOT NULL,
            xhs_id INTEGER,
            qq_id INTEGER,
            created_at INTEGER DEFAULT (strftime('%s', 'now')),
            updated_at INTEGER DEFAULT (strftime('%s', 'now'))
        );
    )";

    // 创建索引确保只有一条授权记录
    const char* createIndexSQL = R"(
        CREATE UNIQUE INDEX IF NOT EXISTS idx_license_single ON license(id);
    )";

    // 创建布局信息表
    const char* createLayoutTableSQL = R"(
        CREATE TABLE IF NOT EXISTS app_layout (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            app_id TEXT NOT NULL,
            position INTEGER NOT NULL,
            area TEXT NOT NULL DEFAULT 'grid',
            created_at INTEGER DEFAULT (strftime('%s', 'now')),
            updated_at INTEGER DEFAULT (strftime('%s', 'now'))
        );
    )";

    // 创建布局唯一索引（同一区域下 app_id 唯一）
    const char* createLayoutIndexSQL = R"(
        CREATE UNIQUE INDEX IF NOT EXISTS idx_layout_app ON app_layout(app_id, area);
    )";

    // 创建壁纸信息表
    const char* createWallpaperTableSQL = R"(
        CREATE TABLE IF NOT EXISTS wallpaper (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            wallpaper_type TEXT NOT NULL UNIQUE,
            file_name TEXT NOT NULL,
            created_at INTEGER DEFAULT (strftime('%s', 'now')),
            updated_at INTEGER DEFAULT (strftime('%s', 'now'))
        );
    )";

    // 创建天气缓存表
    const char* createWeatherCacheTableSQL = R"(
        CREATE TABLE IF NOT EXISTS weather_cache (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            city TEXT NOT NULL,
            temp TEXT NOT NULL,
            description TEXT NOT NULL,
            icon TEXT NOT NULL,
            range_info TEXT NOT NULL,
            request_date TEXT NOT NULL,
            updated_at INTEGER DEFAULT (strftime('%s', 'now')),
            UNIQUE(city, request_date)
        );
    )";

    if (!executeSQL(createLicenseTableSQL)) {
        std::cerr << "创建 license 表失败" << std::endl;
        return false;
    }

    if (!executeSQL(createIndexSQL)) {
        std::cerr << "创建索引失败" << std::endl;
        return false;
    }

    if (!executeSQL(createLayoutTableSQL)) {
        std::cerr << "创建 app_layout 表失败" << std::endl;
        return false;
    }

    if (!executeSQL(createLayoutIndexSQL)) {
        std::cerr << "创建布局索引失败" << std::endl;
        return false;
    }

    if (!executeSQL(createWallpaperTableSQL)) {
        std::cerr << "创建 wallpaper 表失败" << std::endl;
        return false;
    }

    if (!executeSQL(createWeatherCacheTableSQL)) {
        std::cerr << "创建 weather_cache 表失败" << std::endl;
        return false;
    }

    return true;
}

bool DatabaseManager::executeSQL(const std::string& sql) {
    if (!db) return false;

    char* errMsg = nullptr;
    int result = sqlite3_exec(static_cast<sqlite3*>(db), sql.c_str(), nullptr, nullptr, &errMsg);

    if (result != SQLITE_OK) {
        std::cerr << "SQL 执行错误: " << errMsg << std::endl;
        sqlite3_free(errMsg);
        return false;
    }

    return true;
}

bool DatabaseManager::saveLicenseRecord(const LicenseRecord& record) {
    if (!db) return false;

    // 先清除旧记录，确保只保留一条授权
    clearLicenseRecord();

    const char* insertSQL = R"(
        INSERT INTO license (license_key, machine_id, expiration_time, license_type, activation_time, xhs_id, qq_id)
        VALUES (?, ?, ?, ?, ?, ?, ?);
    )";

    sqlite3_stmt* stmt = nullptr;
    int result = sqlite3_prepare_v2(static_cast<sqlite3*>(db), insertSQL, -1, &stmt, nullptr);

    if (result != SQLITE_OK) {
        std::cerr << "准备 SQL 语句失败: " << sqlite3_errmsg(static_cast<sqlite3*>(db)) << std::endl;
        return false;
    }

    sqlite3_bind_text(stmt, 1, record.license_key.c_str(), -1, SQLITE_TRANSIENT);
    sqlite3_bind_text(stmt, 2, record.machine_id.c_str(), -1, SQLITE_TRANSIENT);
    sqlite3_bind_int64(stmt, 3, record.expiration_time);
    sqlite3_bind_text(stmt, 4, record.license_type.c_str(), -1, SQLITE_TRANSIENT);
    sqlite3_bind_int64(stmt, 5, record.activation_time);
    sqlite3_bind_int64(stmt, 6, record.xhsID);
    sqlite3_bind_int64(stmt, 7, record.qqID);

    result = sqlite3_step(stmt);
    sqlite3_finalize(stmt);

    if (result != SQLITE_DONE) {
        std::cerr << "保存授权记录失败: " << sqlite3_errmsg(static_cast<sqlite3*>(db)) << std::endl;
        return false;
    }

    std::cout << "授权记录已保存到数据库" << std::endl;
    return true;
}

bool DatabaseManager::getLicenseRecord(LicenseRecord& outRecord) {
    if (!db) return false;

    const char* selectSQL = R"(
        SELECT license_key, machine_id, expiration_time, license_type, activation_time, xhs_id, qq_id
        FROM license
        ORDER BY id DESC
        LIMIT 1;
    )";

    sqlite3_stmt* stmt = nullptr;
    int result = sqlite3_prepare_v2(static_cast<sqlite3*>(db), selectSQL, -1, &stmt, nullptr);

    if (result != SQLITE_OK) {
        std::cerr << "准备查询语句失败: " << sqlite3_errmsg(static_cast<sqlite3*>(db)) << std::endl;
        return false;
    }

    result = sqlite3_step(stmt);

    if (result == SQLITE_ROW) {
        outRecord.license_key = reinterpret_cast<const char*>(sqlite3_column_text(stmt, 0));
        outRecord.machine_id = reinterpret_cast<const char*>(sqlite3_column_text(stmt, 1));
        outRecord.expiration_time = sqlite3_column_int64(stmt, 2);
        outRecord.license_type = reinterpret_cast<const char*>(sqlite3_column_text(stmt, 3));
        outRecord.activation_time = sqlite3_column_int64(stmt, 4);
        outRecord.xhsID = sqlite3_column_int64(stmt, 5);
        outRecord.qqID = sqlite3_column_int64(stmt, 6);
        
        sqlite3_finalize(stmt);
        return true;
    }

    sqlite3_finalize(stmt);
    return false;
}

bool DatabaseManager::clearLicenseRecord() {
    return executeSQL("DELETE FROM license;");
}

bool DatabaseManager::saveLayout(const std::vector<LayoutItem>& items) {
    if (!db) return false;

    sqlite3* sqliteDb = static_cast<sqlite3*>(db);

    // 开启事务
    if (!executeSQL("BEGIN TRANSACTION;")) return false;

    // 先清除旧布局
    if (!executeSQL("DELETE FROM app_layout;")) {
        executeSQL("ROLLBACK;");
        return false;
    }

    const char* insertSQL = R"(
        INSERT INTO app_layout (app_id, position, area)
        VALUES (?, ?, ?);
    )";

    sqlite3_stmt* stmt = nullptr;
    int result = sqlite3_prepare_v2(sqliteDb, insertSQL, -1, &stmt, nullptr);

    if (result != SQLITE_OK) {
        std::cerr << "准备布局 SQL 语句失败: " << sqlite3_errmsg(sqliteDb) << std::endl;
        executeSQL("ROLLBACK;");
        return false;
    }

    for (const auto& item : items) {
        sqlite3_reset(stmt);
        sqlite3_bind_text(stmt, 1, item.app_id.c_str(), -1, SQLITE_TRANSIENT);
        sqlite3_bind_int(stmt, 2, item.position);
        sqlite3_bind_text(stmt, 3, item.area.c_str(), -1, SQLITE_TRANSIENT);

        result = sqlite3_step(stmt);
        if (result != SQLITE_DONE) {
            std::cerr << "保存布局项失败: " << sqlite3_errmsg(sqliteDb) << std::endl;
            sqlite3_finalize(stmt);
            executeSQL("ROLLBACK;");
            return false;
        }
    }

    sqlite3_finalize(stmt);

    if (!executeSQL("COMMIT;")) {
        executeSQL("ROLLBACK;");
        return false;
    }

    std::cout << "布局已保存到数据库 (" << items.size() << " 项)" << std::endl;
    return true;
}

std::vector<LayoutItem> DatabaseManager::getLayout() {
    std::vector<LayoutItem> items;
    if (!db) return items;

    const char* selectSQL = R"(
        SELECT app_id, position, area
        FROM app_layout
        ORDER BY area ASC, position ASC;
    )";

    sqlite3_stmt* stmt = nullptr;
    int result = sqlite3_prepare_v2(static_cast<sqlite3*>(db), selectSQL, -1, &stmt, nullptr);

    if (result != SQLITE_OK) {
        std::cerr << "准备布局查询语句失败: " << sqlite3_errmsg(static_cast<sqlite3*>(db)) << std::endl;
        return items;
    }

    while (sqlite3_step(stmt) == SQLITE_ROW) {
        LayoutItem item;
        item.app_id = reinterpret_cast<const char*>(sqlite3_column_text(stmt, 0));
        item.position = sqlite3_column_int(stmt, 1);
        item.area = reinterpret_cast<const char*>(sqlite3_column_text(stmt, 2));
        items.push_back(item);
    }

    sqlite3_finalize(stmt);
    return items;
}

bool DatabaseManager::clearLayout() {
    return executeSQL("DELETE FROM app_layout;");
}

bool DatabaseManager::saveWallpaperRecord(const WallpaperRecord& record) {
    if (!db) return false;

    // 使用 INSERT OR REPLACE 实现 upsert（按 wallpaper_type 唯一）
    const char* upsertSQL = R"(
        INSERT OR REPLACE INTO wallpaper (wallpaper_type, file_name, updated_at)
        VALUES (?, ?, strftime('%s', 'now'));
    )";

    sqlite3_stmt* stmt = nullptr;
    int result = sqlite3_prepare_v2(
        static_cast<sqlite3*>(db), upsertSQL, -1, &stmt, nullptr);

    if (result != SQLITE_OK) {
        std::cerr << "准备壁纸 SQL 语句失败: "
                  << sqlite3_errmsg(static_cast<sqlite3*>(db)) << std::endl;
        return false;
    }

    sqlite3_bind_text(stmt, 1,
        record.wallpaper_type.c_str(), -1, SQLITE_TRANSIENT);
    sqlite3_bind_text(stmt, 2,
        record.file_name.c_str(), -1, SQLITE_TRANSIENT);

    result = sqlite3_step(stmt);
    sqlite3_finalize(stmt);

    if (result != SQLITE_DONE) {
        std::cerr << "保存壁纸记录失败: "
                  << sqlite3_errmsg(static_cast<sqlite3*>(db)) << std::endl;
        return false;
    }

    std::cout << "壁纸记录已保存: type=" << record.wallpaper_type
              << ", file=" << record.file_name << std::endl;
    return true;
}

bool DatabaseManager::getWallpaperRecord(
    const std::string& wallpaperType, WallpaperRecord& outRecord) {
    if (!db) return false;

    const char* selectSQL = R"(
        SELECT wallpaper_type, file_name, updated_at
        FROM wallpaper
        WHERE wallpaper_type = ?
        LIMIT 1;
    )";

    sqlite3_stmt* stmt = nullptr;
    int result = sqlite3_prepare_v2(
        static_cast<sqlite3*>(db), selectSQL, -1, &stmt, nullptr);

    if (result != SQLITE_OK) {
        std::cerr << "准备壁纸查询语句失败: "
                  << sqlite3_errmsg(static_cast<sqlite3*>(db)) << std::endl;
        return false;
    }

    sqlite3_bind_text(stmt, 1,
        wallpaperType.c_str(), -1, SQLITE_TRANSIENT);

    result = sqlite3_step(stmt);

    if (result == SQLITE_ROW) {
        outRecord.wallpaper_type = reinterpret_cast<const char*>(
            sqlite3_column_text(stmt, 0));
        outRecord.file_name = reinterpret_cast<const char*>(
            sqlite3_column_text(stmt, 1));
        outRecord.updated_at = sqlite3_column_int64(stmt, 2);

        sqlite3_finalize(stmt);
        return true;
    }

    sqlite3_finalize(stmt);
    return false;
}

bool DatabaseManager::clearWallpaperRecord(
    const std::string& wallpaperType) {
    if (!db) return false;

    const char* deleteSQL = R"(
        DELETE FROM wallpaper WHERE wallpaper_type = ?;
    )";

    sqlite3_stmt* stmt = nullptr;
    int result = sqlite3_prepare_v2(
        static_cast<sqlite3*>(db), deleteSQL, -1, &stmt, nullptr);

    if (result != SQLITE_OK) return false;

    sqlite3_bind_text(stmt, 1,
        wallpaperType.c_str(), -1, SQLITE_TRANSIENT);

    result = sqlite3_step(stmt);
    sqlite3_finalize(stmt);

    return result == SQLITE_DONE;
}

// ==================== 天气缓存操作 ====================

bool DatabaseManager::saveWeatherCache(const WeatherCacheRecord& record) {
    if (!db) return false;

    const char* upsertSQL = R"(
        INSERT OR REPLACE INTO weather_cache (city, temp, description, icon, range_info, request_date, updated_at)
        VALUES (?, ?, ?, ?, ?, ?, strftime('%s', 'now'));
    )";

    sqlite3_stmt* stmt = nullptr;
    int result = sqlite3_prepare_v2(
        static_cast<sqlite3*>(db), upsertSQL, -1, &stmt, nullptr);

    if (result != SQLITE_OK) {
        std::cerr << "准备天气缓存 SQL 语句失败: "
                  << sqlite3_errmsg(static_cast<sqlite3*>(db)) << std::endl;
        return false;
    }

    sqlite3_bind_text(stmt, 1, record.city.c_str(), -1, SQLITE_TRANSIENT);
    sqlite3_bind_text(stmt, 2, record.temp.c_str(), -1, SQLITE_TRANSIENT);
    sqlite3_bind_text(stmt, 3, record.description.c_str(), -1, SQLITE_TRANSIENT);
    sqlite3_bind_text(stmt, 4, record.icon.c_str(), -1, SQLITE_TRANSIENT);
    sqlite3_bind_text(stmt, 5, record.range.c_str(), -1, SQLITE_TRANSIENT);
    sqlite3_bind_text(stmt, 6, record.request_date.c_str(), -1, SQLITE_TRANSIENT);

    result = sqlite3_step(stmt);
    sqlite3_finalize(stmt);

    if (result != SQLITE_DONE) {
        std::cerr << "保存天气缓存失败: "
                  << sqlite3_errmsg(static_cast<sqlite3*>(db)) << std::endl;
        return false;
    }

    std::cout << "天气缓存已保存: city=" << record.city
              << ", date=" << record.request_date << std::endl;
    return true;
}

bool DatabaseManager::getWeatherCache(
    const std::string& city, const std::string& date, WeatherCacheRecord& outRecord) {
    if (!db) return false;

    const char* selectSQL = R"(
        SELECT city, temp, description, icon, range_info, request_date, updated_at
        FROM weather_cache
        WHERE city = ? AND request_date = ?
        LIMIT 1;
    )";

    sqlite3_stmt* stmt = nullptr;
    int result = sqlite3_prepare_v2(
        static_cast<sqlite3*>(db), selectSQL, -1, &stmt, nullptr);

    if (result != SQLITE_OK) {
        std::cerr << "准备天气缓存查询语句失败: "
                  << sqlite3_errmsg(static_cast<sqlite3*>(db)) << std::endl;
        return false;
    }

    sqlite3_bind_text(stmt, 1, city.c_str(), -1, SQLITE_TRANSIENT);
    sqlite3_bind_text(stmt, 2, date.c_str(), -1, SQLITE_TRANSIENT);

    result = sqlite3_step(stmt);

    if (result == SQLITE_ROW) {
        outRecord.city = reinterpret_cast<const char*>(sqlite3_column_text(stmt, 0));
        outRecord.temp = reinterpret_cast<const char*>(sqlite3_column_text(stmt, 1));
        outRecord.description = reinterpret_cast<const char*>(sqlite3_column_text(stmt, 2));
        outRecord.icon = reinterpret_cast<const char*>(sqlite3_column_text(stmt, 3));
        outRecord.range = reinterpret_cast<const char*>(sqlite3_column_text(stmt, 4));
        outRecord.request_date = reinterpret_cast<const char*>(sqlite3_column_text(stmt, 5));
        outRecord.updated_at = sqlite3_column_int64(stmt, 6);

        sqlite3_finalize(stmt);
        return true;
    }

    sqlite3_finalize(stmt);
    return false;
}

bool DatabaseManager::clearExpiredWeatherCache(const std::string& todayDate) {
    if (!db) return false;

    const char* deleteSQL = R"(
        DELETE FROM weather_cache WHERE request_date != ?;
    )";

    sqlite3_stmt* stmt = nullptr;
    int result = sqlite3_prepare_v2(
        static_cast<sqlite3*>(db), deleteSQL, -1, &stmt, nullptr);

    if (result != SQLITE_OK) return false;

    sqlite3_bind_text(stmt, 1, todayDate.c_str(), -1, SQLITE_TRANSIENT);

    result = sqlite3_step(stmt);
    sqlite3_finalize(stmt);

    return result == SQLITE_DONE;
}

bool DatabaseManager::clearAllWeatherCache() {
    return executeSQL("DELETE FROM weather_cache;");
}

} // namespace wwj_core