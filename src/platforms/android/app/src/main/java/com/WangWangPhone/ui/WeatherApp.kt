package com.WangWangPhone.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.WangWangPhone.core.WeatherCacheDbHelper
import kotlinx.coroutines.delay

data class DailyForecast(val day: String, val icon: String, val low: String, val high: String)

@Composable
fun WeatherAppScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    var city by remember { mutableStateOf("广州") }
    var currentTemp by remember { mutableStateOf("25°") }
    var condition by remember { mutableStateOf("多云") }
    var range by remember { mutableStateOf("最高 29° 最低 21°") }
    
    val forecast = listOf(
        DailyForecast("今天", "⛅", "21°", "29°"),
        DailyForecast("明天", "🌧️", "20°", "26°"),
        DailyForecast("周四", "☀️", "22°", "30°"),
        DailyForecast("周五", "☀️", "23°", "31°"),
        DailyForecast("周六", "⛅", "22°", "29°"),
        DailyForecast("周日", "🌧️", "21°", "27°"),
        DailyForecast("下周一", "⛈️", "20°", "25°")
    )

    BackHandler { onClose() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF4FACFE), Color(0xFF00F2FE))))
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(60.dp))
            Text(city, color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Normal)
            Text(currentTemp, color = Color.White, fontSize = 96.sp, fontWeight = FontWeight.Thin)
            Text(condition, color = Color.White.copy(alpha = 0.8f), fontSize = 20.sp, fontWeight = FontWeight.Medium)
            Text(range, color = Color.White.copy(alpha = 0.8f), fontSize = 20.sp, fontWeight = FontWeight.Medium)
            
            Spacer(modifier = Modifier.height(40.dp))
            
            // Forecast List
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(Color.Black.copy(alpha = 0.1f))
                    .padding(15.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("10 日天气预报", color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Divider(color = Color.White.copy(alpha = 0.2f), thickness = 0.5.dp)
                    
                    forecast.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(item.day, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Medium, modifier = Modifier.width(80.dp))
                            Text(item.icon, fontSize = 24.sp, modifier = Modifier.width(40.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(item.low, color = Color.White.copy(alpha = 0.6f), fontSize = 20.sp, fontWeight = FontWeight.Medium)
                                Spacer(modifier = Modifier.width(10.dp))
                                Box(modifier = Modifier.width(100.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(Color.White.copy(alpha = 0.2f)))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(item.high, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        }
        
        // Back Button (Optional as we have BackHandler, but good for UI)
        Text(
            "完成",
            color = Color.White,
            modifier = Modifier.align(Alignment.TopEnd).padding(20.dp).clip(RoundedCornerShape(5.dp)).background(Color.White.copy(alpha = 0.2f)).padding(horizontal = 10.dp, vertical = 5.dp),
            fontSize = 14.sp
        )
    }
}
