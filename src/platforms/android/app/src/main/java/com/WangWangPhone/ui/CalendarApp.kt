package com.WangWangPhone.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.*

data class CalendarEvent(
    val id: String,
    val title: String,
    val location: String,
    val time: String,
    val color: Color,
    val date: LocalDate
)

@Composable
fun CalendarAppScreen(onClose: () -> Unit) {
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }

    BackHandler { onClose() }

    // 示例事件数据
    val events = remember {
        val today = LocalDate.now()
        listOf(
            CalendarEvent("1", "开发任务", "广州", "全天", Color(0xFFFF3B30), today),
            CalendarEvent("2", "团队周会", "线上会议", "10:00 - 11:00", Color(0xFF007AFF), today),
            CalendarEvent("3", "午餐约会", "天河区", "12:00 - 13:00", Color(0xFFFF9500), today),
            CalendarEvent("4", "代码评审", "办公室", "14:00 - 15:30", Color(0xFF5856D6), today),
            CalendarEvent("5", "项目发布会", "会议室A", "09:00 - 10:00", Color(0xFF34C759), today.plusDays(1)),
            CalendarEvent("6", "客户会议", "线上", "15:00 - 16:00", Color(0xFFFF2D55), today.plusDays(1)),
            CalendarEvent("7", "健身", "健身房", "18:00 - 19:00", Color(0xFF007AFF), today.plusDays(2)),
            CalendarEvent("8", "读书会", "图书馆", "19:30 - 21:00", Color(0xFFAF52DE), today.plusDays(3))
        )
    }

    val daysInMonth = currentMonth.lengthOfMonth()
    val firstDayOfWeek = currentMonth.atDay(1).dayOfWeek.value % 7
    val days = (1..daysInMonth).toList()
    val placeholders = (0 until firstDayOfWeek).toList()

    // 获取选中日期的事件
    val selectedEvents = events.filter { it.date == selectedDate }

    // 获取有事件的日期集合
    val eventDates = events.map { it.date }.toSet()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
    ) {
        // Header with month navigation
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 15.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "‹",
                    fontSize = 28.sp,
                    color = Color(0xFFFF3B30),
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clickable { currentMonth = currentMonth.minusMonths(1) }
                        .padding(end = 16.dp)
                )
                Text(
                    "${currentMonth.month.getDisplayName(TextStyle.FULL, Locale.CHINA)} ${currentMonth.year}",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Text(
                    "›",
                    fontSize = 28.sp,
                    color = Color(0xFFFF3B30),
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clickable { currentMonth = currentMonth.plusMonths(1) }
                        .padding(start = 16.dp)
                )
            }
            Row {
                Text(
                    "今天",
                    color = Color(0xFFFF3B30),
                    modifier = Modifier
                        .clickable {
                            currentMonth = YearMonth.now()
                            selectedDate = LocalDate.now()
                        }
                        .padding(end = 16.dp),
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp
                )
                Text(
                    "完成",
                    color = Color(0xFFFF3B30),
                    modifier = Modifier.clickable { onClose() },
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp
                )
            }
        }

        // Weekdays
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp)) {
            listOf("日", "一", "二", "三", "四", "五", "六").forEach {
                Text(
                    it,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontSize = 12.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Days Grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
            userScrollEnabled = false
        ) {
            items(placeholders) { Box(modifier = Modifier.aspectRatio(1f)) }
            items(days) { day ->
                val date = currentMonth.atDay(day)
                val isSelected = date == selectedDate
                val isToday = date == LocalDate.now()
                val hasEvent = eventDates.contains(date)

                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clickable { selectedDate = date },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(contentAlignment = Alignment.Center) {
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(35.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFFF3B30))
                                )
                            }
                            Text(
                                day.toString(),
                                color = if (isSelected) Color.White else if (isToday) Color(0xFFFF3B30) else Color.Black,
                                fontSize = 18.sp,
                                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                        // 事件指示器小圆点
                        if (hasEvent && !isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFF3B30))
                            )
                        } else {
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }
            }
        }

        Divider(
            modifier = Modifier.padding(vertical = 10.dp),
            color = Color.LightGray.copy(alpha = 0.5f)
        )

        // 事件列表
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 20.dp)
        ) {
            if (selectedEvents.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("📅", fontSize = 40.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "没有日程",
                            color = Color.Gray,
                            fontSize = 16.sp
                        )
                    }
                }
            } else {
                // 全天事件
                val allDayEvents = selectedEvents.filter { it.time == "全天" }
                if (allDayEvents.isNotEmpty()) {
                    item {
                        Text(
                            "全天",
                            color = Color.Black,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    allDayEvents.forEach { event ->
                        item {
                            EventRow(event = event)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                    item {
                        Divider(color = Color.LightGray.copy(alpha = 0.3f))
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                // 定时事件
                val timedEvents = selectedEvents.filter { it.time != "全天" }.sortedBy { it.time }
                if (timedEvents.isNotEmpty()) {
                    item {
                        Text(
                            "日程",
                            color = Color.Black,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    timedEvents.forEach { event ->
                        item {
                            EventRow(event = event, showTime = true)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EventRow(event: CalendarEvent, showTime: Boolean = false) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(event.color.copy(alpha = 0.08f))
            .padding(12.dp)
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(if (showTime) 50.dp else 40.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(event.color)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                event.title,
                color = Color.Black,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            if (showTime) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(event.time, color = Color.Gray, fontSize = 13.sp)
            }
            if (event.location.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    "📍 ${event.location}",
                    color = Color.Gray,
                    fontSize = 13.sp
                )
            }
        }
    }
}
