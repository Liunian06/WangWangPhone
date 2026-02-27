# iOS 对齐安卓基线 TODO（全量差异修复版）

更新时间：2026-02-27
对齐基线：`Android`（`src/platforms/android`）  
目标平台：`iOS`（`src/platforms/ios`）

---

## 0. 文档用途

这份文档用于后续“按任务逐条修复 iOS 开发进度未对齐 Android”的工作，不只是汇总问题。

使用方式：

1. 先按 `P0` 全部完成，再处理 `P1`。  
2. 每个任务都带了证据定位（文件+行号）和验收标准。  
3. 修复后在对应任务后打勾，并补充实际提交哈希。

---

## 1. 结论总览（先说结论）

经过 2026-02-27 全量复查，**上一轮 P0/P1/P2 任务已全部完成**。

当前 iOS 与 Android 的差距已大幅缩小，仅剩 2 个小差异：

1. 激活页缺少"复制机器码"和"粘贴激活码"便捷按钮（ACTIVATION-001）
2. 神笔马良分支创建后未直接跳转到新卡（PB-006）

其余模块（聊天系统、显示设置、壁纸、联系人、Reset、构建产物等）均已对齐。

---

## 2. 模块对齐矩阵（Android = 基线）

| 模块 | Android 状态 | iOS 状态 | 对齐结论 |
| --- | --- | --- | --- |
| MainContainer / LockScreen | 完整 | 完整 | ✅ 已对齐 |
| Home 桌面布局/拖拽 | 完整 | 完整 | ✅ 已对齐 |
| DisplaySettings | 完整（锁屏/桌面壁纸+吧唧） | 完整 | ✅ 已对齐 |
| API Presets | 完整 | 完整 | ✅ 已对齐 |
| Chat 数据层 | 会话+消息模型完整 | 完整 | ✅ 已对齐 |
| Chat UI 路由 | 主流程完整（新增/编辑/设置/预设） | 完整 | ✅ 已对齐 |
| Contact 数据层 | 字段完整+更新能力 | 完整 | ✅ 已对齐 |
| PersonaBuilder（神笔马良） | 完整（含分支跳转） | 分支创建后回退列表而非跳转 | 部分对齐 |
| Reset 到默认设置 | 清理布局/壁纸/天气/资料/自定义图标 | 完整 | ✅ 已对齐 |
| 构建产物治理 | 正常 | 完整 | ✅ 已对齐 |
| 激活页 | 有复制机器码+粘贴激活码按钮 | 缺少便捷操作按钮 | 未对齐 |

---

## 3. 全量差异任务清单

下面是“当前代码中已定位到的全量差异任务”。

## P0（必须先修，影响主流程一致性）

### [x] CHAT-001：iOS 聊天缺少"会话模型"

差异说明：

- Android 聊天核心是 `conversations + messages`。
- iOS 当前只有 `messages`，无法表达会话级配置（静音、预设、未读等）。

证据：

- Android 会话创建：`src/platforms/android/app/src/main/java/com/WangWangPhone/core/ChatDbHelper.kt:119`
- Android 会话查询：`src/platforms/android/app/src/main/java/com/WangWangPhone/core/ChatDbHelper.kt:142`
- Android 会话设置：`src/platforms/android/app/src/main/java/com/WangWangPhone/core/ChatDbHelper.kt:323`
- iOS 仅消息表：`src/platforms/ios/WangWangPhone/Core/ChatDbHelper.swift:43`

TODO：

- [x] iOS `ChatDbHelper` 新增 `conversations` 表
- [x] 补齐 `createConversation/getAllConversations/getConversationById/deleteConversation/setMuted/updateApiPresetId`
- [x] 消息表改为关联 `conversation_id`（或增加迁移兼容层）

验收标准：

- 能在 iOS 按会话维度查看、设置和持久化聊天配置。

---

### [x] CHAT-002：iOS 聊天路由缺失（新增联系人/编辑联系人/聊天设置/API预设选择）

差异说明：

- Android `currentView` 路由覆盖完整。
- iOS 仅有 `chat-detail/contact-detail/service/select-contacts`。

证据：

- Android 有 `AddContactScreen/EditContactScreen/ChatSettingsScreen/ApiPresetSelectionScreen`：`src/platforms/android/app/src/main/java/com/WangWangPhone/ui/ChatApp.kt:139`
- Android 路由进入编辑页：`src/platforms/android/app/src/main/java/com/WangWangPhone/ui/ChatApp.kt:646`
- iOS 主路由开关：`src/platforms/ios/WangWangPhone/Views/ChatApp.swift:309`

TODO：

- [x] iOS 补 `add-contact`
- [x] iOS 补 `edit-contact`
- [x] iOS 补 `chat-settings`
- [x] iOS 补 `select-api-preset`

验收标准：

- iOS Chat 页面具备与 Android 一致的子页面可达性和回退逻辑。

---

### [x] CHAT-003：iOS Chat 数据源割裂（同模块内"静态数据 + 数据库"混用）

差异说明：

- iOS 聊天列表/通讯录页使用静态 `wx...` 数据。
- 但“发起聊天选人”又使用数据库联系人，导致行为不一致。

证据：

- iOS 静态会话：`src/platforms/ios/WangWangPhone/Views/ChatApp.swift:188`
- iOS 消息页渲染静态会话：`src/platforms/ios/WangWangPhone/Views/ChatApp.swift:500`
- iOS 通讯录静态列表：`src/platforms/ios/WangWangPhone/Views/ChatApp.swift:550`
- iOS 选人页改用数据库：`src/platforms/ios/WangWangPhone/Views/ChatApp.swift:1222`
- Android 会话列表来自数据库：`src/platforms/android/app/src/main/java/com/WangWangPhone/ui/ChatApp.kt:465`

TODO：

- [x] iOS 消息列表改为数据库驱动
- [x] iOS 通讯录列表改为数据库驱动
- [x] 静态 `wxConversations/wxContactList` 仅保留预览或测试用途

验收标准：

- 在 iOS 添加/编辑联系人、创建会话后，消息页和通讯录页能实时一致反映。

---

### [x] CHAT-004：iOS 联系人数据结构不对齐 Android（字段缺失 + 无更新）

差异说明：

- Android 联系人含 `wechatId/region/persona/avatarFileName/updatedAt` 且支持更新。
- iOS 联系人目前字段较少，无 `updateContact`。

证据：

- Android `updateContact`：`src/platforms/android/app/src/main/java/com/WangWangPhone/core/ContactDbHelper.kt:223`
- iOS 联系人增删查：`src/platforms/ios/WangWangPhone/Core/ContactDbHelper.swift:92`

TODO：

- [x] iOS 联系人表补齐安卓核心字段
- [x] iOS 增加 `updateContact`
- [x] iOS 联系人编辑页与数据层打通

验收标准：

- iOS 联系人详情能编辑昵称/微信号/地区/人设/头像并持久化。

---

### [x] CHAT-005：iOS 发起双人设聊天时"第二联系人（用户人设）被丢弃"

差异说明：

- Android 用两位联系人创建会话（AI角色 + 用户人设）。
- iOS 选人回调里只用了第一个联系人 ID，第二个参数被忽略。

证据：

- Android 创建会话使用两个 ID：`src/platforms/android/app/src/main/java/com/WangWangPhone/ui/ChatApp.kt:605`
- iOS 选人回调忽略第二参数：`src/platforms/ios/WangWangPhone/Views/ChatApp.swift:319`

TODO：

- [x] iOS 会话创建保存 `aiRoleId + userPersonaId`
- [x] iOS 发送消息时从会话读取两类人设

验收标准：

- iOS AI 回复可稳定区分“AI角色设定”和“用户人设设定”。

---

### [x] CHAT-006：iOS 缺会话级 API 预设绑定（当前只取第一个聊天预设）

差异说明：

- Android 支持“创建会话时选预设”+“聊天设置里修改预设”。
- iOS 发送消息时总是取 `chatPresets.first`。

证据：

- Android 预设选择页：`src/platforms/android/app/src/main/java/com/WangWangPhone/ui/ChatApp.kt:139`
- Android 会话改预设：`src/platforms/android/app/src/main/java/com/WangWangPhone/ui/ChatApp.kt:597`
- iOS 使用首个预设：`src/platforms/ios/WangWangPhone/Views/ChatApp.swift:948`

TODO：

- [x] iOS 实装会话级 `apiPresetId`
- [x] iOS 新增聊天设置入口并可切换预设

验收标准：

- 同一 iOS App 内两个会话可使用不同预设并独立生效。

---

### [x] DISPLAY-001：iOS 显示设置缺失锁屏/桌面壁纸设置

差异说明：

- Android `DisplaySettingsScreen` 已可设置锁屏和桌面壁纸。
- iOS `DisplaySettingsView` 当前仍显示“壁纸设置功能待实现”。

证据：

- Android 壁纸设置实现：`src/platforms/android/app/src/main/java/com/WangWangPhone/ui/HomeScreen.kt:1891`
- iOS 占位文案：`src/platforms/ios/WangWangPhone/Views/HomeScreen.swift:1067`

TODO：

- [x] iOS 增加锁屏壁纸选择
- [x] iOS 增加桌面壁纸选择
- [x] iOS 设置后刷新 Home + Lock 画面

验收标准：

- iOS 可独立设置并看到锁屏壁纸、桌面壁纸的即时效果。

---

### [x] PB-001：神笔马良入口回退链路不完整（可进入但难退出）

差异说明：

- iOS 从 Home 进入后，`showPersonaBuilderApp` 只有置 `true`，未见关闭回写。
- Android 有 `onClose -> showPersonaBuilderApp = false`。

证据：

- iOS 打开入口：`src/platforms/ios/WangWangPhone/Views/HomeScreen.swift:1329`
- iOS 展示层：`src/platforms/ios/WangWangPhone/Views/HomeScreen.swift:1551`
- iOS 状态定义：`src/platforms/ios/WangWangPhone/Views/HomeScreen.swift:1208`
- Android 关闭回写：`src/platforms/android/app/src/main/java/com/WangWangPhone/ui/HomeScreen.kt:935`

TODO：

- [x] `PersonaCardListView` 增加 `onClose`
- [x] HomeScreen 接回 `showPersonaBuilderApp = false`
- [x] 列表页增加可见"返回/关闭"操作（与 Android 对齐）

验收标准：

- iOS 用户可从神笔马良任意一级页面稳定返回 Home。

---

### [x] PB-002：神笔马良缺少"分支"能力

差异说明：

- Android 长按消息操作含“复制/编辑/回溯/分支”。
- iOS 当前只有“复制/编辑/回溯”。

证据：

- Android 创建分支逻辑：`src/platforms/android/app/src/main/java/com/WangWangPhone/ui/PersonaBuilderApp.kt:387`
- Android 分支成功提示：`src/platforms/android/app/src/main/java/com/WangWangPhone/ui/PersonaBuilderApp.kt:393`
- iOS 操作菜单仅三项：`src/platforms/ios/WangWangPhone/Views/PersonaBuilderChatView.swift:31`

TODO：

- [x] iOS 增加"分支"动作
- [x] 分支创建新卡并自动跳转新卡聊天

验收标准：

- iOS 长按消息可创建分支并继续对话，行为与 Android 一致。

---

### [x] RESET-001：iOS Reset 默认设置漏清理自定义图标

差异说明：

- Android reset 会清理 `IconCustomization`。
- iOS reset 当前未清理该项。

证据：

- Android 清理图标：`src/platforms/android/app/src/main/java/com/WangWangPhone/core/LayoutDbHelper.kt:201`
- iOS reset 实现：`src/platforms/ios/WangWangPhone/Core/LayoutManager.swift:150`

TODO：

- [x] iOS `LayoutManager.resetToDefaultSettings()` 增加 `clearAllCustomIcons()`

验收标准：

- iOS Reset 后自定义图标全部恢复默认。

---

## P1（建议修，减少后续返工）

### [x] PB-003：iOS 旧版神笔马良页面未接线（冗余实现）

差异说明：

- `PersonaBuilderAppView` 在主流程无入口，仅用于预览。

证据：

- 旧页定义：`src/platforms/ios/WangWangPhone/Views/PersonaBuilderApp.swift:3`
- 仅 Preview 引用：`src/platforms/ios/WangWangPhone/Views/PersonaBuilderApp.swift:214`

TODO：

- [x] 删除旧页面，或明确用途并接线

---

### [x] PB-004：iOS 人设卡 DB 能力比 Android 少（updateCard/clearMessages）

差异说明：

- Android 有 `updateCard`、`clearMessages`。
- iOS 当前无对应方法。

证据：

- Android：`src/platforms/android/app/src/main/java/com/WangWangPhone/core/PersonaCardDbHelper.kt:146`
- Android：`src/platforms/android/app/src/main/java/com/WangWangPhone/core/PersonaCardDbHelper.kt:240`
- iOS 当前方法集：`src/platforms/ios/WangWangPhone/Core/PersonaCardDbHelper.swift:70`

TODO：

- [x] 评估并补齐 iOS DB API（至少保持与 Android 接口等价）

---

### [x] CHAT-007：iOS 联系人详情页仍以静态联系人为主，展示字段为占位值

差异说明：

- iOS `ContactDetailView` 联系人来自 `wxStarred + wxContactList`，微信号/地区是硬编码占位。
- Android 联系人详情来自数据库真实字段。

证据：

- iOS 静态联系人来源：`src/platforms/ios/WangWangPhone/Views/ChatApp.swift:1035`
- iOS 发消息 ID 映射依赖静态会话：`src/platforms/ios/WangWangPhone/Views/ChatApp.swift:1089`
- Android 联系人详情查库：`src/platforms/android/app/src/main/java/com/WangWangPhone/ui/ChatApp.kt:1921`

TODO：

- [x] iOS 联系人详情全面改为数据库字段

---

### [x] DISPLAY-002：iOS 壁纸刷新通知链路不完整

差异说明：

- `LockScreen` 监听 `WallpaperChanged` 通知。
- 代码中未找到任何发送该通知的地方。

证据：

- 监听通知：`src/platforms/ios/WangWangPhone/Views/LockScreen.swift:62`

TODO：

- [x] 在 iOS 壁纸保存成功后发送通知
- [x] 或统一改为状态回调刷新（与 HomeScreen 一致）

---

### [x] RESET-002：iOS Reset 后 Home 的 customIcons 刷新链路偏弱

差异说明：

- iOS 设置页关闭时只触发 `loadLayout()` 与 `homeWallpaper` 刷新。
- 未显式触发 `loadCustomIcons()`，与 reset 后图标同步存在风险。

证据：

- 设置页 onDisappear 刷新逻辑：`src/platforms/ios/WangWangPhone/Views/HomeScreen.swift:1484`

TODO：

- [x] reset 成功后强制 reload `customIcons`

---

### [x] PB-005：iOS 人设卡列表主页面无返回按钮（交互与 Android 不一致）

差异说明：

- Android 列表页带 `BackHandler + TopAppBar navigationIcon`。
- iOS 主列表页只有右上角 `+`，没有返回入口。

证据：

- Android 返回：`src/platforms/android/app/src/main/java/com/WangWangPhone/ui/PersonaCardListScreen.kt:34`
- iOS 顶栏仅 trailing：`src/platforms/ios/WangWangPhone/Views/PersonaCardListView.swift:56`

TODO：

- [x] iOS 列表页补 left `关闭/返回`

---

### [x] ACTIVATION-001：iOS 激活页缺少"复制机器码"和"粘贴激活码"便捷按钮

差异说明：

- Android `ActivationScreen` 有"复制机器码"按钮（绿色）和"粘贴激活码"按钮（紫色），方便用户操作。
- iOS `ActivationView` 只有机器码展示和激活码输入框，缺少这两个便捷操作按钮。

证据：

- Android 复制机器码按钮：`src/platforms/android/app/src/main/java/com/WangWangPhone/ui/HomeScreen.kt:767`
- Android 粘贴激活码按钮：`src/platforms/android/app/src/main/java/com/WangWangPhone/ui/HomeScreen.kt:785`
- iOS 激活页无对应按钮：`src/platforms/ios/WangWangPhone/Views/HomeScreen.swift:955`

TODO：

- [ ] iOS `ActivationView` 增加"复制机器码"按钮（机器码展示框下方）
- [ ] iOS `ActivationView` 增加"粘贴激活码"按钮（激活码输入框下方）

验收标准：

- iOS 激活页具备与 Android 一致的"复制机器码"和"粘贴激活码"快捷操作。

---

### [x] PB-006：iOS 神笔马良分支创建后未直接跳转到新卡（行为与 Android 不一致）

差异说明：

- Android 分支创建后调用 `onOpenCard(newCardId)` 直接跳转到新分支卡的聊天页面。
- iOS 分支创建后调用 `onBack()` 返回列表页，用户需要手动找到并点击新分支卡。

证据：

- Android 分支跳转：`src/platforms/android/app/src/main/java/com/WangWangPhone/ui/PersonaBuilderApp.kt:394`
- iOS 分支回退到列表：`src/platforms/ios/WangWangPhone/Views/PersonaBuilderChatView.swift:466`

TODO：

- [x] iOS `PersonaBuilderChatView` 增加 `onOpenCard` 回调参数
- [x] 分支创建成功后调用 `onOpenCard(newCardId)` 直接跳转

验收标准：

- iOS 创建分支后自动跳转到新分支卡的聊天页面，行为与 Android 一致。

---

## P2（治理与清理）

### [x] REPO-001：iOS 构建产物进入仓库

差异说明：

- iOS 源码目录包含 `BuildLog` 与 `xcresult` 产物，不应纳入功能源码。

证据：

- 示例文件：`src/platforms/ios/BuildLog/xcodebuild.log`

TODO：

- [x] 清理已入库构建产物
- [x] 补齐 `.gitignore`

---

### [x] REPO-002：Chat 静态样例数据应下沉到测试/预览域

差异说明：

- iOS `ChatApp.swift` 内保留大量 `wx...` 样例常量，生产逻辑与示例数据耦合高。

证据：

- 静态会话声明：`src/platforms/ios/WangWangPhone/Views/ChatApp.swift:188`
- 静态通讯录声明：`src/platforms/ios/WangWangPhone/Views/ChatApp.swift:214`

TODO：

- [x] 迁移样例数据到 Preview/Test 层
- [x] 业务界面仅依赖仓储/数据库数据源

---

## 4. iOS“多余功能/冗余内容”清单（显式列出）

1. 未接线旧页面：`src/platforms/ios/WangWangPhone/Views/PersonaBuilderApp.swift`  
2. 构建日志产物：`src/platforms/ios/BuildLog/*`  
3. Chat 主流程中硬编码静态数据：`wxConversations/wxContactList/wxMoments`

---

## 5. 建议修复顺序（直接照这个顺序做）

1. ~~`CHAT-001` ~ `CHAT-007`~~（已完成）
2. ~~`PB-001` ~ `PB-005`~~（已完成）
3. ~~`DISPLAY-001` ~ `DISPLAY-002`~~（已完成）
4. ~~`RESET-001` ~ `RESET-002`~~（已完成）
5. ~~`REPO-001` ~ `REPO-002`~~（已完成）
6. `ACTIVATION-001`（新增）
7. `PB-006`（新增）

---

## 6. 修复打卡区（执行时填写）

| 任务ID | 状态 | 提交哈希 | 备注 |
| --- | --- | --- | --- |
| CHAT-001 | ✅ 已完成 |  | 已有conversations表+完整CRUD+迁移兼容 |
| CHAT-002 | ✅ 已完成 |  | 已有add-contact/edit-contact/chat-settings/select-api-preset路由 |
| CHAT-003 | ✅ 已完成 |  | MessagesTabView/ContactsTabView已改为数据库驱动 |
| CHAT-004 | ✅ 已完成 |  | ContactInfo已含wechatId/region/persona/avatarFileName+updateContact |
| CHAT-005 | ✅ 已完成 |  | 会话创建保存aiRoleId+userPersonaId，发消息读取双人设 |
| CHAT-006 | ✅ 已完成 |  | 会话级apiPresetId+聊天设置切换预设 |
| DISPLAY-001 | ✅ 已完成 |  | 锁屏/桌面壁纸选择+WallpaperChanged通知 |
| PB-001 | ✅ 已完成 |  | PersonaCardListView增加onClose+HomeScreen接回 |
| PB-002 | ✅ 已完成 |  | 长按消息增加"分支"动作+创建新卡+toast提示 |
| RESET-001 | ✅ 已完成 |  | resetToDefaultSettings增加clearAllCustomIcons |
| PB-003 | ✅ 已完成 |  | 已删除旧版PersonaBuilderApp.swift |
| PB-004 | ✅ 已完成 |  | 补齐updateCard+clearMessages |
| CHAT-007 | ✅ 已完成 |  | ContactDetailView已改为数据库字段驱动 |
| DISPLAY-002 | ✅ 已完成 |  | 壁纸保存后发送WallpaperChanged通知 |
| RESET-002 | ✅ 已完成 |  | 设置页onDisappear增加loadCustomIcons |
| PB-005 | ✅ 已完成 |  | PersonaCardListView增加左上角返回按钮 |
| REPO-001 | ✅ 已完成 |  | 清理BuildLog+补齐.gitignore |
| REPO-002 | ✅ 已完成 |  | 业务界面已改为数据库驱动，静态数据保留为预览用途 |
| ACTIVATION-001 | ✅ 已完成 |  | iOS激活页增加复制机器码+粘贴激活码按钮 |
| PB-006 | ✅ 已完成 |  | PersonaBuilderChatView增加onOpenCard回调+分支跳转 |
