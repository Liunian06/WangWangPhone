
package com.WangWangPhone.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

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
    val titles = mapOf("messages" to "消息", "contacts" to "通讯录", "moments" to "朋友圈", "me" to "我")
    val bgColor = Color(0xFFEDEDED)

    Column(modifier = Modifier.fillMaxSize().background(bgColor).statusBarsPadding()) {
        // Header
        Box(
            modifier = Modifier.fillMaxWidth().height(50.dp).background(bgColor),
            contentAlignment = Alignment.Center
        ) {
            Text("‹", modifier = Modifier.align(Alignment.CenterStart).padding(start = 12.dp).clickable { onClose() }, fontSize = 24.sp)
            Text(titles[currentTab] ?: "", fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
            Row(modifier = Modifier.align(Alignment.CenterEnd).padding(end = 12.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("🔍", fontSize = 20.sp)
                Text("⊕", fontSize = 20.sp)
            }
        }
        Divider(color = Color(0xFFD9D9D9), thickness = 0.5.dp)

        // Content
        Box(modifier = Modifier.weight(1f)) {
            when (currentTab) {
                "messages" -> MessagesTab(onOpenChat)
                "contacts" -> ContactsTab(onOpenChat)
                "moments" -> MomentsTab()
                "me" -> MeTab(onOpenService = onOpenService, onOpenMoments = { onTabChange("moments") })
            }
        }

        // Tab bar
        Divider(color = Color(0xFFD9D9D9), thickness = 0.5.dp)
        ChatTabBar(currentTab, onTabChange)
    }
}

@Composable
fun ChatTabBar(currentTab: String, onTabChange: (String) -> Unit) {
    val totalUnread = mockConversations.sumOf { it.unread }
    val tabs = listOf(
        Triple("messages", "💬", "消息"),
        Triple("contacts", "👥", "通讯录"),
        Triple("moments", "📷", "朋友圈"),
        Triple("me", "👤", "我"),
    )

    Row(
        modifier = Modifier.fillMaxWidth().height(56.dp).background(Color(0xFFF7F7F7)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        tabs.forEach { (id, icon, label) ->
            val isActive = currentTab == id
            Column(
                modifier = Modifier.weight(1f).clickable { onTabChange(id) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box {
                    Text(icon, fontSize = 22.sp)
                    if (id == "messages" && totalUnread > 0) {
                        Box(
                            modifier = Modifier.offset(x = 12.dp, y = (-4).dp).background(Color(0xFFFA5151), CircleShape).padding(horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) { Text("$totalUnread", color = Color.White, fontSize = 10.sp) }
                    }
                }
                Text(label, fontSize = 10.sp, color = if (isActive) Color(0xFF07C160) else Color(0xFF999999))
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
                modifier = Modifier.fillMaxWidth().clickable { onOpenChat(conv.id) }.background(Color.White).padding(12.dp, 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar
                Box(
                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(6.dp)).background(conv.iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Text(conv.avatar, fontSize = 24.sp)
                    if (conv.unread > 0) {
                        Box(
                            modifier = Modifier.align(Alignment.TopEnd).offset(x = 4.dp, y = (-4).dp).background(Color(0xFFFA5151), CircleShape).padding(horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) { Text("${conv.unread}", color = Color.White, fontSize = 10.sp) }
                    }
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(conv.name, fontSize = 16.sp, maxLines = 1)
                        Text(conv.time, fontSize = 12.sp, color = Color(0xFFB2B2B2))
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(conv.lastMsg, fontSize = 14.sp, color = Color(0xFFB2B2B2), maxLines = 1, modifier = Modifier.weight(1f))
                        if (conv.muted) Text("🔇", fontSize = 14.sp, color = Color(0xFFC0C0C0))
                    }
                }
            }
            Divider(color = Color(0xFFECECEC), thickness = 0.5.dp, modifier = Modifier.padding(start = 72.dp))
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
                    modifier = Modifier.fillMaxWidth().background(Color.White).padding(12.dp, 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(40.dp).clip(RoundedCornerShape(6.dp)).background(g.color),
                        contentAlignment = Alignment.Center
                    ) { Text(g.icon, fontSize = 20.sp, color = Color.White) }
                    Spacer(Modifier.width(12.dp))
                    Text(g.name, fontSize = 16.sp)
                }
                Divider(color = Color(0xFFECECEC), thickness = 0.5.dp)
            }

            // 星标朋友
            if (mockStarred.isNotEmpty()) {
                Text("星标朋友", modifier = Modifier.fillMaxWidth().background(Color(0xFFEDEDED)).padding(6.dp, 6.dp, 6.dp, 6.dp).padding(start = 10.dp), fontSize = 13.sp, color = Color(0xFF888888))
                mockStarred.forEach { c -> ContactItemRow(c, onOpenChat) }
            }

            // 按字母分组
            var curLetter = ""
            mockContactList.forEach { c ->
                if (c.letter.isNotEmpty() && c.letter != curLetter) {
                    curLetter = c.letter
                    Text(curLetter, modifier = Modifier.fillMaxWidth().background(Color(0xFFEDEDED)).padding(6.dp, 6.dp, 6.dp, 6.dp).padding(start = 10.dp), fontSize = 13.sp, color = Color(0xFF888888))
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
        modifier = Modifier.fillMaxWidth().background(Color.White).clickable {
            val conv = mockConversations.find { it.name == contact.name }
            onOpenChat(conv?.id ?: "u_${contact.id}")
        }.padding(10.dp, 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(6.dp)).background(Color(0xFFF5F5F5)),
            contentAlignment = Alignment.Center
        ) { Text(contact.avatar, fontSize = 22.sp) }
        Spacer(Modifier.width(12.dp))
        Text(contact.name, fontSize = 16.sp)
    }
    Divider(color = Color(0xFFECECEC), thickness = 0.5.dp)
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

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFEDEDED)).statusBarsPadding()) {
        // Header
        Box(modifier = Modifier.fillMaxWidth().height(50.dp).background(Color(0xFFEDEDED)), contentAlignment = Alignment.Center) {
            Text("‹", modifier = Modifier.align(Alignment.CenterStart).padding(start = 12.dp).clickable { onBack() }, fontSize = 24.sp)
            Text(displayTitle, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, maxLines = 1)
            Text("···", modifier = Modifier.align(Alignment.CenterEnd).padding(end = 12.dp), fontSize = 20.sp)
        }
        Divider(color = Color(0xFFD9D9D9), thickness = 0.5.dp)

        // Messages
        LazyColumn(
            modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 10.dp)
        ) {
            items(messages.size) { idx ->
                val msg = messages[idx]
                when (msg.type) {
                    "time" -> {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text(msg.text, fontSize = 12.sp, color = Color(0xFFB2B2B2))
                        }
                    }
                    "sent" -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(1f, fill = false).widthIn(max = 260.dp)) {
                                Box(
                                    modifier = Modifier.background(Color(0xFF95EC69), RoundedCornerShape(topStart = 6.dp, topEnd = 2.dp, bottomStart = 6.dp, bottomEnd = 6.dp)).padding(10.dp, 10.dp)
                                ) { Text(msg.text, fontSize = 15.sp, lineHeight = 22.sp) }
                            }
                            Spacer(Modifier.width(8.dp))
                            Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(6.dp)).background(Color(0xFFF5F5F5)), contentAlignment = Alignment.Center) {
                                Text("🐱", fontSize = 18.sp)
                            }
                        }
                    }
                    "received" -> {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(6.dp)).background(Color(0xFFF5F5F5)), contentAlignment = Alignment.Center) {
                                Text(msg.avatar.ifEmpty { "👤" }, fontSize = 18.sp)
                            }
                            Spacer(Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f, fill = false).widthIn(max = 260.dp)) {
                                if (isGroup) {
                                    Text(msg.name.ifEmpty { chatName }, fontSize = 12.sp, color = Color(0xFF888888))
                                    Spacer(Modifier.height(2.dp))
                                }
                                Box(
                                    modifier = Modifier.background(Color.White, RoundedCornerShape(topStart = 2.dp, topEnd = 6.dp, bottomStart = 6.dp, bottomEnd = 6.dp)).padding(10.dp, 10.dp)
                                ) { Text(msg.text, fontSize = 15.sp, lineHeight = 22.sp) }
                            }
                        }
                    }
                }
            }
        }

        // Input bar
        Divider(color = Color(0xFFD9D9D9), thickness = 0.5.dp)
        Row(
            modifier = Modifier.fillMaxWidth().background(Color(0xFFF7F7F7)).padding(8.dp, 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("🎙️", fontSize = 24.sp)
            BasicTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier.weight(1f).background(Color.White, RoundedCornerShape(6.dp)).border(0.5.dp, Color(0xFFDCDCDC), RoundedCornerShape(6.dp)).padding(8.dp, 8.dp),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 15.sp),
                singleLine = true
            )
            Text("😊", fontSize = 24.sp)
            Text("⊕", fontSize = 24.sp, modifier = Modifier.clickable {
                val text = inputText.text.trim()
                if (text.isNotEmpty()) {
                    messages.add(ChatMessage("sent", text = text))
                    inputText = TextFieldValue("")
                    scope.launch { listState.animateScrollToItem(messages.size - 1) }
                }
            })
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

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFEDEDED)).statusBarsPadding()) {
        // Header
        Box(modifier = Modifier.fillMaxWidth().height(50.dp).background(Color(0xFFEDEDED)), contentAlignment = Alignment.Center) {
            Text("‹", modifier = Modifier.align(Alignment.CenterStart).padding(start = 12.dp).clickable { onBack() }, fontSize = 24.sp)
            Text("服务", fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
            Text("···", modifier = Modifier.align(Alignment.CenterEnd).padding(end = 12.dp), fontSize = 20.sp)
        }
        Divider(color = Color(0xFFD9D9D9), thickness = 0.5.dp)

        // 绿色卡片
        Box(
            modifier = Modifier.fillMaxWidth().padding(12.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF07C160), Color(0xFF06AD56))))
                .padding(28.dp, 24.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("⊡", fontSize = 32.sp, color = Color.White)
                    Text("收付款", fontSize = 14.sp, color = Color.White)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("💳", fontSize = 32.sp, color = Color.White)
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
            color = Color(0xFF888888),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}