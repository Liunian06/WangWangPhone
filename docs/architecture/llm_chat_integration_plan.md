# LLM 聊天集成方案：旧系统融合到 WangWangPhone

## 1. 现状分析

### 1.1 旧系统（LNPhone, Flutter/Dart）
旧系统是一个 **Flutter** 应用，使用 Dart 语言开发，采用 **Provider** 状态管理，核心功能包括：

| 模块 | 说明 |
|------|------|
| LLM 集成 | 多供应商 API（OpenAI/Gemini/自定义兼容接口），系统提示词构建，上下文管理 |
| 消息解析 | JSON 优先 → XML 回退 → 纯文本兜底的三级解析策略 |
| 后台服务 | 定时主动回复，通知推送，用户不活跃检测 |
| 状态管理 | Provider 模式，ChatProvider/ContactProvider 等多个 ChangeNotifier |

### 1.2 新系统（WangWangPhone, C++ Core + 原生 UI）
新系统采用 **C++ 核心 + 各平台原生 UI** 的架构：
- **C++ Core**: SQLite 持久化 (`DatabaseManager`)，授权管理 (`LicenseManager`)
- **Android**: Kotlin + Jetpack Compose
- **iOS**: Swift + SwiftUI
- **Chat App**: 当前是一个纯 UI 模拟，使用 mock 数据，没有真实的 LLM 交互

### 1.3 核心差异

| 维度 | 旧系统 (Flutter) | 新系统 (WangWangPhone) |
|------|------------------|----------------------|
| UI 框架 | Flutter (跨平台一套代码) | 原生 UI (Kotlin/Swift 各一套) |
| 状态管理 | Dart Provider | Compose State / SwiftUI @State |
| 数据库 | Dart Drift ORM | C++ SQLite (跨平台共享) |
| 网络请求 | Dart http 包 | 需要在 C++ 或原生层实现 |
| 后台服务 | Flutter background_service | 原生 Android Service / iOS BGTask |

---

## 2. 融合策略

### 2.1 总体架构

```
┌─────────────────────────────────────────────────────────────────────┐
│                       平台原生 UI 层                                  │
│                                                                      │
│  ┌──────────────────────┐    ┌──────────────────────┐               │
│  │  Android (Kotlin)     │    │  iOS (Swift)          │               │
│  │  ChatApp.kt           │    │  ChatApp.swift        │               │
│  │  - ChatDetailScreen   │    │  - ChatDetailView     │               │
│  │  - LlmChatViewModel  │    │  - LlmChatViewModel  │               │
│  └──────────┬───────────┘    └──────────┬───────────┘               │
│             │                           │                            │
│             │  JNI Bridge               │  Obj-C++ Bridge            │
│             └─────────────┬─────────────┘                            │
│                           │                                          │
└───────────────────────────┼──────────────────────────────────────────┘
                            │
┌───────────────────────────┼──────────────────────────────────────────┐
│                    C++ Core (src/core)                                │
│                                                                      │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │                    ChatService (新增)                        │    │
│  │  - 管理聊天会话 (CRUD)                                       │    │
│  │  - 管理消息 (CRUD)                                          │    │
│  │  - 构建 LLM 上下文                                          │    │
│  │  - 解析 LLM 响应 (JSON → XML → 纯文本)                      │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │                    LlmService (新增)                         │    │
│  │  - 多供应商 API 调用 (OpenAI / Gemini / 兼容接口)             │    │
│  │  - 系统提示词构建                                            │    │
│  │  - 重试机制 (3次, 指数退避)                                   │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │                    ResponseParser (新增)                     │    │
│  │  - JSON 解析 (直接/代码块/数组提取)                           │    │
│  │  - XML 解析 (直接/包装root/修复错误)                          │    │
│  │  - 类型标准化 (word/action/thought/emoji/image)              │    │
│  │  - 正则预处理                                                │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
│  ┌──────────────────────────────────────┐                           │
│  │  DatabaseManager (已有, 需扩展)       │                           │
│  │  + chat_sessions 表                   │                           │
│  │  + chat_messages 表                   │                           │
│  │  + api_presets 表                     │                           │
│  │  + contacts (角色) 表                 │                           │
│  │  + chat_settings 表                   │                           │
│  └──────────────────────────────────────┘                           │
│                                                                      │
└──────────────────────────────────────────────────────────────────────┘
```

### 2.2 分层决策

#### 放入 C++ Core 的逻辑（跨平台共享）

| 模块 | 原因 |
|------|------|
| 聊天数据 CRUD | 数据库操作应统一在 C++ 层，避免各平台重复实现 |
| LLM 上下文构建 | 系统提示词、消息历史、记忆等构建逻辑与平台无关 |
| 响应解析 | JSON/XML 解析、类型标准化是纯逻辑，无平台依赖 |
| API 预设管理 | 供应商配置存储和管理 |

#### 放在平台原生层的逻辑

| 模块 | 原因 |
|------|------|
| HTTP 网络请求 | C++ 的 libcurl 部署复杂；原生 OkHttp/URLSession 更成熟 |
| 后台服务 | Android Foreground Service / iOS BGTask 差异巨大 |
| 通知推送 | 完全平台相关的 API |
| UI 状态管理 | Compose State / SwiftUI @State 是各平台原生机制 |

### 2.3 网络请求策略（关键决策）

旧系统的 LLM API 调用使用 Dart `http` 包。新系统有两个选择：

| 方案 | 优点 | 缺点 | 选择 |
|------|------|------|------|
| **A: 原生层发请求** | 零依赖、成熟稳定、调试方便 | 网络逻辑需要 Android/iOS 各写一套 | ✅ MVP 阶段 |
| B: C++ 层用 libcurl | 一次编写 | 编译复杂、SSL 证书问题多 | ❌ |

**MVP 阶段策略**: 网络请求在原生层实现，C++ Core 只负责构建请求参数和解析响应。

---

## 3. 数据库扩展设计

### 3.1 新增表结构

```sql
-- 聊天会话表
CREATE TABLE IF NOT EXISTS chat_sessions (
    id TEXT PRIMARY KEY,
    role_name TEXT NOT NULL,          -- 角色名称
    role_avatar TEXT DEFAULT '',       -- 角色头像 emoji/文件路径
    role_description TEXT DEFAULT '',  -- 角色设定
    api_preset_id TEXT,               -- 关联的 API 预设
    last_message TEXT DEFAULT '',      -- 最后一条消息预览
    last_message_time INTEGER DEFAULT 0,
    unread_count INTEGER DEFAULT 0,
    is_muted INTEGER DEFAULT 0,
    enable_extended_chat INTEGER DEFAULT 0,  -- 是否启用扩展聊天(动作/想法)
    enable_emoji INTEGER DEFAULT 0,
    created_at INTEGER DEFAULT (strftime('%s', 'now')),
    updated_at INTEGER DEFAULT (strftime('%s', 'now'))
);

-- 聊天消息表
CREATE TABLE IF NOT EXISTS chat_messages (
    id TEXT PRIMARY KEY,
    session_id TEXT NOT NULL,
    sender_type TEXT NOT NULL,         -- 'user' | 'assistant' | 'system'
    message_type TEXT NOT NULL,        -- 'word' | 'action' | 'thought' | 'emoji' | 'image' | 'time'
    content TEXT NOT NULL,
    metadata TEXT DEFAULT '',          -- JSON 格式的额外信息
    is_read INTEGER DEFAULT 0,
    timestamp INTEGER NOT NULL,
    FOREIGN KEY (session_id) REFERENCES chat_sessions(id) ON DELETE CASCADE
);

-- API 预设表
CREATE TABLE IF NOT EXISTS api_presets (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    api_type TEXT NOT NULL,            -- 'text' | 'image'
    provider TEXT NOT NULL,            -- 'openai' | 'gemini' | 'openaicompatible'
    base_url TEXT NOT NULL,
    api_key TEXT NOT NULL,
    model TEXT NOT NULL,
    params TEXT DEFAULT '{}',          -- JSON 格式的额外参数
    is_default INTEGER DEFAULT 0,
    created_at INTEGER DEFAULT (strftime('%s', 'now')),
    updated_at INTEGER DEFAULT (strftime('%s', 'now'))
);

-- 聊天设置表 (键值对)
CREATE TABLE IF NOT EXISTS chat_settings (
    key TEXT PRIMARY KEY,
    value TEXT NOT NULL,
    updated_at INTEGER DEFAULT (strftime('%s', 'now'))
);
```

### 3.2 消息类型定义

```cpp
enum class MessageType {
    Word,       // 普通文字
    Action,     // 动作描写 (*微笑*)
    Thought,    // 内心想法
    State,      // 状态描述
    Emoji,      // 表情包
    Image,      // 图片
    Time,       // 时间标记
    System      // 系统消息
};

enum class SenderType {
    User,       // 用户发送
    Assistant,  // AI 回复
    System      // 系统消息
};
```

---

## 4. 模块融合详细设计

### 4.1 LLM 集成 → C++ ChatService + 原生 ViewModel

**旧系统**: `LlmService.generateResponse()` 在 Dart 中完成所有工作。

**新系统拆分**:

```
原生 ViewModel (Kotlin/Swift)
    │
    │ 1. 从 C++ 获取上下文参数 (通过 Bridge)
    │    → ChatService::buildLlmContext(sessionId)
    │    → 返回: systemPrompt, messages[], apiPresetConfig
    │
    │ 2. 在原生层发送 HTTP 请求
    │    → OkHttp / URLSession
    │    → 根据 provider 选择 OpenAI / Gemini 格式
    │
    │ 3. 将原始响应传回 C++ 解析
    │    → ResponseParser::parse(rawResponse)
    │    → 返回: ParsedMessage[]
    │
    │ 4. 保存解析后的消息
    │    → ChatService::saveMessages(sessionId, messages)
    │
    │ 5. 更新 UI 状态
    └→ LiveData/StateFlow / @Published
```

### 4.2 消息解析 → C++ ResponseParser

旧系统的解析逻辑（JSON → XML → 纯文本）完全是平台无关的，适合放入 C++ Core：

```cpp
// src/core/include/ResponseParser.h

class ResponseParser {
public:
    struct ParsedMessage {
        std::string type;     // "word", "action", "thought", "emoji", "image"
        std::string content;
    };
    
    // 解析 AI 响应
    static std::vector<ParsedMessage> parse(const std::string& response);
    
private:
    // JSON 解析尝试
    static std::vector<ParsedMessage> tryParseJson(const std::string& response);
    
    // XML 解析尝试
    static std::vector<ParsedMessage> tryParseXml(const std::string& response);
    
    // 类型标准化
    static std::string normalizeType(const std::string& type);
    
    // 预处理（正则清理）
    static std::string preprocess(const std::string& response);
};
```

### 4.3 后台服务 → 平台原生实现

后台主动回复是高度平台相关的功能，必须用原生实现：

**Android**:
```kotlin
// 使用 WorkManager (推荐) 或 Foreground Service
class ActiveReplyWorker(context: Context, params: WorkerParameters) 
    : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result {
        // 1. 从 C++ 检查触发条件
        // 2. 从 C++ 获取上下文
        // 3. 发送 API 请求
        // 4. 通过 C++ 解析响应并保存
        // 5. 发送原生通知
        return Result.success()
    }
}
```

**iOS**:
```swift
// 使用 BGAppRefreshTask
func scheduleActiveReply() {
    let request = BGAppRefreshTaskRequest(identifier: "com.wangwang.activeReply")
    request.earliestBeginDate = Date(timeIntervalSinceNow: 30 * 60)
    try? BGTaskScheduler.shared.submit(request)
}
```

### 4.4 状态管理 → 原生 ViewModel

旧系统的 Provider 模式映射到各平台原生状态管理：

| 旧系统 (Flutter Provider) | Android (Kotlin) | iOS (Swift) |
|--------------------------|------------------|-------------|
| `ChatProvider` | `ChatViewModel : ViewModel` | `ChatViewModel : ObservableObject` |
| `ChangeNotifier` | `StateFlow / LiveData` | `@Published` |
| `Consumer<T>` | `collectAsState()` | `@ObservedObject` |
| `Provider.of<T>()` | Hilt/手动注入 | 环境对象注入 |

---

## 5. MVP 阶段实施计划

### Phase 1: 基础数据层 (C++ Core)
1. 扩展 `DatabaseManager` — 新增 4 张表
2. 新建 `ChatService` — 会话和消息的 CRUD
3. 新建 `ResponseParser` — JSON/XML 解析
4. 平台 Bridge 层 — JNI (Android) + Obj-C++ (iOS)

### Phase 2: 聊天 UI 改造 (Android + iOS)
1. 将 mock 数据替换为数据库驱动
2. 新建 `LlmChatViewModel` — 管理聊天状态
3. 接入真实的发送/接收流程
4. 添加 API 预设配置页面

### Phase 3: LLM API 对接 (原生网络层)
1. Android: OkHttp 实现 OpenAI/Gemini 调用
2. iOS: URLSession 实现 OpenAI/Gemini 调用
3. 流式响应支持 (SSE)
4. 重试机制

### Phase 4: 高级功能
1. 后台主动回复 (Android WorkManager / iOS BGTask)
2. 通知推送
3. 表情包系统
4. 记忆系统

---

## 6. 关键技术决策总结

| 决策点 | 选择 | 理由 |
|--------|------|------|
| 网络请求位置 | 原生层 | 避免 C++ HTTP 库的跨平台编译问题 |
| 响应解析位置 | C++ Core | 纯逻辑计算，无平台依赖，一次编写 |
| 数据库位置 | C++ Core (SQLite) | 遵循现有架构，数据一致性保证 |
| 后台服务位置 | 原生层 | Android/iOS 后台机制差异太大 |
| 系统提示词存储 | C++ Core 数据库 | 可运行时修改，不需要重编译 |
| 消息 ID 策略 | 沿用简化 ID | AI 引用准确率高，已验证有效 |

---

*文档版本: 1.0.0 | 创建时间: 2026-02-10*
