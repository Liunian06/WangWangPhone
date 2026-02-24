

import SwiftUI
import PhotosUI

// MARK: - WeTheme & Assets
struct WeTheme {
    static let brandGreen = Color("BrandGreen")
    static let background = Color("WeChatBackground")
    static let backgroundCell = Color("WeChatCellBackground")
    static let textPrimary = Color("WeChatTextPrimary")
    static let textSecondary = Color("WeChatTextSecondary")
    static let textHint = Color("WeChatTextHint")
    static let separator = Color("WeChatSeparator")
    static let bubbleSent = Color("WeChatBubbleSent")
    static let bubbleReceived = Color("WeChatBubbleReceived")
    
    static func dynamicColor(light: Color, dark: Color) -> Color {
        Color(UIColor { $0.userInterfaceStyle == .dark ? UIColor(dark) : UIColor(light) })
    }
    
    static let codeBrandGreen = dynamicColor(light: Color(hex: 0x07C160), dark: Color(hex: 0x06AD56))
    static let codeBackground = dynamicColor(light: Color(hex: 0xEDEDED), dark: Color(hex: 0x111111))
    static let codeBackgroundCell = dynamicColor(light: Color(hex: 0xFFFFFF), dark: Color(hex: 0x191919))
    static let codeTextPrimary = dynamicColor(light: Color(hex: 0x191919), dark: Color(hex: 0xD3D3D3))
    static let codeTextSecondary = dynamicColor(light: Color(hex: 0x808080), dark: Color(hex: 0x666666))
    static let codeTextHint = dynamicColor(light: Color(hex: 0xB2B2B2), dark: Color(hex: 0x4C4C4C))
    static let codeSeparator = dynamicColor(light: Color(hex: 0xD9D9D9), dark: Color(hex: 0x2C2C2C))
    static let codeBubbleSent = dynamicColor(light: Color(hex: 0x95EC69), dark: Color(hex: 0x2EA260))
    static let codeBubbleReceived = dynamicColor(light: Color(hex: 0xFFFFFF), dark: Color(hex: 0x2C2C2C))
}

extension Color {
    init(hex: UInt, alpha: Double = 1) {
        self.init(
            .sRGB,
            red: Double((hex >> 16) & 0xff) / 255,
            green: Double((hex >> 08) & 0xff) / 255,
            blue: Double((hex >> 00) & 0xff) / 255,
            opacity: alpha
        )
    }
}

struct WeIcon: View {
    let name: String
    let fallback: String
    var size: CGFloat = 24
    var color: Color? = nil
    
    var body: some View {
        Image(name)
            .resizable()
            .renderingMode(color != nil ? .template : .original)
            .aspectRatio(contentMode: .fit)
            .frame(width: size, height: size)
            .foregroundColor(color)
    }
}

// MARK: - Image Picker
struct ImagePicker: UIViewControllerRepresentable {
    @Binding var isPresented: Bool
    var onImagePicked: (UIImage) -> Void

    func makeUIViewController(context: Context) -> UIImagePickerController {
        let picker = UIImagePickerController()
        picker.sourceType = .photoLibrary
        picker.delegate = context.coordinator
        return picker
    }

    func updateUIViewController(_ uiViewController: UIImagePickerController, context: Context) {}

    func makeCoordinator() -> Coordinator {
        Coordinator(self)
    }

    class Coordinator: NSObject, UIImagePickerControllerDelegate, UINavigationControllerDelegate {
        let parent: ImagePicker

        init(_ parent: ImagePicker) {
            self.parent = parent
        }

        func imagePickerController(_ picker: UIImagePickerController, didFinishPickingMediaWithInfo info: [UIImagePickerController.InfoKey: Any]) {
            if let image = info[.originalImage] as? UIImage {
                parent.onImagePicked(image)
            }
            parent.isPresented = false
        }

        func imagePickerControllerDidCancel(_ picker: UIImagePickerController) {
            parent.isPresented = false
        }
    }
}

// MARK: - Data Models
enum WXConvType { case chat, subscription, service }

struct WXConversation: Identifiable {
    let id: String
    let name: String
    let avatar: String
    let lastMsg: String
    let time: String
    var unread: Int = 0
    var muted: Bool = false
    let iconBg: Color
    var type: WXConvType = .chat
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
    
    func getPinyinInitial() -> String {
        guard let firstChar = name.first else { return "#" }
        if firstChar.isLetter {
            return String(firstChar).uppercased()
        }
        if firstChar >= "\u{4e00}" && firstChar <= "\u{9fff}" {
            return String(getPinyinFirstLetter(firstChar))
        }
        return "#"
    }
    
    private func getPinyinFirstLetter(_ c: Character) -> Character {
        let code = c.unicodeScalars.first?.value ?? 0
        switch code {
        case 0x963F...0x9FFF: return "A"
        case 0x5DF4...0x5EF6: return "B"
        case 0x5F00...0x62FF: return "C"
        case 0x6300...0x6536: return "D"
        case 0x5384...0x5592: return "E"
        case 0x53D1...0x5926: return "F"
        case 0x7518...0x7A00: return "G"
        case 0x54C8...0x5DF3: return "H"
        case 0x673A...0x6770: return "J"
        case 0x5361...0x5494: return "K"
        case 0x5783...0x5D03: return "L"
        case 0x5988...0x5BFF: return "M"
        case 0x54EA...0x5360: return "N"
        case 0x5594...0x5783: return "O"
        case 0x556A...0x5939: return "P"
        case 0x4E03...0x5360: return "Q"
        case 0x7136...0x7518: return "R"
        case 0x4E09...0x53D0: return "S"
        case 0x584C...0x6316: return "T"
        case 0x6316...0x6770: return "W"
        case 0x5915...0x5BFF: return "X"
        case 0x538B...0x5939: return "Y"
        case 0x531D...0x5594: return "Z"
        default: return "#"
        }
    }
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
    WXConversation(id: "c3", name: "工作群", avatar: "👥", lastMsg: "张三: 收到，谢谢大家的配合", time: "周四", muted: true, unread: 1, iconBg: .blue),
    WXConversation(id: "c4", name: "家人群", avatar: "🏠", lastMsg: "妈妈: [链接] 今日菜谱推荐...", time: "周三", iconBg: .pink),
    WXConversation(id: "c5", name: "同学群", avatar: "🎓", lastMsg: "李华: 科目一快考完了", time: "1月30日", iconBg: .purple),
    WXConversation(id: "c6", name: "技术交流群", avatar: "💻", lastMsg: "王工: 新版本已部署上线", time: "12月2日", muted: true, unread: 1, iconBg: .gray),
    WXConversation(id: "c7", name: "公众号", avatar: "📰", lastMsg: "[3条] 今日科技资讯速览...", time: "16:37", unread: 3, iconBg: Color(red: 0.1, green: 0.46, blue: 0.82), type: .subscription),
    WXConversation(id: "c8", name: "服务号", avatar: "🔔", lastMsg: "[5条通知] 您的快递已到达...", time: "16:30", unread: 5, iconBg: .red, type: .service),
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

// MARK: - User Avatar Component
struct UserAvatarView: View {
    let avatarImage: UIImage?
    var size: CGFloat = 64
    var cornerRadius: CGFloat = 10
    var defaultEmoji: String = "🐱"
    var defaultEmojiSize: CGFloat = 32

    var body: some View {
        if let image = avatarImage {
            Image(uiImage: image)
                .resizable()
                .aspectRatio(contentMode: .fill)
                .frame(width: size, height: size)
                .clipShape(RoundedRectangle(cornerRadius: cornerRadius))
        } else {
            RoundedRectangle(cornerRadius: cornerRadius)
                .fill(Color(red: 0.96, green: 0.96, blue: 0.86))
                .frame(width: size, height: size)
                .overlay(Text(defaultEmoji).font(.system(size: defaultEmojiSize)))
        }
    }
}

// MARK: - Main Entry
struct ChatAppView: View {
    @Binding var isPresented: Bool
    @State private var currentTab = "messages"
    @State private var currentView = "main"
    @State private var currentChatId: String? = nil
    @State private var currentContactId: String? = nil
    @State private var showContactSelection = false

    // 用户资料状态
    @State private var userNickname: String = UserProfileManager.shared.getUserProfile().nickname
    @State private var userSignature: String = UserProfileManager.shared.getUserProfile().signature
    @State private var avatarImage: UIImage? = UserProfileManager.shared.getAvatarImage()
    @State private var coverImage: UIImage? = UserProfileManager.shared.getCoverImage()

    var body: some View {
        ZStack {
            switch currentView {
            case "chat-detail":
                ChatDetailView(chatId: currentChatId ?? "", onBack: { currentView = "main"; currentChatId = nil }, avatarImage: avatarImage)
            case "contact-detail":
                ContactDetailView(contactId: currentContactId ?? "", onBack: { currentView = "main"; currentContactId = nil }, onSendMessage: { id in currentChatId = id; currentView = "chat-detail" })
            case "service":
                ServicePageView(onBack: { currentView = "main"; currentTab = "me" })
            case "select-contacts":
                ContactSelectionView(
                    onBack: { currentView = "main"; showContactSelection = false },
                    onContactsSelected: { contact1Id, contact2Id in
                        // 创建新的聊天会话
                        let chatId = ChatDbHelper.shared.createChat(contact1Id: contact1Id, contact2Id: contact2Id)
                        currentChatId = chatId
                        currentView = "chat-detail"
                        showContactSelection = false
                    }
                )
            default:
                ChatMainView(
                    currentTab: $currentTab,
                    onClose: { isPresented = false },
                    onOpenChat: { id in currentChatId = id; currentView = "chat-detail" },
                    onOpenContact: { id in currentContactId = id; currentView = "contact-detail" },
                    onOpenService: { currentView = "service" },
                    onStartChat: { currentView = "select-contacts"; showContactSelection = true },
                    userNickname: $userNickname,
                    userSignature: $userSignature,
                    avatarImage: $avatarImage,
                    coverImage: $coverImage
                )
            }
        }
    }
}

// MARK: - Main View with Tabs
struct ChatMainView: View {
    @Binding var currentTab: String
    var onClose: () -> Void
    var onOpenChat: (String) -> Void
    var onOpenContact: (String) -> Void
    var onOpenService: () -> Void
    var onStartChat: () -> Void
    @Binding var userNickname: String
    @Binding var userSignature: String
    @Binding var avatarImage: UIImage?
    @Binding var coverImage: UIImage?
    private let titles = ["messages": "微信", "contacts": "通讯录", "moments": "朋友圈", "me": "我"]
    @State private var showMenu = false

    var body: some View {
        VStack(spacing: 0) {
            if currentTab != "me" {
                ZStack {
                    Text(titles[currentTab] ?? "").font(.system(size: 17, weight: .semibold)).foregroundColor(WeTheme.codeTextPrimary)
                    HStack(spacing: 16) {
                        Spacer()
                        WeIcon(name: "ic_search", fallback: "🔍", size: 24, color: WeTheme.codeTextPrimary)
                        if currentTab != "moments" {
                            Menu {
                                Button(action: onStartChat) {
                                    Label("发起聊天", systemImage: "message")
                                }
                                Button(action: {}) {
                                    Label("添加朋友", systemImage: "person.badge.plus")
                                }
                                Button(action: {}) {
                                    Label("扫一扫", systemImage: "camera")
                                }
                                Button(action: {}) {
                                    Label("收付款", systemImage: "creditcard")
                                }
                            } label: {
                                WeIcon(name: "ic_chat_add", fallback: "⊕", size: 24, color: WeTheme.codeTextPrimary)
                            }
                        }
                    }.padding(.horizontal, 12)
                }.frame(height: 50).background(WeTheme.codeBackground)
                Divider().overlay(WeTheme.codeSeparator)
            }

            Group {
                switch currentTab {
                case "messages": MessagesTabView(onOpenChat: onOpenChat)
                case "contacts": ContactsTabView(onOpenContact: onOpenContact)
                case "moments": MomentsTabView(
                    userNickname: $userNickname,
                    userSignature: $userSignature,
                    avatarImage: $avatarImage,
                    coverImage: $coverImage
                )
                case "me": MeTabView(
                    onOpenService: onOpenService,
                    onOpenMoments: { currentTab = "moments" },
                    userNickname: userNickname,
                    avatarImage: avatarImage
                )
                default: EmptyView()
                }
            }.frame(maxHeight: .infinity)

            Divider().overlay(WeTheme.codeSeparator)
            ChatTabBarView(currentTab: $currentTab)
        }.background(WeTheme.codeBackground)
    }
}

// MARK: - Tab Bar
struct ChatTabBarView: View {
    @Binding var currentTab: String
    let totalUnread = wxConversations.reduce(0) { $0 + $1.unread }
    let tabs = [
        ("messages", "ic_tab_chat_selected", "ic_tab_chat_normal", "微信", "💬"),
        ("contacts", "ic_tab_contacts_selected", "ic_tab_contacts_normal", "通讯录", "👥"),
        ("moments", "ic_tab_discover_selected", "ic_tab_discover_normal", "发现", "🧭"),
        ("me", "ic_tab_me_selected", "ic_tab_me_normal", "我", "👤")
    ]

    var body: some View {
        HStack(spacing: 0) {
            ForEach(tabs, id: \.0) { tab in
                let isSelected = currentTab == tab.0
                VStack(spacing: 2) {
                    ZStack {
                        WeIcon(
                            name: isSelected ? tab.1 : tab.2,
                            fallback: tab.4,
                            size: 28,
                            color: isSelected ? nil : WeTheme.codeTextPrimary
                        )
                        if tab.0 == "messages" && totalUnread > 0 {
                            TabUnreadBadgeView(count: totalUnread).position(x: 28, y: 4)
                        }
                    }.frame(width: 28, height: 28)
                    Text(tab.3).font(.system(size: 10, weight: .medium))
                        .foregroundColor(isSelected ? WeTheme.codeBrandGreen : WeTheme.codeTextPrimary)
                }
                .frame(maxWidth: .infinity)
                .contentShape(Rectangle())
                .onTapGesture { currentTab = tab.0 }
            }
        }
        .frame(height: 56)
        .background(WeTheme.codeBackground.edgesIgnoringSafeArea(.bottom))
    }
}

// MARK: - Unread Badge Components
struct UnreadBadgeView: View {
    let conv: WXConversation
    var body: some View {
        if conv.unread <= 0 {
            EmptyView()
        } else if conv.type == .subscription || conv.type == .service {
            Image("ic_badge_new").resizable().aspectRatio(contentMode: .fit).frame(width: 38, height: 18)
        } else if conv.muted {
            Image("ic_badge_dot").resizable().aspectRatio(contentMode: .fit).frame(width: 8, height: 8)
        } else if conv.unread > 99 {
            Image("ic_badge_more").resizable().aspectRatio(contentMode: .fit).frame(width: 33, height: 18)
        } else {
            ZStack {
                Circle().fill(Color(hex: 0xFA5151)).frame(width: 18, height: 18)
                Text("\(conv.unread)").font(.system(size: 10, weight: .bold)).foregroundColor(.white)
            }
        }
    }
}

struct TabUnreadBadgeView: View {
    let count: Int
    var body: some View {
        if count <= 0 {
            EmptyView()
        } else if count > 99 {
            Image("ic_badge_more").resizable().aspectRatio(contentMode: .fit).frame(width: 33, height: 18)
        } else {
            ZStack {
                Circle().fill(Color(hex: 0xFA5151)).frame(width: 18, height: 18)
                Text("\(count)").font(.system(size: 10, weight: .bold)).foregroundColor(.white)
            }
        }
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
                        HStack(spacing: 8) {
                            ZStack {
                                RoundedRectangle(cornerRadius: 4).fill(conv.iconBg).frame(width: 48, height: 48)
                                    .overlay(Text(conv.avatar).font(.system(size: 24)))
                                UnreadBadgeView(conv: conv).position(x: 50, y: 2)
                            }.frame(width: 52, height: 52)
                            VStack(alignment: .leading, spacing: 6) {
                                HStack {
                                    Text(conv.name).font(.system(size: 16)).foregroundColor(WeTheme.codeTextPrimary).lineLimit(1)
                                    Spacer()
                                    Text(conv.time).font(.system(size: 11)).foregroundColor(WeTheme.codeTextHint)
                                }
                                HStack {
                                    Text(conv.lastMsg).font(.system(size: 14)).foregroundColor(WeTheme.codeTextSecondary).lineLimit(1)
                                    Spacer()
                                    if conv.muted { WeIcon(name: "ic_mute", fallback: "🔇", size: 16, color: WeTheme.codeTextHint) }
                                }
                            }
                        }.padding(16).background(WeTheme.codeBackgroundCell)
                    }.buttonStyle(PlainButtonStyle())
                    Divider().overlay(WeTheme.codeSeparator).padding(.leading, 76)
                }
            }
        }
    }
}

// MARK: - Contacts Tab
struct ContactsTabView: View {
    var onOpenContact: (String) -> Void
    var body: some View {
        ScrollView {
            VStack(spacing: 0) {
                ForEach(wxContactGroups) { g in
                    HStack(spacing: 16) {
                        RoundedRectangle(cornerRadius: 4).fill(g.color).frame(width: 36, height: 36)
                            .overlay(Text(g.icon).font(.system(size: 20)).foregroundColor(.white))
                        Text(g.name).font(.system(size: 16)).foregroundColor(WeTheme.codeTextPrimary)
                        Spacer()
                    }.padding(.horizontal, 16).padding(.vertical, 10).background(WeTheme.codeBackgroundCell)
                    Divider().overlay(WeTheme.codeSeparator).padding(.leading, 68)
                }
                if !wxStarred.isEmpty {
                    Text("星标朋友").font(.system(size: 12)).foregroundColor(WeTheme.codeTextSecondary).frame(maxWidth: .infinity, alignment: .leading)
                        .padding(.horizontal, 16).padding(.vertical, 8).background(WeTheme.codeBackground)
                    ForEach(wxStarred) { c in ContactRow(contact: c, onOpenContact: onOpenContact) }
                }
                var lastLetter = ""
                ForEach(wxContactList) { c in
                    if !c.letter.isEmpty && c.letter != lastLetter {
                        let _ = { lastLetter = c.letter }()
                        Text(c.letter).font(.system(size: 12)).foregroundColor(WeTheme.codeTextSecondary).frame(maxWidth: .infinity, alignment: .leading)
                            .padding(.horizontal, 16).padding(.vertical, 8).background(WeTheme.codeBackground)
                    }
                    ContactRow(contact: c, onOpenContact: onOpenContact)
                }
            }
        }
    }
}

struct ContactRow: View {
    let contact: WXContact
    var onOpenContact: (String) -> Void
    var body: some View {
        Button(action: {
            onOpenContact(contact.id)
        }) {
            HStack(spacing: 16) {
                RoundedRectangle(cornerRadius: 4).fill(WeTheme.codeBackground).frame(width: 36, height: 36)
                    .overlay(Text(contact.avatar).font(.system(size: 22)))
                Text(contact.name).font(.system(size: 16)).foregroundColor(WeTheme.codeTextPrimary)
                Spacer()
            }.padding(.horizontal, 16).padding(.vertical, 10).background(WeTheme.codeBackgroundCell)
        }.buttonStyle(PlainButtonStyle())
        Divider().overlay(WeTheme.codeSeparator).padding(.leading, 68)
    }
}

// MARK: - Moments Tab
struct MomentsTabView: View {
    @Binding var userNickname: String
    @Binding var userSignature: String
    @Binding var avatarImage: UIImage?
    @Binding var coverImage: UIImage?

    private let avatarSize: CGFloat = 64
    private let avatarOverlap: CGFloat = 22
    private let coverHeight: CGFloat = 300

    // 图片选择器
    @State private var showCoverPicker = false
    @State private var showAvatarPicker = false

    // 文本编辑
    @State private var showNicknameAlert = false
    @State private var showSignatureAlert = false
    @State private var editingText = ""

    var body: some View {
        let momentCellBackground = WeTheme.dynamicColor(light: .white, dark: Color(hex: 0x111111))
        let momentListBackground = momentCellBackground
        let momentPrimaryColor = WeTheme.dynamicColor(light: Color(red: 0.34, green: 0.42, blue: 0.58), dark: Color(hex: 0x8795B3))
        let momentContentColor = WeTheme.dynamicColor(light: Color(hex: 0x191919), dark: Color(hex: 0xD3D3D3))
        let momentActionBackground = WeTheme.dynamicColor(light: Color(white: 0.97), dark: Color(hex: 0x2A2A2A))
        let momentAvatarBackground = WeTheme.dynamicColor(light: Color(white: 0.96), dark: Color(hex: 0x2C2C2C))
        let momentTimeColor = WeTheme.dynamicColor(light: Color(white: 0.7), dark: Color(hex: 0x666666))
        let momentSignatureColor = WeTheme.dynamicColor(light: Color(hex: 0x999999), dark: Color(hex: 0x666666))

        ScrollView {
            VStack(spacing: 0) {
                // 封面 + 头像 + 签名区域
                VStack(spacing: 0) {
                    ZStack(alignment: .bottomTrailing) {
                        VStack(spacing: 0) {
                            // 封面（可点击更换）
                            ZStack(alignment: .bottomLeading) {
                                if let cover = coverImage {
                                    Image(uiImage: cover)
                                        .resizable()
                                        .aspectRatio(contentMode: .fill)
                                        .frame(height: coverHeight)
                                        .clipped()
                                } else {
                                    LinearGradient(colors: [Color(red: 0.4, green: 0.49, blue: 0.92), Color(red: 0.46, green: 0.29, blue: 0.64)], startPoint: .topLeading, endPoint: .bottomTrailing)
                                        .frame(height: coverHeight)
                                }
                                // 封面提示
                                Text("点击更换封面").font(.system(size: 11)).foregroundColor(.white.opacity(0.7)).padding(12)
                            }
                            .frame(height: coverHeight)
                            .onTapGesture { showCoverPicker = true }

                            momentCellBackground.frame(height: avatarOverlap)
                        }

                        // 昵称 + 头像
                        HStack(alignment: .bottom, spacing: 12) {
                            Text(userNickname).foregroundColor(.white).font(.system(size: 18, weight: .semibold))
                                .shadow(color: .black.opacity(0.3), radius: 2, x: 0, y: 1)
                                .padding(.bottom, avatarOverlap + 6)
                                .onTapGesture {
                                    editingText = userNickname
                                    showNicknameAlert = true
                                }
                            UserAvatarView(avatarImage: avatarImage, size: avatarSize, cornerRadius: 10)
                                .overlay(RoundedRectangle(cornerRadius: 10).stroke(Color.white, lineWidth: 2))
                                .onTapGesture { showAvatarPicker = true }
                        }.padding(.trailing, 16).padding(.bottom, 0)
                    }

                    // 用户签名
                    HStack {
                        Spacer()
                        Text(userSignature)
                            .font(.system(size: 13))
                            .foregroundColor(momentSignatureColor)
                            .lineLimit(1)
                    }
                    .padding(.horizontal, 16)
                    .padding(.vertical, 10)
                    .background(momentCellBackground)
                    .onTapGesture {
                        editingText = userSignature
                        showSignatureAlert = true
                    }
                }

                ForEach(wxMoments) { m in
                    VStack(alignment: .leading, spacing: 0) {
                        HStack(alignment: .top, spacing: 10) {
                            RoundedRectangle(cornerRadius: 6).fill(momentAvatarBackground).frame(width: 40, height: 40)
                                .overlay(Text(m.avatar).font(.system(size: 22)))
                            VStack(alignment: .leading, spacing: 4) {
                                Text(m.name).font(.system(size: 15, weight: .semibold)).foregroundColor(momentPrimaryColor)
                                Text(m.content).font(.system(size: 15)).lineSpacing(4).foregroundColor(momentContentColor)
                                HStack {
                                    Text(m.time).font(.system(size: 12)).foregroundColor(momentTimeColor)
                                    Spacer()
                                    Text("··").font(.system(size: 14)).foregroundColor(momentPrimaryColor)
                                        .padding(.horizontal, 6).padding(.vertical, 2).background(momentActionBackground).cornerRadius(4)
                                }
                                if !m.likes.isEmpty || !m.comments.isEmpty {
                                    VStack(alignment: .leading, spacing: 4) {
                                        if !m.likes.isEmpty {
                                            HStack(spacing: 4) {
                                                WeIcon(name: "ic_like", fallback: "♡", size: 18, color: momentPrimaryColor)
                                                Text(m.likes.joined(separator: "，")).font(.system(size: 13)).foregroundColor(momentPrimaryColor)
                                            }
                                        }
                                        if !m.likes.isEmpty && !m.comments.isEmpty { Divider().overlay(WeTheme.codeSeparator) }
                                        ForEach(m.comments.indices, id: \.self) { i in
                                            HStack(spacing: 0) {
                                                Text(m.comments[i].0).font(.system(size: 13, weight: .medium)).foregroundColor(momentPrimaryColor)
                                                Text("：\(m.comments[i].1)").font(.system(size: 13)).foregroundColor(momentContentColor)
                                            }
                                        }
                                    }.padding(6).background(momentActionBackground).cornerRadius(4)
                                }
                            }
                        }
                    }.padding(12).background(momentCellBackground)
                    Divider().overlay(WeTheme.codeSeparator)
                }
            }
        }
        .background(momentListBackground)
        .sheet(isPresented: $showCoverPicker) {
            ImagePicker(isPresented: $showCoverPicker) { image in
                UserProfileManager.shared.updateCover(image)
                coverImage = image
            }
        }
        .sheet(isPresented: $showAvatarPicker) {
            ImagePicker(isPresented: $showAvatarPicker) { image in
                UserProfileManager.shared.updateAvatar(image)
                avatarImage = image
            }
        }
        .alert("修改昵称", isPresented: $showNicknameAlert) {
            TextField("昵称", text: $editingText)
            Button("取消", role: .cancel) {}
            Button("确定") {
                UserProfileManager.shared.updateNickname(editingText)
                userNickname = editingText
            }
        }
        .alert("修改签名", isPresented: $showSignatureAlert) {
            TextField("签名", text: $editingText)
            Button("取消", role: .cancel) {}
            Button("确定") {
                UserProfileManager.shared.updateSignature(editingText)
                userSignature = editingText
            }
        }
    }
}

// MARK: - Me Tab
struct MeTabView: View {
    var onOpenService: () -> Void
    var onOpenMoments: () -> Void
    var userNickname: String
    var avatarImage: UIImage?

    var body: some View {
        ScrollView {
            VStack(spacing: 0) {
                VStack(spacing: 0) {
                    HStack(spacing: 20) {
                        UserAvatarView(avatarImage: avatarImage, size: 72, cornerRadius: 8)
                        VStack(alignment: .leading, spacing: 10) {
                            Text(userNickname).font(.system(size: 22, weight: .bold)).foregroundColor(WeTheme.codeTextPrimary)
                            HStack(spacing: 4) {
                                Text("微信号：WangWang_User").font(.system(size: 16)).foregroundColor(WeTheme.codeTextSecondary)
                                Spacer().frame(width: 8)
                                WeIcon(name: "ic_badge_more", fallback: "▒", size: 16, color: WeTheme.codeTextSecondary)
                                Text("›").foregroundColor(WeTheme.codeTextHint).font(.system(size: 16))
                            }
                        }
                        Spacer()
                    }
                    Spacer().frame(height: 24)
                    HStack(spacing: 12) {
                        Text("+ 状态").font(.system(size: 14)).foregroundColor(WeTheme.codeTextSecondary).padding(.horizontal, 14).padding(.vertical, 6)
                            .overlay(RoundedRectangle(cornerRadius: 20).stroke(Color(white: 0.88), lineWidth: 0.5))
                        Text("👤👤👤 等40个朋友 ●").font(.system(size: 14)).foregroundColor(WeTheme.codeTextSecondary).padding(.horizontal, 14).padding(.vertical, 6)
                            .overlay(RoundedRectangle(cornerRadius: 20).stroke(Color(white: 0.88), lineWidth: 0.5))
                        Spacer()
                    }
                }
                .padding(.horizontal, 24)
                .padding(.top, 60)
                .padding(.bottom, 24)
                .background(WeTheme.codeBackgroundCell)

                let menuGroups: [[(String, String, Color?, String, (() -> Void)?)]] = [
                    [("ic_pay_logo", "服务", Color(hex: 0x07C160), "✅", onOpenService)],
                    [
                        ("ic_favorites", "收藏", nil, "⭐", nil),
                        ("ic_moment", "朋友圈", nil, "🖼️", onOpenMoments),
                        ("ic_album", "视频号和公众号", Color(hex: 0xFA9D3B), "📺", nil),
                        ("ic_cards", "订单与卡包", nil, "🛒", nil),
                        ("ic_chat_emoji", "表情", Color(hex: 0xFFC300), "😊", nil)
                    ],
                    [("ic_setting", "设置", Color(hex: 0x1976D2), "⚙️", nil)],
                ]

                ForEach(menuGroups.indices, id: \.self) { gi in
                    Color(red: 0.93, green: 0.93, blue: 0.93).frame(height: 8)
                    ForEach(menuGroups[gi].indices, id: \.self) { mi in
                        let item = menuGroups[gi][mi]
                        Button(action: { item.4?() }) {
                            HStack(spacing: 16) {
                                WeIcon(name: item.0, fallback: item.3, size: 24, color: item.2)
                                Text(item.1).font(.system(size: 16)).foregroundColor(WeTheme.codeTextPrimary)
                                Spacer()
                                Text("›").foregroundColor(Color(white: 0.75)).font(.system(size: 16))
                            }.padding(16).background(WeTheme.codeBackgroundCell)
                        }.buttonStyle(PlainButtonStyle())
                        if mi < menuGroups[gi].count - 1 { Divider().padding(.leading, 56) }
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
    var avatarImage: UIImage?
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
                    .font(.system(size: 17, weight: .semibold)).lineLimit(1).foregroundColor(WeTheme.codeTextPrimary)
                HStack {
                    WeIcon(name: "ic_nav_back", fallback: "‹", size: 24, color: WeTheme.codeTextPrimary)
                        .onTapGesture(perform: onBack)
                    Spacer()
                    Text("···").font(.system(size: 20, weight: .bold)).foregroundColor(WeTheme.codeTextPrimary)
                }.padding(.horizontal, 12)
            }.frame(height: 50).background(WeTheme.codeBackground)
            Divider().overlay(WeTheme.codeSeparator)

            ScrollViewReader { proxy in
                ScrollView {
                    LazyVStack(spacing: 16) {
                        ForEach(messages) { msg in
                            switch msg.type {
                            case "time":
                                Text(msg.text).font(.system(size: 12)).foregroundColor(WeTheme.codeTextHint).frame(maxWidth: .infinity)
                            case "sent":
                                HStack(alignment: .top, spacing: 10) {
                                    Spacer()
                                    Text(msg.text).font(.system(size: 16)).padding(12).padding(.vertical, -2)
                                        .frame(minHeight: 40)
                                        .background(WeTheme.codeBubbleSent).cornerRadius(4)
                                        .foregroundColor(WeTheme.codeTextPrimary)
                                    UserAvatarView(avatarImage: avatarImage, size: 40, cornerRadius: 4, defaultEmoji: "🐱", defaultEmojiSize: 20)
                                }
                            case "received":
                                HStack(alignment: .top, spacing: 10) {
                                    RoundedRectangle(cornerRadius: 4).fill(WeTheme.codeBackgroundCell).frame(width: 40, height: 40)
                                        .overlay(Text(msg.avatar.isEmpty ? "👤" : msg.avatar).font(.system(size: 20)))
                                    VStack(alignment: .leading, spacing: 2) {
                                        if isGroup { Text(msg.name.isEmpty ? chatName : msg.name).font(.system(size: 11)).foregroundColor(WeTheme.codeTextHint) }
                                        Text(msg.text).font(.system(size: 16)).padding(12).padding(.vertical, -2)
                                            .frame(minHeight: 40)
                                            .background(WeTheme.codeBubbleReceived).cornerRadius(4)
                                            .foregroundColor(WeTheme.codeTextPrimary)
                                    }
                                    Spacer()
                                }
                            default: EmptyView()
                            }
                        }
                    }.padding(.horizontal, 16).padding(.vertical, 16)
                }
            }

            Divider().overlay(WeTheme.codeSeparator)
            HStack(spacing: 12) {
                WeIcon(name: "ic_chat_voice", fallback: "🎙️", size: 28, color: WeTheme.codeTextPrimary)
                TextField("", text: $inputText)
                    .textFieldStyle(PlainTextFieldStyle())
                    .padding(8)
                    .background(WeTheme.codeBackgroundCell)
                    .cornerRadius(4)
                    .frame(height: 36)
                    .font(.system(size: 16))
                    .foregroundColor(WeTheme.codeTextPrimary)
                WeIcon(name: "ic_chat_emoji", fallback: "😊", size: 28, color: WeTheme.codeTextPrimary)
                
                if !inputText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                    Button(action: sendMessage) {
                        Text("发送").font(.system(size: 14, weight: .medium))
                            .foregroundColor(.white)
                            .frame(width: 56, height: 32)
                            .background(WeTheme.codeBrandGreen)
                            .cornerRadius(4)
                    }
                } else {
                    WeIcon(name: "ic_chat_add", fallback: "⊕", size: 28, color: WeTheme.codeTextPrimary)
                }
            }.padding(10).background(WeTheme.codeBackground)
        }.background(WeTheme.codeBackground)
        .onAppear { loadMessages() }
    }

    private func loadMessages() {
        let dbMessages = ChatDbHelper.shared.getMessages(contactId: chatId)
        
        if dbMessages.isEmpty {
            messages = wxChatMessages[chatId] ?? [
                WXChatMessage(type: "time", text: "今天 12:00"),
                WXChatMessage(type: "received", name: conv?.name ?? "对方", avatar: conv?.avatar ?? "👤", text: "你好！"),
            ]
        } else {
            messages = [WXChatMessage(type: "time", text: "历史消息")]
            for msg in dbMessages {
                if msg.isSent {
                    messages.append(WXChatMessage(type: "sent", text: msg.message))
                } else {
                    messages.append(WXChatMessage(type: "received", name: conv?.name ?? "对方", avatar: conv?.avatar ?? "👤", text: msg.message))
                }
            }
        }
    }

    private func sendMessage() {
        let trimmed = inputText.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return }
        
        // 保存用户消息到数据库
        ChatDbHelper.shared.addMessage(contactId: chatId, isSent: true, message: trimmed)
        messages.append(WXChatMessage(type: "sent", text: trimmed))
        inputText = ""
        
        // 获取AI角色和用户人设
        var aiPersona = ""
        var userPersona = ""
        
        // 尝试从联系人数据库获取AI角色人设
        if let contact = ContactDbHelper.shared.getContactById(chatId) {
            aiPersona = contact.persona
        }
        
        // 获取当前用户人设
        let userProfile = UserProfileManager.shared.getUserProfile()
        userPersona = userProfile.signature
        
        // 获取默认的聊天API预设
        let chatPresets = ApiPresetManager.shared.getPresetsByType("chat")
        guard let preset = chatPresets.first else {
            // 如果没有配置API预设，使用模拟回复
            DispatchQueue.main.asyncAfter(deadline: .now() + 1.0) {
                let mockResponse = "这是模拟回复：\(trimmed)"
                ChatDbHelper.shared.addMessage(contactId: self.chatId, isSent: false, message: mockResponse)
                self.messages.append(WXChatMessage(type: "received", name: self.conv?.name ?? "对方", avatar: self.conv?.avatar ?? "👤", text: mockResponse))
            }
            return
        }
        
        // 显示加载状态
        messages.append(WXChatMessage(type: "received", name: conv?.name ?? "对方", avatar: conv?.avatar ?? "👤", text: "思考中..."))
        
        // 调用LLM API
        LlmApiService.shared.callLlmApi(
            preset: preset,
            userMessage: trimmed,
            aiPersona: aiPersona,
            userPersona: userPersona
        ) { [weak self] response in
            guard let self = self else { return }
            
            // 移除"思考中..."消息
            if self.messages.last?.text == "思考中..." {
                self.messages.removeLast()
            }
            
            if response.isError {
                // 显示错误消息
                let errorMessage = "API错误: \(response.errorMessage ?? "未知错误")"
                ChatDbHelper.shared.addMessage(contactId: self.chatId, isSent: false, message: errorMessage)
                self.messages.append(WXChatMessage(type: "received", name: self.conv?.name ?? "对方", avatar: self.conv?.avatar ?? "👤", text: errorMessage))
            } else {
                // 保存AI回复到数据库并显示
                ChatDbHelper.shared.addMessage(contactId: self.chatId, isSent: false, message: response.content)
                self.messages.append(WXChatMessage(type: "received", name: self.conv?.name ?? "对方", avatar: self.conv?.avatar ?? "👤", text: response.content))
            }
        }
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

// MARK: - Contact Detail
struct ContactDetailView: View {
    let contactId: String
    var onBack: () -> Void
    var onSendMessage: (String) -> Void
    
    private var contact: WXContact {
        (wxStarred + wxContactList).first { $0.id == contactId } ?? WXContact(id: contactId, name: "未知联系人", avatar: "👤")
    }
    
    var body: some View {
        VStack(spacing: 0) {
            ZStack {
                Text("通讯录").font(.system(size: 17, weight: .semibold)).foregroundColor(WeTheme.codeTextPrimary)
                HStack {
                    WeIcon(name: "ic_nav_back", fallback: "‹", size: 24, color: WeTheme.codeTextPrimary)
                        .onTapGesture(perform: onBack)
                    Spacer()
                    Text("···").font(.system(size: 20, weight: .bold)).foregroundColor(WeTheme.codeTextPrimary)
                }.padding(.horizontal, 12)
            }.frame(height: 50).background(WeTheme.codeBackground)
            Divider().overlay(WeTheme.codeSeparator)
            
            ScrollView {
                VStack(spacing: 0) {
                    // 头像 + 昵称 + 微信号 + 地区
                    HStack(alignment: .top, spacing: 16) {
                        RoundedRectangle(cornerRadius: 8).fill(WeTheme.codeBackground).frame(width: 64, height: 64)
                            .overlay(Text(contact.avatar).font(.system(size: 36)))
                        VStack(alignment: .leading, spacing: 6) {
                            Text(contact.name).font(.system(size: 22, weight: .bold)).foregroundColor(WeTheme.codeTextPrimary)
                            Text("微信号：\(contact.id)").font(.system(size: 14)).foregroundColor(WeTheme.codeTextSecondary)
                            Text("地区：北京 朝阳").font(.system(size: 14)).foregroundColor(WeTheme.codeTextSecondary)
                        }
                        Spacer()
                    }.padding(16).background(WeTheme.codeBackgroundCell)
                    
                    Color(red: 0.93, green: 0.93, blue: 0.93).frame(height: 8)
                    
                    // 详细资料
                    HStack {
                        Text("详细资料").font(.system(size: 16)).foregroundColor(WeTheme.codeTextPrimary)
                        Spacer()
                        Text("›").foregroundColor(Color(white: 0.75)).font(.system(size: 16))
                    }.padding(16).background(WeTheme.codeBackgroundCell)
                    
                    Color(red: 0.93, green: 0.93, blue: 0.93).frame(height: 8)
                    
                    // 朋友圈
                    HStack {
                        Text("朋友圈").font(.system(size: 16)).foregroundColor(WeTheme.codeTextPrimary)
                        Spacer()
                        Text("›").foregroundColor(Color(white: 0.75)).font(.system(size: 16))
                    }.padding(16).background(WeTheme.codeBackgroundCell)
                    
                    Color(red: 0.93, green: 0.93, blue: 0.93).frame(height: 8)
                    
                    // 发消息 + 音视频通话
                    VStack(spacing: 0) {
                        Button(action: {
                            let conv = wxConversations.first { $0.name == contact.name }
                            onSendMessage(conv?.id ?? "u_\(contact.id)")
                        }) {
                            Text("发消息").font(.system(size: 16)).foregroundColor(WeTheme.codeTextPrimary)
                                .frame(maxWidth: .infinity).padding(16).background(WeTheme.codeBackgroundCell)
                        }.buttonStyle(PlainButtonStyle())
                        
                        Divider().overlay(WeTheme.codeSeparator).padding(.leading, 16)
                        
                        Text("音视频通话").font(.system(size: 16)).foregroundColor(WeTheme.codeTextPrimary)
                            .frame(maxWidth: .infinity).padding(16).background(WeTheme.codeBackgroundCell)
                    }
                }
                
                // MARK: - Contact Selection View
                struct ContactSelectionView: View {
                    var onBack: () -> Void
                    var onContactsSelected: (String, String) -> Void
                    
                    @State private var selectedContact1: String? = nil
                    @State private var selectedContact2: String? = nil
                    @State private var allContacts: [ContactInfo] = []
                    
                    var body: some View {
                        VStack(spacing: 0) {
                            // 导航栏
                            ZStack {
                                Text("选择联系人").font(.system(size: 17, weight: .semibold)).foregroundColor(WeTheme.codeTextPrimary)
                                HStack {
                                    WeIcon(name: "ic_nav_back", fallback: "‹", size: 24, color: WeTheme.codeTextPrimary)
                                        .onTapGesture(perform: onBack)
                                    Spacer()
                                }.padding(.horizontal, 12)
                            }.frame(height: 50).background(WeTheme.codeBackground)
                            Divider().overlay(WeTheme.codeSeparator)
                            
                            // 提示信息
                            VStack(alignment: .leading, spacing: 8) {
                                Text("请选择两个联系人：").font(.system(size: 14, weight: .medium)).foregroundColor(WeTheme.codeTextPrimary)
                                Text("1. 第一个联系人：AI角色人设").font(.system(size: 13)).foregroundColor(WeTheme.codeTextSecondary)
                                Text("2. 第二个联系人：用户人设").font(.system(size: 13)).foregroundColor(WeTheme.codeTextSecondary)
                            }.frame(maxWidth: .infinity, alignment: .leading).padding(16).background(WeTheme.codeBackgroundCell)
                            
                            Color(red: 0.93, green: 0.93, blue: 0.93).frame(height: 8)
                            
                            // 已选择的联系人显示
                            if selectedContact1 != nil || selectedContact2 != nil {
                                VStack(alignment: .leading, spacing: 8) {
                                    if let contact1Id = selectedContact1,
                                       let contact1 = allContacts.first(where: { $0.id == contact1Id }) {
                                        HStack {
                                            Text("角色人设：").font(.system(size: 14)).foregroundColor(WeTheme.codeTextSecondary)
                                            Text(contact1.nickname).font(.system(size: 14, weight: .medium)).foregroundColor(WeTheme.codeBrandGreen)
                                        }
                                    }
                                    if let contact2Id = selectedContact2,
                                       let contact2 = allContacts.first(where: { $0.id == contact2Id }) {
                                        HStack {
                                            Text("用户人设：").font(.system(size: 14)).foregroundColor(WeTheme.codeTextSecondary)
                                            Text(contact2.nickname).font(.system(size: 14, weight: .medium)).foregroundColor(WeTheme.codeBrandGreen)
                                        }
                                    }
                                }.frame(maxWidth: .infinity, alignment: .leading).padding(16).background(WeTheme.codeBackgroundCell)
                                
                                Color(red: 0.93, green: 0.93, blue: 0.93).frame(height: 8)
                            }
                            
                            // 联系人列表
                            ScrollView {
                                LazyVStack(spacing: 0) {
                                    var lastLetter = ""
                                    ForEach(allContacts) { contact in
                                        let letter = contact.getPinyinInitial()
                                        if letter != lastLetter {
                                            let _ = { lastLetter = letter }()
                                            Text(letter).font(.system(size: 12)).foregroundColor(WeTheme.codeTextSecondary)
                                                .frame(maxWidth: .infinity, alignment: .leading)
                                                .padding(.horizontal, 16).padding(.vertical, 6)
                                                .background(WeTheme.codeBackground)
                                        }
                                        ContactSelectionRow(
                                            contact: contact,
                                            isSelected: contact.id == selectedContact1 || contact.id == selectedContact2,
                                            selectionLabel: contact.id == selectedContact1 ? "角色" : (contact.id == selectedContact2 ? "用户" : nil),
                                            onClick: {
                                                if selectedContact1 == nil {
                                                    selectedContact1 = contact.id
                                                } else if selectedContact2 == nil && contact.id != selectedContact1 {
                                                    selectedContact2 = contact.id
                                                } else if contact.id == selectedContact1 {
                                                    selectedContact1 = nil
                                                } else if contact.id == selectedContact2 {
                                                    selectedContact2 = nil
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                            
                            // 确认按钮
                            VStack(spacing: 0) {
                                Divider().overlay(WeTheme.codeSeparator)
                                Button(action: {
                                    if let c1 = selectedContact1, let c2 = selectedContact2 {
                                        onContactsSelected(c1, c2)
                                    }
                                }) {
                                    Text("确定").font(.system(size: 16, weight: .medium)).foregroundColor(.white)
                                        .frame(maxWidth: .infinity).frame(height: 48)
                                        .background(selectedContact1 != nil && selectedContact2 != nil ? WeTheme.codeBrandGreen : Color(hex: 0xCCCCCC))
                                        .cornerRadius(8)
                                }.disabled(selectedContact1 == nil || selectedContact2 == nil)
                                .padding(16).background(WeTheme.codeBackgroundCell)
                            }
                        }.background(WeTheme.codeBackground)
                        .onAppear {
                            allContacts = ContactDbHelper.shared.getAllContacts()
                        }
                    }
                }
                
                struct ContactSelectionRow: View {
                    let contact: ContactInfo
                    let isSelected: Bool
                    let selectionLabel: String?
                    let onClick: () -> Void
                    
                    var body: some View {
                        Button(action: onClick) {
                            HStack(spacing: 16) {
                                // 头像
                                if let avatarFileName = contact.avatarFileName,
                                   let avatarPath = ContactDbHelper.shared.getAvatarFilePath(avatarFileName),
                                   let uiImage = UIImage(contentsOfFile: avatarPath) {
                                    Image(uiImage: uiImage)
                                        .resizable()
                                        .aspectRatio(contentMode: .fill)
                                        .frame(width: 36, height: 36)
                                        .clipShape(RoundedRectangle(cornerRadius: 4))
                                } else {
                                    RoundedRectangle(cornerRadius: 4)
                                        .fill(Color(red: 0.96, green: 0.96, blue: 0.86))
                                        .frame(width: 36, height: 36)
                                        .overlay(Text(contact.persona.first.map(String.init) ?? "👤").font(.system(size: 22)))
                                }
                                
                                Text(contact.nickname).font(.system(size: 16)).foregroundColor(WeTheme.codeTextPrimary)
                                
                                Spacer()
                                
                                // 选择标签
                                if isSelected, let label = selectionLabel {
                                    Text(label).font(.system(size: 12, weight: .medium)).foregroundColor(.white)
                                        .padding(.horizontal, 8).padding(.vertical, 4)
                                        .background(WeTheme.codeBrandGreen).cornerRadius(4)
                                }
                            }.padding(.horizontal, 16).padding(.vertical, 10).background(WeTheme.codeBackgroundCell)
                        }.buttonStyle(PlainButtonStyle())
                        Divider().overlay(WeTheme.codeSeparator).padding(.leading, 68)
                    }
                }
            }
        }.background(WeTheme.codeBackground)
    }
}
