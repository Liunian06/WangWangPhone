#include "ChatManager.h"
#include "DatabaseManager.h"
#include <sqlite3.h>
#include <iostream>
#include <chrono>
#include <random>
#include <iomanip>
#include <sstream>

namespace wwj_core {

// 获取 sqlite3* 指针
static sqlite3* getSqliteDb() {
    // DatabaseManager 的成员 db 是 void*，在 DatabaseManager.cpp 里它是 sqlite3*
    // 这里我们需要获取它。由于它是 private，我们需要在 DatabaseManager 里加一个 getter 或者声明为 friend。
    // 检查 DatabaseManager.h 发现没有 db 的 getter，但 initialize 里把它存下来了。
    // 为了简单起见，我刚才在 DatabaseManager.h 里看到 db 是私有的。
    // 让我先修改 DatabaseManager.h 暴露一个获取 sqlite3* 的方法，或者让 ChatManager 成为 friend。
    return static_cast<sqlite3*>(DatabaseManager::getInstance().getDbPtr());
}

ChatManager& ChatManager::getInstance() {
    static ChatManager instance;
    return instance;
}

bool ChatManager::initialize() {
    if (!DatabaseManager::getInstance().isInitialized()) {
        return false;
    }
    initialized = true;
    return true;
}

bool ChatManager::isInitialized() const {
    return initialized;
}

// ---------- ChatSession CRUD ----------

bool ChatManager::saveSession(const ChatSession& session) {
    sqlite3* db = getSqliteDb();
    if (!db) return false;

    const char* sql = R"(
        INSERT OR REPLACE INTO chat_sessions 
        (id, role_id, me_id, last_updated, enable_extended_chat, enable_text_to_image, 
         enable_emoji, enable_independent_send_button, current_state, is_pinned, 
         api_preset_id, image_api_preset_id, world_info_ids, text_preset_ids, background_image)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
    )";

    sqlite3_stmt* stmt;
    if (sqlite3_prepare_v2(db, sql, -1, &stmt, nullptr) != SQLITE_OK) return false;

    sqlite3_bind_text(stmt, 1, session.id.c_str(), -1, SQLITE_TRANSIENT);
    sqlite3_bind_text(stmt, 2, session.role_id.c_str(), -1, SQLITE_TRANSIENT);
    sqlite3_bind_text(stmt, 3, session.me_id.c_str(), -1, SQLITE_TRANSIENT);
    sqlite3_bind_int64(stmt, 4, session.last_updated);
    sqlite3_bind_int(stmt, 5, session.enable_extended_chat ? 1 : 0);
    sqlite3_bind_int(stmt, 6, session.enable_text_to_image ? 1 : 0);
    sqlite3_bind_int(stmt, 7, session.enable_emoji ? 1 : 0);
    sqlite3_bind_int(stmt, 8, session.enable_independent_send_button ? 1 : 0);
    sqlite3_bind_text(stmt, 9, session.current_state.c_str(), -1, SQLITE_TRANSIENT);
    sqlite3_bind_int(stmt, 10, session.is_pinned ? 1 : 0);
    sqlite3_bind_text(stmt, 11, session.api_preset_id.c_str(), -1, SQLITE_TRANSIENT);
    sqlite3_bind_text(stmt, 12, session.image_api_preset_id.c_str(), -1, SQLITE_TRANSIENT);
    sqlite3_bind_text(stmt, 13, session.world_info_ids.c_str(), -1, SQLITE_TRANSIENT);
    sqlite3_bind_text(stmt, 14, session.text_preset_ids.c_str(), -1, SQLITE_TRANSIENT);
    sqlite3_bind_text(stmt, 15, session.background_image.c_str(), -1, SQLITE_TRANSIENT);

    bool success = sqlite3_step(stmt) == SQLITE_DONE;
    sqlite3_finalize(stmt);
    return success;
}

bool ChatManager::getSession(const std::string& sessionId, ChatSession& out) {
    sqlite3* db = getSqliteDb();
    if (!db) return false;

    const char* sql = "SELECT * FROM chat_sessions WHERE id = ?;";
    sqlite3_stmt* stmt;
    if (sqlite3_prepare_v2(db, sql, -1, &stmt, nullptr) != SQLITE_OK) return false;

    sqlite3_bind_text(stmt, 1, sessionId.c_str(), -1, SQLITE_TRANSIENT);

    if (sqlite3_step(stmt) == SQLITE_ROW) {
        out.id = (const char*)sqlite3_column_text(stmt, 0);
        out.role_id = (const char*)sqlite3_column_text(stmt, 1);
        out.me_id = (const char*)sqlite3_column_text(stmt, 2);
        out.last_updated = sqlite3_column_int64(stmt, 3);
        out.enable_extended_chat = sqlite3_column_int(stmt, 4) != 0;
        out.enable_text_to_image = sqlite3_column_int(stmt, 5) != 0;
        out.enable_emoji = sqlite3_column_int(stmt, 6) != 0;
        out.enable_independent_send_button = sqlite3_column_int(stmt, 7) != 0;
        const char* state = (const char*)sqlite3_column_text(stmt, 8);
        out.current_state = state ? state : "";
        out.is_pinned = sqlite3_column_int(stmt, 9) != 0;
        const char* api = (const char*)sqlite3_column_text(stmt, 10);
        out.api_preset_id = api ? api : "";
        const char* img_api = (const char*)sqlite3_column_text(stmt, 11);
        out.image_api_preset_id = img_api ? img_api : "";
        out.world_info_ids = (const char*)sqlite3_column_text(stmt, 12);
        out.text_preset_ids = (const char*)sqlite3_column_text(stmt, 13);
        const char* bg = (const char*)sqlite3_column_text(stmt, 14);
        out.background_image = bg ? bg : "";
        
        sqlite3_finalize(stmt);
        return true;
    }

    sqlite3_finalize(stmt);
    return false;
}

std::vector<ChatSession> ChatManager::getAllSessions() {
    std::vector<ChatSession> sessions;
    sqlite3* db = getSqliteDb();
    if (!db) return sessions;

    const char* sql = "SELECT * FROM chat_sessions ORDER BY is_pinned DESC, last_updated DESC;";
    sqlite3_stmt* stmt;
    if (sqlite3_prepare_v2(db, sql, -1, &stmt, nullptr) != SQLITE_OK) return sessions;

    while (sqlite3_step(stmt) == SQLITE_ROW) {
        ChatSession s;
        s.id = (const char*)sqlite3_column_text(stmt, 0);
        s.role_id = (const char*)sqlite3_column_text(stmt, 1);
        s.me_id = (const char*)sqlite3_column_text(stmt, 2);
        s.last_updated = sqlite3_column_int64(stmt, 3);
        s.enable_extended_chat = sqlite3_column_int(stmt, 4) != 0;
        s.enable_text_to_image = sqlite3_column_int(stmt, 5) != 0;
        s.enable_emoji = sqlite3_column_int(stmt, 6) != 0;
        s.enable_independent_send_button = sqlite3_column_int(stmt, 7) != 0;
        const char* state = (const char*)sqlite3_column_text(stmt, 8);
        s.current_state = state ? state : "";
        s.is_pinned = sqlite3_column_int(stmt, 9) != 0;
        const char* api = (const char*)sqlite3_column_text(stmt, 10);
        s.api_preset_id = api ? api : "";
        const char* img_api = (const char*)sqlite3_column_text(stmt, 11);
        s.image_api_preset_id = img_api ? img_api : "";
        s.world_info_ids = (const char*)sqlite3_column_text(stmt, 12);
        s.text_preset_ids = (const char*)sqlite3_column_text(stmt, 13);
        const char* bg = (const char*)sqlite3_column_text(stmt, 14);
        s.background_image = bg ? bg : "";
        sessions.push_back(s);
    }

    sqlite3_finalize(stmt);
    return sessions;
}

bool ChatManager::updateSessionLastUpdated(const std::string& sessionId, int64_t timestamp) {
    sqlite3* db = getSqliteDb();
    if (!db) return false;
    const char* sql = "UPDATE chat_sessions SET last_updated = ? WHERE id = ?;";
    sqlite3_stmt* stmt;
    if (sqlite3_prepare_v2(db, sql, -1, &stmt, nullptr) != SQLITE_OK) return false;
    sqlite3_bind_int64(stmt, 1, timestamp);
    sqlite3_bind_text(stmt, 2, sessionId.c_str(), -1, SQLITE_TRANSIENT);
    bool success = sqlite3_step(stmt) == SQLITE_DONE;
    sqlite3_finalize(stmt);
    return success;
}

bool ChatManager::deleteSession(const std::string& sessionId) {
    sqlite3* db = getSqliteDb();
    if (!db) return false;
    deleteMessagesForSession(sessionId);
    const char* sql = "DELETE FROM chat_sessions WHERE id = ?;";
    sqlite3_stmt* stmt;
    if (sqlite3_prepare_v2(db, sql, -1, &stmt, nullptr) != SQLITE_OK) return false;
    sqlite3_bind_text(stmt, 1, sessionId.c_str(), -1, SQLITE_TRANSIENT);
    bool success = sqlite3_step(stmt) == SQLITE_DONE;
    sqlite3_finalize(stmt);
    return success;
}

// ---------- ChatMessage Record CRUD ----------

bool ChatManager::insertMessage(const ChatMessageRecord& m) {
    sqlite3* db = getSqliteDb();
    if (!db) return false;
    const char* sql = R"(
        INSERT INTO chat_messages (id, session_id, is_me, sender, type, content, timestamp, metadata, is_read)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);
    )";
    sqlite3_stmt* stmt;
    if (sqlite3_prepare_v2(db, sql, -1, &stmt, nullptr) != SQLITE_OK) return false;
    sqlite3_bind_text(stmt, 1, m.id.c_str(), -1, SQLITE_TRANSIENT);
    sqlite3_bind_text(stmt, 2, m.session_id.c_str(), -1, SQLITE_TRANSIENT);
    sqlite3_bind_int(stmt, 3, m.is_me ? 1 : 0);
    sqlite3_bind_text(stmt, 4, m.sender.c_str(), -1, SQLITE_TRANSIENT);
    sqlite3_bind_int(stmt, 5, m.type);
    sqlite3_bind_text(stmt, 6, m.content.c_str(), -1, SQLITE_TRANSIENT);
    sqlite3_bind_int64(stmt, 7, m.timestamp);
    sqlite3_bind_text(stmt, 8, m.metadata.c_str(), -1, SQLITE_TRANSIENT);
    sqlite3_bind_int(stmt, 9, m.is_read ? 1 : 0);
    bool success = sqlite3_step(stmt) == SQLITE_DONE;
    sqlite3_finalize(stmt);
    if (success) {
        updateSessionLastUpdated(m.session_id, m.timestamp);
    }
    return success;
}

std::vector<ChatMessageRecord> ChatManager::getMessages(const std::string& sessionId, int limit, int offset) {
    std::vector<ChatMessageRecord> msgs;
    sqlite3* db = getSqliteDb();
    if (!db) return msgs;
    const char* sql = "SELECT * FROM chat_messages WHERE session_id = ? ORDER BY timestamp DESC LIMIT ? OFFSET ?;";
    sqlite3_stmt* stmt;
    if (sqlite3_prepare_v2(db, sql, -1, &stmt, nullptr) != SQLITE_OK) return msgs;
    sqlite3_bind_text(stmt, 1, sessionId.c_str(), -1, SQLITE_TRANSIENT);
    sqlite3_bind_int(stmt, 2, limit);
    sqlite3_bind_int(stmt, 3, offset);
    while (sqlite3_step(stmt) == SQLITE_ROW) {
        ChatMessageRecord m;
        m.id = (const char*)sqlite3_column_text(stmt, 0);
        m.session_id = (const char*)sqlite3_column_text(stmt, 1);
        m.is_me = sqlite3_column_int(stmt, 2) != 0;
        m.sender = (const char*)sqlite3_column_text(stmt, 3);
        m.type = sqlite3_column_int(stmt, 4);
        m.content = (const char*)sqlite3_column_text(stmt, 5);
        m.timestamp = sqlite3_column_int64(stmt, 6);
        const char* meta = (const char*)sqlite3_column_text(stmt, 7);
        m.metadata = meta ? meta : "";
        m.is_read = sqlite3_column_int(stmt, 8) != 0;
        msgs.push_back(m);
    }
    sqlite3_finalize(stmt);
    return msgs;
}

bool ChatManager::getLastMessage(const std::string& sessionId, ChatMessageRecord& out) {
    sqlite3* db = getSqliteDb();
    if (!db) return false;
    const char* sql = "SELECT * FROM chat_messages WHERE session_id = ? ORDER BY timestamp DESC LIMIT 1;";
    sqlite3_stmt* stmt;
    if (sqlite3_prepare_v2(db, sql, -1, &stmt, nullptr) != SQLITE_OK) return false;
    sqlite3_bind_text(stmt, 1, sessionId.c_str(), -1, SQLITE_TRANSIENT);
    if (sqlite3_step(stmt) == SQLITE_ROW) {
        out.id = (const char*)sqlite3_column_text(stmt, 0);
        out.session_id = (const char*)sqlite3_column_text(stmt, 1);
        out.is_me = sqlite3_column_int(stmt, 2) != 0;
        out.sender = (const char*)sqlite3_column_text(stmt, 3);
        out.type = sqlite3_column_int(stmt, 4);
        out.content = (const char*)sqlite3_column_text(stmt, 5);
        out.timestamp = sqlite3_column_int64(stmt, 6);
        const char* meta = (const char*)sqlite3_column_text(stmt, 7);
        out.metadata = meta ? meta : "";
        out.is_read = sqlite3_column_int(stmt, 8) != 0;
        sqlite3_finalize(stmt);
        return true;
    }
    sqlite3_finalize(stmt);
    return false;
}

bool ChatManager::deleteMessagesForSession(const std::string& sessionId) {
    sqlite3* db = getSqliteDb();
    if (!db) return false;
    const char* sql = "DELETE FROM chat_messages WHERE session_id = ?;";
    sqlite3_stmt* stmt;
    if (sqlite3_prepare_v2(db, sql, -1, &stmt, nullptr) != SQLITE_OK) return false;
    sqlite3_bind_text(stmt, 1, sessionId.c_str(), -1, SQLITE_TRANSIENT);
    bool success = sqlite3_step(stmt) == SQLITE_DONE;
    sqlite3_finalize(stmt);
    return success;
}

// ---------- AppSettings ----------

bool ChatManager::setSetting(const std::string& key, const std::string& value) {
    sqlite3* db = getSqliteDb();
    if (!db) return false;
    const char* sql = "INSERT OR REPLACE INTO app_settings (key, value) VALUES (?, ?);";
    sqlite3_stmt* stmt;
    if (sqlite3_prepare_v2(db, sql, -1, &stmt, nullptr) != SQLITE_OK) return false;
    sqlite3_bind_text(stmt, 1, key.c_str(), -1, SQLITE_TRANSIENT);
    sqlite3_bind_text(stmt, 2, value.c_str(), -1, SQLITE_TRANSIENT);
    bool success = sqlite3_step(stmt) == SQLITE_DONE;
    sqlite3_finalize(stmt);
    return success;
}

std::string ChatManager::getSetting(const std::string& key, const std::string& defaultValue) {
    sqlite3* db = getSqliteDb();
    if (!db) return defaultValue;
    const char* sql = "SELECT value FROM app_settings WHERE key = ?;";
    sqlite3_stmt* stmt;
    if (sqlite3_prepare_v2(db, sql, -1, &stmt, nullptr) != SQLITE_OK) return defaultValue;
    sqlite3_bind_text(stmt, 1, key.c_str(), -1, SQLITE_TRANSIENT);
    std::string value = defaultValue;
    if (sqlite3_step(stmt) == SQLITE_ROW) {
        value = (const char*)sqlite3_column_text(stmt, 0);
    }
    sqlite3_finalize(stmt);
    return value;
}

// ---------- ContactMe CRUD ----------

bool ChatManager::saveMe(const ContactMe& me) {
    sqlite3* db = getSqliteDb();
    if (!db) return false;
    const char* sql = R"(
        INSERT OR REPLACE INTO contact_mes (id, name, avatar_path, info, appearance, reference_images)
        VALUES (?, ?, ?, ?, ?, ?);
    )";
    sqlite3_stmt* stmt;
    if (sqlite3_prepare_v2(db, sql, -1, &stmt, nullptr) != SQLITE_OK) return false;
    sqlite3_bind_text(stmt, 1, me.id.c_str(), -1, SQLITE_TRANSIENT);
    sqlite3_bind_text(stmt, 2, me.name.c_str(), -1, SQLITE_TRANSIENT);
    sqlite3_bind_text(stmt, 3, me.avatar_path.c_str(), -1, SQLITE_TRANSIENT);
    sqlite3_bind_text(stmt, 4, me.info.c_str(), -1, SQLITE_TRANSIENT);
    sqlite3_bind_text(stmt, 5, me.appearance.c_str(), -1, SQLITE_TRANSIENT);
    sqlite3_bind_text(stmt, 6, me.reference_images.c_str(), -1, SQLITE_TRANSIENT);
    bool success = sqlite3_step(stmt) == SQLITE_DONE;
    sqlite3_finalize(stmt);
    return success;
}

bool ChatManager::getMe(const std::string& meId, ContactMe& out) {
    sqlite3* db = getSqliteDb();
    if (!db) return false;
    const char* sql = "SELECT * FROM contact_mes WHERE id = ?;";
    sqlite3_stmt* stmt;
    if (sqlite3_prepare_v2(db, sql, -1, &stmt, nullptr) != SQLITE_OK) return false;
    sqlite3_bind_text(stmt, 1, meId.c_str(), -1, SQLITE_TRANSIENT);
    if (sqlite3_step(stmt) == SQLITE_ROW) {
        out.id = (const char*)sqlite3_column_text(stmt, 0);
        out.name = (const char*)sqlite3_column_text(stmt, 1);
        const char* av = (const char*)sqlite3_column_text(stmt, 2);
        out.avatar_path = av ? av : "";
        const char* info = (const char*)sqlite3_column_text(stmt, 3);
        out.info = info ? info : "";
        const char* app = (const char*)sqlite3_column_text(stmt, 4);
        out.appearance = app ? app : "";
        out.reference_images = (const char*)sqlite3_column_text(stmt, 5);
        sqlite3_finalize(stmt);
        return true;
    }
    sqlite3_finalize(stmt);
    return false;
}

std::vector<ContactMe> ChatManager::getAllMe() {
    std::vector<ContactMe> list;
    sqlite3* db = getSqliteDb();
    if (!db) return list;
    const char* sql = "SELECT * FROM contact_mes;";
    sqlite3_stmt* stmt;
    if (sqlite3_prepare_v2(db, sql, -1, &stmt, nullptr) != SQLITE_OK) return list;
    while (sqlite3_step(stmt) == SQLITE_ROW) {
        ContactMe me;
        me.id = (const char*)sqlite3_column_text(stmt, 0);
        me.name = (const char*)sqlite3_column_text(stmt, 1);
        const char* av = (const char*)sqlite3_column_text(stmt, 2);
        me.avatar_path = av ? av : "";
        const char* info = (const char*)sqlite3_column_text(stmt, 3);
        me.info = info ? info : "";
        const char* app = (const char*)sqlite3_column_text(stmt, 4);
        me.appearance = app ? app : "";
        me.reference_images = (const char*)sqlite3_column_text(stmt, 5);
        list.push_back(me);
    }
    sqlite3_finalize(stmt);
    return list;
}

// ---------- ApiPreset CRUD ----------

bool ChatManager::saveApiPreset(const ApiPreset& p) {
    sqlite3* db = getSqliteDb();
    if (!db) return false;
    const char* sql = R"(
        INSERT OR REPLACE INTO api_presets
        (id, name, type, provider, base_url, api_key, model, params, is_default, created_at, updated_at)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
    )";
    sqlite3_stmt* stmt;
    if (sqlite3_prepare_v2(db, sql, -1, &stmt, nullptr) != SQLITE_OK) return false;
    sqlite3_bind_text(stmt, 1, p.id.c_str(), -1, SQLITE_TRANSIENT);
    sqlite3_bind_text(stmt, 2, p.name.c_str(), -1, SQLITE_TRANSIENT);
    sqlite3_bind_int(stmt, 3, p.type);
    sqlite3_bind_int(stmt, 4, p.provider);
    sqlite3_bind_text(stmt, 5, p.base_url.c_str(), -1, SQLITE_TRANSIENT);
    sqlite3_bind_text(stmt, 6, p.api_key.c_str(), -1, SQLITE_TRANSIENT);
    sqlite3_bind_text(stmt, 7, p.model.c_str(), -1, SQLITE_TRANSIENT);
    sqlite3_bind_text(stmt, 8, p.params.c_str(), -1, SQLITE_TRANSIENT);
    sqlite3_bind_int(stmt, 9, p.is_default ? 1 : 0);
    sqlite3_bind_int64(stmt, 10, p.created_at);
    sqlite3_bind_int64(stmt, 11, p.updated_at);
    bool success = sqlite3_step(stmt) == SQLITE_DONE;
    sqlite3_finalize(stmt);
    return success;
}

bool ChatManager::getApiPreset(const std::string& presetId, ApiPreset& out) {
    sqlite3* db = getSqliteDb();
    if (!db) return false;
    const char* sql = "SELECT * FROM api_presets WHERE id = ?;";
    sqlite3_stmt* stmt;
    if (sqlite3_prepare_v2(db, sql, -1, &stmt, nullptr) != SQLITE_OK) return false;
    sqlite3_bind_text(stmt, 1, presetId.c_str(), -1, SQLITE_TRANSIENT);
    if (sqlite3_step(stmt) == SQLITE_ROW) {
        out.id = (const char*)sqlite3_column_text(stmt, 0);
        out.name = (const char*)sqlite3_column_text(stmt, 1);
        out.type = sqlite3_column_int(stmt, 2);
        out.provider = sqlite3_column_int(stmt, 3);
        out.base_url = (const char*)sqlite3_column_text(stmt, 4);
        out.api_key = (const char*)sqlite3_column_text(stmt, 5);
        const char* model = (const char*)sqlite3_column_text(stmt, 6);
        out.model = model ? model : "";
        out.params = (const char*)sqlite3_column_text(stmt, 7);
        out.is_default = sqlite3_column_int(stmt, 8) != 0;
        out.created_at = sqlite3_column_int64(stmt, 9);
        out.updated_at = sqlite3_column_int64(stmt, 10);
        sqlite3_finalize(stmt);
        return true;
    }
    sqlite3_finalize(stmt);
    return false;
}

// ... 更多实现 ...

} // namespace wwj_core
