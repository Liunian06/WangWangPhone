package com.WangWangPhone.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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

@Composable
fun CalendarAppScreen(onClose: () -> Unit) {
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    
    BackHandler { onClose() }

    val daysInMonth = currentMonth.lengthOfMonth()
    val firstDayOfWeek = currentMonth.atDay(1).dayOfWeek.value % 7 // 0 is Sunday
    val days = (1..daysInMonth).toList()
    val placeholders = (0 until firstDayOfWeek).toList()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 15.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "${currentMonth.month.getDisplayName(TextStyle.FULL, Locale.CHINA)} ${currentMonth.year}",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Text(
                "完成",
                color = Color(0xFFFF3B30),
                modifier = Modifier.clickable { onClose() },
                fontWeight = FontWeight.Medium
            )
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
                
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clickable { selectedDate = date },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Box(modifier = Modifier.size(35.dp).clip(CircleShape).background(Color(0xFFFF3B30)))
                    }
                    Text(
                        day.toString(),
                        color = if (isSelected) Color.White else if (isToday) Color(0xFFFF3B30) else Color.Black,
                        fontSize = 20.sp,
                        fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        Divider(modifier = Modifier.padding(vertical = 20.dp), color = Color.LightGray.copy(alpha = 0.5f))

        // Schedule
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Text("全天", color = Color.Black, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.width(4.dp).height(40.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFFFF3B30)))
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("开发任务", color = Color.Black, fontSize = 17.sp, fontWeight = FontWeight.Medium)
                    Text("广州", color = Color.Gray, fontSize = 14.sp)
                }
            }
        }
    }
}
