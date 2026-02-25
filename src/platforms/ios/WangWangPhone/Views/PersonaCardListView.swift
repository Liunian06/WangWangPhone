import SwiftUI

struct PersonaCardListView: View {
    @State private var cards: [PersonaCard] = []
    @State private var presets: [ApiPreset] = []
    @State private var showingNewCardSheet = false
    @State private var newCardName = ""
    @State private var selectedPresetId: Int64 = -1
    @State private var selectedCardId: Int64?
    
    private let dbHelper = PersonaCardDbHelper()
    private let presetManager = ApiPresetManager.shared
    
    var body: some View {
        NavigationView {
            ZStack {
                Color(UIColor.systemGroupedBackground)
                    .ignoresSafeArea()
                
                if cards.isEmpty {
                    VStack(spacing: 20) {
                        Image(systemName: "person.crop.circle.badge.plus")
                            .font(.system(size: 60))
                            .foregroundColor(.gray)
                        Text("还没有人设卡")
                            .font(.headline)
                            .foregroundColor(.gray)
                        Text("点击右上角 + 创建第一个人设卡")
                            .font(.subheadline)
                            .foregroundColor(.gray)
                    }
                } else {
                    ScrollView {
                        LazyVStack(spacing: 12) {
                            ForEach(cards, id: \.id) { card in
                                PersonaCardRow(card: card, presets: presets)
                                    .onTapGesture {
                                        selectedCardId = card.id
                                    }
                                    .contextMenu {
                                        Button(role: .destructive) {
                                            deleteCard(card.id)
                                        } label: {
                                            Label("删除", systemImage: "trash")
                                        }
                                    }
                            }
                        }
                        .padding()
                    }
                }
            }
            .navigationTitle("人设卡")
            .navigationBarTitleDisplayMode(.large)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button {
                        showingNewCardSheet = true
                    } label: {
                        Image(systemName: "plus")
                    }
                }
            }
            .sheet(isPresented: $showingNewCardSheet) {
                NewCardSheet(
                    presets: presets,
                    newCardName: $newCardName,
                    selectedPresetId: $selectedPresetId,
                    onSave: createCard,
                    onCancel: { showingNewCardSheet = false }
                )
            }
            .fullScreenCover(item: Binding(
                get: { selectedCardId.map { CardIdWrapper(id: $0) } },
                set: { selectedCardId = $0?.id }
            )) { wrapper in
                PersonaBuilderChatView(cardId: wrapper.id) {
                    selectedCardId = nil
                    loadCards()
                }
            }
        }
        .onAppear {
            loadPresets()
            loadCards()
        }
    }
    
    private func loadPresets() {
        presets = presetManager.getAllPresets()
        if let firstPreset = presets.first {
            selectedPresetId = firstPreset.id
        }
    }
    
    private func loadCards() {
        cards = dbHelper.getAllCards()
    }
    
    private func createCard() {
        guard !newCardName.trimmingCharacters(in: .whitespaces).isEmpty,
              selectedPresetId != -1 else {
            return
        }
        
        let cardId = dbHelper.createCard(name: newCardName, apiPresetId: selectedPresetId)
        if cardId != -1 {
            newCardName = ""
            showingNewCardSheet = false
            loadCards()
        }
    }
    
    private func deleteCard(_ id: Int64) {
        dbHelper.deleteCard(id)
        loadCards()
    }
}

struct PersonaCardRow: View {
    let card: PersonaCard
    let presets: [ApiPreset]
    
    var presetName: String {
        presets.first(where: { $0.id == card.apiPresetId })?.name ?? "未知预设"
    }
    
    var formattedDate: String {
        let date = Date(timeIntervalSince1970: TimeInterval(card.updatedAt) / 1000)
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd HH:mm"
        return formatter.string(from: date)
    }
    
    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: "person.circle.fill")
                .font(.system(size: 40))
                .foregroundColor(.blue)
            
            VStack(alignment: .leading, spacing: 4) {
                Text(card.name)
                    .font(.headline)
                
                HStack {
                    Text(presetName)
                        .font(.caption)
                        .foregroundColor(.secondary)
                    
                    Spacer()
                    
                    Text(formattedDate)
                        .font(.caption2)
                        .foregroundColor(.secondary)
                }
            }
            
            Spacer()
            
            Image(systemName: "chevron.right")
                .foregroundColor(.gray)
        }
        .padding()
        .background(Color(UIColor.secondarySystemGroupedBackground))
        .cornerRadius(12)
    }
}

struct NewCardSheet: View {
    let presets: [ApiPreset]
    @Binding var newCardName: String
    @Binding var selectedPresetId: Int64
    let onSave: () -> Void
    let onCancel: () -> Void
    
    var body: some View {
        NavigationView {
            Form {
                Section(header: Text("人设卡名称")) {
                    TextField("例如：神笔马良", text: $newCardName)
                }
                
                Section(header: Text("选择API预设")) {
                    if presets.isEmpty {
                        Text("请先创建API预设")
                            .foregroundColor(.secondary)
                    } else {
                        Picker("API预设", selection: $selectedPresetId) {
                            ForEach(presets, id: \.id) { preset in
                                Text(preset.name).tag(preset.id)
                            }
                        }
                        .pickerStyle(.menu)
                    }
                }
            }
            .navigationTitle("新建人设卡")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("取消") {
                        onCancel()
                    }
                }
                
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("创建") {
                        onSave()
                    }
                    .disabled(newCardName.trimmingCharacters(in: .whitespaces).isEmpty || selectedPresetId == -1)
                }
            }
        }
    }
}

struct CardIdWrapper: Identifiable {
    let id: Int64
}
