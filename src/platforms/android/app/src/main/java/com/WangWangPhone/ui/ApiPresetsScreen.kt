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
    var showAddDialog by remember { mutableStateOf(false) }
    var editingPreset by remember { mutableStateOf<ApiPreset?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<Long?>(null) }
    
    LaunchedEffect(Unit) {
        presets = dbHelper.getPresetsByType(type)
    }
    
    Column(modifier = Modifier.fillMaxSize().background(bg).statusBarsPadding()) {
        Box(modifier = Modifier.fillMaxWidth().height(56.dp).background(card).padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart) {
            Text("返回", color = Color(0xFF007AFF), modifier = Modifier.clickable { onBack() })
            Text(title, modifier = Modifier.align(Alignment.Center), fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = txt)
            Text("添加", color = Color(0xFF007AFF), modifier = Modifier.align(Alignment.CenterEnd).clickable { showAddDialog = true })
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
                        .clickable { editingPreset = preset }.padding(16.dp)) {
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
    
    if (showAddDialog) {
        ApiPresetEditDialog(
            type = type,
            preset = null,
            onDismiss = { showAddDialog = false },
            onSave = { newPreset ->
                dbHelper.savePreset(newPreset)
                presets = dbHelper.getPresetsByType(type)
                showAddDialog = false
            }
        )
    }
    
    if (editingPreset != null) {
        ApiPresetEditDialog(
            type = type,
            preset = editingPreset,
            onDismiss = { editingPreset = null },
            onSave = { updatedPreset ->
                dbHelper.savePreset(updatedPreset)
                presets = dbHelper.getPresetsByType(type)
                editingPreset = null
            },
            onDelete = { id ->
                showDeleteConfirm = id
            }
        )
    }
    
    if (showDeleteConfirm != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("删除预设") },
            text = { Text("确定要删除这个API预设吗？") },
            confirmButton = {
                TextButton(onClick = {
                    dbHelper.deletePreset(showDeleteConfirm!!)
                    presets = dbHelper.getPresetsByType(type)
                    showDeleteConfirm = null
                    editingPreset = null
                }) {
                    Text("删除", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
fun ApiPresetEditDialog(
    type: String,
    preset: ApiPreset?,
    onDismiss: () -> Unit,
    onSave: (ApiPreset) -> Unit,
    onDelete: ((Long) -> Unit)? = null
) {
    val isDark = isSystemInDarkTheme()
    val txt = if (isDark) Color.White else Color.Black
    var name by remember { mutableStateOf(preset?.name ?: "") }
    var provider by remember { mutableStateOf(preset?.provider ?: if (type == "voice") "minimax" else "openai") }
    var apiKey by remember { mutableStateOf(preset?.apiKey ?: "") }
    var baseUrl by remember { mutableStateOf(preset?.baseUrl ?: "") }
    var model by remember { mutableStateOf(preset?.model ?: "") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (preset == null) "添加预设" else "编辑预设") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("预设名称") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                if (type != "voice") {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = provider == "openai",
                            onClick = { provider = "openai" },
                            label = { Text("OpenAI") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = provider == "gemini",
                            onClick = { provider = "gemini" },
                            label = { Text("Gemini") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                } else {
                    Text("提供商: Minimax", fontSize = 14.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("API Key") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = baseUrl,
                    onValueChange = { baseUrl = it },
                    label = { Text("Base URL") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(getDefaultBaseUrl(provider, type)) },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = model,
                    onValueChange = { model = it },
                    label = { Text("模型名称") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(getDefaultModel(provider, type)) },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank() && apiKey.isNotBlank()) {
                        onSave(
                            ApiPreset(
                                id = preset?.id ?: 0,
                                name = name,
                                type = type,
                                provider = provider,
                                apiKey = apiKey,
                                baseUrl = baseUrl.ifBlank { getDefaultBaseUrl(provider, type) },
                                model = model.ifBlank { getDefaultModel(provider, type) }
                            )
                        )
                    }
                },
                enabled = name.isNotBlank() && apiKey.isNotBlank()
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            Row {
                if (preset != null && onDelete != null) {
                    TextButton(onClick = { onDelete(preset.id) }) {
                        Text("删除", color = Color.Red)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("取消")
                }
            }
        }
    )
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
