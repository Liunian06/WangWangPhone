# UI 组件技术拆解 (UI Component Breakdown)

本文档详述了如何在 Android (Jetpack Compose) 和 iOS (SwiftUI) 上实现“汪汪机”的核心视觉特征。

## 1. 虚拟 iOS Dock 栏 (The Glass Dock)

**挑战：** 实时高斯模糊 (Real-time Gaussian Blur) 且不影响性能。

### Android (Jetpack Compose)
*   **方案:** `RenderEffect` (Android 12+) 或 AGSL Shader。
*   **实现细节:**
    *   使用 `Modifier.graphicsLayer { renderEffect = RenderEffect.createBlurEffect(...) }` 作用于 Dock 背景层。
    *   **Fallback:** 对于 Android 12 以下，使用 `Toolkit` 库或预模糊的 Bitmap 方案（但在本项目中我们优先保证高性能设备的实时渲染）。
    *   **裁剪:** 使用 `Modifier.clip(RoundedCornerShape(...))` 确保模糊只限制在 Dock 区域。

### iOS (SwiftUI)
*   **方案:** `UIVisualEffectView` (Via `UIViewRepresentable`)。
*   **实现细节:**
    *   SwiftUI 原生的 `.background(.ultraThinMaterial)` 往往不够通透，无法完美复刻 iOS 16/17 的 Dock 质感。
    *   我们将封装一个 `GlassDockView` 结构体，内部桥接 `UIVisualEffectView(effect: UIBlurEffect(style: .systemUltraThinMaterial))`。

## 2. 仿微信聊天气泡 (WeChat Bubble)

**挑战：** 像素级还原气泡的“尖角 (Tail)”与圆角的平滑融合，以及 9-patch 的拉伸逻辑。

### Android (Jetpack Compose)
*   **方案:** 自定义 `Shape` + `Path`。
*   **代码思路:**
    ```kotlin
    class WeChatBubbleShape(val isMine: Boolean) : Shape {
        override fun createOutline(...) : Outline {
            val path = Path().apply {
                // 1. 绘制圆角矩形主体
                // 2. 使用 cubicTo 或 lineTo 绘制尖角 (Tail)
                // 3. 确保尖角与主体连接处无锯齿
            }
            return Outline.Generic(path)
        }
    }
    ```
*   **应用:** `Surface(shape = WeChatBubbleShape(true), color = WeChatGreen)`。

### iOS (SwiftUI)
*   **方案:** 实现 `Shape` 协议。
*   **代码思路:**
    ```swift
    struct WeChatBubbleShape: Shape {
        var isMine: Bool
        func path(in rect: CGRect) -> Path {
            var path = Path()
            // 使用 addArc 和 addLine 绘制精确路径
            // 重点复刻微信气泡尖角的贝塞尔曲线弧度
            return path
        }
    }
    ```

## 3. 虚拟桌面应用网格 (Virtual App Grid)

**挑战：** 解决与宿主机的滑动手势冲突。

### Android
*   **布局:** `LazyVerticalGrid`。
*   **手势:** 使用 `PointerInputScope` 捕获原始触摸事件。
*   **防冲突:** 在 Activity 层级重写 `onUserLeaveHint` 或使用 `WindowInsetsController` 隐藏系统导航栏，并实现边缘手势拦截 (Edge-to-Edge)。

### iOS
*   **布局:** `LazyVGrid`。
*   **手势:** 隐藏 Home Indicator (`prefersHomeIndicatorAutoHidden`)，并使用 `DragGesture` 覆盖系统手势区域（需谨慎处理 Review 风险，但本项目为未签名分发，限制较少）。

## 4. Live2D 动态壁纸集成

*   **Android:** `AndroidView` 包裹 `GLSurfaceView`，渲染层级置于 Compose UI 最底层 (`Z-Index: -1`)。
*   **iOS:** `MetalKit` (`MTKView`) 通过 `UIViewRepresentable` 嵌入 SwiftUI `ZStack` 的最底层。