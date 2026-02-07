
package com.WangWangPhone.ui

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.input.pointer.pointerInput
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
import com.WangWangPhone.core.WallpaperDbHelper
import com.WangWangPhone.core.WallpaperType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

data class AppIcon(
    val id: String, val name: String, val icon: String,
    val color: Brush, val useImage: Boolean = false
)

fun getDefaultApps(isDark: Boolean): List<AppIcon> = listOf(
    AppIcon("phone", "电话", "📞", Brush.linearGradient(listOf(Color(0xFFFF9A9E), Color(0xFFFECFEF)))),
    AppIcon("chat", "聊天", "💬", Brush.linearGradient(listOf(Color(0xFF07C160), Color(0xFF06AD56)))),
    AppIcon("safari", "Safari", "🧭", Brush.linearGradient(listOf(Color(0xFF84FAB0), Color(0xFF8FD3F4)))),
    AppIcon("music", "音乐", "🎵", Brush.linearGradient(listOf(Color(0xFFF6D365), Color(0xFFFDA085)))),
    AppIcon("camera", "相机", "📷", Brush.linearGradient(listOf(Color.White, Color.LightGray))),
    AppIcon("calendar", "日历", "📅", Brush.linearGradient(listOf(Color.White, Color.LightGray))),
    AppIcon("settings", "设置", if (isDark) "ic_settings_dark" else "ic_settings_light",
        Brush.linearGradient(listOf(Color.White, Color.LightGray)), useImage = true),
    AppIcon("wangwang", "汪汪", "🐶", Brush.linearGradient(listOf(Color.White, Color.LightGray)))
)

const val GRID_COLUMNS = 4
const val GRID_ROWS = 7
const val TOTAL_CELLS = GRID_COLUMNS * GRID_ROWS

data class WeatherInfo(val temp: String, val description: String, val icon: String, val range: String)

suspend fun fetchLocation(): String { delay(500); return "广州" }
suspend fun fetchWeather(city: String): WeatherInfo {
    delay(500); return WeatherInfo("25°", "多云", "⛅", "最高 29° 最低 21°")
}

@Composable
fun WidgetsSection(
    isEditMode: Boolean = false,
    widgetOrder: List<String> = listOf("clock", "weather"),
    onWidgetOrderChanged: (List<String>) -> Unit = {}
) {
    var city by remember { mutableStateOf("...") }
    var weather by remember { mutableStateOf<WeatherInfo?>(null) }
    var widgetDragIndex by remember { mutableStateOf(-1) }
    var widgetDragOffsetX by remember { mutableStateOf(0f) }
    var widgetDragOffsetY by remember { mutableStateOf(0f) }

    LaunchedEffect(Unit) {
        city = fetchLocation()
        if (city.isNotEmpty()) weather = fetchWeather(city)
    }

    val density = LocalDensity.current

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(15.dp)
    ) {
        widgetOrder.forEachIndexed { index, widgetId ->
            val isDragged = widgetDragIndex == index
            val wiggleTransition = rememberInfiniteTransition(label = "ww_$index")
            val wiggleAngle by wiggleTransition.animateFloat(
                initialValue = if (index % 2 == 0) -1.5f else 1.5f,
                targetValue = if (index % 2 == 0) 1.5f else -1.5f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 200, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ), label = "wa_$index"
            )
            Box(
                modifier = Modifier.weight(1f).zIndex(if (isDragged) 10f else 0f)
                    .graphicsLayer {
                        if (isEditMode) rotationZ = if (isDragged) 0f else wiggleAngle
                        scaleX = if (isDragged) 1.05f else 1f
                        scaleY = if (isDragged) 1.05f else 1f
                        alpha = if (isDragged) 0.85f else 1f
                        if (isDragged) { translationX = widgetDragOffsetX; translationY = widgetDragOffsetY }
                    }
                    .pointerInput(isEditMode, index) {
                        if (isEditMode) {
                            detectDragGestures(
                                onDragStart = { widgetDragIndex = index; widgetDragOffsetX = 0f; widgetDragOffsetY = 0f },
                                onDrag = { change, dragAmount ->
                                    change.consume(); widgetDragOffsetX += dragAmount.x; widgetDragOffsetY += dragAmount.y
                                },
                                onDragEnd = {
                                    val threshold = with(density) { 60.dp.toPx() }
                                    if (kotlin.math.abs(widgetDragOffsetX) > threshold && widgetOrder.size == 2) {
                                        onWidgetOrderChanged(widgetOrder.reversed())
                                    }
                                    widgetDragIndex = -1; widgetDragOffsetX = 0f; widgetDragOffsetY = 0f
                                },
                                onDragCancel = { widgetDragIndex = -1; widgetDragOffsetX = 0f; widgetDragOffsetY = 0f }
                            )
                        }
                    }
            ) {
                if (widgetId == "clock") ClockWidget(city = city, modifier = Modifier.fillMaxWidth())
                else WeatherWidget(city = city, weather = weather, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
fun ClockWidget(city: String, modifier: Modifier = Modifier) {
    var currentTime by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) { while (true) { currentTime = LocalDateTime.now(); delay(1000) } }
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    val dateFormatter = DateTimeFormatter.ofPattern("M月d日 EEEE", Locale.CHINA)
    Box(modifier = modifier.height(150.dp).clip(RoundedCornerShape(20.dp))
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
    Box(modifier = modifier.height(150.dp).clip(RoundedCornerShape(20.dp))
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
    onNavigateToActivation: () -> Unit, onNavigateToDisplay: () -> Unit
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
    val context = LocalContext.current
    val licenseManager = remember { LicenseManager.getInstance(context) }
    val wallpaperDbHelper = remember { WallpaperDbHelper(context) }
    var isActivated by remember { mutableStateOf(licenseManager.isActivated()) }
    var expiryDate by remember { mutableStateOf(licenseManager.getExpirationDateString()) }
    var lockWallpaperPath by remember { mutableStateOf(wallpaperDbHelper.getWallpaperFilePath(WallpaperType.LOCK)) }
    var homeWallpaperPath by remember { mutableStateOf(wallpaperDbHelper.getWallpaperFilePath(WallpaperType.HOME)) }
    val isDark = isSystemInDarkTheme()
    Box(modifier = Modifier.fillMaxSize()) {
        HomeScreenContent(isDark = isDark, onSettingsClick = { showSettings = true },
            onChatClick = { showChatApp = true }, homeWallpaperPath = homeWallpaperPath)
        if (showSettings) SettingsScreen(isActivated = isActivated, expiryDate = expiryDate,
            onBack = { showSettings = false }, onNavigateToActivation = { showActivation = true },
            onNavigateToDisplay = { showDisplaySettings = true })
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

@Composable
fun HomeScreenContent(isDark: Boolean, onSettingsClick: () -> Unit, onChatClick: () -> Unit = {}, homeWallpaperPath: String? = null) {
    val context = LocalContext.current
    val layoutDbHelper = remember { LayoutDbHelper(context) }
    val defaultApps = remember(isDark) { getDefaultApps(isDark) }

    // 4x6 网格: cellIndex -> AppIcon (稀疏字典)
    val gridPositions = remember { mutableStateMapOf<Int, AppIcon>() }
    val dockApps = remember { mutableStateListOf<AppIcon>() }
    val maxDockApps = 4
    val widgetOrder = remember { mutableStateListOf("clock", "weather") }
    var isEditMode by remember { mutableStateOf(false) }

    // 拖拽状态
    var draggedApp by remember { mutableStateOf<AppIcon?>(null) }
    var dragSource by remember { mutableStateOf("grid") }
    var dragSourceCellIndex by remember { mutableStateOf(-1) }
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
        gridPositions.clear(); dockApps.clear()
        if (savedLayout.isNotEmpty()) {
            savedLayout.filter { it.area == "grid" }.forEach { li ->
                defaultApps.find { it.id == li.appId }?.let { gridPositions[li.position.coerceIn(0, TOTAL_CELLS - 1)] = it }
            }
            savedLayout.filter { it.area == "dock" }.sortedBy { it.position }.forEach { li ->
                defaultApps.find { it.id == li.appId }?.let { dockApps.add(it) }
            }
            val wi = savedLayout.filter { it.area == "widget" }.sortedBy { it.position }
            if (wi.isNotEmpty()) { widgetOrder.clear(); widgetOrder.addAll(wi.map { it.appId }) }
            val allSavedIds = savedLayout.map { it.appId }.toSet()
            for (app in defaultApps) {
                if (app.id !in allSavedIds) {
                    val empty = (0 until TOTAL_CELLS).firstOrNull { it !in gridPositions }
                    if (empty != null) gridPositions[empty] = app
                }
            }
        } else {
            defaultApps.forEachIndexed { i, app -> if (i < TOTAL_CELLS) gridPositions[i] = app }
        }
    }

    fun saveCurrentLayout() {
        val items = mutableListOf<LayoutItem>()
        gridPositions.forEach { (ci, app) -> items.add(LayoutItem(appId = app.id, position = ci, area = "grid")) }
        dockApps.forEachIndexed { i, app -> items.add(LayoutItem(appId = app.id, position = i, area = "dock")) }
        widgetOrder.forEachIndexed { i, wid -> items.add(LayoutItem(appId = wid, position = i, area = "widget")) }
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

    Box(modifier = Modifier.fillMaxSize()) {
        // 背景
        if (homeWallpaperPath != null) {
            val bitmap = remember(homeWallpaperPath) { android.graphics.BitmapFactory.decodeFile(homeWallpaperPath) }
            if (bitmap != null) Image(bitmap = bitmap.asImageBitmap(), contentDescription = "壁纸",
                modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            else Box(modifier = Modifier.fillMaxSize().background(Color.Black))
        } else Box(modifier = Modifier.fillMaxSize().background(Color.Black))

        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            WidgetsSection(isEditMode = isEditMode, widgetOrder = widgetOrder.toList(),
                onWidgetOrderChanged = { widgetOrder.clear(); widgetOrder.addAll(it); saveCurrentLayout() })

            // 4x7 网格
            BoxWithConstraints(
                modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 16.dp)
                    .onGloballyPositioned { gridAreaOffset = it.positionInRoot(); gridAreaSize = it.size }
                    // 点击空白处退出编辑模式
                    .clickable(
                        indication = null,
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                    ) {
                        if (isEditMode) { isEditMode = false; saveCurrentLayout() }
                    }
            ) {
                val density = LocalDensity.current
                val twPx = constraints.maxWidth; val thPx = constraints.maxHeight
                val cwPx = twPx / GRID_COLUMNS; val chPx = thPx / GRID_ROWS

                // 高亮目标格子
                if (highlightCellIndex in 0 until TOTAL_CELLS && draggedApp != null) {
                    val hr = highlightCellIndex / GRID_COLUMNS; val hc = highlightCellIndex % GRID_COLUMNS
                    Box(modifier = Modifier
                        .offset { IntOffset(hc * cwPx + 4, hr * chPx + 4) }
                        .width(with(density) { (cwPx - 8).toDp() })
                        .height(with(density) { (chPx - 8).toDp() })
                        .border(2.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(12.dp)))
                }

                // 渲染网格图标
                for ((cellIndex, app) in gridPositions) {
                    if (draggedApp?.id == app.id && dragSource == "grid") continue
                    val row = cellIndex / GRID_COLUMNS; val col = cellIndex % GRID_COLUMNS
                    val infT = rememberInfiniteTransition(label = "w_${app.id}")
                    val wAngle by infT.animateFloat(
                        initialValue = if (cellIndex % 2 == 0) -1.5f else 1.5f,
                        targetValue = if (cellIndex % 2 == 0) 1.5f else -1.5f,
                        animationSpec = infiniteRepeatable(tween(150 + (cellIndex % 3) * 50, easing = LinearEasing),
                            RepeatMode.Reverse), label = "wa_${app.id}")

                    Box(modifier = Modifier
                        .offset { IntOffset(col * cwPx, row * chPx) }
                        .width(with(density) { cwPx.toDp() }).height(with(density) { chPx.toDp() })
                        .graphicsLayer { if (isEditMode) rotationZ = wAngle }
                        .pointerInput(isEditMode, cellIndex, app.id) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = { offset ->
                                    if (!isEditMode) isEditMode = true
                                    draggedApp = app; dragSource = "grid"; dragSourceCellIndex = cellIndex
                                    dragOverlayX = gridAreaOffset.x + col * cwPx.toFloat() + offset.x
                                    dragOverlayY = gridAreaOffset.y + row * chPx.toFloat() + offset.y
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    dragOverlayX += dragAmount.x; dragOverlayY += dragAmount.y
                                    highlightCellIndex = if (isOverDock(dragOverlayX, dragOverlayY)) -1
                                        else getCellFromGlobal(dragOverlayX, dragOverlayY).let { if (it >= 0 && it != dragSourceCellIndex) it else -1 }
                                },
                                onDragEnd = {
                                    draggedApp?.let { currentApp ->
                                        if (isOverDock(dragOverlayX, dragOverlayY)) {
                                            if (dockApps.size < maxDockApps) { gridPositions.remove(dragSourceCellIndex); dockApps.add(currentApp) }
                                        } else {
                                            val tc = getCellFromGlobal(dragOverlayX, dragOverlayY)
                                            if (tc in 0 until TOTAL_CELLS) {
                                                // 自由摆放，不自动挤压其他图标，如果有图标则交换
                                                val existing = gridPositions[tc]
                                                if (tc != dragSourceCellIndex) {
                                                    gridPositions.remove(dragSourceCellIndex)
                                                    gridPositions[tc] = currentApp
                                                    if (existing != null) {
                                                        // 交换位置：原位置放入现有图标
                                                        gridPositions[dragSourceCellIndex] = existing
                                                    }
                                                }
                                            }
                                        }
                                        saveCurrentLayout()
                                    }
                                    draggedApp = null; highlightCellIndex = -1; dragSourceCellIndex = -1
                                },
                                onDragCancel = { draggedApp = null; highlightCellIndex = -1; dragSourceCellIndex = -1 }
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
                            Box(modifier = Modifier.size(60.dp), contentAlignment = Alignment.Center) {
                                if (app.useImage) {
                                    val resId = context.resources.getIdentifier(app.icon, "drawable", context.packageName)
                                    if (resId != 0) Image(painter = androidx.compose.ui.res.painterResource(id = resId),
                                        contentDescription = app.name, modifier = Modifier.size(60.dp))
                                } else Text(app.icon, fontSize = 48.sp)
                            }
                            Spacer(modifier = Modifier.height(5.dp))
                            Text(app.name, color = Color.White, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Dock 栏
        Box(
            modifier = Modifier.align(Alignment.BottomCenter)
                .padding(bottom = 20.dp, start = 15.dp, end = 15.dp)
                .fillMaxWidth().height(90.dp).clip(RoundedCornerShape(30.dp))
                .zIndex(20f)
                .onGloballyPositioned { dockAreaOffset = it.positionInRoot(); dockAreaSize = it.size }
        ) {
            val isDraggingOverDock = draggedApp != null && dragSource == "grid" &&
                isOverDock(dragOverlayX, dragOverlayY) && dockApps.size < maxDockApps
            Box(modifier = Modifier.fillMaxSize().background(
                if (isDraggingOverDock) Color(0xFF007AFF).copy(alpha = 0.4f) else Color.White.copy(alpha = 0.3f)
            ).blur(20.dp))

            Row(modifier = Modifier.fillMaxSize(),
                horizontalArrangement = if (dockApps.isEmpty()) Arrangement.Center else Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically) {
                if (dockApps.isEmpty() && !isEditMode) Text("长按拖入应用", color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp)
                if (dockApps.isEmpty() && isEditMode) Text("拖拽应用到此处", color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp)

                dockApps.forEachIndexed { dockIndex, app ->
                    if (draggedApp?.id == app.id && dragSource == "dock") {
                        Spacer(modifier = Modifier.size(60.dp)); return@forEachIndexed
                    }
                    val dwt = rememberInfiniteTransition(label = "dw_$dockIndex")
                    val dwa by dwt.animateFloat(
                        initialValue = if (dockIndex % 2 == 0) -1.5f else 1.5f,
                        targetValue = if (dockIndex % 2 == 0) 1.5f else -1.5f,
                        animationSpec = infiniteRepeatable(tween(150 + (dockIndex % 3) * 50, easing = LinearEasing),
                            RepeatMode.Reverse), label = "dwa_$dockIndex")

                    Box(modifier = Modifier.size(60.dp)
                        .graphicsLayer { if (isEditMode) rotationZ = dwa }
                        .pointerInput(isEditMode, dockIndex) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = { offset ->
                                    if (!isEditMode) isEditMode = true
                                    draggedApp = app; dragSource = "dock"; dragSourceDockIndex = dockIndex
                                    dragOverlayX = dockAreaOffset.x + offset.x + dockIndex * 85f
                                    dragOverlayY = dockAreaOffset.y + offset.y
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    dragOverlayX += dragAmount.x; dragOverlayY += dragAmount.y
                                    highlightCellIndex = if (!isOverDock(dragOverlayX, dragOverlayY)) {
                                        getCellFromGlobal(dragOverlayX, dragOverlayY).let { if (it >= 0) it else -1 }
                                    } else -1
                                },
                                onDragEnd = {
                                    draggedApp?.let { currentApp ->
                                        if (!isOverDock(dragOverlayX, dragOverlayY)) {
                                            val tc = getCellFromGlobal(dragOverlayX, dragOverlayY)
                                            if (tc in 0 until TOTAL_CELLS) {
                                                dockApps.removeAt(dragSourceDockIndex)
                                                val existing = gridPositions[tc]
                                                gridPositions[tc] = currentApp
                                                // 如果目标位置有应用，尝试找空位或放回 Dock
                                                if (existing != null) {
                                                    if (dockApps.size < maxDockApps) {
                                                        dockApps.add(dragSourceDockIndex.coerceAtMost(dockApps.size), existing)
                                                    } else {
                                                        // Dock 满了，尝试找网格空位
                                                        val empty = (0 until TOTAL_CELLS).firstOrNull { it !in gridPositions }
                                                        if (empty != null) gridPositions[empty] = existing
                                                    }
                                                }
                                            }
                                        } else {
                                             // 在 Dock 内部排序逻辑保持不变
                                        }
                                        saveCurrentLayout()
                                    }
                                    draggedApp = null; highlightCellIndex = -1; dragSourceDockIndex = -1; dragSource = "grid"
                                },
                                onDragCancel = { draggedApp = null; highlightCellIndex = -1; dragSourceDockIndex = -1; dragSource = "grid" }
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
                            if (resId != 0) Image(painter = androidx.compose.ui.res.painterResource(id = resId),
                                contentDescription = app.name, modifier = Modifier.size(60.dp))
                        } else Text(app.icon, fontSize = 48.sp)
                    }
                }
            }
        }

        // Home Indicator
        Box(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp)
            .width(120.dp).height(5.dp).clip(RoundedCornerShape(5.dp))
            .background(Color.White.copy(alpha = 0.8f)))

        // 拖拽浮动覆盖层 - 最高层级
        if (draggedApp != null) {
            val density = LocalDensity.current
            Box(
                modifier = Modifier.fillMaxSize().zIndex(10000f),
                contentAlignment = Alignment.TopStart
            ) {
                Box(
                    modifier = Modifier
                        .offset { IntOffset((dragOverlayX - 30 * density.density).roundToInt(),
                            (dragOverlayY - 30 * density.density).roundToInt()) }
                        .size(60.dp)
                        .graphicsLayer { scaleX = 1.15f; scaleY = 1.15f; alpha = 0.85f },
                    contentAlignment = Alignment.Center
                ) {
                    draggedApp?.let { app ->
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