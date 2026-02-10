# LLM 聊天集成方案：旧系统融合到 WangWangPhone

> **文档状态**: v2.0.0 — 基于全量旧文档（11 份）更新  
> **参考文档**: `docs/old/` 下的全部文档

## 1. 现状分析

### 1.1 旧系统（LNPhone, Flutter/Dart）
旧系统是一个功能完善的 **AI Native** 模拟应用，其核心逻辑高度解耦，主要包含：

| 模块 | 说明 |
|------|------|
| **LLM 集成** | 支持 OpenAI/Gemini/Minimax 等多供应商，具备上下文构建和响应重试机制。 |
| **消息解析** | 25 种消息类型，JSON 优先 → XML 回退 → 纯文本兜底。 |
| **生图系统** | 支持 4 种生图 API（火山、Gemini、OpenAI、Grok），具备风格预设和外貌保持。 |
| **表情包系统** | 统一表情池 + 订阅模型，支持 AI 语义匹配和“偷图”机制。 |
| **社交/经济系统** | 完整朋友圈联动、角色长期记忆、虚拟红包/转账。 |
| **持久化** | Drift (SQLite) 驱动，14 张核心表，Schema Version 36。 |

### 1.2 新系统（WangWangPhone, C++ Core + Native UI）
新系统采用 **C++ 核心 + 原生 UI** 架构，目前聊天模块仅有 UI Mock。

---

## 2. 融合架构设计

### 2.1 职责分层 (Responsibility Layering)

我们将遵循“逻辑入 C++，展现入原生”的原则：

| 放入 C++ Core 的逻辑 (Cross-platform Logic) | 放入原生层的逻辑 (Platform-specific) |
|-------------------------------------------|------------------------------------|
| **数据库全量表管理**: 14 张核心表的 CRUD 与迁移 | **网络请求实现**: OkHttp / URLSession (处理 SSE/流式) |
| **系统提示词与上下文**: 拼装角色设定、记忆、世界书 | **后台回复 Service**: Android Foreground Service / iOS BGTask |
| **消息解析引擎**: JSON/XML 解析、类型标准化、正则预处理 | **通知推送**: 系统通知渠道、自定义提示音播放 |
| **表情包匹配逻辑**: 语义搜索、ID 回收机制 | **UI 状态管理**: ViewModel、StateFlow / @Published |
| **数据导入导出**: 导出包含 Base64 图片的完整 JSON | **文件/系统交互**: 拍照、选择文件、存储权限 |

### 2.2 数据流转模型

```
[原生 UI] <-> [原生 ViewModel] <-> [C++ Bridge] <-> [C++ ChatService] <-> [C++ Database]
                                        |
                                [原生 Network Service] <-> [LLM API]
```

---

## 3. 核心模块详细设计

### 3.1 数据库扩展 (src/core/DatabaseManager)
在现有 `DatabaseManager.h/cpp` 基础上，完整移植旧系统的 14 张表定义。
- **关键决策**: 禁用外键约束 (`PRAGMA foreign_keys = OFF`)，避免 `replace` 操作导致的级联删除。
- **存储策略**: 图片资源使用 `localPath` + `backupPath` 双路径设计，避免缓存清理导致数据丢失。

### 3.2 消息解析系统 (ResponseParser)
移植旧系统的解析逻辑到 C++：
1. **预处理**: 正则移除思考过程 (`<think>`)、角色名前缀、Markdown 标记。
2. **三级解析**:
   - `tryParseJson`: 提取 `[]` 数组或 ` ```json ` 块。
   - `tryParseXml`: 包装 `<root>` 后解析标签。
   - `Fallback`: 全文作为 `words` 类型。
3. **类型标准化**: 将 `text/say/message` 统一映射为 `word`。

### 3.3 表情包与“偷图”机制
- **统一表情池**: C++ 管理 `emojis` 全局池。
- **订阅模型**: `contact_roles` 存储订阅的分组/个体 ID。
- **偷图逻辑**: 当解析到 AI 使用了不在角色订阅列表中的 ID 时，C++ 自动更新订阅表。

### 3.4 生图 API 集成
在原生网络层实现四种供应商的适配：
- **Volcengine**: `images/generations` 接口，处理 `b64_json`。
- **Gemini**: `generateContent` 接口（需特殊 URL 参数）。
- **Groklike**: 通过 `chat/completions` 发送提示词并解析 Markdown 图片链接。

---

## 4. 实施阶段计划 (MVP -> Full)

### Phase 1: 核心基础构建 (C++ & Bridge)
- [ ] 扩展 `DatabaseManager` 实现 14 张聊天/角色相关表。
- [ ] 实现 `ResponseParser` 的 C++ 版本（含正则预处理）。
- [ ] 编写 JNI/Obj-C++ 桥接接口：`buildLlmContext`, `parseAndSaveResponse`。

### Phase 2: UI 动态化 (Kotlin & Swift)
- [ ] 将 `ChatApp` 的 mock 数据（Conversation/Message）改为读取 C++ 数据库。
- [ ] 实现基础的发送逻辑：UI -> C++ 存入消息 -> 刷新。

### Phase 3: LLM 实装 (Native Network)
- [ ] 在原生层实现 OpenAI 与 Gemini 格式的 HTTP 调用。
- [ ] 对接生图 API 的原生网络实现。
- [ ] 支持 25 种消息类型的渲染（红包、转账、朋友圈卡片等）。

### Phase 4: 高级功能移植
- [ ] **主动回复**: 实现 Android WorkManager 与 iOS 后台任务。
- [ ] **表情系统**: 实现 C++ 表情语义匹配逻辑。
- [ ] **朋友圈联动**: 实现 AI 自动发布朋友圈及评论点赞系统。

---

## 5. 关键兼容性注意事项
1. **GIF 保护**: 在原生层移动文件时必须使用字节流复制，不可直接用系统 Move 接口，以防损坏透明通道。
2. **简化 ID**: 发送给 AI 的消息列表必须映射为 `0001`, `0002` 格式，C++ 维护 ID 映射表以确保引用准确。
3. **Sensitive Settings**: API Key 不得导出到 JSON，导入后需引导用户重新输入。

---
*文档更新于: 2026-02-10*
