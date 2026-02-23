import SwiftUI

struct PersonaBuilderChatView: View {
    let cardId: Int64
    let onBack: () -> Void
    
    @State private var personaCard: PersonaCard?
    @State private var messages: [PersonaMessage] = []
    @State private var inputText = ""
    @State private var isLoading = false
    @State private var streamingContent = ""
    @State private var selectedMessage: PersonaMessage?
    @State private var showActionSheet = false
    @State private var editingMessage: PersonaMessage?
    @State private var editingText = ""
    @State private var showEditDialog = false
    @State private var showBacktrackAlert = false
    @State private var pendingBacktrackMessage: PersonaMessage?
    
    @AppStorage("backtrack_warning_shown") private var hasShownBacktrackWarning = false
    
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
                                .contentShape(Rectangle())
                                .simultaneousGesture(LongPressGesture(minimumDuration: 0.35).onEnded { _ in
                                    selectedMessage = message
                                    showActionSheet = true
                                })
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
        .confirmationDialog("消息操作", isPresented: $showActionSheet, presenting: selectedMessage) { message in
            Button("复制") {
                UIPasteboard.general.string = message.content
            }
            Button("编辑") {
                editingMessage = message
                editingText = message.content
                showEditDialog = true
            }
            Button("回溯") {
                if hasShownBacktrackWarning {
                    executeBacktrack(message: message)
                } else {
                    pendingBacktrackMessage = message
                    showBacktrackAlert = true
                }
            }
            Button("取消", role: .cancel) {}
        }
        .alert("编辑消息", isPresented: $showEditDialog) {
            TextField("消息内容", text: $editingText, axis: .vertical)
                .lineLimit(3...8)
            Button("保存") {
                guard let message = editingMessage, !editingText.trimmingCharacters(in: .whitespaces).isEmpty else { return }
                if dbHelper.updateMessageContent(cardId: cardId, messageId: message.id, newContent: editingText) {
                    messages = dbHelper.getMessages(cardId: cardId)
                }
                editingMessage = nil
            }
            Button("取消", role: .cancel) {
                editingMessage = nil
            }
        }
        .alert("首次回溯提示", isPresented: $showBacktrackAlert) {
            Button("继续") {
                hasShownBacktrackWarning = true
                if let message = pendingBacktrackMessage {
                    executeBacktrack(message: message)
                }
                pendingBacktrackMessage = nil
            }
            Button("取消", role: .cancel) {
                pendingBacktrackMessage = nil
            }
        } message: {
            Text("回溯会清除该消息之后的所有消息，并从该消息作为最后一条重新生成回复。")
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
                
                let conversationHistory = messages.map {
                    ["role": $0.role, "content": sanitizeMessageForHistory($0.content)]
                }
                
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
    
    private func executeBacktrack(message: PersonaMessage) {
        guard !isLoading else { return }
        
        Task {
            isLoading = true
            streamingContent = ""
            
            do {
                guard let card = personaCard else { return }
                guard let preset = presetDbHelper.getPresetById(card.apiPresetId) else { return }
                
                dbHelper.deleteMessagesAfter(cardId: cardId, messageId: message.id)
                messages = dbHelper.getMessages(cardId: cardId)
                
                let systemPrompt: String
                if let promptPath = Bundle.main.path(forResource: "角色人设设计", ofType: "txt", inDirectory: "prompt"),
                   let promptContent = try? String(contentsOfFile: promptPath, encoding: .utf8) {
                    systemPrompt = promptContent
                } else {
                    systemPrompt = "你是一个专业的角色人设构建助手，帮助用户通过对话构建完整的角色人设。"
                }
                
                let conversationHistory = messages.map {
                    ["role": $0.role, "content": sanitizeMessageForHistory($0.content)]
                }
                
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
                print("回溯失败: \(error)")
            }
            
            isLoading = false
        }
    }
}

private struct ParsedPersonaContent {
    let mainText: String
    let thoughtText: String
}

private func normalizeMarkdownForDisplay(_ raw: String) -> String {
    if raw.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
        return raw
    }

    var result = raw
    if let regex = try? NSRegularExpression(pattern: #"\*\*\s+(.+?)\s+\*\*"#, options: []) {
        let nsText = result as NSString
        let matches = regex.matches(in: result, options: [], range: NSRange(location: 0, length: nsText.length))
        for match in matches.reversed() {
            guard match.numberOfRanges > 1 else { continue }
            let inner = nsText.substring(with: match.range(at: 1)).trimmingCharacters(in: .whitespacesAndNewlines)
            if inner.isEmpty { continue }
            result = (result as NSString).replacingCharacters(in: match.range, with: "**\(inner)**")
        }
    }

    let normalizedLines = result.components(separatedBy: .newlines).map { line -> String in
        let prefix = String(line.prefix { $0 == " " || $0 == "\t" })
        let content = String(line.dropFirst(prefix.count))
        if content.hasPrefix("•") || content.hasPrefix("·") {
            let body = String(content.dropFirst()).trimmingCharacters(in: .whitespaces)
            return "\(prefix)● \(body)"
        }
        return line
    }
    return normalizedLines.joined(separator: "\n")
}

private struct MarkdownTextBlock: View {
    let markdown: String
    let color: Color
    var font: Font = .body

    var body: some View {
        let normalized = normalizeMarkdownForDisplay(markdown)
        Group {
            if let attributed = try? AttributedString(
                markdown: normalized,
                options: AttributedString.MarkdownParsingOptions(interpretedSyntax: .full)
            ) {
                Text(attributed)
            } else {
                Text(normalized)
            }
        }
        .font(font)
        .foregroundColor(color)
        .fixedSize(horizontal: false, vertical: true)
    }
}

private func sanitizeMessageForHistory(_ raw: String) -> String {
    let parsed = parsePersonaContent(raw)
    if !parsed.mainText.isEmpty {
        return parsed.mainText
    }
    if !parsed.thoughtText.isEmpty {
        return parsed.thoughtText
    }
    return raw.trimmingCharacters(in: .whitespacesAndNewlines)
}

private func parsePersonaContent(_ raw: String) -> ParsedPersonaContent {
    if raw.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
        return ParsedPersonaContent(mainText: "", thoughtText: "")
    }

    let normalized = raw
        .replacingOccurrences(of: "<thinking>", with: "<think>", options: .caseInsensitive)
        .replacingOccurrences(of: "</thinking>", with: "</think>", options: .caseInsensitive)

    var main = ""
    var thought = ""
    var cursor = normalized.startIndex

    while cursor < normalized.endIndex {
        guard let openRange = normalized.range(of: "<think>", options: .caseInsensitive, range: cursor..<normalized.endIndex) else {
            main += String(normalized[cursor..<normalized.endIndex])
            break
        }

        main += String(normalized[cursor..<openRange.lowerBound])
        let thoughtStart = openRange.upperBound
        guard let closeRange = normalized.range(of: "</think>", options: .caseInsensitive, range: thoughtStart..<normalized.endIndex) else {
            thought += String(normalized[thoughtStart..<normalized.endIndex])
            cursor = normalized.endIndex
            break
        }

        let chunk = String(normalized[thoughtStart..<closeRange.lowerBound]).trimmingCharacters(in: .whitespacesAndNewlines)
        if !chunk.isEmpty {
            if !thought.isEmpty {
                thought += "\n"
            }
            thought += chunk
        }
        cursor = closeRange.upperBound
    }

    return ParsedPersonaContent(
        mainText: main.trimmingCharacters(in: .whitespacesAndNewlines),
        thoughtText: thought.trimmingCharacters(in: .whitespacesAndNewlines)
    )
}

struct MessageBubble: View {
    let message: PersonaMessage
    @State private var thoughtExpanded = false

    private var parsed: ParsedPersonaContent {
        parsePersonaContent(message.content)
    }
    
    var body: some View {
        HStack(alignment: .top) {
            if message.role == "user" {
                Spacer(minLength: 40)
            }

            VStack(alignment: .leading, spacing: 8) {
                if message.role == "assistant", !parsed.thoughtText.isEmpty {
                    Button {
                        thoughtExpanded.toggle()
                    } label: {
                        HStack(spacing: 8) {
                            Image(systemName: "lightbulb")
                                .font(.system(size: 15, weight: .semibold))
                                .foregroundColor(.secondary)
                            Text(thoughtExpanded ? "思考中（点击收起）" : "已深度思考（点击展开）")
                                .font(.subheadline.weight(.semibold))
                                .foregroundColor(.primary)
                            Spacer(minLength: 6)
                            Image(systemName: thoughtExpanded ? "chevron.up" : "chevron.down")
                                .font(.caption.weight(.semibold))
                                .foregroundColor(.secondary)
                        }
                        .padding(.horizontal, 10)
                        .padding(.vertical, 8)
                        .background(Color(UIColor.systemGray6))
                        .overlay(
                            RoundedRectangle(cornerRadius: 10)
                                .stroke(Color(UIColor.separator).opacity(0.5), lineWidth: 0.8)
                        )
                        .cornerRadius(10)
                    }
                    .buttonStyle(.plain)

                    if thoughtExpanded {
                        MarkdownTextBlock(
                            markdown: parsed.thoughtText,
                            color: .secondary,
                            font: .subheadline
                        )
                        .padding(.horizontal, 10)
                        .padding(.vertical, 8)
                        .background(Color(UIColor.systemGray6).opacity(0.8))
                        .overlay(
                            RoundedRectangle(cornerRadius: 10)
                                .stroke(Color(UIColor.separator).opacity(0.35), lineWidth: 0.8)
                        )
                        .cornerRadius(10)
                        Divider()
                    }
                }

                let displayMain = parsed.mainText.isEmpty && !parsed.thoughtText.isEmpty
                    ? "（仅包含思维链，展开可查看）"
                    : parsed.mainText

                if !displayMain.isEmpty {
                    MarkdownTextBlock(
                        markdown: displayMain,
                        color: message.role == "user" ? .white : .primary,
                        font: .body
                    )
                }
            }
            .padding(12)
            .background(message.role == "user" ? Color.blue : Color(UIColor.secondarySystemBackground))
            .foregroundColor(message.role == "user" ? .white : .primary)
            .cornerRadius(12)
            .frame(maxWidth: 280, alignment: message.role == "user" ? .trailing : .leading)
            
            if message.role == "assistant" {
                Spacer(minLength: 40)
            }
        }
    }
}
