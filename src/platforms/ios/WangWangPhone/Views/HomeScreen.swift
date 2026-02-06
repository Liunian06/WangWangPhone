import SwiftUI

struct AppIconData: Identifiable, Equatable {
    let id: String          // 唯一标识，用于布局持久化
    let name: String
    let icon: String
    let colors: [Color]
    var useImage: Bool = false
    
    static func == (lhs: AppIconData, rhs: AppIconData) -> Bool {
        return lhs.id == rhs.id
    }
}

/// 默认应用列表（初始顺序）
func getDefaultApps() -> [AppIconData] {
    return [
        AppIconData(id: "phone", name: "电话", icon: "📞", colors: [.pink, .orange]),
        AppIconData(id: "message", name: "信息", icon: "💬", colors: [.blue, .cyan]),
        AppIconData(id: "safari", name: "Safari", icon: "🧭", colors: [.green, .blue]),
        AppIconData(id: "music", name: "音乐", icon: "🎵", colors: [.yellow, .red]),
        AppIconData(id: "camera", name: "相机", icon: "📷", colors: [.white, .gray]),
        AppIconData(id: "calendar", name: "日历", icon: "📅", colors: [.white, .gray]),
        AppIconData(id: "settings", name: "设置", icon: "SettingsIcon", colors: [.white, .gray], useImage: true),
        AppIconData(id: "wangwang", name: "汪汪", icon: "🐶", colors: [.white, .gray])
    ]
}

struct ClockWidget: View {
    @State private var currentTime = Date()
    let timer = Timer.publish(every: 1, on: .main, in: .common).autoconnect()
    var city: String
    
    var body: some View {
        ZStack {
            RoundedRectangle(cornerRadius: 20)
                .fill(LinearGradient(colors: [Color(red: 0.88, green: 0.76, blue: 0.99), Color(red: 0.56, green: 0.77, blue: 0.99)], startPoint: .topLeading, endPoint: .bottomTrailing))
                .frame(height: 150)
            
            VStack(alignment: .leading) {
                Text(dateFormatter.string(from: currentTime))
                    .font(.caption)
                    .fontWeight(.medium)
                    .foregroundColor(.white)
                
                Text(timeFormatter.string(from: currentTime))
                    .font(.system(size: 40, weight: .bold))
                    .foregroundColor(.white)
                
                Text(city)
                    .font(.caption2)
                    .foregroundColor(.white.opacity(0.8))
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding()
        }
        .onReceive(timer) { input in
            currentTime = input
        }
    }
    
    private var timeFormatter: DateFormatter {
        let formatter = DateFormatter()
        formatter.dateFormat = "HH:mm"
        return formatter
    }
    
    private var dateFormatter: DateFormatter {
        let formatter = DateFormatter()
        formatter.dateFormat = "M月d日 EEEE"
        formatter.locale = Locale(identifier: "zh_CN")
        return formatter
    }
}

struct WeatherInfo {
    var temp: String
    var description: String
    var icon: String
    var range: String
}

struct WeatherWidget: View {
    var city: String
    var weather: WeatherInfo?

    var body: some View {
        ZStack {
            RoundedRectangle(cornerRadius: 20)
                .fill(LinearGradient(colors: [Color(red: 0.31, green: 0.67, blue: 0.99), Color(red: 0.0, green: 0.95, blue: 0.99)], startPoint: .topLeading, endPoint: .bottomTrailing))
                .frame(height: 150)
            
            HStack {
                VStack(alignment: .leading) {
                    Text(city)
                        .font(.headline)
                        .fontWeight(.bold)
                        .foregroundColor(.white)
                    
                    Text(weather?.temp ?? "--")
                        .font(.system(size: 40, weight: .light))
                        .foregroundColor(.white)
                }
                
                Spacer()
                
                VStack(alignment: .trailing) {
                    HStack(spacing: 5) {
                        Text(weather?.icon ?? "❓")
                            .font(.title)
                        Text(weather?.description ?? "Loading...")
                            .font(.caption)
                            .foregroundColor(.white)
                    }
                    Text(weather?.range ?? "")
                        .font(.caption2)
                        .foregroundColor(.white.opacity(0.8))
                }
            }
            .padding()
        }
    }
}

// MARK: - 可拖拽网格视图
struct DraggableAppGrid: View {
    @Binding var apps: [AppIconData]
    @Binding var isEditMode: Bool
    var onSettingsClick: () -> Unit
    var onLayoutChanged: () -> Unit
    @Environment(\.colorScheme) var colorScheme
    
    let columns = 4
    
    var body: some View {
        let gridColumns = Array(repeating: GridItem(.flexible(), spacing: 20), count: columns)
        
        LazyVGrid(columns: gridColumns, spacing: 25) {
            ForEach(Array(apps.enumerated()), id: \.element.id) { index, app in
                DraggableAppIconView(
                    app: app,
                    index: index,
                    isEditMode: $isEditMode,
                    apps: $apps,
                    colorScheme: colorScheme,
                    onTap: {
                        if !isEditMode && app.id == "settings" {
                            onSettingsClick()
                        }
                    },
                    onLayoutChanged: onLayoutChanged
                )
            }
        }
        .padding(20)
    }
}

struct DraggableAppIconView: View {
    let app: AppIconData
    let index: Int
    @Binding var isEditMode: Bool
    @Binding var apps: [AppIconData]
    let colorScheme: ColorScheme
    var onTap: () -> Void
    var onLayoutChanged: () -> Void
    
    @State private var dragOffset: CGSize = .zero
    @State private var isDragging = false
    
    // 抖动动画
    @State private var wiggleAmount: Double = 0
    
    var body: some View {
        VStack(spacing: 8) {
            ZStack {
                if app.useImage {
                    Image(colorScheme == .dark ? "SettingsIconDark" : "SettingsIconLight")
                        .resizable()
                        .aspectRatio(contentMode: .fit)
                        .frame(width: 80, height: 80)
                } else {
                    Text(app.icon)
                        .font(.system(size: 65))
                }
            }
            .frame(width: 80, height: 80)
            
            Text(app.name)
                .font(.caption)
                .foregroundColor(.white)
        }
        .scaleEffect(isDragging ? 1.15 : 1.0)
        .opacity(isDragging ? 0.85 : 1.0)
        .zIndex(isDragging ? 10 : 0)
        .offset(dragOffset)
        .rotationEffect(isEditMode && !isDragging ? .degrees(wiggleAmount) : .degrees(0))
        .animation(
            isEditMode && !isDragging
                ? Animation.easeInOut(duration: 0.12 + Double(index % 3) * 0.03).repeatForever(autoreverses: true)
                : .default,
            value: isEditMode
        )
        .onAppear {
            if isEditMode {
                wiggleAmount = index % 2 == 0 ? -1.5 : 1.5
            }
        }
        .onChange(of: isEditMode) { newValue in
            if newValue {
                wiggleAmount = index % 2 == 0 ? -1.5 : 1.5
            } else {
                wiggleAmount = 0
            }
        }
        .onTapGesture {
            onTap()
        }
        .simultaneousGesture(
            LongPressGesture(minimumDuration: 0.5)
                .onEnded { _ in
                    withAnimation(.spring()) {
                        isEditMode = true
                    }
                    // 触觉反馈
                    let impactFeedback = UIImpactFeedbackGenerator(style: .medium)
                    impactFeedback.impactOccurred()
                }
        )
        .simultaneousGesture(
            isEditMode ?
            DragGesture()
                .onChanged { value in
                    isDragging = true
                    dragOffset = value.translation
                    
                    // 计算目标位置
                    let cellWidth: CGFloat = 90
                    let cellHeight: CGFloat = 110
                    let dragX = value.translation.width
                    let dragY = value.translation.height
                    
                    let colOffset = Int(round(dragX / cellWidth))
                    let rowOffset = Int(round(dragY / cellHeight))
                    
                    let currentRow = index / columns
                    let currentCol = index % columns
                    
                    let targetCol = max(0, min(columns - 1, currentCol + colOffset))
                    let targetRow = max(0, currentRow + rowOffset)
                    let targetIndex = min(apps.count - 1, max(0, targetRow * columns + targetCol))
                    
                    if targetIndex != index && targetIndex >= 0 && targetIndex < apps.count {
                        withAnimation(.spring(response: 0.3)) {
                            let movedApp = apps.remove(at: index)
                            apps.insert(movedApp, at: targetIndex)
                        }
                    }
                }
                .onEnded { _ in
                    withAnimation(.spring()) {
                        isDragging = false
                        dragOffset = .zero
                    }
                    onLayoutChanged()
                }
            : nil
        )
    }
    
    private var columns: Int { 4 }
}

// MARK: - 主屏幕
struct HomeScreen: View {
    @State private var apps: [AppIconData] = []
    @State private var isEditMode = false
    
    @State private var city: String = "..."
    @State private var weather: WeatherInfo? = nil
    @State private var showSettings = false
    @State private var showActivation = false
    @State private var isActivated = LicenseManager.shared.isActivated()
    @State private var expiryDate = LicenseManager.shared.getExpirationDateString()
    
    @Environment(\.colorScheme) var colorScheme
    
    private let layoutManager = LayoutManager.shared
    private let defaultApps = getDefaultApps()

    var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()
            
            VStack {
                // 小组件区域
                HStack(spacing: 15) {
                    ClockWidget(city: city)
                    WeatherWidget(city: city, weather: weather)
                }
                .padding(.horizontal, 20)
                .padding(.top, 10)
                .onAppear {
                    loadData()
                    loadLayout()
                }
                
                // 编辑模式提示栏
                if isEditMode {
                    HStack {
                        Text("长按拖拽图标以调整位置")
                            .font(.subheadline)
                            .foregroundColor(.white.opacity(0.9))
                        Spacer()
                        Button("完成") {
                            withAnimation(.spring()) {
                                isEditMode = false
                            }
                            saveLayout()
                        }
                        .font(.headline)
                        .foregroundColor(.blue)
                    }
                    .padding(.horizontal, 24)
                    .padding(.vertical, 10)
                    .background(
                        RoundedRectangle(cornerRadius: 12)
                            .fill(Color.white.opacity(0.15))
                    )
                    .padding(.horizontal, 20)
                    .padding(.bottom, 4)
                    .transition(.opacity.combined(with: .move(edge: .top)))
                }
                
                // 可拖拽应用网格
                DraggableAppGrid(
                    apps: $apps,
                    isEditMode: $isEditMode,
                    onSettingsClick: { showSettings = true },
                    onLayoutChanged: { saveLayout() }
                )

                Spacer()

                // Dock 栏
                ZStack {
                    RoundedRectangle(cornerRadius: 30)
                        .fill(Color.white.opacity(0.3))
                        .background(.ultraThinMaterial)
                        .clipShape(RoundedRectangle(cornerRadius: 30))
                        .frame(height: 90)
                        .padding(.horizontal, 15)
                    
                    HStack(spacing: 25) {
                        ForEach(apps.prefix(4)) { app in
                            ZStack {
                                if app.useImage {
                                    Image(colorScheme == .dark ? "SettingsIconDark" : "SettingsIconLight")
                                        .resizable()
                                        .aspectRatio(contentMode: .fit)
                                        .frame(width: 70, height: 70)
                                } else {
                                    Text(app.icon)
                                        .font(.system(size: 55))
                                }
                            }
                            .frame(width: 70, height: 70)
                        }
                    }
                }
                .padding(.bottom, 20)
            }

            // Home Indicator
            VStack {
                Spacer()
                RoundedRectangle(cornerRadius: 5)
                    .fill(Color.white.opacity(0.8))
                    .frame(width: 120, height: 5)
                    .padding(.bottom, 8)
            }

            // 点击空白区域退出编辑模式
            if isEditMode {
                Color.clear
                    .contentShape(Rectangle())
                    .onTapGesture {
                        withAnimation(.spring()) {
                            isEditMode = false
                        }
                        saveLayout()
                    }
                    .zIndex(-1)
            }

            if showSettings {
                SettingsView(showSettings: $showSettings, showActivation: $showActivation, isActivated: $isActivated, expiryDate: expiryDate)
                    .transition(.move(edge: .trailing))
                    .zIndex(1)
            }
            
            if showActivation {
                ActivationView(showActivation: $showActivation, isActivated: $isActivated, expiryDate: $expiryDate)
                    .transition(.move(edge: .trailing))
                    .zIndex(2)
            }
        }
    }
    
    // MARK: - 数据加载
    
    func loadData() {
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
            self.city = "广州"
            self.weather = WeatherInfo(
                temp: "25°",
                description: "多云",
                icon: "⛅",
                range: "H:29° L:21°"
            )
        }
    }
    
    /// 从数据库加载布局
    func loadLayout() {
        let savedLayout = layoutManager.getLayout()
        if !savedLayout.isEmpty {
            var orderedApps: [AppIconData] = []
            let gridItems = savedLayout.filter { $0.area == "grid" }.sorted { $0.position < $1.position }
            
            for layoutItem in gridItems {
                if let app = defaultApps.first(where: { $0.id == layoutItem.appId }) {
                    orderedApps.append(app)
                }
            }
            
            // 补充数据库中没有的新应用
            for app in defaultApps {
                if !orderedApps.contains(where: { $0.id == app.id }) {
                    orderedApps.append(app)
                }
            }
            
            apps = orderedApps
        } else {
            apps = defaultApps
        }
    }
    
    /// 保存当前布局到数据库
    func saveLayout() {
        let items = apps.enumerated().map { index, app in
            LayoutItem(appId: app.id, position: index, area: "grid")
        }
        _ = layoutManager.saveLayout(items)
    }
}

// MARK: - 设置视图
struct SettingsView: View {
    @Binding var showSettings: Bool
    @Binding var showActivation: Bool
    @Binding var isActivated: Bool
    var expiryDate: String
    @Environment(\.colorScheme) var colorScheme: ColorScheme
    
    var body: some View {
        NavigationView {
            List {
                Section(header: Text("激活与授权")) {
                    HStack {
                        VStack(alignment: .leading) {
                            Text("软件激活")
                            if isActivated {
                                Text("有效期至: \(expiryDate)")
                                    .font(.caption)
                                    .foregroundColor(.gray)
                            }
                        }
                        Spacer()
                        Text(isActivated ? "已查看" : "未激活")
                            .foregroundColor(.gray)
                    }
                    .contentShape(Rectangle())
                    .onTapGesture {
                        showActivation = true
                    }
                }
            }
            .listStyle(InsetGroupedListStyle())
            .navigationTitle("设置")
            .navigationBarItems(leading: Button("返回") {
                showSettings = false
            })
        }
    }
}

// MARK: - 激活视图
struct ActivationView: View {
    @Binding var showActivation: Bool
    @Binding var isActivated: Bool
    @Binding var expiryDate: String
    @State private var licenseKey = ""
    @State private var errorMessage: String? = nil
    @Environment(\.colorScheme) var colorScheme: ColorScheme
    
    private var machineId: String {
        LicenseManager.shared.getMachineId()
    }
    
    var body: some View {
        NavigationView {
            Form {
                Section(header: Text("机器码")) {
                    Text(machineId)
                        .foregroundColor(.gray)
                    
                    Button(action: {
                        UIPasteboard.general.string = machineId
                    }) {
                        Text("复制机器码")
                            .frame(maxWidth: .infinity)
                            .foregroundColor(.white)
                    }
                    .listRowBackground(Color.green)
                }
                
                Section(header: Text("激活码")) {
                    TextEditor(text: $licenseKey)
                        .frame(height: 100)
                    
                    Button(action: {
                        if let string = UIPasteboard.general.string {
                            licenseKey = string
                        }
                    }) {
                        Text("粘贴激活码")
                            .frame(maxWidth: .infinity)
                            .foregroundColor(.white)
                    }
                    .listRowBackground(Color.purple)
                }
                
                if let error = errorMessage {
                    Section {
                        Text(error)
                            .foregroundColor(.red)
                            .font(.caption)
                    }
                }
                
                Section {
                    Button(action: {
                        LicenseManager.shared.verifyLicense(licenseKey.trimmingCharacters(in: .whitespacesAndNewlines)) { result in
                            switch result {
                            case .success(_):
                                errorMessage = nil
                                isActivated = LicenseManager.shared.isActivated()
                                expiryDate = LicenseManager.shared.getExpirationDateString()
                                showActivation = false
                            case .error(let message):
                                errorMessage = message
                            }
                        }
                    }) {
                        Text("激活")
                            .frame(maxWidth: .infinity)
                            .foregroundColor(.white)
                    }
                    .listRowBackground(Color.blue)
                }
            }
            .navigationTitle("激活授权")
            .navigationBarItems(leading: Button("取消") {
                showActivation = false
            })
        }
    }
}