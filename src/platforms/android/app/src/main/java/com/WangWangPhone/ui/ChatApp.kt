
package com.WangWangPhone.ui

import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val TabTextNormal: Color @Composable get() = if (isDark) Color(0xFF555555) else Color(0xFF191919) // 微信底部Tab未选中是黑色而不是灰色
    
    val BubbleSent: Color @Composable get() = if (isDark) Color(0xFF2EA260) else Color(0xFF95EC69)
    val BubbleReceived: Color @Composable get() = if (isDark) Color(0xFF2C2C2C) else Color(0xFFFFFFFF)
    
    val SearchBarBg: Color @Composable get() = if (isDark) Color(0xFF202020) else Color(0xFFFFFFFF)
}

// ============================================
// 资源加载辅助
// ============================================
// 强制使用 SVG 图标，不再回退到 Emoji
@Composable
fun WeIcon(
    name: String,
    fallback: String, // 保留参数但忽略，强制加载资源
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified
) {
    val context = LocalContext.current
    // 尝试查找 drawable 资源
    // 1. 尝试直接按 name 查找 (例如 "nav_chat_selected")
    // 2. 如果 name 包含中文 (例如 "导航栏_已选中_微信.svg")，需要先过滤后缀
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
                // 不应用tint，保留图标原始颜色（如selected状态的绿色）
                Image(
                    painter = painterResource(id = resId),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else {
            // [DEBUG MODE] 只有在资源找不到时才显示红色占位符，方便调试
            // 生产环境应显示透明或默认图标
            // 此时显示 fallback emoji 也不失为一种调试手段，看看到底缺了啥，但用户要求强制SVG，所以这里只显示红框
            // 为了避免 UI 崩溃，我们显示一个细微的红框表示缺失
            Box(modifier = Modifier.fillMaxSize().border(1.dp, Color.Red)) {
                 Text("MISSING", fontSize = 6.sp, color = Color.Red, modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

// ============================================
// 数据模型
// ============================================
data class Conversation(
    val id: String,
    val name: String,
    val avatar: String,
    val lastMsg: String,
    val time: String,
    val unread: Int = 0,
    val muted: Boolean = false,
    val iconBg: Color = Color(0xFF4CAF50)
)

data class ContactGroup(val id: String, val name: String, val icon: String, val color: Color)
data class Contact(val id: String, val name: String, val avatar: String, val letter: String = "")
data class Moment(val id: String, val name: String, val avatar: String, val content: String, val time: String, val likes: List<String>, val comments: List<Pair<String, String>>)
data class ChatMessage(val type: String, val name: String = "", val avatar: String = "", val text: String = "")

// ============================================
// 模拟数据
// ============================================
val mockConversations = listOf(
    Conversation("c1", "文件传输助手", "📁", "你好，这是一条测试消息", "12:51", iconBg = Color(0xFFFF9800)),
    Conversation("c2", "小明", "😊", "[语音] 14\"", "11:38", unread = 2, iconBg = Color(0xFF4CAF50)),
    Conversation("c3", "工作群", "👥", "张三: 收到，谢谢大家的配合", "周四", muted = true, iconBg = Color(0xFF2196F3)),
    Conversation("c4", "家人群", "🏠", "妈妈: [链接] 今日菜谱推荐...", "周三", iconBg = Color(0xFFE91E63)),
    Conversation("c5", "同学群", "🎓", "李华: 科目一快考完了", "1月30日", iconBg = Color(0xFF9C27B0)),
    Conversation("c6", "技术交流群", "💻", "王工: 新版本已经部署上线", "12月2日", muted = true, iconBg = Color(0xFF607D8B)),
    Conversation("c7", "公众号", "📰", "[3条] 今日科技资讯速览...", "16:37", unread = 3, iconBg = Color(0xFF1976D2)),
    Conversation("c8", "服务号", "🔔", "[5条通知] 您的快递已到达...", "16:30", unread = 5, iconBg = Color(0xFFD32F2F)),
    Conversation("c9", "技术攻关群", "🔧", "应该就好了", "16:11", iconBg = Color(0xFF795548)),
)

val mockContactGroups = listOf(
    ContactGroup("g1", "新的朋友", "👤", Color(0xFFFF9800)),
    ContactGroup("g2", "仅聊天的朋友", "👤", Color(0xFFFF9800)),
    ContactGroup("g3", "群聊", "👥", Color(0xFF4CAF50)),
    ContactGroup("g4", "标签", "🏷️", Color(0xFF2196F3)),
    ContactGroup("g5", "公众号", "📰", Color(0xFF1976D2)),
    ContactGroup("g6", "服务号", "🔔", Color(0xFFD32F2F)),
)

val mockStarred = listOf(
    Contact("u1", "小明", "😊"),
    Contact("u2", "小红", "🌸"),
)

val mockContactList = listOf(
    Contact("u3", "阿杰", "🧑", "A"),
    Contact("u4", "陈伟", "👨", "C"),
    Contact("u5", "大卫", "🧔", "D"),
    Contact("u6", "方琳", "👩", "F"),
    Contact("u7", "何志强", "👨‍💼", "H"),
    Contact("u8", "李华", "🧑‍🎓", "L"),
    Contact("u9", "马丽", "👩‍🦰", "M"),
    Contact("u1b", "小明", "😊", "X"),
    Contact("u2b", "小红", "🌸", "X"),
    Contact("u10", "张伟", "👨‍🔧", "Z"),
    Contact("u11", "赵敏", "👩‍🏫", "Z"),
)

val mockMoments = listOf(
    Moment("m1", "小明", "😊", "今天天气真好，出去走走 🌞", "1分钟前", listOf("小红", "李华"), listOf("小红" to "确实不错！")),
    Moment("m2", "李华", "🧑‍🎓", "终于把项目做完了，庆祝一下 🎉", "30分钟前", listOf("小明", "张伟", "阿杰"), emptyList()),
    Moment("m3", "小红", "🌸", "分享一首好听的歌曲，推荐给大家～", "2小时前", listOf("小明"), listOf("小明" to "什么歌？", "小红" to "周杰伦的新专辑")),
    Moment("m4", "张伟", "👨‍🔧", "周末去爬山，风景超美！大家有空可以一起来", "昨天", listOf("小红", "李华", "马丽", "陈伟"), listOf("马丽" to "哪个山？下次带我！")),
)

val mockChatMessages = mapOf(
    "c1" to listOf(
        ChatMessage("time", text = "昨天 16:04"),
        ChatMessage("received", "文件传输助手", "📁", "你好，欢迎使用文件传输助手"),
        ChatMessage("sent", text = "你好，这是一条测试消息"),
    ),
    "c2" to listOf(
        ChatMessage("time", text = "昨天 22:27"),
        ChatMessage("received", "小明", "😊", "明天下午有空吗？一起去打球"),
        ChatMessage("sent", text = "可以啊，几点？"),
        ChatMessage("received", "小明", "😊", "下午三点吧"),
        ChatMessage("sent", text = "好的，到时候见！"),
        ChatMessage("time", text = "今天 11:30"),
        ChatMessage("received", "小明", "😊", "[语音] 14\""),
    ),
    "c3" to listOf(
        ChatMessage("time", text = "昨天 16:04"),
        ChatMessage("received", "张三", "👨", "大家好，关于项目进度的问题"),
        ChatMessage("received", "李四", "🧑", "我这边已经完成了80%"),
        ChatMessage("sent", text = "收到"),
        ChatMessage("time", text = "昨天 22:27"),
        ChatMessage("received", "王五", "👨‍💼", "简单三步：先做设计、再写代码、最后测试"),
        ChatMessage("received", "赵六", "🧔", "何意味"),
        ChatMessage("received", "张三", "👨", "收到，谢谢大家的配合"),
    ),
    "c4" to listOf(
        ChatMessage("time", text = "周三 19:00"),
        ChatMessage("received", "妈妈", "👩", "今天做了你最爱吃的红烧肉"),
        ChatMessage("sent", text = "太棒了！我明天回去"),
        ChatMessage("received", "爸爸", "👨", "路上注意安全"),
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

    BackHandler {
        when (currentView) {
            "chat-detail" -> { currentView = "main"; currentChatId = null }
            "service" -> { currentView = "main"; currentTab = "me" }
            else -> onClose()
        }
    }

    when (currentView) {
        "chat-detail" -> ChatDetailScreen(
            chatId = currentChatId ?: "",
            onBack = { currentView = "main"; currentChatId = null }
        )
        "service" -> ServiceScreen(
            onBack = { currentView = "main"; currentTab = "me" }
        )
        else -> ChatMainScreen(
            currentTab = currentTab,
            onTabChange = { currentTab = it },
            onClose = onClose,
            onOpenChat = { id -> currentChatId = id; currentView = "chat-detail" },
            onOpenService = { currentView = "service" }
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
    onOpenService: () -> Unit
) {
    val titles = mapOf("messages" to "微信", "contacts" to "通讯录", "moments" to "朋友圈", "me" to "我")
    val bgColor = WeTheme.Background

    Column(modifier = Modifier.fillMaxSize().background(bgColor).statusBarsPadding()) {
        // Content (权重为1，撑开中间部分)
        Box(modifier = Modifier.weight(1f)) {
            when (currentTab) {
                "messages" -> {
                    Column {
                        // 微信首页顶部只有 微信(n) 标题，加号和搜索，无返回键
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
                            showAdd = true // 通讯录页显示加好友
                        )
                        ContactsTab(onOpenChat)
                    }
                }
                "moments" -> {
                    Column {
                        WeChatHeader(
                            title = titles[currentTab] ?: "",
                            onClose = onClose,
                            showBack = false
                        )
                        MomentsTab()
                    }
                }
                "me" -> {
                    // "我" 页面通常没有Header，或者Header是透明的
                    MeTab(onOpenService = onOpenService, onOpenMoments = { onTabChange("moments") })
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
    showAdd: Boolean = true
) {
    val bgColor = WeTheme.Background
    val textColor = WeTheme.TextPrimary
    
    Box(
        modifier = Modifier.fillMaxWidth().height(50.dp).background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        // 标题始终居中显示
        Text(title, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, color = textColor)

        Row(modifier = Modifier.align(Alignment.CenterEnd).padding(end = 12.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            WeIcon("ic_search", "🔍", modifier = Modifier.size(24.dp), tint = textColor)
            if (showAdd) {
                WeIcon("ic_chat_add", "⊕", modifier = Modifier.size(24.dp), tint = textColor)
            }
        }
    }
}

@Composable
fun ChatTabBar(currentTab: String, onTabChange: (String) -> Unit) {
    val totalUnread = mockConversations.sumOf { it.unread }
    // Tab定义: id, 选中图标, 未选中图标, 标签
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
                    indication = null // 去掉点击时的矩形阴影ripple效果
                ) { onTabChange(item.id) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box {
                    val iconName = if (isActive) item.iconSelected else item.iconNormal
                    // selected图标自带绿色，normal图标需要tint为当前文字颜色
                    WeIcon(iconName, item.fallback, modifier = Modifier.size(28.dp), tint = if (isActive) Color.Unspecified else WeTheme.TabTextNormal)
                    
                    if (item.id == "messages" && totalUnread > 0) {
                        Box(
                            modifier = Modifier.offset(x = 10.dp, y = (-2).dp).background(Color(0xFFFA5151), CircleShape).padding(horizontal = 4.dp).height(16.dp).widthIn(min = 16.dp),
                            contentAlignment = Alignment.Center
                        ) { Text("$totalUnread", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
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
fun MessagesTab(onOpenChat: (String) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(mockConversations) { conv ->
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onOpenChat(conv.id) }.background(WeTheme.BackgroundCell).padding(16.dp, 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar
                Box(
                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(4.dp)).background(conv.iconBg), // 微信头像圆角较小
                    contentAlignment = Alignment.Center
                ) {
                    Text(conv.avatar, fontSize = 24.sp)
                    if (conv.unread > 0) {
                        Box(
                            modifier = Modifier.align(Alignment.TopEnd).offset(x = 6.dp, y = (-6).dp).background(Color(0xFFFA5151), CircleShape).padding(horizontal = 5.dp),
                            contentAlignment = Alignment.Center
                        ) { Text("${conv.unread}", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                    }
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(conv.name, fontSize = 16.sp, maxLines = 1, color = WeTheme.TextPrimary)
                        Text(conv.time, fontSize = 11.sp, color = WeTheme.TextHint) // 时间字体更小
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(conv.lastMsg, fontSize = 14.sp, color = WeTheme.TextSecondary, maxLines = 1, modifier = Modifier.weight(1f))
                        if (conv.muted) Text("🔇", fontSize = 14.sp, color = WeTheme.TextHint)
                    }
                }
            }
            Divider(color = WeTheme.Separator, thickness = 0.5.dp, modifier = Modifier.padding(start = 76.dp)) // 分割线左对齐文字
        }
    }
}

// ============================================
// Tab2: 通讯录
// ============================================
@Composable
fun ContactsTab(onOpenChat: (String) -> Unit) {
    val scrollState = rememberScrollState()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(scrollState)) {
            // 功能入口
            mockContactGroups.forEach { g ->
                Row(
                    modifier = Modifier.fillMaxWidth().background(WeTheme.BackgroundCell).padding(16.dp, 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(36.dp).clip(RoundedCornerShape(4.dp)).background(g.color),
                        contentAlignment = Alignment.Center
                    ) { Text(g.icon, fontSize = 20.sp, color = Color.White) }
                    Spacer(Modifier.width(16.dp))
                    Text(g.name, fontSize = 16.sp, color = WeTheme.TextPrimary)
                }
                Divider(color = WeTheme.Separator, thickness = 0.5.dp, modifier = Modifier.padding(start = 68.dp))
            }

            // 星标朋友
            if (mockStarred.isNotEmpty()) {
                Text("星标朋友", modifier = Modifier.fillMaxWidth().background(WeTheme.Background).padding(16.dp, 6.dp), fontSize = 12.sp, color = WeTheme.TextSecondary)
                mockStarred.forEach { c -> ContactItemRow(c, onOpenChat) }
            }

            // 按字母分组
            var curLetter = ""
            mockContactList.forEach { c ->
                if (c.letter.isNotEmpty() && c.letter != curLetter) {
                    curLetter = c.letter
                    Text(curLetter, modifier = Modifier.fillMaxWidth().background(WeTheme.Background).padding(16.dp, 6.dp), fontSize = 12.sp, color = WeTheme.TextSecondary)
                }
                ContactItemRow(c, onOpenChat)
            }
        }

        // 右侧字母索引
        val letters = listOf("↑", "☆") + mockContactList.map { it.letter }.filter { it.isNotEmpty() }.distinct() + listOf("#")
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
fun ContactItemRow(contact: Contact, onOpenChat: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().background(WeTheme.BackgroundCell).clickable {
            val conv = mockConversations.find { it.name == contact.name }
            onOpenChat(conv?.id ?: "u_${contact.id}")
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
// Tab3: 朋友圈
// ============================================
@Composable
fun MomentsTab() {
    LazyColumn(modifier = Modifier.fillMaxSize().background(Color.White)) {
        // 封面
        item {
            Box(
                modifier = Modifier.fillMaxWidth().height(300.dp)
                    .background(Brush.linearGradient(listOf(Color(0xFF667EEA), Color(0xFF764BA2)))),
                contentAlignment = Alignment.BottomEnd
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("我的昵称", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    Box(
                        modifier = Modifier.size(64.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFFF5F5DC)).border(2.dp, Color.White, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) { Text("🐱", fontSize = 32.sp) }
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
fun MeTab(onOpenService: () -> Unit, onOpenMoments: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        // 个人信息卡片
        Row(
            modifier = Modifier.fillMaxWidth().background(Color.White).padding(24.dp, 24.dp, 16.dp, 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(64.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFFF5F5DC)),
                contentAlignment = Alignment.Center
            ) { Text("🐱", fontSize = 32.sp) }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("我的昵称", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text("账号：WangWang_User", fontSize = 14.sp, color = Color(0xFF888888))
            }
            Text("⊞ ›", fontSize = 20.sp, color = Color(0xFF888888))
        }
        Row(modifier = Modifier.fillMaxWidth().background(Color.White).padding(8.dp, 0.dp, 16.dp, 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(modifier = Modifier.border(0.5.dp, Color(0xFFE0E0E0), RoundedCornerShape(16.dp)).padding(horizontal = 12.dp, vertical = 4.dp)) {
                Text("+ 状态", fontSize = 13.sp, color = Color(0xFF888888))
            }
            Box(modifier = Modifier.border(0.5.dp, Color(0xFFE0E0E0), RoundedCornerShape(16.dp)).padding(horizontal = 12.dp, vertical = 4.dp)) {
                Text("👤👤👤 等40个朋友 ●", fontSize = 13.sp, color = Color(0xFF888888))
            }
        }

        // 菜单
        data class MenuItem(val icon: String, val label: String, val action: (() -> Unit)? = null)
        val menuGroups = listOf(
            listOf(MenuItem("✅", "服务", onOpenService)),
            listOf(
                MenuItem("⭐", "收藏"),
                MenuItem("🖼️", "朋友圈", onOpenMoments),
                MenuItem("📺", "视频号和公众号"),
                MenuItem("🛒", "订单与卡包"),
                MenuItem("😊", "表情"),
            ),
            listOf(MenuItem("⚙️", "设置")),
        )

        menuGroups.forEach { group ->
            Spacer(Modifier.height(8.dp))
            group.forEachIndexed { idx, item ->
                Row(
                    modifier = Modifier.fillMaxWidth().background(Color.White)
                        .then(if (item.action != null) Modifier.clickable { item.action!!() } else Modifier)
                        .padding(14.dp, 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(item.icon, fontSize = 20.sp, modifier = Modifier.width(24.dp))
                    Spacer(Modifier.width(14.dp))
                    Text(item.label, fontSize = 16.sp, modifier = Modifier.weight(1f))
                    Text("›", fontSize = 14.sp, color = Color(0xFFC0C0C0))
                }
                if (idx < group.size - 1) Divider(color = Color(0xFFECECEC), thickness = 0.5.dp)
            }
        }
    }
}

// ============================================
// 聊天详情页
// ============================================
@Composable
fun ChatDetailScreen(chatId: String, onBack: () -> Unit) {
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
            verticalArrangement = Arrangement.spacedBy(16.dp), // 增加消息间距
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
                                    modifier = Modifier.background(WeTheme.BubbleSent, RoundedCornerShape(4.dp)).padding(12.dp, 10.dp) // 圆角更小
                                ) { Text(msg.text, fontSize = 16.sp, lineHeight = 24.sp, color = WeTheme.TextPrimary) }
                            }
                            Spacer(Modifier.width(10.dp))
                            Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFFF5F5F5)), contentAlignment = Alignment.Center) {
                                Text("🐱", fontSize = 20.sp)
                            }
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
                // 发送按钮
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
                     // 更多菜单
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

        // 底部提示
        Text(
            "所有服务已关闭，前往设置 ›",
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            fontSize = 14.sp,
            color = WeTheme.TextHint,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}