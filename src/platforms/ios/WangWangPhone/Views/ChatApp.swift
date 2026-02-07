
import SwiftUI

// MARK: - Data Models
struct WXConversation: Identifiable {
    let id: String
    let name: String
    let avatar: String
    let lastMsg: String
    let time: String
    var unread: Int = 0
    var muted: Bool = false
    let iconBg: Color
}

struct WXContactGroup: Identifiable {
    let id: String
    let name: String
    let icon: String
    let color: Color
}

struct WXContact: Identifiable {
    let id: String
    let name: String
    let avatar: String
    var letter: String = ""
}

struct WXMoment: Identifiable {
    let id: String
    let name: String
    let avatar: String
    let content: String
    let time: String
    let likes: [String]
    let comments: [(String, String)]
}

struct WXChatMessage: Identifiable {
    let id = UUID()
    let type: String
    var name: String = ""
    var avatar: String = ""
    var text: String = ""
}

// MARK: - Mock Data
let wxConversations: [WXConversation] = [
    WXConversation(id: "c1", name: "文件传输助手", avatar: "📁", lastMsg: "你好，这是一条测试消息", time: "12:51", iconBg: Color(red: 1.0, green: 0.6, blue: 0.0)),
    WXConversation(id: "c2", name: "小明", avatar: "😊", lastMsg: "[语音] 14\"", time: "11:38", unread: 2, iconBg: .green),
    WXConversation(id: "c3", name: "工作群", avatar: "👥", lastMsg: "张三: 收到，谢谢大家的配合", time: "周四", muted: true, iconBg: .blue),
    WXConversation(id: "c4", name: "家人群", avatar: "🏠", lastMsg: "妈妈: [链接] 今日菜谱推荐...", time: "周三", iconBg: .pink),
    WXConversation(id: "c5", name: "同学群", avatar: "🎓", lastMsg: "李华: 科目一快考完了", time: "1月30日", iconBg: .purple),
    WXConversation(id: "c6", name: "技术交流群", avatar: "💻", lastMsg: "王工: 新版本已部署上线", time: "12月2日", muted: true, iconBg: .gray),
    WXConversation(id: "c7", name: "公众号", avatar: "📰", lastMsg: "[3条] 今日科技资讯速览...", time: "16:37", unread: 3, iconBg: Color(red: 0.1, green: 0.46, blue: 0.82)),
    WXConversation(id: "c8", name: "服务号", avatar: "🔔", lastMsg: "[5条通知] 您的快递已到达...", time: "16:30", unread: 5, iconBg: .red),
    WXConversation(id: "c9", name: "技术攻关群", avatar: "🔧", lastMsg: "应该就好了", time: "16:11", iconBg: .brown),
]

let wxContactGroups: [WXContactGroup] = [
    WXContactGroup(id: "g1", name: "新的朋友", icon: "👤", color: .orange),
    WXContactGroup(id: "g2", name: "仅聊天的朋友", icon: "👤", color: .orange),
    WXContactGroup(id: "g3", name: "群聊", icon: "👥", color: .green),
    WXContactGroup(id: "g4", name: "标签", icon: "🏷️", color: .blue),
    WXContactGroup(id: "g5", name: "公众号", icon: "📰", color: Color(red: 0.1, green: 0.46, blue: 0.82)),
    WXContactGroup(id: "g6", name: "服务号", icon: "🔔", color: .red),
]

let wxStarred: [WXContact] = [
    WXContact(id: "u1", name: "小明", avatar: "😊"),
    WXContact(id: "u2", name: "小红", avatar: "🌸"),
]

let wxContactList: [WXContact] = [
    WXContact(id: "u3", name: "阿杰", avatar: "🧑", letter: "A"),
    WXContact(id: "u4", name: "陈伟", avatar: "👨", letter: "C"),
    WXContact(id: "u5", name: "大卫", avatar: "🧔", letter: "D"),
    WXContact(id: "u6", name: "方琳", avatar: "👩", letter: "F"),
    WXContact(id: "u7", name: "何志强", avatar: "👨‍💼", letter: "H"),
    WXContact(id: "u8", name: "李华", avatar: "🧑‍🎓", letter: "L"),
    WXContact(id: "u9", name: "马丽", avatar: "👩‍🦰", letter: "M"),
    WXContact(id: "u1b", name: "小明", avatar: "😊", letter: "X"),
    WXContact(id: "u2b", name: "小红", avatar: "🌸", letter: "X"),
    WXContact(id: "u10", name: "张伟", avatar: "👨‍🔧", letter: "Z"),
    WXContact(id: "u11", name: "赵敏", avatar: "👩‍🏫", letter: "Z"),
]

let wxMoments: [WXMoment] = [
    WXMoment(id: "m1", name: "小明", avatar: "😊", content: "今天天气真好，出去走走 🌞", time: "1分钟前", likes: ["小红", "李华"], comments: [("小红", "确实不错！")]),
    WXMoment(id: "m2", name: "李华", avatar: "🧑‍🎓", content: "终于把项目做完了，庆祝一下 🎉", time: "30分钟前", likes: ["小明", "张伟", "阿杰"], comments: []),
    WXMoment(id: "m3", name: "小红", avatar: "🌸", content: "分享一首好听的歌曲，推荐给大家～", time: "2小时前", likes: ["小明"], comments: [("小明", "什么歌？"), ("小红", "周杰伦的新专辑")]),
    WXMoment(id: "m4", name: "张伟", avatar: "👨‍🔧", content: "周末去爬山，风景超美！大家有空可以一起来", time: "昨天", likes: ["小红", "李华", "马丽", "陈伟"], comments: [("马丽", "哪个山？下次带我！")]),
]

let wxChatMessages: [String: [WXChatMessage]] = [
    "c1": [
        WXChatMessage(type: "time", text: "昨天 16:04"),
        WXChatMessage(type: "received", name: "文件传输助手", avatar: "📁", text: "你好，欢迎使用文件传输助手"),
        WXChatMessage(type: "sent", text: "你好，这是一条测试消息"),
    ],
    "c2": [
        WXChatMessage(type: "time", text: "昨天 22:27"),
        WXChatMessage(type: "received", name: "小明", avatar: "😊", text: "明天下午有空吗？一起去打球"),
        WXChatMessage(type: "sent", text: "可以啊，几点？"),
        WXChatMessage(type: "received", name: "小明", avatar: "😊", text: "下午三点吧"),
        WXChatMessage(type: "sent", text: "好的，到时候见！"),
        WXChatMessage(type: "time", text: "今天 11:30"),
        WXChatMessage(type: "received", name: "小明", avatar: "😊", text: "[语音] 14\""),
    ],
    "c3": [
        WXChatMessage(type: "time", text: "昨天 16:04"),
        WXChatMessage(type: "received", name: "张三", avatar: "👨", text: "大家好，关于项目进度的问题"),
        WXChatMessage(type: "received", name: "李四", avatar: "🧑", text: "我这边已经完成了80%"),
        WXChatMessage(type: "sent", text: "收到"),
        WXChatMessage(type: "time", text: "昨天 22:27"),
        WXChatMessage(type: "received", name: "王五", avatar: "👨‍💼", text: "简单三步：先做设计、再写代码、最后测试"),
        WXChatMessage(type: "received", name: "赵六", avatar: "🧔", text: "何意味"),
        WXChatMessage(type: "received", name: "张三", avatar: "👨", text: "收到，谢谢大家的配合"),
    ],
    "c4": [
        WXChatMessage(type: "time", text: "周三 19:00"),
        WXChatMessage(type: "received", name: "妈妈", avatar: "👩", text: "今天做了你最爱吃的红烧肉"),
        WXChatMessage(type: "sent", text: "太棒了！我明天回去"),
        WXChatMessage(type: "received", name: "爸爸", avatar: "👨", text: "路上注意安全"),
    ],
]

// MARK: - Main Entry
struct ChatAppView: View {
    @Binding var isPresented: Bool
    @State private var currentTab = "messages"
    @State private var currentView = "main"
    @State private var currentChatId: String? = nil

    var body: some View {
        ZStack {
            switch currentView {
            case "chat-detail":
                ChatDetailView(chatId: currentChatId ?? "", onBack: { currentView = "main"; currentChatId = nil })
            case "service":
                ServicePageView(onBack: { currentView = "main"; currentTab = "me" })
            default:
                ChatMainView(currentTab: $currentTab, onClose: { isPresented = false },
                    onOpenChat: { id in currentChatId = id; currentView = "chat-detail" },
                    onOpenService: { currentView = "service" })
            }
        }
    }
}

// MARK: - Main View with Tabs
struct ChatMainView: View {
    @Binding var currentTab: String
    var onClose: () -> Void
    var onOpenChat: (String) -> Void
    var onOpenService: () -> Void
    private let titles = ["messages": "消息", "contacts": "通讯录", "moments": "朋友圈", "me": "我"]

    var body: some View {
        VStack(spacing: 0) {
            // Header
            ZStack {
                Text(titles[currentTab] ?? "").font(.system(size: 17, weight: .semibold))
                HStack {
                    Button(action: onClose) { Text("‹").font(.system(size: 24)) }.foregroundColor(.black)
                    Spacer()
                    Text("🔍").font(.system(size: 20))
                    Text("⊕").font(.system(size: 20))
                }.padding(.horizontal, 12)
            }.frame(height: 50).background(Color(red: 0.93, green: 0.93, blue: 0.93))
            Divider()

            // Content
            Group {
                switch currentTab {
                case "messages": MessagesTabView(onOpenChat: onOpenChat)
                case "contacts": ContactsTabView(onOpenChat: onOpenChat)
                case "moments": MomentsTabView()
                case "me": MeTabView(onOpenService: onOpenService, onOpenMoments: { currentTab = "moments" })
                default: EmptyView()
                }
            }.frame(maxHeight: .infinity)

            Divider()
            ChatTabBarView(currentTab: $currentTab)
        }.background(Color(red: 0.93, green: 0.93, blue: 0.93))
    }
}

// MARK: - Tab Bar
struct ChatTabBarView: View {
    @Binding var currentTab: String
    let totalUnread = wxConversations.reduce(0) { $0 + $1.unread }
    let tabs: [(String, String, String)] = [("messages", "💬", "消息"), ("contacts", "👥", "通讯录"), ("moments", "📷", "朋友圈"), ("me", "👤", "我")]

    var body: some View {
        HStack {
            ForEach(tabs, id: \.0) { tab in
                VStack(spacing: 2) {
                    ZStack(alignment: .topTrailing) {
                        Text(tab.1).font(.system(size: 22))
                        if tab.0 == "messages" && totalUnread > 0 {
                            Text("\(totalUnread)").font(.system(size: 10)).foregroundColor(.white)
                                .padding(.horizontal, 4).background(Color.red).clipShape(Capsule())
                                .offset(x: 8, y: -4)
                        }
                    }
                    Text(tab.2).font(.system(size: 10))
                        .foregroundColor(currentTab == tab.0 ? Color(red: 0.03, green: 0.76, blue: 0.38) : .gray)
                }
                .frame(maxWidth: .infinity)
                .onTapGesture { currentTab = tab.0 }
            }
        }.frame(height: 56).background(Color(red: 0.97, green: 0.97, blue: 0.97))
    }
}

// MARK: - Messages Tab
struct MessagesTabView: View {
    var onOpenChat: (String) -> Void
    var body: some View {
        ScrollView {
            LazyVStack(spacing: 0) {
                ForEach(wxConversations) { conv in
                    Button(action: { onOpenChat(conv.id) }) {
                        HStack(spacing: 12) {
                            ZStack(alignment: .topTrailing) {
                                RoundedRectangle(cornerRadius: 6).fill(conv.iconBg).frame(width: 48, height: 48)
                                    .overlay(Text(conv.avatar).font(.system(size: 24)))
                                if conv.unread > 0 {
                                    Text("\(conv.unread)").font(.system(size: 10)).foregroundColor(.white)
                                        .padding(.horizontal, 4).background(Color.red).clipShape(Capsule()).offset(x: 4, y: -4)
                                }
                            }
                            VStack(alignment: .leading, spacing: 4) {
                                HStack {
                                    Text(conv.name).font(.system(size: 16)).foregroundColor(.black).lineLimit(1)
                                    Spacer()
                                    Text(conv.time).font(.system(size: 12)).foregroundColor(Color(white: 0.7))
                                }
                                HStack {
                                    Text(conv.lastMsg).font(.system(size: 14)).foregroundColor(Color(white: 0.7)).lineLimit(1)
                                    Spacer()
                                    if conv.muted { Text("🔇").font(.system(size: 14)) }
                                }
                            }
                        }.padding(12).background(Color.white)
                    }.buttonStyle(PlainButtonStyle())
                    Divider().padding(.leading, 72)
                }
            }
        }
    }
}

// MARK: - Contacts Tab
struct ContactsTabView: View {
    var onOpenChat: (String) -> Void
    var body: some View {
        ScrollView {
            VStack(spacing: 0) {
                ForEach(wxContactGroups) { g in
                    HStack(spacing: 12) {
                        RoundedRectangle(cornerRadius: 6).fill(g.color).frame(width: 40, height: 40)
                            .overlay(Text(g.icon).font(.system(size: 20)).foregroundColor(.white))
                        Text(g.name).font(.system(size: 16))
                        Spacer()
                    }.padding(12).background(Color.white)
                    Divider()
                }
                if !wxStarred.isEmpty {
                    Text("星标朋友").font(.system(size: 13)).foregroundColor(.gray).frame(maxWidth: .infinity, alignment: .leading)
                        .padding(.horizontal, 16).padding(.vertical, 6).background(Color(red: 0.93, green: 0.93, blue: 0.93))
                    ForEach(wxStarred) { c in ContactRow(contact: c, onOpenChat: onOpenChat) }
                }
                var lastLetter = ""
                ForEach(wxContactList) { c in
                    if !c.letter.isEmpty && c.letter != lastLetter {
                        let _ = { lastLetter = c.letter }()
                        Text(c.letter).font(.system(size: 13)).foregroundColor(.gray).frame(maxWidth: .infinity, alignment: .leading)
                            .padding(.horizontal, 16).padding(.vertical, 6).background(Color(red: 0.93, green: 0.93, blue: 0.93))
                    }
                    ContactRow(contact: c, onOpenChat: onOpenChat)
                }
            }
        }
    }
}

struct ContactRow: View {
    let contact: WXContact
    var onOpenChat: (String) -> Void
    var body: some View {
        Button(action: {
            let conv = wxConversations.first { $0.name == contact.name }
            onOpenChat(conv?.id ?? "u_\(contact.id)")
        }) {
            HStack(spacing: 12) {
                RoundedRectangle(cornerRadius: 6).fill(Color(white: 0.96)).frame(width: 40, height: 40)
                    .overlay(Text(contact.avatar).font(.system(size: 22)))
                Text(contact.name).font(.system(size: 16)).foregroundColor(.black)
                Spacer()
            }.padding(.horizontal, 16).padding(.vertical, 10).background(Color.white)
        }.buttonStyle(PlainButtonStyle())
        Divider()
    }
}

// MARK: - Moments Tab
struct MomentsTabView: View {
    var body: some View {
        ScrollView {
            VStack(spacing: 0) {
                // Cover
                ZStack(alignment: .bottomTrailing) {
                    LinearGradient(colors: [Color(red: 0.4, green: 0.49, blue: 0.92), Color(red: 0.46, green: 0.29, blue: 0.64)], startPoint: .topLeading, endPoint: .bottomTrailing)
                        .frame(height: 300)
                    HStack(spacing: 12) {
                        Text("我的昵称").foregroundColor(.white).font(.system(size: 18, weight: .semibold))
                        RoundedRectangle(cornerRadius: 10).fill(Color(red: 0.96, green: 0.96, blue: 0.86))
                            .frame(width: 64, height: 64).overlay(Text("🐱").font(.system(size: 32)))
                            .overlay(RoundedRectangle(cornerRadius: 10).stroke(Color.white, lineWidth: 2))
                    }.padding(16)
                }

                ForEach(wxMoments) { m in
                    VStack(alignment: .leading, spacing: 0) {
                        HStack(alignment: .top, spacing: 10) {
                            RoundedRectangle(cornerRadius: 6).fill(Color(white: 0.96)).frame(width: 40, height: 40)
                                .overlay(Text(m.avatar).font(.system(size: 22)))
                            VStack(alignment: .leading, spacing: 4) {
                                Text(m.name).font(.system(size: 15, weight: .semibold)).foregroundColor(Color(red: 0.34, green: 0.42, blue: 0.58))
                                Text(m.content).font(.system(size: 15)).lineSpacing(4)
                                HStack {
                                    Text(m.time).font(.system(size: 12)).foregroundColor(Color(white: 0.7))
                                    Spacer()
                                    Text("··").font(.system(size: 14)).foregroundColor(Color(red: 0.34, green: 0.42, blue: 0.58))
                                        .padding(.horizontal, 6).padding(.vertical, 2).background(Color(white: 0.97)).cornerRadius(4)
                                }
                                if !m.likes.isEmpty || !m.comments.isEmpty {
                                    VStack(alignment: .leading, spacing: 4) {
                                        if !m.likes.isEmpty {
                                            Text("❤️ \(m.likes.joined(separator: "，"))").font(.system(size: 13)).foregroundColor(Color(red: 0.34, green: 0.42, blue: 0.58))
                                        }
                                        if !m.likes.isEmpty && !m.comments.isEmpty { Divider() }
                                        ForEach(m.comments.indices, id: \.self) { i in
                                            HStack(spacing: 0) {
                                                Text(m.comments[i].0).font(.system(size: 13, weight: .medium)).foregroundColor(Color(red: 0.34, green: 0.42, blue: 0.58))
                                                Text("：\(m.comments[i].1)").font(.system(size: 13))
                                            }
                                        }
                                    }.padding(6).background(Color(white: 0.97)).cornerRadius(4)
                                }
                            }
                        }
                    }.padding(12).background(Color.white)
                    Divider()
                }
            }
        }
    }
}

// MARK: - Me Tab
struct MeTabView: View {
    var onOpenService: () -> Void
    var onOpenMoments: () -> Void
    var body: some View {
        ScrollView {
            VStack(spacing: 0) {
                HStack(spacing: 14) {
                    RoundedRectangle(cornerRadius: 10).fill(Color(red: 0.96, green: 0.96, blue: 0.86))
                        .frame(width: 64, height: 64).overlay(Text("🐱").font(.system(size: 32)))
                    VStack(alignment: .leading, spacing: 4) {
                        Text("我的昵称").font(.system(size: 18, weight: .semibold))
                        Text("账号：WangWang_User").font(.system(size: 14)).foregroundColor(.gray)
                    }
                    Spacer()
                    Text("⊞ ›").foregroundColor(.gray).font(.system(size: 20))
                }.padding(.horizontal, 16).padding(.top, 24).padding(.bottom, 16).background(Color.white)

                HStack(spacing: 10) {
                    Text("+ 状态").font(.system(size: 13)).foregroundColor(.gray).padding(.horizontal, 12).padding(.vertical, 4)
                        .overlay(RoundedRectangle(cornerRadius: 16).stroke(Color(white: 0.88), lineWidth: 0.5))
                    Text("👤👤👤 等40个朋友 ●").font(.system(size: 13)).foregroundColor(.gray).padding(.horizontal, 12).padding(.vertical, 4)
                        .overlay(RoundedRectangle(cornerRadius: 16).stroke(Color(white: 0.88), lineWidth: 0.5))
                    Spacer()
                }.padding(.horizontal, 16).padding(.bottom, 16).background(Color.white)

                let menuGroups: [[(String, String, (() -> Void)?)]] = [
                    [("✅", "服务", onOpenService)],
                    [("⭐", "收藏", nil), ("🖼️", "朋友圈", onOpenMoments), ("📺", "视频号和公众号", nil), ("🛒", "订单与卡包", nil), ("😊", "表情", nil)],
                    [("⚙️", "设置", nil)],
                ]

                ForEach(menuGroups.indices, id: \.self) { gi in
                    Color(red: 0.93, green: 0.93, blue: 0.93).frame(height: 8)
                    ForEach(menuGroups[gi].indices, id: \.self) { mi in
                        let item = menuGroups[gi][mi]
                        Button(action: { item.2?() }) {
                            HStack(spacing: 14) {
                                Text(item.0).font(.system(size: 20)).frame(width: 24)
                                Text(item.1).font(.system(size: 16)).foregroundColor(.black)
                                Spacer()
                                Text("›").foregroundColor(Color(white: 0.75)).font(.system(size: 14))
                            }.padding(14).background(Color.white)
                        }.buttonStyle(PlainButtonStyle())
                        if mi < menuGroups[gi].count - 1 { Divider() }
                    }
                }
            }
        }
    }
}

// MARK: - Chat Detail
struct ChatDetailView: View {
    let chatId: String
    var onBack: () -> Void
    @State private var messages: [WXChatMessage] = []
    @State private var inputText: String = ""

    private var conv: WXConversation? { wxConversations.first { $0.id == chatId } }
    private var chatName: String { conv?.name ?? "聊天" }
    private var isGroup: Bool {
        guard conv != nil else { return false }
        return chatName.contains("群") || Set(messages.filter { $0.type == "received" }.map { $0.name }).count > 1
    }

    var body: some View {
        VStack(spacing: 0) {
            ZStack {
                Text(isGroup ? "\(chatName)(\(Int.random(in: 10...50)))" : chatName)
                    .font(.system(size: 17, weight: .semibold)).lineLimit(1)
                HStack {
                    Button(action: onBack) { Text("‹").font(.system(size: 24)) }.foregroundColor(.black)
                    Spacer()
                    Text("···").font(.system(size: 20))
                }.padding(.horizontal, 12)
            }.frame(height: 50).background(Color(red: 0.93, green: 0.93, blue: 0.93))
            Divider()

            ScrollViewReader { proxy in
                ScrollView {
                    LazyVStack(spacing: 10) {
                        ForEach(messages) { msg in
                            switch msg.type {
                            case "time":
                                Text(msg.text).font(.system(size: 12)).foregroundColor(Color(white: 0.7)).frame(maxWidth: .infinity)
                            case "sent":
                                HStack(alignment: .top, spacing: 8) {
                                    Spacer()
                                    Text(msg.text).font(.system(size: 15)).padding(10)
                                        .background(Color(red: 0.58, green: 0.92, blue: 0.41)).cornerRadius(6)
                                    RoundedRectangle(cornerRadius: 6).fill(Color(white: 0.96)).frame(width: 36, height: 36)
                                        .overlay(Text("🐱").font(.system(size: 18)))
                                }
                            case "received":
                                HStack(alignment: .top, spacing: 8) {
                                    RoundedRectangle(cornerRadius: 6).fill(Color(white: 0.96)).frame(width: 36, height: 36)
                                        .overlay(Text(msg.avatar.isEmpty ? "👤" : msg.avatar).font(.system(size: 18)))
                                    VStack(alignment: .leading, spacing: 2) {
                                        if isGroup { Text(msg.name.isEmpty ? chatName : msg.name).font(.system(size: 12)).foregroundColor(.gray) }
                                        Text(msg.text).font(.system(size: 15)).padding(10)
                                            .background(Color.white).cornerRadius(6)
                                    }
                                    Spacer()
                                }
                            default: EmptyView()
                            }
                        }
                    }.padding(.horizontal, 16).padding(.vertical, 10)
                }
            }

            Divider()
            HStack(spacing: 8) {
                Text("🎙️").font(.system(size: 24))
                TextField("", text: $inputText)
                    .textFieldStyle(RoundedBorderTextFieldStyle()).font(.system(size: 15))
                Text("😊").font(.system(size: 24))
                Button(action: sendMessage) { Text("⊕").font(.system(size: 24)) }.foregroundColor(.black)
            }.padding(8).background(Color(white: 0.97))
        }.background(Color(red: 0.93, green: 0.93, blue: 0.93))
        .onAppear { loadMessages() }
    }

    private func loadMessages() {
        messages = wxChatMessages[chatId] ?? [
            WXChatMessage(type: "time", text: "今天 12:00"),
            WXChatMessage(type: "received", name: conv?.name ?? "对方", avatar: conv?.avatar ?? "👤", text: "你好！"),
        ]
    }

    private func sendMessage() {
        let trimmed = inputText.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return }
        messages.append(WXChatMessage(type: "sent", text: trimmed))
        inputText = ""
    }
}

// MARK: - Service Page
struct ServicePageView: View {
    var onBack: () -> Void
    var body: some View {
        VStack(spacing: 0) {
            ZStack {
                Text("服务").font(.system(size: 17, weight: .semibold))
                HStack {
                    Button(action: onBack) { Text("‹").font(.system(size: 24)) }.foregroundColor(.black)
                    Spacer()
                    Text("···").font(.system(size: 20))
                }.padding(.horizontal, 12)
            }.frame(height: 50).background(Color(red: 0.93, green: 0.93, blue: 0.93))
            Divider()

            VStack(spacing: 0) {
                HStack(spacing: 0) {
                    VStack(spacing: 8) {
                        Text("✓").font(.system(size: 36, weight: .light)).foregroundColor(.white)
                        Text("收付款").font(.system(size: 14)).foregroundColor(.white)
                    }.frame(maxWidth: .infinity).padding(.vertical, 20)

                    VStack(spacing: 8) {
                        Text("💳").font(.system(size: 36))
                        Text("钱包").font(.system(size: 14)).foregroundColor(.white)
                        Text("¥888.88").font(.system(size: 12)).foregroundColor(Color.white.opacity(0.7))
                    }.frame(maxWidth: .infinity).padding(.vertical, 20)
                }
                .background(
                    RoundedRectangle(cornerRadius: 12)
                        .fill(LinearGradient(colors: [Color(red: 0.03, green: 0.76, blue: 0.38), Color(red: 0.02, green: 0.66, blue: 0.33)], startPoint: .topLeading, endPoint: .bottomTrailing))
                )
                .padding(16)
            }

            Spacer()

            Text("所有服务已关闭，前往设置 ›").font(.system(size: 14)).foregroundColor(.gray).padding(.bottom, 40)
        }.background(Color(red: 0.93, green: 0.93, blue: 0.93))
    }
}