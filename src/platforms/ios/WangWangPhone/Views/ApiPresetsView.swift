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
                    
                    if type != "voice" {
                        Picker("提供商", selection: $provider) {
                            Text("OpenAI").tag("openai")
                            Text("Gemini").tag("gemini")
                        }
                        .pickerStyle(SegmentedPickerStyle())
                    } else {
                        HStack {
                            Text("提供商")
                            Spacer()
                            Text("Minimax").foregroundColor(.gray)
                        }
                    }
                }
                
                Section(header: Text("API配置")) {
                    TextField("API Key", text: $apiKey)
                    TextField("Base URL", text: $baseUrl)
                        .autocapitalization(.none)
                        .keyboardType(.URL)
                    if baseUrl.isEmpty {
                        Text(getDefaultBaseUrl(provider: provider, type: type))
                            .font(.caption)
                            .foregroundColor(.gray)
                    }
                    
                    TextField("模型名称", text: $model)
                    if model.isEmpty {
                        Text(getDefaultModel(provider: provider, type: type))
                            .font(.caption)
                            .foregroundColor(.gray)
                    }
                }
                
                if preset != nil && onDelete != nil {
                    Section {
                        Button(action: {
                            if let id = preset?.id {
                                onDelete?(id)
                            }
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
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("保存") {
                        let finalBaseUrl = baseUrl.isEmpty ? getDefaultBaseUrl(provider: provider, type: type) : baseUrl
                        let finalModel = model.isEmpty ? getDefaultModel(provider: provider, type: type) : model
                        
                        let newPreset = ApiPreset(
                            id: preset?.id ?? 0,
                            name: name,
                            type: type,
                            provider: provider,
                            apiKey: apiKey,
                            baseUrl: finalBaseUrl,
                            model: finalModel
                        )
                        onSave(newPreset)
                    }
                    .disabled(name.isEmpty || apiKey.isEmpty)
                }
            }
        }
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
