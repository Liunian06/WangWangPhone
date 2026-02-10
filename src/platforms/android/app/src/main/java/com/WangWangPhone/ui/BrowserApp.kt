package com.WangWangPhone.ui

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BrowserAppScreen(onClose: () -> Unit) {
    var currentUrl by remember { mutableStateOf("https://www.baidu.com") }
    var addressBarText by remember { mutableStateOf(TextFieldValue("https://www.baidu.com")) }
    var pageTitle by remember { mutableStateOf("Safari") }
    var loadingProgress by remember { mutableFloatStateOf(0f) }
    var isLoading by remember { mutableStateOf(true) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    val focusManager = LocalFocusManager.current

    BackHandler {
        if (webViewRef?.canGoBack() == true) {
            webViewRef?.goBack()
        } else {
            onClose()
        }
    }

    fun navigateTo(input: String) {
        var target = input.trim()
        if (target.isEmpty()) return
        if (!target.startsWith("http://") && !target.startsWith("https://")) {
            target = if (target.contains(".") && !target.contains(" ")) {
                "https://$target"
            } else {
                "https://www.baidu.com/s?wd=${java.net.URLEncoder.encode(target, "UTF-8")}"
            }
        }
        currentUrl = target
        addressBarText = TextFieldValue(target)
        focusManager.clearFocus()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
    ) {
        // 顶部地址栏
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF9F9F9))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 地址输入框
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFE8E8ED))
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    BasicTextField(
                        value = addressBarText,
                        onValueChange = { addressBarText = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 14.sp, color = Color.Black),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(
                            onGo = { navigateTo(addressBarText.text) }
                        ),
                        decorationBox = { innerTextField ->
                            if (addressBarText.text.isEmpty()) {
                                Text(
                                    "搜索或输入网站名称",
                                    color = Color(0xFF8E8E93),
                                    fontSize = 14.sp
                                )
                            }
                            innerTextField()
                        }
                    )
                }

                // 取消/关闭按钮
                Text(
                    "完成",
                    color = Color(0xFF007AFF),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable { onClose() }
                )
            }

            // 加载进度条
            if (isLoading && loadingProgress > 0f && loadingProgress < 1f) {
                LinearProgressIndicator(
                    progress = { loadingProgress },
                    modifier = Modifier.fillMaxWidth().height(2.dp),
                    color = Color(0xFF007AFF),
                    trackColor = Color.Transparent,
                )
            }
        }

        // WebView 区域
        Box(modifier = Modifier.weight(1f)) {
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): Boolean {
                                return false
                            }

                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                super.onPageStarted(view, url, favicon)
                                isLoading = true
                                url?.let {
                                    currentUrl = it
                                    addressBarText = TextFieldValue(it)
                                }
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                isLoading = false
                                canGoBack = view?.canGoBack() ?: false
                                canGoForward = view?.canGoForward() ?: false
                                url?.let {
                                    currentUrl = it
                                    addressBarText = TextFieldValue(it)
                                }
                            }
                        }

                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                loadingProgress = newProgress / 100f
                            }

                            override fun onReceivedTitle(view: WebView?, title: String?) {
                                pageTitle = title ?: "Safari"
                            }
                        }

                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.setSupportZoom(true)
                        settings.builtInZoomControls = true
                        settings.displayZoomControls = false
                        settings.useWideViewPort = true
                        settings.loadWithOverviewMode = true

                        loadUrl(currentUrl)
                        webViewRef = this
                    }
                },
                update = { view ->
                    if (view.url != currentUrl && currentUrl.isNotEmpty()) {
                        view.loadUrl(currentUrl)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // 底部工具栏
        Divider(color = Color(0xFFD1D1D6), thickness = 0.5.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .background(Color(0xFFF9F9F9))
                .navigationBarsPadding(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 后退
            Text(
                "◀",
                fontSize = 18.sp,
                color = if (canGoBack) Color(0xFF007AFF) else Color(0xFFC7C7CC),
                modifier = Modifier.clickable(enabled = canGoBack) { webViewRef?.goBack() }
            )
            // 前进
            Text(
                "▶",
                fontSize = 18.sp,
                color = if (canGoForward) Color(0xFF007AFF) else Color(0xFFC7C7CC),
                modifier = Modifier.clickable(enabled = canGoForward) { webViewRef?.goForward() }
            )
            // 分享
            Text("📤", fontSize = 18.sp, color = Color(0xFF007AFF))
            // 书签
            Text("📖", fontSize = 18.sp, color = Color(0xFF007AFF))
            // 标签页
            Text("🔲", fontSize = 18.sp, color = Color(0xFF007AFF))
        }
    }
}
