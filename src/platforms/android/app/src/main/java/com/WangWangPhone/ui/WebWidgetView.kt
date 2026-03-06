package com.WangWangPhone.ui

import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.WangWangPhone.core.WebWidgetDbHelper
import com.WangWangPhone.core.WebWidgetRecord
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.FileInputStream

@Composable
fun WebWidgetView(
    widget: WebWidgetRecord,
    modifier: Modifier = Modifier,
    cornerRadiusDp: Dp = 18.dp
) {
    val context = LocalContext.current
    val dbHelper = remember { WebWidgetDbHelper(context) }
    val document = remember(widget.id, widget.updatedAt, widget.htmlCode, widget.cssCode, widget.jsCode) {
        buildWidgetDocument(widget)
    }
    var webViewRef by remember(widget.id) { mutableStateOf<WebView?>(null) }
    var loadError by remember(widget.id, widget.updatedAt) { mutableStateOf<String?>(null) }
    val shape = remember(cornerRadiusDp) { RoundedCornerShape(cornerRadiusDp) }

    DisposableEffect(widget.id) {
        onDispose {
            webViewRef?.stopLoading()
            webViewRef?.destroy()
            webViewRef = null
        }
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(Color(0x33000000))
    ) {
        AndroidView(
            factory = { viewContext ->
                WebView(viewContext).apply {
                    setBackgroundColor(android.graphics.Color.TRANSPARENT)
                    overScrollMode = WebView.OVER_SCROLL_NEVER
                    isVerticalScrollBarEnabled = false
                    isHorizontalScrollBarEnabled = false
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        allowFileAccess = false
                        allowContentAccess = false
                        databaseEnabled = false
                        setSupportZoom(false)
                        builtInZoomControls = false
                        displayZoomControls = false
                        cacheMode = WebSettings.LOAD_NO_CACHE
                        mediaPlaybackRequiresUserGesture = true
                    }
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): Boolean {
                            val url = request?.url ?: return true
                            return !(
                                (url.scheme == "https" && url.host == "widget.local") ||
                                    (url.scheme == "widget" && url.host == "asset")
                                )
                        }

                        override fun shouldInterceptRequest(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): WebResourceResponse? {
                            val url = request?.url ?: return null
                            if (url.scheme == "widget" && url.host == "asset") {
                                val widgetId = url.pathSegments.getOrNull(0) ?: return blockedResponse()
                                val storedName = url.pathSegments.getOrNull(1) ?: return blockedResponse()
                                if (widgetId != widget.id) return blockedResponse()
                                val asset = dbHelper.resolveAssetByStoredName(widgetId, storedName) ?: return blockedResponse()
                                val file = dbHelper.getAssetFile(widgetId, asset.storedName)
                                if (!file.exists()) return blockedResponse()
                                return try {
                                    WebResourceResponse(
                                        asset.mimeType,
                                        null,
                                        200,
                                        "OK",
                                        mapOf("Cache-Control" to "no-store"),
                                        FileInputStream(file)
                                    )
                                } catch (_: Exception) {
                                    blockedResponse()
                                }
                            }
                            if (url.scheme == "https" && url.host == "widget.local") {
                                return super.shouldInterceptRequest(view, request)
                            }
                            return blockedResponse()
                        }

                        override fun onReceivedError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            error: WebResourceError?
                        ) {
                            super.onReceivedError(view, request, error)
                            if (request?.isForMainFrame == true) {
                                loadError = error?.description?.toString() ?: "渲染失败"
                            }
                        }
                    }
                    loadDataWithBaseURL(
                        "https://widget.local/",
                        document,
                        "text/html",
                        "UTF-8",
                        null
                    )
                    tag = document
                    webViewRef = this
                }
            },
            update = { view ->
                if (view.tag != document) {
                    loadError = null
                    view.loadDataWithBaseURL(
                        "https://widget.local/",
                        document,
                        "text/html",
                        "UTF-8",
                        null
                    )
                    view.tag = document
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        if (loadError != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xCC1C1C1E))
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "组件渲染失败", color = Color.White)
            }
        }
    }
}

private fun blockedResponse(): WebResourceResponse {
    return WebResourceResponse(
        "text/plain",
        "UTF-8",
        403,
        "Blocked",
        mapOf("Cache-Control" to "no-store"),
        ByteArrayInputStream(ByteArray(0))
    )
}

private fun buildWidgetDocument(widget: WebWidgetRecord): String {
    return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8" />
            <meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no" />
            <style>
                html, body {
                    width: 100%;
                    height: 100%;
                    margin: 0;
                    padding: 0;
                    overflow: hidden;
                    display: flex;
                    background: transparent;
                    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
                    color: white;
                }
                *, *::before, *::after {
                    box-sizing: border-box;
                }
                #widget-root {
                    width: 100%;
                    height: 100%;
                    display: flex;
                    flex: 1;
                    min-width: 0;
                    min-height: 0;
                }
                ${widget.cssCode}
            </style>
        </head>
        <body>
            <div id="widget-root">${widget.htmlCode}</div>
            <script>
                window.WangWangWidget = {
                    id: ${JSONObject.quote(widget.id)},
                    spanX: ${widget.spanX},
                    spanY: ${widget.spanY}
                };
            </script>
            <script>
                try {
                    ${widget.jsCode}
                } catch (error) {
                    document.body.innerHTML = '<div style="padding:12px;font-size:12px;color:#fff;background:rgba(0,0,0,.45);border-radius:12px;">组件脚本执行失败：' + error + '</div>';
                }
            </script>
        </body>
        </html>
    """.trimIndent()
}

