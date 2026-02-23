package com.WangWangPhone.ui

import android.net.Uri
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.WangWangPhone.core.ApiPresetDbHelper
import com.WangWangPhone.core.ContactDbHelper
import com.WangWangPhone.core.LlmApiService
import com.WangWangPhone.core.PersonaCardDbHelper
import com.WangWangPhone.core.PersonaMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun PersonaBuilderAppScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val dbHelper = remember { PersonaCardDbHelper(context) }
    val presetDbHelper = remember { ApiPresetDbHelper(context) }
    
    var showCardList by remember { mutableStateOf(true) }
    var selectedCardId by remember { mutableStateOf<Long?>(null) }
    
    if (showCardList) {
        PersonaCardListScreen(
            dbHelper = dbHelper,
            presetDbHelper = presetDbHelper,
            onCardSelected = { cardId ->
                selectedCardId = cardId
                showCardList = false
            },
            onBack = onClose
        )
    } else {
        selectedCardId?.let { cardId ->
            PersonaBuilderChatScreen(
                cardId = cardId,
                onBack = {
                    showCardList = true
                    selectedCardId = null
                }
            )
        }
    }
}

@Composable
fun PersonaBuilderChatScreen(cardId: Long, onBack: () -> Unit) {
    BackHandler { onBack() }
    val isDark = isSystemInDarkTheme()
    val bg = if (isDark) Color(0xFF1C1C1E) else Color(0xFFF2F2F7)
    val card = if (isDark) Color(0xFF2C2C2E) else Color.White
    val txt = if (isDark) Color.White else Color.Black
    
    val context = LocalContext.current
    val dbHelper = remember { PersonaCardDbHelper(context) }
    val presetDbHelper = remember { ApiPresetDbHelper(context) }
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    
    var personaCard by remember { mutableStateOf<com.WangWangPhone.core.PersonaCard?>(null) }
    var messages by remember { mutableStateOf<List<PersonaMessage>>(emptyList()) }
    var inputText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var streamingContent by remember { mutableStateOf("") }
    
    // 加载人设卡和历史消息
    LaunchedEffect(cardId) {
        withContext(Dispatchers.IO) {
            personaCard = dbHelper.getCardById(cardId)
            messages = dbHelper.getMessages(cardId)
            
            // 如果没有消息，添加欢迎消息
            if (messages.isEmpty()) {
                val welcomeMsg = PersonaMessage(
                    cardId = cardId,
                    role = "assistant",
                    content = "你好！我是神笔马良，专门帮你构建角色人设。\n\n我会通过几个问题来了解你想创建的角色：\n1. 角色的基本信息（姓名、年龄、职业等）\n2. 性格特点\n3. 说话风格\n4. 背景故事\n\n请告诉我，你想创建什么样的角色？",
                    timestamp = System.currentTimeMillis()
                )
                dbHelper.addMessage(welcomeMsg)
                messages = listOf(welcomeMsg)
            }
        }
    }
    
    // 自动滚动到底部
    LaunchedEffect(messages.size, streamingContent) {
        if (messages.isNotEmpty() || streamingContent.isNotEmpty()) {
            listState.animateScrollToItem(
                if (streamingContent.isNotEmpty()) messages.size else messages.size - 1
            )
        }
    }
    
    fun sendMessage() {
        if (inputText.isBlank() || isLoading) return
        
        val userMessage = inputText.trim()
        inputText = ""
        
        coroutineScope.launch {
            isLoading = true
            streamingContent = ""
            
            try {
                val card = personaCard
                if (card == null) {
                    android.widget.Toast.makeText(context, "人设卡不存在", android.widget.Toast.LENGTH_SHORT).show()
                    return@launch
                }
                
                val preset = withContext(Dispatchers.IO) {
                    presetDbHelper.getPresetById(card.apiPresetId)
                }
                
                if (preset == null) {
                    android.widget.Toast.makeText(context, "API预设不存在", android.widget.Toast.LENGTH_SHORT).show()
                    return@launch
                }
                
                // 保存用户消息
                val userMsg = PersonaMessage(
                    cardId = cardId,
                    role = "user",
                    content = userMessage,
                    timestamp = System.currentTimeMillis()
                )
                withContext(Dispatchers.IO) {
                    dbHelper.addMessage(userMsg)
                }
                messages = messages + userMsg
                
                // 加载系统提示词
                val systemPrompt = try {
                    context.assets.open("prompt/角色人设设计.txt").bufferedReader().use { it.readText() }
                } catch (e: Exception) {
                    Log.e("PersonaBuilder", "Failed to load prompt file", e)
                    "你是一个专业的角色人设构建助手，帮助用户通过对话构建完整的角色人设。"
                }
                
                val conversationHistory = messages.map {
                    mapOf("role" to it.role, "content" to it.content)
                }
                
                // 使用流式API
                LlmApiService.sendChatRequestStream(
                    preset = preset,
                    messages = conversationHistory,
                    systemPrompt = systemPrompt
                ).catch { e ->
                    Log.e("PersonaBuilder", "Stream error", e)
                    streamingContent = ""
                    val errorMsg = PersonaMessage(
                        cardId = cardId,
                        role = "assistant",
                        content = "发送失败: ${e.message}",
                        timestamp = System.currentTimeMillis()
                    )
                    withContext(Dispatchers.IO) {
                        dbHelper.addMessage(errorMsg)
                    }
                    messages = messages + errorMsg
                }.collect { chunk ->
                    streamingContent += chunk
                }
                
                // 流式响应完成，保存完整消息
                if (streamingContent.isNotEmpty()) {
                    val assistantMsg = PersonaMessage(
                        cardId = cardId,
                        role = "assistant",
                        content = streamingContent,
                        timestamp = System.currentTimeMillis()
                    )
                    withContext(Dispatchers.IO) {
                        dbHelper.addMessage(assistantMsg)
                        dbHelper.updateCardTimestamp(cardId)
                    }
                    messages = messages + assistantMsg
                    streamingContent = ""
                }
                
            } catch (e: Exception) {
                Log.e("PersonaBuilder", "Send message error", e)
                streamingContent = ""
                val errorMsg = PersonaMessage(
                    cardId = cardId,
                    role = "assistant",
                    content = "发送失败: ${e.message}",
                    timestamp = System.currentTimeMillis()
                )
                withContext(Dispatchers.IO) {
                    dbHelper.addMessage(errorMsg)
                }
                messages = messages + errorMsg
            } finally {
                isLoading = false
            }
        }
    }
    
    Box(modifier = Modifier.fillMaxSize().background(bg)) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            // 顶部栏
            Box(
                modifier = Modifier.fillMaxWidth().height(56.dp).background(card)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text("返回", color = Color(0xFF007AFF), modifier = Modifier.clickable { onBack() })
                Text(
                    personaCard?.name ?: "加载中...",
                    modifier = Modifier.align(Alignment.Center),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    color = txt
                )
            }
            
            // 消息列表
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
                state = listState
            ) {
                items(messages) { message ->
                    MessageBubble(message = message, isDark = isDark, card = card, txt = txt)
                    Spacer(modifier = Modifier.height(12.dp))
                }
                
                // 流式响应中的消息
                if (streamingContent.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Box(
                                modifier = Modifier
                                    .widthIn(max = 280.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(card)
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = streamingContent,
                                    color = txt,
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
                
                if (isLoading && streamingContent.isEmpty()) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(card)
                                    .padding(12.dp)
                            ) {
                                Text("正在思考...", color = txt.copy(alpha = 0.6f), fontSize = 14.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
            
            // 输入框
            Row(
                modifier = Modifier.fillMaxWidth()
                    .background(card)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("输入消息...", color = txt.copy(alpha = 0.5f)) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = bg,
                        unfocusedContainerColor = bg,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = txt,
                        unfocusedTextColor = txt
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape)
                        .background(if (inputText.isBlank() || isLoading) Color.Gray else Color(0xFF007AFF))
                        .clickable(enabled = inputText.isNotBlank() && !isLoading) { sendMessage() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("↑", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun MessageBubble(message: PersonaMessage, isDark: Boolean, card: Color, txt: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.role == "user") Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (message.role == "user") Color(0xFF007AFF)
                    else card
                )
                .padding(12.dp)
        ) {
            Text(
                text = message.content,
                color = if (message.role == "user") Color.White else txt,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        }
    }
}
    BackHandler { onClose() }
    val isDark = isSystemInDarkTheme()
    val bg = if (isDark) Color(0xFF1C1C1E) else Color(0xFFF2F2F7)
    val card = if (isDark) Color(0xFF2C2C2E) else Color.White
    val txt = if (isDark) Color.White else Color.Black
    
    var showContactPicker by remember { mutableStateOf(false) }
    var showApiPicker by remember { mutableStateOf(false) }
    var selectedApiId by remember { mutableStateOf<Long?>(null) }
    var messages by remember { mutableStateOf(listOf<PersonaMessage>()) }
    var inputText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var currentPersona by remember { mutableStateOf("") }
    
    val context = LocalContext.current
    val apiPresetDbHelper = remember { ApiPresetDbHelper(context) }
    val contactDbHelper = remember { ContactDbHelper(context) }
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    
    // 初始化系统提示
    LaunchedEffect(Unit) {
        if (messages.isEmpty()) {
            messages = listOf(
                PersonaMessage(
                    role = "assistant",
                    content = "你好！我是神笔马良，专门帮你构建角色人设。\n\n我会通过几个问题来了解你想创建的角色：\n1. 角色的基本信息（姓名、年龄、职业等）\n2. 性格特点\n3. 说话风格\n4. 背景故事\n\n请告诉我，你想创建什么样的角色？"
                )
            )
        }
    }
    
    // 自动滚动到底部
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }
    
    fun sendMessage() {
        if (inputText.isBlank() || isLoading) return
        
        val userMessage = inputText.trim()
        inputText = ""
        messages = messages + PersonaMessage(role = "user", content = userMessage)
        
        coroutineScope.launch {
            isLoading = true
            try {
                val apiId = selectedApiId
                if (apiId == null) {
                    messages = messages + PersonaMessage(
                        role = "assistant",
                        content = "请先选择一个聊天API预设"
                    )
                    return@launch
                }
                
                val preset = apiPresetDbHelper.getPresetById(apiId)
                if (preset == null) {
                    messages = messages + PersonaMessage(
                        role = "assistant",
                        content = "API预设不存在，请重新选择"
                    )
                    return@launch
                }
                
                // 从 assets 读取提示词文件
                val systemPrompt = try {
                    context.assets.open("prompt/角色人设设计.txt").bufferedReader().use { it.readText() }
                } catch (e: Exception) {
                    Log.e("PersonaBuilder", "Failed to load prompt file", e)
                    "你是一个专业的角色人设构建助手，帮助用户通过对话构建完整的角色人设。"
                }
                
                val conversationHistory = messages.map {
                    mapOf("role" to it.role, "content" to it.content)
                }
                
                val response = LlmApiService.sendChatRequest(
                    preset = preset,
                    messages = conversationHistory,
                    systemPrompt = systemPrompt
                )
                
                messages = messages + PersonaMessage(role = "assistant", content = response)
                
                // 提取人设（如果包含【角色人设】标记）
                if (response.contains("【角色人设】")) {
                    currentPersona = response.substringAfter("【角色人设】").trim()
                }
                
            } catch (e: Exception) {
                messages = messages + PersonaMessage(
                    role = "assistant",
                    content = "发送失败: ${e.message}"
                )
            } finally {
                isLoading = false
            }
        }
    }
    
    Box(modifier = Modifier.fillMaxSize().background(bg)) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            // 顶部栏
            Box(
                modifier = Modifier.fillMaxWidth().height(56.dp).background(card)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text("关闭", color = Color(0xFF007AFF), modifier = Modifier.clickable { onClose() })
                Text(
                    "神笔马良",
                    modifier = Modifier.align(Alignment.Center),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    color = txt
                )
                Row(modifier = Modifier.align(Alignment.CenterEnd)) {
                    Text(
                        "导入",
                        color = Color(0xFF007AFF),
                        modifier = Modifier.clickable { showContactPicker = true }
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        "API",
                        color = Color(0xFF007AFF),
                        modifier = Modifier.clickable { showApiPicker = true }
                    )
                }
            }
            
            // API选择提示
            if (selectedApiId == null) {
                Box(
                    modifier = Modifier.fillMaxWidth().background(Color(0xFFFF9500).copy(alpha = 0.1f))
                        .padding(12.dp)
                ) {
                    Text(
                        "请先点击右上角「API」选择聊天API预设",
                        color = Color(0xFFFF9500),
                        fontSize = 13.sp
                    )
                }
            }
            
            // 消息列表
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
                state = listState
            ) {
                items(messages) { message ->
                    MessageBubble(message = message, isDark = isDark, card = card, txt = txt)
                    Spacer(modifier = Modifier.height(12.dp))
                }
                
                if (isLoading) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(card)
                                    .padding(12.dp)
                            ) {
                                Text("正在思考...", color = txt.copy(alpha = 0.6f), fontSize = 14.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
            
            // 操作按钮
            if (currentPersona.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(currentPersona))
                            android.widget.Toast.makeText(context, "人设已复制", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF34C759))
                    ) {
                        Text("复制人设")
                    }
                    Button(
                        onClick = {
                            messages = listOf(
                                PersonaMessage(
                                    role = "assistant",
                                    content = "你好！我是神笔马良，专门帮你构建角色人设。\n\n我会通过几个问题来了解你想创建的角色：\n1. 角色的基本信息（姓名、年龄、职业等）\n2. 性格特点\n3. 说话风格\n4. 背景故事\n\n请告诉我，你想创建什么样的角色？"
                                )
                            )
                            currentPersona = ""
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF))
                    ) {
                        Text("重新开始")
                    }
                }
            }
            
            // 输入框
            Row(
                modifier = Modifier.fillMaxWidth()
                    .background(card)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("输入消息...", color = txt.copy(alpha = 0.5f)) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = bg,
                        unfocusedContainerColor = bg,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = txt,
                        unfocusedTextColor = txt
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape)
                        .background(if (inputText.isBlank() || isLoading) Color.Gray else Color(0xFF007AFF))
                        .clickable(enabled = inputText.isNotBlank() && !isLoading) { sendMessage() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("↑", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        
        // API选择器
        if (showApiPicker) {
            ApiPickerDialog(
                apiPresetDbHelper = apiPresetDbHelper,
                onDismiss = { showApiPicker = false },
                onSelect = { apiId ->
                    selectedApiId = apiId
                    showApiPicker = false
                },
                isDark = isDark,
                bg = bg,
                card = card,
                txt = txt
            )
        }
        
        // 联系人选择器
        if (showContactPicker) {
            ContactPickerDialog(
                contactDbHelper = contactDbHelper,
                onDismiss = { showContactPicker = false },
                onSelect = { contact ->
                    if (contact.persona.isNotEmpty()) {
                        inputText = "请帮我基于以下人设继续完善：\n\n${contact.persona}"
                        showContactPicker = false
                    } else {
                        android.widget.Toast.makeText(context, "该联系人没有人设信息", android.widget.Toast.LENGTH_SHORT).show()
                    }
                },
                isDark = isDark,
                bg = bg,
                card = card,
                txt = txt
            )
        }
    }
}

@Composable
fun MessageBubble(message: PersonaMessage, isDark: Boolean, card: Color, txt: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.role == "user") Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (message.role == "user") Color(0xFF007AFF)
                    else card
                )
                .padding(12.dp)
        ) {
            Text(
                text = message.content,
                color = if (message.role == "user") Color.White else txt,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
fun ApiPickerDialog(
    apiPresetDbHelper: ApiPresetDbHelper,
    onDismiss: () -> Unit,
    onSelect: (Long) -> Unit,
    isDark: Boolean,
    bg: Color,
    card: Color,
    txt: Color
) {
    val chatPresets = remember { apiPresetDbHelper.getPresetsByType("chat") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择聊天API") },
        text = {
            if (chatPresets.isEmpty()) {
                Text("暂无聊天API预设，请先在设置中添加")
            } else {
                LazyColumn {
                    items(chatPresets) { preset ->
                        Box(
                            modifier = Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onSelect(preset.id) }
                                .padding(12.dp)
                        ) {
                            Column {
                                Text(preset.name, fontWeight = FontWeight.Bold, color = txt)
                                Text(
                                    "${preset.provider} - ${preset.model}",
                                    fontSize = 12.sp,
                                    color = txt.copy(alpha = 0.6f)
                                )
                            }
                        }
                        Divider(color = txt.copy(alpha = 0.1f))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
fun ContactPickerDialog(
    contactDbHelper: ContactDbHelper,
    onDismiss: () -> Unit,
    onSelect: (com.WangWangPhone.core.ContactInfo) -> Unit,
    isDark: Boolean,
    bg: Color,
    card: Color,
    txt: Color
) {
    val contacts = remember { contactDbHelper.getAllContacts() }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择联系人") },
        text = {
            if (contacts.isEmpty()) {
                Text("暂无联系人")
            } else {
                LazyColumn {
                    items(contacts) { contact ->
                        Box(
                            modifier = Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onSelect(contact) }
                                .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // 头像
                                if (contact.avatarFileName.isNotEmpty()) {
                                    val avatarPath = contactDbHelper.getAvatarFilePath(contact.avatarFileName)
                                    if (avatarPath != null) {
                                        val bitmap = remember(avatarPath) {
                                            android.graphics.BitmapFactory.decodeFile(avatarPath)
                                        }
                                        if (bitmap != null) {
                                            Image(
                                                bitmap = bitmap.asImageBitmap(),
                                                contentDescription = contact.nickname,
                                                modifier = Modifier.size(40.dp).clip(CircleShape),
                                                contentScale = ContentScale.Crop
                                            )
                                        }
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier.size(40.dp).clip(CircleShape)
                                            .background(Color.Gray),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            contact.nickname.firstOrNull()?.toString() ?: "?",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.width(12.dp))
                                
                                Column {
                                    Text(contact.nickname, fontWeight = FontWeight.Bold, color = txt)
                                    if (contact.persona.isNotEmpty()) {
                                        Text(
                                            "有人设信息",
                                            fontSize = 12.sp,
                                            color = Color(0xFF34C759)
                                        )
                                    } else {
                                        Text(
                                            "无人设信息",
                                            fontSize = 12.sp,
                                            color = txt.copy(alpha = 0.4f)
                                        )
                                    }
                                }
                            }
                        }
                        Divider(color = txt.copy(alpha = 0.1f))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
