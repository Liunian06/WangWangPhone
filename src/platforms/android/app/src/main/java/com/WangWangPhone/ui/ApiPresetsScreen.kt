package com.WangWangPhone.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
    
    Column(modifier = Modifier.fillMaxSize().background(bg).statusBarsPadding()) {
        Box(modifier = Modifier.fillMaxWidth().height(56.dp).background(card).padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart) {
            Text("返回", color = Color(0xFF007AFF), modifier = Modifier.clickable { onBack() })
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
                Text("返回", color = Color(0xFF007AFF), modifier = Modifier.clickable { onBack() })
                Text(title, modifier = Modifier.align(Alignment.Center), fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = txt)
                Text("添加", color = Color(0xFF007AFF), modifier = Modifier.align(Alignment.CenterEnd).clickable {
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
                                    Text(preset.provider.uppercase(), fontSize = 12.sp, color = Color(0xFF007AFF))
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
    val bg = if (isDark) Color(0xFF1C1C1E) else Color(0xFFF2F2F7)
    val card = if (isDark) Color(0xFF2C2C2E) else Color.White
    val txt = if (isDark) Color.White else Color.Black
    
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
    var thinkingBudgetEnabled by remember { mutableStateOf(false) }
    var thinkingBudget by remember { mutableStateOf("1000") }
    var thinkingEffortEnabled by remember { mutableStateOf(false) }
    var thinkingEffort by remember { mutableStateOf("medium") }
    
    Column(modifier = Modifier.fillMaxSize().background(bg).statusBarsPadding()) {
        // 顶部导航栏
        Box(modifier = Modifier.fillMaxWidth().height(56.dp).background(card).padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart) {
            Text("取消", color = Color(0xFF007AFF), modifier = Modifier.clickable { onBack() })
            Text(if (preset == null) "添加预设" else "编辑预设", 
                modifier = Modifier.align(Alignment.Center), 
                fontWeight = FontWeight.SemiBold, 
                fontSize = 18.sp, 
                color = txt)
            Text("保存", 
                color = if (name.isNotBlank() && apiKey.isNotBlank()) Color(0xFF007AFF) else Color.Gray,
                modifier = Modifier.align(Alignment.CenterEnd).clickable(enabled = name.isNotBlank() && apiKey.isNotBlank()) {
                    val extraParams = buildExtraParams(
                        streamEnabled, streamValue, temperatureEnabled, temperature,
                        maxTokensEnabled, maxTokens, topPEnabled, topP,
                        topKEnabled, topK, thinkingLevelEnabled, thinkingLevel,
                        thinkingBudgetEnabled, thinkingBudget, thinkingEffortEnabled, thinkingEffort
                    )
                    onSave(
                        ApiPreset(
                            id = preset?.id ?: 0,
                            name = name,
                            type = type,
                            provider = provider,
                            apiKey = apiKey,
                            baseUrl = baseUrl.ifBlank { getDefaultBaseUrl(provider, type) },
                            model = model.ifBlank { getDefaultModel(provider, type) },
                            extraParams = extraParams
                        )
                    )
                })
        }
        
        LazyColumn(modifier = Modifier.fillMaxSize().padding(vertical = 20.dp)) {
            // 基本信息
            item {
                Text("基本信息", modifier = Modifier.padding(horizontal = 26.dp, vertical = 8.dp), 
                    fontSize = 13.sp, color = Color.Gray)
                Box(modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp)).background(card).padding(16.dp)) {
                    Column {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("预设名称") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = txt,
                                unfocusedTextColor = txt
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
            
            // 提供商选择
            item {
                Text("提供商", modifier = Modifier.padding(horizontal = 26.dp, vertical = 8.dp), 
                    fontSize = 13.sp, color = Color.Gray)
                Box(modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp)).background(card).padding(16.dp)) {
                    if (type != "voice") {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { 
                                    provider = "openai"
                                    baseUrl = ""
                                    model = ""
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (provider == "openai") Color(0xFF007AFF) else Color.Gray.copy(alpha = 0.3f)
                                )
                            ) {
                                Text("OpenAI", color = Color.White)
                            }
                            Button(
                                onClick = { 
                                    provider = "gemini"
                                    baseUrl = ""
                                    model = ""
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (provider == "gemini") Color(0xFF007AFF) else Color.Gray.copy(alpha = 0.3f)
                                )
                            ) {
                                Text("Gemini", color = Color.White)
                            }
                        }
                    } else {
                        Text("Minimax", fontSize = 16.sp, color = txt)
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
            
            // API配置
            item {
                Text("API配置", modifier = Modifier.padding(horizontal = 26.dp, vertical = 8.dp), 
                    fontSize = 13.sp, color = Color.Gray)
                Box(modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp)).background(card).padding(16.dp)) {
                    Column {
                        OutlinedTextField(
                            value = apiKey,
                            onValueChange = { apiKey = it },
                            label = { Text("API Key") },
                            placeholder = { Text("sk-...", color = Color.Gray) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = txt,
                                unfocusedTextColor = txt
                            )
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        OutlinedTextField(
                            value = baseUrl,
                            onValueChange = { baseUrl = it },
                            label = { Text("Base URL") },
                            placeholder = { Text(getDefaultBaseUrl(provider, type), color = Color.Gray) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = txt,
                                unfocusedTextColor = txt
                            )
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = model,
                                onValueChange = { model = it },
                                label = { Text("模型名称") },
                                placeholder = { Text(getDefaultModel(provider, type), color = Color.Gray) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = txt,
                                    unfocusedTextColor = txt
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
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
                                                apiKey = apiKey,
                                                baseUrl = baseUrl.ifBlank { getDefaultBaseUrl(provider, type) }
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
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF))
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
                        
                        // 错误提示
                        if (modelError != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = modelError!!,
                                color = Color.Red,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
            
            // 模型参数
            item {
                Text("模型参数", modifier = Modifier.padding(horizontal = 26.dp, vertical = 8.dp), 
                    fontSize = 13.sp, color = Color.Gray)
                Box(modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp)).background(card).padding(16.dp)) {
                    Column {
                        // 流式输出
                        ParameterRow(
                            label = "流式输出",
                            enabled = streamEnabled,
                            onEnabledChange = { streamEnabled = it },
                            value = if (streamValue) "True" else "False",
                            onValueChange = { streamValue = it == "True" },
                            txt = txt,
                            isDropdown = true,
                            dropdownOptions = listOf("True", "False")
                        )
                        
                        Divider(modifier = Modifier.padding(vertical = 12.dp), color = Color.Gray.copy(alpha = 0.2f))
                        
                        // 温度
                        ParameterRow(
                            label = "温度 (0-2)",
                            enabled = temperatureEnabled,
                            onEnabledChange = { temperatureEnabled = it },
                            value = temperature,
                            onValueChange = { 
                                val filtered = it.filter { c -> c.isDigit() || c == '.' }
                                val parts = filtered.split(".")
                                if (parts.size <= 2 && parts.getOrNull(1)?.length ?: 0 <= 2) {
                                    val num = filtered.toFloatOrNull()
                                    if (num == null || num in 0f..2f) {
                                        temperature = filtered
                                    }
                                }
                            },
                            txt = txt
                        )
                        
                        Divider(modifier = Modifier.padding(vertical = 12.dp), color = Color.Gray.copy(alpha = 0.2f))
                        
                        // Max Tokens
                        ParameterRow(
                            label = "最大令牌数",
                            enabled = maxTokensEnabled,
                            onEnabledChange = { maxTokensEnabled = it },
                            value = maxTokens,
                            onValueChange = { if (it.all { c -> c.isDigit() }) maxTokens = it },
                            txt = txt
                        )
                        
                        Divider(modifier = Modifier.padding(vertical = 12.dp), color = Color.Gray.copy(alpha = 0.2f))
                        
                        // Top P
                        ParameterRow(
                            label = "Top P (0-1)",
                            enabled = topPEnabled,
                            onEnabledChange = { topPEnabled = it },
                            value = topP,
                            onValueChange = { 
                                val filtered = it.filter { c -> c.isDigit() || c == '.' }
                                val parts = filtered.split(".")
                                if (parts.size <= 2 && parts.getOrNull(1)?.length ?: 0 <= 2) {
                                    val num = filtered.toFloatOrNull()
                                    if (num == null || num in 0f..1f) {
                                        topP = filtered
                                    }
                                }
                            },
                            txt = txt
                        )
                        
                        Divider(modifier = Modifier.padding(vertical = 12.dp), color = Color.Gray.copy(alpha = 0.2f))
                        
                        // Top K
                        ParameterRow(
                            label = "Top K",
                            enabled = topKEnabled,
                            onEnabledChange = { topKEnabled = it },
                            value = topK,
                            onValueChange = { if (it.all { c -> c.isDigit() }) topK = it },
                            txt = txt
                        )
                        
                        Divider(modifier = Modifier.padding(vertical = 12.dp), color = Color.Gray.copy(alpha = 0.2f))
                        
                        // Thinking Level
                        ParameterRow(
                            label = "思考级别",
                            enabled = thinkingLevelEnabled,
                            onEnabledChange = { thinkingLevelEnabled = it },
                            value = thinkingLevel,
                            onValueChange = { if (it.all { c -> c.isDigit() }) thinkingLevel = it },
                            txt = txt
                        )
                        
                        Divider(modifier = Modifier.padding(vertical = 12.dp), color = Color.Gray.copy(alpha = 0.2f))
                        
                        // Thinking Budget
                        ParameterRow(
                            label = "思考预算",
                            enabled = thinkingBudgetEnabled,
                            onEnabledChange = { thinkingBudgetEnabled = it },
                            value = thinkingBudget,
                            onValueChange = { if (it.all { c -> c.isDigit() }) thinkingBudget = it },
                            txt = txt
                        )
                        
                        Divider(modifier = Modifier.padding(vertical = 12.dp), color = Color.Gray.copy(alpha = 0.2f))
                        
                        // Thinking Effort
                        ParameterRow(
                            label = "思考努力度",
                            enabled = thinkingEffortEnabled,
                            onEnabledChange = { thinkingEffortEnabled = it },
                            value = thinkingEffort,
                            onValueChange = { thinkingEffort = it },
                            txt = txt,
                            isDropdown = true,
                            dropdownOptions = listOf("minimal", "low", "medium", "high", "max")
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
            
            // 测试连通性和保存按钮
            item {
                var isTestingConnection by remember { mutableStateOf(false) }
                var testSuccess by remember { mutableStateOf<Boolean?>(null) }
                var testResponse by remember { mutableStateOf<String?>(null) }
                
                Column(modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth()) {
                    // 测试连通性按钮
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(card)
                            .clickable(enabled = !isTestingConnection && name.isNotBlank() && apiKey.isNotBlank()) {
                                isTestingConnection = true
                                testSuccess = null
                                testResponse = null
                                scope.launch {
                                    try {
                                        val currentPreset = ApiPreset(
                                            id = preset?.id ?: 0,
                                            name = name,
                                            type = type,
                                            provider = provider,
                                            apiKey = apiKey,
                                            baseUrl = baseUrl.ifBlank { getDefaultBaseUrl(provider, type) },
                                            model = model.ifBlank { getDefaultModel(provider, type) },
                                            extraParams = buildExtraParams(
                                                streamEnabled, streamValue, temperatureEnabled, temperature,
                                                maxTokensEnabled, maxTokens, topPEnabled, topP,
                                                topKEnabled, topK, thinkingLevelEnabled, thinkingLevel,
                                                thinkingBudgetEnabled, thinkingBudget, thinkingEffortEnabled, thinkingEffort
                                            )
                                        )
                                        val (success, response) = LlmApiService.testConnection(currentPreset)
                                        testSuccess = success
                                        testResponse = response
                                    } catch (e: Exception) {
                                        testSuccess = false
                                        testResponse = "连接失败: ${e.message}"
                                    } finally {
                                        isTestingConnection = false
                                    }
                                }
                            }
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isTestingConnection) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color(0xFF007AFF),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("测试中...", color = txt, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                            }
                        } else {
                            Text(
                                "测试连通性",
                                color = if (name.isNotBlank() && apiKey.isNotBlank()) Color(0xFF007AFF) else Color.Gray,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    
                    // 测试结果提示
                    if (testSuccess != null && testResponse != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (testSuccess == true) Color(0xFF34C759).copy(alpha = 0.1f) else Color.Red.copy(alpha = 0.1f))
                                .padding(12.dp)
                        ) {
                            Column {
                                Text(
                                    text = if (testSuccess == true) "✓ 连接成功" else "✗ 连接失败",
                                    color = if (testSuccess == true) Color(0xFF34C759) else Color.Red,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = testResponse!!,
                                    color = txt,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // 保存预设按钮
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (name.isNotBlank() && apiKey.isNotBlank()) Color(0xFF007AFF) else Color.Gray.copy(alpha = 0.3f))
                            .clickable(enabled = name.isNotBlank() && apiKey.isNotBlank()) {
                                val extraParams = buildExtraParams(
                                    streamEnabled, streamValue, temperatureEnabled, temperature,
                                    maxTokensEnabled, maxTokens, topPEnabled, topP,
                                    topKEnabled, topK, thinkingLevelEnabled, thinkingLevel,
                                    thinkingBudgetEnabled, thinkingBudget, thinkingEffortEnabled, thinkingEffort
                                )
                                onSave(
                                    ApiPreset(
                                        id = preset?.id ?: 0,
                                        name = name,
                                        type = type,
                                        provider = provider,
                                        apiKey = apiKey,
                                        baseUrl = baseUrl.ifBlank { getDefaultBaseUrl(provider, type) },
                                        model = model.ifBlank { getDefaultModel(provider, type) },
                                        extraParams = extraParams
                                    )
                                )
                            }
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("保存预设", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
            
            // 删除按钮
            if (preset != null && onDelete != null) {
                item {
                    Box(modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp)).background(card)
                        .clickable { showDeleteConfirm = true }.padding(16.dp),
                        contentAlignment = Alignment.Center) {
                        Text("删除预设", color = Color.Red, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    }
                    Spacer(modifier = Modifier.height(20.dp))
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

@Composable
fun ParameterRow(
    label: String,
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    value: String,
    onValueChange: (String) -> Unit,
    txt: Color,
    readOnly: Boolean = false,
    isDropdown: Boolean = false,
    dropdownOptions: List<String> = emptyList()
) {
    var expanded by remember { mutableStateOf(false) }
    
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontSize = 14.sp, color = txt)
        }
        
        if (!readOnly && enabled) {
            if (isDropdown) {
                Box(modifier = Modifier.width(120.dp)) {
                    OutlinedTextField(
                        value = value,
                        onValueChange = {},
                        modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
                        readOnly = true,
                        enabled = false,
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = txt,
                            disabledBorderColor = Color.Gray
                        ),
                        trailingIcon = {
                            Text("▼", fontSize = 10.sp, color = Color.Gray,
                                modifier = Modifier.clickable { expanded = !expanded })
                        }
                    )
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.width(120.dp)
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
                    modifier = Modifier.width(120.dp),
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = txt,
                        unfocusedTextColor = txt
                    )
                )
            }
        } else if (readOnly && enabled) {
            Text(value, fontSize = 14.sp, color = Color.Gray, modifier = Modifier.padding(end = 8.dp))
        }
        
        Switch(
            checked = enabled,
            onCheckedChange = onEnabledChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF34C759),
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color.Gray
            )
        )
    }
}

private fun buildExtraParams(
    streamEnabled: Boolean, streamValue: Boolean, temperatureEnabled: Boolean, temperature: String,
    maxTokensEnabled: Boolean, maxTokens: String, topPEnabled: Boolean, topP: String,
    topKEnabled: Boolean, topK: String, thinkingLevelEnabled: Boolean, thinkingLevel: String,
    thinkingBudgetEnabled: Boolean, thinkingBudget: String, thinkingEffortEnabled: Boolean, thinkingEffort: String
): String {
    val params = mutableMapOf<String, Any>()
    
    if (streamEnabled) params["stream"] = streamValue
    if (temperatureEnabled) params["temperature"] = temperature.toFloatOrNull() ?: 1.0f
    if (maxTokensEnabled) params["max_tokens"] = maxTokens.toIntOrNull() ?: 64000
    if (topPEnabled) params["top_p"] = topP.toFloatOrNull() ?: 1.0f
    if (topKEnabled) params["top_k"] = topK.toIntOrNull() ?: 40
    if (thinkingLevelEnabled) params["thinking_level"] = thinkingLevel.toIntOrNull() ?: 1
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
