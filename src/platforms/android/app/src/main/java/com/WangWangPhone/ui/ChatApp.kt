
package com.WangWangPhone.ui

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.WangWangPhone.core.UserProfileDbHelper
import com.WangWangPhone.core.ContactDbHelper
import com.WangWangPhone.core.ContactInfo
import com.WangWangPhone.core.ChatDbHelper
import com.WangWangPhone.core.ConversationData
import com.WangWangPhone.core.MessageData
import com.WangWangPhone.core.ApiPresetDbHelper
import com.WangWangPhone.core.LlmApiService
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ============================================
// 微信风格配色 (Light/Dark Mode)
// ============================================
object WeTheme {
    val isDark: Boolean @Composable get() = isSystemInDarkTheme()

    val BrandGreen: Color @Composable get() = if (isDark) Color(0xFF06AD56) else Color(0xFF07C160)
    val Background: Color @Composable get() = if (isDark) Color(0xFF111111) else Color(0xFFEDEDED)
    val BackgroundCell: Color @Composable get() = if (isDark) Color(0xFF191919) else Color(0xFFFFFFFF)
    val TextPrimary: Color @Composable get() = if (isDark) Color(0xFFD3D3D3) else Color(0xFF191919)
    val TextSecondary: Color @Composable get() = if (isDark) Color(0xFF666666) else Color(0xFF808080)
    val TextHint: Color @Composable get() = if (isDark) Color(0xFF4C4C4C) else Color(0xFFB2B2B2)
    val Separator: Color @Composable get() = if (isDark) Color(0xFF2C2C2C) else Color(0x19000000)
    
    val TabTextSelected: Color @Composable get() = BrandGreen
    val TabTextNormal: Color @Composable get() = if (isDark) Color(0xFF555555) else Color(0xFF191919)
    
    val BubbleSent: Color @Composable get() = if (isDark) Color(0xFF2EA260) else Color(0xFF95EC69)
    val BubbleReceived: Color @Composable get() = if (isDark) Color(0xFF2C2C2C) else Color(0xFFFFFFFF)
    
    val SearchBarBg: Color @Composable get() = if (isDark) Color(0xFF202020) else Color(0xFFFFFFFF)
}

// ============================================
// 资源加载辅助
// ============================================
@Composable
fun WeIcon(
    name: String,
    fallback: String,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified
) {
    val context = LocalContext.current
    val resName = name.replace(".svg", "")
    val resId = remember(resName) {
        context.resources.getIdentifier(resName, "drawable", context.packageName)
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (resId != 0) {
            if (tint != Color.Unspecified) {
                Icon(
                    painter = painterResource(id = resId),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    tint = tint
                )
            } else {
                Image(
                    painter = painterResource(id = resId),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else {
            Box(modifier = Modifier.fillMaxSize().border(1.dp, Color.Red)) {
                 Text("MISSING", fontSize = 6.sp, color = Color.Red, modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

// ============================================
// 数据模型
// ============================================
enum class ConvType { CHAT, SUBSCRIPTION, SERVICE }

data class Conversation(
    val id: String,
    val name: String,
    val avatar: String,
    val lastMsg: String,
    val time: String,
    val unread: Int = 0,
    val muted: Boolean = false,
    val iconBg: Color = Color(0xFF4CAF50),
    val type: ConvType = ConvType.CHAT
)

data class ContactGroup(val id: String, val name: String, val icon: String, val color: Color)
data class Contact(val id: String, val name: String, val avatar: String, val letter: String = "")
data class Moment(val id: String, val name: String, val avatar: String, val content: String, val time: String, val likes: List<String>, val comments: List<Pair<String, String>>)
data class ChatMessage(val type: String, val name: String = "", val avatar: String = "", val text: String = "")

// ============================================
// API预设选择界面
// ============================================
@Composable
fun ApiPresetSelectionScreen(
    onBack: () -> Unit,
    onPresetSelected: (Long) -> Unit,
    apiPresetDbHelper: ApiPresetDbHelper
) {
    val chatPresets = remember { apiPresetDbHelper.getPresetsByType("chat") }
    
    BackHandler { onBack() }
    
    Column(modifier = Modifier.fillMaxSize().background(WeTheme.Background).statusBarsPadding()) {
        Box(modifier = Modifier.fillMaxWidth().height(50.dp).background(WeTheme.Background), contentAlignment = Alignment.Center) {
            WeIcon("ic_nav_back", "‹", modifier = Modifier.align(Alignment.CenterStart).padding(start = 12.dp).size(24.dp).clickable { onBack() }, tint = WeTheme.TextPrimary)
            Text("选择API预设", fontWeight = FontWeight.SemiBold, fontSize = 17.sp, color = WeTheme.TextPrimary)
        }
        Divider(color = WeTheme.Separator, thickness = 0.5.dp)
        
        if (chatPresets.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("暂无聊天API预设，请先在设置中添加", fontSize = 14.sp, color = WeTheme.TextSecondary)
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(chatPresets) { preset ->
                    Box(
                        modifier = Modifier.fillMaxWidth().background(WeTheme.BackgroundCell)
                            .clickable { onPresetSelected(preset.id) }
                            .padding(16.dp, 12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(preset.name, fontSize = 16.sp, color = WeTheme.TextPrimary, modifier = Modifier.weight(1f))
                            Text(preset.provider.uppercase(), fontSize = 12.sp, color = WeTheme.BrandGreen)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text("模型: ${preset.model}", fontSize = 12.sp, color = WeTheme.TextSecondary)
                    }
                    Divider(color = WeTheme.Separator, thickness = 0.5.dp, modifier = Modifier.padding(start = 16.dp))
                }
            }
        }
    }
}

// ============================================
// 联系人选择界面（用于发起聊天）
// ============================================
@Composable
fun ContactSelectionScreen(
    onBack: () -> Unit,
    onContactsSelected: (String, String) -> Unit,
    contactDbHelper: ContactDbHelper,
    selectedContact1: String?,
    selectedContact2: String?,
    onContact1Selected: (String?) -> Unit,
    onContact2Selected: (String?) -> Unit,
    allContacts: List<ContactInfo>
) {
    // 使用传入的状态变量
    val selectedContact1State = selectedContact1
    val selectedContact2State = selectedContact2
    val onContact1SelectedState = onContact1Selected
    val onContact2SelectedState = onContact2Selected
    
    BackHandler { onBack() }
    
    Column(modifier = Modifier.fillMaxSize().background(WeTheme.Background).statusBarsPadding()) {
        Box(modifier = Modifier.fillMaxWidth().height(50.dp).background(WeTheme.Background), contentAlignment = Alignment.Center) {
            WeIcon("ic_nav_back", "‹", modifier = Modifier.align(Alignment.CenterStart).padding(start = 12.dp).size(24.dp).clickable { onBack() }, tint = WeTheme.TextPrimary)
            Text("选择联系人", fontWeight = FontWeight.SemiBold, fontSize = 17.sp, color = WeTheme.TextPrimary)
        }
        Divider(color = WeTheme.Separator, thickness = 0.5.dp)
        
        // 提示信息
        Box(
            modifier = Modifier.fillMaxWidth().background(WeTheme.BackgroundCell).padding(16.dp)
        ) {
            Column {
                Text("请选择两个联系人：", fontSize = 14.sp, color = WeTheme.TextPrimary, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(8.dp))
                Text("1. 第一个联系人：AI角色人设", fontSize = 13.sp, color = WeTheme.TextSecondary)
                Text("2. 第二个联系人：用户人设", fontSize = 13.sp, color = WeTheme.TextSecondary)
            }
        }
        
        Spacer(Modifier.height(8.dp))
        
        // 已选择的联系人显示
        if (selectedContact1State != null || selectedContact2State != null) {
            Column(modifier = Modifier.fillMaxWidth().background(WeTheme.BackgroundCell).padding(16.dp)) {
                if (selectedContact1State != null) {
                    val contact1 = allContacts.find { it.id == selectedContact1State }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("角色人设：", fontSize = 14.sp, color = WeTheme.TextSecondary)
                        Spacer(Modifier.width(8.dp))
                        Text(contact1?.nickname ?: "", fontSize = 14.sp, color = WeTheme.BrandGreen, fontWeight = FontWeight.Medium)
                    }
                }
                if (selectedContact2State != null) {
                    Spacer(Modifier.height(8.dp))
                    val contact2 = allContacts.find { it.id == selectedContact2State }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("用户人设：", fontSize = 14.sp, color = WeTheme.TextSecondary)
                        Spacer(Modifier.width(8.dp))
                        Text(contact2?.nickname ?: "", fontSize = 14.sp, color = WeTheme.BrandGreen, fontWeight = FontWeight.Medium)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        
        // 联系人列表
        LazyColumn(modifier = Modifier.weight(1f)) {
            var curLetter = ""
            allContacts.forEach { c ->
                val letter = c.getPinyinInitial()
                if (letter != curLetter) {
                    curLetter = letter
                    item {
                        Text(curLetter, modifier = Modifier.fillMaxWidth().background(WeTheme.Background).padding(16.dp, 6.dp), fontSize = 12.sp, color = WeTheme.TextSecondary)
                    }
                }
                item {
                    ContactSelectionItem(
                        contact = c,
                        isSelected = c.id == selectedContact1State || c.id == selectedContact2State,
                        selectionLabel = when (c.id) {
                            selectedContact1State -> "角色"
                            selectedContact2State -> "用户"
                            else -> null
                        },
                        onClick = {
                            when {
                                selectedContact1State == null -> onContact1SelectedState(c.id)
                                selectedContact2State == null && c.id != selectedContact1State -> onContact2SelectedState(c.id)
                                c.id == selectedContact1State -> onContact1SelectedState(null)
                                c.id == selectedContact2State -> onContact2SelectedState(null)
                            }
                        },
                        contactDbHelper = contactDbHelper
                    )
                }
            }
        }
        
        // 确认按钮
        Box(
            modifier = Modifier.fillMaxWidth().background(WeTheme.BackgroundCell).padding(16.dp)
        ) {
            // API预设选择按钮
            Box(
                modifier = Modifier.fillMaxWidth().height(48.dp).background(
                    if (selectedContact1 != null && selectedContact2 != null) WeTheme.BrandGreen else Color(0xFFCCCCCC),
                    RoundedCornerShape(8.dp)
                ).clickable(enabled = selectedContact1 != null && selectedContact2 != null) {
                    if (selectedContact1State != null && selectedContact2State != null) {
                        onContactsSelected(selectedContact1State!!, selectedContact2State!!)
                    }
                },
                contentAlignment = Alignment.Center
            ) {
                Text("选择API预设", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
fun ContactSelectionItem(
    contact: ContactInfo,
    isSelected: Boolean,
    selectionLabel: String?,
    onClick: () -> Unit,
    contactDbHelper: ContactDbHelper
) {
    val avatarPath = remember(contact.avatarFileName) {
        contact.avatarFileName?.let { contactDbHelper.getAvatarFilePath(it) }
    }
    
    Row(
        modifier = Modifier.fillMaxWidth().background(WeTheme.BackgroundCell).clickable { onClick() }.padding(16.dp, 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 头像
        if (avatarPath != null) {
            val bitmap = remember(avatarPath) { BitmapFactory.decodeFile(avatarPath) }
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "头像",
                    modifier = Modifier.size(36.dp).clip(RoundedCornerShape(4.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier.size(36.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFFF5F5DC)),
                    contentAlignment = Alignment.Center
                ) { Text(contact.persona.firstOrNull()?.toString() ?: "👤", fontSize = 22.sp) }
            }
        } else {
            Box(
                modifier = Modifier.size(36.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFFF5F5DC)),
                contentAlignment = Alignment.Center
            ) { Text(contact.persona.firstOrNull()?.toString() ?: "👤", fontSize = 22.sp) }
        }
        
        Spacer(Modifier.width(16.dp))
        Text(contact.nickname, fontSize = 16.sp, color = WeTheme.TextPrimary, modifier = Modifier.weight(1f))
        
        // 选择标签
        if (isSelected && selectionLabel != null) {
            Box(
                modifier = Modifier.background(WeTheme.BrandGreen, RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(selectionLabel, fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Medium)
            }
        }
    }
    Divider(color = WeTheme.Separator, thickness = 0.5.dp, modifier = Modifier.padding(start = 68.dp))
}

// ============================================
// 聊天设置界面
// ============================================
@Composable
fun ChatSettingsScreen(
    chatId: String,
    onBack: () -> Unit,
    chatDbHelper: ChatDbHelper,
    apiPresetDbHelper: ApiPresetDbHelper,
    onApiPresetChanged: () -> Unit
) {
    val context = LocalContext.current
    val conversation = remember(chatId) { chatDbHelper.getConversationById(chatId) }
    val currentPresetId = conversation?.apiPresetId ?: -1L
    val currentPreset = remember(currentPresetId) {
        if (currentPresetId != -1L) {
            apiPresetDbHelper.getPresetById(currentPresetId)
        } else {
            null
        }
    }
    val chatPresets = remember { apiPresetDbHelper.getPresetsByType("chat") }
    
    BackHandler { onBack() }
    
    Column(modifier = Modifier.fillMaxSize().background(WeTheme.Background).statusBarsPadding()) {
        Box(modifier = Modifier.fillMaxWidth().height(50.dp).background(WeTheme.Background), contentAlignment = Alignment.Center) {
            WeIcon("ic_nav_back", "‹", modifier = Modifier.align(Alignment.CenterStart).padding(start = 12.dp).size(24.dp).clickable { onBack() }, tint = WeTheme.TextPrimary)
            Text("聊天设置", fontWeight = FontWeight.SemiBold, fontSize = 17.sp, color = WeTheme.TextPrimary)
        }
        Divider(color = WeTheme.Separator, thickness = 0.5.dp)
        
        // API预设设置
        Column(modifier = Modifier.fillMaxWidth().background(WeTheme.BackgroundCell)) {
            Text("API预设", fontSize = 14.sp, color = WeTheme.TextSecondary, modifier = Modifier.padding(16.dp, 12.dp, 16.dp, 0.dp))
            
            // 当前选择的预设
            Box(
                modifier = Modifier.fillMaxWidth().padding(16.dp, 12.dp)
            ) {
                if (currentPreset != null) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(currentPreset.name, fontSize = 16.sp, color = WeTheme.TextPrimary, modifier = Modifier.weight(1f))
                            Text(currentPreset.provider.uppercase(), fontSize = 12.sp, color = WeTheme.BrandGreen)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text("模型: ${currentPreset.model}", fontSize = 12.sp, color = WeTheme.TextSecondary)
                    }
                } else {
                    Text("未选择API预设", fontSize = 16.sp, color = WeTheme.TextHint)
                }
            }
            
            Divider(color = WeTheme.Separator, thickness = 0.5.dp, modifier = Modifier.padding(start = 16.dp))
            
            // 更改按钮
            Box(
                modifier = Modifier.fillMaxWidth().padding(16.dp, 12.dp)
                    .clickable {
                        // 跳转到API预设选择界面
                        onBack()
                        onApiPresetChanged()
                    }
            ) {
                Text("更改API预设", fontSize = 16.sp, color = WeTheme.BrandGreen)
            }
        }
        
        Spacer(Modifier.height(16.dp))
        
        // 其他设置选项（可以扩展）
        Column(modifier = Modifier.fillMaxWidth().background(WeTheme.BackgroundCell)) {
            Text("其他设置", fontSize = 14.sp, color = WeTheme.TextSecondary, modifier = Modifier.padding(16.dp, 12.dp, 16.dp, 0.dp))
            
            // 静音设置
            var isMuted by remember { mutableStateOf(conversation?.isMuted ?: false) }
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp, 12.dp)
                    .clickable {
                        isMuted = !isMuted
                        chatDbHelper.setMuted(chatId, isMuted)
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("消息免打扰", fontSize = 16.sp, color = WeTheme.TextPrimary, modifier = Modifier.weight(1f))
                Switch(
                    checked = isMuted,
                    onCheckedChange = { newValue ->
                        isMuted = newValue
                        chatDbHelper.setMuted(chatId, newValue)
                    },
                    colors = SwitchDefaults.colors(
                        uncheckedThumbColor = WeTheme.BackgroundCell,
                        uncheckedTrackColor = WeTheme.TextHint,
                        checkedThumbColor = Color.White,
                        checkedTrackColor = WeTheme.BrandGreen
                    )
                )
            }
        }
    }
}

// ============================================
// 从数据库加载会话列表
// ============================================
fun getConversations(contactDbHelper: ContactDbHelper, chatDbHelper: ChatDbHelper): List<Conversation> {
    val conversations = mutableListOf<Conversation>()
    val allConvs = chatDbHelper.getAllConversations()
    
    allConvs.forEach { conv ->
        val contactInfo = contactDbHelper.getContactById(conv.aiRoleId)
        if (contactInfo != null) {
            conversations.add(
                Conversation(
                    id = conv.id,
                    name = contactInfo.nickname,
                    avatar = contactInfo.persona.firstOrNull()?.toString() ?: "👤",
                    lastMsg = conv.lastMessage,
                    time = formatTime(conv.lastMessageTime),
                    unread = conv.unreadCount,
                    muted = false,
                    iconBg = Color(0xFF4CAF50),
                    type = ConvType.CHAT
                )
            )
        }
    }
    
    return conversations.sortedByDescending { it.time }
}

fun formatTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    return when {
        diff < 60000 -> "刚刚"
        diff < 3600000 -> "${diff / 60000}分钟前"
        diff < 86400000 -> SimpleDateFormat("HH:mm", Locale.CHINA).format(Date(timestamp))
        diff < 172800000 -> "昨天"
        else -> SimpleDateFormat("MM-dd", Locale.CHINA).format(Date(timestamp))
    }
}

val mockMoments = listOf(
    Moment("m1", "小明", "😊", "今天天气真好，出去走走 🌞", "1分钟前", listOf("小红", "李华"), listOf("小红" to "确实不错！")),
)

val mockChatMessages = mapOf(
    "c1" to listOf(
        ChatMessage("time", text = "昨天 16:04"),
        ChatMessage("received", "文件传输助手", "📁", "你好，欢迎使用文件传输助手"),
        ChatMessage("sent", text = "你好，这是一条测试消息"),
    ),
)

// ============================================
// 主入口
// ============================================
@Composable
fun ChatAppScreen(onClose: () -> Unit) {
    var currentTab by remember { mutableStateOf("messages") }
    var currentView by remember { mutableStateOf("main") }
    var currentChatId by remember { mutableStateOf<String?>(null) }
    var currentContactId by remember { mutableStateOf<String?>(null) }
    var editingContactId by remember { mutableStateOf<String?>(null) }
    var showContactSelection by remember { mutableStateOf(false) }
    var showApiPresetSelection by remember { mutableStateOf(false) }
    var selectedApiPresetId by remember { mutableStateOf(-1L) }
    var selectedContact1Id by remember { mutableStateOf<String?>(null) }
    var selectedContact2Id by remember { mutableStateOf<String?>(null) }

    var showChatSettings by remember { mutableStateOf(false) }

    // 用户资料状态 - 在顶层管理，传递给子组件
    val context = LocalContext.current
    val profileDbHelper = remember { UserProfileDbHelper(context) }
    val contactDbHelper = remember { ContactDbHelper(context) }
    val chatDbHelper = remember { ChatDbHelper(context) }
    val apiPresetDbHelper = remember { ApiPresetDbHelper(context) }
    var userNickname by remember { mutableStateOf(profileDbHelper.getUserProfile().nickname) }
    var userSignature by remember { mutableStateOf(profileDbHelper.getUserProfile().signature) }
    var avatarPath by remember { mutableStateOf(profileDbHelper.getAvatarFilePath()) }
    var coverPath by remember { mutableStateOf(profileDbHelper.getCoverFilePath()) }
    var contactsRefreshTrigger by remember { mutableStateOf(0) }
    var chatsRefreshTrigger by remember { mutableStateOf(0) }

    BackHandler {
        when (currentView) {
            "chat-detail" -> { currentView = "main"; currentChatId = null }
            "contact-detail" -> { currentView = "main"; currentContactId = null }
            "add-contact" -> { currentView = "main"; currentTab = "contacts" }
            "edit-contact" -> { currentView = "contact-detail"; editingContactId = null }
            "service" -> { currentView = "main"; currentTab = "me" }
            "select-contacts" -> { currentView = "main"; showContactSelection = false }
            "select-api-preset" -> { currentView = "select-contacts"; showApiPresetSelection = false }
            "chat-settings" -> { currentView = "chat-detail"; showChatSettings = false }
            else -> onClose()
        }
    }

    when (currentView) {
         "select-contacts" -> {
             var selectedContact1 by remember { mutableStateOf<String?>(null) }
             var selectedContact2 by remember { mutableStateOf<String?>(null) }
             val allContacts = remember { contactDbHelper.getAllContacts() }
             
             ContactSelectionScreen(
                 onBack = { currentView = "main"; showContactSelection = false },
                 onContactsSelected = { contact1Id, contact2Id ->
                     // 保存选择的联系人
                     selectedContact1Id = contact1Id
                     selectedContact2Id = contact2Id
                     // 跳转到API预设选择界面
                     currentView = "select-api-preset"
                     showApiPresetSelection = true
                 },
                 contactDbHelper = contactDbHelper,
                 selectedContact1 = selectedContact1,
                 selectedContact2 = selectedContact2,
                 onContact1Selected = { selectedContact1 = it },
                 onContact2Selected = { selectedContact2 = it },
                 allContacts = allContacts
             )
         }
       "select-api-preset" -> ApiPresetSelectionScreen(
           onBack = {
               if (currentView == "chat-settings") {
                   currentView = "chat-settings"
               } else {
                   currentView = "select-contacts"
               }
               showApiPresetSelection = false
           },
           onPresetSelected = { presetId ->
               if (currentView == "chat-settings" && currentChatId != null) {
                   // 更新现有聊天的API预设
                   if (chatDbHelper.updateApiPresetId(currentChatId!!, presetId)) {
                       currentView = "chat-detail"
                       showApiPresetSelection = false
                       chatsRefreshTrigger++
                   }
               } else {
                   // 创建新聊天
                   if (selectedContact1Id != null && selectedContact2Id != null) {
                       val chatId = chatDbHelper.createConversation(selectedContact1Id!!, selectedContact2Id!!, presetId)
                       if (chatId != null) {
                           currentChatId = chatId
                           currentView = "chat-detail"
                           showApiPresetSelection = false
                           chatsRefreshTrigger++
                       }
                   }
               }
           },
           apiPresetDbHelper = ApiPresetDbHelper(context)
       )
        "chat-detail" -> ChatDetailScreen(
            chatId = currentChatId ?: "",
            onBack = {
                chatsRefreshTrigger++
                currentView = "main"
                currentChatId = null
            },
            onSettingsClick = {
                // 跳转到聊天设置界面
                currentView = "chat-settings"
                showChatSettings = true
            },
            avatarPath = avatarPath,
            contactDbHelper = contactDbHelper,
            chatDbHelper = chatDbHelper
        )
        "contact-detail" -> ContactDetailScreen(
            contactId = currentContactId ?: "",
            onBack = { currentView = "main"; currentContactId = null },
            onSendMessage = { id -> currentChatId = id; currentView = "chat-detail" },
            onEdit = { id -> editingContactId = id; currentView = "edit-contact" },
            onDelete = {
                contactsRefreshTrigger++
                currentView = "main"
                currentContactId = null
                currentTab = "contacts"
            },
            contactDbHelper = contactDbHelper
        )
        "edit-contact" -> EditContactScreen(
            contactId = editingContactId ?: "",
            onBack = { currentView = "contact-detail" },
            onContactUpdated = {
                contactsRefreshTrigger++
                currentView = "contact-detail"
            },
            contactDbHelper = contactDbHelper
        )
        "add-contact" -> AddContactScreen(
            onBack = { currentView = "main"; currentTab = "contacts" },
            onContactAdded = {
                contactsRefreshTrigger++
                currentView = "main"
                currentTab = "contacts"
            },
            contactDbHelper = contactDbHelper
        )
        "chat-settings" -> ChatSettingsScreen(
            chatId = currentChatId ?: "",
            onBack = { currentView = "chat-detail"; showChatSettings = false },
            chatDbHelper = chatDbHelper,
            apiPresetDbHelper = apiPresetDbHelper,
            onApiPresetChanged = {
                // 跳转到API预设选择界面用于更改现有聊天的API预设
                currentView = "select-api-preset"
                showApiPresetSelection = true
            }
        )
        "service" -> ServiceScreen(
            onBack = { currentView = "main"; currentTab = "me" }
        )
        else -> ChatMainScreen(
            currentTab = currentTab,
            onTabChange = { currentTab = it },
            onClose = onClose,
            onOpenChat = { id ->
                if (id == "new") {
                    currentView = "select-contacts"
                    showContactSelection = true
                } else {
                    currentChatId = id
                    currentView = "chat-detail"
                }
            },
            onOpenContact = { id -> currentContactId = id; currentView = "contact-detail" },
            onAddContact = { currentView = "add-contact" },
            onOpenService = { currentView = "service" },
            userNickname = userNickname,
            userSignature = userSignature,
            avatarPath = avatarPath,
            coverPath = coverPath,
            contactDbHelper = contactDbHelper,
            chatDbHelper = chatDbHelper,
            contactsRefreshTrigger = contactsRefreshTrigger,
            chatsRefreshTrigger = chatsRefreshTrigger,
            onNicknameChanged = { newName ->
                profileDbHelper.updateNickname(newName)
                userNickname = newName
            },
            onSignatureChanged = { newSig ->
                profileDbHelper.updateSignature(newSig)
                userSignature = newSig
            },
            onAvatarChanged = { uri ->
                profileDbHelper.updateAvatar(uri)
                avatarPath = profileDbHelper.getAvatarFilePath()
            },
            onCoverChanged = { uri ->
                profileDbHelper.updateCover(uri)
                coverPath = profileDbHelper.getCoverFilePath()
            }
        )
    }
}

// ============================================
// 主界面（含底部Tab）
// ============================================
@Composable
fun ChatMainScreen(
    currentTab: String,
    onTabChange: (String) -> Unit,
    onClose: () -> Unit,
    onOpenChat: (String) -> Unit,
    onOpenContact: (String) -> Unit,
    onAddContact: () -> Unit,
    onOpenService: () -> Unit,
    userNickname: String,
    userSignature: String,
    avatarPath: String?,
    coverPath: String?,
    contactDbHelper: ContactDbHelper,
    chatDbHelper: ChatDbHelper,
    contactsRefreshTrigger: Int,
    chatsRefreshTrigger: Int,
    onNicknameChanged: (String) -> Unit,
    onSignatureChanged: (String) -> Unit,
    onAvatarChanged: (Uri) -> Unit,
    onCoverChanged: (Uri) -> Unit
) {
    val titles = mapOf("messages" to "微信", "contacts" to "通讯录", "moments" to "朋友圈", "me" to "我")
    val bgColor = WeTheme.Background
    val conversations = remember(chatsRefreshTrigger) { getConversations(contactDbHelper, chatDbHelper) }

    Column(modifier = Modifier.fillMaxSize().background(bgColor).statusBarsPadding()) {
        // Content (权重为1，撑开中间部分)
        Box(modifier = Modifier.weight(1f)) {
            when (currentTab) {
            "messages" -> {
                Column {
                    WeChatHeader(
                        title = titles[currentTab] ?: "",
                        onClose = onClose,
                        showBack = false,
                        onStartChat = { onOpenChat("new") }
                    )
                    MessagesTab(onOpenChat, conversations, contactDbHelper)
                }
            }
                "contacts" -> {
                    Column {
                        WeChatHeader(
                            title = titles[currentTab] ?: "",
                            onClose = onClose,
                            showBack = false,
                            showAdd = true,
                            onAddClick = onAddContact
                        )
                        ContactsTab(
                            onOpenContact = onOpenContact,
                            contactDbHelper = contactDbHelper,
                            refreshTrigger = contactsRefreshTrigger
                        )
                    }
                }
                "moments" -> {
                    Column {
                        WeChatHeader(
                            title = titles[currentTab] ?: "",
                            onClose = onClose,
                            showBack = false
                        )
                        MomentsTab(
                            userNickname = userNickname,
                            userSignature = userSignature,
                            avatarPath = avatarPath,
                            coverPath = coverPath,
                            onAvatarChanged = onAvatarChanged,
                            onCoverChanged = onCoverChanged,
                            onNicknameChanged = onNicknameChanged,
                            onSignatureChanged = onSignatureChanged
                        )
                    }
                }
                "me" -> {
                    MeTab(
                        onOpenService = onOpenService,
                        onOpenMoments = { onTabChange("moments") },
                        userNickname = userNickname,
                        avatarPath = avatarPath
                    )
                }
            }
        }

        // Tab bar
        Divider(color = WeTheme.Separator, thickness = 0.5.dp)
        ChatTabBar(currentTab, onTabChange, conversations)
    }
}

@Composable
fun WeChatHeader(
    title: String,
    onClose: () -> Unit,
    showBack: Boolean = false,
    showAdd: Boolean = true,
    onAddClick: (() -> Unit)? = null,
    onStartChat: (() -> Unit)? = null
) {
    val bgColor = WeTheme.Background
    val textColor = WeTheme.TextPrimary
    var showMenu by remember { mutableStateOf(false) }
    
    Box(
        modifier = Modifier.fillMaxWidth().height(50.dp).background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Text(title, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, color = textColor)

        Row(modifier = Modifier.align(Alignment.CenterEnd).padding(end = 12.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            WeIcon("ic_search", "🔍", modifier = Modifier.size(24.dp), tint = textColor)
            if (showAdd) {
                Box {
                    WeIcon(
                        "ic_chat_add", "⊕",
                        modifier = Modifier.size(24.dp).clickable { showMenu = true },
                        tint = textColor
                    )
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier
                            .background(Color(0xFF414041))
                            .width(140.dp)
                    ) {
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    WeIcon("ic_chats", "💬", modifier = Modifier.size(20.dp), tint = Color.White)
                                    Spacer(Modifier.width(12.dp))
                                    Text("发起聊天", color = Color.White, fontSize = 16.sp)
                                }
                            },
                            onClick = {
                                showMenu = false
                                onStartChat?.invoke()
                            },
                            modifier = Modifier.height(48.dp)
                        )
                        Divider(color = Color(0xFF5A5A5A), thickness = 0.5.dp)
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    WeIcon("ic_add_friends", "👤", modifier = Modifier.size(20.dp), tint = Color.White)
                                    Spacer(Modifier.width(12.dp))
                                    Text("添加朋友", color = Color.White, fontSize = 16.sp)
                                }
                            },
                            onClick = {
                                showMenu = false
                                onAddClick?.invoke()
                            },
                            modifier = Modifier.height(48.dp)
                        )
                        Divider(color = Color(0xFF5A5A5A), thickness = 0.5.dp)
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    WeIcon("ic_scan", "📷", modifier = Modifier.size(20.dp), tint = Color.White)
                                    Spacer(Modifier.width(12.dp))
                                    Text("扫一扫", color = Color.White, fontSize = 16.sp)
                                }
                            },
                            onClick = { showMenu = false },
                            modifier = Modifier.height(48.dp)
                        )
                        Divider(color = Color(0xFF5A5A5A), thickness = 0.5.dp)
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    WeIcon("ic_pay_vendor", "💳", modifier = Modifier.size(20.dp), tint = Color.White)
                                    Spacer(Modifier.width(12.dp))
                                    Text("收付款", color = Color.White, fontSize = 16.sp)
                                }
                            },
                            onClick = { showMenu = false },
                            modifier = Modifier.height(48.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChatTabBar(currentTab: String, onTabChange: (String) -> Unit, conversations: List<Conversation> = emptyList()) {
    val totalUnread = conversations.sumOf { it.unread }
    data class TabItem(val id: String, val iconSelected: String, val iconNormal: String, val label: String, val fallback: String)
    
    val tabs = listOf(
        TabItem("messages", "ic_tab_chat_selected", "ic_tab_chat_normal", "微信", "💬"),
        TabItem("contacts", "ic_tab_contacts_selected", "ic_tab_contacts_normal", "通讯录", "👥"),
        TabItem("moments", "ic_tab_discover_selected", "ic_tab_discover_normal", "发现", "🧭"),
        TabItem("me", "ic_tab_me_selected", "ic_tab_me_normal", "我", "👤"),
    )

    val bg = if (WeTheme.isDark) Color(0xFF191919) else Color(0xFFF7F7F7)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(bg)
            .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(56.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEach { item ->
                val isActive = currentTab == item.id
                val color = if (isActive) WeTheme.TabTextSelected else WeTheme.TabTextNormal
                
                Column(
                    modifier = Modifier.weight(1f).clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onTabChange(item.id) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        val iconName = if (isActive) item.iconSelected else item.iconNormal
                        WeIcon(iconName, item.fallback, modifier = Modifier.size(28.dp), tint = if (isActive) Color.Unspecified else WeTheme.TabTextNormal)
                        
                        if (item.id == "messages" && totalUnread > 0) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(y = 4.dp)
                                    .size(0.dp)
                                    .wrapContentSize(unbounded = true, align = Alignment.Center)
                            ) {
                                TabUnreadBadge(count = totalUnread)
                            }
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(item.label, fontSize = 10.sp, color = color, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

// ============================================
// Tab1: 消息列表
// ============================================
@Composable
fun UnreadBadge(conv: Conversation, modifier: Modifier = Modifier) {
    if (conv.unread <= 0) return
    
    when {
        conv.type == ConvType.SUBSCRIPTION || conv.type == ConvType.SERVICE -> {
            Image(
                painter = painterResource(id = LocalContext.current.resources.getIdentifier("ic_badge_new", "drawable", LocalContext.current.packageName)),
                contentDescription = "New",
                modifier = modifier.height(18.dp).width(38.dp)
            )
        }
        conv.muted -> {
            Image(
                painter = painterResource(id = LocalContext.current.resources.getIdentifier("ic_badge_dot", "drawable", LocalContext.current.packageName)),
                contentDescription = "dot",
                modifier = modifier.size(8.dp)
            )
        }
        conv.unread > 99 -> {
            Image(
                painter = painterResource(id = LocalContext.current.resources.getIdentifier("ic_badge_more", "drawable", LocalContext.current.packageName)),
                contentDescription = "99+",
                modifier = modifier.height(18.dp).width(33.dp)
            )
        }
        else -> {
            Box(
                modifier = modifier.size(18.dp).background(Color(0xFFFA5151), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("${conv.unread}", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun TabUnreadBadge(count: Int, modifier: Modifier = Modifier) {
    if (count <= 0) return
    when {
        count > 99 -> {
            Image(
                painter = painterResource(id = LocalContext.current.resources.getIdentifier("ic_badge_more", "drawable", LocalContext.current.packageName)),
                contentDescription = "99+",
                modifier = modifier.height(18.dp).width(33.dp)
            )
        }
        else -> {
            Box(
                modifier = modifier.size(18.dp).background(Color(0xFFFA5151), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("$count", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun MessagesTab(onOpenChat: (String) -> Unit, conversations: List<Conversation>, contactDbHelper: ContactDbHelper) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(conversations) { conv ->
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onOpenChat(conv.id) }.background(WeTheme.BackgroundCell).padding(16.dp, 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(52.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // 如果是联系人会话，显示真实头像
                    if (conv.id.startsWith("contact_")) {
                        val contactId = conv.id.removePrefix("contact_")
                        val contactInfo = remember(contactId) { contactDbHelper.getContactById(contactId) }
                        val avatarPath = remember(contactInfo?.avatarFileName) {
                            contactInfo?.avatarFileName?.let { contactDbHelper.getAvatarFilePath(it) }
                        }
                        
                        if (avatarPath != null) {
                            val bitmap = remember(avatarPath) { BitmapFactory.decodeFile(avatarPath) }
                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "头像",
                                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(4.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(4.dp)).background(conv.iconBg),
                                    contentAlignment = Alignment.Center
                                ) { Text(conv.avatar, fontSize = 24.sp) }
                            }
                        } else {
                            Box(
                                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(4.dp)).background(conv.iconBg),
                                contentAlignment = Alignment.Center
                            ) { Text(conv.avatar, fontSize = 24.sp) }
                        }
                    } else {
                        Box(
                            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(4.dp)).background(conv.iconBg),
                            contentAlignment = Alignment.Center
                        ) { Text(conv.avatar, fontSize = 24.sp) }
                    }
                    
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = (-2).dp, y = 2.dp)
                            .size(0.dp)
                            .wrapContentSize(unbounded = true, align = Alignment.Center)
                    ) {
                        UnreadBadge(conv = conv)
                    }
                }

                Spacer(Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(conv.name, fontSize = 16.sp, maxLines = 1, color = WeTheme.TextPrimary)
                        Text(conv.time, fontSize = 11.sp, color = WeTheme.TextHint)
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(conv.lastMsg, fontSize = 14.sp, color = WeTheme.TextSecondary, maxLines = 1, modifier = Modifier.weight(1f))
                        if (conv.muted) WeIcon("ic_mute", "🔇", modifier = Modifier.size(16.dp), tint = WeTheme.TextHint)
                    }
                }
            }
            Divider(color = WeTheme.Separator, thickness = 0.5.dp, modifier = Modifier.padding(start = 76.dp))
        }
    }
}

// ============================================
// Tab2: 通讯录
// ============================================
@Composable
fun ContactsTab(
    onOpenContact: (String) -> Unit,
    contactDbHelper: ContactDbHelper,
    refreshTrigger: Int
) {
    val scrollState = rememberScrollState()
    val allContacts = remember(refreshTrigger) { contactDbHelper.getAllContacts() }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(scrollState)) {
            var curLetter = ""
            allContacts.forEach { c ->
                val letter = c.getPinyinInitial()
                if (letter != curLetter) {
                    curLetter = letter
                    Text(curLetter, modifier = Modifier.fillMaxWidth().background(WeTheme.Background).padding(16.dp, 6.dp), fontSize = 12.sp, color = WeTheme.TextSecondary)
                }
                ContactItemRow(
                    Contact(c.id, c.nickname, c.persona, letter),
                    onOpenContact,
                    contactDbHelper
                )
            }
        }

        val letters = if (allContacts.isNotEmpty()) {
            allContacts.map { it.getPinyinInitial() }.distinct()
        } else {
            listOf("#")
        }
        Column(
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            letters.forEach { l ->
                Text(l, fontSize = 10.sp, color = Color(0xFF07C160), modifier = Modifier.padding(vertical = 1.dp))
            }
        }
    }
}

@Composable
fun ContactItemRow(contact: Contact, onOpenContact: (String) -> Unit, contactDbHelper: ContactDbHelper) {
    val context = LocalContext.current
    val contactInfo = remember(contact.id) { contactDbHelper.getContactById(contact.id) }
    val avatarPath = remember(contactInfo?.avatarFileName) {
        contactInfo?.avatarFileName?.let { contactDbHelper.getAvatarFilePath(it) }
    }
    
    Row(
        modifier = Modifier.fillMaxWidth().background(WeTheme.BackgroundCell).clickable {
            onOpenContact(contact.id)
        }.padding(16.dp, 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 显示真实头像或默认emoji
        if (avatarPath != null) {
            val bitmap = remember(avatarPath) { BitmapFactory.decodeFile(avatarPath) }
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "头像",
                    modifier = Modifier.size(36.dp).clip(RoundedCornerShape(4.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier.size(36.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFFF5F5DC)),
                    contentAlignment = Alignment.Center
                ) { Text(contact.avatar.firstOrNull()?.toString() ?: "👤", fontSize = 22.sp) }
            }
        } else {
            Box(
                modifier = Modifier.size(36.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFFF5F5DC)),
                contentAlignment = Alignment.Center
            ) { Text(contact.avatar.firstOrNull()?.toString() ?: "👤", fontSize = 22.sp) }
        }
        Spacer(Modifier.width(16.dp))
        Text(contact.name, fontSize = 16.sp, color = WeTheme.TextPrimary)
    }
    Divider(color = WeTheme.Separator, thickness = 0.5.dp, modifier = Modifier.padding(start = 68.dp))
}

// ============================================
// 用户头像组件（复用：支持从文件加载或显示默认emoji）
// ============================================
@Composable
fun UserAvatarImage(
    avatarPath: String?,
    size: androidx.compose.ui.unit.Dp,
    cornerRadius: androidx.compose.ui.unit.Dp = 10.dp,
    defaultEmoji: String = "🐱",
    defaultEmojiSize: androidx.compose.ui.unit.TextUnit = 32.sp,
    modifier: Modifier = Modifier
) {
    if (avatarPath != null) {
        val bitmap = remember(avatarPath) { BitmapFactory.decodeFile(avatarPath) }
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "头像",
                modifier = modifier.size(size).clip(RoundedCornerShape(cornerRadius)),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = modifier.size(size).clip(RoundedCornerShape(cornerRadius)).background(Color(0xFFF5F5DC)),
                contentAlignment = Alignment.Center
            ) { Text(defaultEmoji, fontSize = defaultEmojiSize) }
        }
    } else {
        Box(
            modifier = modifier.size(size).clip(RoundedCornerShape(cornerRadius)).background(Color(0xFFF5F5DC)),
            contentAlignment = Alignment.Center
        ) { Text(defaultEmoji, fontSize = defaultEmojiSize) }
    }
}

// ============================================
// 文本编辑对话框
// ============================================
@Composable
fun TextEditDialog(
    title: String,
    currentValue: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf(currentValue) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color.White,
            tonalElevation = 8.dp
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF191919))
                Spacer(Modifier.height(16.dp))
                BasicTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 16.sp, color = Color(0xFF191919)),
                    singleLine = true
                )
                Spacer(Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("取消", color = Color(0xFF999999))
                    }
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = { onConfirm(text); onDismiss() }) {
                        Text("确定", color = Color(0xFF07C160))
                    }
                }
            }
        }
    }
}

// ============================================
// Tab3: 朋友圈
// ============================================
@Composable
fun MomentsTab(
    userNickname: String,
    userSignature: String,
    avatarPath: String?,
    coverPath: String?,
    onAvatarChanged: (Uri) -> Unit,
    onCoverChanged: (Uri) -> Unit,
    onNicknameChanged: (String) -> Unit,
    onSignatureChanged: (String) -> Unit
) {
    val avatarSize = 64.dp
    val avatarOverlap = 22.dp
    val coverHeight = 300.dp

    // 图片选择器
    val coverLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { onCoverChanged(it) }
    }
    val avatarLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { onAvatarChanged(it) }
    }

    // 编辑对话框状态
    var showNicknameDialog by remember { mutableStateOf(false) }
    var showSignatureDialog by remember { mutableStateOf(false) }
    val momentCellBackground = if (WeTheme.isDark) WeTheme.Background else WeTheme.BackgroundCell
    val momentListBackground = momentCellBackground
    val momentPrimaryColor = if (WeTheme.isDark) Color(0xFF8795B3) else Color(0xFF576B95)
    val momentContentColor = if (WeTheme.isDark) Color(0xFFD3D3D3) else Color(0xFF191919)
    val momentAvatarBackground = if (WeTheme.isDark) Color(0xFF2C2C2C) else Color(0xFFF5F5F5)
    val momentActionBackground = if (WeTheme.isDark) Color(0xFF2A2A2A) else Color(0xFFF7F7F7)
    val momentTimeColor = if (WeTheme.isDark) Color(0xFF666666) else Color(0xFFB2B2B2)
    val momentSignatureColor = if (WeTheme.isDark) Color(0xFF666666) else Color(0xFF999999)
    val momentDividerColor = WeTheme.Separator

    if (showNicknameDialog) {
        TextEditDialog(
            title = "修改昵称",
            currentValue = userNickname,
            onDismiss = { showNicknameDialog = false },
            onConfirm = { onNicknameChanged(it) }
        )
    }

    if (showSignatureDialog) {
        TextEditDialog(
            title = "修改签名",
            currentValue = userSignature,
            onDismiss = { showSignatureDialog = false },
            onConfirm = { onSignatureChanged(it) }
        )
    }

    LazyColumn(modifier = Modifier.fillMaxSize().background(momentListBackground)) {
        // 封面 + 头像 + 签名区域
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                // 封面 + 头像叠加区域
                Box(
                    modifier = Modifier.fillMaxWidth().height(coverHeight + avatarOverlap)
                ) {
                    // 封面背景（可点击更换）
                    Box(
                        modifier = Modifier.fillMaxWidth().height(coverHeight)
                            .clickable { coverLauncher.launch("image/*") }
                    ) {
                        if (coverPath != null) {
                            val coverBitmap = remember(coverPath) { BitmapFactory.decodeFile(coverPath) }
                            if (coverBitmap != null) {
                                Image(
                                    bitmap = coverBitmap.asImageBitmap(),
                                    contentDescription = "朋友圈封面",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(modifier = Modifier.fillMaxSize()
                                    .background(Brush.linearGradient(listOf(Color(0xFF667EEA), Color(0xFF764BA2)))))
                            }
                        } else {
                            Box(modifier = Modifier.fillMaxSize()
                                .background(Brush.linearGradient(listOf(Color(0xFF667EEA), Color(0xFF764BA2)))))
                        }
                        // 封面右下角提示文字
                        Text(
                            "点击更换封面",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 11.sp,
                            modifier = Modifier.align(Alignment.BottomStart).padding(12.dp)
                        )
                    }

                    // 昵称 + 头像
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 16.dp),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // 昵称（可点击编辑）
                        Text(
                            userNickname,
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = avatarOverlap + 6.dp)
                                .clickable { showNicknameDialog = true }
                        )
                        // 头像（可点击更换）
                        Box(
                            modifier = Modifier
                                .size(avatarSize)
                                .clip(RoundedCornerShape(10.dp))
                                .border(2.dp, Color.White, RoundedCornerShape(10.dp))
                                .clickable { avatarLauncher.launch("image/*") }
                        ) {
                            UserAvatarImage(
                                avatarPath = avatarPath,
                                size = avatarSize,
                                cornerRadius = 10.dp
                            )
                        }
                    }
                }

                // 用户签名文字区域（可点击编辑）
                Box(
                    modifier = Modifier.fillMaxWidth().background(momentCellBackground)
                        .clickable { showSignatureDialog = true }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Text(
                        userSignature,
                        fontSize = 13.sp,
                        color = momentSignatureColor,
                        maxLines = 1
                    )
                }
            }
        }

        // 动态列表
        items(mockMoments) { m ->
            Column(modifier = Modifier.fillMaxWidth().background(momentCellBackground).padding(12.dp, 12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier.size(40.dp).clip(RoundedCornerShape(6.dp)).background(momentAvatarBackground),
                        contentAlignment = Alignment.Center
                    ) { Text(m.avatar, fontSize = 22.sp) }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(m.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = momentPrimaryColor)
                        Spacer(Modifier.height(4.dp))
                        Text(m.content, fontSize = 15.sp, lineHeight = 22.sp, color = momentContentColor)
                        Spacer(Modifier.height(8.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(m.time, fontSize = 12.sp, color = momentTimeColor)
                            Box(
                                modifier = Modifier.background(momentActionBackground, RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)
                            ) { Text("··", fontSize = 14.sp, color = momentPrimaryColor) }
                        }

                        // 互动区
                        if (m.likes.isNotEmpty() || m.comments.isNotEmpty()) {
                            Spacer(Modifier.height(6.dp))
                            Column(modifier = Modifier.fillMaxWidth().background(momentActionBackground, RoundedCornerShape(4.dp)).padding(6.dp, 6.dp)) {
                                if (m.likes.isNotEmpty()) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        WeIcon("ic_like", "♡", modifier = Modifier.size(18.dp), tint = momentPrimaryColor)
                                        Text(m.likes.joinToString("，"), fontSize = 13.sp, color = momentPrimaryColor)
                                    }
                                }
                                if (m.likes.isNotEmpty() && m.comments.isNotEmpty()) {
                                    Divider(color = momentDividerColor, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 4.dp))
                                }
                                m.comments.forEach { (name, text) ->
                                    Row {
                                        Text(name, fontSize = 13.sp, color = momentPrimaryColor, fontWeight = FontWeight.Medium)
                                        Text("：$text", fontSize = 13.sp, color = momentContentColor)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Divider(color = momentDividerColor, thickness = 0.5.dp)
        }
    }
}

// ============================================
// Tab4: 我的页面
// ============================================
@Composable
fun MeTab(
    onOpenService: () -> Unit,
    onOpenMoments: () -> Unit,
    userNickname: String,
    avatarPath: String?
) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        // 个人信息卡片
        Column(
            modifier = Modifier.fillMaxWidth().background(WeTheme.BackgroundCell)
                .padding(start = 24.dp, end = 24.dp, top = 60.dp, bottom = 24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 头像
                UserAvatarImage(
                    avatarPath = avatarPath,
                    size = 72.dp,
                    cornerRadius = 8.dp
                )
                Spacer(Modifier.width(20.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(userNickname, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = WeTheme.TextPrimary)
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("微信号：WangWang_User", fontSize = 16.sp, color = WeTheme.TextSecondary)
                        Spacer(Modifier.width(8.dp))
                        WeIcon("ic_badge_more", "▒", modifier = Modifier.size(16.dp), tint = WeTheme.TextSecondary)
                        Spacer(Modifier.width(4.dp))
                        Text("›", fontSize = 16.sp, color = WeTheme.TextHint)
                    }
                }
            }
            
            Spacer(Modifier.height(24.dp))
            
            // 状态和朋友按钮
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier.border(0.5.dp, Color(0xFFE0E0E0), RoundedCornerShape(50))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text("+ 状态", fontSize = 14.sp, color = WeTheme.TextSecondary)
                }
                Box(
                    modifier = Modifier.border(0.5.dp, Color(0xFFE0E0E0), RoundedCornerShape(50))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text("👤👤👤 等40个朋友 ●", fontSize = 14.sp, color = WeTheme.TextSecondary)
                }
            }
        }

        // 菜单
        data class MenuItem(
            val icon: String,
            val label: String,
            val tint: Color = Color.Unspecified,
            val fallback: String,
            val action: (() -> Unit)? = null
        )
        
        val menuGroups = listOf(
            listOf(
                MenuItem("ic_pay_logo", "服务", Color(0xFF07C160), "✅", onOpenService)
            ),
            listOf(
                MenuItem("ic_favorites", "收藏", Color.Unspecified, "⭐"),
                MenuItem("ic_moment", "朋友圈", Color.Unspecified, "🖼️", onOpenMoments),
                MenuItem("ic_album", "视频号和公众号", Color(0xFFFA9D3B), "📺"),
                MenuItem("ic_cards", "订单与卡包", Color.Unspecified, "🛒"),
                MenuItem("ic_chat_emoji", "表情", Color(0xFFFFC300), "😊"),
            ),
            listOf(
                MenuItem("ic_setting", "设置", Color(0xFF1976D2), "⚙️")
            ),
        )

        menuGroups.forEach { group ->
            Spacer(Modifier.height(8.dp))
            group.forEachIndexed { idx, item ->
                Row(
                    modifier = Modifier.fillMaxWidth().background(WeTheme.BackgroundCell)
                        .then(if (item.action != null) Modifier.clickable { item.action!!() } else Modifier)
                        .padding(16.dp, 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    WeIcon(item.icon, item.fallback, modifier = Modifier.size(24.dp), tint = item.tint)
                    Spacer(Modifier.width(16.dp))
                    Text(item.label, fontSize = 16.sp, color = WeTheme.TextPrimary, modifier = Modifier.weight(1f))
                    Text("›", fontSize = 16.sp, color = Color(0xFFC0C0C0))
                }
                if (idx < group.size - 1) {
                    Divider(color = WeTheme.Separator, thickness = 0.5.dp, modifier = Modifier.padding(start = 56.dp))
                }
            }
        }
    }
}

// ============================================
// 聊天详情页
// ============================================
@Composable
fun ChatDetailScreen(
    chatId: String,
    onBack: () -> Unit,
    onSettingsClick: () -> Unit,
    avatarPath: String? = null,
    contactDbHelper: ContactDbHelper? = null,
    chatDbHelper: ChatDbHelper? = null
) {
    val context = LocalContext.current
    
    // 如果是联系人会话，获取联系人信息
    val contactInfo = remember(chatId) {
        if (chatId.startsWith("contact_")) {
            val contactId = chatId.removePrefix("contact_")
            contactDbHelper?.getContactById(contactId)
        } else null
    }
    
    val chatName = contactInfo?.nickname ?: "聊天"
    val chatAvatar = contactInfo?.persona?.firstOrNull()?.toString() ?: "👤"
    val contactAvatarPath = remember(contactInfo?.avatarFileName) {
        contactInfo?.avatarFileName?.let { contactDbHelper?.getAvatarFilePath(it) }
    }
    
    // 从数据库加载历史消息
    val messages = remember(chatId) {
        mutableStateListOf<ChatMessage>().also { list ->
            if (chatId.startsWith("contact_")) {
                val contactId = chatId.removePrefix("contact_")
                val dbMessages = chatDbHelper?.getMessages(contactId) ?: emptyList()
                
                if (dbMessages.isNotEmpty()) {
                    dbMessages.forEach { msg ->
                        list.add(
                            ChatMessage(
                                type = if (msg.isFromUser) "sent" else "received",
                                name = if (msg.isFromUser) "" else chatName,
                                avatar = if (msg.isFromUser) "" else chatAvatar,
                                text = msg.content
                            )
                        )
                    }
                } else {
                    list.add(ChatMessage("time", text = "今天 10:00"))
                }
            } else {
                list.addAll(mockChatMessages[chatId] ?: listOf(
                    ChatMessage("time", text = "今天 10:00"),
                    ChatMessage("received", chatName, chatAvatar, "你好！"),
                    ChatMessage("sent", text = "你好，有什么事吗？"),
                ))
            }
        }
    }

    val isGroup = false
    val memberCount = 0
    val displayTitle = chatName

    var inputText by remember { mutableStateOf(TextFieldValue("")) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    BackHandler { onBack() }

    Column(modifier = Modifier.fillMaxSize().background(WeTheme.Background).statusBarsPadding()) {
        // Header
        Box(modifier = Modifier.fillMaxWidth().height(50.dp).background(WeTheme.Background), contentAlignment = Alignment.Center) {
            WeIcon("ic_nav_back", "‹", modifier = Modifier.align(Alignment.CenterStart).padding(start = 12.dp).size(24.dp).clickable { onBack() }, tint = WeTheme.TextPrimary)
            Text(displayTitle, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, maxLines = 1, color = WeTheme.TextPrimary)
            Text("···", modifier = Modifier.align(Alignment.CenterEnd).padding(end = 12.dp).clickable { onSettingsClick() }, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = WeTheme.TextPrimary)
        }
        Divider(color = WeTheme.Separator, thickness = 0.5.dp)

        // Messages
        LazyColumn(
            modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            items(messages.size) { idx ->
                val msg = messages[idx]
                when (msg.type) {
                    "time" -> {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text(msg.text, fontSize = 12.sp, color = WeTheme.TextHint)
                        }
                    }
                    "sent" -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(1f, fill = false).widthIn(max = 260.dp)) {
                                Box(
                                    modifier = Modifier.background(WeTheme.BubbleSent, RoundedCornerShape(4.dp)).padding(12.dp, 10.dp)
                                ) { Text(msg.text, fontSize = 16.sp, lineHeight = 24.sp, color = WeTheme.TextPrimary) }
                            }
                            Spacer(Modifier.width(10.dp))
                            // 发送消息的头像使用用户头像
                            UserAvatarImage(
                                avatarPath = avatarPath,
                                size = 40.dp,
                                cornerRadius = 4.dp,
                                defaultEmoji = "🐱",
                                defaultEmojiSize = 20.sp
                            )
                        }
                    }
                    "received" -> {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            // 显示联系人真实头像
                            if (contactAvatarPath != null) {
                                val bitmap = remember(contactAvatarPath) { BitmapFactory.decodeFile(contactAvatarPath) }
                                if (bitmap != null) {
                                    Image(
                                        bitmap = bitmap.asImageBitmap(),
                                        contentDescription = "头像",
                                        modifier = Modifier.size(40.dp).clip(RoundedCornerShape(4.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFFF5F5F5)), contentAlignment = Alignment.Center) {
                                        Text(msg.avatar.ifEmpty { "👤" }, fontSize = 20.sp)
                                    }
                                }
                            } else {
                                Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFFF5F5F5)), contentAlignment = Alignment.Center) {
                                    Text(msg.avatar.ifEmpty { "👤" }, fontSize = 20.sp)
                                }
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f, fill = false).widthIn(max = 260.dp)) {
                                if (isGroup) {
                                    Text(msg.name.ifEmpty { chatName }, fontSize = 11.sp, color = WeTheme.TextHint)
                                    Spacer(Modifier.height(2.dp))
                                }
                                Box(
                                    modifier = Modifier.background(WeTheme.BubbleReceived, RoundedCornerShape(4.dp)).padding(12.dp, 10.dp)
                                ) { Text(msg.text, fontSize = 16.sp, lineHeight = 24.sp, color = WeTheme.TextPrimary) }
                            }
                        }
                    }
                }
            }
        }

        // Input bar
        Divider(color = WeTheme.Separator, thickness = 0.5.dp)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (WeTheme.isDark) Color(0xFF191919) else Color(0xFFF7F7F7))
                .imePadding()
                .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(10.dp, 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
            WeIcon("ic_chat_voice", "🎙️", modifier = Modifier.size(28.dp), tint = WeTheme.TextPrimary)
            BasicTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier.weight(1f).height(36.dp).background(WeTheme.BackgroundCell, RoundedCornerShape(4.dp)).padding(8.dp, 8.dp),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 16.sp, color = WeTheme.TextPrimary),
                singleLine = true
            )
            WeIcon("ic_chat_emoji", "😊", modifier = Modifier.size(28.dp), tint = WeTheme.TextPrimary)
            
             val hasText = inputText.text.trim().isNotEmpty()
             if (hasText) {
                  Box(
                      modifier = Modifier.height(32.dp).width(56.dp).background(WeTheme.BrandGreen, RoundedCornerShape(4.dp)).clickable {
                          val messageText = inputText.text.trim()
                          messages.add(ChatMessage("sent", text = messageText))
                          
                          // 保存用户消息到数据库
                          var conversationId = chatId
                          if (chatId.startsWith("contact_")) {
                              val contactId = chatId.removePrefix("contact_")
                              chatDbHelper?.addMessage(contactId, true, messageText)
                              
                              // 获取聊天API预设
                              val apiPresetDbHelper = ApiPresetDbHelper(context)
                              val conversation = chatDbHelper?.getConversationById(chatId)
                              val presetId = conversation?.apiPresetId ?: -1L
                              
                              val preset = if (presetId != -1L) {
                                  apiPresetDbHelper.getPresetById(presetId)
                              } else {
                                  // 如果没有指定预设，使用第一个可用的
                                  apiPresetDbHelper.getPresetsByType("chat").firstOrNull()
                              }
                              
                              if (preset != null) {
                                  
                                  // 获取AI角色和用户人设
                                  val aiContact = contactInfo
                                  // 从会话中获取用户人设ID
                                  val conversation = chatDbHelper?.getConversationById(chatId)
                                  val userPersonaId = conversation?.userPersonaId ?: ""
                                  val userContact = contactDbHelper?.getContactById(userPersonaId)
                                  
                                  val aiPersona = aiContact?.persona ?: ""
                                  val userPersona = userContact?.persona ?: ""
                                  
                                  // 获取历史消息
                                  val historyMessages = chatDbHelper?.getMessages(contactId) ?: emptyList()
                                  
                                  // 调用LLM API获取AI回复
                                  scope.launch {
                                      val llmService = LlmApiService()
                                      val aiResponse = llmService.sendChatRequest(
                                          preset = preset,
                                          aiPersona = aiPersona,
                                          userPersona = userPersona,
                                          messages = historyMessages,
                                          userMessage = messageText
                                      )
                                      
                                      if (aiResponse != null) {
                                          // 添加AI回复到消息列表
                                          messages.add(ChatMessage("received", chatName, chatAvatar, aiResponse))
                                          
                                          // 保存AI回复到数据库
                                          chatDbHelper?.addMessage(contactId, false, aiResponse)
                                      } else {
                                          // API调用失败，显示错误消息
                                          messages.add(ChatMessage("received", chatName, chatAvatar, "抱歉，我无法处理您的请求。请检查API设置。"))
                                      }
                                      
                                      // 滚动到底部
                                      listState.animateScrollToItem(messages.size - 1)
                                  }
                              } else {
                                  // 没有配置API预设，显示提示消息
                                  messages.add(ChatMessage("received", chatName, chatAvatar, "请先在设置中配置聊天API预设。"))
                              }
                          }
                          
                          inputText = TextFieldValue("")
                          scope.launch { listState.animateScrollToItem(messages.size - 1) }
                      },
                      contentAlignment = Alignment.Center
                  ) {
                      Text("发送", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                  }
             } else {
                 WeIcon("ic_chat_add", "⊕", modifier = Modifier.size(28.dp).clickable {
                 }, tint = WeTheme.TextPrimary)
             }
            }
        }
    }

    // 自动滚动到底部
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }
}

// ============================================
// 服务页
// ============================================
@Composable
fun ServiceScreen(onBack: () -> Unit) {
    BackHandler { onBack() }

    Column(modifier = Modifier.fillMaxSize().background(WeTheme.Background).statusBarsPadding()) {
        // Header
        Box(modifier = Modifier.fillMaxWidth().height(50.dp).background(WeTheme.Background), contentAlignment = Alignment.Center) {
            WeIcon("ic_nav_back", "‹", modifier = Modifier.align(Alignment.CenterStart).padding(start = 12.dp).size(24.dp).clickable { onBack() }, tint = WeTheme.TextPrimary)
            Text("服务", fontWeight = FontWeight.SemiBold, fontSize = 17.sp, color = WeTheme.TextPrimary)
            Text("···", modifier = Modifier.align(Alignment.CenterEnd).padding(end = 12.dp), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = WeTheme.TextPrimary)
        }
        Divider(color = WeTheme.Separator, thickness = 0.5.dp)

        // 绿色卡片
        Box(
            modifier = Modifier.fillMaxWidth().padding(10.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF07C160), Color(0xFF06AD56))))
                .padding(24.dp, 24.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("⊡", fontSize = 36.sp, color = Color.White)
                    Text("收付款", fontSize = 14.sp, color = Color.White)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("💳", fontSize = 36.sp)
                    Text("钱包", fontSize = 14.sp, color = Color.White)
                    Text("¥888.88", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
                }
            }
        }

        Spacer(Modifier.weight(1f))

        Text(
            "所有服务已关闭，前往设置 ›",
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            fontSize = 14.sp,
            color = WeTheme.TextHint,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

// ============================================
// 联系人详情页
// ============================================
@Composable
fun ContactDetailScreen(
    contactId: String,
    onBack: () -> Unit,
    onSendMessage: (String) -> Unit,
    onEdit: (String) -> Unit,
    onDelete: () -> Unit,
    contactDbHelper: ContactDbHelper
) {
    val context = LocalContext.current
    val contactInfo = remember(contactId) { contactDbHelper.getContactById(contactId) }
    val avatarPath = remember(contactInfo?.avatarFileName) {
        contactInfo?.avatarFileName?.let { contactDbHelper.getAvatarFilePath(it) }
    }
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    BackHandler { onBack() }
    
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("删除联系人") },
            text = { Text("确定要删除 ${contactInfo?.nickname ?: "此联系人"} 吗？") },
            confirmButton = {
                TextButton(onClick = {
                    contactDbHelper.deleteContact(contactId)
                    showDeleteDialog = false
                    onDelete()
                }) {
                    Text("删除", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
    
    Column(modifier = Modifier.fillMaxSize().background(WeTheme.Background).statusBarsPadding()) {
        Box(modifier = Modifier.fillMaxWidth().height(50.dp).background(WeTheme.Background), contentAlignment = Alignment.Center) {
            WeIcon("ic_nav_back", "‹", modifier = Modifier.align(Alignment.CenterStart).padding(start = 12.dp).size(24.dp).clickable { onBack() }, tint = WeTheme.TextPrimary)
            Text("通讯录", fontWeight = FontWeight.SemiBold, fontSize = 17.sp, color = WeTheme.TextPrimary)
            Box(modifier = Modifier.align(Alignment.CenterEnd).padding(end = 12.dp)) {
                Text("···", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = WeTheme.TextPrimary, modifier = Modifier.clickable { showMenu = true })
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.background(Color(0xFF414041)).width(120.dp)
                ) {
                    DropdownMenuItem(
                        text = { Text("编辑", color = Color.White, fontSize = 16.sp) },
                        onClick = {
                            showMenu = false
                            onEdit(contactId)
                        },
                        modifier = Modifier.height(48.dp)
                    )
                    Divider(color = Color(0xFF5A5A5A), thickness = 0.5.dp)
                    DropdownMenuItem(
                        text = { Text("删除", color = Color.Red, fontSize = 16.sp) },
                        onClick = {
                            showMenu = false
                            showDeleteDialog = true
                        },
                        modifier = Modifier.height(48.dp)
                    )
                }
            }
        }
        Divider(color = WeTheme.Separator, thickness = 0.5.dp)
        
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            // 头像和基本信息
            Column(modifier = Modifier.fillMaxWidth().background(WeTheme.BackgroundCell).padding(20.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    // 显示真实头像或默认emoji
                    if (avatarPath != null) {
                        val bitmap = remember(avatarPath) { BitmapFactory.decodeFile(avatarPath) }
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "头像",
                                modifier = Modifier.size(64.dp).clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier.size(64.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFF5F5DC)),
                                contentAlignment = Alignment.Center
                            ) { Text(contactInfo?.persona?.firstOrNull()?.toString() ?: "👤", fontSize = 36.sp) }
                        }
                    } else {
                        Box(
                            modifier = Modifier.size(64.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFF5F5DC)),
                            contentAlignment = Alignment.Center
                        ) { Text(contactInfo?.persona?.firstOrNull()?.toString() ?: "👤", fontSize = 36.sp) }
                    }
                    
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(
                            contactInfo?.nickname ?: "未知联系人",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = WeTheme.TextPrimary
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "微信号：${contactInfo?.wechatId ?: "未设置"}",
                            fontSize = 14.sp,
                            color = WeTheme.TextSecondary
                        )
                        if (!contactInfo?.region.isNullOrEmpty()) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "地区：${contactInfo?.region}",
                                fontSize = 14.sp,
                                color = WeTheme.TextSecondary
                            )
                        }
                    }
                }
            }
            
            // 人设信息
            if (!contactInfo?.persona.isNullOrEmpty()) {
                Spacer(Modifier.height(8.dp))
                Column(modifier = Modifier.fillMaxWidth().background(WeTheme.BackgroundCell).padding(16.dp)) {
                    Text("人设信息", fontSize = 13.sp, color = WeTheme.TextSecondary, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        contactInfo?.persona ?: "",
                        fontSize = 15.sp,
                        color = WeTheme.TextPrimary,
                        lineHeight = 22.sp
                    )
                }
            }
            
            Spacer(Modifier.height(8.dp))
            
            // 操作按钮
            Column(modifier = Modifier.fillMaxWidth().background(WeTheme.BackgroundCell)) {
                Box(modifier = Modifier.fillMaxWidth().clickable {
                    onSendMessage("contact_${contactInfo?.id ?: contactId}")
                }.padding(16.dp), contentAlignment = Alignment.Center) {
                    Text("发消息", fontSize = 16.sp, color = WeTheme.TextPrimary)
                }
                Divider(color = WeTheme.Separator, thickness = 0.5.dp, modifier = Modifier.padding(start = 16.dp))
                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    Text("音视频通话", fontSize = 16.sp, color = WeTheme.TextPrimary)
                }
            }
        }
    }
}

// ============================================
// 添加联系人页面
// ============================================
@Composable
fun AddContactScreen(
    onBack: () -> Unit,
    onContactAdded: () -> Unit,
    contactDbHelper: ContactDbHelper
) {
    var nickname by remember { mutableStateOf("") }
    var wechatId by remember { mutableStateOf("") }
    var region by remember { mutableStateOf("") }
    var persona by remember { mutableStateOf("") }
    var avatarUri by remember { mutableStateOf<Uri?>(null) }
    
    val context = LocalContext.current
    val avatarLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        avatarUri = uri
    }
    
    BackHandler { onBack() }
    
    Column(modifier = Modifier.fillMaxSize().background(WeTheme.Background).statusBarsPadding()) {
        Box(modifier = Modifier.fillMaxWidth().height(50.dp).background(WeTheme.Background), contentAlignment = Alignment.Center) {
            WeIcon("ic_nav_back", "‹", modifier = Modifier.align(Alignment.CenterStart).padding(start = 12.dp).size(24.dp).clickable { onBack() }, tint = WeTheme.TextPrimary)
            Text("添加朋友", fontWeight = FontWeight.SemiBold, fontSize = 17.sp, color = WeTheme.TextPrimary)
        }
        Divider(color = WeTheme.Separator, thickness = 0.5.dp)
        
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
            // 头像选择区域
            Text("头像", fontSize = 14.sp, color = WeTheme.TextSecondary)
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier.fillMaxWidth().background(WeTheme.BackgroundCell, RoundedCornerShape(8.dp)).padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier.size(80.dp).clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFF5F5F5))
                            .clickable { avatarLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        if (avatarUri != null) {
                            val bitmap = remember(avatarUri) {
                                try {
                                    context.contentResolver.openInputStream(avatarUri!!)?.use {
                                        BitmapFactory.decodeStream(it)
                                    }
                                } catch (e: Exception) {
                                    null
                                }
                            }
                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "头像",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Text("📷", fontSize = 32.sp)
                            }
                        } else {
                            Text("📷", fontSize = 32.sp)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("点击选择头像", fontSize = 12.sp, color = WeTheme.TextHint)
                }
            }
            
            Spacer(Modifier.height(16.dp))
            Text("昵称 *", fontSize = 14.sp, color = WeTheme.TextSecondary)
            Spacer(Modifier.height(8.dp))
            BasicTextField(
                value = nickname,
                onValueChange = { nickname = it },
                modifier = Modifier.fillMaxWidth().background(WeTheme.BackgroundCell, RoundedCornerShape(8.dp)).padding(12.dp),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 16.sp, color = WeTheme.TextPrimary),
                decorationBox = { innerTextField ->
                    Box {
                        if (nickname.isEmpty()) {
                            Text("请输入昵称", fontSize = 16.sp, color = WeTheme.TextHint)
                        }
                        innerTextField()
                    }
                }
            )
            
            Spacer(Modifier.height(16.dp))
            Text("微信号", fontSize = 14.sp, color = WeTheme.TextSecondary)
            Spacer(Modifier.height(8.dp))
            BasicTextField(
                value = wechatId,
                onValueChange = { wechatId = it },
                modifier = Modifier.fillMaxWidth().background(WeTheme.BackgroundCell, RoundedCornerShape(8.dp)).padding(12.dp),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 16.sp, color = WeTheme.TextPrimary),
                decorationBox = { innerTextField ->
                    Box {
                        if (wechatId.isEmpty()) {
                            Text("请输入微信号（选填）", fontSize = 16.sp, color = WeTheme.TextHint)
                        }
                        innerTextField()
                    }
                }
            )
            
            Spacer(Modifier.height(16.dp))
            Text("地区", fontSize = 14.sp, color = WeTheme.TextSecondary)
            Spacer(Modifier.height(8.dp))
            BasicTextField(
                value = region,
                onValueChange = { region = it },
                modifier = Modifier.fillMaxWidth().background(WeTheme.BackgroundCell, RoundedCornerShape(8.dp)).padding(12.dp),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 16.sp, color = WeTheme.TextPrimary),
                decorationBox = { innerTextField ->
                    Box {
                        if (region.isEmpty()) {
                            Text("例如：北京 海淀", fontSize = 16.sp, color = WeTheme.TextHint)
                        }
                        innerTextField()
                    }
                }
            )
            
            Spacer(Modifier.height(16.dp))
            Text("人设信息 *", fontSize = 14.sp, color = WeTheme.TextSecondary)
            Spacer(Modifier.height(8.dp))
            BasicTextField(
                value = persona,
                onValueChange = { persona = it },
                modifier = Modifier.fillMaxWidth().height(100.dp).background(WeTheme.BackgroundCell, RoundedCornerShape(8.dp)).padding(12.dp),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 16.sp, color = WeTheme.TextPrimary, lineHeight = 22.sp),
                decorationBox = { innerTextField ->
                    Box {
                        if (persona.isEmpty()) {
                            Text("描述这个联系人的性格、职业等信息", fontSize = 16.sp, color = WeTheme.TextHint)
                        }
                        innerTextField()
                    }
                }
            )
            
            Spacer(Modifier.height(32.dp))
            Box(
                modifier = Modifier.fillMaxWidth().height(48.dp).background(
                    if (nickname.isNotBlank() && persona.isNotBlank()) WeTheme.BrandGreen else Color(0xFFCCCCCC),
                    RoundedCornerShape(8.dp)
                ).clickable(enabled = nickname.isNotBlank() && persona.isNotBlank()) {
                    if (nickname.isNotBlank() && persona.isNotBlank()) {
                        contactDbHelper.addContact(nickname, wechatId, region, persona, avatarUri)
                        onContactAdded()
                    }
                },
                contentAlignment = Alignment.Center
            ) {
                Text("添加", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

// ============================================
// 编辑联系人页面
// ============================================
@Composable
fun EditContactScreen(
    contactId: String,
    onBack: () -> Unit,
    onContactUpdated: () -> Unit,
    contactDbHelper: ContactDbHelper
) {
    val context = LocalContext.current
    val contactInfo = remember(contactId) { contactDbHelper.getContactById(contactId) }
    
    var nickname by remember { mutableStateOf(contactInfo?.nickname ?: "") }
    var wechatId by remember { mutableStateOf(contactInfo?.wechatId ?: "") }
    var region by remember { mutableStateOf(contactInfo?.region ?: "") }
    var persona by remember { mutableStateOf(contactInfo?.persona ?: "") }
    var avatarUri by remember { mutableStateOf<Uri?>(null) }
    
    val avatarLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        avatarUri = uri
    }
    
    val currentAvatarPath = remember(contactInfo?.avatarFileName) {
        contactInfo?.avatarFileName?.let { contactDbHelper.getAvatarFilePath(it) }
    }
    
    BackHandler { onBack() }
    
    Column(modifier = Modifier.fillMaxSize().background(WeTheme.Background).statusBarsPadding()) {
        Box(modifier = Modifier.fillMaxWidth().height(50.dp).background(WeTheme.Background), contentAlignment = Alignment.Center) {
            WeIcon("ic_nav_back", "‹", modifier = Modifier.align(Alignment.CenterStart).padding(start = 12.dp).size(24.dp).clickable { onBack() }, tint = WeTheme.TextPrimary)
            Text("编辑联系人", fontWeight = FontWeight.SemiBold, fontSize = 17.sp, color = WeTheme.TextPrimary)
        }
        Divider(color = WeTheme.Separator, thickness = 0.5.dp)
        
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
            Text("头像", fontSize = 14.sp, color = WeTheme.TextSecondary)
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier.fillMaxWidth().background(WeTheme.BackgroundCell, RoundedCornerShape(8.dp)).padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier.size(80.dp).clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFF5F5F5))
                            .clickable { avatarLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            avatarUri != null -> {
                                val bitmap = remember(avatarUri) {
                                    try {
                                        context.contentResolver.openInputStream(avatarUri!!)?.use {
                                            BitmapFactory.decodeStream(it)
                                        }
                                    } catch (e: Exception) { null }
                                }
                                if (bitmap != null) {
                                    Image(bitmap = bitmap.asImageBitmap(), contentDescription = "头像",
                                        modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                } else {
                                    Text("📷", fontSize = 32.sp)
                                }
                            }
                            currentAvatarPath != null -> {
                                val bitmap = remember(currentAvatarPath) { BitmapFactory.decodeFile(currentAvatarPath) }
                                if (bitmap != null) {
                                    Image(bitmap = bitmap.asImageBitmap(), contentDescription = "头像",
                                        modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                } else {
                                    Text("📷", fontSize = 32.sp)
                                }
                            }
                            else -> Text("📷", fontSize = 32.sp)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("点击更换头像", fontSize = 12.sp, color = WeTheme.TextHint)
                }
            }
            
            Spacer(Modifier.height(16.dp))
            Text("昵称 *", fontSize = 14.sp, color = WeTheme.TextSecondary)
            Spacer(Modifier.height(8.dp))
            BasicTextField(
                value = nickname,
                onValueChange = { nickname = it },
                modifier = Modifier.fillMaxWidth().background(WeTheme.BackgroundCell, RoundedCornerShape(8.dp)).padding(12.dp),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 16.sp, color = WeTheme.TextPrimary),
                decorationBox = { innerTextField ->
                    Box {
                        if (nickname.isEmpty()) Text("请输入昵称", fontSize = 16.sp, color = WeTheme.TextHint)
                        innerTextField()
                    }
                }
            )
            
            Spacer(Modifier.height(16.dp))
            Text("微信号", fontSize = 14.sp, color = WeTheme.TextSecondary)
            Spacer(Modifier.height(8.dp))
            BasicTextField(
                value = wechatId,
                onValueChange = { wechatId = it },
                modifier = Modifier.fillMaxWidth().background(WeTheme.BackgroundCell, RoundedCornerShape(8.dp)).padding(12.dp),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 16.sp, color = WeTheme.TextPrimary),
                decorationBox = { innerTextField ->
                    Box {
                        if (wechatId.isEmpty()) Text("请输入微信号（选填）", fontSize = 16.sp, color = WeTheme.TextHint)
                        innerTextField()
                    }
                }
            )
            
            Spacer(Modifier.height(16.dp))
            Text("地区", fontSize = 14.sp, color = WeTheme.TextSecondary)
            Spacer(Modifier.height(8.dp))
            BasicTextField(
                value = region,
                onValueChange = { region = it },
                modifier = Modifier.fillMaxWidth().background(WeTheme.BackgroundCell, RoundedCornerShape(8.dp)).padding(12.dp),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 16.sp, color = WeTheme.TextPrimary),
                decorationBox = { innerTextField ->
                    Box {
                        if (region.isEmpty()) Text("例如：北京 海淀", fontSize = 16.sp, color = WeTheme.TextHint)
                        innerTextField()
                    }
                }
            )
            
            Spacer(Modifier.height(16.dp))
            Text("人设信息 *", fontSize = 14.sp, color = WeTheme.TextSecondary)
            Spacer(Modifier.height(8.dp))
            BasicTextField(
                value = persona,
                onValueChange = { persona = it },
                modifier = Modifier.fillMaxWidth().height(100.dp).background(WeTheme.BackgroundCell, RoundedCornerShape(8.dp)).padding(12.dp),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 16.sp, color = WeTheme.TextPrimary, lineHeight = 22.sp),
                decorationBox = { innerTextField ->
                    Box {
                        if (persona.isEmpty()) Text("描述这个联系人的性格、职业等信息", fontSize = 16.sp, color = WeTheme.TextHint)
                        innerTextField()
                    }
                }
            )
            
            Spacer(Modifier.height(32.dp))
            Box(
                modifier = Modifier.fillMaxWidth().height(48.dp).background(
                    if (nickname.isNotBlank() && persona.isNotBlank()) WeTheme.BrandGreen else Color(0xFFCCCCCC),
                    RoundedCornerShape(8.dp)
                ).clickable(enabled = nickname.isNotBlank() && persona.isNotBlank()) {
                    if (nickname.isNotBlank() && persona.isNotBlank()) {
                        contactDbHelper.updateContact(contactId, nickname, wechatId, region, persona, avatarUri)
                        onContactUpdated()
                    }
                },
                contentAlignment = Alignment.Center
            ) {
                Text("保存", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
    

}
