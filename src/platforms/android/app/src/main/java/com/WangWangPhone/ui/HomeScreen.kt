package com.WangWangPhone.ui

import androidx.compose.foundation.background
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

data class AppIcon(val name: String, val icon: String, val color: Brush)

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
                Text(
                    text = weather?.icon ?: "❓",
                    fontSize = 24.sp
                )
                Spacer(modifier = Modifier.height(5.dp))
                Text(
                    text = weather?.description ?: "加载中...",
                    color = Color.White,
                    fontSize = 14.sp
                )
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
fun HomeScreen() {
    val apps = listOf(
        AppIcon("电话", "📞", Brush.linearGradient(listOf(Color(0xFFFF9A9E), Color(0xFFFECFEF)))),
        AppIcon("信息", "💬", Brush.linearGradient(listOf(Color(0xFFA1C4FD), Color(0xFFC2E9FB)))),
        AppIcon("Safari", "🧭", Brush.linearGradient(listOf(Color(0xFF84FAB0), Color(0xFF8FD3F4)))),
        AppIcon("音乐", "🎵", Brush.linearGradient(listOf(Color(0xFFF6D365), Color(0xFFFDA085)))),
        AppIcon("相机", "📷", Brush.linearGradient(listOf(Color.White, Color.LightGray))),
        AppIcon("日历", "📅", Brush.linearGradient(listOf(Color.White, Color.LightGray))),
        AppIcon("设置", "⚙️", Brush.linearGradient(listOf(Color.White, Color.LightGray))),
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
                    AppIconItem(app)
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
                            .size(55.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(app.color),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(app.icon, fontSize = 28.sp)
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
fun AppIconItem(app: AppIcon) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(app.color),
            contentAlignment = Alignment.Center
        ) {
            Text(app.icon, fontSize = 30.sp)
        }
        Spacer(modifier = Modifier.height(5.dp))
        Text(app.name, color = Color.White, fontSize = 12.sp)
    }
}