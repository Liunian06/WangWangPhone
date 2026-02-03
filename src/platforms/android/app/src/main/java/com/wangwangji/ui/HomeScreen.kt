package com.wangwangji.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class AppIcon(val name: String, val icon: String, val color: Brush)

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
            // 虚拟状态栏
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("09:41", color = Color.White, fontWeight = FontWeight.Bold)
                Row {
                    Text("📶 5G 🔋", color = Color.White)
                }
            }

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
                .background(Color.White.copy(alpha = 0.3f))
                .blur(20.dp)
        ) {
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