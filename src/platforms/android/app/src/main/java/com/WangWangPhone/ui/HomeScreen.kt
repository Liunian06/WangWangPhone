package com.WangWangPhone.ui

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventTimeoutCancellationException
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.WangWangPhone.core.LayoutDbHelper
import com.WangWangPhone.core.LayoutItem
import com.WangWangPhone.core.LicenseManager
import com.WangWangPhone.core.LicenseResult
import com.WangWangPhone.core.UserProfileDbHelper
import com.WangWangPhone.core.WallpaperDbHelper
import com.WangWangPhone.core.WallpaperType
import com.WangWangPhone.core.WeatherCacheDbHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

// 定义统一的网格项类型
interface GridItem {
    val id: String
    val type: String // "app" or "widget"
    val spanX: Int
    val spanY: Int
}

data class AppIcon(
    override val id: String,
    val name: String,
    val icon: String,
    val color: Brush,
    val useImage: Boolean = false,
    override val spanX: Int = 1,
    override val spanY: Int = 1
) : GridItem {
    override val type = "app"
}

data class WidgetItem(
    override val id: String,
    val widgetType: String, // "clock", "weather"
    override val spanX: Int = 2,
    override val spanY: Int = 2
) : GridItem {
    override val type = "widget"
}

fun getDefaultApps(isDark: Boolean): List<AppIcon> = listOf(
    AppIcon("phone", "电话", "📞", Brush.linearGradient(listOf(Color(0xFFFF9A9E), Color(0xFFFECFEF)))),
    AppIcon("chat", "聊天", "💬", Brush.linearGradient(listOf(Color(0xFF07C160), Color(0xFF06AD56)))),
    AppIcon("safari", "Safari", "🧭", Brush.linearGradient(listOf(Color(0xFF84FAB0), Color(0xFF8FD3F4)))),
    AppIcon("music", "音乐", "🎵", Brush.linearGradient(listOf(Color(0xFFF6D365), Color(0xFFFDA085)))),
    AppIcon("camera", "相机", "📷", Brush.linearGradient(listOf(Color.White, Color.LightGray))),
    AppIcon("calendar", "日历", "📅", Brush.linearGradient(listOf(Color.White, Color.LightGray))),
    AppIcon("settings", "设置", if (isDark) "ic_settings_dark" else "ic_settings_light",
        Brush.linearGradient(listOf(Color.White, Color.LightGray)), useImage = true),
    AppIcon("wangwang", "汪汪", "🐶", Brush.linearGradient(listOf(Color.White, Color.LightGray))),
    // 第二批应用
    AppIcon("photos", "照片", "🖼️", Brush.linearGradient(listOf(Color(0xFFFDEB71), Color(0xFFF8D800)))),
    AppIcon("video", "视频", "🎬", Brush.linearGradient(listOf(Color(0xFFA18CD1), Color(0xFFFBC2EB)))),
    AppIcon("map", "地图", "🗺️", Brush.linearGradient(listOf(Color(0xFF667EEA), Color(0xFF764BA2)))),
    AppIcon("notes", "备忘录", "📝", Brush.linearGradient(listOf(Color(0xFFFEDA77), Color(0xFFFDB99B)))),
    AppIcon("calculator", "计算器", "🔢", Brush.linearGradient(listOf(Color(0xFF2C3E50), Color(0xFF4CA1AF)))),
    AppIcon("clock_app", "时钟", "⏰", Brush.linearGradient(listOf(Color(0xFF232526), Color(0xFF414345)))),
    AppIcon("appstore", "应用商店", "🏪", Brush.linearGradient(listOf(Color(0xFF2196F3), Color(0xFF21CBF3)))),
    AppIcon("mail", "邮件", "📧", Brush.linearGradient(listOf(Color(0xFF1A73E8), Color(0xFF4FC3F7)))),
    AppIcon("contacts", "通讯录", "👤", Brush.linearGradient(listOf(Color(0xFFE0E0E0), Color(0xFFBDBDBD)))),
    AppIcon("files", "文件", "📁", Brush.linearGradient(listOf(Color(0xFF42A5F5), Color(0xFF1976D2)))),
    AppIcon("health", "健康", "❤️", Brush.linearGradient(listOf(Color(0xFFFF6B6B), Color(0xFFEE5A24)))),
    AppIcon("wallet", "钱包", "💳", Brush.linearGradient(listOf(Color(0xFF232526), Color(0xFF414345)))),
    // 第三批应用
    AppIcon("weather_app", "天气", "🌤️", Brush.linearGradient(listOf(Color(0xFF4FACFE), Color(0xFF00F2FE)))),
    AppIcon("compass", "指南针", "🧭", Brush.linearGradient(listOf(Color(0xFF232526), Color(0xFF414345)))),
    AppIcon("voice_memo", "语音备忘", "🎙️", Brush.linearGradient(listOf(Color(0xFFFF416C), Color(0xFFFF4B2B)))),
    AppIcon("translate", "翻译", "🌐", Brush.linearGradient(listOf(Color(0xFF4FACFE), Color(0xFF00F2FE)))),
    AppIcon("books", "图书", "📚", Brush.linearGradient(listOf(Color(0xFFFF9A9E), Color(0xFFFAD0C4)))),
    AppIcon("podcast", "播客", "🎧", Brush.linearGradient(listOf(Color(0xFF8E2DE2), Color(0xFF4A00E0)))),
    AppIcon("reminder", "提醒事项", "📋", Brush.linearGradient(listOf(Color(0xFFFF9A9E), Color(0xFFFECFEF)))),
    AppIcon("facetime", "FaceTime", "📹", Brush.linearGradient(listOf(Color(0xFF43E97B), Color(0xFF38F9D7)))),
    AppIcon("news", "新闻", "📰", Brush.linearGradient(listOf(Color(0xFFFF416C), Color(0xFFFF4B2B)))),
    AppIcon("stocks", "股票", "📈", Brush.linearGradient(listOf(Color(0xFF232526), Color(0xFF414345))))
)

fun getDefaultWidgets(): List<WidgetItem> = listOf(
    WidgetItem("clock_widget", "clock"),
    WidgetItem("weather_widget", "weather")
)

const val GRID_COLUMNS = 4
const val GRID_ROWS = 7
const val TOTAL_CELLS = GRID_COLUMNS * GRID_ROWS

data class WeatherInfo(val temp: String, val description: String, val icon: String, val range: String)

suspend fun fetchLocation(): String { delay(500); return "广州" }
suspend fun fetchWeather(city: String): WeatherInfo {
    delay(500); return WeatherInfo("25°", "多云", "⛅", "最高 29° 最低 21°")
}

/**
 * 带缓存的天气加载逻辑
 * 1. 先查数据库缓存，如果今天已请求过则直接使用缓存
 * 2. 如果没有缓存，则请求网络并保存到数据库
 */
@Composable
fun WidgetContent(widgetType: String, modifier: Modifier = Modifier) {
    var city by remember { mutableStateOf("...") }
    var weather by remember { mutableStateOf<WeatherInfo?>(null) }
    val context = LocalContext.current
    val weatherCacheDbHelper = remember { com.WangWangPhone.core.WeatherCacheDbHelper(context) }

    LaunchedEffect(Unit) {
        city = fetchLocation()
        if (city.isNotEmpty() && city != "...") {
            // 先查缓存
            val cached = weatherCacheDbHelper.getTodayWeatherCache(city)
            if (cached != null) {
                // 今天已经请求过，直接使用缓存
                weather = WeatherInfo(
                    temp = cached.temp,
                    description = cached.description,
                    icon = cached.icon,
                    range = cached.range
                )
            } else {
                // 没有缓存，请求网络
                val freshWeather = fetchWeather(city)
                weather = freshWeather
                // 保存到数据库
                weatherCacheDbHelper.saveWeatherCache(
                    com.WangWangPhone.core.WeatherCacheRecord(
                        city = city,
                        temp = freshWeather.temp,
                        description = freshWeather.description,
                        icon = freshWeather.icon,
                        range = freshWeather.range,
                        requestDate = com.WangWangPhone.core.WeatherCacheDbHelper.getTodayDateString()
                    )
                )
                // 清除过期缓存
                weatherCacheDbHelper.clearExpiredCache()
            }
        }
    }

    if (widgetType == "clock") {
        ClockWidget(city = city, modifier = modifier)
    } else if (widgetType == "weather") {
        WeatherWidget(city = city, weather = weather, modifier = modifier)
    }
}

@Composable
fun ClockWidget(city: String, modifier: Modifier = Modifier) {
    var currentTime by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) { while (true) { currentTime = LocalDateTime.now(); delay(1000) } }
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    val dateFormatter = DateTimeFormatter.ofPattern("M月d日 EEEE", Locale.CHINA)
    Box(modifier = modifier.fillMaxSize().clip(RoundedCornerShape(20.dp))
        .background(Brush.linearGradient(listOf(Color(0xFFE0C3FC), Color(0xFF8EC5FC)))).padding(15.dp)) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
            Text(currentTime.format(dateFormatter), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(5.dp))
            Text(currentTime.format(timeFormatter), color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.Bold)
            Text(city, color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
        }
    }
}

@Composable
fun WeatherWidget(city: String, weather: WeatherInfo?, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize().clip(RoundedCornerShape(20.dp))
        .background(Brush.linearGradient(listOf(Color(0xFF4FACFE), Color(0xFF00F2FE)))).padding(15.dp)) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(city, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(weather?.temp ?: "--", color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.Light)
            }
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(weather?.icon ?: "❓", fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(weather?.description ?: "加载中...", color = Color.White, fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(weather?.range ?: "", color = Color.White.copy(alpha = 0.8f), fontSize = 10.sp)
            }
        }
    }
}

@Composable
fun SettingsScreen(
    isActivated: Boolean, expiryDate: String, onBack: () -> Unit,
    onNavigateToActivation: () -> Unit, onNavigateToDisplay: () -> Unit,
    onResetToDefault: () -> Unit
) {
    BackHandler { onBack() }
    val isDark = isSystemInDarkTheme()
    val bg = if (isDark) Color(0xFF1C1C1E) else Color(0xFFF2F2F7)
    val card = if (isDark) Color(0xFF2C2C2E) else Color.White
    val txt = if (isDark) Color.White else Color.Black
    Column(modifier = Modifier.fillMaxSize().background(bg).statusBarsPadding()) {
        Box(modifier = Modifier.fillMaxWidth().height(56.dp).background(card).padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart) {
            Text("返回", color = Color(0xFF007AFF), modifier = Modifier.clickable { onBack() })
            Text("设置", modifier = Modifier.align(Alignment.Center), fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = txt)
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        Text("激活与授权", modifier = Modifier.padding(horizontal = 26.dp, vertical = 8.dp), fontSize = 13.sp, color = Color.Gray)
        Box(modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(RoundedCornerShape(10.dp))
            .background(card).clickable(onClick = onNavigateToActivation).padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("软件激活", fontSize = 16.sp, color = txt)
                    if (isActivated) Text("有效期至: $expiryDate", fontSize = 12.sp, color = Color.Gray)
                }
                Text(if (isActivated) "已查看 >" else "未激活 >", color = Color.Gray, fontSize = 16.sp)
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        Text("外观", modifier = Modifier.padding(horizontal = 26.dp, vertical = 8.dp), fontSize = 13.sp, color = Color.Gray)
        Box(modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(RoundedCornerShape(10.dp))
            .background(card).clickable(onClick = onNavigateToDisplay).padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("显示设置", fontSize = 16.sp, color = txt)
                Text(">", color = Color.Gray, fontSize = 16.sp)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        Text("系统", modifier = Modifier.padding(horizontal = 26.dp, vertical = 8.dp), fontSize = 13.sp, color = Color.Gray)
        Box(modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(RoundedCornerShape(10.dp))
            .background(card).clickable(onClick = onResetToDefault).padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("恢复默认设置", fontSize = 16.sp, color = Color.Red)
                Text(">", color = Color.Gray, fontSize = 16.sp)
            }
        }
    }
}

@Composable
fun ActivationScreen(onBack: () -> Unit, onActivated: () -> Unit) {
    BackHandler { onBack() }
    val isDark = isSystemInDarkTheme()
    val bg = if (isDark) Color(0xFF1C1C1E) else Color(0xFFF2F2F7)
    val card = if (isDark) Color(0xFF2C2C2E) else Color.White
    val txt = if (isDark) Color.White else Color.Black
    var licenseKey by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val licenseManager = remember { LicenseManager.getInstance(context) }
    val coroutineScope = rememberCoroutineScope()
    Column(modifier = Modifier.fillMaxSize().background(bg).statusBarsPadding()) {
        Box(modifier = Modifier.fillMaxWidth().height(56.dp).background(card).padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart) {
            Text("取消", color = Color(0xFF007AFF), modifier = Modifier.clickable { onBack() })
            Text("激活授权", modifier = Modifier.align(Alignment.Center), fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = txt)
        }
        val machineId = remember { licenseManager.getMachineId() }
        val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
        Column(modifier = Modifier.padding(20.dp)) {
            Text("机器码", fontSize = 14.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(10.dp))
            Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(card).padding(12.dp)) {
                Text(machineId, color = txt)
            }
            Spacer(modifier = Modifier.height(10.dp))
            androidx.compose.material3.Button(
                onClick = { clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(machineId)) },
                modifier = Modifier.fillMaxWidth().height(40.dp), shape = RoundedCornerShape(10.dp),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFF34C759))
            ) { Text("复制机器码", color = Color.White, fontSize = 14.sp) }
            Spacer(modifier = Modifier.height(20.dp))
            Text("激活码", fontSize = 14.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(5.dp))
            androidx.compose.material3.TextField(
                value = licenseKey, onValueChange = { licenseKey = it },
                modifier = Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(10.dp)),
                colors = androidx.compose.material3.TextFieldDefaults.colors(
                    focusedContainerColor = card, unfocusedContainerColor = card,
                    focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = txt, unfocusedTextColor = txt
                )
            )
            Spacer(modifier = Modifier.height(10.dp))
            androidx.compose.material3.Button(
                onClick = { clipboardManager.getText()?.let { licenseKey = it.text } },
                modifier = Modifier.fillMaxWidth().height(40.dp), shape = RoundedCornerShape(10.dp),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFF5856D6))
            ) { Text("粘贴激活码", color = Color.White, fontSize = 14.sp) }
            errorMessage?.let { msg ->
                Spacer(modifier = Modifier.height(10.dp))
                Text(msg, color = Color.Red, fontSize = 13.sp)
            }
            Spacer(modifier = Modifier.height(30.dp))
            androidx.compose.material3.Button(
                onClick = {
                    coroutineScope.launch {
                        when (val result = licenseManager.verifyLicense(licenseKey.trim())) {
                            is LicenseResult.Success -> { errorMessage = null; onActivated(); onBack() }
                            is LicenseResult.Error -> { errorMessage = result.message }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(10.dp),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF))
            ) { Text("激活", color = Color.White) }
        }
    }
}

@Composable
fun HomeScreen() {
    var showSettings by remember { mutableStateOf(false) }
    var showActivation by remember { mutableStateOf(false) }
    var showDisplaySettings by remember { mutableStateOf(false) }
    var showChatApp by remember { mutableStateOf(false) }
    var showActivationAlert by remember { mutableStateOf(false) }
    var showResetConfirm by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val licenseManager = remember { LicenseManager.getInstance(context) }
    val wallpaperDbHelper = remember { WallpaperDbHelper(context) }
    val layoutDbHelper = remember { LayoutDbHelper(context) }
    val weatherCacheDbHelper = remember { WeatherCacheDbHelper(context) }
    val userProfileDbHelper = remember { UserProfileDbHelper(context) }
    var isActivated by remember { mutableStateOf(licenseManager.isActivated()) }
    var expiryDate by remember { mutableStateOf(licenseManager.getExpirationDateString()) }
    var lockWallpaperPath by remember { mutableStateOf(wallpaperDbHelper.getWallpaperFilePath(WallpaperType.LOCK)) }
    var homeWallpaperPath by remember { mutableStateOf(wallpaperDbHelper.getWallpaperFilePath(WallpaperType.HOME)) }
    val isDark = isSystemInDarkTheme()
    Box(modifier = Modifier.fillMaxSize()) {
        HomeScreenContent(
            isDark = isDark,
            isActivated = isActivated,
            onSettingsClick = { showSettings = true },
            onChatClick = { showChatApp = true },
            onActivationAlert = { showActivationAlert = true },
            homeWallpaperPath = homeWallpaperPath
        )
        
        if (showActivationAlert) {
             androidx.compose.material3.AlertDialog(
                onDismissRequest = { showActivationAlert = false },
                title = { Text("未激活") },
                text = { Text("请先激活软件") },
                confirmButton = {
                    androidx.compose.material3.TextButton(
                        onClick = {
                            showActivationAlert = false
                            showSettings = true
                            showActivation = true
                        }
                    ) { Text("去激活") }
                },
                dismissButton = {
                    androidx.compose.material3.TextButton(onClick = { showActivationAlert = false }) { Text("取消") }
                }
            )
        }

        if (showSettings) SettingsScreen(isActivated = isActivated, expiryDate = expiryDate,
            onBack = { showSettings = false }, onNavigateToActivation = { showActivation = true },
            onNavigateToDisplay = { showDisplaySettings = true },
            onResetToDefault = { showResetConfirm = true })
        
        if (showResetConfirm) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showResetConfirm = false },
                title = { Text("恢复默认设置") },
                text = { Text("此操作将清除所有自定义布局、壁纸、天气缓存和用户资料，且无法撤销。是否继续？") },
                confirmButton = {
                    androidx.compose.material3.TextButton(
                        onClick = {
                            showResetConfirm = false
                            if (layoutDbHelper.resetToDefaultSettings(wallpaperDbHelper, weatherCacheDbHelper, userProfileDbHelper)) {
                                // 强制重新加载以应用默认设置
                                android.widget.Toast.makeText(context, "设置已恢复", android.widget.Toast.LENGTH_SHORT).show()
                                // 简单粗暴的做法是重启，或者通过重置状态来更新UI
                                // 这里我们选择触发壁纸和设置状态的更新
                                lockWallpaperPath = wallpaperDbHelper.getWallpaperFilePath(WallpaperType.LOCK)
                                homeWallpaperPath = wallpaperDbHelper.getWallpaperFilePath(WallpaperType.HOME)
                                // 对于多页布局，它在 LaunchedEffect(isDark) 中加载，
                                // 如果我们想强制触发它，可以增加一个触发器状态，或者直接在 HomeScreen 里做更深层的逻辑
                                // 但通常最简单的逻辑是恢复后提示重启或手动重置关键 State
                                showSettings = false
                            }
                        }
                    ) { Text("确定", color = Color.Red) }
                },
                dismissButton = {
                    androidx.compose.material3.TextButton(onClick = { showResetConfirm = false }) { Text("取消") }
                }
            )
        }

        if (showDisplaySettings) DisplaySettingsScreen(wallpaperDbHelper = wallpaperDbHelper,
            onBack = { showDisplaySettings = false }, onWallpaperChanged = {
                lockWallpaperPath = wallpaperDbHelper.getWallpaperFilePath(WallpaperType.LOCK)
                homeWallpaperPath = wallpaperDbHelper.getWallpaperFilePath(WallpaperType.HOME)
            })
        if (showChatApp) ChatAppScreen(onClose = { showChatApp = false })
        if (showActivation) ActivationScreen(onBack = { showActivation = false }, onActivated = {
            isActivated = licenseManager.isActivated(); expiryDate = licenseManager.getExpirationDateString()
        })
    }
}

/**
 * 将所有应用和Widget分配到多个页面
 * 第一页：Widget + 部分应用
 * 后续页：纯应用
 */
fun distributeItemsToPages(
    allApps: List<AppIcon>,
    widgets: List<WidgetItem>
): List<Map<Int, GridItem>> {
    val pages = mutableListOf<MutableMap<Int, GridItem>>()
    val remainingApps = allApps.toMutableList()

    // 第一页：放置Widget和部分应用
    val page0 = mutableMapOf<Int, GridItem>()
    // 放置Widget（每个占2x2）
    if (widgets.isNotEmpty()) page0[0] = widgets[0]  // 左上角 clock
    if (widgets.size > 1) page0[2] = widgets[1]      // 右上角 weather

    // 第一页从第3行开始放应用（前2行被Widget占据）
    var pos = 8 // row 2, col 0
    while (pos < TOTAL_CELLS && remainingApps.isNotEmpty()) {
        page0[pos] = remainingApps.removeAt(0)
        pos++
    }
    pages.add(page0)

    // 后续页：每页最多 TOTAL_CELLS 个应用
    while (remainingApps.isNotEmpty()) {
        val page = mutableMapOf<Int, GridItem>()
        var pagePos = 0
        while (pagePos < TOTAL_CELLS && remainingApps.isNotEmpty()) {
            page[pagePos] = remainingApps.removeAt(0)
            pagePos++
        }
        pages.add(page)
    }

    return pages
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreenContent(
    isDark: Boolean,
    isActivated: Boolean,
    onSettingsClick: () -> Unit,
    onChatClick: () -> Unit = {},
    onActivationAlert: () -> Unit,
    homeWallpaperPath: String? = null
) {
    val context = LocalContext.current
    val layoutDbHelper = remember { LayoutDbHelper(context) }
    val defaultApps = remember(isDark) { getDefaultApps(isDark) }
    val defaultWidgets = remember { getDefaultWidgets() }

    // 多页网格: pageIndex -> (cellIndex -> GridItem)
    val allPages = remember { mutableStateListOf<MutableMap<Int, GridItem>>() }
    val dockApps = remember { mutableStateListOf<AppIcon>() }
    val maxDockApps = 4
    var isEditMode by remember { mutableStateOf(false) }
    var pageCount by remember { mutableIntStateOf(1) }

    // 拖拽状态
    var draggedItem by remember { mutableStateOf<GridItem?>(null) }
    var dragSource by remember { mutableStateOf("grid") }
    var dragSourceCellIndex by remember { mutableStateOf(-1) }
    var dragSourcePageIndex by remember { mutableStateOf(-1) }
    var dragSourceDockIndex by remember { mutableStateOf(-1) }
    var dragOverlayX by remember { mutableStateOf(0f) }
    var dragOverlayY by remember { mutableStateOf(0f) }
    var highlightCellIndex by remember { mutableStateOf(-1) }
    var gridAreaOffset by remember { mutableStateOf(Offset.Zero) }
    var gridAreaSize by remember { mutableStateOf(IntSize.Zero) }
    var dockAreaOffset by remember { mutableStateOf(Offset.Zero) }
    var dockAreaSize by remember { mutableStateOf(IntSize.Zero) }

    // 从数据库加载布局
    LaunchedEffect(isDark) {
        val savedLayout = layoutDbHelper.getLayout()
        allPages.clear(); dockApps.clear()

        if (savedLayout.isNotEmpty()) {
            // 解析多页布局
            val pageMap = mutableMapOf<Int, MutableMap<Int, GridItem>>()

            savedLayout.filter { it.area.startsWith("grid") }.forEach { li ->
                val pageIdx = if (li.area == "grid") 0 else {
                    li.area.removePrefix("grid_").toIntOrNull() ?: 0
                }
                val page = pageMap.getOrPut(pageIdx) { mutableMapOf() }
                val app = defaultApps.find { it.id == li.appId }
                if (app != null) {
                    page[li.position] = app
                } else {
                    val widget = defaultWidgets.find { it.id == li.appId }
                    if (widget != null) {
                        page[li.position] = widget
                    }
                }
            }

            savedLayout.filter { it.area == "dock" }.sortedBy { it.position }.forEach { li ->
                defaultApps.find { it.id == li.appId }?.let { dockApps.add(it) }
            }

            // 确保所有页面按顺序添加
            val maxPage = pageMap.keys.maxOrNull() ?: 0
            for (i in 0..maxPage) {
                allPages.add(pageMap.getOrDefault(i, mutableMapOf()))
            }

            // 确保默认应用和组件存在
            val allSavedIds = savedLayout.map { it.appId }.toSet()

            // 添加缺失的 Widget 到第一页
            if (allPages.isEmpty()) allPages.add(mutableMapOf())
            for (widget in defaultWidgets) {
                if (widget.id !in allSavedIds) {
                    val page0 = allPages[0]
                    for (i in 0 until TOTAL_CELLS) {
                        if (checkOccupancy(page0, i, widget.spanX, widget.spanY, null)) {
                            page0[i] = widget
                            break
                        }
                    }
                }
            }

            // 添加缺失的 App
            for (app in defaultApps) {
                if (app.id !in allSavedIds) {
                    var placed = false
                    for (page in allPages) {
                        for (i in 0 until TOTAL_CELLS) {
                            if (checkOccupancy(page, i, 1, 1, null)) {
                                page[i] = app
                                placed = true
                                break
                            }
                        }
                        if (placed) break
                    }
                    if (!placed) {
                        // 需要新页面
                        val newPage = mutableMapOf<Int, GridItem>(0 to app)
                        allPages.add(newPage)
                    }
                }
            }
        } else {
            // 默认布局：使用分页分配
            val pages = distributeItemsToPages(defaultApps, defaultWidgets)
            for (page in pages) {
                allPages.add(page.toMutableMap())
            }
        }

        pageCount = allPages.size.coerceAtLeast(1)
    }

    fun saveCurrentLayout() {
        val items = mutableListOf<LayoutItem>()
        allPages.forEachIndexed { pageIdx, page ->
            val areaName = if (pageIdx == 0) "grid" else "grid_$pageIdx"
            page.forEach { (ci, item) ->
                items.add(LayoutItem(appId = item.id, position = ci, area = areaName))
            }
        }
        dockApps.forEachIndexed { i, app -> items.add(LayoutItem(appId = app.id, position = i, area = "dock")) }
        layoutDbHelper.saveLayout(items)
    }

    fun getCellFromGlobal(gx: Float, gy: Float): Int {
        if (gridAreaSize.width == 0 || gridAreaSize.height == 0) return -1
        val lx = gx - gridAreaOffset.x; val ly = gy - gridAreaOffset.y
        if (lx < 0 || ly < 0 || lx > gridAreaSize.width || ly > gridAreaSize.height) return -1
        val col = (lx / (gridAreaSize.width.toFloat() / GRID_COLUMNS)).toInt().coerceIn(0, GRID_COLUMNS - 1)
        val row = (ly / (gridAreaSize.height.toFloat() / GRID_ROWS)).toInt().coerceIn(0, GRID_ROWS - 1)
        return row * GRID_COLUMNS + col
    }

    fun isOverDock(gx: Float, gy: Float): Boolean {
        if (dockAreaSize.width == 0) return false
        val lx = gx - dockAreaOffset.x; val ly = gy - dockAreaOffset.y
        return lx in 0f..dockAreaSize.width.toFloat() && ly in 0f..dockAreaSize.height.toFloat()
    }

    if (isEditMode) BackHandler { isEditMode = false; saveCurrentLayout() }

    // Pager state
    val pagerState = rememberPagerState(pageCount = { pageCount })
    val coroutineScope = rememberCoroutineScope()

    // 自动翻页逻辑
    var autoScrollJob by remember { mutableStateOf<Job?>(null) }
    fun handleAutoScroll(globalX: Float) {
        val displayMetrics = context.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels.toFloat()
        val edgeThreshold = 100f // 固定 100px 边缘
        val currentPage = pagerState.currentPage

        if (globalX < edgeThreshold && currentPage > 0) {
            if (autoScrollJob?.isActive != true) {
                autoScrollJob = coroutineScope.launch {
                    delay(500)
                    if (pagerState.currentPage > 0) {
                        pagerState.scrollToPage(pagerState.currentPage - 1)
                    }
                }
            }
        } else if (globalX > screenWidth - edgeThreshold && currentPage < pageCount - 1) {
            if (autoScrollJob?.isActive != true) {
                autoScrollJob = coroutineScope.launch {
                    delay(500)
                    if (pagerState.currentPage < pageCount - 1) {
                        pagerState.scrollToPage(pagerState.currentPage + 1)
                    }
                }
            }
        } else {
            autoScrollJob?.cancel()
            autoScrollJob = null
        }
    }

    // 全局手势处理
    Box(modifier = Modifier.fillMaxSize()
        .pointerInput(Unit) {
            awaitEachGesture {
                val down = awaitFirstDown()
                down.consume()

                // Hit Testing
                val touchX = down.position.x
                val touchY = down.position.y
                
                var startItem: GridItem? = null
                var startCellIndex = -1
                var startPageIndex = -1
                var startDockIndex = -1
                var isDockItem = false

                // 检查 Dock
                if (isOverDock(touchX, touchY)) {
                    val relX = touchX - dockAreaOffset.x
                    // 粗略估算 index，假设间隔 85f
                    val index = ((relX) / 85f).toInt()
                    if (index in 0 until dockApps.size) {
                        if (touchY >= dockAreaOffset.y && touchY <= dockAreaOffset.y + dockAreaSize.height) {
                            startItem = dockApps[index]
                            startDockIndex = index
                            isDockItem = true
                        }
                    }
                }
                
                // 检查 Grid (如果不在 Dock)
                if (startItem == null) {
                    val cell = getCellFromGlobal(touchX, touchY)
                    if (cell != -1) {
                        val currentPageGrid = if (pagerState.currentPage < allPages.size) allPages[pagerState.currentPage] else null
                        if (currentPageGrid != null) {
                            // 找到占据该 cell 的 item (考虑 span)
                            for ((pos, item) in currentPageGrid) {
                                val itemRow = pos / GRID_COLUMNS
                                val itemCol = pos % GRID_COLUMNS
                                val targetRow = cell / GRID_COLUMNS
                                val targetCol = cell % GRID_COLUMNS
                                
                                if (targetRow >= itemRow && targetRow < itemRow + item.spanY &&
                                    targetCol >= itemCol && targetCol < itemCol + item.spanX) {
                                    startItem = item
                                    startCellIndex = pos
                                    startPageIndex = pagerState.currentPage
                                    break
                                }
                            }
                        }
                    }
                }

                if (startItem != null) {
                    var longPressTriggered = false
                    var tapDetected = false
                    try {
                        withTimeout(viewConfiguration.longPressTimeoutMillis) {
                            while (true) {
                                val event = awaitPointerEvent()
                                if (event.changes.all { it.changedToUp() }) {
                                    event.changes.forEach { it.consume() }
                                    tapDetected = true
                                    break
                                }
                                val movedTooMuch = event.changes.any {
                                    val change = it.positionChange()
                                    change.x * change.x + change.y * change.y > 100
                                }
                                if (movedTooMuch) break
                            }
                        }
                    } catch (_: PointerEventTimeoutCancellationException) {
                        longPressTriggered = true
                    }

                    if (tapDetected) {
                        if (!isEditMode && startItem is AppIcon) {
                            if (startItem!!.id == "settings") {
                                onSettingsClick()
                            } else {
                                if (isActivated) {
                                    if (startItem!!.id == "chat") onChatClick()
                                } else {
                                    onActivationAlert()
                                }
                            }
                        } else if (isEditMode) {
                            isEditMode = false; saveCurrentLayout()
                        }
                    } else if (longPressTriggered) {
                        if (!isEditMode) isEditMode = true
                        val startPos = down.position
                        draggedItem = startItem
                        if (isDockItem) {
                            dragSource = "dock"
                            dragSourceDockIndex = startDockIndex
                            // 修正初始 offset
                            dragOverlayX = touchX
                            dragOverlayY = touchY
                        } else {
                            dragSource = "grid"
                            dragSourceCellIndex = startCellIndex
                            dragSourcePageIndex = startPageIndex
                            dragOverlayX = touchX
                            dragOverlayY = touchY
                        }

                        try {
                            while (true) {
                                val event = awaitPointerEvent()
                                val allUp = event.changes.all { it.changedToUp() }
                                event.changes.forEach { change ->
                                    if (!change.changedToUp()) {
                                        val dragAmount = change.positionChange()
                                        change.consume()
                                        dragOverlayX += dragAmount.x
                                        dragOverlayY += dragAmount.y

                                        handleAutoScroll(dragOverlayX)

                                        if (!isOverDock(dragOverlayX, dragOverlayY)) {
                                            val rawCell = getCellFromGlobal(dragOverlayX, dragOverlayY)
                                            if (rawCell >= 0) {
                                                val targetCol = rawCell % GRID_COLUMNS
                                                val targetRow = rawCell / GRID_COLUMNS
                                                val safeCol = targetCol.coerceAtMost(GRID_COLUMNS - startItem!!.spanX)
                                                val safeRow = targetRow.coerceAtMost(GRID_ROWS - startItem!!.spanY)
                                                highlightCellIndex = safeRow * GRID_COLUMNS + safeCol
                                            } else { highlightCellIndex = -1 }
                                        } else { highlightCellIndex = -1 }
                                    } else {
                                        autoScrollJob?.cancel(); autoScrollJob = null
                                    }
                                }
                                if (allUp) break
                            }

                            // 放置逻辑
                            draggedItem?.let { currentItem ->
                                val currentPage = pagerState.currentPage
                                val sourcePage = if (dragSourcePageIndex < allPages.size && dragSourcePageIndex >= 0) allPages[dragSourcePageIndex] else null
                                val targetPage = if (currentPage < allPages.size) allPages[currentPage] else null

                                if (isOverDock(dragOverlayX, dragOverlayY) && currentItem is AppIcon) {
                                    if (dockApps.size < maxDockApps) {
                                        if (dragSource == "grid") sourcePage?.remove(dragSourceCellIndex)
                                        else if (dragSource == "dock") dockApps.removeAt(dragSourceDockIndex)
                                        dockApps.add(currentItem)
                                    } else if (dragSource == "dock") {
                                        // 简单实现：放回原位，暂不支持 Dock 内排序
                                        // dockApps.removeAt(dragSourceDockIndex)
                                        // dockApps.add(...)
                                    }
                                } else if (highlightCellIndex != -1 && targetPage != null) {
                                    val targetCell = highlightCellIndex
                                    
                                    // 统一放置逻辑 (Grid -> Grid, Dock -> Grid)
                                    // 1. 获取目标区域冲突
                                    val targetCells = mutableListOf<Int>()
                                    for (r in 0 until currentItem.spanY) {
                                        for (c in 0 until currentItem.spanX) {
                                            targetCells.add(targetCell + r * GRID_COLUMNS + c)
                                        }
                                    }
                                    val conflictingItems = targetCells.mapNotNull { targetPage[it] }.filter { it.id != currentItem.id }.distinct()
                                    
                                    // 2. 判断是否可放置/交换
                                    var canPlace = false
                                    var needSwap = false
                                    
                                    if (conflictingItems.isEmpty()) {
                                        // 目标区域为空 (注意：如果源和目标重叠，需排除源占用的位置)
                                        val ignoreSource = if (dragSource == "grid" && dragSourcePageIndex == currentPage) dragSourceCellIndex else null
                                        if (checkOccupancy(targetPage, targetCell, currentItem.spanX, currentItem.spanY, ignoreSource)) {
                                            canPlace = true
                                        }
                                    } else if (currentItem.spanX > 1 && conflictingItems.all { it.spanX == 1 && it.spanY == 1 }) {
                                        // Widget 换 Apps
                                        canPlace = true
                                        needSwap = true
                                    } else if (conflictingItems.size == 1 && conflictingItems[0].spanX == currentItem.spanX && conflictingItems[0].spanY == currentItem.spanY) {
                                        // 同尺寸互换
                                        canPlace = true
                                        needSwap = true
                                    }
                                    
                                    if (canPlace) {
                                        // 移除源
                                        if (dragSource == "grid") sourcePage?.remove(dragSourceCellIndex)
                                        else dockApps.removeAt(dragSourceDockIndex)
                                        
                                        // 移除目标区域旧 items
                                        if (needSwap) {
                                            targetCells.forEach { targetPage.remove(it) }
                                            // 移除可能存在的残留 key
                                            for ((k, v) in targetPage.toMap()) {
                                                if (conflictingItems.any { it.id == v.id }) targetPage.remove(k)
                                            }
                                        }
                                        
                                        // 放置新 item
                                        targetPage[targetCell] = currentItem
                                        
                                        // 处理被挤出的 items (回填源位置)
                                        if (needSwap) {
                                            if (dragSource == "grid" && dragSourcePageIndex == currentPage) {
                                                // 同页交换：计算源区域不重叠部分
                                                val sourceCells = mutableListOf<Int>()
                                                for (r in 0 until currentItem.spanY) {
                                                    for (c in 0 until currentItem.spanX) {
                                                        sourceCells.add(dragSourceCellIndex + r * GRID_COLUMNS + c)
                                                    }
                                                }
                                                val targetCellsSet = targetCells.toSet()
                                                val availableCells = sourceCells.filter { it !in targetCellsSet }
                                                
                                                var idx = 0
                                                // 优先填入源区域空位
                                                for (cell in availableCells) {
                                                    if (idx < conflictingItems.size && cell < TOTAL_CELLS) {
                                                        targetPage[cell] = conflictingItems[idx]
                                                        idx++
                                                    }
                                                }
                                                // 溢出找全页空位
                                                while (idx < conflictingItems.size) {
                                                    val empty = (0 until TOTAL_CELLS).firstOrNull { checkOccupancy(targetPage, it, 1, 1, null) }
                                                    if (empty != null) targetPage[empty] = conflictingItems[idx]
                                                    idx++
                                                }
                                            } else {
                                                // 跨页或Dock交换：找当前页空位
                                                var idx = 0
                                                while (idx < conflictingItems.size) {
                                                    val empty = (0 until TOTAL_CELLS).firstOrNull { checkOccupancy(targetPage, it, 1, 1, null) }
                                                    if (empty != null) targetPage[empty] = conflictingItems[idx]
                                                    idx++
                                                }
                                            }
                                        }
                                    }
                                }
                                saveCurrentLayout()
                            }
                        } finally {
                            autoScrollJob?.cancel(); autoScrollJob = null
                            draggedItem = null; highlightCellIndex = -1; dragSourceDockIndex = -1; dragSource = "grid"
                        }
                    }
                }
            }
        }
    ) {
        // 背景
        if (homeWallpaperPath != null) {
            val bitmap = remember(homeWallpaperPath) { android.graphics.BitmapFactory.decodeFile(homeWallpaperPath) }
            if (bitmap != null) Image(bitmap = bitmap.asImageBitmap(), contentDescription = "壁纸",
                modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            else Box(modifier = Modifier.fillMaxSize().background(Color.Black))
        } else Box(modifier = Modifier.fillMaxSize().background(Color.Black))

        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            // 注意：拖拽浮层已移至顶层 Box，不在 Column 内

            // HorizontalPager - 多页网格区域
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth().weight(1f),
                userScrollEnabled = draggedItem == null // 拖拽时禁止翻页
            ) { pageIndex ->
                if (pageIndex < allPages.size) {
                    val currentPageGrid = allPages[pageIndex]

                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 20.dp)
                            .then(
                                if (pageIndex == pagerState.currentPage) {
                                    Modifier.onGloballyPositioned {
                                        gridAreaOffset = it.positionInRoot()
                                        gridAreaSize = it.size
                                    }
                                } else Modifier
                            )
                            // 移除 GridItem 上的 pointerInput，统一到外层处理
                    ) {
                        val density = LocalDensity.current
                        val twPx = constraints.maxWidth; val thPx = constraints.maxHeight
                        val cwPx = twPx / GRID_COLUMNS; val chPx = thPx / GRID_ROWS

                        // 高亮目标格子
                        if (highlightCellIndex in 0 until TOTAL_CELLS && draggedItem != null && pageIndex == pagerState.currentPage) {
                            val hr = highlightCellIndex / GRID_COLUMNS; val hc = highlightCellIndex % GRID_COLUMNS
                            val spanX = draggedItem!!.spanX
                            val spanY = draggedItem!!.spanY

                            if (hc + spanX <= GRID_COLUMNS && hr + spanY <= GRID_ROWS) {
                                Box(modifier = Modifier
                                    .offset { IntOffset(hc * cwPx + 4, hr * chPx + 4) }
                                    .width(with(density) { (cwPx * spanX - 8).toDp() })
                                    .height(with(density) { (chPx * spanY - 8).toDp() })
                                    .border(2.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(12.dp)))
                            }
                        }

                        // 渲染网格内容
                        currentPageGrid.forEach { (cellIndex, item) ->
                            val isDraggedFromHere = draggedItem?.id == item.id && dragSource == "grid" && dragSourcePageIndex == pageIndex

                            val row = cellIndex / GRID_COLUMNS; val col = cellIndex % GRID_COLUMNS
                            val infT = rememberInfiniteTransition(label = "w_${item.id}")
                            val wAngle by infT.animateFloat(
                                initialValue = if (cellIndex % 2 == 0) -1.5f else 1.5f,
                                targetValue = if (cellIndex % 2 == 0) 1.5f else -1.5f,
                                animationSpec = infiniteRepeatable(tween(150 + (cellIndex % 3) * 50, easing = LinearEasing),
                                    RepeatMode.Reverse), label = "wa_${item.id}")

                            val itemWidth = cwPx * item.spanX
                            val itemHeight = chPx * item.spanY

                            Box(modifier = Modifier
                                .offset { IntOffset(col * cwPx, row * chPx) }
                                .width(with(density) { itemWidth.toDp() }).height(with(density) { itemHeight.toDp() })
                                .graphicsLayer {
                                    if (isEditMode) rotationZ = wAngle
                                    if (isDraggedFromHere) alpha = 0f
                                }
                                // pointerInput 已移除
                                , contentAlignment = Alignment.Center
                            ) {
                                if (item is WidgetItem) {
                                    WidgetContent(widgetType = item.widgetType, modifier = Modifier.fillMaxSize().padding(8.dp))
                                } else if (item is AppIcon) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    ) {
                                        Box(modifier = Modifier.size(60.dp), contentAlignment = Alignment.Center) {
                                            if (item.useImage) {
                                                val resId = context.resources.getIdentifier(item.icon, "drawable", context.packageName)
                                                if (resId != 0) Image(painter = androidx.compose.ui.res.painterResource(id = resId),
                                                    contentDescription = item.name, modifier = Modifier.size(60.dp))
                                            } else Text(item.icon, fontSize = 48.sp)
                                        }
                                        Spacer(modifier = Modifier.height(5.dp))
                                        Text(item.name, color = Color.White, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 页面指示器（小圆点）
            if (pageCount > 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(pageCount) { index ->
                        val isSelected = pagerState.currentPage == index
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .size(if (isSelected) 8.dp else 6.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) Color.White
                                    else Color.White.copy(alpha = 0.4f)
                                )
                        )
                    }
                }
            }

            // Dock 栏
            Box(
                modifier = Modifier
                    .padding(bottom = 20.dp, start = 15.dp, end = 15.dp)
                    .fillMaxWidth().height(90.dp).clip(RoundedCornerShape(30.dp))
                    .zIndex(20f)
                    .onGloballyPositioned { dockAreaOffset = it.positionInRoot(); dockAreaSize = it.size }
            ) {
                val isDraggingOverDock = draggedItem != null && dragSource == "grid" &&
                    isOverDock(dragOverlayX, dragOverlayY) && dockApps.size < maxDockApps && draggedItem is AppIcon
                Box(modifier = Modifier.fillMaxSize().background(
                    if (isDraggingOverDock) Color(0xFF007AFF).copy(alpha = 0.4f) else Color.White.copy(alpha = 0.3f)
                ).blur(20.dp))

                Row(modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = if (dockApps.isEmpty()) Arrangement.Center else Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically) {
                    if (dockApps.isEmpty() && !isEditMode) Text("长按拖入应用", color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp)
                    if (dockApps.isEmpty() && isEditMode) Text("拖拽应用到此处", color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp)

                    dockApps.forEachIndexed { dockIndex, app ->
                        val isDraggedFromDock = draggedItem?.id == app.id && dragSource == "dock"
                        val dwt = rememberInfiniteTransition(label = "dw_$dockIndex")
                        val dwa by dwt.animateFloat(
                            initialValue = if (dockIndex % 2 == 0) -1.5f else 1.5f,
                            targetValue = if (dockIndex % 2 == 0) 1.5f else -1.5f,
                            animationSpec = infiniteRepeatable(tween(150 + (dockIndex % 3) * 50, easing = LinearEasing),
                                RepeatMode.Reverse), label = "dwa_$dockIndex")

                        Box(modifier = Modifier.size(60.dp)
                            .graphicsLayer {
                                if (isEditMode) rotationZ = dwa
                                if (isDraggedFromDock) alpha = 0f
                            }
                            // pointerInput 已移除，统一到外层处理
                            , contentAlignment = Alignment.Center
                        ) {
                            if (app.useImage) {
                                val resId = context.resources.getIdentifier(app.icon, "drawable", context.packageName)
                                if (resId != 0) Image(painter = androidx.compose.ui.res.painterResource(id = resId),
                                    contentDescription = app.name, modifier = Modifier.size(60.dp))
                            } else Text(app.icon, fontSize = 48.sp)
                        }
                    }
                }
            }

            // Home Indicator
            Box(modifier = Modifier
                .padding(bottom = 8.dp)
                .width(120.dp).height(5.dp).clip(RoundedCornerShape(5.dp))
                .background(Color.White.copy(alpha = 0.8f))
                .align(Alignment.CenterHorizontally))
        }

        // 拖拽浮动覆盖层 - 在顶层 Box 中，不在 Column 内，避免布局重排导致闪屏
        if (draggedItem != null) {
            val density = LocalDensity.current
            // 根据网格尺寸动态计算Widget浮层大小
            val cellWidthDp = if (gridAreaSize.width > 0) with(density) { (gridAreaSize.width / GRID_COLUMNS).toDp() } else 75.dp
            val cellHeightDp = if (gridAreaSize.height > 0) with(density) { (gridAreaSize.height / GRID_ROWS).toDp() } else 90.dp
            val itemWidth = cellWidthDp * draggedItem!!.spanX
            val itemHeight = cellHeightDp * draggedItem!!.spanY

            Box(
                modifier = Modifier.fillMaxSize().zIndex(10000f),
                contentAlignment = Alignment.TopStart
            ) {
                Box(
                    modifier = Modifier
                        .offset { IntOffset((dragOverlayX - 30 * density.density).roundToInt(),
                            (dragOverlayY - 30 * density.density).roundToInt()) }
                        .size(itemWidth, itemHeight)
                        .graphicsLayer { scaleX = 1.15f; scaleY = 1.15f; alpha = 0.85f },
                    contentAlignment = Alignment.Center
                ) {
                    if (draggedItem is WidgetItem) {
                        WidgetContent((draggedItem as WidgetItem).widgetType, modifier = Modifier.fillMaxSize())
                    } else {
                        val app = draggedItem as AppIcon
                        if (app.useImage) {
                            val resId = context.resources.getIdentifier(app.icon, "drawable", context.packageName)
                            if (resId != 0) Image(painter = androidx.compose.ui.res.painterResource(id = resId),
                                contentDescription = app.name, modifier = Modifier.size(60.dp))
                        } else Text(app.icon, fontSize = 48.sp)
                    }
                }
            }
        }
    }
}

fun checkOccupancy(positions: Map<Int, GridItem>, startCell: Int, spanX: Int, spanY: Int, ignoreCell: Int?): Boolean {
    val startRow = startCell / GRID_COLUMNS
    val startCol = startCell % GRID_COLUMNS

    // Check boundary
    if (startCol + spanX > GRID_COLUMNS || startRow + spanY > GRID_ROWS) return false

    // Check intersection with existing items
    for (r in 0 until spanY) {
        for (c in 0 until spanX) {
            for ((pos, item) in positions) {
                if (pos == ignoreCell) continue
                val itemRow = pos / GRID_COLUMNS
                val itemCol = pos % GRID_COLUMNS
                if (startRow + r >= itemRow && startRow + r < itemRow + item.spanY &&
                    startCol + c >= itemCol && startCol + c < itemCol + item.spanX) {
                    return false
                }
            }
        }
    }
    return true
}

@Composable
fun DisplaySettingsScreen(wallpaperDbHelper: WallpaperDbHelper, onBack: () -> Unit, onWallpaperChanged: () -> Unit) {
    BackHandler { onBack() }
    val isDark = isSystemInDarkTheme()
    val bg = if (isDark) Color(0xFF1C1C1E) else Color(0xFFF2F2F7)
    val card = if (isDark) Color(0xFF2C2C2E) else Color.White
    val txt = if (isDark) Color.White else Color.Black
    var lockPreviewPath by remember { mutableStateOf(wallpaperDbHelper.getWallpaperFilePath(WallpaperType.LOCK)) }
    var homePreviewPath by remember { mutableStateOf(wallpaperDbHelper.getWallpaperFilePath(WallpaperType.HOME)) }
    val lockLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            wallpaperDbHelper.copyImageToStorage(it)?.let { fn ->
                wallpaperDbHelper.saveWallpaper(WallpaperType.LOCK, fn)
                lockPreviewPath = wallpaperDbHelper.getWallpaperFilePath(WallpaperType.LOCK); onWallpaperChanged()
            }
        }
    }
    val homeLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            wallpaperDbHelper.copyImageToStorage(it)?.let { fn ->
                wallpaperDbHelper.saveWallpaper(WallpaperType.HOME, fn)
                homePreviewPath = wallpaperDbHelper.getWallpaperFilePath(WallpaperType.HOME); onWallpaperChanged()
            }
        }
    }
    Column(modifier = Modifier.fillMaxSize().background(bg).statusBarsPadding()) {
        Box(modifier = Modifier.fillMaxWidth().height(56.dp).background(card).padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart) {
            Text("返回", color = Color(0xFF007AFF), modifier = Modifier.clickable { onBack() })
            Text("显示设置", modifier = Modifier.align(Alignment.Center), fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = txt)
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text("锁屏壁纸", modifier = Modifier.padding(horizontal = 26.dp, vertical = 8.dp), fontSize = 13.sp, color = Color.Gray)
        WallpaperSettingCard("锁屏壁纸设置", lockPreviewPath, card, txt) { lockLauncher.launch("image/*") }
        Spacer(modifier = Modifier.height(20.dp))
        Text("桌面壁纸", modifier = Modifier.padding(horizontal = 26.dp, vertical = 8.dp), fontSize = 13.sp, color = Color.Gray)
        WallpaperSettingCard("桌面壁纸设置", homePreviewPath, card, txt) { homeLauncher.launch("image/*") }
    }
}

@Composable
fun WallpaperSettingCard(title: String, previewPath: String?, cardColor: Color, textColor: Color, onSelectImage: () -> Unit) {
    Box(modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(RoundedCornerShape(10.dp))
        .background(cardColor).clickable(onClick = onSelectImage).padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 16.sp, color = textColor)
                Text(if (previewPath != null) "已设置，点击更换" else "点击从相册选择图片", fontSize = 12.sp, color = Color.Gray)
            }
            if (previewPath != null) {
                val bitmap = remember(previewPath) { android.graphics.BitmapFactory.decodeFile(previewPath) }
                if (bitmap != null) Image(bitmap = bitmap.asImageBitmap(), contentDescription = title,
                    modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
            } else {
                Box(modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp))
                    .background(Color.Gray.copy(alpha = 0.3f)), contentAlignment = Alignment.Center) {
                    Text("🖼️", fontSize = 24.sp)
                }
            }
        }
    }
}