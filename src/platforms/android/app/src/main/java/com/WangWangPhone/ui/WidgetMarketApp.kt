package com.WangWangPhone.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.WangWangPhone.core.LayoutDbHelper
import com.WangWangPhone.core.LayoutItem
import com.WangWangPhone.core.WebWidgetAssetRecord
import com.WangWangPhone.core.WebWidgetDbHelper
import com.WangWangPhone.core.WebWidgetPackageManager
import com.WangWangPhone.core.WebWidgetRecord
import com.WangWangPhone.core.webWidgetLayoutId
import com.WangWangPhone.core.widgetIdFromLayoutId

private enum class WidgetMarketView {
    LIST,
    EDIT
}

@Composable
fun WidgetMarketAppScreen(
    isDark: Boolean,
    onClose: () -> Unit,
    onLayoutChanged: () -> Unit
) {
    val context = LocalContext.current
    val dbHelper = remember { WebWidgetDbHelper(context) }
    val layoutDbHelper = remember { LayoutDbHelper(context) }
    val packageManager = remember { WebWidgetPackageManager(context, dbHelper) }
    var currentView by remember { mutableStateOf(WidgetMarketView.LIST) }
    var editingWidgetId by remember { mutableStateOf<String?>(null) }
    var widgets by remember { mutableStateOf(dbHelper.getAllWidgets()) }
    var pendingDeleteWidget by remember { mutableStateOf<WebWidgetRecord?>(null) }
    var pendingExportWidgetId by remember { mutableStateOf<String?>(null) }

    fun reloadWidgets() {
        widgets = dbHelper.getAllWidgets()
    }

    fun toast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        val widgetId = pendingExportWidgetId
        pendingExportWidgetId = null
        if (uri == null || widgetId == null) return@rememberLauncherForActivityResult
        val success = packageManager.exportWidget(widgetId, uri)
        toast(if (success) "组件已导出" else "组件导出失败")
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val result = packageManager.importWidget(uri)
        toast(result.message)
        if (result.success) {
            reloadWidgets()
            editingWidgetId = result.widget?.id
        }
    }

    BackHandler {
        if (currentView == WidgetMarketView.EDIT) {
            currentView = WidgetMarketView.LIST
            editingWidgetId = null
        } else {
            onClose()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = if (isDark) Color(0xFF111111) else Color(0xFFF6F6F6)
    ) {
        when (currentView) {
            WidgetMarketView.LIST -> {
                WidgetMarketListScreen(
                    widgets = widgets,
                    onBack = onClose,
                    onCreate = {
                        editingWidgetId = null
                        currentView = WidgetMarketView.EDIT
                    },
                    onEdit = { widget ->
                        editingWidgetId = widget.id
                        currentView = WidgetMarketView.EDIT
                    },
                    onImport = { importLauncher.launch(arrayOf("*/*")) },
                    onExport = { widget ->
                        pendingExportWidgetId = widget.id
                        exportLauncher.launch("${sanitizeExportName(widget.name)}.wwwidget")
                    },
                    onAddToDesktop = { widget ->
                        val success = addWebWidgetToDesktop(
                            widget = widget,
                            isDark = isDark,
                            layoutDbHelper = layoutDbHelper,
                            webWidgetDbHelper = dbHelper
                        )
                        toast(if (success) "已添加到桌面" else "桌面没有足够空间")
                        if (success) onLayoutChanged()
                    },
                    onDelete = { widget ->
                        pendingDeleteWidget = widget
                    }
                )
            }

            WidgetMarketView.EDIT -> {
                WidgetEditorScreen(
                    initialWidgetId = editingWidgetId,
                    onBack = {
                        currentView = WidgetMarketView.LIST
                        editingWidgetId = null
                        reloadWidgets()
                    },
                    onSaved = { widgetId ->
                        editingWidgetId = widgetId
                        reloadWidgets()
                        onLayoutChanged()
                    },
                    onLayoutChanged = onLayoutChanged
                )
            }
        }
    }

    pendingDeleteWidget?.let { widget ->
        AlertDialog(
            onDismissRequest = { pendingDeleteWidget = null },
            title = { Text("删除组件") },
            text = { Text("删除后会同步从桌面移除该组件，确定继续吗？") },
            confirmButton = {
                TextButton(onClick = {
                    pendingDeleteWidget = null
                    removeWebWidgetFromLayout(layoutDbHelper, widget.id)
                    val deleted = dbHelper.deleteWidget(widget.id)
                    toast(if (deleted) "组件已删除" else "组件删除失败")
                    reloadWidgets()
                    onLayoutChanged()
                }) {
                    Text("删除", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteWidget = null }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun WidgetMarketListScreen(
    widgets: List<WebWidgetRecord>,
    onBack: () -> Unit,
    onCreate: () -> Unit,
    onEdit: (WebWidgetRecord) -> Unit,
    onImport: () -> Unit,
    onExport: (WebWidgetRecord) -> Unit,
    onAddToDesktop: (WebWidgetRecord) -> Unit,
    onDelete: (WebWidgetRecord) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "小组件市场",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onImport) { Text("导入") }
                TextButton(onClick = onCreate) { Text("新建") }
                TextButton(onClick = onBack) { Text("关闭") }
            }
        }

        if (widgets.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("还没有组件，先新建一个吧")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(widgets, key = { it.id }) { widget ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.96f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(widget.name, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("尺寸 ${widget.spanX} x ${widget.spanY}", color = Color(0xFF666666), fontSize = 13.sp)
                                }
                                Box(
                                    modifier = Modifier
                                        .width(72.dp)
                                        .height(72.dp)
                                        .background(Color(0xFFF1F3F5), RoundedCornerShape(16.dp))
                                        .padding(6.dp)
                                ) {
                                    WebWidgetView(widget = widget, modifier = Modifier.fillMaxSize(), cornerRadiusDp = 12.dp)
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            ActionChipRow(
                                actions = listOf(
                                    "编辑" to { onEdit(widget) },
                                    "加到桌面" to { onAddToDesktop(widget) },
                                    "导出" to { onExport(widget) },
                                    "删除" to { onDelete(widget) }
                                )
                            )
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WidgetEditorScreen(
    initialWidgetId: String?,
    onBack: () -> Unit,
    onSaved: (String) -> Unit,
    onLayoutChanged: () -> Unit
) {
    val context = LocalContext.current
    val dbHelper = remember { WebWidgetDbHelper(context) }
    val clipboardManager = remember {
        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    }
    val initialRecord = remember(initialWidgetId) { initialWidgetId?.let { dbHelper.getWidget(it) } }

    var widgetId by remember(initialWidgetId) { mutableStateOf(initialRecord?.id) }
    var widgetName by remember(initialWidgetId) { mutableStateOf(initialRecord?.name ?: "") }
    var spanXText by remember(initialWidgetId) { mutableStateOf((initialRecord?.spanX ?: 2).toString()) }
    var spanYText by remember(initialWidgetId) { mutableStateOf((initialRecord?.spanY ?: 2).toString()) }
    var htmlCode by remember(initialWidgetId) { mutableStateOf(initialRecord?.htmlCode ?: defaultHtmlTemplate()) }
    var cssCode by remember(initialWidgetId) { mutableStateOf(initialRecord?.cssCode ?: defaultCssTemplate()) }
    var jsCode by remember(initialWidgetId) { mutableStateOf(initialRecord?.jsCode ?: defaultJsTemplate()) }
    var assets by remember(initialWidgetId) { mutableStateOf(if (initialWidgetId != null) dbHelper.getWidgetAssets(initialWidgetId) else emptyList()) }

    fun toast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    val importImageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        val currentWidgetId = widgetId
        if (uri == null || currentWidgetId == null) return@rememberLauncherForActivityResult
        val record = dbHelper.importAssetFromUri(currentWidgetId, uri)
        if (record != null) {
            assets = dbHelper.getWidgetAssets(currentWidgetId)
            toast("图片已导入")
            onLayoutChanged()
        } else {
            toast("图片导入失败")
        }
    }

    fun saveWidget(): String? {
        val parsedSpanX = spanXText.toIntOrNull()?.coerceIn(1, GRID_COLUMNS)
        val parsedSpanY = spanYText.toIntOrNull()?.coerceIn(1, GRID_ROWS)
        if (parsedSpanX == null || parsedSpanY == null) {
            toast("尺寸请输入 1 到 ${GRID_COLUMNS} / ${GRID_ROWS} 之间的整数")
            return null
        }

        val record = WebWidgetRecord(
            id = widgetId ?: java.util.UUID.randomUUID().toString(),
            name = widgetName.trim().ifBlank { "未命名组件" },
            htmlCode = htmlCode,
            cssCode = cssCode,
            jsCode = jsCode,
            spanX = parsedSpanX,
            spanY = parsedSpanY,
            createdAt = initialRecord?.createdAt ?: (System.currentTimeMillis() / 1000),
            updatedAt = System.currentTimeMillis() / 1000
        )
        val success = dbHelper.saveWidget(record)
        return if (success) {
            widgetId = record.id
            assets = dbHelper.getWidgetAssets(record.id)
            onSaved(record.id)
            toast("组件已保存")
            record.id
        } else {
            toast("组件保存失败")
            null
        }
    }

    val previewRecord = remember(widgetId, widgetName, spanXText, spanYText, htmlCode, cssCode, jsCode) {
        WebWidgetRecord(
            id = widgetId ?: "preview",
            name = widgetName.ifBlank { "未命名组件" },
            htmlCode = htmlCode,
            cssCode = cssCode,
            jsCode = jsCode,
            spanX = spanXText.toIntOrNull()?.coerceIn(1, GRID_COLUMNS) ?: 2,
            spanY = spanYText.toIntOrNull()?.coerceIn(1, GRID_ROWS) ?: 2,
            createdAt = 0,
            updatedAt = System.currentTimeMillis() / 1000
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(if (widgetId == null) "新建组件" else "编辑组件", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onBack) { Text("返回") }
                Button(onClick = { saveWidget() }) { Text("保存") }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = widgetName,
                onValueChange = { widgetName = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("组件名称") }
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = spanXText,
                    onValueChange = { spanXText = it.filter { ch -> ch.isDigit() }.take(2) },
                    modifier = Modifier.weight(1f),
                    label = { Text("宽度格数") },
                    supportingText = { Text("1 - $GRID_COLUMNS") }
                )
                OutlinedTextField(
                    value = spanYText,
                    onValueChange = { spanYText = it.filter { ch -> ch.isDigit() }.take(2) },
                    modifier = Modifier.weight(1f),
                    label = { Text("高度格数") },
                    supportingText = { Text("1 - $GRID_ROWS") }
                )
            }

            CodeEditorField(
                title = "HTML",
                value = htmlCode,
                onValueChange = { htmlCode = it },
                minLines = 8
            )
            CodeEditorField(
                title = "CSS",
                value = cssCode,
                onValueChange = { cssCode = it },
                minLines = 8
            )
            CodeEditorField(
                title = "JS",
                value = jsCode,
                onValueChange = { jsCode = it },
                minLines = 8
            )

            Card(colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.96f))) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("资源管理", fontWeight = FontWeight.SemiBold)
                    if (widgetId == null) {
                        Text("先保存组件，再导入图片资源", color = Color(0xFF666666), fontSize = 13.sp)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { if (widgetId != null) importImageLauncher.launch("image/*") else toast("请先保存组件") }) {
                            Text("导入图片")
                        }
                        TextButton(onClick = {
                            val currentWidgetId = widgetId
                            if (currentWidgetId == null) {
                                toast("请先保存组件")
                            } else {
                                saveWidget()
                            }
                        }) {
                            Text("刷新资源")
                        }
                    }
                    if (assets.isEmpty()) {
                        Text("暂无图片资源", color = Color(0xFF666666), fontSize = 13.sp)
                    } else {
                        assets.forEach { asset ->
                            AssetRow(
                                widgetId = widgetId ?: return@forEach,
                                asset = asset,
                                dbHelper = dbHelper,
                                onCopy = { text ->
                                    clipboardManager.setPrimaryClip(ClipData.newPlainText("widget-asset", text))
                                    toast("资源地址已复制")
                                },
                                onDelete = {
                                    val currentWidgetId = widgetId ?: return@AssetRow
                                    dbHelper.deleteAsset(currentWidgetId, asset.id)
                                    assets = dbHelper.getWidgetAssets(currentWidgetId)
                                    toast("资源已删除")
                                    onLayoutChanged()
                                }
                            )
                        }
                    }
                }
            }

            Card(colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.96f))) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("实时预览", fontWeight = FontWeight.SemiBold)
                    Text(
                        text = "当前预览尺寸：${previewRecord.spanX} x ${previewRecord.spanY}",
                        color = Color(0xFF666666),
                        fontSize = 13.sp
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .background(Color(0xFFF3F4F6), RoundedCornerShape(20.dp))
                            .padding(10.dp)
                    ) {
                        WebWidgetView(widget = previewRecord, modifier = Modifier.fillMaxSize(), cornerRadiusDp = 16.dp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun CodeEditorField(
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    minLines: Int
) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.96f))) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                minLines = minLines,
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp)
            )
        }
    }
}

@Composable
private fun AssetRow(
    widgetId: String,
    asset: WebWidgetAssetRecord,
    dbHelper: WebWidgetDbHelper,
    onCopy: (String) -> Unit,
    onDelete: () -> Unit
) {
    val assetUrl = remember(widgetId, asset.id, asset.storedName) {
        dbHelper.getAssetUrl(widgetId, asset.storedName)
    }
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA))) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(asset.originalName, fontWeight = FontWeight.Medium, fontSize = 13.sp)
            Text(assetUrl, color = Color(0xFF666666), fontSize = 12.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { onCopy(assetUrl) }) { Text("复制地址") }
                TextButton(onClick = onDelete) { Text("删除", color = Color.Red) }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ActionChipRow(actions: List<Pair<String, () -> Unit>>) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        actions.forEach { (label, action) ->
            Box(
                modifier = Modifier
                    .border(1.dp, Color(0xFFD0D7DE), RoundedCornerShape(999.dp))
                    .clickable { action() }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(label, fontSize = 13.sp)
            }
        }
    }
}

private fun addWebWidgetToDesktop(
    widget: WebWidgetRecord,
    isDark: Boolean,
    layoutDbHelper: LayoutDbHelper,
    webWidgetDbHelper: WebWidgetDbHelper
): Boolean {
    val pages = mutableListOf<MutableMap<Int, GridItem>>()
    val dockApps = mutableListOf<AppIcon>()
    val defaultApps = getDefaultApps(isDark)
    val defaultWidgets = getDefaultWidgets()
    val webWidgetMap = webWidgetDbHelper.getAllWidgets().associateBy { it.id }
    val savedLayout = layoutDbHelper.getLayout()

    if (savedLayout.isNotEmpty()) {
        val pageMap = mutableMapOf<Int, MutableMap<Int, GridItem>>()
        savedLayout.filter { it.area.startsWith("grid") }.forEach { item ->
            val pageIndex = if (item.area == "grid") 0 else item.area.removePrefix("grid_").toIntOrNull() ?: 0
            val page = pageMap.getOrPut(pageIndex) { mutableMapOf() }
            val app = defaultApps.find { it.id == item.appId }
            if (app != null) {
                page[item.position] = app
            } else {
                val defaultWidget = defaultWidgets.find { it.id == item.appId }
                if (defaultWidget != null) {
                    page[item.position] = defaultWidget
                } else {
                    val dynamicWidget = widgetIdFromLayoutId(item.appId)?.let { webWidgetMap[it] }
                    if (dynamicWidget != null) {
                        page[item.position] = WebWidgetGridItem(dynamicWidget)
                    }
                }
            }
        }
        val maxPage = pageMap.keys.maxOrNull() ?: 0
        for (pageIndex in 0..maxPage) {
            pages.add(pageMap.getOrDefault(pageIndex, mutableMapOf()))
        }
        savedLayout.filter { it.area == "dock" }.sortedBy { it.position }.forEach { item ->
            defaultApps.find { it.id == item.appId }?.let { dockApps.add(it) }
        }
    } else {
        distributeItemsToPages(defaultApps, defaultWidgets).forEach { pages.add(it.toMutableMap()) }
    }

    if (pages.isEmpty()) pages.add(mutableMapOf())
    val dynamicItem = WebWidgetGridItem(widget)
    var placed = false

    for (page in pages) {
        for (cell in 0 until TOTAL_CELLS) {
            if (checkOccupancy(page, cell, dynamicItem.spanX, dynamicItem.spanY, null)) {
                page[cell] = dynamicItem
                placed = true
                break
            }
        }
        if (placed) break
    }

    if (!placed) {
        val newPage = mutableMapOf<Int, GridItem>()
        for (cell in 0 until TOTAL_CELLS) {
            if (checkOccupancy(newPage, cell, dynamicItem.spanX, dynamicItem.spanY, null)) {
                newPage[cell] = dynamicItem
                pages.add(newPage)
                placed = true
                break
            }
        }
    }

    if (!placed) return false

    val layoutItems = mutableListOf<LayoutItem>()
    pages.forEachIndexed { pageIndex, page ->
        val areaName = if (pageIndex == 0) "grid" else "grid_$pageIndex"
        page.forEach { (cellIndex, item) ->
            layoutItems.add(LayoutItem(appId = item.id, position = cellIndex, area = areaName))
        }
    }
    dockApps.forEachIndexed { index, app ->
        layoutItems.add(LayoutItem(appId = app.id, position = index, area = "dock"))
    }
    return layoutDbHelper.saveLayout(layoutItems)
}

private fun removeWebWidgetFromLayout(layoutDbHelper: LayoutDbHelper, widgetId: String): Boolean {
    val targetLayoutId = webWidgetLayoutId(widgetId)
    val filtered = layoutDbHelper.getLayout().filterNot { it.appId == targetLayoutId }
    return layoutDbHelper.saveLayout(filtered)
}

private fun sanitizeExportName(name: String): String {
    val trimmed = name.trim().ifBlank { "widget" }
    return trimmed.replace(Regex("[\\\\/:*?\"<>|]"), "_")
}

private fun defaultHtmlTemplate(): String {
    return """
        <div class="widget-card">
          <div class="widget-title">我的组件</div>
          <div class="widget-subtitle">把这里改成你自己的 HTML</div>
        </div>
    """.trimIndent()
}

private fun defaultCssTemplate(): String {
    return """
        .widget-card {
          width: 100%;
          height: 100%;
          border-radius: 20px;
          padding: 16px;
          background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
          display: flex;
          flex-direction: column;
          justify-content: center;
        }

        .widget-title {
          font-size: 22px;
          font-weight: 700;
          margin-bottom: 8px;
        }

        .widget-subtitle {
          font-size: 13px;
          opacity: 0.9;
        }
    """.trimIndent()
}

private fun defaultJsTemplate(): String {
    return """
        const title = document.querySelector('.widget-title');
        if (title) {
          title.textContent = '我的组件';
        }
    """.trimIndent()
}
