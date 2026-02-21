import SwiftUI

struct ApiPresetsView: View {
    @Binding var showApiPresets: Bool
    @Binding var showChatApiPresets: Bool
    @Binding var showImageApiPresets: Bool
    @Binding var showVoiceApiPresets: Bool
    
    var body: some View {
        NavigationView {
            ZStack {
                Color(UIColor.systemGroupedBackground).ignoresSafeArea()
                
                ScrollView {
                    VStack(alignment: .leading, spacing: 20) {
                        Section(header: Text("聊天").font(.caption).foregroundColor(.gray).padding(.horizontal)) {
                            Button(action: { showChatApiPresets = true }) {
                                HStack {
                                    Text("聊天API预设").foregroundColor(.primary)
                                    Spacer()
                                    Image(systemName: "chevron.right").font(.system(size: 14, weight: .semibold)).foregroundColor(.gray)
                                }
                                .padding()
                                .background(Color(UIColor.secondarySystemGroupedBackground))
                            }
                            .cornerRadius(10)
                            .padding(.horizontal)
                        }
                        
                        Section(header: Text("生图").font(.caption).foregroundColor(.gray).padding(.horizontal)) {
                            Button(action: { showImageApiPresets = true }) {
                                HStack {
                                    Text("生图API预设").foregroundColor(.primary)
                                    Spacer()
                                    Image(systemName: "chevron.right").font(.system(size: 14, weight: .semibold)).foregroundColor(.gray)
                                }
                                .padding()
                                .background(Color(UIColor.secondarySystemGroupedBackground))
                            }
                            .cornerRadius(10)
                            .padding(.horizontal)
                        }
                        
                        Section(header: Text("语音").font(.caption).foregroundColor(.gray).padding(.horizontal)) {
                            Button(action: { showVoiceApiPresets = true }) {
                                HStack {
                                    Text("语音API预设").foregroundColor(.primary)
                                    Spacer()
                                    Image(systemName: "chevron.right").font(.system(size: 14, weight: .semibold)).foregroundColor(.gray)
                                }
                                .padding()
                                .background(Color(UIColor.secondarySystemGroupedBackground))
                            }
                            .cornerRadius(10)
                            .padding(.horizontal)
                        }
                    }
                    .padding(.vertical)
                }
            }
            .navigationTitle("API预设")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("返回") { showApiPresets = false }
                }
            }
        }
    }
}

struct ApiPresetListView: View {
    let type: String
    let title: String
    @Binding var showView: Bool
    @State private var presets: [ApiPreset] = []
    @State private var showAddDialog = false
    @State private var editingPreset: ApiPreset?
    @State private var showDeleteConfirm: Int64?
    
    private let apiPresetManager = ApiPresetManager.shared
    
    var body: some View {
        NavigationView {
            ZStack {
                Color(UIColor.systemGroupedBackground).ignoresSafeArea()
                
                if presets.isEmpty {
                    VStack {
                        Spacer()
                        Text("暂无预设，点击右上角添加")
                            .foregroundColor(.gray)
                            .font(.system(size: 14))
                        Spacer()
                    }
                } else {
                    ScrollView {
                        VStack(spacing: 12) {
                            ForEach(presets) { preset in
                                Button(action: { editingPreset = preset }) {
                                    VStack(alignment: .leading, spacing: 8) {
                                        HStack {
                                            Text(preset.name)
                                                .font(.system(size: 16, weight: .semibold))
                                                .foregroundColor(.primary)
                                            Spacer()
                                            Text(preset.provider.uppercased())
                                                .font(.system(size: 12))
                                                .foregroundColor(Color(red: 0.0, green: 0.48, blue: 1.0))
                                        }
                                        Text("模型: \(preset.model)")
                                            .font(.system(size: 12))
                                            .foregroundColor(.gray)
                                        Text("地址: \(preset.baseUrl)")
                                            .font(.system(size: 12))
                                            .foregroundColor(.gray)
                                            .lineLimit(1)
                                    }
                                    .padding()
                                    .frame(maxWidth: .infinity, alignment: .leading)
                                    .background(Color(UIColor.secondarySystemGroupedBackground))
                                    .cornerRadius(10)
                                }
                                .padding(.horizontal)
                            }
                        }
                        .padding(.vertical)
                    }
                }
            }
            .navigationTitle(title)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("返回") { showView = false }
                }
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("添加") { showAddDialog = true }
                }
            }
            .onAppear {
                loadPresets()
            }
            .sheet(isPresented: $showAddDialog) {
                ApiPresetEditView(
                    type: type,
                    preset: nil,
                    onSave: { newPreset in
                        _ = apiPresetManager.savePreset(newPreset)
                        loadPresets()
                        showAddDialog = false
                    },
                    onCancel: { showAddDialog = false }
                )
            }
            .sheet(item: $editingPreset) { preset in
                ApiPresetEditView(
                    type: type,
                    preset: preset,
                    onSave: { updatedPreset in
                        _ = apiPresetManager.savePreset(updatedPreset)
                        loadPresets()
                        editingPreset = nil
                    },
                    onDelete: { id in
                        showDeleteConfirm = id
                    },
                    onCancel: { editingPreset = nil }
                )
            }
            .alert(isPresented: Binding(
                get: { showDeleteConfirm != nil },
                set: { if !$0 { showDeleteConfirm = nil } }
            )) {
                Alert(
                    title: Text("删除预设"),
                    message: Text("确定要删除这个API预设吗？"),
                    primaryButton: .destructive(Text("删除")) {
                        if let id = showDeleteConfirm {
                            _ = apiPresetManager.deletePreset(id)
                            loadPresets()
                            showDeleteConfirm = nil
                            editingPreset = nil
                        }
                    },
                    secondaryButton: .cancel()
                )
            }
        }
    }
    
    private func loadPresets() {
        presets = apiPresetManager.getPresetsByType(type)
    }
}

struct ApiPresetEditView: View {
    let type: String
    let preset: ApiPreset?
    let onSave: (ApiPreset) -> Void
    let onDelete: ((Int64) -> Void)?
    let onCancel: () -> Void
    
    @State private var name: String
    @State private var provider: String
    @State private var apiKey: String
    @State private var baseUrl: String
    @State private var model: String
    @State private var showDeleteConfirm = false
    
    // 参数开关状态
    @State private var streamEnabled = true
    @State private var streamValue = true
    @State private var temperatureEnabled = true
    @State private var temperature = "1.00"
    @State private var maxTokensEnabled = true
    @State private var maxTokens = "64000"
    @State private var topPEnabled = false
    @State private var topP = "1.00"
    @State private var topKEnabled = false
    @State private var topK = "40"
    @State private var thinkingLevelEnabled = false
    @State private var thinkingLevel = "1"
    @State private var thinkingBudgetEnabled = false
    @State private var thinkingBudget = "1000"
    @State private var thinkingEffortEnabled = false
    @State private var thinkingEffort = "medium"
    
    @State private var showModelPicker = false
    @State private var availableModels: [String] = []
    @State private var isLoadingModels = false
    @State private var modelError: String?
    @State private var isTestingConnection = false
    @State private var testSuccess: Bool?
    @State private var testResponse: String?
    
    init(type: String, preset: ApiPreset?, onSave: @escaping (ApiPreset) -> Void, onDelete: ((Int64) -> Void)? = nil, onCancel: @escaping () -> Void) {
        self.type = type
        self.preset = preset
        self.onSave = onSave
        self.onDelete = onDelete
        self.onCancel = onCancel
        
        _name = State(initialValue: preset?.name ?? "")
        _provider = State(initialValue: preset?.provider ?? (type == "voice" ? "minimax" : "openai"))
        _apiKey = State(initialValue: preset?.apiKey ?? "")
        _baseUrl = State(initialValue: preset?.baseUrl ?? "")
        _model = State(initialValue: preset?.model ?? "")
    }
    
    var body: some View {
        NavigationView {
            Form {
                Section(header: Text("基本信息")) {
                    TextField("预设名称", text: $name)
                }
                
                Section(header: Text("提供商")) {
                    if type != "voice" {
                        Picker("提供商", selection: $provider) {
                            Text("OpenAI").tag("openai")
                            Text("Gemini").tag("gemini")
                        }
                        .pickerStyle(SegmentedPickerStyle())
                        .onChange(of: provider) { _ in
                            baseUrl = ""
                            model = ""
                        }
                    } else {
                        HStack {
                            Text("提供商")
                            Spacer()
                            Text("Minimax").foregroundColor(.gray)
                        }
                    }
                }
                
                Section(header: Text("API配置")) {
                    ZStack(alignment: .leading) {
                        if apiKey.isEmpty {
                            Text("sk-...")
                                .foregroundColor(.gray.opacity(0.5))
                                .padding(.leading, 4)
                        }
                        TextField("API Key", text: $apiKey)
                            .autocapitalization(.none)
                    }
                    
                    ZStack(alignment: .leading) {
                        if baseUrl.isEmpty {
                            Text(getDefaultBaseUrl(provider: provider, type: type))
                                .foregroundColor(.gray.opacity(0.5))
                                .padding(.leading, 4)
                        }
                        TextField("Base URL", text: $baseUrl)
                            .autocapitalization(.none)
                            .keyboardType(.URL)
                    }
                    
                    HStack {
                        ZStack(alignment: .leading) {
                            if model.isEmpty {
                                Text(getDefaultModel(provider: provider, type: type))
                                    .foregroundColor(.gray.opacity(0.5))
                                    .padding(.leading, 4)
                            }
                            TextField("模型名称", text: $model)
                        }
                        Button(action: {
                            if apiKey.isEmpty {
                                modelError = "请先填写API Key"
                                return
                            }
                            let finalBaseUrl = baseUrl.isEmpty ? getDefaultBaseUrl(provider: provider, type: type) : baseUrl
                            isLoadingModels = true
                            modelError = nil
                            LlmApiService.fetchModels(
                                provider: provider,
                                apiKey: apiKey,
                                baseUrl: finalBaseUrl
                            ) { models in
                                isLoadingModels = false
                                if models.isEmpty {
                                    modelError = "未获取到模型列表"
                                } else {
                                    availableModels = models
                                    showModelPicker = true
                                }
                            }
                        }) {
                            if isLoadingModels {
                                ProgressView()
                                    .scaleEffect(0.7)
                            } else {
                                Text("拉取模型")
                            }
                        }
                        .font(.caption)
                        .disabled(isLoadingModels || apiKey.isEmpty)
                    }
                    
                    if let error = modelError {
                        Text(error)
                            .font(.caption)
                            .foregroundColor(.red)
                    }
                }
                
                Section(header: Text("模型参数")) {
                    HStack {
                        Text("流式输出")
                        Spacer()
                        if streamEnabled {
                            Picker("", selection: $streamValue) {
                                Text("True").tag(true)
                                Text("False").tag(false)
                            }
                            .pickerStyle(MenuPickerStyle())
                            .frame(width: 100)
                        }
                        Toggle("", isOn: $streamEnabled)
                            .labelsHidden()
                    }
                    
                    ParameterToggleRow(
                        label: "温度 (0-2)",
                        isEnabled: $temperatureEnabled,
                        value: $temperature,
                        keyboardType: .decimalPad
                    )
                    
                    ParameterToggleRow(
                        label: "最大令牌数",
                        isEnabled: $maxTokensEnabled,
                        value: $maxTokens,
                        keyboardType: .numberPad
                    )
                    
                    ParameterToggleRow(
                        label: "Top P (0-1)",
                        isEnabled: $topPEnabled,
                        value: $topP,
                        keyboardType: .decimalPad
                    )
                    
                    ParameterToggleRow(
                        label: "Top K",
                        isEnabled: $topKEnabled,
                        value: $topK,
                        keyboardType: .numberPad
                    )
                    
                    ParameterToggleRow(
                        label: "思考级别",
                        isEnabled: $thinkingLevelEnabled,
                        value: $thinkingLevel,
                        keyboardType: .numberPad
                    )
                    
                    ParameterToggleRow(
                        label: "思考预算",
                        isEnabled: $thinkingBudgetEnabled,
                        value: $thinkingBudget,
                        keyboardType: .numberPad
                    )
                    
                    HStack {
                        Text("思考努力度")
                        Spacer()
                        if thinkingEffortEnabled {
                            Picker("", selection: $thinkingEffort) {
                                Text("minimal").tag("minimal")
                                Text("low").tag("low")
                                Text("medium").tag("medium")
                                Text("high").tag("high")
                                Text("max").tag("max")
                            }
                            .pickerStyle(MenuPickerStyle())
                            .frame(width: 120)
                        }
                        Toggle("", isOn: $thinkingEffortEnabled)
                            .labelsHidden()
                    }
                }
                
                Section {
                    Button(action: {
                        let finalBaseUrl = baseUrl.isEmpty ? getDefaultBaseUrl(provider: provider, type: type) : baseUrl
                        let finalModel = model.isEmpty ? getDefaultModel(provider: provider, type: type) : model
                        let extraParams = buildExtraParams()
                        
                        let testPreset = ApiPreset(
                            id: preset?.id ?? 0,
                            name: name,
                            type: type,
                            provider: provider,
                            apiKey: apiKey,
                            baseUrl: finalBaseUrl,
                            model: finalModel,
                            extraParams: extraParams
                        )
                        
                        isTestingConnection = true
                        testSuccess = nil
                        testResponse = nil
                        LlmApiService.shared.testConnection(preset: testPreset) { success, response in
                            isTestingConnection = false
                            testSuccess = success
                            testResponse = response
                        }
                    }) {
                        HStack {
                            if isTestingConnection {
                                ProgressView()
                                    .scaleEffect(0.8)
                                Text("测试中...")
                                    .foregroundColor(.primary)
                            } else {
                                Text("测试连通性")
                                    .foregroundColor(name.isEmpty || apiKey.isEmpty ? .gray : .blue)
                            }
                        }
                        .frame(maxWidth: .infinity, alignment: .center)
                    }
                    .disabled(name.isEmpty || apiKey.isEmpty || isTestingConnection)
                    
                    if let success = testSuccess {
                        VStack(spacing: 8) {
                            Text(success ? "✓ 连接成功" : "✗ 连接失败")
                                .font(.caption)
                                .foregroundColor(success ? .green : .red)
                                .frame(maxWidth: .infinity, alignment: .center)
                            
                            if let response = testResponse {
                                ScrollView {
                                    Text(response)
                                        .font(.caption)
                                        .foregroundColor(.primary)
                                        .padding(8)
                                        .frame(maxWidth: .infinity, alignment: .leading)
                                        .background(Color(UIColor.secondarySystemGroupedBackground))
                                        .cornerRadius(8)
                                }
                                .frame(maxHeight: 150)
                            }
                        }
                    }
                    
                    Button(action: {
                        let finalBaseUrl = baseUrl.isEmpty ? getDefaultBaseUrl(provider: provider, type: type) : baseUrl
                        let finalModel = model.isEmpty ? getDefaultModel(provider: provider, type: type) : model
                        let extraParams = buildExtraParams()
                        
                        let newPreset = ApiPreset(
                            id: preset?.id ?? 0,
                            name: name,
                            type: type,
                            provider: provider,
                            apiKey: apiKey,
                            baseUrl: finalBaseUrl,
                            model: finalModel,
                            extraParams: extraParams
                        )
                        onSave(newPreset)
                    }) {
                        Text("保存预设")
                            .foregroundColor(.white)
                            .frame(maxWidth: .infinity, alignment: .center)
                            .padding(.vertical, 8)
                            .background(name.isEmpty || apiKey.isEmpty ? Color.gray : Color.blue)
                            .cornerRadius(8)
                    }
                    .disabled(name.isEmpty || apiKey.isEmpty)
                }
                
                if preset != nil && onDelete != nil {
                    Section {
                        Button(action: {
                            showDeleteConfirm = true
                        }) {
                            Text("删除预设")
                                .foregroundColor(.red)
                                .frame(maxWidth: .infinity, alignment: .center)
                        }
                    }
                }
            }
            .navigationTitle(preset == nil ? "添加预设" : "编辑预设")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("取消") { onCancel() }
                }
            }
            .alert(isPresented: $showDeleteConfirm) {
                Alert(
                    title: Text("删除预设"),
                    message: Text("确定要删除这个API预设吗？"),
                    primaryButton: .destructive(Text("删除")) {
                        if let id = preset?.id {
                            onDelete?(id)
                        }
                    },
                    secondaryButton: .cancel()
                )
            }
            .sheet(isPresented: $showModelPicker) {
                NavigationView {
                    List {
                        ForEach(availableModels, id: \.self) { modelName in
                            Button(action: {
                                model = modelName
                                showModelPicker = false
                            }) {
                                HStack {
                                    Text(modelName)
                                        .foregroundColor(.primary)
                                    Spacer()
                                    if model == modelName {
                                        Image(systemName: "checkmark")
                                            .foregroundColor(.blue)
                                    }
                                }
                            }
                        }
                    }
                    .navigationTitle("选择模型")
                    .navigationBarTitleDisplayMode(.inline)
                    .toolbar {
                        ToolbarItem(placement: .navigationBarTrailing) {
                            Button("取消") {
                                showModelPicker = false
                            }
                        }
                    }
                }
            }
        }
    }
    
    private func buildExtraParams() -> String {
        var params: [String: Any] = [:]
        
        if streamEnabled { params["stream"] = streamValue }
        if temperatureEnabled { params["temperature"] = Double(temperature) ?? 1.0 }
        if maxTokensEnabled { params["max_tokens"] = Int(maxTokens) ?? 64000 }
        if topPEnabled { params["top_p"] = Double(topP) ?? 1.0 }
        if topKEnabled { params["top_k"] = Int(topK) ?? 40 }
        if thinkingLevelEnabled { params["thinking_level"] = Int(thinkingLevel) ?? 1 }
        if thinkingBudgetEnabled { params["thinking_budget"] = Int(thinkingBudget) ?? 1000 }
        if thinkingEffortEnabled { params["thinking_effort"] = thinkingEffort }
        
        let jsonString = params.map { key, value in
            if let stringValue = value as? String {
                return "\"\(key)\":\"\(stringValue)\""
            } else if let boolValue = value as? Bool {
                return "\"\(key)\":\(boolValue)"
            } else {
                return "\"\(key)\":\(value)"
            }
        }.joined(separator: ",")
        
        return "{\(jsonString)}"
    }
    
    private func getDefaultBaseUrl(provider: String, type: String) -> String {
        switch provider {
        case "openai": return "https://api.openai.com/v1"
        case "gemini": return "https://generativelanguage.googleapis.com/v1beta"
        case "minimax": return "https://api.minimax.chat/v1"
        default: return ""
        }
    }
    
    private func getDefaultModel(provider: String, type: String) -> String {
        switch (provider, type) {
        case ("openai", "chat"): return "gpt-4"
        case ("openai", "image"): return "dall-e-3"
        case ("gemini", "chat"): return "gemini-pro"
        case ("gemini", "image"): return "imagen-3.0-generate-001"
        case ("minimax", "voice"): return "speech-01"
        default: return ""
        }
    }
}

struct ParameterToggleRow: View {
    let label: String
    @Binding var isEnabled: Bool
    @Binding var value: String
    var keyboardType: UIKeyboardType = .default
    var isReadOnly: Bool = false
    
    var body: some View {
        HStack {
            Text(label)
            Spacer()
            if !isReadOnly && isEnabled {
                TextField("", text: $value)
                    .keyboardType(keyboardType)
                    .multilineTextAlignment(.trailing)
                    .frame(width: 100)
            } else if isReadOnly && isEnabled {
                Text(value)
                    .foregroundColor(.gray)
            }
            Toggle("", isOn: $isEnabled)
                .labelsHidden()
        }
    }
}
