# 安卓平台功能现状分析文档（截至 2026-03-05）

## 1. 文档目标与范围

本文档用于系统盘点当前仓库中 **Android 端已实现功能**、**能力边界**、**数据结构** 与 **完成度现状**，便于后续做：

- MVP 收口
- 功能对齐（未来 iOS / Web / Windows）
- 测试与发布规划
- 研发任务拆解

> 说明：本文仅基于 `src/platforms/android/app/src/main` 及相关配置文件的当前实现做静态分析，不包含线上运行数据。

---

## 2. 技术基线概览

### 2.1 工程与构建

- 平台：Android（Kotlin + Jetpack Compose）
- `compileSdk`：36
- `targetSdk`：36
- `minSdk`：31
- 版本来源：根目录 `version.properties`
  - 当前 `VERSION_NAME=2.2.3`
  - 当前 `VERSION_CODE=16`
- 启用 Compose：`buildFeatures.compose = true`
- 关键依赖：
  - Compose BOM + Material3
  - CameraX（camera-core/camera2/lifecycle/video/view/extensions）
  - OkHttp（网络请求）
  - org.json（JSON 解析）
  - commonmark（Markdown 渲染）

### 2.2 权限与系统能力

Manifest 里已声明：

- `INTERNET`（联网能力）
- `CAMERA`（拍照能力）
- `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_DATA_SYNC`（后台保活请求）
- `WRITE_EXTERNAL_STORAGE`（仅 `maxSdkVersion=28`）

并声明：

- `ApiKeepAliveService` 前台服务（数据同步类型）
- `MainActivity` 为唯一启动 Activity

---

## 3. 产品主框架（系统壳层）

### 3.1 启动流程

启动链路如下：

1. `MainActivity` 初始化授权管理 `LicenseManager.initialize()`
2. 初始化请求保活 `ApiRequestKeepAlive.initialize()`
3. 进入 `MainContainer`
4. 默认先显示锁屏，解锁后进入桌面

### 3.2 锁屏（LockScreen）

已实现能力：

- 显示时间与日期（秒级刷新）
- 支持自定义锁屏壁纸（读取本地文件）
- 上滑解锁手势（`swipeable`）
- 解锁后切换到桌面页

### 3.3 桌面（HomeScreen）

当前是安卓端最核心、实现最完整的模块之一。

#### 3.3.1 桌面布局体系

- 网格规格：`4 x 7`（28 格）
- 多页桌面（`HorizontalPager`）
- Dock 栏（最多 4 个应用）
- 默认组件（Widgets）：
  - 时钟组件
  - 天气组件
  - 吧唧组件（支持自定义图片）
- 默认应用图标数量：31 个（包含大量占位应用）

#### 3.3.2 编辑模式与拖拽

已实现完整的长按编辑交互：

- 长按进入编辑态（图标抖动）
- 网格内拖拽排序
- Dock ↔ 网格互拖
- 同尺寸交换、Widget 与多个 App 的冲突处理
- 边缘自动翻页拖拽
- 拖拽高亮落位区域
- 布局实时持久化到数据库

#### 3.3.3 启动动画与应用保活

- 冷启动：图标放大过渡动画 + 延迟打开
- 热启动：已预热应用可直接进入
- 关闭应用后可设置 keep-alive 图层，提升二次打开体感

#### 3.3.4 激活拦截策略

桌面点击应用时存在授权拦截：

- `settings`（设置）可直接进入
- 其他可启动应用在未激活时会弹激活引导

#### 3.3.5 设置中心（Settings）

桌面内置设置页包含：

- 激活与授权入口
- 显示设置入口
- API 预设管理入口（聊天/生图/语音）
- 恢复默认设置（含二次确认）

### 3.4 显示个性化能力

从设置页可进入显示相关能力：

- 锁屏壁纸更换
- 桌面壁纸更换
- 桌面图标自定义（按应用级别替换图片）
- 吧唧组件图片自定义与恢复默认

### 3.5 恢复默认设置（全量重置）

一键重置已打通，重置范围包括：

- 桌面布局
- 壁纸
- 天气缓存
- 用户资料（昵称/签名/头像/封面）
- 所有自定义图标

---

## 4. 子应用功能现状

### 4.1 聊天应用（ChatApp，类微信）

这是当前安卓端第二大功能模块，完成度较高。

#### 4.1.1 主体结构

四大 Tab：

- 消息
- 通讯录
- 发现（朋友圈）
- 我

并支持多级页面流转：

- 会话详情
- 联系人详情
- 添加联系人
- 编辑联系人
- 服务页
- 发起聊天联系人选择页
- API 预设选择页
- 聊天设置页

#### 4.1.2 会话与消息

已实现：

- 会话列表（最近消息、时间、未读）
- 未读角标（普通/99+/点/新）
- 聊天气泡双向渲染（用户/对方）
- 输入框与发送动作
- 消息列表自动滚动到底部
- 从数据库加载历史消息

#### 4.1.3 角色扮演聊天链路

针对 `contact_*` 会话已打通 AI 对话：

- 发送用户消息后写入消息库
- 按会话绑定 API 预设
- 读取 AI 角色人设与用户人设
- 调用 `LlmApiService.sendChatRequest`
- 返回后写入 AI 回复到消息库
- 未配置 API 预设时给出明确提示

#### 4.1.4 联系人与人设数据

已实现完整联系人 CRUD：

- 新增联系人（昵称、微信号、地区、persona、头像）
- 编辑联系人
- 删除联系人
- 按拼音首字母分组显示
- 联系人详情页可一键发消息

#### 4.1.5 朋友圈与“我”页

已实现：

- 用户头像/封面更换
- 昵称/签名编辑
- 朋友圈卡片流（当前基于 mock 数据）
- 我页菜单与服务入口

#### 4.1.6 聊天设置

单会话可配置：

- API 预设切换
- 消息免打扰（静音开关）

---

### 4.2 神笔马良（PersonaBuilder）

这是一个“人设构建+对话”工作台，完成度较高，且具备差异化能力。

#### 4.2.1 人设卡管理

已实现：

- 人设卡列表
- 新建人设卡（绑定 API 预设）
- 编辑人设卡（名称/API 预设）
- 删除人设卡（含对话级联删除）

#### 4.2.2 对话能力

已实现：

- 进入人设卡后加载历史消息
- 首次自动注入引导欢迎语
- 消息发送与落库
- 流式回复（`sendChatRequestStream`）
- Markdown 渲染（CommonMark + HtmlCompat）
- 思考链 `<think>` 与正文分离展示

#### 4.2.3 高级对话操作

对单条消息长按支持：

- 复制
- 编辑（改写历史消息）
- 回溯（删除某节点后的消息并重新生成）
- 分支（从历史节点克隆新卡继续）

并且：

- 首次回溯会弹风险提示
- 对话状态支持“思考中”加载态

#### 4.2.4 Prompt 资产

- 使用内置提示词资产：`assets/prompt/角色人设设计.txt`
- 对话历史会做思考内容清洗后再参与上下文，避免污染

---

### 4.3 天气应用（WeatherApp）

天气模块在“桌面组件 + 独立天气应用”两条链路都已落地。

#### 4.3.1 数据获取与缓存策略

已实现：

- 位置获取：`myip.ipip.net` 解析城市
- 天气数据：`wttr.in/{city}?format=j1`
- 中文描述本地化与图标映射
- 失败重试 + 内存缓存兜底
- SQLite 日缓存（按城市+日期）
- 过期缓存清理

#### 4.3.2 独立天气应用 UI

已实现：

- 当前城市、温度、天气、温度范围
- 强制刷新按钮
- 小时级预报横向滚动
- 未来多日预报（温差条）
- 详情卡片（体感、湿度、能见度、UV、风速、气压）

#### 4.3.3 桌面天气组件

桌面内的时钟/天气组件会复用缓存能力，支持：

- 首屏秒开（优先读缓存）
- 无效缓存自动重拉

---

### 4.4 浏览器（BrowserApp）

基于 Android `WebView` 已实现：

- 地址栏输入（URL 或搜索词）
- 默认搜索引擎跳转（百度）
- 页面标题与加载进度
- 前进/后退/刷新/主页
- 系统分享当前 URL
- Back 键优先网页回退，否则退出应用

---

### 4.5 相机（CameraApp）

基于 CameraX 已实现：

- 首次申请相机权限
- 前后摄切换
- 预览流渲染
- 快门动画与闪光白屏反馈
- 拍照保存到系统相册（`Pictures/WangWangPhone`）
- 顶栏拍照计数显示

说明：

- 模式栏虽展示“延时/慢动作/视频/照片/人像/全景”，当前实际落地的是照片主流程，其他模式偏 UI 占位。

---

### 4.6 日历（CalendarApp）

已实现：

- 月视图网格
- 上下月切换与“今天”快捷回到当日
- 日期选中态
- 事件点标记
- 当日事件列表（全天/定时）

说明：

- 当前事件数据为示例数据（内存 mock），未接入持久化与系统日历。

---

### 4.7 备忘录（NotesApp）

已实现：

- 备忘录列表
- 搜索过滤
- 新建/编辑/删除
- 详情编辑页
- 返回时自动保存逻辑

说明：

- 当前数据驻留内存状态，未落库；重启应用后会恢复初始示例数据。

---

### 4.8 计算器（CalculatorApp）

已实现：

- 基础运算（加减乘除）
- 百分比、正负号、小数点
- 清空与结果格式化
- iOS 风格按键布局与主题

---

## 5. API 预设系统（跨聊天/生图/语音）

这是全局关键能力，已经具备较完整配置链路。

### 5.1 能力概览

- 预设分类：`chat` / `image` / `voice`
- 提供商：
  - 聊天/生图：OpenAI、Gemini
  - 语音：Minimax（固定）
- 预设字段：
  - 名称、provider、apiKey、baseUrl、model、extraParams

### 5.2 编辑器能力

已实现：

- 新增 / 编辑 / 删除
- 拉取可用模型列表（按 provider）
- 连接测试（并展示返回内容）
- 高级参数开关化配置：
  - stream
  - temperature
  - max_tokens
  - top_p
  - thinking_level
  - enable_thinking
  - top_k
  - thinking_budget
  - thinking_effort

并支持将参数序列化到 `extraParams` JSON 字符串。

---

## 6. 授权与安全能力

### 6.1 License 机制

当前 Android 端已实现本地授权闭环：

- 机器码生成（基于 Android ID）
- 激活码校验（`WANGWANG-` 格式）
- RSA 签名验证（公钥编译注入 `BuildConfig.RSA_PUBLIC_KEY`）
- 到期检查
- 本地持久化（`wangwang_license.db`）
- 启动时强校验与失效清理

### 6.2 与 C++ Core 的关系

- `LicenseManager` 内有 JNI 对接位与注释
- 当前实际流程仍是 Kotlin + SQLite 本地实现
- 说明“跨平台核心对齐”已留扩展位，但尚未打通 Android JNI 实接

---

## 7. 数据持久化总览（SQLite）

当前 Android 端数据库拆分清晰，按职责隔离：

- `api_preset.db`：API 预设
- `wangwang_chat.db`：会话与消息
- `wangwang_contacts.db`：联系人与头像
- `persona_cards.db`：人设卡与人设消息
- `wangwang_layout.db`：桌面布局
- `wangwang_icon_customization.db`：图标替换
- `wangwang_wallpaper.db`：壁纸映射
- `wangwang_user_profile.db`：用户昵称/签名/头像/封面
- `wangwang_weather_cache.db`：天气缓存 + 手动位置 + 定位缓存
- `wangwang_license.db`：激活信息

文件存储目录也已做模块化：

- 壁纸：`files/wallpapers`
- 图标：`files/custom_icons`
- 联系人头像：`files/contact_images`
- 用户资料图：`files/profile_images`

---

## 8. 功能完成度分层

### 8.1 已可用于 MVP 主链路（可演示）

- 锁屏 → 桌面 → 启动应用流程
- 桌面编辑与持久化
- 设置、显示个性化、API 预设管理
- 授权激活流程
- 聊天（联系人+对话+LLM）
- 人设构建（流式、回溯、分支）
- 天气（组件+独立页）
- 浏览器、相机、计算器

### 8.2 半成品/演示态功能

- 日历（示例事件，未落库）
- 备忘录（内存态，未落库）
- 朋友圈动态（mock 数据）
- 服务页（展示为主）
- 相机多模式（大多为 UI 占位）

### 8.3 明确占位（图标存在但未落地页面）

桌面中大批图标为视觉占位，仅少量在 `launchableAppIds` 中可启动。
目前可启动应用仅 9 个：

- settings
- chat
- safari
- calculator
- weather_app
- calendar
- camera
- notes
- persona_builder

其余如 phone/music/photos/video/map/mail/files/wallet/news/stocks 等尚未接入独立页面逻辑。

---

## 9. 当前架构优点与主要风险

### 9.1 优点

- 模块边界清晰（壳层 / 子应用 / 数据层）
- 桌面交互深度高，具备产品辨识度
- 数据落库覆盖面广，便于后续迁移
- API 预设系统可复用到多个 AI 功能
- 人设构建模块具备较强差异化能力

### 9.2 风险与技术债

- 多处功能仍是 mock/内存态，重启后状态不可持续
- C++ 核心 JNI 通路未接入，跨端一致性依赖人工维护
- 目前未看到自动化测试（单测/UI 测试）覆盖
- 资源文案存在编码与可读性问题风险（后续需要统一清理）
- 图标占位多，若作为“可用产品”对外展示容易造成预期落差

---

## 10. 建议的下一步（安卓优先）

建议按“可用性优先”推进：

1. 先补齐高频入口应用（电话、相册、文件、钱包等至少做基础页面）
2. 把日历、备忘录从内存态改为落库持久化
3. 清理文案与编码问题，建立统一文案资源规范
4. 建最小化自动化回归（桌面拖拽、聊天发送、API 测试、授权校验）
5. 逐步将授权和关键业务逻辑切换到 C++ Core + JNI

---

## 11. 关键代码入口（便于继续阅读）

- 应用入口：`src/platforms/android/app/src/main/java/com/WangWangPhone/MainActivity.kt`
- 系统容器：`src/platforms/android/app/src/main/java/com/WangWangPhone/ui/MainContainer.kt`
- 锁屏：`src/platforms/android/app/src/main/java/com/WangWangPhone/ui/LockScreen.kt`
- 桌面壳层：`src/platforms/android/app/src/main/java/com/WangWangPhone/ui/HomeScreen.kt`
- 聊天应用：`src/platforms/android/app/src/main/java/com/WangWangPhone/ui/ChatApp.kt`
- 人设构建：`src/platforms/android/app/src/main/java/com/WangWangPhone/ui/PersonaBuilderApp.kt`
- 天气应用：`src/platforms/android/app/src/main/java/com/WangWangPhone/ui/WeatherApp.kt`
- API 预设：`src/platforms/android/app/src/main/java/com/WangWangPhone/ui/ApiPresetsScreen.kt`
- 授权核心：`src/platforms/android/app/src/main/java/com/WangWangPhone/core/LicenseManager.kt`
- 网络层：`src/platforms/android/app/src/main/java/com/WangWangPhone/core/LlmApiService.kt`
