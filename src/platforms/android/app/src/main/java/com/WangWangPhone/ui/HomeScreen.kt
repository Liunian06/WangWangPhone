package com.WangWangPhone.ui

import android.icu.text.Transliterator
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventTimeoutCancellationException
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.WangWangPhone.core.IconCustomizationDbHelper
import com.WangWangPhone.core.LayoutDbHelper
import com.WangWangPhone.core.LayoutItem
import com.WangWangPhone.core.LicenseManager
import com.WangWangPhone.core.LicenseResult
import com.WangWangPhone.core.UserProfileDbHelper
import com.WangWangPhone.core.WallpaperDbHelper
import com.WangWangPhone.core.WallpaperType
import com.WangWangPhone.core.WeatherCacheDbHelper
import com.WangWangPhone.core.WebWidgetDbHelper
import com.WangWangPhone.core.WebWidgetRecord
import com.WangWangPhone.core.webWidgetLayoutId
import com.WangWangPhone.core.widgetIdFromLayoutId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
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
    val widgetType: String, // "clock", "weather", "badge"
    override val spanX: Int = 2,
    override val spanY: Int = 2
) : GridItem {
    override val type = "widget"
}

data class WebWidgetGridItem(
    val widget: WebWidgetRecord,
    override val id: String = webWidgetLayoutId(widget.id),
    override val spanX: Int = widget.spanX,
    override val spanY: Int = widget.spanY
) : GridItem {
    override val type = "web_widget"
}

private const val BADGE_WIDGET_ID = "badge_widget"

fun getDefaultApps(isDark: Boolean): List<AppIcon> = listOf(
    AppIcon("phone", "电话", "📞", Brush.linearGradient(listOf(Color(0xFFFF9A9E), Color(0xFFFECFEF)))),
    AppIcon("chat", "聊天", if (isDark) "ic_messages_dark" else "ic_messages_light",
        Brush.linearGradient(listOf(Color(0xFF07C160), Color(0xFF06AD56))), useImage = true),
    AppIcon("safari", "Safari", "🧭", Brush.linearGradient(listOf(Color(0xFF84FAB0), Color(0xFF8FD3F4)))),
    AppIcon("music", "音乐", "🎵", Brush.linearGradient(listOf(Color(0xFFF6D365), Color(0xFFFDA085)))),
    AppIcon("camera", "相机", "📷", Brush.linearGradient(listOf(Color.White, Color.LightGray))),
    AppIcon("calendar", "日历", "📅", Brush.linearGradient(listOf(Color.White, Color.LightGray))),
    AppIcon("settings", "设置", if (isDark) "ic_settings_dark" else "ic_settings_light",
        Brush.linearGradient(listOf(Color.White, Color.LightGray)), useImage = true),
    AppIcon("wangwang", "汪汪", "🐶", Brush.linearGradient(listOf(Color.White, Color.LightGray))),
    AppIcon("persona_builder", "神笔马良", "✨", Brush.linearGradient(listOf(Color(0xFFFFD700), Color(0xFFFFA500)))),
    AppIcon("widget_market", "组件市场", "ic_widget_market", Brush.linearGradient(listOf(Color(0xFF07C160), Color(0xFF06AD56))), useImage = true),
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
    WidgetItem("weather_widget", "weather"),
    WidgetItem(BADGE_WIDGET_ID, "badge")
)

const val GRID_COLUMNS = 4
const val GRID_ROWS = 7
const val TOTAL_CELLS = GRID_COLUMNS * GRID_ROWS

data class WeatherInfo(val temp: String, val description: String, val icon: String, val range: String)

private val launchableAppIds = setOf(
    "settings",
    "chat",
    "safari",
    "calculator",
    "weather_app",
    "calendar",
    "camera",
    "notes",
    "persona_builder",
    "widget_market"
)

private data class AppLaunchRequest(
    val app: AppIcon,
    val customIconPath: String?,
    val startRect: Rect,
    val nonce: Long = System.nanoTime()
)

private fun lerpFloat(start: Float, end: Float, progress: Float): Float {
    return start + (end - start) * progress
}

private const val COLD_LAUNCH_OPEN_DELAY_MS = 100
private const val COLD_LAUNCH_ANIMATION_MS = 260

private val homeIconLabelTextStyle = TextStyle(
    platformStyle = PlatformTextStyle(includeFontPadding = true)
)

private val weatherHttpClient by lazy { OkHttpClient() }

private val locationIgnoreKeywords = setOf(
    "中国", "电信", "移动", "联通", "铁通", "教育网", "鹏博士", "宽带", "公司", "网络", "网络服务商"
)

private fun sanitizeCityName(rawCity: String): String {
    return rawCity.trim()
        .removeSuffix("市")
        .removeSuffix("特别行政区")
        .replace("自治区", "")
}

private fun parseCityFromIpResponse(responseText: String): String? {
    val fromPart = when {
        responseText.contains("来自于：") -> responseText.substringAfter("来自于：")
        responseText.contains("来自于:") -> responseText.substringAfter("来自于:")
        else -> responseText
    }

    val tokens = fromPart
        .replace("，", " ")
        .replace(",", " ")
        .replace("：", " ")
        .replace(":", " ")
        .trim()
        .split(Regex("\\s+"))
        .map { it.trim() }
        .filter { it.isNotEmpty() }

    for (i in tokens.size - 1 downTo 0) {
        val token = tokens[i]
        val normalized = sanitizeCityName(token)
        if (normalized.isBlank()) continue
        if (normalized in locationIgnoreKeywords) continue
        if (normalized.matches(Regex("\\d+\\.\\d+\\.\\d+\\.\\d+"))) continue
        return normalized
    }
    return null
}

private fun cityToPinyin(city: String): String {
    val cleaned = sanitizeCityName(city)
    return try {
        val transliterator = Transliterator.getInstance("Han-Latin; Latin-ASCII")
        transliterator.transliterate(cleaned)
            .replace(Regex("[^A-Za-z\\s]"), " ")
            .trim()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .joinToString("")
            .lowercase(Locale.ROOT)
    } catch (_: Exception) {
        cleaned.lowercase(Locale.ROOT)
    }
}

private fun normalizeTemperature(rawTemp: String): String {
    val match = Regex("[-+]?\\d+").find(rawTemp)
    return if (match != null) "${match.value.replace("+", "")}°" else rawTemp.replace(" ", "")
}

private fun containsChinese(text: String): Boolean {
    return text.any { it in '\u4e00'..'\u9fff' }
}

private fun localizeWeatherDescription(rawDescription: String): String {
    val cleaned = rawDescription.trim()
    if (cleaned.isEmpty()) return "天气未知"
    if (containsChinese(cleaned)) return cleaned

    val text = cleaned.lowercase(Locale.ROOT)
    if (text == "unknown" || text == "n/a" || text == "--") return "天气未知"

    val level = when {
        "heavy" in text -> "大"
        "moderate" in text -> "中"
        "light" in text || "patchy" in text -> "小"
        else -> ""
    }
    fun withLevel(base: String): String = if (level.isEmpty()) base else "$level$base"

    return when {
        "thunder" in text || "storm" in text -> "雷暴"
        "sleet" in text -> withLevel("雨夹雪")
        "snow" in text || "blizzard" in text -> withLevel("雪")
        "hail" in text -> "冰雹"
        "drizzle" in text -> if (level.isEmpty()) "毛毛雨" else withLevel("雨")
        "shower" in text -> "阵雨"
        "rain" in text -> withLevel("雨")
        "fog" in text || "mist" in text || "haze" in text -> "有雾"
        "overcast" in text -> "阴天"
        "partly cloudy" in text -> "局部多云"
        "cloudy" in text || "cloud" in text -> "多云"
        "clear" in text || "sunny" in text || "sun" in text -> "晴"
        "wind" in text || "breeze" in text -> "有风"
        else -> cleaned
    }
}

private fun mapWeatherIcon(description: String): String {
    val text = description.lowercase(Locale.ROOT)
    return when {
        "thunder" in text || "storm" in text || "雷" in text -> "⛈️"
        "snow" in text || "sleet" in text || "雪" in text -> "❄️"
        "rain" in text || "drizzle" in text || "shower" in text || "雨" in text -> "🌧️"
        "fog" in text || "mist" in text || "haze" in text || "雾" in text -> "🌫️"
        "cloud" in text || "overcast" in text || "云" in text || "阴" in text -> "⛅"
        "sun" in text || "clear" in text || "晴" in text -> "☀️"
        else -> "🌤️"
    }
}

private fun buildRangeText(maxTemp: String, minTemp: String): String {
    val maxText = if (maxTemp.isNotBlank()) normalizeTemperature(maxTemp) else "--"
    val minText = if (minTemp.isNotBlank()) normalizeTemperature(minTemp) else "--"
    return "最高 $maxText 最低 $minText"
}

private fun widgetRangeWithoutWind(rawRange: String): String {
    val normalized = rawRange.trim()
    if (normalized.isEmpty()) return "最高 -- 最低 --"
    val withoutWind = normalized.substringBefore("|").trim()
    return if (withoutWind.isEmpty()) "最高 -- 最低 --" else withoutWind
}

private fun isUnknownWeather(temp: String, description: String): Boolean {
    val normalizedDesc = description.trim()
    if (normalizedDesc.isEmpty()) return true
    val lowerDesc = normalizedDesc.lowercase(Locale.ROOT)
    if (normalizedDesc.contains("未知") || lowerDesc == "unknown") return true

    val normalizedTemp = temp.trim()
    return normalizedTemp == "--" && (normalizedDesc == "--" || lowerDesc == "n/a")
}

suspend fun fetchLocation(
    weatherCacheDbHelper: WeatherCacheDbHelper,
    forceRefresh: Boolean = false
): String {
    weatherCacheDbHelper.getManualLocation()?.let { return it }
    if (!forceRefresh) {
        weatherCacheDbHelper.getCachedLocation()?.let { return it }
    }

    val onlineCity = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("https://myip.ipip.net/")
                .header("User-Agent", "WangWangPhone/1.0")
                .build()

            weatherHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body?.string().orEmpty()
                parseCityFromIpResponse(body)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    if (!onlineCity.isNullOrBlank()) {
        weatherCacheDbHelper.saveLocationCache(onlineCity)
        return onlineCity
    }

    if (forceRefresh) {
        weatherCacheDbHelper.getCachedLocation()?.let { return it }
    }

    return "北京"
}

suspend fun fetchWeather(city: String): WeatherInfo {
    val fallback = WeatherInfo("--", "天气未知", "❓", "最高 -- 最低 --")

    return withContext(Dispatchers.IO) {
        try {
            val pinyinCity = cityToPinyin(city).ifBlank { sanitizeCityName(city) }
            val encodedCity = URLEncoder.encode(pinyinCity, "UTF-8")
            val request = Request.Builder()
                .url("https://wttr.in/$encodedCity?format=j1")
                .header("User-Agent", "WangWangPhone/1.0")
                .build()

            weatherHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext fallback
                val body = response.body?.string().orEmpty()
                WeatherRealtimeMemoryCache.save(city = city, payload = body)
                val json = JSONObject(body)

                val current = json.optJSONArray("current_condition")?.optJSONObject(0)
                val today = json.optJSONArray("weather")?.optJSONObject(0)
                val zhDesc = current
                    ?.optJSONArray("lang_zh")
                    ?.optJSONObject(0)
                    ?.optString("value", "")
                    .orEmpty()
                val enDesc = current
                    ?.optJSONArray("weatherDesc")
                    ?.optJSONObject(0)
                    ?.optString("value", "")
                    .orEmpty()
                val rawDescription = zhDesc.ifBlank { enDesc.ifBlank { "天气未知" } }
                val description = localizeWeatherDescription(rawDescription)

                val tempC = current?.optString("temp_C", "--").orEmpty()
                val maxTemp = today?.optString("maxtempC", "").orEmpty()
                val minTemp = today?.optString("mintempC", "").orEmpty()
                WeatherInfo(
                    temp = normalizeTemperature(tempC),
                    description = description,
                    icon = mapWeatherIcon(description),
                    range = buildRangeText(maxTemp, minTemp)
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            fallback
        }
    }
}

/**
 * 带缓存的天气加载逻辑
 * 1. 先查数据库缓存，如果今天已请求过则直接使用缓存
 * 2. 如果没有缓存，则请求网络并保存到数据库
 */
@Composable
fun WidgetContent(item: GridItem, badgeImagePath: String? = null, modifier: Modifier = Modifier) {
    when (item) {
        is WebWidgetGridItem -> {
            WebWidgetView(widget = item.widget, modifier = modifier)
            return
        }
        is WidgetItem -> {
            if (item.widgetType == "badge") {
                BadgeWidget(imagePath = badgeImagePath, modifier = modifier)
                return
            }
        }
        else -> return
    }

    val widgetType = item.widgetType
    var city by remember { mutableStateOf("...") }
    var weather by remember { mutableStateOf<WeatherInfo?>(null) }
    val context = LocalContext.current
    val weatherCacheDbHelper = remember { com.WangWangPhone.core.WeatherCacheDbHelper(context) }

    LaunchedEffect(widgetType) {
        city = fetchLocation(weatherCacheDbHelper)
        if (city.isNotEmpty() && city != "...") {
            val cached = weatherCacheDbHelper.getTodayWeatherCache(city)
            if (cached != null && !isUnknownWeather(cached.temp, cached.description)) {
                val localizedDescription = localizeWeatherDescription(cached.description)
                weather = WeatherInfo(
                    temp = cached.temp,
                    description = localizedDescription,
                    icon = mapWeatherIcon(localizedDescription),
                    range = cached.range
                )
            } else {
                val freshWeather = fetchWeather(city)
                weather = freshWeather
                if (!isUnknownWeather(freshWeather.temp, freshWeather.description)) {
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
                }
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
fun BadgeWidget(imagePath: String?, modifier: Modifier = Modifier) {
    val imageBitmap = remember(imagePath) { imagePath?.let { android.graphics.BitmapFactory.decodeFile(it) } }
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            contentAlignment = Alignment.Center
        ) {
            val diameter = minOf(maxWidth, maxHeight)
            Box(
                modifier = Modifier.size(diameter),
                contentAlignment = Alignment.Center
            ) {
                // 第一层：底层阴影（向下偏移，体现实体厚度）
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .offset(y = 4.dp)
                        .shadow(8.dp, CircleShape, clip = false)
                        .background(Color.White.copy(alpha = 0.01f), CircleShape)
                )

                // 第二层：内容层（圆形裁剪图片）
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.98f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (imageBitmap != null) {
                        Image(
                            bitmap = imageBitmap.asImageBitmap(),
                            contentDescription = "badge widget",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(Color(0xFFF9F9F9), Color(0xFFE7E7E7)),
                                        center = Offset(80f, 70f),
                                        radius = 360f
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "导入图片",
                                color = Color(0xFF7A7A7A),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // 第三层 + 第四层：边缘内阴影与顶部月牙高光
                Canvas(modifier = Modifier.matchParentSize()) {
                    val radius = size.minDimension / 2f
                    val center = Offset(size.width / 2f, size.height / 2f)

                    // 第三层：边缘内阴影（外圈约 10%-15%）
                    drawCircle(
                        brush = Brush.radialGradient(
                            colorStops = arrayOf(
                                0.00f to Color.Transparent,
                                0.84f to Color.Transparent,
                                0.92f to Color.Black.copy(alpha = 0.20f),
                                1.00f to Color.Black.copy(alpha = 0.40f)
                            ),
                            center = center,
                            radius = radius
                        ),
                        center = center,
                        radius = radius
                    )

                    // 第四层：顶部月牙高光（PET 覆膜反光）
                    val outerOval = Rect(
                        left = size.width * 0.08f,
                        top = size.height * 0.04f,
                        right = size.width * 0.92f,
                        bottom = size.height * 0.34f
                    )
                    val innerOval = Rect(
                        left = size.width * 0.15f,
                        top = size.height * 0.11f,
                        right = size.width * 0.85f,
                        bottom = size.height * 0.35f
                    )
                    val highlightPath = Path().apply {
                        fillType = PathFillType.EvenOdd
                        addOval(outerOval)
                        addOval(innerOval)
                    }
                    drawPath(
                        path = highlightPath,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.72f),
                                Color.White.copy(alpha = 0.32f),
                                Color.Transparent
                            ),
                            start = Offset(size.width / 2f, outerOval.top),
                            end = Offset(size.width / 2f, outerOval.bottom)
                        )
                    )
                }
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
    val weatherInfo = weather ?: WeatherInfo("--", "加载中...", "❓", "最高 -- 最低 --")
    Box(modifier = modifier.fillMaxSize().clip(RoundedCornerShape(20.dp))
        .background(Brush.linearGradient(listOf(Color(0xFF2E8BFF), Color(0xFF12C2E9))))) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.08f),
                            Color.Black.copy(alpha = 0.32f)
                        )
                    )
                )
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(city, color = Color.White.copy(alpha = 0.95f), fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(2.dp))
                Text(weatherInfo.temp, color = Color.White, fontSize = 44.sp, fontWeight = FontWeight.SemiBold)
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.2f))
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(weatherInfo.icon, fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = localizeWeatherDescription(weatherInfo.description),
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = widgetRangeWithoutWind(weatherInfo.range),
                    color = Color.White.copy(alpha = 0.92f),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun SettingsScreen(
    isActivated: Boolean, expiryDate: String, onBack: () -> Unit,
    onNavigateToActivation: () -> Unit, onNavigateToDisplay: () -> Unit,
    onNavigateToChatApi: () -> Unit,
    onNavigateToImageApi: () -> Unit,
    onNavigateToVoiceApi: () -> Unit,
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
        Text("API预设", modifier = Modifier.padding(horizontal = 26.dp, vertical = 8.dp), fontSize = 13.sp, color = Color.Gray)
        Column(modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(card)) {
            Box(modifier = Modifier.fillMaxWidth().clickable(onClick = onNavigateToChatApi).padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("聊天API预设", fontSize = 16.sp, color = txt)
                    Text(">", color = Color.Gray, fontSize = 16.sp)
                }
            }
            androidx.compose.material3.Divider(modifier = Modifier.padding(horizontal = 16.dp), color = Color.Gray.copy(alpha = 0.2f))
            Box(modifier = Modifier.fillMaxWidth().clickable(onClick = onNavigateToImageApi).padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("生图API预设", fontSize = 16.sp, color = txt)
                    Text(">", color = Color.Gray, fontSize = 16.sp)
                }
            }
            androidx.compose.material3.Divider(modifier = Modifier.padding(horizontal = 16.dp), color = Color.Gray.copy(alpha = 0.2f))
            Box(modifier = Modifier.fillMaxWidth().clickable(onClick = onNavigateToVoiceApi).padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("语音API预设", fontSize = 16.sp, color = txt)
                    Text(">", color = Color.Gray, fontSize = 16.sp)
                }
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
    var showIconCustomization by remember { mutableStateOf(false) }
    var showChatApiPresets by remember { mutableStateOf(false) }
    var showImageApiPresets by remember { mutableStateOf(false) }
    var showVoiceApiPresets by remember { mutableStateOf(false) }
    var showChatApp by remember { mutableStateOf(false) }
    var showBrowserApp by remember { mutableStateOf(false) }
    var showCalculatorApp by remember { mutableStateOf(false) }
    var showWeatherApp by remember { mutableStateOf(false) }
    var showCalendarApp by remember { mutableStateOf(false) }
    var showCameraApp by remember { mutableStateOf(false) }
    var showNotesApp by remember { mutableStateOf(false) }
    var showPersonaBuilderApp by remember { mutableStateOf(false) }
    var showWidgetMarketApp by remember { mutableStateOf(false) }
    var showActivationAlert by remember { mutableStateOf(false) }
    var showResetConfirm by remember { mutableStateOf(false) }
    var layoutReloadTrigger by remember { mutableIntStateOf(0) }
    val context = LocalContext.current
    val licenseManager = remember { LicenseManager.getInstance(context) }
    val wallpaperDbHelper = remember { WallpaperDbHelper(context) }
    val layoutDbHelper = remember { LayoutDbHelper(context) }
    val weatherCacheDbHelper = remember { WeatherCacheDbHelper(context) }
    val userProfileDbHelper = remember { UserProfileDbHelper(context) }
    val iconDbHelper = remember { IconCustomizationDbHelper(context) }
    val webWidgetDbHelper = remember { WebWidgetDbHelper(context) }
    var isActivated by remember { mutableStateOf(licenseManager.isActivated()) }
    var expiryDate by remember { mutableStateOf(licenseManager.getExpirationDateString()) }
    var lockWallpaperPath by remember { mutableStateOf(wallpaperDbHelper.getWallpaperFilePath(WallpaperType.LOCK)) }
    var homeWallpaperPath by remember { mutableStateOf(wallpaperDbHelper.getWallpaperFilePath(WallpaperType.HOME)) }
    var pendingLaunch by remember { mutableStateOf<AppLaunchRequest?>(null) }
    val launchProgress = remember { Animatable(0f) }
    val warmedAppStates = remember { mutableStateMapOf<String, Boolean>() }
    val keepAliveAppStates = remember { mutableStateMapOf<String, Boolean>() }

    fun openAppById(appId: String) {
        when (appId) {
            "settings" -> showSettings = true
            "chat" -> showChatApp = true
            "safari" -> showBrowserApp = true
            "calculator" -> showCalculatorApp = true
            "weather_app" -> showWeatherApp = true
            "calendar" -> showCalendarApp = true
            "camera" -> showCameraApp = true
            "notes" -> showNotesApp = true
            "persona_builder" -> showPersonaBuilderApp = true
            "widget_market" -> showWidgetMarketApp = true
        }
    }

    fun closeSettingsRoot() {
        showVoiceApiPresets = false
        showImageApiPresets = false
        showChatApiPresets = false
        showIconCustomization = false
        showDisplaySettings = false
        showSettings = false
        if (warmedAppStates["settings"] == true) {
            keepAliveAppStates["settings"] = true
        }
    }

    LaunchedEffect(pendingLaunch?.nonce) {
        val launchRequest = pendingLaunch ?: return@LaunchedEffect
        launchProgress.snapTo(0f)

        // 冷启动：100ms 后开始进入应用，其余动画继续覆盖，降低感知卡顿
        val openJob = launch {
            delay(COLD_LAUNCH_OPEN_DELAY_MS.toLong())
            openAppById(launchRequest.app.id)
            warmedAppStates[launchRequest.app.id] = true
        }

        launchProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = COLD_LAUNCH_ANIMATION_MS, easing = FastOutSlowInEasing)
        )
        openJob.join()
        pendingLaunch = null
    }

    val isDark = isSystemInDarkTheme()
    Box(modifier = Modifier.fillMaxSize()) {
        HomeScreenContent(
            isDark = isDark,
            isActivated = isActivated,
            onAppLaunchRequest = { app, startRect, customIconPath ->
                if (app.id in launchableAppIds && pendingLaunch == null) {
                    val isWarmStart = warmedAppStates[app.id] == true
                    if (isWarmStart) {
                        // 热启动：直接进入，不再使用蒙版
                        openAppById(app.id)
                    } else {
                        pendingLaunch = AppLaunchRequest(
                            app = app,
                            customIconPath = customIconPath,
                            startRect = startRect
                        )
                    }
                }
            },
            onActivationAlert = { showActivationAlert = true },
            homeWallpaperPath = homeWallpaperPath,
            layoutReloadTrigger = layoutReloadTrigger
        )

        pendingLaunch?.let { launch ->
            AppLaunchTransitionOverlay(
                app = launch.app,
                customIconPath = launch.customIconPath,
                startRect = launch.startRect,
                progress = launchProgress.value,
                isDarkTheme = isDark
            )
        }
        
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

        KeepAliveAppLayer(
            visible = showSettings,
            keepAlive = keepAliveAppStates["settings"] == true
        ) {
            SettingsScreen(
                isActivated = isActivated,
                expiryDate = expiryDate,
                onBack = { closeSettingsRoot() },
                onNavigateToActivation = { showActivation = true },
                onNavigateToDisplay = { showDisplaySettings = true },
                onNavigateToChatApi = { showChatApiPresets = true },
                onNavigateToImageApi = { showImageApiPresets = true },
                onNavigateToVoiceApi = { showVoiceApiPresets = true },
                onResetToDefault = { showResetConfirm = true }
            )
        }
        
        if (showResetConfirm) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showResetConfirm = false },
                title = { Text("恢复默认设置") },
                text = { Text("此操作将清除所有自定义布局、壁纸、天气缓存、用户资料和自定义图标，且无法撤销。是否继续？") },
                confirmButton = {
                    androidx.compose.material3.TextButton(
                        onClick = {
                            showResetConfirm = false
                            if (layoutDbHelper.resetToDefaultSettings(wallpaperDbHelper, weatherCacheDbHelper, userProfileDbHelper, iconDbHelper, webWidgetDbHelper)) {
                                // 强制重新加载以应用默认设置
                                android.widget.Toast.makeText(context, "设置已恢复", android.widget.Toast.LENGTH_SHORT).show()
                                // 简单粗暴的做法是重启，或者通过重置状态来更新UI
                                // 这里我们选择触发壁纸和设置状态的更新
                                lockWallpaperPath = wallpaperDbHelper.getWallpaperFilePath(WallpaperType.LOCK)
                                homeWallpaperPath = wallpaperDbHelper.getWallpaperFilePath(WallpaperType.HOME)
                                layoutReloadTrigger++
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

        KeepAliveAppLayer(
            visible = showChatApp,
            keepAlive = keepAliveAppStates["chat"] == true
        ) {
            ChatAppScreen(
                onClose = {
                    showChatApp = false
                    if (warmedAppStates["chat"] == true) {
                        keepAliveAppStates["chat"] = true
                    }
                }
            )
        }

        KeepAliveAppLayer(
            visible = showBrowserApp,
            keepAlive = keepAliveAppStates["safari"] == true
        ) {
            BrowserAppScreen(
                onClose = {
                    showBrowserApp = false
                    if (warmedAppStates["safari"] == true) {
                        keepAliveAppStates["safari"] = true
                    }
                }
            )
        }

        KeepAliveAppLayer(
            visible = showCalculatorApp,
            keepAlive = keepAliveAppStates["calculator"] == true
        ) {
            CalculatorAppScreen(
                onClose = {
                    showCalculatorApp = false
                    if (warmedAppStates["calculator"] == true) {
                        keepAliveAppStates["calculator"] = true
                    }
                }
            )
        }

        KeepAliveAppLayer(
            visible = showWeatherApp,
            keepAlive = keepAliveAppStates["weather_app"] == true
        ) {
            WeatherAppScreen(
                onClose = {
                    showWeatherApp = false
                    if (warmedAppStates["weather_app"] == true) {
                        keepAliveAppStates["weather_app"] = true
                    }
                }
            )
        }

        KeepAliveAppLayer(
            visible = showCalendarApp,
            keepAlive = keepAliveAppStates["calendar"] == true
        ) {
            CalendarAppScreen(
                onClose = {
                    showCalendarApp = false
                    if (warmedAppStates["calendar"] == true) {
                        keepAliveAppStates["calendar"] = true
                    }
                }
            )
        }

        KeepAliveAppLayer(
            visible = showCameraApp,
            keepAlive = keepAliveAppStates["camera"] == true
        ) {
            CameraAppScreen(
                onClose = {
                    showCameraApp = false
                    if (warmedAppStates["camera"] == true) {
                        keepAliveAppStates["camera"] = true
                    }
                }
            )
        }

        KeepAliveAppLayer(
            visible = showNotesApp,
            keepAlive = keepAliveAppStates["notes"] == true
        ) {
            NotesAppScreen(
                onClose = {
                    showNotesApp = false
                    if (warmedAppStates["notes"] == true) {
                        keepAliveAppStates["notes"] = true
                    }
                }
            )
        }

        KeepAliveAppLayer(
            visible = showWidgetMarketApp,
            keepAlive = keepAliveAppStates["widget_market"] == true
        ) {
            WidgetMarketAppScreen(
                isDark = isDark,
                onClose = {
                    showWidgetMarketApp = false
                    if (warmedAppStates["widget_market"] == true) {
                        keepAliveAppStates["widget_market"] = true
                    }
                },
                onLayoutChanged = { layoutReloadTrigger++ }
            )
        }

        KeepAliveAppLayer(
            visible = showPersonaBuilderApp,
            keepAlive = keepAliveAppStates["persona_builder"] == true
        ) {
            PersonaBuilderAppScreen(
                onClose = {
                    showPersonaBuilderApp = false
                    if (warmedAppStates["persona_builder"] == true) {
                        keepAliveAppStates["persona_builder"] = true
                    }
                }
            )
        }

        if (showDisplaySettings) DisplaySettingsScreen(
            wallpaperDbHelper = wallpaperDbHelper,
            onBack = { showDisplaySettings = false },
            onWallpaperChanged = {
                lockWallpaperPath = wallpaperDbHelper.getWallpaperFilePath(WallpaperType.LOCK)
                homeWallpaperPath = wallpaperDbHelper.getWallpaperFilePath(WallpaperType.HOME)
            },
            onNavigateToIconCustomization = { showIconCustomization = true },
            onBadgeWidgetChanged = { layoutReloadTrigger++ }
        )
        if (showIconCustomization) IconCustomizationScreen(
            onBack = { showIconCustomization = false },
            onIconChanged = { layoutReloadTrigger++ }
        )
        if (showChatApiPresets) ChatApiPresetsScreen(onBack = { showChatApiPresets = false })
        if (showImageApiPresets) ImageApiPresetsScreen(onBack = { showImageApiPresets = false })
        if (showVoiceApiPresets) VoiceApiPresetsScreen(onBack = { showVoiceApiPresets = false })

        BackHandler(enabled = showSettings || showDisplaySettings || showIconCustomization || showChatApiPresets || showImageApiPresets || showVoiceApiPresets) {
            when {
                showVoiceApiPresets -> showVoiceApiPresets = false
                showImageApiPresets -> showImageApiPresets = false
                showChatApiPresets -> showChatApiPresets = false
                showIconCustomization -> showIconCustomization = false
                showDisplaySettings -> showDisplaySettings = false
                showSettings -> closeSettingsRoot()
            }
        }

        if (showActivation) ActivationScreen(onBack = { showActivation = false }, onActivated = {
            isActivated = licenseManager.isActivated(); expiryDate = licenseManager.getExpirationDateString()
        })
    }
}

@Composable
private fun AppLaunchTransitionOverlay(
    app: AppIcon,
    customIconPath: String?,
    startRect: Rect,
    progress: Float,
    isDarkTheme: Boolean
) {
    val clamped = progress.coerceIn(0f, 1f)
    val cornerRadius = lerpFloat(16f, 0f, clamped)
    val iconAlpha = (1f - clamped * 1.15f).coerceIn(0f, 1f)
    val iconScale = lerpFloat(1f, 1.2f, clamped)
    val maskColor = if (isDarkTheme) Color.Black else Color.White
    val panelColor = if (isDarkTheme) Color(0xFF111111) else Color(0xFFF8F8F8)

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(9000f)
    ) {
        val density = LocalDensity.current
        val endWidth = constraints.maxWidth.toFloat()
        val endHeight = constraints.maxHeight.toFloat()
        val left = lerpFloat(startRect.left, 0f, clamped)
        val top = lerpFloat(startRect.top, 0f, clamped)
        val width = lerpFloat(startRect.width, endWidth, clamped)
        val height = lerpFloat(startRect.height, endHeight, clamped)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(maskColor.copy(alpha = 0.14f * clamped))
        )

        Box(
            modifier = Modifier
                .offset { IntOffset(left.roundToInt(), top.roundToInt()) }
                .width(with(density) { width.toDp() })
                .height(with(density) { height.toDp() })
                .clip(RoundedCornerShape(cornerRadius.dp))
                .background(panelColor),
            contentAlignment = Alignment.Center
        ) {
            if (iconAlpha > 0f) {
                LaunchIconPreview(
                    app = app,
                    customIconPath = customIconPath,
                    modifier = Modifier.graphicsLayer {
                        alpha = iconAlpha
                        scaleX = iconScale
                        scaleY = iconScale
                    }
                )
            }
        }
    }
}

@Composable
private fun KeepAliveAppLayer(
    visible: Boolean,
    keepAlive: Boolean,
    content: @Composable () -> Unit
) {
    if (!visible && !keepAlive) return

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(if (visible) 5000f else -200f)
            .graphicsLayer { alpha = if (visible) 1f else 0f }
    ) {
        content()
    }
}

@Composable
private fun LaunchIconPreview(
    app: AppIcon,
    customIconPath: String?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val iconSize = 78.dp
    val iconShape = RoundedCornerShape(20.dp)
    val customBitmap = remember(customIconPath) {
        customIconPath?.let { android.graphics.BitmapFactory.decodeFile(it) }
    }

    Box(
        modifier = modifier
            .size(iconSize)
            .clip(iconShape)
            .background(app.color),
        contentAlignment = Alignment.Center
    ) {
        if (customBitmap != null) {
            Image(
                bitmap = customBitmap.asImageBitmap(),
                contentDescription = app.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else if (app.useImage) {
            val resId = context.resources.getIdentifier(app.icon, "drawable", context.packageName)
            if (resId != 0) {
                Image(
                    painter = androidx.compose.ui.res.painterResource(id = resId),
                    contentDescription = app.name,
                    modifier = Modifier.fillMaxSize().padding(10.dp)
                )
            }
        } else {
            Text(text = app.icon, fontSize = 42.sp)
        }
    }
}

/**
 * 将所有应用和Widget分配到多个页面
 * 第一页：Widget + 聊天 + 设置（仅保留核心应用）
 * 第二页起：其余所有应用
 */
fun distributeItemsToPages(
    allApps: List<AppIcon>,
    widgets: List<WidgetItem>
): List<Map<Int, GridItem>> {
    val pages = mutableListOf<MutableMap<Int, GridItem>>()

    // 分离核心应用（聊天+设置）和其余应用
    val coreAppIds = setOf("chat", "settings", "safari", "calculator", "weather_app", "calendar", "camera", "notes", "widget_market")
    val coreApps = allApps.filter { it.id in coreAppIds }
    val otherApps = allApps.filter { it.id !in coreAppIds }.toMutableList()

    // 第一页：Widget + 核心应用
    val page0 = mutableMapOf<Int, GridItem>()
    for (widget in widgets) {
        for (i in 0 until TOTAL_CELLS) {
            if (checkOccupancy(page0, i, widget.spanX, widget.spanY, null)) {
                page0[i] = widget
                break
            }
        }
    }

    val overflowCoreApps = mutableListOf<AppIcon>()
    for (app in coreApps) {
        var placed = false
        for (i in 0 until TOTAL_CELLS) {
            if (checkOccupancy(page0, i, 1, 1, null)) {
                page0[i] = app
                placed = true
                break
            }
        }
        if (!placed) overflowCoreApps.add(app)
    }
    pages.add(page0)
    otherApps.addAll(0, overflowCoreApps)

    // 第二页起：其余所有应用
    while (otherApps.isNotEmpty()) {
        val page = mutableMapOf<Int, GridItem>()
        var pagePos = 0
        while (pagePos < TOTAL_CELLS && otherApps.isNotEmpty()) {
            page[pagePos] = otherApps.removeAt(0)
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
    onAppLaunchRequest: (app: AppIcon, startRect: Rect, customIconPath: String?) -> Unit = { _, _, _ -> },
    onActivationAlert: () -> Unit,
    homeWallpaperPath: String? = null,
    layoutReloadTrigger: Int = 0
) {
    val context = LocalContext.current
    val layoutDbHelper = remember { LayoutDbHelper(context) }
    val iconDbHelper = remember { IconCustomizationDbHelper(context) }
    val webWidgetDbHelper = remember { WebWidgetDbHelper(context) }
    val defaultApps = remember(isDark) { getDefaultApps(isDark) }
    val defaultWidgets = remember { getDefaultWidgets() }
    var customIcons by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    
    LaunchedEffect(layoutReloadTrigger) {
        val records = iconDbHelper.getAllCustomIcons()
        customIcons = records.associate { it.appId to (iconDbHelper.getCustomIconFilePath(it.appId) ?: "") }
    }

    // 多页网格: pageIndex -> (cellIndex -> GridItem)
    val allPages = remember { mutableStateListOf<MutableMap<Int, GridItem>>() }
    val dockApps = remember { mutableStateListOf<AppIcon>() }
    val maxDockApps = 4
    var isEditMode by remember { mutableStateOf(false) }
    var pageCount by remember { mutableIntStateOf(1) }
    var gestureHoldingItem by remember { mutableStateOf(false) }

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
    val appIconBounds = remember { mutableStateMapOf<String, Rect>() }
    val previewPages = remember { mutableStateListOf<MutableMap<Int, GridItem>>() }
    val previewDockApps = remember { mutableStateListOf<AppIcon>() }
    var hasDragPreview by remember { mutableStateOf(false) }
    var canCommitDragPreview by remember { mutableStateOf(false) }
    var lastPreviewTargetKey by remember { mutableStateOf("") }
    val haptic = LocalHapticFeedback.current
    val launchIconSizePx = with(LocalDensity.current) { 56.dp.toPx() }

    fun gridIconKey(pageIndex: Int, cellIndex: Int, appId: String): String {
        return "grid:$pageIndex:$cellIndex:$appId"
    }

    fun dockIconKey(dockIndex: Int, appId: String): String {
        return "dock:$dockIndex:$appId"
    }

    fun fallbackLaunchRect(touchX: Float, touchY: Float): Rect {
        val half = launchIconSizePx / 2f
        val safeLeft = (touchX - half).coerceAtLeast(0f)
        val safeTop = (touchY - half).coerceAtLeast(0f)
        val safeRight = safeLeft + launchIconSizePx
        val safeBottom = safeTop + launchIconSizePx
        return Rect(safeLeft, safeTop, safeRight, safeBottom)
    }

    // 从数据库加载布局
    fun normalizeGridPages(keepTrailingEmpty: Boolean) {
        val compactPages = allPages
            .filter { it.isNotEmpty() }
            .map { it.toMutableMap() }
            .toMutableList()

        if (compactPages.isEmpty()) {
            compactPages.add(mutableMapOf())
        }
        if (keepTrailingEmpty && compactPages.last().isNotEmpty()) {
            compactPages.add(mutableMapOf())
        }

        allPages.clear()
        compactPages.forEach { allPages.add(it) }
        pageCount = allPages.size.coerceAtLeast(1)
    }

    LaunchedEffect(isDark, layoutReloadTrigger) {
        val savedLayout = layoutDbHelper.getLayout()
        val webWidgetMap = webWidgetDbHelper.getAllWidgets().associateBy { it.id }
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
                    } else {
                        val webWidgetId = widgetIdFromLayoutId(li.appId)
                        val webWidget = webWidgetId?.let { webWidgetMap[it] }
                        if (webWidget != null) {
                            page[li.position] = WebWidgetGridItem(webWidget)
                        }
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

        normalizeGridPages(keepTrailingEmpty = false)
    }

    fun saveCurrentLayout() {
        val compactPages = allPages.filter { it.isNotEmpty() }
        val items = mutableListOf<LayoutItem>()
        compactPages.forEachIndexed { pageIdx, page ->
            val areaName = if (pageIdx == 0) "grid" else "grid_$pageIdx"
            page.forEach { (ci, item) ->
                items.add(LayoutItem(appId = item.id, position = ci, area = areaName))
            }
        }
        dockApps.forEachIndexed { i, app -> items.add(LayoutItem(appId = app.id, position = i, area = "dock")) }
        layoutDbHelper.saveLayout(items)
    }

    fun exitEditMode() {
        normalizeGridPages(keepTrailingEmpty = false)
        isEditMode = false
        saveCurrentLayout()
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

    fun getDockSlotFromGlobal(gx: Float, slotCount: Int = maxDockApps): Int {
        if (dockAreaSize.width <= 0 || slotCount <= 0) return 0
        val dockWidth = dockAreaSize.width.toFloat()
        val clampedX = (gx - dockAreaOffset.x).coerceIn(0f, (dockWidth - 1f).coerceAtLeast(0f))
        val slotWidth = dockWidth / slotCount.toFloat()
        return (clampedX / slotWidth).toInt().coerceIn(0, slotCount - 1)
    }

    fun getDockItemIndexFromGlobal(gx: Float, gy: Float): Int {
        if (!isOverDock(gx, gy) || dockApps.isEmpty()) return -1
        return getDockSlotFromGlobal(gx, dockApps.size)
    }

    val pagerState = rememberPagerState(pageCount = { pageCount })
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(pageCount) {
        val lastPageIndex = (pageCount - 1).coerceAtLeast(0)
        if (pagerState.currentPage > lastPageIndex) {
            pagerState.scrollToPage(lastPageIndex)
        }
    }

    fun getDockDropIndexFromGlobal(gx: Float, isDockSource: Boolean): Int {
        val slotCount = when {
            dockApps.isEmpty() -> 1
            isDockSource -> dockApps.size
            else -> dockApps.size + 1
        }
        val slot = getDockSlotFromGlobal(gx, slotCount)
        return if (isDockSource) {
            slot.coerceIn(0, (dockApps.size - 1).coerceAtLeast(0))
        } else {
            slot.coerceIn(0, dockApps.size)
        }
    }

    fun copyCurrentLayoutToPreview() {
        previewPages.clear()
        allPages.forEach { previewPages.add(it.toMutableMap()) }
        previewDockApps.clear()
        previewDockApps.addAll(dockApps)
    }

    fun clearDragPreview() {
        hasDragPreview = false
        canCommitDragPreview = false
        lastPreviewTargetKey = ""
        previewPages.clear()
        previewDockApps.clear()
    }

    fun isPreviewableGridItem(item: GridItem?): Boolean {
        return item is AppIcon || item is WidgetItem || item is WebWidgetGridItem
    }

    fun occupiedCells(startCell: Int, item: GridItem): Set<Int> {
        val cells = linkedSetOf<Int>()
        val startRow = startCell / GRID_COLUMNS
        val startCol = startCell % GRID_COLUMNS
        for (rowOffset in 0 until item.spanY) {
            for (colOffset in 0 until item.spanX) {
                val row = startRow + rowOffset
                val col = startCol + colOffset
                if (row in 0 until GRID_ROWS && col in 0 until GRID_COLUMNS) {
                    cells.add(row * GRID_COLUMNS + col)
                }
            }
        }
        return cells
    }

    fun findNearestSlotIndex(slots: List<Int>, targetCell: Int): Int {
        if (slots.isEmpty()) return -1
        var bestIndex = 0
        var bestDistance = Int.MAX_VALUE
        slots.forEachIndexed { index, cell ->
            val distance = abs(cell - targetCell)
            if (distance < bestDistance) {
                bestDistance = distance
                bestIndex = index
            }
        }
        return bestIndex
    }

    fun findNearestEmptyIndex(sequence: List<AppIcon?>, targetIndex: Int): Int? {
        var bestIndex: Int? = null
        var bestDistance = Int.MAX_VALUE
        sequence.forEachIndexed { index, app ->
            if (app == null) {
                val distance = abs(index - targetIndex)
                if (distance < bestDistance) {
                    bestDistance = distance
                    bestIndex = index
                }
            }
        }
        return bestIndex
    }

    fun buildSingleCellPreviewPage(
        basePage: Map<Int, GridItem>,
        draggedApp: AppIcon,
        targetCell: Int,
        sourceCell: Int?
    ): MutableMap<Int, GridItem>? {
        if (targetCell !in 0 until TOTAL_CELLS) return null
        if (targetCell % GRID_COLUMNS + draggedApp.spanX > GRID_COLUMNS) return null
        if (targetCell / GRID_COLUMNS + draggedApp.spanY > GRID_ROWS) return null

        val orderedEntries = basePage.entries
            .filter { it.value.id != draggedApp.id }
            .sortedBy { it.key }

        val insertionIndex = orderedEntries.indexOfFirst { (cell, item) ->
            val cells = occupiedCells(cell, item)
            targetCell <= (cells.maxOrNull() ?: cell)
        }.let { if (it == -1) orderedEntries.size else it }

        val orderedItems = mutableListOf<GridItem>()
        orderedEntries.forEachIndexed { index, entry ->
            if (index == insertionIndex) {
                orderedItems.add(draggedApp)
            }
            orderedItems.add(entry.value)
        }
        if (insertionIndex == orderedEntries.size) {
            orderedItems.add(draggedApp)
        }

        val result = mutableMapOf<Int, GridItem>()
        orderedItems.forEach { item ->
            val preferredCell = if (item.id == draggedApp.id) targetCell else null
            val availableCell = when {
                preferredCell != null && checkOccupancy(result, preferredCell, item.spanX, item.spanY, null) -> preferredCell
                else -> (0 until TOTAL_CELLS).firstOrNull {
                    checkOccupancy(result, it, item.spanX, item.spanY, null)
                }
            } ?: return null
            result[availableCell] = item
        }
        return result
    }

    fun buildMultiSpanPreviewPage(
        basePage: Map<Int, GridItem>,
        draggedItem: GridItem,
        targetCell: Int
    ): MutableMap<Int, GridItem>? {
        if (targetCell !in 0 until TOTAL_CELLS) return null
        if (targetCell % GRID_COLUMNS + draggedItem.spanX > GRID_COLUMNS) return null
        if (targetCell / GRID_COLUMNS + draggedItem.spanY > GRID_ROWS) return null

        val orderedEntries = basePage.entries
            .filter { it.value.id != draggedItem.id }
            .sortedBy { it.key }

        val insertionIndex = orderedEntries.indexOfFirst { (cell, item) ->
            val cells = occupiedCells(cell, item)
            targetCell <= (cells.maxOrNull() ?: cell)
        }.let { if (it == -1) orderedEntries.size else it }

        val orderedItems = mutableListOf<GridItem>()
        orderedEntries.forEachIndexed { index, entry ->
            if (index == insertionIndex) {
                orderedItems.add(draggedItem)
            }
            orderedItems.add(entry.value)
        }
        if (insertionIndex == orderedEntries.size) {
            orderedItems.add(draggedItem)
        }

        val result = mutableMapOf<Int, GridItem>()
        orderedItems.forEach { item ->
            if (item.id == draggedItem.id) {
                if (!checkOccupancy(result, targetCell, item.spanX, item.spanY, null)) {
                    return null
                }
                result[targetCell] = item
            } else {
                val availableCell = (0 until TOTAL_CELLS).firstOrNull {
                    checkOccupancy(result, it, item.spanX, item.spanY, null)
                } ?: return null
                result[availableCell] = item
            }
        }
        return result
    }

    fun updateDragPreview(currentItem: GridItem) {
        copyCurrentLayoutToPreview()
        hasDragPreview = true
        canCommitDragPreview = false

        if (dragSource == "dock" && dragSourceDockIndex in previewDockApps.indices) {
            previewDockApps.removeAt(dragSourceDockIndex)
        } else if (dragSource == "grid" && dragSourcePageIndex in previewPages.indices) {
            previewPages[dragSourcePageIndex].remove(dragSourceCellIndex)
        }

        if (isOverDock(dragOverlayX, dragOverlayY)) {
            if (currentItem is AppIcon) {
                val canDropToDock = dragSource == "dock" || previewDockApps.size < maxDockApps
                if (canDropToDock) {
                    val insertIndex = getDockDropIndexFromGlobal(dragOverlayX, dragSource == "dock")
                        .coerceIn(0, previewDockApps.size)
                    previewDockApps.add(insertIndex, currentItem)
                    canCommitDragPreview = true
                    val previewTargetKey = "dock:$insertIndex"
                    if (previewTargetKey != lastPreviewTargetKey) {
                        if (lastPreviewTargetKey.isNotEmpty()) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        lastPreviewTargetKey = previewTargetKey
                    }
                }
            }
            return
        }

        val currentPage = pagerState.currentPage
        if (highlightCellIndex == -1 || currentPage !in allPages.indices || currentPage !in previewPages.indices) {
            return
        }

        val previewPage = if (currentItem is AppIcon && currentItem.spanX == 1 && currentItem.spanY == 1) {
            val sourceCell = if (dragSource == "grid" && dragSourcePageIndex == currentPage) {
                dragSourceCellIndex
            } else {
                null
            }
            buildSingleCellPreviewPage(
                basePage = allPages[currentPage],
                draggedApp = currentItem,
                targetCell = highlightCellIndex,
                sourceCell = sourceCell
            )
        } else {
            buildMultiSpanPreviewPage(
                basePage = allPages[currentPage],
                draggedItem = currentItem,
                targetCell = highlightCellIndex
            )
        } ?: return

        previewPages[currentPage].clear()
        previewPages[currentPage].putAll(previewPage)
        canCommitDragPreview = true
        val previewTargetKey = "grid:$currentPage:$highlightCellIndex"
        if (previewTargetKey.isNotEmpty() && previewTargetKey != lastPreviewTargetKey) {
            if (lastPreviewTargetKey.isNotEmpty()) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
            lastPreviewTargetKey = previewTargetKey
        }
    }

    if (isEditMode) BackHandler { exitEditMode() }

    // 自动翻页逻辑
    var autoScrollJob by remember { mutableStateOf<Job?>(null) }
    var autoScrollTargetPage by remember { mutableIntStateOf(-1) }
    val autoScrollEdgeThresholdPx = with(LocalDensity.current) { 72.dp.toPx() }
    val autoScrollDelayMs = 240L
    fun handleAutoScroll(globalX: Float) {
        val displayMetrics = context.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels.toFloat()
        val currentPage = pagerState.currentPage
        val targetPage = when {
            globalX < autoScrollEdgeThresholdPx && currentPage > 0 -> currentPage - 1
            globalX > screenWidth - autoScrollEdgeThresholdPx && currentPage < pageCount - 1 -> currentPage + 1
            else -> -1
        }

        if (targetPage == -1) {
            autoScrollJob?.cancel()
            autoScrollJob = null
            autoScrollTargetPage = -1
            return
        }

        if (autoScrollTargetPage == targetPage && autoScrollJob?.isActive == true) {
            return
        }

        autoScrollJob?.cancel()
        autoScrollTargetPage = targetPage
        autoScrollJob = coroutineScope.launch {
            delay(autoScrollDelayMs)
            if (pagerState.currentPage != targetPage) {
                pagerState.animateScrollToPage(targetPage)
                autoScrollTargetPage = -1
                draggedItem?.let { dragged ->
                    if (isPreviewableGridItem(dragged)) {
                        updateDragPreview(dragged)
                    }
                }
            }
        }
    }

    // ??????

    val renderedPages = if (hasDragPreview) previewPages else allPages
    val renderedDockApps = if (hasDragPreview) previewDockApps else dockApps
    val edgePreviewVisible = draggedItem != null && autoScrollTargetPage != -1 && gridAreaSize.height > 0
    val edgePreviewDirection = when {
        autoScrollTargetPage == -1 -> 0
        autoScrollTargetPage < pagerState.currentPage -> -1
        else -> 1
    }
    val edgePreviewAlpha by animateFloatAsState(
        targetValue = if (edgePreviewVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 180),
        label = "edge_preview_alpha"
    )
    val edgePreviewPulse = rememberInfiniteTransition(label = "edge_preview_pulse")
    val edgePreviewScale by edgePreviewPulse.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            tween(560, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "edge_preview_scale"
    )

    Box(modifier = Modifier.fillMaxSize()
        .pointerInput(isActivated) {
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
                val dockIndex = getDockItemIndexFromGlobal(touchX, touchY)
                if (dockIndex in dockApps.indices) {
                    startItem = dockApps[dockIndex]
                    startDockIndex = dockIndex
                    isDockItem = true
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
                    gestureHoldingItem = true
                    var longPressTriggered = false
                    var tapDetected = false
                    var completedBeforeTimeout = false
                    val upChange = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                        val result = waitForUpOrCancellation()
                        completedBeforeTimeout = true
                        result
                    }
                    if (completedBeforeTimeout) {
                        tapDetected = upChange != null
                        upChange?.consume()
                    } else {
                        longPressTriggered = true
                    }

                    if (tapDetected) {
                        if (isEditMode) {
                            exitEditMode()
                        } else if (startItem is AppIcon) {
                            val app = startItem
                            val launchRect = when {
                                isDockItem && startDockIndex >= 0 -> {
                                    appIconBounds[dockIconKey(startDockIndex, app.id)] ?: fallbackLaunchRect(touchX, touchY)
                                }

                                startPageIndex >= 0 && startCellIndex >= 0 -> {
                                    appIconBounds[gridIconKey(startPageIndex, startCellIndex, app.id)]
                                        ?: fallbackLaunchRect(touchX, touchY)
                                }

                                else -> fallbackLaunchRect(touchX, touchY)
                            }

                            if (app.id == "settings") {
                                onAppLaunchRequest(app, launchRect, customIcons[app.id])
                            } else if (isActivated) {
                                if (app.id in launchableAppIds) {
                                    onAppLaunchRequest(app, launchRect, customIcons[app.id])
                                }
                            } else {
                                onActivationAlert()
                            }
                        }
                    } else if (longPressTriggered) {
                        if (!isEditMode) isEditMode = true
                        normalizeGridPages(keepTrailingEmpty = true)
                        coroutineScope.launch { pagerState.scrollToPage(pagerState.currentPage) }
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

                        hasDragPreview = isPreviewableGridItem(startItem)
                        canCommitDragPreview = false
                        copyCurrentLayoutToPreview()
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (isPreviewableGridItem(startItem)) {
                            updateDragPreview(startItem)
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
                                                val safeCol = targetCol.coerceAtMost(GRID_COLUMNS - startItem.spanX)
                                                val safeRow = targetRow.coerceAtMost(GRID_ROWS - startItem.spanY)
                                                highlightCellIndex = safeRow * GRID_COLUMNS + safeCol
                                            } else { highlightCellIndex = -1 }
                                        } else { highlightCellIndex = -1 }
                                        if (isPreviewableGridItem(startItem)) {
                                            updateDragPreview(startItem)
                                        }
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

                                if (currentItem is AppIcon && hasDragPreview) {
                                    if (canCommitDragPreview) {
                                        allPages.clear()
                                        previewPages.forEach { allPages.add(it.toMutableMap()) }
                                        dockApps.clear()
                                        dockApps.addAll(previewDockApps)
                                        pageCount = allPages.size.coerceAtLeast(1)
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                } else if (isOverDock(dragOverlayX, dragOverlayY) && currentItem is AppIcon) {
                                    val dropDockIndex = getDockDropIndexFromGlobal(
                                        gx = dragOverlayX,
                                        isDockSource = dragSource == "dock"
                                    )
                                    if (dragSource == "dock") {
                                        if (dragSourceDockIndex in dockApps.indices) {
                                            val movedApp = dockApps.removeAt(dragSourceDockIndex)
                                            val insertIndex = dropDockIndex.coerceIn(0, dockApps.size)
                                            dockApps.add(insertIndex, movedApp)
                                        }
                                    } else if (dragSource == "grid" && dockApps.size < maxDockApps) {
                                        sourcePage?.remove(dragSourceCellIndex)
                                        val insertIndex = dropDockIndex.coerceIn(0, dockApps.size)
                                        dockApps.add(insertIndex, currentItem)
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
                                        else if (dragSourceDockIndex in dockApps.indices) dockApps.removeAt(dragSourceDockIndex)
                                        
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
                                normalizeGridPages(keepTrailingEmpty = isEditMode)
                                saveCurrentLayout()
                            }
                        } finally {
                            autoScrollJob?.cancel(); autoScrollJob = null
                            clearDragPreview()
                            draggedItem = null; highlightCellIndex = -1; dragSourceDockIndex = -1; dragSource = "grid"
                        }
                    }
                    gestureHoldingItem = false
                } else if (isEditMode) {
                    var completedBeforeTimeout = false
                    val upChange = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                        val result = waitForUpOrCancellation()
                        completedBeforeTimeout = true
                        result
                    }
                    if (completedBeforeTimeout && upChange != null) {
                        upChange.consume()
                        exitEditMode()
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
            else Box(modifier = Modifier.fillMaxSize().background(Color(0xFF2C2C2C)))
        } else Box(modifier = Modifier.fillMaxSize().background(Color(0xFF2C2C2C)))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
        ) {
            // 注意：拖拽浮层已移至顶层 Box，不在 Column 内

            // HorizontalPager - 多页网格区域
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth().weight(1f),
                userScrollEnabled = draggedItem == null && !gestureHoldingItem
            ) { pageIndex ->
                if (pageIndex < allPages.size) {
                    val currentPageGrid = renderedPages[pageIndex]

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
                        if (highlightCellIndex in 0 until TOTAL_CELLS && draggedItem != null && pageIndex == pagerState.currentPage && !hasDragPreview) {
                            val hr = highlightCellIndex / GRID_COLUMNS; val hc = highlightCellIndex % GRID_COLUMNS
                            val spanX = draggedItem!!.spanX
                            val spanY = draggedItem!!.spanY
                            val highlightTransition = rememberInfiniteTransition(label = "drop_highlight")
                            val highlightScale by highlightTransition.animateFloat(
                                initialValue = 0.99f,
                                targetValue = 1.02f,
                                animationSpec = infiniteRepeatable(
                                    tween(520, easing = FastOutSlowInEasing),
                                    RepeatMode.Reverse
                                ),
                                label = "drop_highlight_scale"
                            )
                            val highlightAlpha by highlightTransition.animateFloat(
                                initialValue = 0.12f,
                                targetValue = 0.24f,
                                animationSpec = infiniteRepeatable(
                                    tween(520, easing = FastOutSlowInEasing),
                                    RepeatMode.Reverse
                                ),
                                label = "drop_highlight_alpha"
                            )

                            if (hc + spanX <= GRID_COLUMNS && hr + spanY <= GRID_ROWS) {
                                Box(modifier = Modifier
                                    .offset { IntOffset(hc * cwPx + 4, hr * chPx + 4) }
                                    .width(with(density) { (cwPx * spanX - 8).toDp() })
                                    .height(with(density) { (chPx * spanY - 8).toDp() })
                                    .graphicsLayer {
                                        scaleX = highlightScale
                                        scaleY = highlightScale
                                    }
                                    .background(Color.White.copy(alpha = highlightAlpha), RoundedCornerShape(14.dp))
                                    .border(2.dp, Color.White.copy(alpha = 0.78f), RoundedCornerShape(14.dp)))
                            }
                        }

                        // 渲染网格内容
                        currentPageGrid.forEach { (cellIndex, item) ->
                            val isDraggedFromHere = draggedItem?.id == item.id && (hasDragPreview || (dragSource == "grid" && dragSourcePageIndex == pageIndex))

                            val row = cellIndex / GRID_COLUMNS; val col = cellIndex % GRID_COLUMNS
                            val infT = rememberInfiniteTransition(label = "w_${item.id}")
                            val wiggleRotation by infT.animateFloat(
                                initialValue = if (cellIndex % 2 == 0) -1.2f else 1.2f,
                                targetValue = if (cellIndex % 2 == 0) 1.2f else -1.2f,
                                animationSpec = infiniteRepeatable(
                                    tween(190 + (cellIndex % 4) * 18, easing = LinearEasing),
                                    RepeatMode.Reverse
                                ),
                                label = "wa_${item.id}"
                            )
                            val wiggleScale by infT.animateFloat(
                                initialValue = 1.0f,
                                targetValue = 1.018f,
                                animationSpec = infiniteRepeatable(
                                    tween(260 + (cellIndex % 5) * 20, easing = FastOutSlowInEasing),
                                    RepeatMode.Reverse
                                ),
                                label = "ws_${item.id}"
                            )
                            val wiggleShiftX by infT.animateFloat(
                                initialValue = if (cellIndex % 2 == 0) -0.8f else 0.8f,
                                targetValue = if (cellIndex % 2 == 0) 0.8f else -0.8f,
                                animationSpec = infiniteRepeatable(
                                    tween(220 + (cellIndex % 3) * 16, easing = LinearEasing),
                                    RepeatMode.Reverse
                                ),
                                label = "wx_${item.id}"
                            )
                            val wiggleShiftY by infT.animateFloat(
                                initialValue = -0.6f,
                                targetValue = 0.6f,
                                animationSpec = infiniteRepeatable(
                                    tween(280 + (cellIndex % 4) * 22, easing = FastOutSlowInEasing),
                                    RepeatMode.Reverse
                                ),
                                label = "wy_${item.id}"
                            )

                            val itemWidth = cwPx * item.spanX
                            val itemHeight = chPx * item.spanY
                            val animatedGridOffset by animateIntOffsetAsState(
                                targetValue = IntOffset(col * cwPx, row * chPx),
                                animationSpec = spring(
                                    dampingRatio = 0.82f,
                                    stiffness = Spring.StiffnessMediumLow
                                ),
                                label = "grid_pos_${item.id}_${pageIndex}"
                            )

                            Box(modifier = Modifier
                                .offset { animatedGridOffset }
                                .width(with(density) { itemWidth.toDp() }).height(with(density) { itemHeight.toDp() })
                                .graphicsLayer {
                                    if (isEditMode) {
                                        rotationZ = wiggleRotation
                                        translationX = wiggleShiftX
                                        translationY = wiggleShiftY
                                        scaleX = wiggleScale
                                        scaleY = wiggleScale
                                    }
                                    if (isDraggedFromHere) alpha = 0f
                                }
                                // pointerInput 已移除
                                , contentAlignment = Alignment.Center
                            ) {
                                if (item is WidgetItem || item is WebWidgetGridItem) {
                                    WidgetContent(
                                        item = item,
                                        badgeImagePath = customIcons[BADGE_WIDGET_ID],
                                        modifier = Modifier.fillMaxSize().padding(8.dp)
                                    )
                                    if (isEditMode && draggedItem == null) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(top = 6.dp, end = 6.dp)
                                                .size(22.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFFFF3B30))
                                                .clickable {
                                                    allPages.getOrNull(pageIndex)?.remove(cellIndex)
                                                    normalizeGridPages(keepTrailingEmpty = true)
                                                    saveCurrentLayout()
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("X", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                } else if (item is AppIcon) {
                                    val itemWidthDp = with(density) { itemWidth.toDp() }
                                    val itemHeightDp = with(density) { itemHeight.toDp() }
                                    val iconSize = minOf(
                                        60.dp,
                                        (itemWidthDp - 8.dp).coerceAtLeast(36.dp),
                                        (itemHeightDp - 22.dp).coerceAtLeast(36.dp)
                                    )
                                    val emojiFontSize = when {
                                        iconSize >= 56.dp -> 48.sp
                                        iconSize >= 50.dp -> 42.sp
                                        iconSize >= 44.dp -> 38.sp
                                        else -> 34.sp
                                    }
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Top,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(top = 4.dp, bottom = 2.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(iconSize)
                                                .onGloballyPositioned { coordinates ->
                                                    appIconBounds[gridIconKey(pageIndex, cellIndex, item.id)] = Rect(
                                                        offset = coordinates.positionInRoot(),
                                                        size = Size(
                                                            coordinates.size.width.toFloat(),
                                                            coordinates.size.height.toFloat()
                                                        )
                                                    )
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            val customIconPath = customIcons[item.id]
                                            if (customIconPath != null) {
                                                val bitmap = remember(customIconPath) { android.graphics.BitmapFactory.decodeFile(customIconPath) }
                                                if (bitmap != null) {
                                                    Image(bitmap = bitmap.asImageBitmap(), contentDescription = item.name,
                                                        modifier = Modifier.size(iconSize).clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop)
                                                }
                                            } else if (item.useImage) {
                                                val resId = context.resources.getIdentifier(item.icon, "drawable", context.packageName)
                                                if (resId != 0) Image(painter = androidx.compose.ui.res.painterResource(id = resId),
                                                    contentDescription = item.name, modifier = Modifier.size(iconSize))
                                            } else Text(item.icon, fontSize = emojiFontSize)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = item.name,
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 2.dp),
                                            style = homeIconLabelTextStyle
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ??????????
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
            Box(
                modifier = Modifier
                    .padding(bottom = 20.dp, start = 15.dp, end = 15.dp)
                    .fillMaxWidth().height(90.dp).clip(RoundedCornerShape(30.dp))
                    .zIndex(20f)
                    .onGloballyPositioned { dockAreaOffset = it.positionInRoot(); dockAreaSize = it.size }
            ) {
                val isDraggingOverDock = draggedItem != null && dragSource == "grid" &&
                    isOverDock(dragOverlayX, dragOverlayY) && renderedDockApps.size < maxDockApps && draggedItem is AppIcon
                Box(modifier = Modifier.fillMaxSize().background(
                    if (isDraggingOverDock) Color(0xFF007AFF).copy(alpha = 0.4f) else Color.White.copy(alpha = 0.3f)
                ).blur(20.dp))

                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val density = LocalDensity.current
                    val dockIconSizePx = with(density) { 60.dp.roundToPx() }
                    val dockWidthPx = constraints.maxWidth
                    val dockHeightPx = constraints.maxHeight
                    val dockTopPx = ((dockHeightPx - dockIconSizePx) / 2f).roundToInt()

                    fun dockLeftFor(index: Int, count: Int): Int {
                        val slotCount = count.coerceAtLeast(1)
                        val slotWidth = dockWidthPx / slotCount.toFloat()
                        return (slotWidth * index + (slotWidth - dockIconSizePx) / 2f).roundToInt()
                    }

                    if (renderedDockApps.isEmpty() && !isEditMode) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("长按拖入应用", color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp)
                        }
                    }
                    if (renderedDockApps.isEmpty() && isEditMode) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("拖拽应用到此处", color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp)
                        }
                    }

                    renderedDockApps.forEachIndexed { dockIndex, app ->
                        val isDraggedFromDock = draggedItem?.id == app.id && (hasDragPreview || dragSource == "dock")
                        val dwt = rememberInfiniteTransition(label = "dw_$dockIndex")
                        val dockWiggleRotation by dwt.animateFloat(
                            initialValue = if (dockIndex % 2 == 0) -1.15f else 1.15f,
                            targetValue = if (dockIndex % 2 == 0) 1.15f else -1.15f,
                            animationSpec = infiniteRepeatable(
                                tween(195 + (dockIndex % 4) * 18, easing = LinearEasing),
                                RepeatMode.Reverse
                            ),
                            label = "dwa_$dockIndex"
                        )
                        val dockWiggleScale by dwt.animateFloat(
                            initialValue = 1.0f,
                            targetValue = 1.016f,
                            animationSpec = infiniteRepeatable(
                                tween(250 + (dockIndex % 4) * 18, easing = FastOutSlowInEasing),
                                RepeatMode.Reverse
                            ),
                            label = "dws_$dockIndex"
                        )
                        val dockWiggleShiftY by dwt.animateFloat(
                            initialValue = -0.5f,
                            targetValue = 0.5f,
                            animationSpec = infiniteRepeatable(
                                tween(270 + (dockIndex % 3) * 14, easing = FastOutSlowInEasing),
                                RepeatMode.Reverse
                            ),
                            label = "dwy_$dockIndex"
                        )
                        val animatedDockOffset by animateIntOffsetAsState(
                            targetValue = IntOffset(
                                dockLeftFor(dockIndex, renderedDockApps.size),
                                dockTopPx
                            ),
                            animationSpec = spring(
                                dampingRatio = 0.8f,
                                stiffness = Spring.StiffnessLow
                            ),
                            label = "dock_pos_${app.id}_$dockIndex"
                        )

                        Box(modifier = Modifier
                            .offset { animatedDockOffset }
                            .size(60.dp)
                            .onGloballyPositioned { coordinates ->
                                appIconBounds[dockIconKey(dockIndex, app.id)] = Rect(
                                    offset = coordinates.positionInRoot(),
                                    size = Size(
                                        coordinates.size.width.toFloat(),
                                        coordinates.size.height.toFloat()
                                    )
                                )
                            }
                            .graphicsLayer {
                                if (isEditMode) {
                                    rotationZ = dockWiggleRotation
                                    translationY = dockWiggleShiftY
                                    scaleX = dockWiggleScale
                                    scaleY = dockWiggleScale
                                }
                                if (isDraggedFromDock) alpha = 0f
                            },
                            contentAlignment = Alignment.Center
                        ) {
                            val customIconPath = customIcons[app.id]
                            if (customIconPath != null) {
                                val bitmap = remember(customIconPath) { android.graphics.BitmapFactory.decodeFile(customIconPath) }
                                if (bitmap != null) {
                                    Image(bitmap = bitmap.asImageBitmap(), contentDescription = app.name,
                                        modifier = Modifier.size(60.dp).clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop)
                                }
                            } else if (app.useImage) {
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
        if (edgePreviewVisible) {
            val density = LocalDensity.current
            val overlayWidth = 78.dp
            val overlayWidthPx = with(density) { overlayWidth.roundToPx() }
            val overlayHeightDp = with(density) { gridAreaSize.height.toDp() }
            val overlayTopPx = gridAreaOffset.y.roundToInt()
            val overlayLeftPx = gridAreaOffset.x.roundToInt()
            val isLeftPreview = edgePreviewDirection < 0
            Box(
                modifier = Modifier.fillMaxSize().zIndex(9500f),
                contentAlignment = Alignment.TopStart
            ) {
                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                if (isLeftPreview) overlayLeftPx else overlayLeftPx + gridAreaSize.width - overlayWidthPx,
                                overlayTopPx
                            )
                        }
                        .width(overlayWidth)
                        .height(overlayHeightDp)
                        .graphicsLayer {
                            alpha = edgePreviewAlpha
                            scaleX = edgePreviewScale
                            scaleY = edgePreviewScale
                        }
                        .background(
                            brush = if (isLeftPreview) {
                                Brush.horizontalGradient(
                                    listOf(Color.White.copy(alpha = 0.22f), Color.White.copy(alpha = 0.06f), Color.Transparent)
                                )
                            } else {
                                Brush.horizontalGradient(
                                    listOf(Color.Transparent, Color.White.copy(alpha = 0.06f), Color.White.copy(alpha = 0.22f))
                                )
                            },
                            shape = RoundedCornerShape(24.dp)
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.28f), RoundedCornerShape(24.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 6.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (isLeftPreview) "<" else ">",
                            color = Color.White,
                            fontSize = 38.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isLeftPreview) "???" else "???",
                            color = Color.White,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        if (draggedItem != null) {
            val density = LocalDensity.current
            // Dynamically calculate drag overlay size based on grid size
            val cellWidthDp = if (gridAreaSize.width > 0) with(density) { (gridAreaSize.width / GRID_COLUMNS).toDp() } else 75.dp
            val cellHeightDp = if (gridAreaSize.height > 0) with(density) { (gridAreaSize.height / GRID_ROWS).toDp() } else 90.dp
            val itemWidth = cellWidthDp * draggedItem!!.spanX
            val itemHeight = cellHeightDp * draggedItem!!.spanY
            val itemWidthPx = with(density) { itemWidth.toPx() }
            val itemHeightPx = with(density) { itemHeight.toPx() }
            val baseOverlayOffset = IntOffset(
                (dragOverlayX - itemWidthPx / 2f).roundToInt(),
                (dragOverlayY - itemHeightPx * 0.38f).roundToInt()
            )
            var snapTargetOffset = baseOverlayOffset
            val snapThresholdPx = with(density) { 54.dp.toPx() }
            if (!isOverDock(dragOverlayX, dragOverlayY) && highlightCellIndex in 0 until TOTAL_CELLS && gridAreaSize.width > 0) {
                val cellWidthPx = gridAreaSize.width.toFloat() / GRID_COLUMNS
                val cellHeightPx = gridAreaSize.height.toFloat() / GRID_ROWS
                val targetCol = highlightCellIndex % GRID_COLUMNS
                val targetRow = highlightCellIndex / GRID_COLUMNS
                val targetLeft = (gridAreaOffset.x + targetCol * cellWidthPx).roundToInt()
                val targetTop = (gridAreaOffset.y + targetRow * cellHeightPx).roundToInt()
                val deltaX = targetLeft - baseOverlayOffset.x
                val deltaY = targetTop - baseOverlayOffset.y
                if (kotlin.math.abs(deltaX) < snapThresholdPx || kotlin.math.abs(deltaY) < snapThresholdPx) {
                    snapTargetOffset = IntOffset(
                        lerpFloat(baseOverlayOffset.x.toFloat(), targetLeft.toFloat(), 0.24f).roundToInt(),
                        lerpFloat(baseOverlayOffset.y.toFloat(), targetTop.toFloat(), 0.24f).roundToInt()
                    )
                }
            } else if (isOverDock(dragOverlayX, dragOverlayY) && draggedItem is AppIcon && dockAreaSize.width > 0) {
                val previewIndex = renderedDockApps.indexOfFirst { it.id == draggedItem!!.id }
                if (previewIndex >= 0) {
                    val dockSlotCount = renderedDockApps.size.coerceAtLeast(1)
                    val dockSlotWidth = dockAreaSize.width.toFloat() / dockSlotCount.toFloat()
                    val targetLeft = (dockAreaOffset.x + dockSlotWidth * previewIndex + (dockSlotWidth - itemWidthPx) / 2f).roundToInt()
                    val targetTop = (dockAreaOffset.y + (dockAreaSize.height - itemHeightPx) / 2f).roundToInt()
                    val deltaX = targetLeft - baseOverlayOffset.x
                    val deltaY = targetTop - baseOverlayOffset.y
                    if (kotlin.math.abs(deltaX) < snapThresholdPx * 1.2f || kotlin.math.abs(deltaY) < snapThresholdPx * 1.2f) {
                        snapTargetOffset = IntOffset(
                            lerpFloat(baseOverlayOffset.x.toFloat(), targetLeft.toFloat(), 0.3f).roundToInt(),
                            lerpFloat(baseOverlayOffset.y.toFloat(), targetTop.toFloat(), 0.3f).roundToInt()
                        )
                    }
                }
            }
            val animatedOverlayOffset by animateIntOffsetAsState(
                targetValue = snapTargetOffset,
                animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow),
                label = "drag_overlay_offset"
            )
            val dragOverlayScale by animateFloatAsState(
                targetValue = 1.08f,
                animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow),
                label = "drag_overlay_scale"
            )
            val dragOverlayAlpha by animateFloatAsState(
                targetValue = 0.94f,
                animationSpec = tween(durationMillis = 120),
                label = "drag_overlay_alpha"
            )

            Box(
                modifier = Modifier.fillMaxSize().zIndex(10000f),
                contentAlignment = Alignment.TopStart
            ) {
                Box(
                    modifier = Modifier
                        .offset { animatedOverlayOffset }
                        .size(itemWidth, itemHeight)
                        .graphicsLayer {
                            scaleX = dragOverlayScale
                            scaleY = dragOverlayScale
                            alpha = dragOverlayAlpha
                            shadowElevation = 24.dp.toPx()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (draggedItem is WidgetItem || draggedItem is WebWidgetGridItem) {
                        WidgetContent(
                            item = draggedItem as GridItem,
                            badgeImagePath = customIcons[BADGE_WIDGET_ID],
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        val app = draggedItem as AppIcon
                        val customIconPath = customIcons[app.id]
                        if (customIconPath != null) {
                            val bitmap = remember(customIconPath) { android.graphics.BitmapFactory.decodeFile(customIconPath) }
                            if (bitmap != null) {
                                Image(bitmap = bitmap.asImageBitmap(), contentDescription = app.name,
                                    modifier = Modifier.size(60.dp).clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop)
                            }
                        } else if (app.useImage) {
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
fun IconCustomizationScreen(onBack: () -> Unit, onIconChanged: () -> Unit) {
    BackHandler { onBack() }
    val isDark = isSystemInDarkTheme()
    val bg = if (isDark) Color(0xFF1C1C1E) else Color(0xFFF2F2F7)
    val card = if (isDark) Color(0xFF2C2C2E) else Color.White
    val txt = if (isDark) Color.White else Color.Black
    val context = LocalContext.current
    val iconDbHelper = remember { IconCustomizationDbHelper(context) }
    val defaultApps = remember(isDark) { getDefaultApps(isDark) }
    var customIcons by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var selectedAppId by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(Unit) {
        val records = iconDbHelper.getAllCustomIcons()
        customIcons = records.associate { it.appId to (iconDbHelper.getCustomIconFilePath(it.appId) ?: "") }
    }
    
    val imageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            selectedAppId?.let { appId ->
                iconDbHelper.copyImageToStorage(it)?.let { fn ->
                    if (iconDbHelper.saveCustomIcon(appId, fn)) {
                        val path = iconDbHelper.getCustomIconFilePath(appId)
                        if (path != null) {
                            customIcons = customIcons + (appId to path)
                            onIconChanged()
                        }
                    }
                }
            }
        }
        selectedAppId = null
    }
    
    Column(modifier = Modifier.fillMaxSize().background(bg).statusBarsPadding()) {
        Box(modifier = Modifier.fillMaxWidth().height(56.dp).background(card).padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart) {
            Text("返回", color = Color(0xFF007AFF), modifier = Modifier.clickable { onBack() })
            Text("桌面图标设置", modifier = Modifier.align(Alignment.Center), fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = txt)
        }
        
        LazyColumn(modifier = Modifier.fillMaxSize().padding(vertical = 20.dp)) {
            items(defaultApps) { app ->
                val customIconPath = customIcons[app.id]
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp)).background(card)
                    .clickable {
                        selectedAppId = app.id
                        imageLauncher.launch("image/*")
                    }.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(modifier = Modifier.size(50.dp), contentAlignment = Alignment.Center) {
                                if (customIconPath != null) {
                                    val bitmap = remember(customIconPath) { android.graphics.BitmapFactory.decodeFile(customIconPath) }
                                    if (bitmap != null) {
                                        Image(bitmap = bitmap.asImageBitmap(), contentDescription = app.name,
                                            modifier = Modifier.size(50.dp).clip(RoundedCornerShape(10.dp)), contentScale = ContentScale.Crop)
                                    }
                                } else {
                                    if (app.useImage) {
                                        val resId = context.resources.getIdentifier(app.icon, "drawable", context.packageName)
                                        if (resId != 0) {
                                            Image(painter = androidx.compose.ui.res.painterResource(id = resId),
                                                contentDescription = app.name, modifier = Modifier.size(50.dp))
                                        }
                                    } else {
                                        Text(app.icon, fontSize = 40.sp)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(app.name, fontSize = 16.sp, color = txt)
                                Text(if (customIconPath != null) "已自定义" else "使用默认图标", fontSize = 12.sp, color = Color.Gray)
                            }
                        }
                        if (customIconPath != null) {
                            androidx.compose.material3.TextButton(onClick = {
                                iconDbHelper.clearCustomIcon(app.id)
                                customIcons = customIcons - app.id
                                onIconChanged()
                            }) {
                                Text("恢复默认", color = Color(0xFF007AFF), fontSize = 14.sp)
                            }
                        } else {
                            Text(">", color = Color.Gray, fontSize = 16.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DisplaySettingsScreen(
    wallpaperDbHelper: WallpaperDbHelper,
    onBack: () -> Unit,
    onWallpaperChanged: () -> Unit,
    onNavigateToIconCustomization: () -> Unit,
    onBadgeWidgetChanged: () -> Unit
) {
    BackHandler { onBack() }
    val isDark = isSystemInDarkTheme()
    val bg = if (isDark) Color(0xFF1C1C1E) else Color(0xFFF2F2F7)
    val card = if (isDark) Color(0xFF2C2C2E) else Color.White
    val txt = if (isDark) Color.White else Color.Black
    val context = LocalContext.current
    val iconDbHelper = remember { IconCustomizationDbHelper(context) }
    var lockPreviewPath by remember { mutableStateOf(wallpaperDbHelper.getWallpaperFilePath(WallpaperType.LOCK)) }
    var homePreviewPath by remember { mutableStateOf(wallpaperDbHelper.getWallpaperFilePath(WallpaperType.HOME)) }
    var badgePreviewPath by remember { mutableStateOf(iconDbHelper.getCustomIconFilePath(BADGE_WIDGET_ID)) }
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
    val badgeLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            iconDbHelper.copyImageToStorage(it)?.let { fn ->
                if (iconDbHelper.saveCustomIcon(BADGE_WIDGET_ID, fn)) {
                    badgePreviewPath = iconDbHelper.getCustomIconFilePath(BADGE_WIDGET_ID)
                    onBadgeWidgetChanged()
                }
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
        Spacer(modifier = Modifier.height(20.dp))
        Text("桌面图标", modifier = Modifier.padding(horizontal = 26.dp, vertical = 8.dp), fontSize = 13.sp, color = Color.Gray)
        Box(modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(RoundedCornerShape(10.dp))
            .background(card).clickable(onClick = onNavigateToIconCustomization).padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("桌面图标设置", fontSize = 16.sp, color = txt)
                Text(">", color = Color.Gray, fontSize = 16.sp)
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text("吧唧小组件", modifier = Modifier.padding(horizontal = 26.dp, vertical = 8.dp), fontSize = 13.sp, color = Color.Gray)
        BadgeWidgetSettingCard("吧唧图片设置", badgePreviewPath, card, txt) { badgeLauncher.launch("image/*") }
        if (badgePreviewPath != null) {
            androidx.compose.material3.TextButton(
                onClick = {
                    iconDbHelper.clearCustomIcon(BADGE_WIDGET_ID)
                    badgePreviewPath = null
                    onBadgeWidgetChanged()
                },
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Text("恢复默认吧唧图片", color = Color(0xFF007AFF))
            }
        }
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

@Composable
fun BadgeWidgetSettingCard(
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
            Box(modifier = Modifier.size(64.dp)) {
                BadgeWidget(imagePath = previewPath, modifier = Modifier.fillMaxSize())
            }
        }
        
        @Composable
        fun KeepAliveAppLayer(
            visible: Boolean,
            keepAlive: Boolean,
            content: @Composable () -> Unit
        ) {
            if (visible) {
                content()
            } else if (keepAlive) {
                Box(modifier = Modifier.fillMaxSize().graphicsLayer { alpha = 0f }) {
                    content()
                }
            }
        }
    }
}
