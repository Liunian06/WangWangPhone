
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
        AppIconData(id: "chat", name: "聊天", icon: "💬", colors: [Color(red: 0.03, green: 0.76, blue: 0.38), Color(red: 0.02, green: 0.68, blue: 0.34)]),
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
    @Binding var dockApps: [AppIconData]
    @Binding var isEditMode: Bool
    @Binding var isDraggingOverDock: Bool
    var maxDockApps: Int
    var onSettingsClick: () -> Void
    var onChatClick: () -> Void
    var onLayoutChanged: () -> Void
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
                    dockApps: $dockApps,
                    isDraggingOverDock: $isDraggingOverDock,
                    maxDockApps: maxDockApps,
                    colorScheme: colorScheme,
                    onTap: {
                        if !isEditMode {
                            if app.id == "settings" {
                                onSettingsClick()
                            } else if app.id == "chat" {
                                onChatClick()
                            }
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
    @Binding var dockApps: [AppIconData]
    @Binding var isDraggingOverDock: Bool
    let maxDockApps: Int
    let colorScheme: ColorScheme
    var onTap: () -> Void
    var onLayoutChanged: () -> Void
    
    @State private var dragOffset: CGSize = .zero
    @State private var isDragging = false
    
    // 抖动动画
    @State private var wiggleAmount: Double = 0
    
    var body: some View {
        VStack(spacing: 6) {
            ZStack {
                if app.useImage {
                    Image(colorScheme == .dark ? "SettingsIconDark" : "SettingsIconLight")
                        .resizable()
                        .aspectRatio(contentMode: .fit)
                        .frame(width: 60, height: 60)
                } else {
                    Text(app.icon)
                        .font(.system(size: 48))
                }
            }
            .frame(width: 60, height: 60)
            
            Text(app.name)
                .font(.caption2)
                .foregroundColor(.white)
        }
        .scaleEffect(isDragging ? 1.15 : 1.0)
        .opacity(isDragging ? 0.3 : 1.0)
        .zIndex(isDragging ? 10000 : 0)
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
                    
                    // 检查是否拖到了Dock区域（通过Y偏移判断）
                    isDraggingOverDock = value.translation.height > 150
                    
                    // 计算目标位置（仅在grid内排序）
                    if !isDraggingOverDock {
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
                }
                .onEnded { _ in
                    // 如果拖到了Dock区域且Dock未满，移到Dock
                    if isDraggingOverDock && dockApps.count < maxDockApps && index >= 0 && index < apps.count {
                        let movedApp = apps.remove(at: index)
                        dockApps.append(movedApp)
                    }
                    
                    withAnimation(.spring()) {
                        isDragging = false
                        dragOffset = .zero
                        isDraggingOverDock = false
                    }
                    onLayoutChanged()
                }
            : nil
        )
    }
    
    private var columns: Int { 4 }
}

// MARK: - Dock栏可拖拽图标
struct DraggableDockIconView: View {
    let app: AppIconData
    let dockIndex: Int
    @Binding var isEditMode: Bool
    @Binding var apps: [AppIconData]
    @Binding var dockApps: [AppIconData]
    let colorScheme: ColorScheme
    var onTap: () -> Void
    var onLayoutChanged: () -> Void
    
    @State private var dragOffset: CGSize = .zero
    @State private var isDragging = false
    
    // 抖动动画
    @State private var wiggleAmount: Double = 0
    
    var body: some View {
        ZStack {
            if app.useImage {
                Image(colorScheme == .dark ? "SettingsIconDark" : "SettingsIconLight")
                    .resizable()
                    .aspectRatio(contentMode: .fit)
                    .frame(width: 60, height: 60)
            } else {
                Text(app.icon)
                    .font(.system(size: 48))
            }
        }
        .frame(width: 60, height: 60)
        .scaleEffect(isDragging ? 1.15 : 1.0)
        .opacity(isDragging ? 0.3 : 1.0)
        .zIndex(isDragging ? 10000 : 0)
        .offset(dragOffset)
        .rotationEffect(isEditMode && !isDragging ? .degrees(wiggleAmount) : .degrees(0))
        .animation(
            isEditMode && !isDragging
                ? Animation.easeInOut(duration: 0.12 + Double(dockIndex % 3) * 0.03).repeatForever(autoreverses: true)
                : .default,
            value: isEditMode
        )
        .onAppear {
            if isEditMode {
                wiggleAmount = dockIndex % 2 == 0 ? -1.5 : 1.5
            }
        }
        .onChange(of: isEditMode) { newValue in
            if newValue {
                wiggleAmount = dockIndex % 2 == 0 ? -1.5 : 1.5
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
                    
                    // Dock内水平排序
                    if abs(value.translation.height) < 50 {
                        let dockCellWidth: CGFloat = 85 // 图标宽度 + 间距
                        let colOffset = Int(round(value.translation.width / dockCellWidth))
                        
                        if colOffset != 0 {
                            let targetIndex = max(0, min(dockApps.count - 1, dockIndex + colOffset))
                            if targetIndex != dockIndex && targetIndex >= 0 && targetIndex < dockApps.count {
                                withAnimation(.spring(response: 0.3)) {
                                    let movedApp = dockApps.remove(at: dockIndex)
                                    dockApps.insert(movedApp, at: targetIndex)
                                }
                            }
                        }
                    }
                }
                .onEnded { value in
                    // 如果向上拖出Dock区域，移回主屏幕
                    if value.translation.height < -50 && dockIndex >= 0 && dockIndex < dockApps.count {
                        let movedApp = dockApps.remove(at: dockIndex)
                        apps.append(movedApp)
                    }
                    
                    withAnimation(.spring()) {
                        isDragging = false
                        dragOffset = .zero
                    }
                    onLayoutChanged()
                }
            : nil
        )
    }
}

// MARK: - 主屏幕
struct HomeScreen: View {
    @State private var apps: [AppIconData] = []
    @State private var dockApps: [AppIconData] = []  // Dock栏独立管理，初始为空
    @State private var widgetOrder: [String] = ["clock", "weather"]  // 小组件顺序
    @State private var isEditMode = false
    @State private var isDraggingOverDock = false
    @State private var homeWallpaper: UIImage? = WallpaperManager.shared.getWallpaperImage(type: .home)
    
    @State private var city: String = "..."
    @State private var weather: WeatherInfo? = nil
    @State private var showSettings = false
    @State private var showChatApp = false
    @State private var showActivation = false
    @State private var showDisplaySettings = false
    @State private var isActivated = LicenseManager.shared.isActivated()
    @State private var expiryDate = LicenseManager.shared.getExpirationDateString()
    
    // 小组件拖拽状态
    @State private var widgetDragIndex: Int = -1
    @State private var widgetDragOffset: CGSize = .zero
    
    @Environment(\.colorScheme) var colorScheme
    
    private let layoutManager = LayoutManager.shared
    private let defaultApps = getDefaultApps()
    private let maxDockApps = 4

    var body: some View {
        ZStack {
            if let wallpaper = homeWallpaper {
                Image(uiImage: wallpaper)
                    .resizable()
                    .aspectRatio(contentMode: .fill)
                    .ignoresSafeArea()
            } else {
                Color.black.ignoresSafeArea()
            }
            
            VStack {
                // 小组件区域（支持拖拽交换）
                HStack(spacing: 15) {
                    ForEach(Array(widgetOrder.enumerated()), id: \.element) { index, widgetId in
                        let isDragged = widgetDragIndex == index
                        
                        Group {
                            if widgetId == "clock" {
                                ClockWidget(city: city)
                            } else {
                                WeatherWidget(city: city, weather: weather)
                            }
                        }
                        .scaleEffect(isDragged ? 1.05 : 1.0)
                        .opacity(isDragged ? 0.85 : 1.0)
                        .zIndex(isDragged ? 100 : 0)
                        .offset(isDragged ? widgetDragOffset : .zero)
                        .rotationEffect(isEditMode && !isDragged ? .degrees(index % 2 == 0 ? -1.0 : 1.0) : .degrees(0))
                        .animation(
                            isEditMode && !isDragged
                                ? Animation.easeInOut(duration: 0.15).repeatForever(autoreverses: true)
                                : .default,
                            value: isEditMode
                        )
                        .gesture(
                            isEditMode ?
                            DragGesture()
                                .onChanged { value in
                                    widgetDragIndex = index
                                    widgetDragOffset = value.translation
                                }
                                .onEnded { value in
                                    // 如果水平拖动超过阈值，交换
                                    if abs(value.translation.width) > 60 && widgetOrder.count == 2 {
                                        withAnimation(.spring()) {
                                            let temp = widgetOrder[0]
                                            widgetOrder[0] = widgetOrder[1]
                                            widgetOrder[1] = temp
                                        }
                                        saveLayout()
                                    }
                                    withAnimation(.spring()) {
                                        widgetDragIndex = -1
                                        widgetDragOffset = .zero
                                    }
                                }
                            : nil
                        )
                        .simultaneousGesture(
                            LongPressGesture(minimumDuration: 0.5)
                                .onEnded { _ in
                                    if !isEditMode {
                                        withAnimation(.spring()) {
                                            isEditMode = true
                                        }
                                        let impactFeedback = UIImpactFeedbackGenerator(style: .medium)
                                        impactFeedback.impactOccurred()
                                    }
                                }
                        )
                    }
                }
                .padding(.horizontal, 20)
                .padding(.top, 10)
                .onAppear {
                    loadData()
                    loadLayout()
                }
                .onReceive(NotificationCenter.default.publisher(for: NSNotification.Name("WallpaperChanged"))) { _ in
                    self.homeWallpaper = WallpaperManager.shared.getWallpaperImage(type: .home)
                }
                
                // 编辑模式完成按钮（浮动在右上角，不显示提示文字）
                if isEditMode {
                    HStack {
                        Spacer()
                        Button("完成") {
                            withAnimation(.spring()) {
                                isEditMode = false
                            }
                            saveLayout()
                        }
                        .font(.headline)
                        .foregroundColor(.blue)
                        .padding(.horizontal, 16)
                        .padding(.vertical, 8)
                        .background(
                            RoundedRectangle(cornerRadius: 16)
                                .fill(Color.white.opacity(0.25))
                        )
                    }
                    .padding(.horizontal, 20)
                    .padding(.bottom, 4)
                    .transition(.opacity.combined(with: .move(edge: .top)))
                }
                
                // 可拖拽应用网格
                DraggableAppGrid(
                    apps: $apps,
                    dockApps: $dockApps,
                    isEditMode: $isEditMode,
                    isDraggingOverDock: $isDraggingOverDock,
                    maxDockApps: maxDockApps,
                    onSettingsClick: { showSettings = true },
                    onChatClick: { showChatApp = true },
                    onLayoutChanged: { saveLayout() }
                )

                Spacer()

                // Dock 栏（独立管理，不再自动取前4个）
                ZStack {
                    RoundedRectangle(cornerRadius: 30)
                        .fill(isDraggingOverDock && dockApps.count < maxDockApps
                              ? Color.blue.opacity(0.4)
                              : Color.white.opacity(0.3))
                        .background(.ultraThinMaterial)
                        .clipShape(RoundedRectangle(cornerRadius: 30))
                        .frame(height: 90)
                        .padding(.horizontal, 15)
                    
                    if dockApps.isEmpty && !isEditMode {
                        Text("长按拖入应用")
                            .font(.caption)
                            .foregroundColor(.white.opacity(0.4))
                    }
                    
                    if dockApps.isEmpty && isEditMode {
                        Text("拖拽应用到此处")
                            .font(.caption)
                            .foregroundColor(.white.opacity(0.5))
                    }
                    
                    HStack(spacing: 25) {
                        ForEach(Array(dockApps.enumerated()), id: \.element.id) { dockIndex, app in
                            DraggableDockIconView(
                                app: app,
                                dockIndex: dockIndex,
                                isEditMode: $isEditMode,
                                apps: $apps,
                                dockApps: $dockApps,
                                colorScheme: colorScheme,
                                onTap: {
                                    if !isEditMode {
                                        if app.id == "settings" {
                                            showSettings = true
                                        } else if app.id == "chat" {
                                            showChatApp = true
                                        }
                                    }
                                },
                                onLayoutChanged: { saveLayout() }
                            )
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
                SettingsView(showSettings: $showSettings, showActivation: $showActivation, showDisplaySettings: $showDisplaySettings, isActivated: $isActivated, expiryDate: expiryDate)
                    .transition(.move(edge: .trailing))
                    .zIndex(1)
            }

            if showDisplaySettings {
                DisplaySettingsView(showDisplaySettings: $showDisplaySettings)
                    .transition(.move(edge: .trailing))
                    .zIndex(1.5)
            }
            
            if showActivation {
                ActivationView(showActivation: $showActivation, isActivated: $isActivated, expiryDate: $expiryDate)
                    .transition(.move(edge: .trailing))
                    .zIndex(2)
            }

            if showChatApp {
                ChatAppView(isPresented: $showChatApp)
                    .transition(.move(edge: .trailing))
                    .zIndex(3)
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
            
            // 加载Dock栏
            var orderedDockApps: [AppIconData] = []
            let dockItems = savedLayout.filter { $0.area == "dock" }.sorted { $0.position < $1.position }
            
            for layoutItem in dockItems {
                if let app = defaultApps.first(where: { $0.id == layoutItem.appId }) {
                    orderedDockApps.append(app)
                }
            }
            
            // 加载小组件顺序
            let widgetItems = savedLayout.filter { $0.area == "widget" }.sorted { $0.position < $1.position }
            if !widgetItems.isEmpty {
                widgetOrder = widgetItems.map { $0.appId }
            }
            
            // 补充数据库中没有的新应用到主屏幕（排除已在dock中的）
            let allSavedIds = Set(savedLayout.map { $0.appId })
            for app in defaultApps {
                if !allSavedIds.contains(app.id) {
                    orderedApps.append(app)
                }
            }
            
            apps = orderedApps
            dockApps = orderedDockApps
        } else {
            apps = defaultApps
            dockApps = []  // Dock初始为空
        }
    }
    
    /// 保存当前布局到数据库（同时保存grid、dock和widget）
    func saveLayout() {
        var items: [LayoutItem] = []
        // 保存主屏幕网格
        items += apps.enumerated().map { index, app in
            LayoutItem(appId: app.id, position: index, area: "grid")
        }
        // 保存Dock栏
        items += dockApps.enumerated().map { index, app in
            LayoutItem(appId: app.id, position: index, area: "dock")
        }
        // 保存小组件顺序
        items += widgetOrder.enumerated().map { index, widgetId in
            LayoutItem(appId: widgetId, position: index, area: "widget")
        }
        _ = layoutManager.saveLayout(items)
    }
}

// MARK: - 设置视图
struct SettingsView: View {
    @Binding var showSettings: Bool
    @Binding var showActivation: Bool
    @Binding var showDisplaySettings: Bool
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

                Section(header: Text("外观")) {
                    HStack {
                        Text("显示设置")
                        Spacer()
                        Image(systemName: "chevron.right")
                            .foregroundColor(.gray)
                    }
                    .contentShape(Rectangle())
                    .onTapGesture {
                        showDisplaySettings = true
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

// MARK: - 显示设置视图
struct DisplaySettingsView: View {
    @Binding var showDisplaySettings: Bool
    @State private var lockWallpaper: UIImage? = WallpaperManager.shared.getWallpaperImage(type: .lock)
    @State private var homeWallpaper: UIImage? = WallpaperManager.shared.getWallpaperImage(type: .home)
    @State private var showingImagePicker = false
    @State private var pickerType: WallpaperType = .lock

    var body: some View {
        NavigationView {
            List {
                Section(header: Text("锁屏壁纸")) {
                    HStack {
                        VStack(alignment: .leading) {
                            Text("锁屏壁纸设置")
                            Text(lockWallpaper != nil ? "已设置" : "点击选择图片")
                                .font(.caption)
                                .foregroundColor(.gray)
                        }
                        Spacer()
                        if let image = lockWallpaper {
                            Image(uiImage: image)
                                .resizable()
                                .aspectRatio(contentMode: .fill)
                                .frame(width: 60, height: 60)
                                .cornerRadius(8)
                        } else {
                            Text("🖼️").font(.largeTitle)
                        }
                    }
                    .contentShape(Rectangle())
                    .onTapGesture {
                        pickerType = .lock
                        showingImagePicker = true
                    }
                }

                Section(header: Text("桌面壁纸")) {
                    HStack {
                        VStack(alignment: .leading) {
                            Text("桌面壁纸设置")
                            Text(homeWallpaper != nil ? "已设置" : "点击选择图片")
                                .font(.caption)
                                .foregroundColor(.gray)
                        }
                        Spacer()
                        if let image = homeWallpaper {
                            Image(uiImage: image)
                                .resizable()
                                .aspectRatio(contentMode: .fill)
                                .frame(width: 60, height: 60)
                                .cornerRadius(8)
                        } else {
                            Text("🖼️").font(.largeTitle)
                        }
                    }
                    .contentShape(Rectangle())
                    .onTapGesture {
                        pickerType = .home
                        showingImagePicker = true
                    }
                }
            }
            .listStyle(InsetGroupedListStyle())
            .navigationTitle("显示设置")
            .navigationBarItems(leading: Button("返回") {
                showDisplaySettings = false
            })
            .sheet(isPresented: $showingImagePicker) {
                ImagePicker(selectedImage: pickerType == .lock ? $lockWallpaper : $homeWallpaper, type: pickerType)
            }
        }
    }
}

// MARK: - 图片选择器适配器
struct ImagePicker: UIViewControllerRepresentable {
    @Binding var selectedImage: UIImage?
    let type: WallpaperType
    @Environment(\.presentationMode) var presentationMode

    func makeUIViewController(context: Context) -> UIImagePickerController {
        let picker = UIImagePickerController()
        picker.delegate = context.coordinator
        return picker
    }

    func updateUIViewController(_ uiViewController: UIImagePickerController, context: Context) {}

    func makeCoordinator() -> Coordinator {
        Coordinator(self)
    }

    class Coordinator: NSObject, UINavigationControllerDelegate, UIImagePickerControllerDelegate {
        let parent: ImagePicker
        init(_ parent: ImagePicker) { self.parent = parent }

        func imagePickerController(_ picker: UIImagePickerController, didFinishPickingMediaWithInfo info: [UIImagePickerController.InfoKey : Any]) {
            if let image = info[.originalImage] as? UIImage {
                if let fileName = WallpaperManager.shared.copyImageToStorage(image) {
                    if WallpaperManager.shared.saveWallpaper(type: parent.type, fileName: fileName) {
                        parent.selectedImage = image
                        // 通知主界面更新
                        NotificationCenter.default.post(name: NSNotification.Name("WallpaperChanged"), object: nil)
                    }
                }
            }
            parent.presentationMode.wrappedValue.dismiss()
        }
    }
}