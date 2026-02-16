
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
import kotlinx.coroutines.launch
import java.io.File

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
// 模拟数据（仅保留会话列表）
// ============================================
val mockConversations = listOf(
    Conversation("c1", "文件传输助手", "📁", "你好，这是一条测试消息", "12:51", iconBg = Color(0xFFFF9800)),
)

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

    // 用户资料状态 - 在顶层管理，传递给子组件
    val context = LocalContext.current
    val profileDbHelper = remember { UserProfileDbHelper(context) }
    val contactDbHelper = remember { ContactDbHelper(context) }
    var userNickname by remember { mutableStateOf(profileDbHelper.getUserProfile().nickname) }
    var userSignature by remember { mutableStateOf(profileDbHelper.getUserProfile().signature) }
    var avatarPath by remember { mutableStateOf(profileDbHelper.getAvatarFilePath()) }
    var coverPath by remember { mutableStateOf(profileDbHelper.getCoverFilePath()) }
    var contactsRefreshTrigger by remember { mutableStateOf(0) }

    BackHandler {
        when (currentView) {
            "chat-detail" -> { currentView = "main"; currentChatId = null }
            "contact-detail" -> { currentView = "main"; currentContactId = null }
            "add-contact" -> { currentView = "main"; currentTab = "contacts" }
            "service" -> { currentView = "main"; currentTab = "me" }
            else -> onClose()
        }
    }

    when (currentView) {
        "chat-detail" -> ChatDetailScreen(
            chatId = currentChatId ?: "",
            onBack = { currentView = "main"; currentChatId = null },
            avatarPath = avatarPath
        )
        "contact-detail" -> ContactDetailScreen(
            contactId = currentContactId ?: "",
            onBack = { currentView = "main"; currentContactId = null },
            onSendMessage = { id -> currentChatId = id; currentView = "chat-detail" },
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
        "service" -> ServiceScreen(
            onBack = { currentView = "main"; currentTab = "me" }
        )
        else -> ChatMainScreen(
            currentTab = currentTab,
            onTabChange = { currentTab = it },
            onClose = onClose,
            onOpenChat = { id -> currentChatId = id; currentView = "chat-detail" },
            onOpenContact = { id -> currentContactId = id; currentView = "contact-detail" },
            onAddContact = { currentView = "add-contact" },
            onOpenService = { currentView = "service" },
            userNickname = userNickname,
            userSignature = userSignature,
            avatarPath = avatarPath,
            coverPath = coverPath,
            contactDbHelper = contactDbHelper,
            contactsRefreshTrigger = contactsRefreshTrigger,
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
    contactsRefreshTrigger: Int,
    onNicknameChanged: (String) -> Unit,
    onSignatureChanged: (String) -> Unit,
    onAvatarChanged: (Uri) -> Unit,
    onCoverChanged: (Uri) -> Unit
) {
    val titles = mapOf("messages" to "微信", "contacts" to "通讯录", "moments" to "朋友圈", "me" to "我")
    val bgColor = WeTheme.Background

    Column(modifier = Modifier.fillMaxSize().background(bgColor).statusBarsPadding()) {
        // Content (权重为1，撑开中间部分)
        Box(modifier = Modifier.weight(1f)) {
            when (currentTab) {
                "messages" -> {
                    Column {
                        WeChatHeader(
                            title = titles[currentTab] ?: "",
                            onClose = onClose,
                            showBack = false
                        )
                        MessagesTab(onOpenChat)
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
        ChatTabBar(currentTab, onTabChange)
    }
}

@Composable
fun WeChatHeader(
    title: String,
    onClose: () -> Unit,
    showBack: Boolean = false,
    showAdd: Boolean = true,
    onAddClick: (() -> Unit)? = null
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
                                    Text("发起群聊", color = Color.White, fontSize = 16.sp)
                                }
                            },
                            onClick = { showMenu = false },
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
fun ChatTabBar(currentTab: String, onTabChange: (String) -> Unit) {
    val totalUnread = mockConversations.sumOf { it.unread }
    data class TabItem(val id: String, val iconSelected: String, val iconNormal: String, val label: String, val fallback: String)
    
    val tabs = listOf(
        TabItem("messages", "ic_tab_chat_selected", "ic_tab_chat_normal", "微信", "💬"),
        TabItem("contacts", "ic_tab_contacts_selected", "ic_tab_contacts_normal", "通讯录", "👥"),
        TabItem("moments", "ic_tab_discover_selected", "ic_tab_discover_normal", "发现", "🧭"),
        TabItem("me", "ic_tab_me_selected", "ic_tab_me_normal", "我", "👤"),
    )

    val bg = if (WeTheme.isDark) Color(0xFF191919) else Color(0xFFF7F7F7)

    Row(
        modifier = Modifier.fillMaxWidth().height(56.dp).background(bg),
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
fun MessagesTab(onOpenChat: (String) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(mockConversations) { conv ->
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onOpenChat(conv.id) }.background(WeTheme.BackgroundCell).padding(16.dp, 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(52.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier.size(48.dp).clip(RoundedCornerShape(4.dp)).background(conv.iconBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(conv.avatar, fontSize = 24.sp)
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
                val letter = c.nickname.firstOrNull()?.uppercaseChar()?.toString() ?: "#"
                if (letter != curLetter) {
                    curLetter = letter
                    Text(curLetter, modifier = Modifier.fillMaxWidth().background(WeTheme.Background).padding(16.dp, 6.dp), fontSize = 12.sp, color = WeTheme.TextSecondary)
                }
                ContactItemRow(
                    Contact(c.id, c.nickname, c.persona, letter),
                    onOpenContact
                )
            }
        }

        val letters = if (allContacts.isNotEmpty()) {
            allContacts.map { it.nickname.firstOrNull()?.uppercaseChar()?.toString() ?: "#" }.distinct()
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
fun ContactItemRow(contact: Contact, onOpenContact: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().background(WeTheme.BackgroundCell).clickable {
            onOpenContact(contact.id)
        }.padding(16.dp, 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(36.dp).clip(RoundedCornerShape(4.dp)).background(WeTheme.Background),
            contentAlignment = Alignment.Center
        ) { Text(contact.avatar, fontSize = 22.sp) }
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

    LazyColumn(modifier = Modifier.fillMaxSize().background(Color.White)) {
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
                    modifier = Modifier.fillMaxWidth().background(Color.White)
                        .clickable { showSignatureDialog = true }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Text(
                        userSignature,
                        fontSize = 13.sp,
                        color = Color(0xFF999999),
                        maxLines = 1
                    )
                }
            }
        }

        // 动态列表
        items(mockMoments) { m ->
            Column(modifier = Modifier.fillMaxWidth().background(Color.White).padding(12.dp, 12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier.size(40.dp).clip(RoundedCornerShape(6.dp)).background(Color(0xFFF5F5F5)),
                        contentAlignment = Alignment.Center
                    ) { Text(m.avatar, fontSize = 22.sp) }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(m.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF576B95))
                        Spacer(Modifier.height(4.dp))
                        Text(m.content, fontSize = 15.sp, lineHeight = 22.sp)
                        Spacer(Modifier.height(8.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(m.time, fontSize = 12.sp, color = Color(0xFFB2B2B2))
                            Box(
                                modifier = Modifier.background(Color(0xFFF7F7F7), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)
                            ) { Text("··", fontSize = 14.sp, color = Color(0xFF576B95)) }
                        }

                        // 互动区
                        if (m.likes.isNotEmpty() || m.comments.isNotEmpty()) {
                            Spacer(Modifier.height(6.dp))
                            Column(modifier = Modifier.fillMaxWidth().background(Color(0xFFF7F7F7), RoundedCornerShape(4.dp)).padding(6.dp, 6.dp)) {
                                if (m.likes.isNotEmpty()) {
                                    Text("❤️ ${m.likes.joinToString("，")}", fontSize = 13.sp, color = Color(0xFF576B95))
                                }
                                if (m.likes.isNotEmpty() && m.comments.isNotEmpty()) {
                                    Divider(color = Color(0xFFDEDEDE), thickness = 0.5.dp, modifier = Modifier.padding(vertical = 4.dp))
                                }
                                m.comments.forEach { (name, text) ->
                                    Row {
                                        Text(name, fontSize = 13.sp, color = Color(0xFF576B95), fontWeight = FontWeight.Medium)
                                        Text("：$text", fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Divider(color = Color(0xFFECECEC), thickness = 0.5.dp)
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
fun ChatDetailScreen(chatId: String, onBack: () -> Unit, avatarPath: String? = null) {
    val conv = mockConversations.find { it.id == chatId }
    val chatName = conv?.name ?: "聊天"
    val messages = remember {
        mutableStateListOf<ChatMessage>().also {
            it.addAll(mockChatMessages[chatId] ?: listOf(
                ChatMessage("time", text = "今天 10:00"),
                ChatMessage("received", chatName, conv?.avatar ?: "👤", "你好！"),
                ChatMessage("sent", text = "你好，有什么事吗？"),
            ))
        }
    }

    val isGroup = conv != null && (conv.name.contains("群") || messages.filter { it.type == "received" }.map { it.name }.distinct().size > 1)
    val memberCount = if (isGroup) (10..50).random() else 0
    val displayTitle = if (isGroup) "$chatName($memberCount)" else chatName

    var inputText by remember { mutableStateOf(TextFieldValue("")) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    BackHandler { onBack() }

    Column(modifier = Modifier.fillMaxSize().background(WeTheme.Background).statusBarsPadding()) {
        // Header
        Box(modifier = Modifier.fillMaxWidth().height(50.dp).background(WeTheme.Background), contentAlignment = Alignment.Center) {
            WeIcon("ic_nav_back", "‹", modifier = Modifier.align(Alignment.CenterStart).padding(start = 12.dp).size(24.dp).clickable { onBack() }, tint = WeTheme.TextPrimary)
            Text(displayTitle, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, maxLines = 1, color = WeTheme.TextPrimary)
            Text("···", modifier = Modifier.align(Alignment.CenterEnd).padding(end = 12.dp), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = WeTheme.TextPrimary)
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
                            Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFFF5F5F5)), contentAlignment = Alignment.Center) {
                                Text(msg.avatar.ifEmpty { "👤" }, fontSize = 20.sp)
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
        Row(
            modifier = Modifier.fillMaxWidth().background(if (WeTheme.isDark) Color(0xFF191919) else Color(0xFFF7F7F7)).padding(10.dp, 10.dp),
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
                         messages.add(ChatMessage("sent", text = inputText.text.trim()))
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
    contactDbHelper: ContactDbHelper
) {
    val contactInfo = remember(contactId) { contactDbHelper.getContactById(contactId) }
    val contact = contactInfo?.let { Contact(it.id, it.nickname, it.persona, "") } ?: Contact(contactId, "未知联系人", "👤")
    
    BackHandler { onBack() }
    
    Column(modifier = Modifier.fillMaxSize().background(WeTheme.Background).statusBarsPadding()) {
        Box(modifier = Modifier.fillMaxWidth().height(50.dp).background(WeTheme.Background), contentAlignment = Alignment.Center) {
            WeIcon("ic_nav_back", "‹", modifier = Modifier.align(Alignment.CenterStart).padding(start = 12.dp).size(24.dp).clickable { onBack() }, tint = WeTheme.TextPrimary)
            Text("通讯录", fontWeight = FontWeight.SemiBold, fontSize = 17.sp, color = WeTheme.TextPrimary)
            Text("···", modifier = Modifier.align(Alignment.CenterEnd).padding(end = 12.dp), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = WeTheme.TextPrimary)
        }
        Divider(color = WeTheme.Separator, thickness = 0.5.dp)
        
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Column(modifier = Modifier.fillMaxWidth().background(WeTheme.BackgroundCell).padding(16.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    Box(modifier = Modifier.size(64.dp).clip(RoundedCornerShape(8.dp)).background(WeTheme.Background), contentAlignment = Alignment.Center) {
                        Text(contact.avatar, fontSize = 36.sp)
                    }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(contact.name, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = WeTheme.TextPrimary)
                        Spacer(Modifier.height(6.dp))
                        Text("微信号：${contactInfo?.wechatId ?: contact.id}", fontSize = 14.sp, color = WeTheme.TextSecondary)
                        if (!contactInfo?.region.isNullOrEmpty()) {
                            Spacer(Modifier.height(4.dp))
                            Text("地区：${contactInfo?.region}", fontSize = 14.sp, color = WeTheme.TextSecondary)
                        }
                    }
                }
            }
            
            Spacer(Modifier.height(8.dp))
            
            Column(modifier = Modifier.fillMaxWidth().background(WeTheme.BackgroundCell)) {
                Box(modifier = Modifier.fillMaxWidth().clickable {
                    onSendMessage("u_${contact.id}")
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
    var name by remember { mutableStateOf("") }
    var wechatId by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var avatar by remember { mutableStateOf("👤") }
    
    BackHandler { onBack() }
    
    Column(modifier = Modifier.fillMaxSize().background(WeTheme.Background).statusBarsPadding()) {
        Box(modifier = Modifier.fillMaxWidth().height(50.dp).background(WeTheme.Background), contentAlignment = Alignment.Center) {
            WeIcon("ic_nav_back", "‹", modifier = Modifier.align(Alignment.CenterStart).padding(start = 12.dp).size(24.dp).clickable { onBack() }, tint = WeTheme.TextPrimary)
            Text("添加朋友", fontWeight = FontWeight.SemiBold, fontSize = 17.sp, color = WeTheme.TextPrimary)
        }
        Divider(color = WeTheme.Separator, thickness = 0.5.dp)
        
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text("姓名", fontSize = 14.sp, color = WeTheme.TextSecondary)
            Spacer(Modifier.height(8.dp))
            BasicTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth().background(WeTheme.BackgroundCell, RoundedCornerShape(8.dp)).padding(12.dp),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 16.sp, color = WeTheme.TextPrimary)
            )
            
            Spacer(Modifier.height(16.dp))
            Text("微信号", fontSize = 14.sp, color = WeTheme.TextSecondary)
            Spacer(Modifier.height(8.dp))
            BasicTextField(
                value = wechatId,
                onValueChange = { wechatId = it },
                modifier = Modifier.fillMaxWidth().background(WeTheme.BackgroundCell, RoundedCornerShape(8.dp)).padding(12.dp),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 16.sp, color = WeTheme.TextPrimary)
            )
            
            Spacer(Modifier.height(16.dp))
            Text("电话（可选）", fontSize = 14.sp, color = WeTheme.TextSecondary)
            Spacer(Modifier.height(8.dp))
            BasicTextField(
                value = phone,
                onValueChange = { phone = it },
                modifier = Modifier.fillMaxWidth().background(WeTheme.BackgroundCell, RoundedCornerShape(8.dp)).padding(12.dp),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 16.sp, color = WeTheme.TextPrimary)
            )
            
            Spacer(Modifier.height(16.dp))
            Text("头像", fontSize = 14.sp, color = WeTheme.TextSecondary)
            Spacer(Modifier.height(8.dp))
            BasicTextField(
                value = avatar,
                onValueChange = { avatar = it },
                modifier = Modifier.fillMaxWidth().background(WeTheme.BackgroundCell, RoundedCornerShape(8.dp)).padding(12.dp),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 16.sp, color = WeTheme.TextPrimary)
            )
            
            Spacer(Modifier.height(32.dp))
            Box(
                modifier = Modifier.fillMaxWidth().height(48.dp).background(WeTheme.BrandGreen, RoundedCornerShape(8.dp)).clickable {
                    if (name.isNotBlank() && wechatId.isNotBlank()) {
                        contactDbHelper.addContact(name, wechatId, phone, avatar, null)
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