package com.WangWangPhone.ui

import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

data class AppIcon(val name: String, val icon: String, val color: Brush, val useImage: Boolean = false)

// Mock Location & Weather Logic (In real app this would be in Repository/ViewModel)
suspend fun fetchLocation(): String {
    // In a real Android app, you would use LocationManager or FusedLocationProviderClient
    // For this prototype, we simulate an API call or logic similar to the JS version
    // Mocking an async operation:
    delay(500)
    return "广州" // Mock result
}

data class WeatherInfo(
    val temp: String,
    val description: String,
    val icon: String,
    val range: String
)

suspend fun fetchWeather(city: String): WeatherInfo {
    delay(500) // Mock API
    // Real app would use Retrofit to call goweather.herokuapp.com
    return WeatherInfo("25°", "多云", "⛅", "最高 29° 最低 21°")
}

@Composable
fun WidgetsSection() {
    // State for location and weather
    var city by remember { mutableStateOf("...") }
    var weather by remember { mutableStateOf<WeatherInfo?>(null) }
    
    LaunchedEffect(Unit) {
        city = fetchLocation()
        if (city.isNotEmpty()) {
            weather = fetchWeather(city)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(15.dp)
    ) {
        // Clock Widget
        ClockWidget(city = city, modifier = Modifier.weight(1f))
        
        // Weather Widget
        WeatherWidget(city = city, weather = weather, modifier = Modifier.weight(1f))
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
fun SettingsScreen(isActivated: Boolean, expiryDate: String, onBack: () -> Unit, onNavigateToActivation: () -> Unit) {
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
        // Header
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

        // Section: Activation
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
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .statusBarsPadding()
    ) {
        // Header
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

        val context = androidx.compose.ui.platform.LocalContext.current
        val androidId = remember {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "UNKNOWN_DEVICE"
        }
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
                Text(androidId, color = textColor)
            }

            Spacer(modifier = Modifier.height(10.dp))

            androidx.compose.material3.Button(
                onClick = { clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(androidId)) },
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

            Spacer(modifier = Modifier.height(30.dp))

            androidx.compose.material3.Button(
                onClick = {
                    // 模拟激活逻辑
                    if (licenseKey.startsWith("WANGWANG-")) {
                        // 激活成功反馈 (这里由于是 UI 演示，不涉及底层 C++ 调用成功后的持久化)
                        onActivated()
                        onBack()
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
    var isActivated by remember { mutableStateOf(false) }
    var expiryDate by remember { mutableStateOf("2030-01-01") } // 模拟获取到的过期时间

    val isDark = isSystemInDarkTheme()
    val apps = listOf(
        AppIcon("电话", "📞", Brush.linearGradient(listOf(Color(0xFFFF9A9E), Color(0xFFFECFEF)))),
        AppIcon("信息", "💬", Brush.linearGradient(listOf(Color(0xFFA1C4FD), Color(0xFFC2E9FB)))),
        AppIcon("Safari", "🧭", Brush.linearGradient(listOf(Color(0xFF84FAB0), Color(0xFF8FD3F4)))),
        AppIcon("音乐", "🎵", Brush.linearGradient(listOf(Color(0xFFF6D365), Color(0xFFFDA085)))),
        AppIcon("相机", "📷", Brush.linearGradient(listOf(Color.White, Color.LightGray))),
        AppIcon("日历", "📅", Brush.linearGradient(listOf(Color.White, Color.LightGray))),
        AppIcon("设置", if (isDark) "ic_settings_dark" else "ic_settings_light", Brush.linearGradient(listOf(Color.White, Color.LightGray)), useImage = true),
        AppIcon("汪汪", "🐶", Brush.linearGradient(listOf(Color.White, Color.LightGray)))
    )

    Box(modifier = Modifier.fillMaxSize()) {
        HomeScreenContent(onSettingsClick = { showSettings = true })

        if (showSettings) {
            SettingsScreen(
                isActivated = isActivated,
                expiryDate = expiryDate,
                onBack = { showSettings = false },
                onNavigateToActivation = {
                    showActivation = true
                }
            )
        }

        if (showActivation) {
            ActivationScreen(
                onBack = { showActivation = false },
                onActivated = { isActivated = true }
            )
        }
    }
}

@Composable
fun HomeScreenContent(onSettingsClick: () -> Unit) {
    val isDark = isSystemInDarkTheme()
    val apps = listOf(
        AppIcon("电话", "📞", Brush.linearGradient(listOf(Color(0xFFFF9A9E), Color(0xFFFECFEF)))),
        AppIcon("信息", "💬", Brush.linearGradient(listOf(Color(0xFFA1C4FD), Color(0xFFC2E9FB)))),
        AppIcon("Safari", "🧭", Brush.linearGradient(listOf(Color(0xFF84FAB0), Color(0xFF8FD3F4)))),
        AppIcon("音乐", "🎵", Brush.linearGradient(listOf(Color(0xFFF6D365), Color(0xFFFDA085)))),
        AppIcon("相机", "📷", Brush.linearGradient(listOf(Color.White, Color.LightGray))),
        AppIcon("日历", "📅", Brush.linearGradient(listOf(Color.White, Color.LightGray))),
        AppIcon("设置", if (isDark) "ic_settings_dark" else "ic_settings_light", Brush.linearGradient(listOf(Color.White, Color.LightGray)), useImage = true),
        AppIcon("汪汪", "🐶", Brush.linearGradient(listOf(Color.White, Color.LightGray)))
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // 背景 (实际应为 Live2D 或壁纸)
        Box(modifier = Modifier.fillMaxSize().background(Color.Black))

        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            // 小组件区域
            WidgetsSection()

            // 应用网格
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                contentPadding = PaddingValues(20.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                items(apps) { app ->
                    AppIconItem(app, onClick = {
                        if (app.name == "设置") onSettingsClick()
                    })
                }
            }
        }

        // Dock 栏
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 20.dp, start = 15.dp, end = 15.dp)
                .fillMaxWidth()
                .height(90.dp)
                .clip(RoundedCornerShape(30.dp))
        ) {
            // 磨砂玻璃背景层
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.3f))
                    .blur(20.dp)
            )

            // 应用图标层 (在背景层之上)
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                apps.take(4).forEach { app ->
                    Box(
                        modifier = Modifier
                            .size(55.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (app.useImage) {
                            val context = androidx.compose.ui.platform.LocalContext.current
                            val resId = context.resources.getIdentifier(app.icon, "drawable", context.packageName)
                            if (resId != 0) {
                                Image(
                                    painter = androidx.compose.ui.res.painterResource(id = resId),
                                    contentDescription = app.name,
                                    modifier = Modifier.size(35.dp)
                                )
                            }
                        } else {
                            Text(app.icon, fontSize = 28.sp)
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

@Composable
fun AppIconItem(app: AppIcon, onClick: () -> Unit = {}) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(60.dp),
            contentAlignment = Alignment.Center
        ) {
            if (app.useImage) {
                val context = androidx.compose.ui.platform.LocalContext.current
                val resId = context.resources.getIdentifier(app.icon, "drawable", context.packageName)
                if (resId != 0) {
                    Image(
                        painter = androidx.compose.ui.res.painterResource(id = resId),
                        contentDescription = app.name,
                        modifier = Modifier.size(40.dp)
                    )
                }
            } else {
                Text(app.icon, fontSize = 30.sp)
            }
        }
        Spacer(modifier = Modifier.height(5.dp))
        Text(app.name, color = Color.White, fontSize = 12.sp)
    }
}