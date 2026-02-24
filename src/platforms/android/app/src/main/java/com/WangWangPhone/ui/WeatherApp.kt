package com.WangWangPhone.ui

import android.icu.text.Transliterator
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.WangWangPhone.core.WeatherCacheRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.util.Locale

data class DailyForecast(val day: String, val icon: String, val low: String, val high: String)
data class HourlyForecast(val time: String, val icon: String, val temp: String)
data class WeatherDetail(val title: String, val value: String, val subtitle: String)

data class WeatherAppRealtimeData(
    val city: String,
    val weatherInfo: WeatherInfo,
    val summary: String,
    val hourlyForecast: List<HourlyForecast>,
    val dailyForecast: List<DailyForecast>,
    val weatherDetails: List<WeatherDetail>
)

private val weatherAppHttpClient by lazy { OkHttpClient() }

private fun weatherAppContainsChinese(text: String): Boolean {
    return text.any { it in '\u4e00'..'\u9fff' }
}

private fun weatherAppSanitizeCityName(city: String): String {
    return city
        .trim()
        .replace("特别行政区", "")
        .replace("自治区", "")
        .replace("市", "")
}

private fun weatherAppCityToPinyin(city: String): String {
    val cleaned = weatherAppSanitizeCityName(city)
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

private fun weatherAppNormalizeTemperature(rawTemp: String): String {
    val cleaned = rawTemp.trim()
    if (cleaned.isEmpty() || cleaned == "--") return "--"
    val match = Regex("[-+]?\\d+").find(cleaned)
    return if (match != null) "${match.value.replace("+", "")}°" else cleaned.replace(" ", "")
}

private fun weatherAppLocalizeDescription(rawDescription: String): String {
    val cleaned = rawDescription.trim()
    if (cleaned.isEmpty()) return "天气未知"
    if (weatherAppContainsChinese(cleaned)) return cleaned

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

private fun weatherAppMapIcon(description: String): String {
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

private fun weatherAppFormatWindKmph(raw: String): String {
    val cleaned = raw.trim()
    if (cleaned.isEmpty() || cleaned == "--") return "--"
    return if (cleaned.contains("km/h", ignoreCase = true)) {
        cleaned.replace("km/h", "公里/小时", ignoreCase = true)
    } else {
        "$cleaned 公里/小时"
    }
}

private fun weatherAppBuildRangeText(maxTemp: String, minTemp: String, windKmph: String): String {
    val maxText = if (maxTemp.isNotBlank()) weatherAppNormalizeTemperature(maxTemp) else "--"
    val minText = if (minTemp.isNotBlank()) weatherAppNormalizeTemperature(minTemp) else "--"
    val windText = weatherAppFormatWindKmph(windKmph)
    return "最高 $maxText 最低 $minText | 风速 $windText"
}

private fun weatherAppDayLabel(dateText: String, index: Int): String {
    if (index == 0) return "今天"
    if (index == 1) return "明天"
    return try {
        when (LocalDate.parse(dateText).dayOfWeek) {
            DayOfWeek.MONDAY -> "周一"
            DayOfWeek.TUESDAY -> "周二"
            DayOfWeek.WEDNESDAY -> "周三"
            DayOfWeek.THURSDAY -> "周四"
            DayOfWeek.FRIDAY -> "周五"
            DayOfWeek.SATURDAY -> "周六"
            DayOfWeek.SUNDAY -> "周日"
        }
    } catch (_: DateTimeParseException) {
        "第${index + 1}天"
    }
}

private fun weatherAppHourLabel(rawTime: String): String {
    val value = rawTime.trim().toIntOrNull() ?: return "--"
    val hour = (value / 100).coerceIn(0, 23)
    return String.format(Locale.CHINA, "%02d时", hour)
}

private fun weatherAppTemperatureInt(tempText: String, fallback: Int): Int {
    val value = Regex("[-+]?\\d+").find(tempText)?.value?.toIntOrNull()
    return value ?: fallback
}

private fun weatherAppIsUnknownWeather(temp: String, description: String): Boolean {
    val normalizedDesc = description.trim()
    if (normalizedDesc.isEmpty()) return true
    val lowerDesc = normalizedDesc.lowercase(Locale.ROOT)
    if (normalizedDesc.contains("未知") || lowerDesc == "unknown") return true

    val normalizedTemp = temp.trim()
    return normalizedTemp == "--" && (normalizedDesc == "--" || lowerDesc == "n/a")
}

private fun defaultHourlyForecast(temp: String = "--"): List<HourlyForecast> {
    return listOf(
        HourlyForecast("现在", "🌤️", temp),
        HourlyForecast("03时", "🌙", "--"),
        HourlyForecast("06时", "⛅", "--"),
        HourlyForecast("09时", "⛅", "--"),
        HourlyForecast("12时", "☀️", "--"),
        HourlyForecast("15时", "🌤️", "--"),
        HourlyForecast("18时", "⛅", "--"),
        HourlyForecast("21时", "🌙", "--")
    )
}

private fun defaultDailyForecast(): List<DailyForecast> {
    return listOf(
        DailyForecast("今天", "🌤️", "--", "--"),
        DailyForecast("明天", "🌤️", "--", "--"),
        DailyForecast("周三", "🌤️", "--", "--")
    )
}

private fun defaultWeatherDetails(temp: String = "--"): List<WeatherDetail> {
    return listOf(
        WeatherDetail("体感温度", temp, "人体感知温度"),
        WeatherDetail("湿度", "--", "空气湿度"),
        WeatherDetail("能见度", "--", "当前视线范围"),
        WeatherDetail("紫外线指数", "--", "紫外线强度"),
        WeatherDetail("风速", "--", "实时风向风速"),
        WeatherDetail("气压", "--", "大气压强")
    )
}

private suspend fun fetchWeatherAppRealtimeData(city: String): WeatherAppRealtimeData? {
    return withContext(Dispatchers.IO) {
        try {
            val pinyinCity = weatherAppCityToPinyin(city).ifBlank { weatherAppSanitizeCityName(city) }
            val encodedCity = URLEncoder.encode(pinyinCity, "UTF-8")
            val request = Request.Builder()
                .url("https://wttr.in/$encodedCity?format=j1")
                .header("User-Agent", "WangWangPhone/1.0")
                .build()

            weatherAppHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body?.string().orEmpty()
                val json = JSONObject(body)

                val current = json.optJSONArray("current_condition")?.optJSONObject(0)
                val weatherDays = json.optJSONArray("weather")
                val today = weatherDays?.optJSONObject(0)

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
                val description = weatherAppLocalizeDescription(zhDesc.ifBlank { enDesc.ifBlank { "天气未知" } })

                val tempC = current?.optString("temp_C", "--").orEmpty()
                val feelsLikeC = current?.optString("FeelsLikeC", "--").orEmpty()
                val maxTemp = today?.optString("maxtempC", "").orEmpty()
                val minTemp = today?.optString("mintempC", "").orEmpty()
                val windKmph = current?.optString("windspeedKmph", "--").orEmpty()

                val weatherInfo = WeatherInfo(
                    temp = weatherAppNormalizeTemperature(tempC),
                    description = description,
                    icon = weatherAppMapIcon(description),
                    range = weatherAppBuildRangeText(maxTemp, minTemp, windKmph)
                )

                val summary = "当前${weatherInfo.description}，气温${weatherInfo.temp}，体感${weatherAppNormalizeTemperature(feelsLikeC)}"

                val hourlyForecast = mutableListOf(
                    HourlyForecast("现在", weatherInfo.icon, weatherInfo.temp)
                )
                val todayHourly = today?.optJSONArray("hourly")
                if (todayHourly != null) {
                    val count = minOf(todayHourly.length(), 9)
                    for (i in 0 until count) {
                        val item = todayHourly.optJSONObject(i) ?: continue
                        val time = weatherAppHourLabel(item.optString("time", ""))
                        val hourTemp = weatherAppNormalizeTemperature(item.optString("tempC", "--"))
                        val hourZh = item
                            .optJSONArray("lang_zh")
                            ?.optJSONObject(0)
                            ?.optString("value", "")
                            .orEmpty()
                        val hourEn = item
                            .optJSONArray("weatherDesc")
                            ?.optJSONObject(0)
                            ?.optString("value", "")
                            .orEmpty()
                        val hourDesc = weatherAppLocalizeDescription(hourZh.ifBlank { hourEn.ifBlank { "天气未知" } })
                        hourlyForecast.add(HourlyForecast(time, weatherAppMapIcon(hourDesc), hourTemp))
                    }
                }

                val dailyForecast = mutableListOf<DailyForecast>()
                if (weatherDays != null) {
                    val count = minOf(weatherDays.length(), 10)
                    for (i in 0 until count) {
                        val dayObj = weatherDays.optJSONObject(i) ?: continue
                        val dateText = dayObj.optString("date", "")
                        val lowText = weatherAppNormalizeTemperature(dayObj.optString("mintempC", "--"))
                        val highText = weatherAppNormalizeTemperature(dayObj.optString("maxtempC", "--"))
                        val hourlyArray = dayObj.optJSONArray("hourly")
                        val iconSource = hourlyArray?.optJSONObject(minOf(4, (hourlyArray.length() - 1).coerceAtLeast(0)))
                            ?: hourlyArray?.optJSONObject(0)
                        val dayZh = iconSource
                            ?.optJSONArray("lang_zh")
                            ?.optJSONObject(0)
                            ?.optString("value", "")
                            .orEmpty()
                        val dayEn = iconSource
                            ?.optJSONArray("weatherDesc")
                            ?.optJSONObject(0)
                            ?.optString("value", "")
                            .orEmpty()
                        val dayDesc = weatherAppLocalizeDescription(dayZh.ifBlank { dayEn.ifBlank { description } })
                        dailyForecast.add(
                            DailyForecast(
                                day = weatherAppDayLabel(dateText, i),
                                icon = weatherAppMapIcon(dayDesc),
                                low = lowText,
                                high = highText
                            )
                        )
                    }
                }

                val humidityRaw = current?.optString("humidity", "--").orEmpty().trim()
                val visibilityRaw = current?.optString("visibility", "--").orEmpty().trim()
                val uvRaw = current?.optString("uvIndex", "--").orEmpty().trim()
                val pressureRaw = current?.optString("pressure", "--").orEmpty().trim()
                val windDirRaw = current?.optString("winddir16Point", "").orEmpty().trim()

                val humidity = if (humidityRaw.isEmpty() || humidityRaw == "--") "--" else "$humidityRaw%"
                val visibility = if (visibilityRaw.isEmpty() || visibilityRaw == "--") "--" else "$visibilityRaw 公里"
                val uv = if (uvRaw.isEmpty()) "--" else uvRaw
                val pressure = if (pressureRaw.isEmpty() || pressureRaw == "--") "--" else "$pressureRaw hPa"
                val wind = weatherAppFormatWindKmph(windKmph)
                val windValue = if (wind == "--") "--" else if (windDirRaw.isNotEmpty()) "${windDirRaw}风 $wind" else wind

                val details = listOf(
                    WeatherDetail("体感温度", weatherAppNormalizeTemperature(feelsLikeC), "人体感知温度"),
                    WeatherDetail("湿度", humidity, "空气湿度"),
                    WeatherDetail("能见度", visibility, "当前视线范围"),
                    WeatherDetail("紫外线指数", uv, "紫外线强度"),
                    WeatherDetail("风速", windValue, "实时风向风速"),
                    WeatherDetail("气压", pressure, "大气压强")
                )

                WeatherAppRealtimeData(
                    city = city,
                    weatherInfo = weatherInfo,
                    summary = summary,
                    hourlyForecast = hourlyForecast,
                    dailyForecast = dailyForecast,
                    weatherDetails = details
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

@Composable
fun WeatherAppScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val weatherCacheDbHelper = remember { WeatherCacheDbHelper(context) }

    var city by remember { mutableStateOf("定位中...") }
    var currentTemp by remember { mutableStateOf("--") }
    var condition by remember { mutableStateOf("加载中...") }
    var range by remember { mutableStateOf("最高 -- 最低 -- | 风速 --") }
    var summary by remember { mutableStateOf("正在获取天气数据...") }

    var hourlyForecast by remember { mutableStateOf(defaultHourlyForecast()) }
    var forecast by remember { mutableStateOf(defaultDailyForecast()) }
    var weatherDetails by remember { mutableStateOf(defaultWeatherDetails()) }

    LaunchedEffect(Unit) {
        val currentCity = fetchLocation(weatherCacheDbHelper)
        city = currentCity

        if (currentCity.isNotBlank() && currentCity != "...") {
            val cached = weatherCacheDbHelper.getTodayWeatherCache(currentCity)
            if (cached != null && !weatherAppIsUnknownWeather(cached.temp, cached.description)) {
                val localizedDescription = weatherAppLocalizeDescription(cached.description)
                currentTemp = cached.temp
                condition = localizedDescription
                range = cached.range
                summary = "当前$localizedDescription，气温${cached.temp}"
            }

            val realtime = fetchWeatherAppRealtimeData(currentCity)
            if (realtime != null) {
                city = realtime.city
                currentTemp = realtime.weatherInfo.temp
                condition = realtime.weatherInfo.description
                range = realtime.weatherInfo.range
                summary = realtime.summary
                hourlyForecast = if (realtime.hourlyForecast.isNotEmpty()) realtime.hourlyForecast else defaultHourlyForecast(currentTemp)
                forecast = if (realtime.dailyForecast.isNotEmpty()) realtime.dailyForecast else defaultDailyForecast()
                weatherDetails = if (realtime.weatherDetails.isNotEmpty()) realtime.weatherDetails else defaultWeatherDetails(currentTemp)

                if (!weatherAppIsUnknownWeather(realtime.weatherInfo.temp, realtime.weatherInfo.description)) {
                    weatherCacheDbHelper.saveWeatherCache(
                        WeatherCacheRecord(
                            city = currentCity,
                            temp = realtime.weatherInfo.temp,
                            description = realtime.weatherInfo.description,
                            icon = realtime.weatherInfo.icon,
                            range = realtime.weatherInfo.range,
                            requestDate = WeatherCacheDbHelper.getTodayDateString()
                        )
                    )
                }
                weatherCacheDbHelper.clearExpiredCache()
            } else if (condition == "加载中...") {
                condition = "天气未知"
                summary = "天气数据暂不可用"
            }
        }
    }

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
                    Text(condition, color = Color.White.copy(alpha = 0.86f), fontSize = 20.sp, fontWeight = FontWeight.Medium)
                    Text(range, color = Color.White.copy(alpha = 0.86f), fontSize = 17.sp, fontWeight = FontWeight.Medium)
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
                        Text(summary, color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
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

            // 未来预报
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
                        Text("📅  未来天气预报", color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp, fontWeight = FontWeight.Bold)
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
                                    Box(modifier = Modifier.width(80.dp).height(4.dp).clip(RoundedCornerShape(2.dp))) {
                                        Box(modifier = Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.2f)))
                                        val lowVal = weatherAppTemperatureInt(item.low, 20)
                                        val highVal = weatherAppTemperatureInt(item.high, 30)
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
