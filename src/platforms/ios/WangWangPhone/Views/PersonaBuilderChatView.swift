import SwiftUI

struct PersonaBuilderChatView: View {
    let cardId: Int64
    let onBack: () -> Void
    
    @State private var personaCard: PersonaCard?
    @State private var messages: [PersonaMessage] = []
    @State private var inputText = ""
    @State private var isLoading = false
    @State private var streamingContent = ""
    
    private let dbHelper = PersonaCardDbHelper()
    private let presetDbHelper = ApiPresetDbHelper()
    
    var body: some View {
        VStack(spacing: 0) {
            // 顶部导航栏
            HStack {
                Button {
                    onBack()
                } label: {
                    HStack(spacing: 4) {
                        Image(systemName: "chevron.left")
                        Text("返回")
                    }
                    .foregroundColor(.blue)
                }
                
                Spacer()
                
                Text(personaCard?.name ?? "加载中...")
                    .font(.headline)
                
                Spacer()
                
                // 占位，保持标题居中
                HStack(spacing: 4) {
                    Image(systemName: "chevron.left")
                    Text("返回")
                }
                .opacity(0)
            }
            .padding()
            .background(Color(UIColor.systemBackground))
            
            Divider()
            
            // 消息列表
            ScrollViewReader { proxy in
                ScrollView {
                    LazyVStack(spacing: 12) {
                        ForEach(messages, id: \.id) { message in
                            MessageBubble(message: message)
                                .id(message.id)
                        }
                        
                        // 流式响应中的消息
                        if !streamingContent.isEmpty {
                            MessageBubble(message: PersonaMessage(
                                id: -1,
                                cardId: cardId,
                                role: "assistant",
                                content: streamingContent,
                                timestamp: Int64(Date().timeIntervalSince1970 * 1000)
                            ))
                            .id(-1)
                        }
                        
                        if isLoading && streamingContent.isEmpty {
                            HStack {
                                HStack(spacing: 8) {
                                    ProgressView()
                                        .scaleEffect(0.8)
                                    Text("正在思考...")
                                        .font(.subheadline)
                                        .foregroundColor(.secondary)
                                }
                                .padding(12)
                                .background(Color(UIColor.secondarySystemBackground))
                                .cornerRadius(12)
                                
                                Spacer()
                            }
                            .padding(.horizontal)
                        }
                    }
                    .padding()
                }
                .onChange(of: messages.count) { _ in
                    if let lastMessage = messages.last {
                        withAnimation {
                            proxy.scrollTo(lastMessage.id, anchor: .bottom)
                        }
                    }
                }
                .onChange(of: streamingContent) { _ in
                    if !streamingContent.isEmpty {
                        withAnimation {
                            proxy.scrollTo(-1, anchor: .bottom)
                        }
                    }
                }
            }
            
            Divider()
            
            // 输入框
            HStack(spacing: 12) {
                TextField("输入消息...", text: $inputText)
                    .textFieldStyle(.roundedBorder)
                    .disabled(isLoading)
                
                Button {
                    sendMessage()
                } label: {
                    Image(systemName: "arrow.up.circle.fill")
                        .font(.system(size: 32))
                        .foregroundColor(inputText.trimmingCharacters(in: .whitespaces).isEmpty || isLoading ? .gray : .blue)
                }
                .disabled(inputText.trimmingCharacters(in: .whitespaces).isEmpty || isLoading)
            }
            .padding()
            .background(Color(UIColor.systemBackground))
        }
        .navigationBarHidden(true)
        .onAppear {
            loadData()
        }
    }
    
    private func loadData() {
        personaCard = dbHelper.getCardById(cardId)
        messages = dbHelper.getMessages(cardId: cardId)
        
        // 如果没有消息，添加欢迎消息
        if messages.isEmpty {
            let welcomeMsg = PersonaMessage(
                id: -1,
                cardId: cardId,
                role: "assistant",
                content: "你好！我是神笔马良，专门帮你构建角色人设。\n\n我会通过几个问题来了解你想创建的角色：\n1. 角色的基本信息（姓名、年龄、职业等）\n2. 性格特点\n3. 说话风格\n4. 背景故事\n\n请告诉我，你想创建什么样的角色？",
                timestamp: Int64(Date().timeIntervalSince1970 * 1000)
            )
            _ = dbHelper.addMessage(welcomeMsg)
            messages = [welcomeMsg]
        }
    }
    
    private func sendMessage() {
        guard !inputText.trimmingCharacters(in: .whitespaces).isEmpty, !isLoading else { return }
        
        let userMessage = inputText.trimmingCharacters(in: .whitespaces)
        inputText = ""
        
        Task {
            isLoading = true
            streamingContent = ""
            
            do {
                guard let card = personaCard else {
                    print("人设卡不存在")
                    return
                }
                
                guard let preset = presetDbHelper.getPresetById(card.apiPresetId) else {
                    print("API预设不存在")
                    return
                }
                
                // 保存用户消息
                let userMsg = PersonaMessage(
                    id: -1,
                    cardId: cardId,
                    role: "user",
                    content: userMessage,
                    timestamp: Int64(Date().timeIntervalSince1970 * 1000)
                )
                _ = dbHelper.addMessage(userMsg)
                messages = dbHelper.getMessages(cardId: cardId)
                
                // 加载系统提示词
                let systemPrompt: String
                if let promptPath = Bundle.main.path(forResource: "角色人设设计", ofType: "txt", inDirectory: "prompt"),
                   let promptContent = try? String(contentsOfFile: promptPath, encoding: .utf8) {
                    systemPrompt = promptContent
                } else {
                    systemPrompt = "你是一个专业的角色人设构建助手，帮助用户通过对话构建完整的角色人设。"
                }
                
                let conversationHistory = messages.map { ["role": $0.role, "content": $0.content] }
                
                // 使用流式API
                let stream = LlmApiService.shared.sendChatRequestStream(
                    preset: preset,
                    messages: conversationHistory,
                    systemPrompt: systemPrompt
                )
                
                for try await chunk in stream {
                    await MainActor.run {
                        streamingContent += chunk
                    }
                }
                
                // 流式响应完成，保存完整消息
                if !streamingContent.isEmpty {
                    let assistantMsg = PersonaMessage(
                        id: -1,
                        cardId: cardId,
                        role: "assistant",
                        content: streamingContent,
                        timestamp: Int64(Date().timeIntervalSince1970 * 1000)
                    )
                    _ = dbHelper.addMessage(assistantMsg)
                    dbHelper.updateCardTimestamp(cardId)
                    messages = dbHelper.getMessages(cardId: cardId)
                    streamingContent = ""
                }
                
            } catch {
                print("发送消息错误: \(error)")
                let errorMsg = PersonaMessage(
                    id: -1,
                    cardId: cardId,
                    role: "assistant",
                    content: "发送失败: \(error.localizedDescription)",
                    timestamp: Int64(Date().timeIntervalSince1970 * 1000)
                )
                _ = dbHelper.addMessage(errorMsg)
                messages = dbHelper.getMessages(cardId: cardId)
                streamingContent = ""
            }
            
            isLoading = false
        }
    }
}

struct MessageBubble: View {
    let message: PersonaMessage
    
    var body: some View {
        HStack {
            if message.role == "user" {
                Spacer()
            }
            
            Text(message.content)
                .padding(12)
                .background(message.role == "user" ? Color.blue : Color(UIColor.secondarySystemBackground))
                .foregroundColor(message.role == "user" ? .white : .primary)
                .cornerRadius(12)
                .frame(maxWidth: 280, alignment: message.role == "user" ? .trailing : .leading)
            
            if message.role == "assistant" {
                Spacer()
            }
        }
    }
}
