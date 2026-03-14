package com.WangWangPhone.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import kotlin.math.max
import kotlin.math.min

/**
 * 图片裁切界面 - 固定1:1裁切框，支持拖动和缩放图片
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
        LaunchedEffect(Unit) {
            onCancel()
        }
        return
    }
    
    // 容器尺寸
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    
    // 图片变换状态：缩放、偏移
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    
    // 计算固定的1:1裁切框（居中，占容器80%）
    val cropBoxSize = remember(containerSize) {
        if (containerSize.width > 0 && containerSize.height > 0) {
            min(containerSize.width, containerSize.height) * 0.8f
        } else {
            0f
        }
    }
    
    val cropBoxLeft = remember(containerSize, cropBoxSize) {
        (containerSize.width - cropBoxSize) / 2
    }
    
    val cropBoxTop = remember(containerSize, cropBoxSize) {
        (containerSize.height - cropBoxSize) / 2
    }
    
    // 初始化图片缩放，使其适配裁切框
    LaunchedEffect(originalBitmap, cropBoxSize) {
        if (cropBoxSize > 0) {
            val imageRatio = originalBitmap.width.toFloat() / originalBitmap.height
            // 计算初始缩放，确保图片能覆盖裁切框
            val minScale = if (imageRatio > 1f) {
                // 横图
                cropBoxSize / originalBitmap.height
            } else {
                // 竖图
                cropBoxSize / originalBitmap.width
            }
            scale = minScale * 1.2f // 稍微放大一点
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
                        val croppedBitmap = cropBitmap(
                            originalBitmap,
                            scale,
                            offsetX,
                            offsetY,
                            cropBoxLeft,
                            cropBoxTop,
                            cropBoxSize,
                            containerSize
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
                .clipToBounds()
                .onGloballyPositioned { coordinates ->
                    containerSize = coordinates.size
                }
        ) {
            if (containerSize.width > 0 && containerSize.height > 0) {
                // 显示图片（可拖动和缩放）
                Image(
                    painter = BitmapPainter(originalBitmap.asImageBitmap()),
                    contentDescription = "待裁切图片",
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offsetX,
                            translationY = offsetY
                        )
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                // 计算图片在容器中的实际显示尺寸
                                val imageRatio = originalBitmap.width.toFloat() / originalBitmap.height
                                val containerRatio = containerSize.width.toFloat() / containerSize.height
                                
                                val (displayWidth, displayHeight) = if (imageRatio > containerRatio) {
                                    containerSize.width.toFloat() to containerSize.width / imageRatio
                                } else {
                                    containerSize.height * imageRatio to containerSize.height.toFloat()
                                }
                                
                                val displayOffsetX = (containerSize.width - displayWidth) / 2
                                val displayOffsetY = (containerSize.height - displayHeight) / 2
                                
                                // 更新缩放，确保图片能完全覆盖裁切框
                                val minScale = cropBoxSize / min(displayWidth, displayHeight)
                                val newScale = (scale * zoom).coerceIn(minScale, 5f)
                                scale = newScale
                                
                                // 计算缩放后的图片尺寸和位置
                                val scaledWidth = displayWidth * scale
                                val scaledHeight = displayHeight * scale
                                
                                // 更新偏移
                                var newOffsetX = offsetX + pan.x
                                var newOffsetY = offsetY + pan.y
                                
                                // 计算图片边界
                                val imageLeft = displayOffsetX + newOffsetX
                                val imageRight = imageLeft + scaledWidth
                                val imageTop = displayOffsetY + newOffsetY
                                val imageBottom = imageTop + scaledHeight
                                
                                // 限制偏移，确保裁切框不超出图片范围
                                // 左边界：图片左边不能超过裁切框左边
                                if (imageLeft > cropBoxLeft) {
                                    newOffsetX = cropBoxLeft - displayOffsetX
                                }
                                // 右边界：图片右边不能超过裁切框右边
                                if (imageRight < cropBoxLeft + cropBoxSize) {
                                    newOffsetX = (cropBoxLeft + cropBoxSize) - displayOffsetX - scaledWidth
                                }
                                // 上边界：图片上边不能超过裁切框上边
                                if (imageTop > cropBoxTop) {
                                    newOffsetY = cropBoxTop - displayOffsetY
                                }
                                // 下边界：图片下边不能超过裁切框下边
                                if (imageBottom < cropBoxTop + cropBoxSize) {
                                    newOffsetY = (cropBoxTop + cropBoxSize) - displayOffsetY - scaledHeight
                                }
                                
                                offsetX = newOffsetX
                                offsetY = newOffsetY
                            }
                        },
                    contentScale = ContentScale.Fit
                )
                
                // 绘制裁切框和遮罩
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // 绘制半透明遮罩（裁切框外的区域）
                    // 上方
                    drawRect(
                        color = Color.Black.copy(alpha = 0.5f),
                        topLeft = Offset.Zero,
                        size = Size(size.width, cropBoxTop)
                    )
                    // 下方
                    drawRect(
                        color = Color.Black.copy(alpha = 0.5f),
                        topLeft = Offset(0f, cropBoxTop + cropBoxSize),
                        size = Size(size.width, size.height - cropBoxTop - cropBoxSize)
                    )
                    // 左侧
                    drawRect(
                        color = Color.Black.copy(alpha = 0.5f),
                        topLeft = Offset(0f, cropBoxTop),
                        size = Size(cropBoxLeft, cropBoxSize)
                    )
                    // 右侧
                    drawRect(
                        color = Color.Black.copy(alpha = 0.5f),
                        topLeft = Offset(cropBoxLeft + cropBoxSize, cropBoxTop),
                        size = Size(size.width - cropBoxLeft - cropBoxSize, cropBoxSize)
                    )
                    
                    // 绘制裁切框边框
                    drawRect(
                        color = Color.White,
                        topLeft = Offset(cropBoxLeft, cropBoxTop),
                        size = Size(cropBoxSize, cropBoxSize),
                        style = Stroke(width = 2f)
                    )
                    
                    // 绘制九宫格辅助线
                    val gridSize = cropBoxSize / 3
                    for (i in 1..2) {
                        // 垂直线
                        drawLine(
                            color = Color.White.copy(alpha = 0.5f),
                            start = Offset(cropBoxLeft + gridSize * i, cropBoxTop),
                            end = Offset(cropBoxLeft + gridSize * i, cropBoxTop + cropBoxSize),
                            strokeWidth = 1f
                        )
                        // 水平线
                        drawLine(
                            color = Color.White.copy(alpha = 0.5f),
                            start = Offset(cropBoxLeft, cropBoxTop + gridSize * i),
                            end = Offset(cropBoxLeft + cropBoxSize, cropBoxTop + gridSize * i),
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
 * 裁切图片
 */
private fun cropBitmap(
    originalBitmap: Bitmap,
    scale: Float,
    offsetX: Float,
    offsetY: Float,
    cropBoxLeft: Float,
    cropBoxTop: Float,
    cropBoxSize: Float,
    containerSize: IntSize
): Bitmap? {
    return try {
        // 计算图片在容器中的实际显示尺寸
        val imageRatio = originalBitmap.width.toFloat() / originalBitmap.height
        val containerRatio = containerSize.width.toFloat() / containerSize.height
        
        val (displayWidth, displayHeight) = if (imageRatio > containerRatio) {
            containerSize.width.toFloat() to containerSize.width / imageRatio
        } else {
            containerSize.height * imageRatio to containerSize.height.toFloat()
        }
        
        val displayOffsetX = (containerSize.width - displayWidth) / 2
        val displayOffsetY = (containerSize.height - displayHeight) / 2
        
        // 计算裁切框在缩放和平移后的图片上的位置
        val scaledWidth = displayWidth * scale
        val scaledHeight = displayHeight * scale
        val scaledOffsetX = displayOffsetX + offsetX
        val scaledOffsetY = displayOffsetY + offsetY
        
        // 裁切框相对于缩放后图片的位置
        val cropX = (cropBoxLeft - scaledOffsetX) / scale
        val cropY = (cropBoxTop - scaledOffsetY) / scale
        val cropSize = cropBoxSize / scale
        
        // 转换到原始图片坐标
        val scaleToOriginal = originalBitmap.width / displayWidth
        val originalCropX = (cropX * scaleToOriginal).toInt().coerceIn(0, originalBitmap.width)
        val originalCropY = (cropY * scaleToOriginal).toInt().coerceIn(0, originalBitmap.height)
        val originalCropSize = (cropSize * scaleToOriginal).toInt()
            .coerceIn(1, min(originalBitmap.width - originalCropX, originalBitmap.height - originalCropY))
        
        // 裁切并缩放到正方形
        val croppedBitmap = Bitmap.createBitmap(
            originalBitmap,
            originalCropX,
            originalCropY,
            originalCropSize,
            originalCropSize
        )
        
        // 如果裁切后的尺寸过大，缩放到合理大小（1024x1024）
        val maxSize = 1024
        if (croppedBitmap.width > maxSize || croppedBitmap.height > maxSize) {
            Bitmap.createScaledBitmap(croppedBitmap, maxSize, maxSize, true).also {
                croppedBitmap.recycle()
            }
        } else {
            croppedBitmap
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
