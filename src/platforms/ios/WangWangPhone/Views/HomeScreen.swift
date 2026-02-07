
import SwiftUI

struct AppIconData: Identifiable, Equatable {
    let id: String
    let name: String
    let icon: String
    let colors: [Color]
    var useImage: Bool = false
    
    static func == (lhs: AppIconData, rhs: AppIconData) -> Bool {
        return lhs.id == rhs.id
    }
}

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
                Text(dateFormatter.string(from: currentTime)).font(.caption).fontWeight(.medium).foregroundColor(.white)
                Text(timeFormatter.string(from: currentTime)).font(.system(size: 40, weight: .bold)).foregroundColor(.white)
                Text(city).font(.caption2).foregroundColor(.white.opacity(0.8))
            }
            .frame(maxWidth: .infinity, alignment: .leading).padding()
        }
        .onReceive(timer) { input in currentTime = input }
    }
    
    private var timeFormatter: DateFormatter {
        let f = DateFormatter(); f.dateFormat = "HH:mm"; return f
    }
    private var dateFormatter: DateFormatter {
        let f = DateFormatter(); f.dateFormat = "M月d日 EEEE"; f.locale = Locale(identifier: "zh_CN"); return f
    }
}

struct WeatherInfo {
    var temp: String; var description: String; var icon: String; var range: String
}

struct WeatherWidget: View {
    var city: String; var weather: WeatherInfo?
    var body: some View {
        ZStack {
            RoundedRectangle(cornerRadius: 20)
                .fill(LinearGradient(colors: [Color(red: 0.31, green: 0.67, blue: 0.99), Color(red: 0.0, green: 0.95, blue: 0.99)], startPoint: .topLeading, endPoint: .bottomTrailing))
                .frame(height: 150)
            HStack {
                VStack(alignment: .leading) {
                    Text(city).font(.headline).fontWeight(.bold).foregroundColor(.white)
                    Text(weather?.temp ?? "--").font(.system(size: 40, weight: .light)).foregroundColor(.white)
                }
                Spacer()
                VStack(alignment: .trailing) {
                    HStack(spacing: 5) {
                        Text(weather?.icon ?? "❓").font(.title)
                        Text(weather?.description ?? "Loading...").font(.caption).foregroundColor(.white)
                    }
                    Text(weather?.range ?? "").font(.caption2).foregroundColor(.white.opacity(0.8))
                }
            }.padding()
        }
    }
}

// MARK: - 4x6 网格系统
struct DraggableAppGrid: View {
    @Binding var gridPositions: [Int: AppIconData]
    @Binding var dockApps: [AppIconData]
    @Binding var isEditMode: Bool
    @Binding var isDraggingOverDock: Bool
    @Binding var draggingApp: AppIconData?
    @Binding var draggingOffset: CGSize
    @Binding var draggingFromCell: Int
    var maxDockApps: Int
    var onSettingsClick: () -> Void
    var onChatClick: () -> Void
    var onLayoutChanged: () -> Void
    @Environment(\.colorScheme) var colorScheme
    
    let columns = 4
    let rows = 7
    
    @State private var highlightCellIndex: Int = -1

    var body: some View {
        GeometryReader { geometry in
            let cellWidth = geometry.size.width / CGFloat(columns)
            let cellHeight = geometry.size.height / CGFloat(rows)
            
            ZStack {
                // 高亮目标格子
                if highlightCellIndex >= 0 && highlightCellIndex < columns * rows {
                    let col = highlightCellIndex % columns
                    let row = highlightCellIndex / columns
                    RoundedRectangle(cornerRadius: 12)
                        .stroke(Color.white.opacity(0.5), lineWidth: 2)
                        .frame(width: cellWidth - 10, height: cellHeight - 10)
                        .position(x: CGFloat(col) * cellWidth + cellWidth / 2, y: CGFloat(row) * cellHeight + cellHeight / 2)
                }

                ForEach(0..<(columns * rows), id: \.self) { cellIndex in
                    if let app = gridPositions[cellIndex] {
                        let col = cellIndex % columns
                        let row = cellIndex / columns
                        let isDragged = draggingApp?.id == app.id && draggingFromCell == cellIndex
                        
                        appIconView(app: app, cellIndex: cellIndex, cellWidth: cellWidth, cellHeight: cellHeight)
                            .frame(width: cellWidth, height: cellHeight)
                            .position(x: CGFloat(col) * cellWidth + cellWidth / 2, y: CGFloat(row) * cellHeight + cellHeight / 2)
                            .opacity(isDragged ? 0.3 : 1.0)
                            .zIndex(isDragged ? 100 : 0)
                    }
                }
            }
        }
        .padding(.horizontal, 20)
    }
    
    @ViewBuilder
    func appIconView(app: AppIconData, cellIndex: Int, cellWidth: CGFloat, cellHeight: CGFloat) -> some View {
        let wiggle: Double = cellIndex % 2 == 0 ? -1.5 : 1.5
        
        VStack(spacing: 6) {
            ZStack {
                if app.useImage {
                    Image(colorScheme == .dark ? "SettingsIconDark" : "SettingsIconLight")
                        .resizable().aspectRatio(contentMode: .fit).frame(width: 60, height: 60)
                } else {
                    Text(app.icon).font(.system(size: 48))
                }
            }.frame(width: 60, height: 60)
            Text(app.name).font(.caption2).foregroundColor(.white)
        }
        .rotationEffect(isEditMode ? .degrees(wiggle) : .degrees(0))
        .animation(isEditMode ? Animation.easeInOut(duration: 0.12 + Double(cellIndex % 3) * 0.03).repeatForever(autoreverses: true) : .default, value: isEditMode)
        .onTapGesture {
            if !isEditMode {
                if app.id == "settings" { onSettingsClick() }
                else if app.id == "chat" { onChatClick() }
            }
        }
        .simultaneousGesture(
            LongPressGesture(minimumDuration: 0.5).onEnded { _ in
                if !isEditMode {
                    withAnimation(.spring()) { isEditMode = true }
                    UIImpactFeedbackGenerator(style: .medium).impactOccurred()
                }
            }
        )
        .simultaneousGesture(
            isEditMode ?
            DragGesture(coordinateSpace: .global)
                .onChanged { value in
                    if draggingApp == nil {
                        draggingApp = app
                        draggingFromCell = cellIndex
                    }
                    draggingOffset = value.translation
                    isDraggingOverDock = value.translation.height > 200
                }
                .onEnded { value in
                    if isDraggingOverDock && dockApps.count < maxDockApps {
                        gridPositions.removeValue(forKey: cellIndex)
                        dockApps.append(app)
                    } else {
                        let colOffset = Int(round(value.translation.width / cellWidth))
                        let rowOffset = Int(round(value.translation.height / cellHeight))
                        let curRow = cellIndex / columns
                        let curCol = cellIndex % columns
                        let tCol = max(0, min(columns - 1, curCol + colOffset))
                        let tRow = max(0, min(rows - 1, curRow + rowOffset))
                        let targetCell = tRow * columns + tCol

                        highlightCellIndex = targetCell
                    }
                }
                .onEnded { value in
                    if isDraggingOverDock && dockApps.count < maxDockApps {
                        gridPositions.removeValue(forKey: cellIndex)
                        dockApps.append(app)
                    } else {
                        let colOffset = Int(round(value.translation.width / cellWidth))
                        let rowOffset = Int(round(value.translation.height / cellHeight))
                        let curRow = cellIndex / columns
                        let curCol = cellIndex % columns
                        let tCol = max(0, min(columns - 1, curCol + colOffset))
                        let tRow = max(0, min(rows - 1, curRow + rowOffset))
                        let targetCell = tRow * columns + tCol
                        
                        // 自由摆放，不挤压，如果有图标则交换
                        if targetCell != cellIndex && targetCell >= 0 && targetCell < columns * rows {
                            let existingApp = gridPositions[targetCell]
                            gridPositions.removeValue(forKey: cellIndex)
                            gridPositions[targetCell] = app
                            
                            // 如果目标位置有应用，交换到原位置
                            if let existing = existingApp {
                                gridPositions[cellIndex] = existing
                            }
                        }
                    }
                    withAnimation(.spring()) {
                        draggingApp = nil; draggingOffset = .zero; draggingFromCell = -1; isDraggingOverDock = false; highlightCellIndex = -1
                    }
                    onLayoutChanged()
                }
            : nil
        )
    }
}

// MARK: - Dock栏可拖拽图标
struct DraggableDockIconView: View {
    let app: AppIconData
    let dockIndex: Int
    @Binding var isEditMode: Bool
    @Binding var gridPositions: [Int: AppIconData]
    @Binding var dockApps: [AppIconData]
    @Binding var draggingApp: AppIconData?
    @Binding var draggingOffset: CGSize
    let colorScheme: ColorScheme
    var onTap: () -> Void
    var onLayoutChanged: () -> Void
    let gridCols: Int
    let gridRows: Int
    
    @State private var dragOffset: CGSize = .zero
    @State private var isDragging = false
    @State private var wiggleAmount: Double = 0
    
    var body: some View {
        ZStack {
            if app.useImage {
                Image(colorScheme == .dark ? "SettingsIconDark" : "SettingsIconLight")
                    .resizable().aspectRatio(contentMode: .fit).frame(width: 60, height: 60)
            } else {
                Text(app.icon).font(.system(size: 48))
            }
        }
        .frame(width: 60, height: 60)
        .scaleEffect(isDragging ? 1.15 : 1.0)
        .opacity(isDragging ? 0.3 : 1.0)
        .zIndex(isDragging ? 100 : 0)
        .offset(dragOffset)
        .rotationEffect(isEditMode && !isDragging ? .degrees(wiggleAmount) : .degrees(0))
        .animation(isEditMode && !isDragging ? Animation.easeInOut(duration: 0.12 + Double(dockIndex % 3) * 0.03).repeatForever(autoreverses: true) : .default, value: isEditMode)
        .onAppear { if isEditMode { wiggleAmount = dockIndex % 2 == 0 ? -1.5 : 1.5 } }
        .onChange(of: isEditMode) { nv in wiggleAmount = nv ? (dockIndex % 2 == 0 ? -1.5 : 1.5) : 0 }
        .onTapGesture { onTap() }
        .simultaneousGesture(
            LongPressGesture(minimumDuration: 0.5).onEnded { _ in
                withAnimation(.spring()) { isEditMode = true }
                UIImpactFeedbackGenerator(style: .medium).impactOccurred()
            }
        )
        .simultaneousGesture(
            isEditMode ?
            DragGesture()
                .onChanged { value in
                    isDragging = true
                    dragOffset = value.translation
                    draggingApp = app
                    draggingOffset = value.translation
                    if abs(value.translation.height) < 50 {
                        let dockCellWidth: CGFloat = 85
                        let colOffset = Int(round(value.translation.width / dockCellWidth))
                        if colOffset != 0 {
                            let targetIdx = max(0, min(dockApps.count - 1, dockIndex + colOffset))
                            if targetIdx != dockIndex && targetIdx >= 0 && targetIdx < dockApps.count {
                                withAnimation(.spring(response: 0.3)) {
                                    let movedApp = dockApps.remove(at: dockIndex)
                                    dockApps.insert(movedApp, at: targetIdx)
                                }
                            }
                        }
                    }
                }
                .onEnded { value in
                    if value.translation.height < -50 && dockIndex >= 0 && dockIndex < dockApps.count {
                        let movedApp = dockApps.remove(at: dockIndex)
                        // 找到空位放入网格
                        for i in 0..<(gridCols * gridRows) {
                            if gridPositions[i] == nil {
                                gridPositions[i] = movedApp
                                break
                            }
                        }
                    }
                    withAnimation(.spring()) {
                        isDragging = false; dragOffset = .zero
                        draggingApp = nil; draggingOffset = .zero
                    }
                    onLayoutChanged()
                }
            : nil
        )
    }
}

// MARK: - 主屏幕
struct HomeScreen: View {
    @State private var gridPositions: [Int: AppIconData] = [:]
    @State private var dockApps: [AppIconData] = []
    @State private var widgetOrder: [String] = ["clock", "weather"]
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
    
    @State private var widgetDragIndex: Int = -1
    @State private var widgetDragOffset: CGSize = .zero
    @State private var draggingApp: AppIconData? = nil
    @State private var draggingOffset: CGSize = .zero
    @State private var draggingFromCell: Int = -1
    
    @Environment(\.colorScheme) var colorScheme
    
    private let layoutManager = LayoutManager.shared
    private let defaultApps = getDefaultApps()
    private let maxDockApps = 4
    private let gridCols = 4
    private let gridRows = 7

    var body: some View {
        ZStack {
            if let wallpaper = homeWallpaper {
                Image(uiImage: wallpaper).resizable().aspectRatio(contentMode: .fill).ignoresSafeArea()
            } else {
                Color.black.ignoresSafeArea()
            }
            
            VStack {
                // 小组件区域
                HStack(spacing: 15) {
                    ForEach(Array(widgetOrder.enumerated()), id: \.element) { index, widgetId in
                        let isDragged = widgetDragIndex == index
                        Group {
                            if widgetId == "clock" { ClockWidget(city: city) }
                            else { WeatherWidget(city: city, weather: weather) }
                        }
                        .scaleEffect(isDragged ? 1.05 : 1.0)
                        .opacity(isDragged ? 0.85 : 1.0)
                        .zIndex(isDragged ? 100 : 0)
                        .offset(isDragged ? widgetDragOffset : .zero)
                        .rotationEffect(isEditMode && !isDragged ? .degrees(index % 2 == 0 ? -1.0 : 1.0) : .degrees(0))
                        .animation(isEditMode && !isDragged ? Animation.easeInOut(duration: 0.15).repeatForever(autoreverses: true) : .default, value: isEditMode)
                        .gesture(
                            isEditMode ?
                            DragGesture()
                                .onChanged { value in
                                    widgetDragIndex = index
                                    widgetDragOffset = value.translation
                                }
                                .onEnded { value in
                                    if abs(value.translation.width) > 60 && widgetOrder.count == 2 {
                                        withAnimation(.spring()) {
                                            let temp = widgetOrder[0]; widgetOrder[0] = widgetOrder[1]; widgetOrder[1] = temp
                                        }
                                        saveLayout()
                                    }
                                    withAnimation(.spring()) { widgetDragIndex = -1; widgetDragOffset = .zero }
                                }
                            : nil
                        )
                        .simultaneousGesture(
                            LongPressGesture(minimumDuration: 0.5).onEnded { _ in
                                if !isEditMode {
                                    withAnimation(.spring()) { isEditMode = true }
                                    UIImpactFeedbackGenerator(style: .medium).impactOccurred()
                                }
                            }
                        )
                    }
                }
                .padding(.horizontal, 20)
                .padding(.top, 10)
                .onAppear { loadData(); loadLayout() }
                .onReceive(NotificationCenter.default.publisher(for: NSNotification.Name("WallpaperChanged"))) { _ in
                    self.homeWallpaper = WallpaperManager.shared.getWallpaperImage(type: .home)
                }
                
                // 4x7 网格
                DraggableAppGrid(
                    gridPositions: $gridPositions, dockApps: $dockApps,
                    isEditMode: $isEditMode, isDraggingOverDock: $isDraggingOverDock,
                    draggingApp: $draggingApp, draggingOffset: $draggingOffset,
                    draggingFromCell: $draggingFromCell, maxDockApps: maxDockApps,
                    onSettingsClick: { showSettings = true },
                    onChatClick: { showChatApp = true },
                    onLayoutChanged: { saveLayout() }
                )

                Spacer()

                // Dock 栏
                ZStack {
                    RoundedRectangle(cornerRadius: 30)
                        .fill(isDraggingOverDock && dockApps.count < maxDockApps ? Color.blue.opacity(0.4) : Color.white.opacity(0.3))
                        .background(.ultraThinMaterial)
                        .clipShape(RoundedRectangle(cornerRadius: 30))
                        .frame(height: 90).padding(.horizontal, 15)
                    
                    if dockApps.isEmpty && !isEditMode {
                        Text("长按拖入应用").font(.caption).foregroundColor(.white.opacity(0.4))
                    }
                    if dockApps.isEmpty && isEditMode {
                        Text("拖拽应用到此处").font(.caption).foregroundColor(.white.opacity(0.5))
                    }
                    
                    HStack(spacing: 25) {
                        ForEach(Array(dockApps.enumerated()), id: \.element.id) { dockIndex, app in
                            DraggableDockIconView(
                                app: app, dockIndex: dockIndex,
                                isEditMode: $isEditMode,
                                gridPositions: $gridPositions,
                                dockApps: $dockApps,
                                draggingApp: $draggingApp,
                                draggingOffset: $draggingOffset,
                                colorScheme: colorScheme,
                                onTap: {
                                    if !isEditMode {
                                        if app.id == "settings" { showSettings = true }
                                        else if app.id == "chat" { showChatApp = true }
                                    }
                                },
                                onLayoutChanged: { saveLayout() },
                                gridCols: gridCols, gridRows: gridRows
                            )
                        }
                    }
                }
                .padding(.bottom, 20)
            }

            // Home Indicator
            VStack { Spacer()
                RoundedRectangle(cornerRadius: 5).fill(Color.white.opacity(0.8)).frame(width: 120, height: 5).padding(.bottom, 8)
            }

            // 拖拽浮层 - 在最顶层
            if let app = draggingApp {
                VStack(spacing: 6) {
                    ZStack {
                        if app.useImage {
                            Image(colorScheme == .dark ? "SettingsIconDark" : "SettingsIconLight")
                                .resizable().aspectRatio(contentMode: .fit).frame(width: 60, height: 60)
                        } else {
                            Text(app.icon).font(.system(size: 48))
                        }
                    }.frame(width: 60, height: 60)
                    Text(app.name).font(.caption2).foregroundColor(.white)
                }
                .scaleEffect(1.15).opacity(0.85)
                .offset(draggingOffset)
                .zIndex(10000)
                .allowsHitTesting(false)
            }

            // 点击空白退出编辑
            if isEditMode {
                Color.clear.contentShape(Rectangle())
                    .onTapGesture {
                        withAnimation(.spring()) { isEditMode = false }
                        saveLayout()
                    }
                    .zIndex(1) // 确保在壁纸之上，但在图标之下
            }

            if showSettings {
                SettingsView(showSettings: $showSettings, showActivation: $showActivation, showDisplaySettings: $showDisplaySettings, isActivated: $isActivated, expiryDate: expiryDate)
                    .transition(.move(edge: .trailing)).zIndex(1)
            }
            if showDisplaySettings {
                DisplaySettingsView(showDisplaySettings: $showDisplaySettings)
                    .transition(.move(edge: .trailing)).zIndex(1.5)
            }
            if showActivation {
                ActivationView(showActivation: $showActivation, isActivated: $isActivated, expiryDate: $expiryDate)
                    .transition(.move(edge: .trailing)).zIndex(2)
            }
            if showChatApp {
                ChatAppView(isPresented: $showChatApp)
                    .transition(.move(edge: .trailing)).zIndex(3)
            }
        }
    }
    
    func loadData() {
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
            self.city = "广州"
            self.weather = WeatherInfo(temp: "25°", description: "多云", icon: "⛅", range: "H:29° L:21°")
        }
    }
    
    func loadLayout() {
        let savedLayout = layoutManager.getLayout()
        if !savedLayout.isEmpty {
            var positions: [Int: AppIconData] = [:]
            let gridItems = savedLayout.filter { $0.area == "grid" }
            for li in gridItems {
                if let app = defaultApps.first(where: { $0.id == li.appId }), li.position >= 0, li.position < gridCols * gridRows {
                    positions[li.position] = app
                }
            }
            var orderedDock: [AppIconData] = []
            let dockItems = savedLayout.filter { $0.area == "dock" }.sorted { $0.position < $1.position }
            for li in dockItems {
                if let app = defaultApps.first(where: { $0.id == li.appId }) { orderedDock.append(app) }
            }
            let widgetItems = savedLayout.filter { $0.area == "widget" }.sorted { $0.position < $1.position }
            if !widgetItems.isEmpty { widgetOrder = widgetItems.map { $0.appId } }
            let allSavedIds = Set(savedLayout.map { $0.appId })
            for app in defaultApps {
                if !allSavedIds.contains(app.id) {
                    for i in 0..<(gridCols * gridRows) { if positions[i] == nil { positions[i] = app; break } }
                }
            }
            gridPositions = positions; dockApps = orderedDock
        } else {
            var positions: [Int: AppIconData] = [:]
            for (i, app) in defaultApps.enumerated() { if i < gridCols * gridRows { positions[i] = app } }
            gridPositions = positions; dockApps = []
        }
    }
    
    func saveLayout() {
        var items: [LayoutItem] = []
        for (cellIndex, app) in gridPositions { items.append(LayoutItem(appId: app.id, position: cellIndex, area: "grid")) }
        items += dockApps.enumerated().map { LayoutItem(appId: $1.id, position: $0, area: "dock") }
        items += widgetOrder.enumerated().map { LayoutItem(appId: $1, position: $0, area: "widget") }
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
                            if isActivated { Text("有效期至: \(expiryDate)").font(.caption).foregroundColor(.gray) }
                        }
                        Spacer()
                        Text(isActivated ? "已查看" : "未激活").foregroundColor(.gray)
                    }
                    .contentShape(Rectangle()).onTapGesture { showActivation = true }
                }
                Section(header: Text("外观")) {
                    HStack {
                        Text("显示设置"); Spacer()
                        Image(systemName: "chevron.right").foregroundColor(.gray)
                    }
                    .contentShape(Rectangle()).onTapGesture { showDisplaySettings = true }
                }
            }
            .listStyle(InsetGroupedListStyle())
            .navigationTitle("设置")
            .navigationBarItems(leading: Button("返回") { showSettings = false })
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
    
    private var machineId: String { LicenseManager.shared.getMachineId() }
    
    var body: some View {
        NavigationView {
            Form {
                Section(header: Text("机器码")) {
                    Text(machineId).foregroundColor(.gray)
                    Button(action: { UIPasteboard.general.string = machineId }) {
                        Text("复制机器码").frame(maxWidth: .infinity).foregroundColor(.white)
                    }.listRowBackground(Color.green)
                }
                Section(header: Text("激活码")) {
                    TextEditor(text: $licenseKey).frame(height: 100)
                    Button(action: { if let s = UIPasteboard.general.string { licenseKey = s } }) {
                        Text("粘贴激活码").frame(maxWidth: .infinity).foregroundColor(.white)
                    }.listRowBackground(Color.purple)
                }
                if let error = errorMessage {
                    Section { Text(error).foregroundColor(.red).font(.caption) }
                }
                Section {
                    Button(action: {
                        LicenseManager.shared.verifyLicense(licenseKey.trimmingCharacters(in: .whitespacesAndNewlines)) { result in
                            switch result {
                            case .success(_):
                                errorMessage = nil; isActivated = LicenseManager.shared.isActivated()
                                expiryDate = LicenseManager.shared.getExpirationDateString(); showActivation = false
                            case .error(let message): errorMessage = message
                            }
                        }
                    }) { Text("激活").frame(maxWidth: .infinity).foregroundColor(.white) }.listRowBackground(Color.blue)
                }
            }
            .navigationTitle("激活授权")
            .navigationBarItems(leading: Button("取消") { showActivation = false })
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
                            Text(lockWallpaper != nil ? "已设置" : "点击选择图片").font(.caption).foregroundColor(.gray)
                        }
                        Spacer()
                        if let image = lockWallpaper {
                            Image(uiImage: image).resizable().aspectRatio(contentMode: .fill).frame(width: 60, height: 60).cornerRadius(8)
                        } else { Text("🖼️").font(.largeTitle) }
                    }
                    .contentShape(Rectangle()).onTapGesture { pickerType = .lock; showingImagePicker = true }
                }
                Section(header: Text("桌面壁纸")) {
                    HStack {
                        VStack(alignment: .leading) {
                            Text("桌面壁纸设置")
                            Text(homeWallpaper != nil ? "已设置" : "点击选择图片").font(.caption).foregroundColor(.gray)
                        }
                        Spacer()
                        if let image = homeWallpaper {
                            Image(uiImage: image).resizable().aspectRatio(contentMode: .fill).frame(width: 60, height: 60).cornerRadius(8)
                        } else { Text("🖼️").font(.largeTitle) }
                    }
                    .contentShape(Rectangle()).onTapGesture { pickerType = .home; showingImagePicker = true }
                }
            }
            .listStyle(InsetGroupedListStyle())
            .navigationTitle("显示设置")
            .navigationBarItems(leading: Button("返回") { showDisplaySettings = false })
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
    func makeCoordinator() -> Coordinator { Coordinator(self) }

    class Coordinator: NSObject, UINavigationControllerDelegate, UIImagePickerControllerDelegate {
        let parent: ImagePicker
        init(_ parent: ImagePicker) { self.parent = parent }
        func imagePickerController(_ picker: UIImagePickerController, didFinishPickingMediaWithInfo info: [UIImagePickerController.InfoKey : Any]) {
            if let image = info[.originalImage] as? UIImage {
                if let fileName = WallpaperManager.shared.copyImageToStorage(image) {
                    if WallpaperManager.shared.saveWallpaper(type: parent.type, fileName: fileName) {
                        parent.selectedImage = image
                        NotificationCenter.default.post(name: NSNotification.Name("WallpaperChanged"), object: nil)
                    }
                }
            }
            parent.presentationMode.wrappedValue.dismiss()
        }
    }
}