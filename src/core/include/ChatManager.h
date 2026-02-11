#ifndef CHAT_MANAGER_H
#define CHAT_MANAGER_H

#include <string>
#include <vector>
#include <map>
#include <cstdint>

namespace wwj_core {

// ============================================
// 枚举定义
// ============================================

// 消息类型 (对应旧系统 25 种 MessageType)
enum class MessageType : int {
    Words = 0,
    Action = 1,
    Thought = 2,
    State = 3,
    Emoji = 4,
    Image = 5,
    Location = 6,
    RedPacket = 7,
    Transfer = 8,
    AcceptRedPacket = 9,
    RejectRedPacket = 10,
    AcceptTransfer = 11,
    RejectTransfer = 12,
    Product = 13,
    Link = 14,
    Note = 15,
    Anniversary = 16,
    Memory = 17,
    Diary = 18,
    Moment = 19,
    MomentComment = 20,
    MomentLike = 21,
    Scene = 22,
    Narration = 23,
    Options = 24,
};

// API 供应商
enum class ApiProvider : int {
    OpenAI = 0,
    Gemini = 1,
    Volcengine = 2,
    OpenAICompatible = 3,
    Minimax = 4,
    GrokLike = 5,
};

// API 类型
enum class ApiType : int {
    Text = 0,
    ImageGen = 1,
    Voice = 2,
};

// 表情类型
enum class EmojiType : int {
    Global = 0,
    Role = 1,
};

// 文本预设类型
enum class TextPresetType : int {
    Chat = 0,
    ImageStyle = 1,
};

// 记忆分类
enum class MemoryCategory : int {
    General = 0,
    Important = 1,
};

// 钱包交易类型
enum class WalletTransactionType : int {
    TransferType = 0,
    RedPacketType = 1,
};

// 钱包交易方向
enum class WalletTransactionDirection : int {
    Income = 0,
    Expense = 1,
};

// ============================================
// 数据结构
// ============================================

struct ChatSession {
    std::string id;
    std::string role_id;
    std::string me_id;
    int64_t last_updated;
    bool enable_extended_chat;
    bool enable_text_to_image;
    bool enable_emoji;
    bool enable_independent_send_button;
    std::string current_state;
    bool is_pinned;
    std::string api_preset_id;
    std::string image_api_preset_id;
    std::string world_info_ids;      // JSON array: "[]"
    std::string text_preset_ids;     // JSON array: "[]"
    std::string background_image;
};

struct ChatMessageRecord {
    std::string id;
    std::string session_id;
    bool is_me;
    std::string sender;
    int type;                        // MessageType as int
    std::string content;
    int64_t timestamp;
    std::string metadata;            // JSON string
    bool is_read;
};

struct ContactRole {
    std::string id;
    std::string name;
    std::string avatar_path;
    std::string description;
    std::string appearance;
    std::string reference_images;    // JSON array: "[]"
    std::string subscribed_group_ids;// JSON array: "[]"
    std::string subscribed_emoji_ids;// JSON array: "[]"
};

struct ContactMe {
    std::string id;
    std::string name;
    std::string avatar_path;
    std::string info;
    std::string appearance;
    std::string reference_images;    // JSON array: "[]"
};

struct ApiPreset {
    std::string id;
    std::string name;
    int type;                        // ApiType as int
    int provider;                    // ApiProvider as int
    std::string base_url;
    std::string api_key;
    std::string model;
    std::string params;              // JSON string
    bool is_default;
    int64_t created_at;
    int64_t updated_at;
};

struct EmojiRecord {
    std::string id;                  // "emoji-id-00001"
    std::string meaning;
    std::string raw_content;
    std::string group_id;
    std::string local_path;
    std::string backup_path;
    int type;                        // EmojiType as int
    std::string role_id;
    int64_t created_at;
};

struct EmojiGroup {
    std::string id;
    std::string name;
    int type;                        // EmojiType as int
    std::string role_id;
    bool is_visible;
    int64_t created_at;
};

struct WorldInfo {
    std::string id;
    std::string name;
    std::string content;
    int64_t created_at;
    int64_t updated_at;
};

struct TextPreset {
    std::string id;
    std::string name;
    std::string content;
    int type;                        // TextPresetType as int
    bool is_built_in;
    int64_t created_at;
    int64_t updated_at;
};

struct RoleMemory {
    std::string id;
    std::string role_id;
    std::string content;
    int64_t created_at;
    int64_t updated_at;
    std::string source_session_id;
    int category;                    // MemoryCategory as int
};

struct MomentsPost {
    std::string id;
    std::string user_json;           // JSON of MomentsUser
    std::string content;
    std::string media_items_json;    // JSON array
    std::string likes_json;          // JSON array
    std::string comments_json;       // JSON array
    std::string location;
    int64_t created_at;
};

struct MomentsUserSetting {
    std::string id;
    std::string user_json;           // JSON of MomentsUser
    int64_t updated_at;
};

struct WalletTransaction {
    std::string id;
    int type;
    int direction;
    double amount;
    std::string description;
    std::string related_contact_id;
    std::string related_contact_name;
    std::string related_session_id;
    std::string related_message_id;
    int64_t timestamp;
};

struct AppSetting {
    std::string key;
    std::string value;
};

// ============================================
// ChatManager 核心类
// ============================================

class ChatManager {
public:
    static ChatManager& getInstance();

    bool initialize();
    bool isInitialized() const;

    // ChatSession CRUD
    bool saveSession(const ChatSession& session);
    bool getSession(const std::string& sessionId, ChatSession& outSession);
    std::vector<ChatSession> getAllSessions();
    bool updateSessionLastUpdated(const std::string& sessionId, int64_t timestamp);
    bool deleteSession(const std::string& sessionId);

    // ChatMessage CRUD
    bool insertMessage(const ChatMessageRecord& message);
    std::vector<ChatMessageRecord> getMessages(const std::string& sessionId, int limit = 50, int offset = 0);
    bool getLastMessage(const std::string& sessionId, ChatMessageRecord& outMessage);
    bool deleteMessage(const std::string& messageId);
    bool deleteMessagesForSession(const std::string& sessionId);
    int getUnreadCount(const std::string& sessionId);

    // ContactRole CRUD
    bool saveRole(const ContactRole& role);
    bool getRole(const std::string& roleId, ContactRole& outRole);
    std::vector<ContactRole> getAllRoles();
    bool deleteRole(const std::string& roleId);

    // ContactMe CRUD
    bool saveMe(const ContactMe& me);
    bool getMe(const std::string& meId, ContactMe& outMe);
    std::vector<ContactMe> getAllMe();
    bool deleteMe(const std::string& meId);

    // ApiPreset CRUD
    bool saveApiPreset(const ApiPreset& preset);
    bool getApiPreset(const std::string& presetId, ApiPreset& outPreset);
    std::vector<ApiPreset> getApiPresets(int type = -1);
    bool deleteApiPreset(const std::string& presetId);

    // Emoji CRUD
    bool saveEmoji(const EmojiRecord& emoji);
    bool getEmoji(const std::string& emojiId, EmojiRecord& outEmoji);
    std::vector<EmojiRecord> getAllEmojis();
    std::vector<EmojiRecord> getEmojisByGroup(const std::string& groupId);
    bool deleteEmoji(const std::string& emojiId);
    std::string generateNextEmojiId();

    // EmojiGroup CRUD
    bool saveEmojiGroup(const EmojiGroup& group);
    std::vector<EmojiGroup> getAllEmojiGroups();
    bool deleteEmojiGroup(const std::string& groupId);

    // WorldInfo CRUD
    bool saveWorldInfo(const WorldInfo& info);
    std::vector<WorldInfo> getAllWorldInfos();
    bool deleteWorldInfo(const std::string& infoId);

    // TextPreset CRUD
    bool saveTextPreset(const TextPreset& preset);
    std::vector<TextPreset> getAllTextPresets();
    bool deleteTextPreset(const std::string& presetId);

    // RoleMemory CRUD
    bool saveMemory(const RoleMemory& memory);
    std::vector<RoleMemory> getMemoriesForRole(const std::string& roleId);
    bool deleteMemory(const std::string& memoryId);

    // MomentsPost CRUD
    bool saveMomentsPost(const MomentsPost& post);
    std::vector<MomentsPost> getAllMomentsPosts();
    bool deleteMomentsPost(const std::string& postId);

    // WalletTransaction CRUD
    bool saveWalletTransaction(const WalletTransaction& tx);
    std::vector<WalletTransaction> getAllWalletTransactions();
    double getWalletBalance();

    // AppSettings
    bool setSetting(const std::string& key, const std::string& value);
    std::string getSetting(const std::string& key, const std::string& defaultValue = "");

private:
    ChatManager() : initialized(false) {}
    ~ChatManager() = default;

    ChatManager(const ChatManager&) = delete;
    ChatManager& operator=(const ChatManager&) = delete;

    bool createChatTables();

    bool initialized;
};

} // namespace wwj_core

#endif // CHAT_MANAGER_H
