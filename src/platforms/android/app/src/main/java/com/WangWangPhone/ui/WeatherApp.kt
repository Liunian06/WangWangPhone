package com.WangWangPhone.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
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

data class DailyForecast(val day: String, val icon: String, val low: String, val high: String)
data class HourlyForecast(val time: String, val icon: String, val temp: String)
data class WeatherDetail(val title: String, val value: String, val subtitle: String)

@Composable
fun WeatherAppScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    var city by remember { mutableStateOf("广州") }
    var currentTemp by remember { mutableStateOf("25°") }
    var condition by remember { mutableStateOf("多云") }
    var range by remember { mutableStateOf("最高 29° 最低 21°") }
    
    val hourlyForecast = listOf(
        HourlyForecast("现在", "⛅", "25°"),
        HourlyForecast("14时", "🌤️", "27°"),
        HourlyForecast("15时", "☀️", "28°"),
        HourlyForecast("16时", "☀️", "29°"),
        HourlyForecast("17时", "⛅", "28°"),
        HourlyForecast("18时", "🌥️", "26°"),
        HourlyForecast("19时", "🌙", "24°"),
        HourlyForecast("20时", "🌙", "23°"),
        HourlyForecast("21时", "🌙", "22°"),
        HourlyForecast("22时", "🌙", "21°")
    )

    val forecast = listOf(
        DailyForecast("今天", "⛅", "21°", "29°"),
        DailyForecast("明天", "🌧️", "20°", "26°"),
        DailyForecast("周四", "☀️", "22°", "30°"),
        DailyForecast("周五", "☀️", "23°", "31°"),
        DailyForecast("周六", "⛅", "22°", "29°"),
        DailyForecast("周日", "🌧️", "21°", "27°"),
        DailyForecast("下周一", "⛈️", "20°", "25°"),
        DailyForecast("下周二", "🌤️", "22°", "28°"),
        DailyForecast("下周三", "☀️", "23°", "30°"),
        DailyForecast("下周四", "⛅", "22°", "29°")
    )

    val weatherDetails = listOf(
        WeatherDetail("体感温度", "27°", "湿度使体感温度更高"),
        WeatherDetail("湿度", "68%", "露点温度 18°"),
        WeatherDetail("能见度", "16 公里", "能见度良好"),
        WeatherDetail("紫外线指数", "5", "中等"),
        WeatherDetail("风速", "东南风 12 km/h", "阵风 20 km/h"),
        WeatherDetail("气压", "1013 hPa", "正常")
    )

    BackHandler { onClose() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF4FACFE), Color(0xFF00F2FE))))
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // 关闭按钮
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text(
                        "完成",
                        color = Color.White,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .clip(RoundedCornerShape(5.dp))
                            .background(Color.White.copy(alpha = 0.2f))
                            .clickable { onClose() }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        fontSize = 14.sp
                    )
                }
            }

            // 城市和温度
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(30.dp))
                    Text(city, color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Normal)
                    Text(currentTemp, color = Color.White, fontSize = 96.sp, fontWeight = FontWeight.Thin)
                    Text(condition, color = Color.White.copy(alpha = 0.8f), fontSize = 20.sp, fontWeight = FontWeight.Medium)
                    Text(range, color = Color.White.copy(alpha = 0.8f), fontSize = 20.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(30.dp))
                }
            }

            // 小时预报
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .clip(RoundedCornerShape(15.dp))
                        .background(Color.Black.copy(alpha = 0.1f))
                        .padding(15.dp)
                ) {
                    Column {
                        Text("今天下午将会以多云为主，当前气温 25°。",
                            color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Divider(color = Color.White.copy(alpha = 0.2f), thickness = 0.5.dp)
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            hourlyForecast.forEach { item ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.width(50.dp)
                                ) {
                                    Text(item.time, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(item.icon, fontSize = 24.sp)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(item.temp, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }

            // 10天预报
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .clip(RoundedCornerShape(15.dp))
                        .background(Color.Black.copy(alpha = 0.1f))
                        .padding(15.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("📅  10 日天气预报", color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Divider(color = Color.White.copy(alpha = 0.2f), thickness = 0.5.dp)

                        forecast.forEach { item ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(item.day, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Medium, modifier = Modifier.width(80.dp))
                                Text(item.icon, fontSize = 22.sp, modifier = Modifier.width(35.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(item.low, color = Color.White.copy(alpha = 0.6f), fontSize = 18.sp, fontWeight = FontWeight.Medium)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    // 温度条
                                    Box(modifier = Modifier.width(80.dp).height(4.dp).clip(RoundedCornerShape(2.dp))) {
                                        Box(modifier = Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.2f)))
                                        val lowVal = item.low.replace("°", "").toIntOrNull() ?: 20
                                        val highVal = item.high.replace("°", "").toIntOrNull() ?: 30
                                        val startFraction = ((lowVal - 18).toFloat() / 15f).coerceIn(0f, 1f)
                                        val endFraction = ((highVal - 18).toFloat() / 15f).coerceIn(0f, 1f)
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .fillMaxWidth(endFraction)
                                                .padding(start = (80 * startFraction).dp)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(
                                                    Brush.horizontalGradient(
                                                        listOf(Color(0xFF4FACFE), Color(0xFFFFC107), Color(0xFFFF5722))
                                                    )
                                                )
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(item.high, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }

            // 天气详情网格
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    for (i in weatherDetails.indices step 2) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            WeatherDetailCard(
                                detail = weatherDetails[i],
                                modifier = Modifier.weight(1f)
                            )
                            if (i + 1 < weatherDetails.size) {
                                WeatherDetailCard(
                                    detail = weatherDetails[i + 1],
                                    modifier = Modifier.weight(1f)
                                )
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(40.dp)) }
        }
    }
}

@Composable
fun WeatherDetailCard(detail: WeatherDetail, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(15.dp))
            .background(Color.Black.copy(alpha = 0.1f))
            .padding(15.dp)
    ) {
        Column {
            Text(
                detail.title,
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                detail.value,
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                detail.subtitle,
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp
            )
        }
    }
}
