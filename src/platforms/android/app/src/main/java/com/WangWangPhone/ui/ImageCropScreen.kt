package com.WangWangPhone.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import kotlin.math.max
import kotlin.math.min

/**
 * 图片裁切界面
 * @param imageUri 要裁切的图片 Uri
 * @param onCropComplete 裁切完成回调，返回裁切后的 Bitmap
 * @param onCancel 取消回调
 */
@Composable
fun ImageCropScreen(
    imageUri: Uri,
    onCropComplete: (Bitmap) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    
    // 加载原始图片
    val originalBitmap = remember(imageUri) {
        try {
            context.contentResolver.openInputStream(imageUri)?.use {
                BitmapFactory.decodeStream(it)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    if (originalBitmap == null) {
        // 加载失败，直接返回
        LaunchedEffect(Unit) {
            onCancel()
        }
        return
    }
    
    // 图片显示区域的尺寸
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    
    // 计算图片在容器中的实际显示尺寸和位置（保持宽高比）
    val imageDisplayInfo = remember(originalBitmap, containerSize) {
        if (containerSize.width == 0 || containerSize.height == 0) {
            ImageDisplayInfo(0f, 0f, 0f, 0f)
        } else {
            val containerRatio = containerSize.width.toFloat() / containerSize.height
            val imageRatio = originalBitmap.width.toFloat() / originalBitmap.height
            
            val (displayWidth, displayHeight) = if (imageRatio > containerRatio) {
                // 图片更宽，以宽度为准
                containerSize.width.toFloat() to containerSize.width / imageRatio
            } else {
                // 图片更高，以高度为准
                containerSize.height * imageRatio to containerSize.height.toFloat()
            }
            
            val offsetX = (containerSize.width - displayWidth) / 2
            val offsetY = (containerSize.height - displayHeight) / 2
            
            ImageDisplayInfo(displayWidth, displayHeight, offsetX, offsetY)
        }
    }
    
    // 裁切框的位置和大小（相对于容器）
    var cropRect by remember {
        mutableStateOf(
            Rect(
                left = 0f,
                top = 0f,
                right = 0f,
                bottom = 0f
            )
        )
    }
    
    // 初始化裁切框（居中的正方形）
    LaunchedEffect(imageDisplayInfo) {
        if (imageDisplayInfo.width > 0 && imageDisplayInfo.height > 0) {
            val size = min(imageDisplayInfo.width, imageDisplayInfo.height) * 0.8f
            val left = imageDisplayInfo.offsetX + (imageDisplayInfo.width - size) / 2
            val top = imageDisplayInfo.offsetY + (imageDisplayInfo.height - size) / 2
            cropRect = Rect(left, top, left + size, top + size)
        }
    }
    
    BackHandler { onCancel() }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
    ) {
        // 顶部工具栏
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(Color(0xFF1F1F1F)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "取消",
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 16.dp)
                    .clickable { onCancel() },
                color = Color.White,
                fontSize = 16.sp
            )
            Text(
                "裁切图片",
                fontWeight = FontWeight.SemiBold,
                fontSize = 17.sp,
                color = Color.White
            )
            Text(
                "完成",
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp)
                    .clickable {
                        // 执行裁切
                        val croppedBitmap = cropBitmap(
                            originalBitmap,
                            cropRect,
                            imageDisplayInfo
                        )
                        if (croppedBitmap != null) {
                            onCropComplete(croppedBitmap)
                        } else {
                            onCancel()
                        }
                    },
                color = Color(0xFF07C160),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
        
        // 图片和裁切框区域
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .onGloballyPositioned { coordinates ->
                    containerSize = coordinates.size
                }
        ) {
            // 显示图片
            if (imageDisplayInfo.width > 0) {
                Image(
                    bitmap = originalBitmap.asImageBitmap(),
                    contentDescription = "待裁切图片",
                    modifier = Modifier
                        .size(
                            with(density) { imageDisplayInfo.width.toDp() },
                            with(density) { imageDisplayInfo.height.toDp() }
                        )
                        .offset(
                            with(density) { imageDisplayInfo.offsetX.toDp() },
                            with(density) { imageDisplayInfo.offsetY.toDp() }
                        ),
                    contentScale = ContentScale.Fit
                )
                
                // 绘制裁切框和遮罩
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                
                                // 移动裁切框
                                val newLeft = cropRect.left + dragAmount.x
                                val newTop = cropRect.top + dragAmount.y
                                val newRight = cropRect.right + dragAmount.x
                                val newBottom = cropRect.bottom + dragAmount.y
                                
                                // 限制在图片范围内
                                if (newLeft >= imageDisplayInfo.offsetX &&
                                    newRight <= imageDisplayInfo.offsetX + imageDisplayInfo.width &&
                                    newTop >= imageDisplayInfo.offsetY &&
                                    newBottom <= imageDisplayInfo.offsetY + imageDisplayInfo.height
                                ) {
                                    cropRect = Rect(newLeft, newTop, newRight, newBottom)
                                }
                            }
                        }
                        .pointerInput(Unit) {
                            detectTransformGestures { _, _, zoom, _ ->
                                // 缩放裁切框
                                val currentWidth = cropRect.width
                                val currentHeight = cropRect.height
                                val newWidth = (currentWidth * zoom).coerceIn(
                                    100f,
                                    imageDisplayInfo.width
                                )
                                val newHeight = (currentHeight * zoom).coerceIn(
                                    100f,
                                    imageDisplayInfo.height
                                )
                                
                                val centerX = cropRect.center.x
                                val centerY = cropRect.center.y
                                
                                var newLeft = centerX - newWidth / 2
                                var newTop = centerY - newHeight / 2
                                var newRight = centerX + newWidth / 2
                                var newBottom = centerY + newHeight / 2
                                
                                // 限制在图片范围内
                                if (newLeft < imageDisplayInfo.offsetX) {
                                    newLeft = imageDisplayInfo.offsetX
                                    newRight = newLeft + newWidth
                                }
                                if (newRight > imageDisplayInfo.offsetX + imageDisplayInfo.width) {
                                    newRight = imageDisplayInfo.offsetX + imageDisplayInfo.width
                                    newLeft = newRight - newWidth
                                }
                                if (newTop < imageDisplayInfo.offsetY) {
                                    newTop = imageDisplayInfo.offsetY
                                    newBottom = newTop + newHeight
                                }
                                if (newBottom > imageDisplayInfo.offsetY + imageDisplayInfo.height) {
                                    newBottom = imageDisplayInfo.offsetY + imageDisplayInfo.height
                                    newTop = newBottom - newHeight
                                }
                                
                                cropRect = Rect(newLeft, newTop, newRight, newBottom)
                            }
                        }
                ) {
                    // 绘制半透明遮罩（裁切框外的区域）
                    drawRect(
                        color = Color.Black.copy(alpha = 0.5f),
                        topLeft = Offset.Zero,
                        size = Size(size.width, cropRect.top)
                    )
                    drawRect(
                        color = Color.Black.copy(alpha = 0.5f),
                        topLeft = Offset(0f, cropRect.bottom),
                        size = Size(size.width, size.height - cropRect.bottom)
                    )
                    drawRect(
                        color = Color.Black.copy(alpha = 0.5f),
                        topLeft = Offset(0f, cropRect.top),
                        size = Size(cropRect.left, cropRect.height)
                    )
                    drawRect(
                        color = Color.Black.copy(alpha = 0.5f),
                        topLeft = Offset(cropRect.right, cropRect.top),
                        size = Size(size.width - cropRect.right, cropRect.height)
                    )
                    
                    // 绘制裁切框边框
                    drawRect(
                        color = Color.White,
                        topLeft = Offset(cropRect.left, cropRect.top),
                        size = Size(cropRect.width, cropRect.height),
                        style = Stroke(width = 2f)
                    )
                    
                    // 绘制九宫格辅助线
                    val gridWidth = cropRect.width / 3
                    val gridHeight = cropRect.height / 3
                    for (i in 1..2) {
                        // 垂直线
                        drawLine(
                            color = Color.White.copy(alpha = 0.5f),
                            start = Offset(cropRect.left + gridWidth * i, cropRect.top),
                            end = Offset(cropRect.left + gridWidth * i, cropRect.bottom),
                            strokeWidth = 1f
                        )
                        // 水平线
                        drawLine(
                            color = Color.White.copy(alpha = 0.5f),
                            start = Offset(cropRect.left, cropRect.top + gridHeight * i),
                            end = Offset(cropRect.right, cropRect.top + gridHeight * i),
                            strokeWidth = 1f
                        )
                    }
                }
            }
        }
        
        // 底部提示
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .background(Color(0xFF1F1F1F)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "拖动调整位置，双指缩放调整大小",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp
            )
        }
    }
}

/**
 * 图片显示信息
 */
private data class ImageDisplayInfo(
    val width: Float,
    val height: Float,
    val offsetX: Float,
    val offsetY: Float
)

/**
 * 裁切图片
 */
private fun cropBitmap(
    originalBitmap: Bitmap,
    cropRect: Rect,
    imageDisplayInfo: ImageDisplayInfo
): Bitmap? {
    return try {
        // 计算裁切区域在原始图片中的位置
        val scaleX = originalBitmap.width / imageDisplayInfo.width
        val scaleY = originalBitmap.height / imageDisplayInfo.height
        
        val x = ((cropRect.left - imageDisplayInfo.offsetX) * scaleX).toInt().coerceIn(0, originalBitmap.width)
        val y = ((cropRect.top - imageDisplayInfo.offsetY) * scaleY).toInt().coerceIn(0, originalBitmap.height)
        val width = (cropRect.width * scaleX).toInt().coerceIn(1, originalBitmap.width - x)
        val height = (cropRect.height * scaleY).toInt().coerceIn(1, originalBitmap.height - y)
        
        Bitmap.createBitmap(originalBitmap, x, y, width, height)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
