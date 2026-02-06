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
    const char* createAppLayoutTableSQL = R"(
        CREATE TABLE IF NOT EXISTS app_layout (
            app_id TEXT PRIMARY KEY,
            col INTEGER NOT NULL,
            row INTEGER NOT NULL,
            span_x INTEGER DEFAULT 1,
            span_y INTEGER DEFAULT 1,
            updated_at INTEGER DEFAULT (strftime('%s', 'now'))
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

    if (!executeSQL(createAppLayoutTableSQL)) {
        std::cerr << "创建 app_layout 表失败" << std::endl;
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

bool DatabaseManager::saveAppLayout(const AppLayout& layout) {
    if (!db) return false;

    const char* replaceSQL = R"(
        INSERT OR REPLACE INTO app_layout (app_id, col, row, span_x, span_y, updated_at)
        VALUES (?, ?, ?, ?, ?, strftime('%s', 'now'));
    )";

    sqlite3_stmt* stmt = nullptr;
    int result = sqlite3_prepare_v2(static_cast<sqlite3*>(db), replaceSQL, -1, &stmt, nullptr);

    if (result != SQLITE_OK) {
        return false;
    }

    sqlite3_bind_text(stmt, 1, layout.app_id.c_str(), -1, SQLITE_TRANSIENT);
    sqlite3_bind_int(stmt, 2, layout.column);
    sqlite3_bind_int(stmt, 3, layout.row);
    sqlite3_bind_int(stmt, 4, layout.span_x);
    sqlite3_bind_int(stmt, 5, layout.span_y);

    result = sqlite3_step(stmt);
    sqlite3_finalize(stmt);

    return result == SQLITE_DONE;
}

bool DatabaseManager::getAppLayouts(std::vector<AppLayout>& outLayouts) {
    if (!db) return false;

    const char* selectSQL = "SELECT app_id, col, row, span_x, span_y FROM app_layout;";

    sqlite3_stmt* stmt = nullptr;
    int result = sqlite3_prepare_v2(static_cast<sqlite3*>(db), selectSQL, -1, &stmt, nullptr);

    if (result != SQLITE_OK) {
        return false;
    }

    outLayouts.clear();
    while (sqlite3_step(stmt) == SQLITE_ROW) {
        AppLayout layout;
        layout.app_id = reinterpret_cast<const char*>(sqlite3_column_text(stmt, 0));
        layout.column = sqlite3_column_int(stmt, 1);
        layout.row = sqlite3_column_int(stmt, 2);
        layout.span_x = sqlite3_column_int(stmt, 3);
        layout.span_y = sqlite3_column_int(stmt, 4);
        outLayouts.push_back(layout);
    }

    sqlite3_finalize(stmt);
    return true;
}

bool DatabaseManager::clearAppLayouts() {
    return executeSQL("DELETE FROM app_layout;");
}

} // namespace wwj_core