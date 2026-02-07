
package com.WangWangPhone.ui

import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.WangWangPhone.core.LayoutDbHelper
import com.WangWangPhone.core.LayoutItem
import com.WangWangPhone.core.LicenseManager
import com.WangWangPhone.core.LicenseResult
import com.WangWangPhone.core.WallpaperDbHelper
import com.WangWangPhone.core.WallpaperType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

/**
 * 应用图标数据
 * @param id 唯一标识，用于布局持久化
 */
data class AppIcon(
    val id: String,
    val name: String,
    val icon: String,
    val color: Brush,
    val useImage: Boolean = false
)

/** 默认应用列表（初始顺序） */
fun getDefaultApps(isDark: Boolean): List<AppIcon> = listOf(
    AppIcon("phone", "电话", "📞", Brush.linearGradient(listOf(Color(0xFFFF9A9E), Color(0xFFFECFEF)))),
    AppIcon("chat", "聊天", "💬", Brush.linearGradient(listOf(Color(0xFF07C160), Color(0xFF06AD56)))),
    AppIcon("safari", "Safari", "🧭", Brush.linearGradient(listOf(Color(0xFF84FAB0), Color(0xFF8FD3F4)))),
    AppIcon("music", "音乐", "🎵", Brush.linearGradient(listOf(Color(0xFFF6D365), Color(0xFFFDA085)))),
    AppIcon("camera", "相机", "📷", Brush.linearGradient(listOf(Color.White, Color.LightGray))),
    AppIcon("calendar", "日历", "📅", Brush.linearGradient(listOf(Color.White, Color.LightGray))),
    AppIcon("settings", "设置", if (isDark) "ic_settings_dark" else "ic_settings_light", Brush.linearGradient(listOf(Color.White, Color.LightGray)), useImage = true),
    AppIcon("wangwang", "汪汪", "🐶", Brush.linearGradient(listOf(Color.White, Color.LightGray)))
)

// Mock Location & Weather Logic
suspend fun fetchLocation(): String {
    delay(500)
    return "广州"
}

data class WeatherInfo(
    val temp: String,
    val description: String,
    val icon: String,
    val range: String
)

suspend fun fetchWeather(city: String): WeatherInfo {
    delay(500)
    return WeatherInfo("25°", "多云", "⛅", "最高 29° 最低 21°")
}

@Composable
fun WidgetsSection(
    isEditMode: Boolean = false,
    widgetOrder: List<String> = listOf("clock", "weather"),
    onWidgetOrderChanged: (List<String>) -> Unit = {}
) {
    var city by remember { mutableStateOf("...") }
    var weather by remember { mutableStateOf<WeatherInfo?>(null) }

    // 小组件拖拽状态
    var widgetDragIndex by remember { mutableStateOf(-1) }
    var widgetDragOffsetX by remember { mutableStateOf(0f) }

    LaunchedEffect(Unit) {
        city = fetchLocation()
        if (city.isNotEmpty()) {
            weather = fetchWeather(city)
        }
    }

    val density = LocalDensity.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(15.dp)
    ) {
        widgetOrder.forEachIndexed { index, widgetId ->
            val isDragged = widgetDragIndex == index

            // 抖动动画（编辑模式）
            val wiggleTransition = rememberInfiniteTransition(label = "widget_wiggle_$index")
            val wiggleAngle by wiggleTransition.animateFloat(
                initialValue = if (index % 2 == 0) -1.5f else 1.5f,
                targetValue = if (index % 2 == 0) 1.5f else -1.5f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 200, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "widget_wiggle_angle_$index"
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .zIndex(if (isDragged) 10f else 0f)
                    .graphicsLayer {
                        if (isEditMode) {
                            rotationZ = if (isDragged) 0f else wiggleAngle
                        }
                        scaleX = if (isDragged) 1.05f else 1f
                        scaleY = if (isDragged) 1.05f else 1f
                        alpha = if (isDragged) 0.85f else 1f
                        if (isDragged) {
                            translationX = widgetDragOffsetX
                        }
                    }
                    .pointerInput(isEditMode, index) {
                        if (isEditMode) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    widgetDragIndex = index
                                    widgetDragOffsetX = 0f
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    widgetDragOffsetX += dragAmount.x
                                },
                                onDragEnd = {
                                    // 如果拖动超过一定距离，交换
                                    val threshold = with(density) { 60.dp.toPx() }
                                    if (kotlin.math.abs(widgetDragOffsetX) > threshold && widgetOrder.size == 2) {
                                        val newOrder = widgetOrder.toMutableList()
                                        val temp = newOrder[0]
                                        newOrder[0] = newOrder[1]
                                        newOrder[1] = temp
                                        onWidgetOrderChanged(newOrder)
                                    }
                                    widgetDragIndex = -1
                                    widgetDragOffsetX = 0f
                                },
                                onDragCancel = {
                                    widgetDragIndex = -1
                                    widgetDragOffsetX = 0f
                                }
                            )
                        }
                    }
            ) {
                if (widgetId == "clock") {
                    ClockWidget(city = city, modifier = Modifier.fillMaxWidth())
                } else {
                    WeatherWidget(city = city, weather = weather, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
fun ClockWidget(city: String, modifier: Modifier = Modifier) {
    var currentTime by remember { mutableStateOf(LocalDateTime.now()) }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = LocalDateTime.now()
            delay(1000)
        }
    }

    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    val dateFormatter = DateTimeFormatter.ofPattern("M月d日 EEEE", Locale.CHINA)

    Box(
        modifier = modifier
            .height(150.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(Color(0xFFE0C3FC), Color(0xFF8EC5FC))))
            .padding(15.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = currentTime.format(dateFormatter),
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                text = currentTime.format(timeFormatter),
                color = Color.White,
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = city,
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun WeatherWidget(city: String, weather: WeatherInfo?, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(150.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF4FACFE), Color(0xFF00F2FE))))
            .padding(15.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = city,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = weather?.temp ?: "--",
                    color = Color.White,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Light
                )
            }
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = weather?.icon ?: "❓",
                        fontSize = 24.sp
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = weather?.description ?: "加载中...",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = weather?.range ?: "",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
fun SettingsScreen(
    isActivated: Boolean,
    expiryDate: String,
    onBack: () -> Unit,
    onNavigateToActivation: () -> Unit,
    onNavigateToDisplay: () -> Unit
) {
    BackHandler { onBack() }
    val isDark = isSystemInDarkTheme()
    val backgroundColor = if (isDark) Color(0xFF1C1C1E) else Color(0xFFF2F2F7)
    val cardColor = if (isDark) Color(0xFF2C2C2E) else Color.White
    val textColor = if (isDark) Color.White else Color.Black

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .statusBarsPadding()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(cardColor)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                "返回",
                color = Color(0xFF007AFF),
                modifier = Modifier.clickable { onBack() }
            )
            Text(
                "设置",
                modifier = Modifier.align(Alignment.Center),
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
                color = textColor
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            "激活与授权",
            modifier = Modifier.padding(horizontal = 26.dp, vertical = 8.dp),
            fontSize = 13.sp,
            color = Color.Gray
        )
        Box(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(cardColor)
                .clickable(enabled = true, onClick = onNavigateToActivation)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("软件激活", fontSize = 16.sp, color = textColor)
                    if (isActivated) {
                        Text("有效期至: $expiryDate", fontSize = 12.sp, color = Color.Gray)
                    }
                }
                Text(if (isActivated) "已查看 >" else "未激活 >", color = Color.Gray, fontSize = 16.sp)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            "外观",
            modifier = Modifier.padding(horizontal = 26.dp, vertical = 8.dp),
            fontSize = 13.sp,
            color = Color.Gray
        )
        Box(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(cardColor)
                .clickable(onClick = onNavigateToDisplay)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("显示设置", fontSize = 16.sp, color = textColor)
                Text(">", color = Color.Gray, fontSize = 16.sp)
            }
        }
    }
}

@Composable
fun ActivationScreen(onBack: () -> Unit, onActivated: () -> Unit) {
    BackHandler { onBack() }
    val isDark = isSystemInDarkTheme()
    val backgroundColor = if (isDark) Color(0xFF1C1C1E) else Color(0xFFF2F2F7)
    val cardColor = if (isDark) Color(0xFF2C2C2E) else Color.White
    val textColor = if (isDark) Color.White else Color.Black
    var licenseKey by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val licenseManager = remember { LicenseManager.getInstance(context) }
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .statusBarsPadding()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(cardColor)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                "取消",
                color = Color(0xFF007AFF),
                modifier = Modifier.clickable { onBack() }
            )
            Text(
                "激活授权",
                modifier = Modifier.align(Alignment.Center),
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
                color = textColor
            )
        }

        val machineId = remember { licenseManager.getMachineId() }
        val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current

        Column(modifier = Modifier.padding(20.dp)) {
            Text("机器码", fontSize = 14.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(5.dp))
            Spacer(modifier = Modifier.height(5.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(cardColor)
                    .padding(12.dp)
            ) {
                Text(machineId, color = textColor)
            }

            Spacer(modifier = Modifier.height(10.dp))

            androidx.compose.material3.Button(
                onClick = { clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(machineId)) },
                modifier = Modifier.fillMaxWidth().height(40.dp),
                shape = RoundedCornerShape(10.dp),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFF34C759))
            ) {
                Text("复制机器码", color = Color.White, fontSize = 14.sp)
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text("激活码", fontSize = 14.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(5.dp))
            androidx.compose.material3.TextField(
                value = licenseKey,
                onValueChange = { licenseKey = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(10.dp)),
                colors = androidx.compose.material3.TextFieldDefaults.colors(
                    focusedContainerColor = cardColor,
                    unfocusedContainerColor = cardColor,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = textColor,
                    unfocusedTextColor = textColor
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            androidx.compose.material3.Button(
                onClick = { clipboardManager.getText()?.let { licenseKey = it.text } },
                modifier = Modifier.fillMaxWidth().height(40.dp),
                shape = RoundedCornerShape(10.dp),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFF5856D6))
            ) {
                Text("粘贴激活码", color = Color.White, fontSize = 14.sp)
            }

            errorMessage?.let { msg ->
                Spacer(modifier = Modifier.height(10.dp))
                Text(msg, color = Color.Red, fontSize = 13.sp)
            }

            Spacer(modifier = Modifier.height(30.dp))

            androidx.compose.material3.Button(
                onClick = {
                    coroutineScope.launch {
                        val result = licenseManager.verifyLicense(licenseKey.trim())
                        when (result) {
                            is LicenseResult.Success -> {
                                errorMessage = null
                                onActivated()
                                onBack()
                            }
                            is LicenseResult.Error -> {
                                errorMessage = result.message
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(10.dp),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF))
            ) {
                Text("激活", color = Color.White)
            }
        }
    }
}

@Composable
fun HomeScreen() {
    var showSettings by remember { mutableStateOf(false) }
    var showActivation by remember { mutableStateOf(false) }
    var showDisplaySettings by remember { mutableStateOf(false) }
    var showChatApp by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val licenseManager = remember { LicenseManager.getInstance(context) }
    val wallpaperDbHelper = remember { com.WangWangPhone.core.WallpaperDbHelper(context) }
    var isActivated by remember { mutableStateOf(licenseManager.isActivated()) }
    var expiryDate by remember { mutableStateOf(licenseManager.getExpirationDateString()) }

    // 壁纸状态：存储壁纸文件路径
    var lockWallpaperPath by remember { mutableStateOf(wallpaperDbHelper.getWallpaperFilePath(com.WangWangPhone.core.WallpaperType.LOCK)) }
    var homeWallpaperPath by remember { mutableStateOf(wallpaperDbHelper.getWallpaperFilePath(com.WangWangPhone.core.WallpaperType.HOME)) }

    val isDark = isSystemInDarkTheme()

    Box(modifier = Modifier.fillMaxSize()) {
        HomeScreenContent(
            isDark = isDark,
            onSettingsClick = { showSettings = true },
            onChatClick = { showChatApp = true },
            homeWallpaperPath = homeWallpaperPath
        )

        if (showSettings) {
            SettingsScreen(
                isActivated = isActivated,
                expiryDate = expiryDate,
                onBack = { showSettings = false },
                onNavigateToActivation = { showActivation = true },
                onNavigateToDisplay = { showDisplaySettings = true }
            )
        }

        if (showDisplaySettings) {
            DisplaySettingsScreen(
                wallpaperDbHelper = wallpaperDbHelper,
                onBack = { showDisplaySettings = false },
                onWallpaperChanged = {
                    lockWallpaperPath = wallpaperDbHelper.getWallpaperFilePath(com.WangWangPhone.core.WallpaperType.LOCK)
                    homeWallpaperPath = wallpaperDbHelper.getWallpaperFilePath(com.WangWangPhone.core.WallpaperType.HOME)
                }
            )
        }

        if (showChatApp) {
            ChatAppScreen(onClose = { showChatApp = false })
        }

        if (showActivation) {
            ActivationScreen(
                onBack = { showActivation = false },
                onActivated = {
                    isActivated = licenseManager.isActivated()
                    expiryDate = licenseManager.getExpirationDateString()
                }
            )
        }
    }
}

@Composable
fun HomeScreenContent(isDark: Boolean, onSettingsClick: () -> Unit, onChatClick: () -> Unit = {}, homeWallpaperPath: String? = null) {
    val context = LocalContext.current
    val layoutDbHelper = remember { LayoutDbHelper(context) }
    val coroutineScope = rememberCoroutineScope()

    // 默认应用列表
    val defaultApps = remember(isDark) { getDefaultApps(isDark) }

    // 当前主屏幕应用排列（可变列表，支持拖拽重排）
    val apps = remember { mutableStateListOf<AppIcon>() }

    // Dock栏应用（独立管理，初始为空，最多4个）
    val dockApps = remember { mutableStateListOf<AppIcon>() }
    val maxDockApps = 4

    // 小组件顺序
    val widgetOrder = remember { mutableStateListOf("clock", "weather") }

    // 编辑模式状态
    var isEditMode by remember { mutableStateOf(false) }

    // 当前正在拖拽的项的索引
    var draggedIndex by remember { mutableStateOf(-1) }

    // 拖拽偏移量
    var dragOffsetX by remember { mutableStateOf(0f) }
    var dragOffsetY by remember { mutableStateOf(0f) }

    // 拖拽来源: "grid" 或 "dock"
    var dragSource by remember { mutableStateOf("grid") }

    // 是否正在拖拽到Dock区域
    var isDraggingOverDock by remember { mutableStateOf(false) }

    // 从数据库加载布局
    LaunchedEffect(isDark) {
        val savedLayout = layoutDbHelper.getLayout()
        apps.clear()
        dockApps.clear()
        if (savedLayout.isNotEmpty()) {
            // 加载主屏幕网格
            val gridItems = savedLayout.filter { it.area == "grid" }.sortedBy { it.position }
            for (layoutItem in gridItems) {
                val app = defaultApps.find { it.id == layoutItem.appId }
                if (app != null) {
                    apps.add(app)
                }
            }

            // 加载Dock栏
            val dockItems = savedLayout.filter { it.area == "dock" }.sortedBy { it.position }
            for (layoutItem in dockItems) {
                val app = defaultApps.find { it.id == layoutItem.appId }
                if (app != null) {
                    dockApps.add(app)
                }
            }

            // 加载小组件顺序
            val widgetItems = savedLayout.filter { it.area == "widget" }.sortedBy { it.position }
            if (widgetItems.isNotEmpty()) {
                widgetOrder.clear()
                widgetOrder.addAll(widgetItems.map { it.appId })
            }

            // 补充数据库中没有的新应用到主屏幕（排除已在dock中的）
            val allSavedIds = savedLayout.map { it.appId }.toSet()
            for (app in defaultApps) {
                if (app.id !in allSavedIds) {
                    apps.add(app)
                }
            }
        } else {
            apps.addAll(defaultApps)
            // Dock初始为空
        }
    }

    // 保存布局（同时保存grid、dock和widget）
    fun saveCurrentLayout() {
        val items = mutableListOf<LayoutItem>()
        // 保存主屏幕网格
        apps.forEachIndexed { index, app ->
            items.add(LayoutItem(appId = app.id, position = index, area = "grid"))
        }
        // 保存Dock栏
        dockApps.forEachIndexed { index, app ->
            items.add(LayoutItem(appId = app.id, position = index, area = "dock"))
        }
        // 保存小组件顺序
        widgetOrder.forEachIndexed { index, widgetId ->
            items.add(LayoutItem(appId = widgetId, position = index, area = "widget"))
        }
        layoutDbHelper.saveLayout(items)
    }

    // 处理退出编辑模式
    if (isEditMode) {
        BackHandler {
            isEditMode = false
            saveCurrentLayout()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 背景（支持自定义壁纸）
        if (homeWallpaperPath != null) {
            val bitmap = remember(homeWallpaperPath) {
                android.graphics.BitmapFactory.decodeFile(homeWallpaperPath)
            }
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "桌面壁纸",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            } else {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black))
            }
        } else {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black))
        }

        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            // 小组件区域（支持拖拽交换）
            WidgetsSection(
                isEditMode = isEditMode,
                widgetOrder = widgetOrder.toList(),
                onWidgetOrderChanged = { newOrder ->
                    widgetOrder.clear()
                    widgetOrder.addAll(newOrder)
                    saveCurrentLayout()
                }
            )

            // 编辑模式完成按钮（浮动在右侧，不显示提示文字）
            if (isEditMode) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        "完成",
                        color = Color(0xFF007AFF),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.25f))
                            .clickable {
                                isEditMode = false
                                saveCurrentLayout()
                            }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }

            // 应用网格 - 使用固定网格手动布局以支持拖拽
            val columns = 4
            val itemSizeDp = 90.dp
            val spacingDp = 12.dp

            // 计算格子布局
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp)
                    .then(
                        if (!isEditMode) {
                            Modifier
                        } else {
                            Modifier.clickable(
                                indication = null,
                                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                            ) {
                                isEditMode = false
                                saveCurrentLayout()
                            }
                        }
                    )
            ) {
                val density = LocalDensity.current
                val totalWidth = constraints.maxWidth
                val cellWidth = totalWidth / columns

                apps.forEachIndexed { index, app ->
                    val row = index / columns
                    val col = index % columns

                    val baseX = col * cellWidth
                    val baseY = with(density) { ((itemSizeDp + spacingDp) * row).toPx() }

                    val isDragged = draggedIndex == index && dragSource == "grid"

                    // 抖动动画（编辑模式）
                    val infiniteTransition = rememberInfiniteTransition(label = "wiggle_$index")
                    val wiggleAngle by infiniteTransition.animateFloat(
                        initialValue = if (index % 2 == 0) -1.5f else 1.5f,
                        targetValue = if (index % 2 == 0) 1.5f else -1.5f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(
                                durationMillis = 150 + (index % 3) * 50,
                                easing = LinearEasing
                            ),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "wiggle_angle_$index"
                    )

                    Box(
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    x = baseX + if (isDragged) dragOffsetX.roundToInt() else 0,
                                    y = baseY.roundToInt() + if (isDragged) dragOffsetY.roundToInt() else 0
                                )
                            }
                            .width(with(density) { cellWidth.toDp() })
                            .zIndex(if (isDragged) 10000f else 0f)
                            .graphicsLayer {
                                if (isEditMode) {
                                    rotationZ = if (isDragged) 0f else wiggleAngle
                                }
                                scaleX = if (isDragged) 1.15f else 1f
                                scaleY = if (isDragged) 1.15f else 1f
                                alpha = if (isDragged) 0.3f else 1f
                            }
                            .pointerInput(isEditMode, index) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = {
                                        if (!isEditMode) {
                                            isEditMode = true
                                        }
                                        draggedIndex = index
                                        dragSource = "grid"
                                        dragOffsetX = 0f
                                        dragOffsetY = 0f
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        dragOffsetX += dragAmount.x
                                        dragOffsetY += dragAmount.y

                                        // 检查是否拖到了Dock区域（通过Y坐标判断）
                                        val currentAbsY = baseY + dragOffsetY
                                        val gridMaxY = with(density) { ((itemSizeDp + spacingDp) * ((apps.size + columns - 1) / columns)).toPx() }
                                        isDraggingOverDock = currentAbsY > gridMaxY + with(density) { 50.dp.toPx() }

                                        // 仅在grid区域内排序
                                        if (!isDraggingOverDock) {
                                            val cellHeightPx = with(density) { (itemSizeDp + spacingDp).toPx() }
                                            val currentCenterX = baseX.toFloat() + cellWidth.toFloat() / 2f + dragOffsetX
                                            val currentCenterY = baseY + cellHeightPx / 2f + dragOffsetY

                                            val targetCol = (currentCenterX / cellWidth.toFloat()).toInt().coerceIn(0, columns - 1)
                                            val targetRow = (currentCenterY / cellHeightPx).toInt().coerceAtLeast(0)
                                            val targetIndex = (targetRow * columns + targetCol).coerceIn(0, apps.size - 1)

                                            if (targetIndex != draggedIndex && targetIndex >= 0 && targetIndex < apps.size) {
                                                val draggedApp = apps[draggedIndex]
                                                apps.removeAt(draggedIndex)
                                                apps.add(targetIndex, draggedApp)

                                                val oldRow = draggedIndex / columns
                                                val oldCol = draggedIndex % columns
                                                val newRow = targetIndex / columns
                                                val newCol = targetIndex % columns
                                                dragOffsetX += (oldCol - newCol).toFloat() * cellWidth.toFloat()
                                                dragOffsetY += (oldRow - newRow).toFloat() * cellHeightPx

                                                draggedIndex = targetIndex
                                            }
                                        }
                                    },
                                    onDragEnd = {
                                        // 如果拖到了Dock区域且Dock未满，移到Dock
                                        if (isDraggingOverDock && dockApps.size < maxDockApps && draggedIndex >= 0 && draggedIndex < apps.size) {
                                            val app = apps[draggedIndex]
                                            apps.removeAt(draggedIndex)
                                            dockApps.add(app)
                                        }
                                        draggedIndex = -1
                                        dragOffsetX = 0f
                                        dragOffsetY = 0f
                                        isDraggingOverDock = false
                                        saveCurrentLayout()
                                    },
                                    onDragCancel = {
                                        draggedIndex = -1
                                        dragOffsetX = 0f
                                        dragOffsetY = 0f
                                        isDraggingOverDock = false
                                    }
                                )
                            },
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable(enabled = !isEditMode) {
                                    if (app.id == "settings") onSettingsClick()
                                    else if (app.id == "chat") onChatClick()
                                }
                                .padding(vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier.size(60.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (app.useImage) {
                                    val resId = context.resources.getIdentifier(app.icon, "drawable", context.packageName)
                                    if (resId != 0) {
                                        Image(
                                            painter = androidx.compose.ui.res.painterResource(id = resId),
                                            contentDescription = app.name,
                                            modifier = Modifier.size(60.dp)
                                        )
                                    }
                                } else {
                                    Text(app.icon, fontSize = 48.sp)
                                }
                            }
                            Spacer(modifier = Modifier.height(5.dp))
                            Text(app.name, color = Color.White, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Dock 栏（独立管理，不再自动取前4个）
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 20.dp, start = 15.dp, end = 15.dp)
                .fillMaxWidth()
                .height(90.dp)
                .clip(RoundedCornerShape(30.dp))
        ) {
            // 磨砂玻璃背景层（拖拽到Dock时高亮）
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        if (isDraggingOverDock && dragSource == "grid" && dockApps.size < maxDockApps)
                            Color(0xFF007AFF).copy(alpha = 0.4f)
                        else
                            Color.White.copy(alpha = 0.3f)
                    )
                    .blur(20.dp)
            )

            // Dock应用图标层
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = if (dockApps.isEmpty()) Arrangement.Center else Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (dockApps.isEmpty() && !isEditMode) {
                    // Dock为空时的提示
                    Text(
                        "长按拖入应用",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 12.sp
                    )
                }

                if (dockApps.isEmpty() && isEditMode) {
                    // 编辑模式下Dock为空时的占位提示
                    Text(
                        "拖拽应用到此处",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 13.sp
                    )
                }

                dockApps.forEachIndexed { dockIndex, app ->
                    val isDockDragged = draggedIndex == dockIndex && dragSource == "dock"

                    // Dock栏抖动动画（编辑模式）
                    val dockWiggleTransition = rememberInfiniteTransition(label = "dock_wiggle_$dockIndex")
                    val dockWiggleAngle by dockWiggleTransition.animateFloat(
                        initialValue = if (dockIndex % 2 == 0) -1.5f else 1.5f,
                        targetValue = if (dockIndex % 2 == 0) 1.5f else -1.5f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(
                                durationMillis = 150 + (dockIndex % 3) * 50,
                                easing = LinearEasing
                            ),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "dock_wiggle_angle_$dockIndex"
                    )

                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .zIndex(if (isDockDragged) 10000f else 0f)
                            .graphicsLayer {
                                if (isEditMode) {
                                    rotationZ = if (isDockDragged) 0f else dockWiggleAngle
                                }
                                scaleX = if (isDockDragged) 1.15f else 1f
                                scaleY = if (isDockDragged) 1.15f else 1f
                                alpha = if (isDockDragged) 0.3f else 1f
                                if (isDockDragged) {
                                    translationX = dragOffsetX
                                    translationY = dragOffsetY
                                }
                            }
                            .pointerInput(isEditMode, dockIndex) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = {
                                        if (!isEditMode) {
                                            isEditMode = true
                                        }
                                        draggedIndex = dockIndex
                                        dragSource = "dock"
                                        dragOffsetX = 0f
                                        dragOffsetY = 0f
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        dragOffsetX += dragAmount.x
                                        dragOffsetY += dragAmount.y

                                        // Dock内水平排序
                                        if (kotlin.math.abs(dragOffsetY) < with(density) { 50.dp.toPx() } && dockApps.size > 1) {
                                            val dockCellWidth = with(density) { 85.dp.toPx() }
                                            val colOffset = (dragOffsetX / dockCellWidth).roundToInt()
                                            if (colOffset != 0) {
                                                val targetIdx = (draggedIndex + colOffset).coerceIn(0, dockApps.size - 1)
                                                if (targetIdx != draggedIndex) {
                                                    val draggedApp = dockApps[draggedIndex]
                                                    dockApps.removeAt(draggedIndex)
                                                    dockApps.add(targetIdx, draggedApp)
                                                    // 调整偏移量
                                                    dragOffsetX -= (targetIdx - draggedIndex) * dockCellWidth
                                                    draggedIndex = targetIdx
                                                }
                                            }
                                        }
                                    },
                                    onDragEnd = {
                                        // 如果向上拖出Dock区域，移回主屏幕
                                        if (dragOffsetY < -with(density) { 50.dp.toPx() } && draggedIndex >= 0 && draggedIndex < dockApps.size) {
                                            val movedApp = dockApps[draggedIndex]
                                            dockApps.removeAt(draggedIndex)
                                            apps.add(movedApp)
                                        }
                                        draggedIndex = -1
                                        dragOffsetX = 0f
                                        dragOffsetY = 0f
                                        dragSource = "grid"
                                        saveCurrentLayout()
                                    },
                                    onDragCancel = {
                                        draggedIndex = -1
                                        dragOffsetX = 0f
                                        dragOffsetY = 0f
                                        dragSource = "grid"
                                    }
                                )
                            }
                            .clickable(enabled = !isEditMode) {
                                if (app.id == "settings") onSettingsClick()
                                else if (app.id == "chat") onChatClick()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (app.useImage) {
                            val resId = context.resources.getIdentifier(app.icon, "drawable", context.packageName)
                            if (resId != 0) {
                                Image(
                                    painter = androidx.compose.ui.res.painterResource(id = resId),
                                    contentDescription = app.name,
                                    modifier = Modifier.size(60.dp)
                                )
                            }
                        } else {
                            Text(app.icon, fontSize = 48.sp)
                        }
                    }
                }
            }
        }

        // Home Indicator
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp)
                .width(120.dp)
                .height(5.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(Color.White.copy(alpha = 0.8f))
        )
    }
}

/**
 * 显示设置页面（二级菜单）
 * 包含锁屏壁纸设置和桌面壁纸设置
 */
@Composable
fun DisplaySettingsScreen(
    wallpaperDbHelper: WallpaperDbHelper,
    onBack: () -> Unit,
    onWallpaperChanged: () -> Unit
) {
    BackHandler { onBack() }
    val isDark = isSystemInDarkTheme()
    val backgroundColor = if (isDark) Color(0xFF1C1C1E) else Color(0xFFF2F2F7)
    val cardColor = if (isDark) Color(0xFF2C2C2E) else Color.White
    val textColor = if (isDark) Color.White else Color.Black

    // 壁纸预览状态
    var lockPreviewPath by remember {
        mutableStateOf(wallpaperDbHelper.getWallpaperFilePath(WallpaperType.LOCK))
    }
    var homePreviewPath by remember {
        mutableStateOf(wallpaperDbHelper.getWallpaperFilePath(WallpaperType.HOME))
    }

    // 图片选择器 - 锁屏壁纸
    val lockWallpaperLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val fileName = wallpaperDbHelper.copyImageToStorage(it)
            if (fileName != null) {
                wallpaperDbHelper.saveWallpaper(WallpaperType.LOCK, fileName)
                lockPreviewPath = wallpaperDbHelper.getWallpaperFilePath(WallpaperType.LOCK)
                onWallpaperChanged()
            }
        }
    }

    // 图片选择器 - 桌面壁纸
    val homeWallpaperLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val fileName = wallpaperDbHelper.copyImageToStorage(it)
            if (fileName != null) {
                wallpaperDbHelper.saveWallpaper(WallpaperType.HOME, fileName)
                homePreviewPath = wallpaperDbHelper.getWallpaperFilePath(WallpaperType.HOME)
                onWallpaperChanged()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .statusBarsPadding()
    ) {
        // 顶部导航栏
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(cardColor)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                "返回",
                color = Color(0xFF007AFF),
                modifier = Modifier.clickable { onBack() }
            )
            Text(
                "显示设置",
                modifier = Modifier.align(Alignment.Center),
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
                color = textColor
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 锁屏壁纸设置
        Text(
            "锁屏壁纸",
            modifier = Modifier.padding(horizontal = 26.dp, vertical = 8.dp),
            fontSize = 13.sp,
            color = Color.Gray
        )
        WallpaperSettingCard(
            title = "锁屏壁纸设置",
            previewPath = lockPreviewPath,
            cardColor = cardColor,
            textColor = textColor,
            onSelectImage = { lockWallpaperLauncher.launch("image/*") }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 桌面壁纸设置
        Text(
            "桌面壁纸",
            modifier = Modifier.padding(horizontal = 26.dp, vertical = 8.dp),
            fontSize = 13.sp,
            color = Color.Gray
        )
        WallpaperSettingCard(
            title = "桌面壁纸设置",
            previewPath = homePreviewPath,
            cardColor = cardColor,
            textColor = textColor,
            onSelectImage = { homeWallpaperLauncher.launch("image/*") }
        )
    }
}

/**
 * 壁纸设置卡片组件
 */
@Composable
fun WallpaperSettingCard(
    title: String,
    previewPath: String?,
    cardColor: Color,
    textColor: Color,
    onSelectImage: () -> Unit
) {
    Box(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(cardColor)
            .clickable(onClick = onSelectImage)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 16.sp, color = textColor)
                Text(
                    if (previewPath != null) "已设置，点击更换" else "点击从相册选择图片",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
            // 壁纸缩略图预览
            if (previewPath != null) {
                val bitmap = remember(previewPath) {
                    android.graphics.BitmapFactory.decodeFile(previewPath)
                }
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = title,
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Gray.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🖼️", fontSize = 24.sp)
                }
            }
        }
    }
}