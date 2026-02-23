package com.WangWangPhone.ui

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

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
                },
                onOpenCard = { newCardId ->
                    selectedCardId = newCardId
                    showCardList = false
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun PersonaBuilderChatScreen(
    cardId: Long,
    onBack: () -> Unit,
    onOpenCard: (Long) -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val dbHelper = remember { PersonaCardDbHelper(context) }
    val presetDbHelper = remember { ApiPresetDbHelper(context) }
    val clipboardManager = LocalClipboardManager.current
    val prefs = remember { context.getSharedPreferences("persona_builder_prefs", Context.MODE_PRIVATE) }
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var personaCard by remember { mutableStateOf<com.WangWangPhone.core.PersonaCard?>(null) }
    var messages by remember { mutableStateOf<List<PersonaMessage>>(emptyList()) }
    var inputText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var streamingContent by remember { mutableStateOf("") }
    var actionMessage by remember { mutableStateOf<PersonaMessage?>(null) }
    var showMessageActionSheet by remember { mutableStateOf(false) }
    var editingMessage by remember { mutableStateOf<PersonaMessage?>(null) }
    var editingText by remember { mutableStateOf("") }
    var pendingBacktrackMessage by remember { mutableStateOf<PersonaMessage?>(null) }
    var showBacktrackFirstDialog by remember { mutableStateOf(false) }
    var hasShownBacktrackWarning by remember {
        mutableStateOf(prefs.getBoolean("backtrack_warning_shown", false))
    }

    LaunchedEffect(cardId) {
        withContext(Dispatchers.IO) {
            personaCard = dbHelper.getCard(cardId)
            messages = dbHelper.getMessages(cardId)
            if (messages.isEmpty()) {
                val welcomeMsg = PersonaMessage(
                    cardId = cardId,
                    role = "assistant",
                    content = "你好！我是神笔马良，专门帮你构建角色人设。\n\n我会通过几个问题来了解你想创建的角色：\n1. 角色的基本信息（姓名、年龄、职业等）\n2. 性格特点\n3. 说话风格\n4. 背景故事\n\n请告诉我，你想创建什么样的角色？",
                    timestamp = System.currentTimeMillis()
                )
                dbHelper.saveMessage(welcomeMsg.cardId, welcomeMsg.role, welcomeMsg.content)
                messages = listOf(welcomeMsg)
            }
        }
    }

    LaunchedEffect(messages.size, streamingContent, isLoading) {
        val targetIndex = when {
            streamingContent.isNotEmpty() -> messages.size
            messages.isNotEmpty() -> messages.size - 1
            else -> -1
        }
        if (targetIndex >= 0) {
            listState.animateScrollToItem(targetIndex)
        }
    }

    suspend fun streamAssistantReply(
        baseMessages: List<PersonaMessage>,
        selectedCard: com.WangWangPhone.core.PersonaCard
    ) {
        val preset = withContext(Dispatchers.IO) {
            presetDbHelper.getPresetById(selectedCard.apiPresetId)
        }
        if (preset == null) {
            android.widget.Toast.makeText(context, "API 预设不存在", android.widget.Toast.LENGTH_SHORT).show()
            return
        }

        val systemPrompt = try {
            context.assets.open("prompt/角色人设设计.txt").bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            Log.e("PersonaBuilder", "Failed to load prompt file", e)
            "你是一个专业的角色人设构建助手，帮助用户通过对话构建完整的角色人设。"
        }

        val conversationHistory = baseMessages.map {
            mapOf("role" to it.role, "content" to it.content)
        }

        try {
            LlmApiService.sendChatRequestStream(
                preset = preset,
                messages = conversationHistory,
                systemPrompt = systemPrompt
            ).collect { chunk ->
                streamingContent += chunk
            }

            if (streamingContent.isNotEmpty()) {
                val assistantMsg = PersonaMessage(
                    cardId = cardId,
                    role = "assistant",
                    content = streamingContent,
                    timestamp = System.currentTimeMillis()
                )
                withContext(Dispatchers.IO) {
                    dbHelper.saveMessage(assistantMsg.cardId, assistantMsg.role, assistantMsg.content)
                }
                messages = messages + assistantMsg
                streamingContent = ""
            }
        } catch (e: Exception) {
            Log.e("PersonaBuilder", "Stream error", e)
            streamingContent = ""
            val errorMsg = PersonaMessage(
                cardId = cardId,
                role = "assistant",
                content = "发送失败: ${e.message}",
                timestamp = System.currentTimeMillis()
            )
            withContext(Dispatchers.IO) {
                dbHelper.saveMessage(errorMsg.cardId, errorMsg.role, errorMsg.content)
            }
            messages = messages + errorMsg
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
                val selectedCard = personaCard
                if (selectedCard == null) {
                    android.widget.Toast.makeText(context, "人设卡不存在", android.widget.Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val userMsg = PersonaMessage(
                    cardId = cardId,
                    role = "user",
                    content = userMessage,
                    timestamp = System.currentTimeMillis()
                )
                withContext(Dispatchers.IO) {
                    dbHelper.saveMessage(userMsg.cardId, userMsg.role, userMsg.content)
                }

                val updatedMessages = messages + userMsg
                messages = updatedMessages
                streamAssistantReply(updatedMessages, selectedCard)
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
                    dbHelper.saveMessage(errorMsg.cardId, errorMsg.role, errorMsg.content)
                }
                messages = messages + errorMsg
            } finally {
                isLoading = false
            }
        }
    }

    fun executeBacktrack(target: PersonaMessage) {
        if (isLoading) return

        coroutineScope.launch {
            val selectedCard = personaCard
            if (selectedCard == null) {
                android.widget.Toast.makeText(context, "人设卡不存在", android.widget.Toast.LENGTH_SHORT).show()
                return@launch
            }

            isLoading = true
            streamingContent = ""
            try {
                withContext(Dispatchers.IO) {
                    dbHelper.deleteMessagesAfter(cardId, target.id)
                }
                val checkpointMessages = withContext(Dispatchers.IO) {
                    dbHelper.getMessages(cardId)
                }
                messages = checkpointMessages
                streamAssistantReply(checkpointMessages, selectedCard)
            } catch (e: Exception) {
                Log.e("PersonaBuilder", "Backtrack failed", e)
                android.widget.Toast.makeText(context, "回溯失败: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
            } finally {
                isLoading = false
            }
        }
    }

    fun executeBranch(target: PersonaMessage) {
        val selectedCard = personaCard ?: return
        val targetIndex = messages.indexOfFirst { it.id == target.id }
        if (targetIndex < 0) return

        val historyToCopy = messages.take(targetIndex + 1)
        if (historyToCopy.isEmpty()) return

        coroutineScope.launch {
            try {
                val branchName = "${selectedCard.name}-${randomHex4()}"
                val newCardId = withContext(Dispatchers.IO) {
                    val id = dbHelper.createCard(branchName, selectedCard.apiPresetId)
                    historyToCopy.forEach { msg ->
                        dbHelper.saveMessage(id, msg.role, msg.content)
                    }
                    id
                }
                android.widget.Toast.makeText(context, "已创建分支: $branchName", android.widget.Toast.LENGTH_SHORT).show()
                onOpenCard(newCardId)
            } catch (e: Exception) {
                Log.e("PersonaBuilder", "Create branch failed", e)
                android.widget.Toast.makeText(context, "分支创建失败: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.systemBars.only(
            WindowInsetsSides.Top + WindowInsetsSides.Horizontal
        ),
        topBar = {
            TopAppBar(
                title = {
                    Text(personaCard?.name ?: "人设对话")
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier
                    .navigationBarsPadding()
                    .imePadding(),
                tonalElevation = 2.dp,
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("输入消息...") },
                        minLines = 1,
                        maxLines = 4
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    FilledIconButton(
                        onClick = { sendMessage() },
                        enabled = inputText.isNotBlank() && !isLoading,
                        modifier = Modifier.size(48.dp)
                    ) {
                        if (isLoading && streamingContent.isEmpty()) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Send, contentDescription = "发送")
                        }
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding),
            state = listState,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(messages) { message ->
                MessageBubble(
                    message = message,
                    onLongPress = { selected ->
                        actionMessage = selected
                        showMessageActionSheet = true
                    }
                )
            }

            if (streamingContent.isNotEmpty()) {
                item {
                    MessageBubble(
                        message = PersonaMessage(
                            cardId = cardId,
                            role = "assistant",
                            content = streamingContent,
                            timestamp = System.currentTimeMillis()
                        )
                    )
                }
            }

            if (isLoading && streamingContent.isEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("正在思考...", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showMessageActionSheet) {
        actionMessage?.let { target ->
            ModalBottomSheet(
                onDismissRequest = {
                    showMessageActionSheet = false
                    actionMessage = null
                }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp)
                ) {
                    Text(
                        text = "消息操作",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                    )
                    MessageActionItem("复制") {
                        clipboardManager.setText(AnnotatedString(target.content))
                        android.widget.Toast.makeText(context, "已复制", android.widget.Toast.LENGTH_SHORT).show()
                        showMessageActionSheet = false
                        actionMessage = null
                    }
                    MessageActionItem("编辑") {
                        editingMessage = target
                        editingText = target.content
                        showMessageActionSheet = false
                        actionMessage = null
                    }
                    MessageActionItem("回溯") {
                        showMessageActionSheet = false
                        actionMessage = null
                        if (hasShownBacktrackWarning) {
                            executeBacktrack(target)
                        } else {
                            pendingBacktrackMessage = target
                            showBacktrackFirstDialog = true
                        }
                    }
                    MessageActionItem("分支") {
                        showMessageActionSheet = false
                        actionMessage = null
                        executeBranch(target)
                    }
                }
            }
        }
    }

    if (showBacktrackFirstDialog && pendingBacktrackMessage != null) {
        AlertDialog(
            onDismissRequest = {
                showBacktrackFirstDialog = false
                pendingBacktrackMessage = null
            },
            title = { Text("首次回溯提示") },
            text = { Text("回溯会清除该消息之后的所有消息，并从该消息作为最后一条重新生成回复。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val target = pendingBacktrackMessage
                        showBacktrackFirstDialog = false
                        pendingBacktrackMessage = null
                        hasShownBacktrackWarning = true
                        prefs.edit().putBoolean("backtrack_warning_shown", true).apply()
                        if (target != null) {
                            executeBacktrack(target)
                        }
                    }
                ) {
                    Text("继续")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showBacktrackFirstDialog = false
                        pendingBacktrackMessage = null
                    }
                ) {
                    Text("取消")
                }
            }
        )
    }

    if (editingMessage != null) {
        AlertDialog(
            onDismissRequest = { editingMessage = null },
            title = { Text("编辑消息") },
            text = {
                OutlinedTextField(
                    value = editingText,
                    onValueChange = { editingText = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 8
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val target = editingMessage ?: return@TextButton
                        val newContent = editingText.trim()
                        if (newContent.isEmpty()) return@TextButton

                        coroutineScope.launch {
                            val success = withContext(Dispatchers.IO) {
                                dbHelper.updateMessageContent(cardId, target.id, newContent)
                            }
                            if (success) {
                                messages = messages.map { msg ->
                                    if (msg.id == target.id) msg.copy(content = newContent) else msg
                                }
                                android.widget.Toast.makeText(context, "消息已更新", android.widget.Toast.LENGTH_SHORT).show()
                            } else {
                                android.widget.Toast.makeText(context, "更新失败", android.widget.Toast.LENGTH_SHORT).show()
                            }
                            editingMessage = null
                        }
                    },
                    enabled = editingText.isNotBlank()
                ) {
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingMessage = null }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun MessageActionItem(title: String, onClick: () -> Unit) {
    Text(
        text = title,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 24.dp, vertical = 14.dp),
        style = MaterialTheme.typography.bodyLarge
    )
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    message: PersonaMessage,
    onLongPress: ((PersonaMessage) -> Unit)? = null
) {
    val isUser = message.role == "user"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        val bubbleModifier = if (onLongPress != null) {
            Modifier
                .widthIn(max = 320.dp)
                .combinedClickable(
                    onClick = {},
                    onLongClick = { onLongPress(message) }
                )
        } else {
            Modifier.widthIn(max = 320.dp)
        }

        Card(
            modifier = bubbleModifier,
            colors = CardDefaults.cardColors(
                containerColor = if (isUser) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            )
        ) {
            Text(
                text = message.content,
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = if (isUser) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

private fun randomHex4(): String {
    val chars = "0123456789abcdef"
    return buildString {
        repeat(4) {
            append(chars[Random.nextInt(chars.length)])
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
