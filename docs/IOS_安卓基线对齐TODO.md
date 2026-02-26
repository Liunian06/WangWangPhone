# iOS 对齐安卓基线 TODO（审阅版）

更新时间：2026-02-26  
对齐基线：`Android`（`src/platforms/android`）  
对齐目标：`iOS`（`src/platforms/ios`）

## 1. 分析范围与方法

本次对比覆盖了 Android / iOS 两端的：

- UI 入口与主流程（Home、Chat、PersonaBuilder、Settings、各 App 子页面）
- Core 数据层与服务层（Chat、Contact、Layout、Wallpaper、API Preset、License、Llm）
- 页面路由与状态机（尤其是 Chat、PersonaBuilder）

采用的检查方法：

- 同名/同职责文件映射比对
- 关键状态变量与路由 case 对比
- Core 方法签名与表结构能力对比
- 功能入口是否“可达、可回退、可持久化”的链路检查

---

## 2. 进度概览（以安卓为 100% 基准）

说明：以下百分比是“功能对齐度估算”，不是代码行数比。

| 模块 | Android 基线状态 | iOS 当前状态 | 对齐度估算 | 结论 |
| --- | --- | --- | --- | --- |
| 锁屏/主容器（Main + Lock） | 完整 | 基本完整 | 90% | 主流程可用 |
| 桌面与布局编辑（Home） | 完整 | 基本完整 | 80% | 显示设置有关键缺口 |
| API 预设（设置） | 完整 | 基本完整 | 85% | 主能力齐全 |
| 聊天系统（Chat） | 完整（会话/联系人/设置/预设绑定） | 明显简化 | 45% | 当前最大差距 |
| 人设构建（PersonaBuilder） | 完整 | 基本完整 | 80% | 入口回退链路有缺陷 |
| 天气/浏览器/相机/日历/备忘录等子应用 | 完整 | 大体齐全 | 85% | 细节可后补 |
| Core 持久化与重置 | 完整 | 中等对齐 | 75% | Reset 逻辑有漏项 |

辅助统计（仅作参考）：

- Android UI 行数：`8813`
- iOS View 行数：`6761`
- Android Core 行数：`3614`
- iOS Core 行数：`3121`

---

## 3. iOS 缺失功能（相对安卓）TODO

以下按优先级排序，`P0` 建议优先处理。

## P0-1 聊天数据模型与主流程对齐（最高优先级）

问题摘要：

- Android 聊天是“会话模型”（conversation + message），iOS 当前仅按 `contact_id` 存消息，缺少会话层。
- iOS 聊天列表和通讯录列表仍使用硬编码 mock 数据，未完全接入数据库。

证据定位：

- Android 会话能力：`src/platforms/android/app/src/main/java/com/WangWangPhone/core/ChatDbHelper.kt:119`
- Android 会话查询：`src/platforms/android/app/src/main/java/com/WangWangPhone/core/ChatDbHelper.kt:142`
- Android 会话设置（静音/预设）：`src/platforms/android/app/src/main/java/com/WangWangPhone/core/ChatDbHelper.kt:323`
- Android 聊天页路由包含设置/新增/编辑：`src/platforms/android/app/src/main/java/com/WangWangPhone/ui/ChatApp.kt:646`
- iOS 仅消息表：`src/platforms/ios/WangWangPhone/Core/ChatDbHelper.swift:43`
- iOS 聊天列表使用 mock：`src/platforms/ios/WangWangPhone/Views/ChatApp.swift:188`
- iOS 聊天列表渲染 mock：`src/platforms/ios/WangWangPhone/Views/ChatApp.swift:500`
- iOS 选人后直接 `chatId = contact1Id`：`src/platforms/ios/WangWangPhone/Views/ChatApp.swift:321`

TODO：

- [ ] iOS `ChatDbHelper` 新增会话表与会话 API（创建、查询、删除、静音、绑定 API 预设）
- [ ] iOS `ChatAppView` 路由补齐：`add-contact`、`edit-contact`、`chat-settings`、`select-api-preset`
- [ ] iOS 消息页、通讯录页改为数据库驱动，移除对 `wxConversations/wxContactList` 的业务依赖
- [ ] 新建聊天必须调用“创建会话”，禁止直接复用联系人 ID 作为会话 ID

验收标准：

- iOS 能完整跑通“选两位联系人 -> 选 API 预设 -> 创建会话 -> 聊天 -> 改会话预设/静音 -> 返回列表”的全链路。

## P0-2 通讯录能力对齐（新增/编辑/字段）

问题摘要：

- iOS `ContactDbHelper` 字段偏少，缺 Android 的 `wechatId/region/updatedAt` 等信息。
- iOS 缺少联系人编辑入口与页面能力。

证据定位：

- Android 联系人含更新：`src/platforms/android/app/src/main/java/com/WangWangPhone/core/ContactDbHelper.kt:223`
- iOS 联系人无更新方法：`src/platforms/ios/WangWangPhone/Core/ContactDbHelper.swift:92`
- Android 聊天页含新增/编辑联系人页面：`src/platforms/android/app/src/main/java/com/WangWangPhone/ui/ChatApp.kt:2075`
- iOS 聊天页未提供对应页面：`src/platforms/ios/WangWangPhone/Views/ChatApp.swift:293`

TODO：

- [ ] iOS 联系人表结构补齐安卓核心字段
- [ ] iOS 补 `updateContact` 能力
- [ ] iOS 聊天模块补联系人新增/编辑 UI 与路由

## P0-3 显示设置缺口：壁纸设置未实现

问题摘要：

- Android 已支持锁屏壁纸与桌面壁纸选择。
- iOS 显示设置中仍是“待实现”占位文案，实际不可设置。

证据定位：

- Android 壁纸设置入口与实现：`src/platforms/android/app/src/main/java/com/WangWangPhone/ui/HomeScreen.kt:1891`
- iOS 占位文案：`src/platforms/ios/WangWangPhone/Views/HomeScreen.swift:1067`

TODO：

- [ ] iOS `DisplaySettingsView` 实装锁屏/桌面壁纸选择
- [ ] 调用 `WallpaperManager.saveWallpaper(type:fileName:)`，并广播刷新锁屏和桌面

验收标准：

- iOS 可分别设置锁屏壁纸、桌面壁纸，锁屏与桌面实时生效。

## P0-4 人设入口回退链路不完整（用户可能无法退出）

问题摘要：

- iOS 从桌面打开人设应用后，`showPersonaBuilderApp` 只有 `true` 赋值，未看到任何 `false` 关闭路径。
- Android 存在明确 onClose 回传。

证据定位：

- iOS 打开人设：`src/platforms/ios/WangWangPhone/Views/HomeScreen.swift:1329`
- iOS 覆盖渲染人设：`src/platforms/ios/WangWangPhone/Views/HomeScreen.swift:1551`
- iOS 无 `showPersonaBuilderApp = false`：`src/platforms/ios/WangWangPhone/Views/HomeScreen.swift:1208`
- Android 关闭回写：`src/platforms/android/app/src/main/java/com/WangWangPhone/ui/HomeScreen.kt:935`

TODO：

- [ ] 给 `PersonaCardListView` 增加 `onClose` 回调
- [ ] 在 `HomeScreen` 中接回 `showPersonaBuilderApp = false`

## P0-5 恢复默认设置漏清理图标自定义

问题摘要：

- Android reset 会清理自定义图标。
- iOS reset 未调用 `IconCustomizationManager.clearAllCustomIcons()`。

证据定位：

- Android reset 清理图标：`src/platforms/android/app/src/main/java/com/WangWangPhone/core/LayoutDbHelper.kt:201`
- iOS reset 当前实现：`src/platforms/ios/WangWangPhone/Core/LayoutManager.swift:150`

TODO：

- [ ] iOS `LayoutManager.resetToDefaultSettings()` 增加清理自定义图标

---

## 4. iOS 多余功能/冗余实现 TODO

## P1-1 未接线旧版人设页面（建议清理或并入）

问题摘要：

- `PersonaBuilderAppView` 仅在自身预览中被引用，未进入实际业务入口。

证据定位：

- 定义与预览：`src/platforms/ios/WangWangPhone/Views/PersonaBuilderApp.swift:3`
- 项目内无其他引用：`src/platforms/ios/WangWangPhone/Views/PersonaBuilderApp.swift:214`

TODO：

- [ ] 方案 A：删除该旧页面，避免维护分叉
- [ ] 方案 B：若保留，明确挂载入口并与当前 `PersonaCardListView` 职责拆分

## P2-1 构建产物被纳入源码目录（建议清理）

问题摘要：

- iOS 目录存在 `BuildLog` 与 `xcresult` 产物，属于开发/构建临时文件，不应作为功能源码长期维护。

证据定位：

- 构建日志目录：`src/platforms/ios/BuildLog/xcodebuild.log`

TODO：

- [ ] 清理仓库中的构建产物
- [ ] 补充 `.gitignore` 规则，避免再次提交

---

## 5. 推荐实施顺序（安卓优先策略）

1. `P0-1` 聊天会话模型与路由补齐  
2. `P0-2` 通讯录字段与新增/编辑能力补齐  
3. `P0-4` 人设入口回退修复（避免用户卡死）  
4. `P0-3` 显示设置壁纸能力补齐  
5. `P0-5` reset 逻辑补齐  
6. `P1/P2` 冗余清理与结构收敛

---

## 6. 结论

当前 iOS 在“桌面与基础应用”层面对齐度较高，但在聊天与配置链路上仍明显落后 Android 基线。  
若以 MVP 一致性为目标，优先补齐 Chat + Contact + DisplaySettings + PersonaBuilder 回退链路，可快速把 iOS 对齐度拉升到可交付水平。
