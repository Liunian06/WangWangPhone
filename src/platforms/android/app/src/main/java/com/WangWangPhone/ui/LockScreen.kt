package com.WangWangPhone.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FractionalThreshold
import androidx.compose.material.rememberSwipeableState
import androidx.compose.material.swipeable
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun LockScreen(onUnlock: () -> Unit, lockWallpaperPath: String? = null) {
    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }
    
    LaunchedEffect(Unit) {
        while(true) {
            currentTime = System.currentTimeMillis()
            kotlinx.coroutines.delay(1000)
        }
    }

    val swipeableState = rememberSwipeableState(initialValue = 0)
    // 使用 BoxWithConstraints 获取高度以动态计算滑动锚点
    // 或者直接增加像素值。1000f 可能在某些高分屏上太短。
    // targetValue 为 1 时立即触发，但 swipeableState.currentValue
    // 可能在动画完成后才更新。
    
    val anchors = mapOf(0f to 0, -2500f to 1) // 增加滑动距离到 2500 像素

    // 使用 snapshotFlow 监听 offset 的实时变化，或者监听 targetValue 以实现更快的响应
    LaunchedEffect(swipeableState.targetValue) {
        if (swipeableState.targetValue == 1) {
            onUnlock()
        }
    }

    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val dateFormat = SimpleDateFormat("M月d日 EEEE", Locale.getDefault())

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .swipeable(
                state = swipeableState,
                anchors = anchors,
                thresholds = { _, _ -> FractionalThreshold(0.3f) },
                orientation = Orientation.Vertical
            )
            .offset(y = (swipeableState.offset.value / 3).dp) // Visual feedback
    ) {
        // 锁屏壁纸背景
        if (lockWallpaperPath != null) {
            val bitmap = remember(lockWallpaperPath) {
                android.graphics.BitmapFactory.decodeFile(lockWallpaperPath)
            }
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "锁屏壁纸",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 100.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = timeFormat.format(Date(currentTime)),
                fontSize = 80.sp,
                fontWeight = FontWeight.ExtraLight,
                color = Color.White
            )
            Text(
                text = dateFormat.format(Date(currentTime)),
                fontSize = 20.sp,
                color = Color.White
            )
        }

        Text(
            text = "向上滑动解锁",
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 50.dp),
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 16.sp
        )
    }
}