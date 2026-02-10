package com.WangWangPhone.ui

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.os.Build
import android.provider.MediaStore
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun CameraAppScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Camera permission state
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    // Request permission on first composition if not granted
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Camera mode state
    val cameraModes = listOf("延时摄影", "慢动作", "视频", "照片", "人像", "全景")
    var selectedModeIndex by remember { mutableIntStateOf(3) } // Default to "照片"

    // Front/Back camera state
    var useFrontCamera by remember { mutableStateOf(false) }
    
    // ImageCapture use case
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    
    // Shutter animation
    var isShutterPressed by remember { mutableStateOf(false) }
    val shutterScale by animateFloatAsState(
        targetValue = if (isShutterPressed) 0.85f else 1f,
        animationSpec = tween(100),
        label = "shutter"
    )
    
    // Photo count
    var photosTaken by remember { mutableIntStateOf(0) }
    
    // Flash animation
    var showFlash by remember { mutableStateOf(false) }

    BackHandler { onClose() }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (hasCameraPermission) {
            // Camera Preview
            val cameraSelector = if (useFrontCamera) {
                CameraSelector.DEFAULT_FRONT_CAMERA
            } else {
                CameraSelector.DEFAULT_BACK_CAMERA
            }

            // Key on useFrontCamera to force re-create when switching
            key(useFrontCamera) {
                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        }
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }
                            
                            // ImageCapture
                            val ic = ImageCapture.Builder()
                                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                .build()
                            imageCapture = ic
                            
                            try {
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, ic)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }, ContextCompat.getMainExecutor(ctx))
                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            // Flash overlay
            if (showFlash) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White)
                )
                LaunchedEffect(showFlash) {
                    kotlinx.coroutines.delay(100)
                    showFlash = false
                }
            }
        } else {
            // No permission placeholder
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📷", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "需要相机权限才能使用",
                        color = Color.White,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "点击授权",
                        color = Color.Yellow,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable {
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    )
                }
            }
        }

        // UI Overlay
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("⚡", color = Color.White, fontSize = 24.sp)
                if (photosTaken > 0) {
                    Text(
                        "已拍 $photosTaken 张",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 14.sp
                    )
                }
                Text(
                    "完成",
                    color = Color.Yellow,
                    modifier = Modifier.clickable { onClose() },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Bottom Bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(bottom = 30.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Camera Mode Selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    cameraModes.forEachIndexed { index, mode ->
                        Text(
                            text = mode,
                            color = if (index == selectedModeIndex) Color.Yellow else Color.White,
                            fontSize = 13.sp,
                            fontWeight = if (index == selectedModeIndex) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.clickable {
                                selectedModeIndex = index
                            }
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 40.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Gallery Thumbnail
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(Color.DarkGray),
                        contentAlignment = Alignment.Center
                    ) {
                        if (photosTaken > 0) {
                            Text("$photosTaken", color = Color.White, fontSize = 14.sp)
                        }
                    }

                    // Shutter Button
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .scale(shutterScale)
                            .border(4.dp, Color.White, CircleShape)
                            .padding(4.dp)
                            .clip(CircleShape)
                            .background(
                                if (selectedModeIndex == 2) Color.Red else Color.White
                            )
                            .clickable {
                                isShutterPressed = true
                                
                                // 拍照
                                val ic = imageCapture
                                if (ic != null && hasCameraPermission) {
                                    val name = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                                    val contentValues = ContentValues().apply {
                                        put(MediaStore.MediaColumns.DISPLAY_NAME, "WangWang_$name")
                                        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/WangWangPhone")
                                        }
                                    }
                                    
                                    val outputOptions = ImageCapture.OutputFileOptions.Builder(
                                        context.contentResolver,
                                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                        contentValues
                                    ).build()
                                    
                                    ic.takePicture(
                                        outputOptions,
                                        ContextCompat.getMainExecutor(context),
                                        object : ImageCapture.OnImageSavedCallback {
                                            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                                photosTaken++
                                                showFlash = true
                                                Toast.makeText(context, "📸 照片已保存", Toast.LENGTH_SHORT).show()
                                            }
                                            
                                            override fun onError(exception: ImageCaptureException) {
                                                Toast.makeText(context, "拍照失败: ${exception.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    )
                                }
                                
                                // Reset animation
                                MainScope().launch {
                                    delay(150)
                                    isShutterPressed = false
                                }
                            }
                    )

                    // Switch Camera
                    Text(
                        "🔄",
                        fontSize = 30.sp,
                        color = Color.White,
                        modifier = Modifier.clickable {
                            useFrontCamera = !useFrontCamera
                        }
                    )
                }
            }
        }
    }
}
