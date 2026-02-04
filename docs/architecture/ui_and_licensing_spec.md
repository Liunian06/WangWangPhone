# 汪汪机 UI 适配与授权逻辑技术文档

## 1. UI 适配策略

为了确保在不同平台和设备上提供一致且原生的体验，项目采用了以下适配策略：

### 1.1 系统原生状态栏适配
- **Android**: 使用 Jetpack Compose 的 `statusBarsPadding()` 修饰符，确保设置和激活界面不会被系统状态栏遮挡。
- **iOS**: SwiftUI 默认处理安全区域（Safe Area），在自定义视图中通过 `NavigationView` 和 `List` 自动适配顶部状态栏和底部操作条。
- **Web**: 在覆盖层（Overlay）中使用 `padding-top: env(safe-area-inset-top)`，通过 CSS 环境变量适配移动端浏览器的刘海屏及状态栏区域。
- **Windows**: 采用固定标题栏设计，UI 布局基于 Grid 分层，确保内容在窗口范围内正确对齐。

### 1.2 深色/浅色模式适配
- **Android**: 通过 `isSystemInDarkTheme()` 实时检测系统主题，动态切换颜色变量（如背景色、卡片色和文字颜色）。
- **iOS**: 利用 `@Environment(\.colorScheme)` 自动响应系统主题切换，原生组件（如 `List`, `Form`）会自动适配颜色。
- **Web**: 使用媒体查询 `@media (prefers-color-scheme: dark)` 定义两套 CSS 变量（`--bg-color`, `--card-bg` 等），实现无缝主题切换。
- **Windows**: 在 `App.xaml` 中定义 `ThemeDictionaries`，使用 `DynamicResource` 绑定系统颜色，支持 Windows 11 原生深色模式切换。

## 2. 授权管理逻辑 (RSA Offline Validation)

### 2.1 核心流程
1. **设备识别**：App 获取当前设备的硬件唯一标识（Machine ID）。
2. **离线激活**：用户输入基于 RSA 私钥签名的激活码。
3. **本地验证**：C++ 核心层使用内置公钥解密并验证签名。

### 2.2 具体的授权校验逻辑 (C++)
在 [`LicenseManager.cpp`](../../src/core/src/LicenseManager.cpp) 中实现了具体的模拟校验逻辑：
- **格式校验**：激活码必须以 `WANGWANG-` 为前缀（模拟 RSA 签名后的 Base64 结构识别）。
- **Payload 解析**：模拟从解密后的数据中提取 `machine_id`, `expiration_time` 和 `type`。
- **一致性检查**：校验 Payload 中的 `machine_id` 是否与当前设备匹配。
- **有效期检查**：对比系统当前时间戳与 Payload 中的过期时间，确保授权未过期。

## 3. 密钥管理与签发工具

### 3.1 签发脚本 (Node.js)
位于 [`tools/license_tool.js`](../../tools/license_tool.js)，支持以下功能：
1. **生成密钥对**: `node license_tool.js gen` 会在 `tools/keys` 下生成 RSA 2048 位的公私钥。
2. **签发激活码**: `node license_tool.js sign <机器码>` 使用私钥对包含机器码的 Payload 进行签名。

### 3.2 集成流程
- **开发者**: 妥善保存 `private.pem`。
- **App**: 将 `public.pem` 或是生成的公钥字符串硬编码到 C++ 核心层的 `LicenseManager` 中用于离线验签。

## 3. 待完善项
- [ ] 集成真正的 OpenSSL 或 mbedTLS 库以替换模拟的解密逻辑。
- [ ] 在 Android/iOS 原生端实现更稳定的硬件 ID 获取方法（如 KeyStore/Keychain 存储 UUID）。
