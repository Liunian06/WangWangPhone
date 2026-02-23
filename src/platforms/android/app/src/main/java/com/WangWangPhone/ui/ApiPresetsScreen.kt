package com.WangWangPhone.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.WangWangPhone.core.ApiPreset
import com.WangWangPhone.core.ApiPresetDbHelper
import com.WangWangPhone.core.LlmApiService
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)

@Composable
fun ApiPresetsScreen(
    onBack: () -> Unit,
    onNavigateToChatApi: () -> Unit,
    onNavigateToImageApi: () -> Unit,
    onNavigateToVoiceApi: () -> Unit
) {
    BackHandler { onBack() }
    val isDark = isSystemInDarkTheme()
    val bg = if (isDark) Color(0xFF1C1C1E) else Color(0xFFF2F2F7)
    val card = if (isDark) Color(0xFF2C2C2E) else Color.White
    val txt = if (isDark) Color.White else Color.Black
    val accent = if (isDark) Color(0xFF32C766) else Color(0xFF20A85A)
    
    Column(modifier = Modifier.fillMaxSize().background(bg).statusBarsPadding()) {
        Box(modifier = Modifier.fillMaxWidth().height(56.dp).background(card).padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart) {
            Text("返回", color = accent, modifier = Modifier.clickable { onBack() })
            Text("API预设", modifier = Modifier.align(Alignment.Center), fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = txt)
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        Text("聊天", modifier = Modifier.padding(horizontal = 26.dp, vertical = 8.dp), fontSize = 13.sp, color = Color.Gray)
        Box(modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(RoundedCornerShape(10.dp))
            .background(card).clickable(onClick = onNavigateToChatApi).padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("聊天API预设", fontSize = 16.sp, color = txt)
                Text(">", color = Color.Gray, fontSize = 16.sp)
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        Text("生图", modifier = Modifier.padding(horizontal = 26.dp, vertical = 8.dp), fontSize = 13.sp, color = Color.Gray)
        Box(modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(RoundedCornerShape(10.dp))
            .background(card).clickable(onClick = onNavigateToImageApi).padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("生图API预设", fontSize = 16.sp, color = txt)
                Text(">", color = Color.Gray, fontSize = 16.sp)
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        Text("语音", modifier = Modifier.padding(horizontal = 26.dp, vertical = 8.dp), fontSize = 13.sp, color = Color.Gray)
        Box(modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(RoundedCornerShape(10.dp))
            .background(card).clickable(onClick = onNavigateToVoiceApi).padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("语音API预设", fontSize = 16.sp, color = txt)
                Text(">", color = Color.Gray, fontSize = 16.sp)
            }
        }
    }
}

@Composable
fun ChatApiPresetsScreen(onBack: () -> Unit) {
    ApiPresetListScreen(type = "chat", title = "聊天API预设", onBack = onBack)
}

@Composable
fun ImageApiPresetsScreen(onBack: () -> Unit) {
    ApiPresetListScreen(type = "image", title = "生图API预设", onBack = onBack)
}

@Composable
fun VoiceApiPresetsScreen(onBack: () -> Unit) {
    ApiPresetListScreen(type = "voice", title = "语音API预设", onBack = onBack)
}

@Composable
fun ApiPresetListScreen(type: String, title: String, onBack: () -> Unit) {
    BackHandler { onBack() }
    val isDark = isSystemInDarkTheme()
    val bg = if (isDark) Color(0xFF1C1C1E) else Color(0xFFF2F2F7)
    val card = if (isDark) Color(0xFF2C2C2E) else Color.White
    val txt = if (isDark) Color.White else Color.Black
    val accent = if (isDark) Color(0xFF32C766) else Color(0xFF20A85A)
    val context = LocalContext.current
    val dbHelper = remember { ApiPresetDbHelper(context) }
    var presets by remember { mutableStateOf<List<ApiPreset>>(emptyList()) }
    var showEditScreen by remember { mutableStateOf(false) }
    var editingPreset by remember { mutableStateOf<ApiPreset?>(null) }
    var needsRefresh by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit, needsRefresh) {
        presets = dbHelper.getPresetsByType(type)
        if (needsRefresh) needsRefresh = false
    }
    
    if (showEditScreen) {
        ApiPresetEditScreen(
            type = type,
            preset = editingPreset,
            onBack = {
                showEditScreen = false
                editingPreset = null
                needsRefresh = true
            },
            onSave = { newPreset ->
                dbHelper.savePreset(newPreset)
                showEditScreen = false
                editingPreset = null
                needsRefresh = true
            },
            onDelete = if (editingPreset != null) { { id ->
                dbHelper.deletePreset(id)
                showEditScreen = false
                editingPreset = null
                needsRefresh = true
            } } else null
        )
    } else {
        Column(modifier = Modifier.fillMaxSize().background(bg).statusBarsPadding()) {
            Box(modifier = Modifier.fillMaxWidth().height(56.dp).background(card).padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart) {
                Text("返回", color = accent, modifier = Modifier.clickable { onBack() })
                Text(title, modifier = Modifier.align(Alignment.Center), fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = txt)
                Text("添加", color = accent, modifier = Modifier.align(Alignment.CenterEnd).clickable {
                    editingPreset = null
                    showEditScreen = true
                })
            }
            
            if (presets.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("暂无预设，点击右上角添加", color = Color.Gray, fontSize = 14.sp)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(vertical = 20.dp)) {
                    items(presets) { preset ->
                        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp)).background(card)
                            .clickable {
                                editingPreset = preset
                                showEditScreen = true
                            }.padding(16.dp)) {
                            Column {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(preset.name, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = txt)
                                    Text(preset.provider.uppercase(), fontSize = 12.sp, color = accent)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("模型: ${preset.model}", fontSize = 12.sp, color = Color.Gray)
                                Text("地址: ${preset.baseUrl}", fontSize = 12.sp, color = Color.Gray, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ApiPresetEditScreen(
    type: String,
    preset: ApiPreset?,
    onBack: () -> Unit,
    onSave: (ApiPreset) -> Unit,
    onDelete: ((Long) -> Unit)? = null
) {
    BackHandler { onBack() }
    val isDark = isSystemInDarkTheme()
    val uiColors = apiPresetEditColors(isDark)
    val backgroundBrush = remember(isDark) {
        Brush.verticalGradient(listOf(uiColors.backgroundTop, uiColors.backgroundBottom))
    }
    
    var name by remember { mutableStateOf(preset?.name ?: "") }
    var provider by remember { mutableStateOf(preset?.provider ?: if (type == "voice") "minimax" else "openai") }
    var apiKey by remember { mutableStateOf(preset?.apiKey ?: "") }
    var baseUrl by remember { mutableStateOf(preset?.baseUrl ?: "") }
    var model by remember { mutableStateOf(preset?.model ?: "") }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showModelPicker by remember { mutableStateOf(false) }
    var availableModels by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoadingModels by remember { mutableStateOf(false) }
    var modelError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    
    // 参数开关状态
    var streamEnabled by remember { mutableStateOf(true) }
    var streamValue by remember { mutableStateOf(true) }
    var temperatureEnabled by remember { mutableStateOf(true) }
    var temperature by remember { mutableStateOf("1.00") }
    var maxTokensEnabled by remember { mutableStateOf(true) }
    var maxTokens by remember { mutableStateOf("64000") }
    var topPEnabled by remember { mutableStateOf(false) }
    var topP by remember { mutableStateOf("1.00") }
    var topKEnabled by remember { mutableStateOf(false) }
    var topK by remember { mutableStateOf("40") }
    var thinkingLevelEnabled by remember { mutableStateOf(false) }
    var thinkingLevel by remember { mutableStateOf("1") }
    var enableThinkingEnabled by remember { mutableStateOf(true) }
    var enableThinkingValue by remember { mutableStateOf(true) }
    var thinkingBudgetEnabled by remember { mutableStateOf(false) }
    var thinkingBudget by remember { mutableStateOf("1000") }
    var thinkingEffortEnabled by remember { mutableStateOf(false) }
    var thinkingEffort by remember { mutableStateOf("medium") }
    var isTestingConnection by remember { mutableStateOf(false) }
    var testSuccess by remember { mutableStateOf<Boolean?>(null) }
    var testResponse by remember { mutableStateOf<String?>(null) }
    val canSave = name.isNotBlank() && apiKey.isNotBlank()
    val dividerColor = uiColors.separator.copy(alpha = 0.75f)
    val buildCurrentPreset = {
        val extraParams = buildExtraParams(
            streamEnabled, streamValue, temperatureEnabled, temperature,
            maxTokensEnabled, maxTokens, topPEnabled, topP,
            thinkingLevelEnabled, thinkingLevel, enableThinkingEnabled, enableThinkingValue,
            topKEnabled, topK,
            thinkingBudgetEnabled, thinkingBudget, thinkingEffortEnabled, thinkingEffort
        )
        ApiPreset(
            id = preset?.id ?: 0,
            name = name.trim(),
            type = type,
            provider = provider,
            apiKey = apiKey.trim(),
            baseUrl = baseUrl.trim().ifBlank { getDefaultBaseUrl(provider, type) },
            model = model.trim().ifBlank { getDefaultModel(provider, type) },
            extraParams = extraParams
        )
    }
    val inputFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = uiColors.textPrimary,
        unfocusedTextColor = uiColors.textPrimary,
        disabledTextColor = uiColors.textHint,
        cursorColor = uiColors.accent,
        focusedBorderColor = uiColors.accent,
        unfocusedBorderColor = uiColors.inputBorder,
        disabledBorderColor = uiColors.inputBorder,
        focusedLabelColor = uiColors.accent,
        unfocusedLabelColor = uiColors.textSecondary,
        disabledLabelColor = uiColors.textHint,
        focusedPlaceholderColor = uiColors.textHint,
        unfocusedPlaceholderColor = uiColors.textHint,
        disabledPlaceholderColor = uiColors.textHint,
        focusedContainerColor = uiColors.inputBackground,
        unfocusedContainerColor = uiColors.inputBackground,
        disabledContainerColor = uiColors.inputBackground
    )
    
    Column(modifier = Modifier.fillMaxSize().background(backgroundBrush)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(uiColors.topBar)
                .statusBarsPadding()
                .height(56.dp)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text("取消", color = uiColors.accent, modifier = Modifier.clickable { onBack() })
            Text(
                if (preset == null) "添加预设" else "编辑预设",
                modifier = Modifier.align(Alignment.Center),
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
                color = uiColors.textPrimary
            )
            Text(
                "保存",
                color = if (canSave) uiColors.accent else uiColors.textHint,
                modifier = Modifier.align(Alignment.CenterEnd).clickable(enabled = canSave) {
                    onSave(buildCurrentPreset())
                }
            )
        }
        Divider(color = uiColors.separator, thickness = 0.8.dp)
        
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 基本信息
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ApiPresetSectionTitle("基本信息", uiColors)
                    ApiPresetSectionCard(uiColors) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("预设名称") },
                            placeholder = { Text("例如：OpenAI 主力模型") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true,
                            colors = inputFieldColors
                        )
                    }
                }
            }
            
            // 提供商选择
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ApiPresetSectionTitle("提供商", uiColors)
                    ApiPresetSectionCard(uiColors) {
                        if (type != "voice") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(uiColors.inputBackground)
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ProviderChoiceButton(
                                    label = "OpenAI",
                                    selected = provider == "openai",
                                    onClick = {
                                        provider = "openai"
                                        baseUrl = ""
                                        model = ""
                                        modelError = null
                                    },
                                    uiColors = uiColors,
                                    modifier = Modifier.weight(1f)
                                )
                                ProviderChoiceButton(
                                    label = "Gemini",
                                    selected = provider == "gemini",
                                    onClick = {
                                        provider = "gemini"
                                        baseUrl = ""
                                        model = ""
                                        modelError = null
                                    },
                                    uiColors = uiColors,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        } else {
                            Text("Minimax", fontSize = 16.sp, color = uiColors.textPrimary, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
            
            // API配置
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ApiPresetSectionTitle("API配置", uiColors)
                    ApiPresetSectionCard(uiColors) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = apiKey,
                                onValueChange = { apiKey = it },
                                label = { Text("API Key") },
                                placeholder = { Text("sk-...") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                singleLine = true,
                                colors = inputFieldColors
                            )

                            OutlinedTextField(
                                value = baseUrl,
                                onValueChange = { baseUrl = it },
                                label = { Text("Base URL") },
                                placeholder = { Text(getDefaultBaseUrl(provider, type)) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                singleLine = true,
                                colors = inputFieldColors
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = model,
                                    onValueChange = { model = it },
                                    label = { Text("模型名称") },
                                    placeholder = { Text(getDefaultModel(provider, type)) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(14.dp),
                                    singleLine = true,
                                    colors = inputFieldColors
                                )
                                Button(
                                    onClick = {
                                        if (apiKey.isBlank()) {
                                            modelError = "请先填写 API Key"
                                            return@Button
                                        }
                                        isLoadingModels = true
                                        modelError = null
                                        scope.launch {
                                            try {
                                                val models = LlmApiService.fetchModels(
                                                    provider = provider,
                                                    apiKey = apiKey.trim(),
                                                    baseUrl = baseUrl.trim().ifBlank { getDefaultBaseUrl(provider, type) }
                                                )
                                                if (models.isEmpty()) {
                                                    modelError = "未获取到模型列表"
                                                } else {
                                                    availableModels = models
                                                    showModelPicker = true
                                                }
                                            } catch (e: Exception) {
                                                modelError = "获取失败: ${e.message}"
                                            } finally {
                                                isLoadingModels = false
                                            }
                                        }
                                    },
                                    enabled = !isLoadingModels,
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = uiColors.accent,
                                        contentColor = Color.White,
                                        disabledContainerColor = uiColors.disabledAction,
                                        disabledContentColor = Color.White.copy(alpha = 0.5f)
                                    )
                                ) {
                                    if (isLoadingModels) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            color = Color.White,
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Text("拉取模型", fontSize = 12.sp)
                                    }
                                }
                            }

                            if (modelError != null) {
                                Text(
                                    text = modelError!!,
                                    color = uiColors.error,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
            
            // 模型参数
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ApiPresetSectionTitle("模型参数", uiColors)
                    ApiPresetSectionCard(uiColors) {
                        Column {
                            ParameterRow(
                                label = "流式输出",
                                enabled = streamEnabled,
                                onEnabledChange = { streamEnabled = it },
                                value = if (streamValue) "True" else "False",
                                onValueChange = { streamValue = it == "True" },
                                colors = uiColors,
                                isDropdown = true,
                                dropdownOptions = listOf("True", "False")
                            )

                            Divider(modifier = Modifier.padding(vertical = 12.dp), color = dividerColor)

                            ParameterRow(
                                label = "温度 (0-2)",
                                enabled = temperatureEnabled,
                                onEnabledChange = { temperatureEnabled = it },
                                value = temperature,
                                onValueChange = {
                                    val filtered = it.filter { c -> c.isDigit() || c == '.' }
                                    val parts = filtered.split(".")
                                    if (parts.size <= 2 && (parts.getOrNull(1)?.length ?: 0) <= 2) {
                                        val num = filtered.toFloatOrNull()
                                        if (num == null || num in 0f..2f) {
                                            temperature = filtered
                                        }
                                    }
                                },
                                colors = uiColors
                            )

                            Divider(modifier = Modifier.padding(vertical = 12.dp), color = dividerColor)

                            ParameterRow(
                                label = "最大令牌数",
                                enabled = maxTokensEnabled,
                                onEnabledChange = { maxTokensEnabled = it },
                                value = maxTokens,
                                onValueChange = { if (it.all { c -> c.isDigit() }) maxTokens = it },
                                colors = uiColors
                            )

                            Divider(modifier = Modifier.padding(vertical = 12.dp), color = dividerColor)

                            ParameterRow(
                                label = "Top P (0-1)",
                                enabled = topPEnabled,
                                onEnabledChange = { topPEnabled = it },
                                value = topP,
                                onValueChange = {
                                    val filtered = it.filter { c -> c.isDigit() || c == '.' }
                                    val parts = filtered.split(".")
                                    if (parts.size <= 2 && (parts.getOrNull(1)?.length ?: 0) <= 2) {
                                        val num = filtered.toFloatOrNull()
                                        if (num == null || num in 0f..1f) {
                                            topP = filtered
                                        }
                                    }
                                },
                                colors = uiColors
                            )

                            Divider(modifier = Modifier.padding(vertical = 12.dp), color = dividerColor)

                            ParameterRow(
                                label = "思考级别",
                                enabled = thinkingLevelEnabled,
                                onEnabledChange = { thinkingLevelEnabled = it },
                                value = thinkingLevel,
                                onValueChange = { if (it.all { c -> c.isDigit() }) thinkingLevel = it },
                                colors = uiColors
                            )

                            Divider(modifier = Modifier.padding(vertical = 12.dp), color = dividerColor)

                            ParameterRow(
                                label = "启用思考",
                                enabled = enableThinkingEnabled,
                                onEnabledChange = { enableThinkingEnabled = it },
                                value = if (enableThinkingValue) "True" else "False",
                                onValueChange = { enableThinkingValue = it == "True" },
                                colors = uiColors,
                                isDropdown = true,
                                dropdownOptions = listOf("True", "False")
                            )

                            Divider(modifier = Modifier.padding(vertical = 12.dp), color = dividerColor)

                            ParameterRow(
                                label = "Top K",
                                enabled = topKEnabled,
                                onEnabledChange = { topKEnabled = it },
                                value = topK,
                                onValueChange = { if (it.all { c -> c.isDigit() }) topK = it },
                                colors = uiColors
                            )

                            Divider(modifier = Modifier.padding(vertical = 12.dp), color = dividerColor)

                            ParameterRow(
                                label = "思考预算",
                                enabled = thinkingBudgetEnabled,
                                onEnabledChange = { thinkingBudgetEnabled = it },
                                value = thinkingBudget,
                                onValueChange = { if (it.all { c -> c.isDigit() }) thinkingBudget = it },
                                colors = uiColors
                            )

                            Divider(modifier = Modifier.padding(vertical = 12.dp), color = dividerColor)

                            ParameterRow(
                                label = "思考努力度",
                                enabled = thinkingEffortEnabled,
                                onEnabledChange = { thinkingEffortEnabled = it },
                                value = thinkingEffort,
                                onValueChange = { thinkingEffort = it },
                                colors = uiColors,
                                isDropdown = true,
                                dropdownOptions = listOf("minimal", "low", "medium", "high", "max")
                            )
                        }
                    }
                }
            }
            
            // 测试连通性和保存按钮
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(uiColors.cardBackground)
                            .border(
                                border = BorderStroke(
                                    1.dp,
                                    if (canSave) uiColors.accent.copy(alpha = 0.45f) else uiColors.inputBorder
                                ),
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable(enabled = !isTestingConnection && canSave) {
                                isTestingConnection = true
                                testSuccess = null
                                testResponse = null
                                scope.launch {
                                    try {
                                        val (success, response) = LlmApiService.testConnection(buildCurrentPreset())
                                        testSuccess = success
                                        testResponse = response
                                    } catch (e: Exception) {
                                        testSuccess = false
                                        testResponse = "连接失败: ${e.message}"
                                    } finally {
                                        isTestingConnection = false
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isTestingConnection) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = uiColors.accent,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("测试中...", color = uiColors.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                            }
                        } else {
                            Text(
                                "测试连通性",
                                color = if (canSave) uiColors.accent else uiColors.textHint,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    
                    if (testSuccess != null && testResponse != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (testSuccess == true) uiColors.success.copy(alpha = 0.14f)
                                    else uiColors.error.copy(alpha = 0.14f)
                                )
                                .border(
                                    border = BorderStroke(
                                        1.dp,
                                        if (testSuccess == true) uiColors.success.copy(alpha = 0.4f)
                                        else uiColors.error.copy(alpha = 0.4f)
                                    ),
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Column {
                                Text(
                                    text = if (testSuccess == true) "✓ 连接成功" else "✗ 连接失败",
                                    color = if (testSuccess == true) uiColors.success else uiColors.error,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = testResponse!!,
                                    color = uiColors.textPrimary,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (canSave) uiColors.accent else uiColors.disabledAction)
                            .clickable(enabled = canSave) { onSave(buildCurrentPreset()) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "保存预设",
                            color = Color.White.copy(alpha = if (canSave) 1f else 0.6f),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            
            // 删除按钮
            if (preset != null && onDelete != null) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(uiColors.cardBackground)
                            .border(
                                border = BorderStroke(1.dp, uiColors.error.copy(alpha = 0.45f)),
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable { showDeleteConfirm = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("删除预设", color = uiColors.error, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
    
    if (showDeleteConfirm && preset != null && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("删除预设") },
            text = { Text("确定要删除这个API预设吗？") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(preset.id)
                    showDeleteConfirm = false
                }) {
                    Text("删除", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("取消")
                }
            }
        )
    }
    
    // 模型选择对话框
    if (showModelPicker && availableModels.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { showModelPicker = false },
            title = { Text("选择模型") },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                    items(availableModels) { modelName ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    model = modelName
                                    showModelPicker = false
                                }
                                .padding(vertical = 12.dp, horizontal = 16.dp)
                        ) {
                            Text(modelName, fontSize = 14.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showModelPicker = false }) {
                    Text("取消")
                }
            }
        )
    }
}

private data class ApiPresetEditColors(
    val backgroundTop: Color,
    val backgroundBottom: Color,
    val topBar: Color,
    val cardBackground: Color,
    val cardBorder: Color,
    val inputBackground: Color,
    val inputBorder: Color,
    val separator: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textHint: Color,
    val accent: Color,
    val success: Color,
    val error: Color,
    val disabledAction: Color
)

private fun apiPresetEditColors(isDark: Boolean): ApiPresetEditColors {
    return if (isDark) {
        ApiPresetEditColors(
            backgroundTop = Color(0xFF17181D),
            backgroundBottom = Color(0xFF12131A),
            topBar = Color(0xFF23252D),
            cardBackground = Color(0xFF2A2D36),
            cardBorder = Color(0xFF393D48),
            inputBackground = Color(0xFF1F222B),
            inputBorder = Color(0xFF4B5262),
            separator = Color(0xFF373B46),
            textPrimary = Color(0xFFF4F6FF),
            textSecondary = Color(0xFFBAC1CE),
            textHint = Color(0xFF80899B),
            accent = Color(0xFF32C766),
            success = Color(0xFF32C766),
            error = Color(0xFFFF5E57),
            disabledAction = Color(0xFF3A3F4D)
        )
    } else {
        ApiPresetEditColors(
            backgroundTop = Color(0xFFF5F8FF),
            backgroundBottom = Color(0xFFEDEFF6),
            topBar = Color(0xFFF9FAFD),
            cardBackground = Color.White,
            cardBorder = Color(0xFFD9E0EC),
            inputBackground = Color(0xFFF4F7FC),
            inputBorder = Color(0xFFC5CDDD),
            separator = Color(0xFFDCE3EF),
            textPrimary = Color(0xFF1C2433),
            textSecondary = Color(0xFF5F6A7C),
            textHint = Color(0xFF8D97A8),
            accent = Color(0xFF20A85A),
            success = Color(0xFF20A85A),
            error = Color(0xFFD33A32),
            disabledAction = Color(0xFFC0C8D8)
        )
    }
}

@Composable
private fun ApiPresetSectionTitle(title: String, uiColors: ApiPresetEditColors) {
    Text(
        text = title,
        modifier = Modifier.padding(start = 4.dp),
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        color = uiColors.textSecondary
    )
}

@Composable
private fun ApiPresetSectionCard(
    uiColors: ApiPresetEditColors,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = uiColors.cardBackground,
        border = BorderStroke(1.dp, uiColors.cardBorder),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(modifier = Modifier.padding(14.dp), content = content)
    }
}

@Composable
private fun ProviderChoiceButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    uiColors: ApiPresetEditColors,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) uiColors.accent else uiColors.cardBackground)
            .border(
                border = BorderStroke(
                    1.dp,
                    if (selected) uiColors.accent else uiColors.inputBorder.copy(alpha = 0.9f)
                ),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) Color.White else uiColors.textSecondary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun ParameterRow(
    label: String,
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    value: String,
    onValueChange: (String) -> Unit,
    colors: ApiPresetEditColors,
    readOnly: Boolean = false,
    isDropdown: Boolean = false,
    dropdownOptions: List<String> = emptyList()
) {
    var expanded by remember { mutableStateOf(false) }
    val valueFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = colors.textPrimary,
        unfocusedTextColor = colors.textPrimary,
        disabledTextColor = colors.textPrimary,
        cursorColor = colors.accent,
        focusedBorderColor = colors.accent,
        unfocusedBorderColor = colors.inputBorder,
        disabledBorderColor = colors.inputBorder,
        focusedContainerColor = colors.inputBackground,
        unfocusedContainerColor = colors.inputBackground,
        disabledContainerColor = colors.inputBackground
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            fontSize = 15.sp,
            color = if (enabled) colors.textPrimary else colors.textSecondary
        )

        if (!readOnly && enabled) {
            if (isDropdown) {
                Box(modifier = Modifier.width(136.dp)) {
                    OutlinedTextField(
                        value = value,
                        onValueChange = {},
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = true,
                        enabled = false,
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        textStyle = LocalTextStyle.current.copy(fontSize = 14.sp, fontWeight = FontWeight.Medium),
                        colors = valueFieldColors,
                        trailingIcon = {
                            Text("▼", fontSize = 10.sp, color = colors.textHint)
                        }
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { expanded = true }
                    )
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.width(136.dp)
                    ) {
                        dropdownOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    onValueChange(option)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            } else {
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.width(136.dp),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    textStyle = LocalTextStyle.current.copy(fontSize = 14.sp, fontWeight = FontWeight.Medium),
                    colors = valueFieldColors
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
        } else if (readOnly && enabled) {
            Text(
                value,
                fontSize = 14.sp,
                color = colors.textSecondary,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        } else {
            Text(
                "未启用",
                fontSize = 12.sp,
                color = colors.textHint,
                modifier = Modifier.padding(end = 8.dp)
            )
        }

        Switch(
            checked = enabled,
            onCheckedChange = onEnabledChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = colors.success,
                checkedBorderColor = Color.Transparent,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = colors.inputBorder,
                uncheckedBorderColor = Color.Transparent
            )
        )
    }
}

private fun buildExtraParams(
    streamEnabled: Boolean, streamValue: Boolean, temperatureEnabled: Boolean, temperature: String,
    maxTokensEnabled: Boolean, maxTokens: String, topPEnabled: Boolean, topP: String,
    thinkingLevelEnabled: Boolean, thinkingLevel: String, enableThinkingEnabled: Boolean, enableThinkingValue: Boolean,
    topKEnabled: Boolean, topK: String,
    thinkingBudgetEnabled: Boolean, thinkingBudget: String, thinkingEffortEnabled: Boolean, thinkingEffort: String
): String {
    val params = mutableMapOf<String, Any>()
    
    if (streamEnabled) params["stream"] = streamValue
    if (temperatureEnabled) params["temperature"] = temperature.toFloatOrNull() ?: 1.0f
    if (maxTokensEnabled) params["max_tokens"] = maxTokens.toIntOrNull() ?: 64000
    if (topPEnabled) params["top_p"] = topP.toFloatOrNull() ?: 1.0f
    if (thinkingLevelEnabled) params["thinking_level"] = thinkingLevel.toIntOrNull() ?: 1
    if (enableThinkingEnabled) params["enable_thinking"] = enableThinkingValue
    if (topKEnabled) params["top_k"] = topK.toIntOrNull() ?: 40
    if (thinkingBudgetEnabled) params["thinking_budget"] = thinkingBudget.toIntOrNull() ?: 1000
    if (thinkingEffortEnabled) params["thinking_effort"] = thinkingEffort
    
    return params.entries.joinToString(",", "{", "}") { (k, v) ->
        when (v) {
            is String -> "\"$k\":\"$v\""
            is Boolean -> "\"$k\":$v"
            else -> "\"$k\":$v"
        }
    }
}

private fun getDefaultBaseUrl(provider: String, type: String): String {
    return when (provider) {
        "openai" -> "https://api.openai.com/v1"
        "gemini" -> "https://generativelanguage.googleapis.com/v1beta"
        "minimax" -> "https://api.minimax.chat/v1"
        else -> ""
    }
}

private fun getDefaultModel(provider: String, type: String): String {
    return when {
        provider == "openai" && type == "chat" -> "gpt-4"
        provider == "openai" && type == "image" -> "dall-e-3"
        provider == "gemini" && type == "chat" -> "gemini-pro"
        provider == "gemini" && type == "image" -> "imagen-3.0-generate-001"
        provider == "minimax" && type == "voice" -> "speech-01"
        else -> ""
    }
}
