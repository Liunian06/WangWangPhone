import Foundation
import SwiftUI

protocol GridItem {
    var id: String { get }
    var spanX: Int { get }
    var spanY: Int { get }
    var type: String { get }
}

struct AppIconData: Identifiable, Equatable, GridItem {
    let id: String
    let name: String
    let icon: String
    let colors: [Color]
    var useImage: Bool = false
    var spanX: Int = 1
    var spanY: Int = 1
    var type: String = "app"
    
    static func == (lhs: AppIconData, rhs: AppIconData) -> Bool {
        return lhs.id == rhs.id
    }
}

struct WidgetItem: Identifiable, Equatable, GridItem {
    let id: String
    let widgetType: String // "clock", "weather"
    var spanX: Int = 2
    var spanY: Int = 2
    var type: String = "widget"
    
    static func == (lhs: WidgetItem, rhs: WidgetItem) -> Bool {
        return lhs.id == rhs.id
    }
}

struct AnyGridItem: Equatable {
    let item: any GridItem
    
    static func == (lhs: AnyGridItem, rhs: AnyGridItem) -> Bool {
        return lhs.item.id == rhs.item.id
    }
}

func getDefaultApps() -> [AppIconData] {
    return [
        AppIconData(id: "phone", name: "电话", icon: "📞", colors: [.pink, .orange]),
        AppIconData(id: "chat", name: "聊天", icon: "MessagesIcon", colors: [Color(red: 0.03, green: 0.76, blue: 0.38), Color(red: 0.02, green: 0.68, blue: 0.34)], useImage: true),
        AppIconData(id: "safari", name: "Safari", icon: "🧭", colors: [.green, .blue]),
        AppIconData(id: "music", name: "音乐", icon: "🎵", colors: [.yellow, .red]),
        AppIconData(id: "camera", name: "相机", icon: "📷", colors: [.white, .gray]),
        AppIconData(id: "calendar", name: "日历", icon: "📅", colors: [.white, .gray]),
        AppIconData(id: "settings", name: "设置", icon: "SettingsIcon", colors: [.white, .gray], useImage: true),
        AppIconData(id: "persona_builder", name: "神笔马良", icon: "✨", colors: [Color(red: 1.0, green: 0.84, blue: 0.0), Color(red: 1.0, green: 0.65, blue: 0.0)]),
        AppIconData(id: "wangwang", name: "汪汪", icon: "🐶", colors: [.white, .gray]),
        // 第二批应用
        AppIconData(id: "photos", name: "照片", icon: "🖼️", colors: [.yellow, Color(red: 0.97, green: 0.85, blue: 0.0)]),
        AppIconData(id: "video", name: "视频", icon: "🎬", colors: [.purple, .pink]),
        AppIconData(id: "map", name: "地图", icon: "🗺️", colors: [Color(red: 0.4, green: 0.49, blue: 0.92), .purple]),
        AppIconData(id: "notes", name: "备忘录", icon: "📝", colors: [.yellow, .orange]),
        AppIconData(id: "calculator", name: "计算器", icon: "🔢", colors: [Color(red: 0.17, green: 0.24, blue: 0.31), Color(red: 0.3, green: 0.63, blue: 0.69)]),
        AppIconData(id: "clock_app", name: "时钟", icon: "⏰", colors: [Color(red: 0.14, green: 0.14, blue: 0.15), Color(red: 0.25, green: 0.26, blue: 0.27)]),
        AppIconData(id: "appstore", name: "应用商店", icon: "🏪", colors: [.blue, .cyan]),
        AppIconData(id: "mail", name: "邮件", icon: "📧", colors: [Color(red: 0.1, green: 0.45, blue: 0.91), .cyan]),
        AppIconData(id: "contacts", name: "通讯录", icon: "👤", colors: [Color(red: 0.88, green: 0.88, blue: 0.88), .gray]),
        AppIconData(id: "files", name: "文件", icon: "📁", colors: [.blue, Color(red: 0.1, green: 0.46, blue: 0.82)]),
        AppIconData(id: "health", name: "健康", icon: "❤️", colors: [Color(red: 1.0, green: 0.42, blue: 0.42), Color(red: 0.93, green: 0.35, blue: 0.14)]),
        AppIconData(id: "wallet", name: "钱包", icon: "💳", colors: [Color(red: 0.14, green: 0.14, blue: 0.15), Color(red: 0.25, green: 0.26, blue: 0.27)]),
        // 第三批应用
        AppIconData(id: "weather_app", name: "天气", icon: "🌤️", colors: [Color(red: 0.31, green: 0.67, blue: 0.99), .cyan]),
        AppIconData(id: "compass", name: "指南针", icon: "🧭", colors: [Color(red: 0.14, green: 0.14, blue: 0.15), Color(red: 0.25, green: 0.26, blue: 0.27)]),
        AppIconData(id: "voice_memo", name: "语音备忘", icon: "🎙️", colors: [Color(red: 1.0, green: 0.25, blue: 0.42), Color(red: 1.0, green: 0.29, blue: 0.17)]),
        AppIconData(id: "translate", name: "翻译", icon: "🌐", colors: [Color(red: 0.31, green: 0.67, blue: 0.99), .cyan]),
        AppIconData(id: "books", name: "图书", icon: "📚", colors: [.pink, Color(red: 0.98, green: 0.82, blue: 0.77)]),
        AppIconData(id: "podcast", name: "播客", icon: "🎧", colors: [.purple, Color(red: 0.29, green: 0.0, blue: 0.88)]),
        AppIconData(id: "reminder", name: "提醒事项", icon: "📋", colors: [.pink, Color(red: 0.99, green: 0.81, blue: 0.94)]),
        AppIconData(id: "facetime", name: "FaceTime", icon: "📹", colors: [.green, .mint]),
        AppIconData(id: "news", name: "新闻", icon: "📰", colors: [Color(red: 1.0, green: 0.25, blue: 0.42), Color(red: 1.0, green: 0.29, blue: 0.17)]),
        AppIconData(id: "stocks", name: "股票", icon: "📈", colors: [Color(red: 0.14, green: 0.14, blue: 0.15), Color(red: 0.25, green: 0.26, blue: 0.27)])
    ]
}

func getDefaultWidgets() -> [WidgetItem] {
    return [
        WidgetItem(id: "clock_widget", widgetType: "clock"),
        WidgetItem(id: "weather_widget", widgetType: "weather")
    ]
}

let gridColumns = 4
let gridRows = 7
let totalCells = gridColumns * gridRows

struct ClockWidget: View {
    @State private var currentTime = Date()
    let timer = Timer.publish(every: 1, on: .main, in: .common).autoconnect()
    var city: String
    
    var body: some View {
        ZStack {
            RoundedRectangle(cornerRadius: 20)
                .fill(LinearGradient(colors: [Color(red: 0.88, green: 0.76, blue: 0.99), Color(red: 0.56, green: 0.77, blue: 0.99)], startPoint: .topLeading, endPoint: .bottomTrailing))
                .frame(maxWidth: .infinity, maxHeight: .infinity)
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
        let info = weather ?? WeatherInfo(temp: "--", description: "加载中...", icon: "❓", range: "最高 -- 最低 -- | 风速 --")
        ZStack {
            RoundedRectangle(cornerRadius: 20)
                .fill(LinearGradient(colors: [Color(red: 0.18, green: 0.55, blue: 1.0), Color(red: 0.07, green: 0.76, blue: 0.91)], startPoint: .topLeading, endPoint: .bottomTrailing))
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            RoundedRectangle(cornerRadius: 20)
                .fill(LinearGradient(colors: [.black.opacity(0.06), .black.opacity(0.30)], startPoint: .top, endPoint: .bottom))
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            VStack(alignment: .leading, spacing: 10) {
                VStack(alignment: .leading, spacing: 2) {
                    Text(city)
                        .font(.system(size: 18, weight: .bold))
                        .foregroundColor(.white.opacity(0.95))
                    Text(info.temp)
                        .font(.system(size: 44, weight: .semibold))
                        .foregroundColor(.white)
                }

                VStack(alignment: .leading, spacing: 4) {
                    HStack(spacing: 6) {
                        Text(info.icon)
                            .font(.system(size: 20))
                        Text(info.description)
                            .font(.system(size: 14, weight: .semibold))
                            .foregroundColor(.white)
                            .lineLimit(1)
                    }
                    Text(info.range.isEmpty ? "最高 -- 最低 -- | 风速 --" : info.range)
                        .font(.system(size: 11, weight: .medium))
                        .foregroundColor(.white.opacity(0.92))
                        .lineLimit(1)
                }
                .padding(.horizontal, 10)
                .padding(.vertical, 8)
                .background(
                    RoundedRectangle(cornerRadius: 12)
                        .fill(Color.white.opacity(0.2))
                )
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
            .padding(14)
        }
    }
}

// MARK: - 将应用分配到多个页面
func distributeItemsToPages(allApps: [AppIconData], widgets: [WidgetItem]) -> [[Int: AnyGridItem]] {
    var pages: [[Int: AnyGridItem]] = []
    
    // 分离核心应用（聊天+设置）和其余应用
    let coreAppIds: Set<String> = ["chat", "settings", "safari", "calculator", "weather_app", "calendar", "camera", "notes"]
    let coreApps = allApps.filter { coreAppIds.contains($0.id) }
    var otherApps = allApps.filter { !coreAppIds.contains($0.id) }
    
    // 第一页：Widget + 核心应用
    var page0: [Int: AnyGridItem] = [:]
    if !widgets.isEmpty { page0[0] = AnyGridItem(item: widgets[0]) }
    if widgets.count > 1 { page0[2] = AnyGridItem(item: widgets[1]) }
    
    // 从第3行开始放核心应用（前2行被Widget占据）
    var pos = 8
    for app in coreApps {
        if pos < totalCells {
            page0[pos] = AnyGridItem(item: app)
            pos += 1
        }
    }
    pages.append(page0)
    
    // 第二页起：其余所有应用
    while !otherApps.isEmpty {
        var page: [Int: AnyGridItem] = [:]
        var pagePos = 0
        while pagePos < totalCells && !otherApps.isEmpty {
            page[pagePos] = AnyGridItem(item: otherApps.removeFirst())
            pagePos += 1
        }
        pages.append(page)
    }
    
    return pages
}

// MARK: - 单页网格视图
struct PageGridView: View {
    let pageIndex: Int
    @Binding var gridPositions: [Int: AnyGridItem]
    @Binding var dockApps: [AppIconData]
    @Binding var isEditMode: Bool
    @Binding var isDraggingOverDock: Bool
    @Binding var draggingItem: AnyGridItem?
    @Binding var draggingOffset: CGSize
    @Binding var draggingFromCell: Int
    @Binding var draggingFromPage: Int
    @Binding var city: String
    @Binding var weather: WeatherInfo?
    
    var maxDockApps: Int
    var customIcons: [String: UIImage]
    var onSettingsClick: () -> Void
    var onChatClick: () -> Void
    var onBrowserClick: () -> Void
    var onCalculatorClick: () -> Void
    var onWeatherClick: () -> Void
    var onCalendarClick: () -> Void
    var onCameraClick: () -> Void
    var onNotesClick: () -> Void
    var onPersonaBuilderClick: () -> Void
    var onLayoutChanged: () -> Void
    @Environment(\.colorScheme) var colorScheme
    
    @State private var highlightCellIndex: Int = -1
    @State private var lastScrollTime: TimeInterval = 0 // 节流控制
    @Binding var currentPage: Int
    @Binding var pageCount: Int
    
    var isActivated: Bool
    var onActivationAlert: () -> Void
    
    var body: some View {
        GeometryReader { geometry in
            let cellWidth = geometry.size.width / CGFloat(gridColumns)
            let cellHeight = geometry.size.height / CGFloat(gridRows)
            
            ZStack {
                // 高亮目标格子
                if highlightCellIndex >= 0 && highlightCellIndex < totalCells {
                    let col = highlightCellIndex % gridColumns
                    let row = highlightCellIndex / gridColumns
                    
                    if let item = draggingItem?.item {
                        if col + item.spanX <= gridColumns && row + item.spanY <= gridRows {
                            RoundedRectangle(cornerRadius: 12)
                                .stroke(Color.white.opacity(0.5), lineWidth: 2)
                                .frame(width: cellWidth * CGFloat(item.spanX) - 10, height: cellHeight * CGFloat(item.spanY) - 10)
                                .position(
                                    x: CGFloat(col) * cellWidth + (cellWidth * CGFloat(item.spanX)) / 2,
                                    y: CGFloat(row) * cellHeight + (cellHeight * CGFloat(item.spanY)) / 2
                                )
                        }
                    }
                }
                
                ForEach(0..<totalCells, id: \.self) { cellIndex in
                    if let anyItem = gridPositions[cellIndex] {
                        let item = anyItem.item
                        let col = cellIndex % gridColumns
                        let row = cellIndex / gridColumns
                        let isDragged = draggingItem?.item.id == item.id && draggingFromCell == cellIndex && draggingFromPage == pageIndex
                        
                        if !isDragged {
                            itemView(item: item, cellIndex: cellIndex, cellWidth: cellWidth, cellHeight: cellHeight)
                                .frame(width: cellWidth * CGFloat(item.spanX), height: cellHeight * CGFloat(item.spanY))
                                .position(
                                    x: CGFloat(col) * cellWidth + (cellWidth * CGFloat(item.spanX)) / 2,
                                    y: CGFloat(row) * cellHeight + (cellHeight * CGFloat(item.spanY)) / 2
                                )
                        }
                    }
                }
            }
        }
        .padding(.horizontal, 20)
    }
    
    @ViewBuilder
    func itemView(item: any GridItem, cellIndex: Int, cellWidth: CGFloat, cellHeight: CGFloat) -> some View {
        let wiggle: Double = cellIndex % 2 == 0 ? -1.5 : 1.5
        
        Group {
            if let widget = item as? WidgetItem {
                if widget.widgetType == "clock" { ClockWidget(city: city) }
                else { WeatherWidget(city: city, weather: weather) }
            } else if let app = item as? AppIconData {
                VStack(spacing: 6) {
                    ZStack {
                        if let customIcon = customIcons[app.id] {
                            Image(uiImage: customIcon)
                                .resizable()
                                .aspectRatio(contentMode: .fill)
                                .frame(width: 60, height: 60)
                                .clipShape(RoundedRectangle(cornerRadius: 12))
                        } else if app.useImage {
                            Image(colorScheme == .dark ? "\(app.icon)Dark" : "\(app.icon)Light")
                                .resizable().aspectRatio(contentMode: .fit).frame(width: 60, height: 60)
                        } else {
                            Text(app.icon).font(.system(size: 48))
                        }
                    }.frame(width: 60, height: 60)
                    Text(app.name).font(.caption2).foregroundColor(.white)
                }
            }
        }
        .contentShape(Rectangle())
        .rotationEffect(isEditMode ? .degrees(wiggle) : .degrees(0))
        .animation(isEditMode ? Animation.easeInOut(duration: 0.12 + Double(cellIndex % 3) * 0.03).repeatForever(autoreverses: true) : .linear(duration: 0.1), value: isEditMode)
        .simultaneousGesture(
            TapGesture()
                .onEnded {
                    if !isEditMode {
                        if let app = item as? AppIconData {
                            if app.id == "settings" {
                                onSettingsClick()
                            } else {
                                if isActivated {
                                    if app.id == "chat" { onChatClick() }
                                    else if app.id == "safari" { onBrowserClick() }
                                    else if app.id == "calculator" { onCalculatorClick() }
                                    else if app.id == "weather_app" { onWeatherClick() }
                                    else if app.id == "calendar" { onCalendarClick() }
                                    else if app.id == "camera" { onCameraClick() }
                                    else if app.id == "notes" { onNotesClick() }
                                    else if app.id == "persona_builder" { onPersonaBuilderClick() }
                                } else {
                                    onActivationAlert()
                                }
                            }
                        }
                    }
                }
        )
        .gesture(
            LongPressGesture(minimumDuration: 0.5)
                .sequenced(before: DragGesture(coordinateSpace: .global))
                .onChanged { value in
                    switch value {
                    case .first(true):
                        // 长按识别成功，进入编辑模式（不使用withAnimation避免闪屏）
                        if !isEditMode {
                            isEditMode = true
                            UIImpactFeedbackGenerator(style: .medium).impactOccurred()
                        }
                        if draggingItem == nil {
                            draggingItem = AnyGridItem(item: item)
                            draggingFromCell = cellIndex
                            draggingFromPage = pageIndex
                        }
                    case .second(true, let drag):
                        // 长按后开始拖动
                        if let drag = drag {
                            if draggingItem == nil {
                                draggingItem = AnyGridItem(item: item)
                                draggingFromCell = cellIndex
                                draggingFromPage = pageIndex
                            }
                            draggingOffset = drag.translation
                            
                            if item.type == "app" {
                                isDraggingOverDock = drag.location.y > UIScreen.main.bounds.height - 150
                            } else {
                                isDraggingOverDock = false
                            }
                            
                            let colOffset = Int(round(drag.translation.width / cellWidth))
                            let rowOffset = Int(round(drag.translation.height / cellHeight))
                            let curRow = cellIndex / gridColumns
                            let curCol = cellIndex % gridColumns
                            
                            let tCol = max(0, min(gridColumns - item.spanX, curCol + colOffset))
                            let tRow = max(0, min(gridRows - item.spanY, curRow + rowOffset))
                            let targetCell = tRow * gridColumns + tCol
                            
                            if !isDraggingOverDock {
                                highlightCellIndex = targetCell
                            } else {
                                highlightCellIndex = -1
                            }
                            
                            // 自动翻页
                            let screenWidth = UIScreen.main.bounds.width
                            let edgeThreshold: CGFloat = 50
                            let now = Date().timeIntervalSince1970
                            
                            if now - lastScrollTime > 0.8 { // 0.8秒冷却时间
                                if drag.location.x < edgeThreshold && currentPage > 0 {
                                    lastScrollTime = now
                                    withAnimation { currentPage -= 1 }
                                } else if drag.location.x > screenWidth - edgeThreshold && currentPage < pageCount - 1 {
                                    lastScrollTime = now
                                    withAnimation { currentPage += 1 }
                                }
                            }
                        }
                    default:
                        break
                    }
                }
                .onEnded { value in
                    if let currentItem = draggingItem?.item {
                        if isDraggingOverDock && dockApps.count < maxDockApps && currentItem is AppIconData {
                            gridPositions.removeValue(forKey: cellIndex)
                            if case .second(true, let drag) = value, let drag = drag {
                                let dockPadding: CGFloat = 15
                                let dockWidth = max(1, UIScreen.main.bounds.width - dockPadding * 2)
                                let slotWidth = dockWidth / CGFloat(maxDockApps)
                                let slot = Int(((drag.location.x - dockPadding) / slotWidth).rounded(.down))
                                let insertIndex = max(0, min(dockApps.count, slot))
                                dockApps.insert(currentItem as! AppIconData, at: insertIndex)
                            } else {
                                dockApps.append(currentItem as! AppIconData)
                            }
                        } else if highlightCellIndex != -1 {
                            let targetCell = highlightCellIndex
                            // 计算覆盖区域
                            var targetCells: [Int] = []
                            for r in 0..<currentItem.spanY {
                                for c in 0..<currentItem.spanX {
                                    targetCells.append(targetCell + r * gridColumns + c)
                                }
                            }
                            
                            // 查找冲突 items (排除自己)
                            let conflictingItems = targetCells.compactMap { gridPositions[$0] }.filter { $0.item.id != currentItem.id }
                            // 去重 (因为大 item 可能占多个格子)
                            let uniqueConflicts = Array(Set(conflictingItems.map { $0.item.id })).compactMap { id in conflictingItems.first(where: { $0.item.id == id }) }

                            if uniqueConflicts.count == 1 && uniqueConflicts[0].item.spanX == currentItem.spanX && uniqueConflicts[0].item.spanY == currentItem.spanY {
                                // Case 1: 同尺寸互换
                                let target = uniqueConflicts[0]
                                if targetCell != cellIndex {
                                    gridPositions.removeValue(forKey: cellIndex)
                                    gridPositions[targetCell] = AnyGridItem(item: currentItem)
                                    // 简单互换：把 target 放到 cellIndex
                                    // 需注意：如果是大组件，这里只更新了左上角 key，这是符合逻辑的
                                    gridPositions[cellIndex] = target
                                }
                            } else if uniqueConflicts.isEmpty {
                                // Case 2: 目标为空
                                if checkOccupancyGlobal(positions: gridPositions, startCell: targetCell, spanX: currentItem.spanX, spanY: currentItem.spanY, ignoreCell: cellIndex) {
                                    gridPositions.removeValue(forKey: cellIndex)
                                    gridPositions[targetCell] = AnyGridItem(item: currentItem)
                                }
                            } else if currentItem.spanX > 1 && uniqueConflicts.allSatisfy({ $0.item.spanX == 1 && $0.item.spanY == 1 }) {
                                // Case 3: Widget 交换 Apps（支持源/目标重叠）
                                var sourceCells: [Int] = []
                                for r in 0..<currentItem.spanY {
                                    for c in 0..<currentItem.spanX {
                                        sourceCells.append(cellIndex + r * gridColumns + c)
                                    }
                                }
                                let targetCellsSet = Set(targetCells)
                                let availableCells = sourceCells.filter { !targetCellsSet.contains($0) }
                                
                                // 1. 移除源 Widget
                                gridPositions.removeValue(forKey: cellIndex)
                                // 2. 移除目标 Apps
                                targetCells.forEach { gridPositions.removeValue(forKey: $0) }
                                // 3. 放置 Widget
                                gridPositions[targetCell] = AnyGridItem(item: currentItem)
                                // 4. 将 Apps 放入可用cells
                                var idx = 0
                                for cell in availableCells {
                                    if idx < uniqueConflicts.count && cell < totalCells {
                                        gridPositions[cell] = uniqueConflicts[idx]
                                        idx += 1
                                    }
                                }
                                // 溢出App找页面空位
                                while idx < uniqueConflicts.count {
                                    if let emptyCell = (0..<totalCells).first(where: { gridPositions[$0] == nil }) {
                                        gridPositions[emptyCell] = uniqueConflicts[idx]
                                    }
                                    idx += 1
                                }
                            }
                        }
                    }
                    
                    withAnimation(.spring()) {
                        draggingItem = nil; draggingOffset = .zero; draggingFromCell = -1; draggingFromPage = -1; isDraggingOverDock = false; highlightCellIndex = -1
                    }
                    onLayoutChanged()
                }
        )
    }
}

// MARK: - 全局占位检查
func checkOccupancyGlobal(positions: [Int: AnyGridItem], startCell: Int, spanX: Int, spanY: Int, ignoreCell: Int?) -> Bool {
    let startRow = startCell / gridColumns
    let startCol = startCell % gridColumns
    
    if startCol + spanX > gridColumns || startRow + spanY > gridRows { return false }
    
    for r in 0..<spanY {
        for c in 0..<spanX {
            for (pos, anyItem) in positions {
                if pos == ignoreCell { continue }
                let item = anyItem.item
                let itemRow = pos / gridColumns
                let itemCol = pos % gridColumns
                
                if startRow + r >= itemRow && startRow + r < itemRow + item.spanY &&
                    startCol + c >= itemCol && startCol + c < itemCol + item.spanX {
                    return false
                }
            }
        }
    }
    return true
}

// MARK: - Dock栏可拖拽图标
struct DraggableDockIconView: View {
    let app: AppIconData
    let dockIndex: Int
    @Binding var isEditMode: Bool
    @Binding var gridPositions: [[Int: AnyGridItem]]  // 所有页面
    @Binding var dockApps: [AppIconData]
    @Binding var draggingItem: AnyGridItem?
    @Binding var draggingOffset: CGSize
    let currentPage: Int
    let maxDockApps: Int
    let colorScheme: ColorScheme
    let customIcons: [String: UIImage]
    var isActivated: Bool
    var onActivationAlert: () -> Void
    var onTap: () -> Void
    var onBrowserClick: () -> Void
    var onCalculatorClick: () -> Void
    var onWeatherClick: () -> Void
    var onCalendarClick: () -> Void
    var onCameraClick: () -> Void
    var onNotesClick: () -> Void
    var onPersonaBuilderClick: () -> Void
    var onLayoutChanged: () -> Void
    
    @State private var dragOffset: CGSize = .zero
    @State private var isDragging = false
    @State private var wiggleAmount: Double = 0
    
    var body: some View {
        ZStack {
            if let customIcon = customIcons[app.id] {
                Image(uiImage: customIcon)
                    .resizable()
                    .aspectRatio(contentMode: .fill)
                    .frame(width: 60, height: 60)
                    .clipShape(RoundedRectangle(cornerRadius: 12))
            } else if app.useImage {
                Image(colorScheme == .dark ? "\(app.icon)Dark" : "\(app.icon)Light")
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
        .animation(isEditMode && !isDragging ? Animation.easeInOut(duration: 0.12 + Double(dockIndex % 3) * 0.03).repeatForever(autoreverses: true) : .linear(duration: 0.1), value: isEditMode)
        .onAppear { if isEditMode { wiggleAmount = dockIndex % 2 == 0 ? -1.5 : 1.5 } }
        .onChange(of: isEditMode) { nv in wiggleAmount = nv ? (dockIndex % 2 == 0 ? -1.5 : 1.5) : 0 }
        .simultaneousGesture(
            TapGesture()
                .onEnded {
                    if !isEditMode {
                        if app.id == "settings" {
                            onTap()
                        } else {
                            if isActivated {
                                if app.id == "chat" { onTap() }
                                else if app.id == "safari" { onBrowserClick() }
                                else if app.id == "calculator" { onCalculatorClick() }
                                else if app.id == "weather_app" { onWeatherClick() }
                                else if app.id == "calendar" { onCalendarClick() }
                                else if app.id == "camera" { onCameraClick() }
                                else if app.id == "notes" { onNotesClick() }
                                else if app.id == "persona_builder" { onPersonaBuilderClick() }
                                else { onTap() }
                            } else {
                                onActivationAlert()
                            }
                        }
                    }
                }
        )
        .gesture(
            LongPressGesture(minimumDuration: 0.5)
                .sequenced(before: DragGesture())
                .onChanged { value in
                    switch value {
                    case .first(true):
                        // 长按识别成功，进入编辑模式（不使用withAnimation避免闪屏）
                        if !isEditMode {
                            isEditMode = true
                            UIImpactFeedbackGenerator(style: .medium).impactOccurred()
                        }
                        if !isDragging {
                            isDragging = true
                            draggingItem = AnyGridItem(item: app)
                        }
                    case .second(true, let drag):
                        // 长按后开始拖动
                        if let drag = drag {
                            if !isDragging {
                                isDragging = true
                                draggingItem = AnyGridItem(item: app)
                            }
                            dragOffset = drag.translation
                            draggingOffset = drag.translation
                        }
                    default:
                        break
                    }
                }
                .onEnded { value in
                    // 提取最终的拖动值
                    let finalTranslation: CGSize
                    switch value {
                    case .second(true, let drag):
                        finalTranslation = drag?.translation ?? .zero
                    default:
                        finalTranslation = .zero
                    }
                    
                    if finalTranslation.height < -50 && dockIndex >= 0 && dockIndex < dockApps.count {
                        let movedApp = dockApps.remove(at: dockIndex)
                        var placed = false
                        // 尝试放入当前页面
                        let preferredPages = [currentPage] + (0..<gridPositions.count).filter { $0 != currentPage }
                        for page in preferredPages {
                            if page < gridPositions.count {
                                for i in 0..<totalCells {
                                    if gridPositions[page][i] == nil {
                                        gridPositions[page][i] = AnyGridItem(item: movedApp)
                                        placed = true
                                        break
                                    }
                                }
                            }
                            if placed { break }
                        }
                        if !placed {
                            dockApps.insert(movedApp, at: dockIndex)
                        }
                    } else {
                        let dockCellWidth: CGFloat = max(72, (UIScreen.main.bounds.width - 78) / CGFloat(maxDockApps))
                        let colOffset = Int(round(finalTranslation.width / dockCellWidth))
                        if colOffset != 0 {
                            let targetIdx = max(0, min(dockApps.count - 1, dockIndex + colOffset))
                            if targetIdx != dockIndex {
                                let movedApp = dockApps.remove(at: dockIndex)
                                dockApps.insert(movedApp, at: targetIdx)
                            }
                        }
                    }
                    
                    withAnimation(.spring()) {
                        isDragging = false; dragOffset = .zero
                        draggingItem = nil; draggingOffset = .zero
                    }
                    onLayoutChanged()
                }
        )
    }
}

// MARK: - 页面指示器
struct PageIndicator: View {
    let pageCount: Int
    let currentPage: Int
    
    var body: some View {
        HStack(spacing: 8) {
            ForEach(0..<pageCount, id: \.self) { index in
                Circle()
                    .fill(index == currentPage ? Color.white : Color.white.opacity(0.4))
                    .frame(width: index == currentPage ? 8 : 6, height: index == currentPage ? 8 : 6)
            }
        }
        .padding(.vertical, 8)
    }
}

// MARK: - 设置组件
struct SettingsRow: View {
    let title: String
    var value: String = ""
    var textColor: Color = .primary
    let action: () -> Void
    
    var body: some View {
        Button(action: action) {
            HStack {
                Text(title).foregroundColor(textColor)
                Spacer()
                if !value.isEmpty {
                    Text(value).foregroundColor(.gray)
                }
                Image(systemName: "chevron.right").font(.system(size: 14, weight: .semibold)).foregroundColor(.gray)
            }
            .padding()
            .background(Color(UIColor.secondarySystemGroupedBackground))
        }
    }
}

struct SettingsView: View {
    @Binding var showSettings: Bool
    @Binding var showActivation: Bool
    @Binding var showDisplaySettings: Bool
    @Binding var showChatApiPresets: Bool
    @Binding var showImageApiPresets: Bool
    @Binding var showVoiceApiPresets: Bool
    @Binding var isActivated: Bool
    let expiryDate: String
    
    @State private var showResetConfirm = false
    
    var body: some View {
        NavigationView {
            ZStack {
                Color(UIColor.systemGroupedBackground).ignoresSafeArea()
                
                ScrollView {
                    VStack(alignment: .leading, spacing: 20) {
                        Section(header: Text("激活与授权").font(.caption).foregroundColor(.gray).padding(.horizontal)) {
                            VStack(spacing: 0) {
                                SettingsRow(title: "软件激活", value: isActivated ? "已查看" : "未激活") {
                                    showActivation = true
                                }
                                if isActivated {
                                    Divider().padding(.leading)
                                    HStack {
                                        Text("有效期至").foregroundColor(.primary)
                                        Spacer()
                                        Text(expiryDate).foregroundColor(.gray)
                                    }
                                    .padding()
                                    .background(Color(UIColor.secondarySystemGroupedBackground))
                                }
                            }
                            .cornerRadius(10)
                            .padding(.horizontal)
                        }
                        
                        Section(header: Text("外观").font(.caption).foregroundColor(.gray).padding(.horizontal)) {
                            SettingsRow(title: "显示设置") {
                                showDisplaySettings = true
                            }
                            .cornerRadius(10)
                            .padding(.horizontal)
                        }
                        
                        Section(header: Text("API预设").font(.caption).foregroundColor(.gray).padding(.horizontal)) {
                            VStack(spacing: 0) {
                                SettingsRow(title: "聊天API预设") {
                                    showChatApiPresets = true
                                }
                                Divider().padding(.leading)
                                SettingsRow(title: "生图API预设") {
                                    showImageApiPresets = true
                                }
                                Divider().padding(.leading)
                                SettingsRow(title: "语音API预设") {
                                    showVoiceApiPresets = true
                                }
                            }
                            .cornerRadius(10)
                            .padding(.horizontal)
                        }
                        
                        Section(header: Text("系统").font(.caption).foregroundColor(.gray).padding(.horizontal)) {
                            SettingsRow(title: "恢复默认设置", textColor: .red) {
                                showResetConfirm = true
                            }
                            .cornerRadius(10)
                            .padding(.horizontal)
                        }
                    }
                    .padding(.vertical)
                }
            }
            .navigationTitle("设置")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("返回") { showSettings = false }
                }
            }
            .alert(isPresented: $showResetConfirm) {
                Alert(
                    title: Text("恢复默认设置"),
                    message: Text("此操作将清除所有自定义布局、壁纸、天气缓存和用户资料，且无法撤销。是否继续？"),
                    primaryButton: .destructive(Text("确定")) {
                        if LayoutManager.shared.resetToDefaultSettings() {
                            // 由于 iOS 状态在各个组件内部，重置后最简单的办法是提示或重新加载关键数据
                            // 在这个 Demo 中，我们通过关闭设置页并让 HomeScreen 重新 load 来体现
                            showSettings = false
                            // 实际项目中可能需要更细粒度的通知机制
                        }
                    },
                    secondaryButton: .cancel()
                )
            }
        }
    }
}

struct DisplaySettingsView: View {
    @Binding var showDisplaySettings: Bool
    @Binding var showIconCustomization: Bool
    var onIconChanged: () -> Void
    
    var body: some View {
        NavigationView {
            ZStack {
                Color(UIColor.systemGroupedBackground).ignoresSafeArea()
                
                ScrollView {
                    VStack(alignment: .leading, spacing: 20) {
                        Section(header: Text("壁纸").font(.caption).foregroundColor(.gray).padding(.horizontal)) {
                            Text("壁纸设置功能待实现").padding().background(Color(UIColor.secondarySystemGroupedBackground)).cornerRadius(10).padding(.horizontal)
                        }
                        
                        Section(header: Text("桌面图标").font(.caption).foregroundColor(.gray).padding(.horizontal)) {
                            Button(action: { showIconCustomization = true }) {
                                HStack {
                                    Text("桌面图标设置").foregroundColor(.primary)
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
            .navigationTitle("显示设置")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("返回") { showDisplaySettings = false }
                }
            }
        }
    }
}

// MARK: - 主屏幕
struct HomeScreen: View {
    @State private var allPages: [[Int: AnyGridItem]] = []
    @State private var dockApps: [AppIconData] = []
    @State private var isEditMode = false
    @State private var isDraggingOverDock = false
    @State private var homeWallpaper: UIImage? = WallpaperManager.shared.getWallpaperImage(type: .home)
    @State private var currentPage: Int = 0
    
    @State private var city: String = "..."
    @State private var weather: WeatherInfo? = nil
    @State private var showSettings = false
    @State private var showChatApp = false
    @State private var showBrowserApp = false
    @State private var showCalculatorApp = false
    @State private var showWeatherApp = false
    @State private var showCalendarApp = false
    @State private var showCameraApp = false
    @State private var showNotesApp = false
    @State private var showPersonaBuilderApp = false
    @State private var showActivation = false
    @State private var showDisplaySettings = false
    @State private var showIconCustomization = false
    @State private var showChatApiPresets = false
    @State private var showImageApiPresets = false
    @State private var showVoiceApiPresets = false
    @State private var isActivated = LicenseManager.shared.isActivated()
    @State private var expiryDate = LicenseManager.shared.getExpirationDateString()
    
    @State private var showActivationAlert = false
    @State private var layoutReloadTrigger = UUID()
    
    @State private var draggingItem: AnyGridItem? = nil
    @State private var draggingOffset: CGSize = .zero
    @State private var draggingFromCell: Int = -1
    @State private var draggingFromPage: Int = -1
    
    @State private var customIcons: [String: UIImage] = [:]
    
    @Environment(\.colorScheme) var colorScheme
    
    private let layoutManager = LayoutManager.shared
    private let iconManager = IconCustomizationManager.shared
    private let defaultApps = getDefaultApps()
    private let defaultWidgets = getDefaultWidgets()
    private let maxDockApps = 4

    var body: some View {
        ZStack {
            if let wallpaper = homeWallpaper {
                Image(uiImage: wallpaper).resizable().aspectRatio(contentMode: .fill).ignoresSafeArea()
            } else {
                Color(red: 0.17, green: 0.17, blue: 0.17).ignoresSafeArea()
            }
            
            VStack(spacing: 0) {
                Spacer().frame(height: 10)
                
                // TabView 分页滑动
                TabView(selection: $currentPage) {
                    ForEach(0..<allPages.count, id: \.self) { pageIndex in
                        PageGridView(
                            pageIndex: pageIndex,
                            currentPage: $currentPage,
                            pageCount: Binding(get: { allPages.count }, set: { _ in }),
                            gridPositions: Binding(
                                get: { pageIndex < allPages.count ? allPages[pageIndex] : [:] },
                                set: { newValue in
                                    if pageIndex < allPages.count {
                                        allPages[pageIndex] = newValue
                                    }
                                }
                            ),
                            dockApps: $dockApps,
                            isEditMode: $isEditMode,
                            isDraggingOverDock: $isDraggingOverDock,
                            draggingItem: $draggingItem,
                            draggingOffset: $draggingOffset,
                            draggingFromCell: $draggingFromCell,
                            draggingFromPage: $draggingFromPage,
                            city: $city,
                            weather: $weather,
                            maxDockApps: maxDockApps,
                            customIcons: customIcons,
                            onSettingsClick: { showSettings = true },
                            onChatClick: { showChatApp = true },
                            onBrowserClick: { showBrowserApp = true },
                            onCalculatorClick: { showCalculatorApp = true },
                            onWeatherClick: { showWeatherApp = true },
                            onCalendarClick: { showCalendarApp = true },
                            onCameraClick: { showCameraApp = true },
                            onNotesClick: { showNotesApp = true },
                            onPersonaBuilderClick: { showPersonaBuilderApp = true },
                            onLayoutChanged: { saveLayout() },
                            isActivated: isActivated,
                            onActivationAlert: { showActivationAlert = true }
                        )
                        .id(layoutReloadTrigger)
                        .tag(pageIndex)
                    }
                }
                .tabViewStyle(.page(indexDisplayMode: .never))
                
                // 页面指示器
                if allPages.count > 1 {
                    PageIndicator(pageCount: allPages.count, currentPage: currentPage)
                }

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
                    
                    HStack(spacing: 0) {
                        ForEach(0..<maxDockApps, id: \.self) { slotIndex in
                            ZStack {
                                if slotIndex < dockApps.count {
                                    let app = dockApps[slotIndex]
                                    DraggableDockIconView(
                                        app: app, dockIndex: slotIndex,
                                        isEditMode: $isEditMode,
                                        gridPositions: $allPages,
                                        dockApps: $dockApps,
                                        draggingItem: $draggingItem,
                                        draggingOffset: $draggingOffset,
                                        currentPage: currentPage,
                                        maxDockApps: maxDockApps,
                                        colorScheme: colorScheme,
                                        customIcons: customIcons,
                                        isActivated: isActivated,
                                        onActivationAlert: { showActivationAlert = true },
                                        onTap: {
                                            if !isEditMode {
                                                if app.id == "settings" { showSettings = true }
                                                else if app.id == "chat" { showChatApp = true }
                                            }
                                        },
                                        onBrowserClick: { showBrowserApp = true },
                                        onCalculatorClick: { showCalculatorApp = true },
                                        onWeatherClick: { showWeatherApp = true },
                                        onCalendarClick: { showCalendarApp = true },
                                        onCameraClick: { showCameraApp = true },
                                        onNotesClick: { showNotesApp = true },
                                        onPersonaBuilderClick: { showPersonaBuilderApp = true },
                                        onLayoutChanged: { saveLayout() }
                                    )
                                }
                            }
                            .frame(maxWidth: .infinity)
                        }
                    }
                    .padding(.horizontal, 24)
                }
                .padding(.bottom, 20)
            }

            // Home Indicator
            VStack { Spacer()
                RoundedRectangle(cornerRadius: 5).fill(Color.white.opacity(0.8)).frame(width: 120, height: 5).padding(.bottom, 8)
            }

            // 拖拽浮层
            if let anyItem = draggingItem {
                let item = anyItem.item
                // 动态计算浮层尺寸
                let screenWidth = UIScreen.main.bounds.width
                let cellWidth = (screenWidth - 40) / CGFloat(gridColumns) // 假设 padding 20*2
                let cellHeight = cellWidth * 1.2 // 估算比例
                
                let itemWidth = cellWidth * CGFloat(item.spanX)
                let itemHeight = cellHeight * CGFloat(item.spanY)
                
                ZStack {
                    if let widget = item as? WidgetItem {
                        if widget.widgetType == "clock" { ClockWidget(city: city) }
                        else { WeatherWidget(city: city, weather: weather) }
                    } else if let app = item as? AppIconData {
                        VStack(spacing: 6) {
                            ZStack {
                                if let customIcon = customIcons[app.id] {
                                    Image(uiImage: customIcon)
                                        .resizable()
                                        .aspectRatio(contentMode: .fill)
                                        .frame(width: 60, height: 60)
                                        .clipShape(RoundedRectangle(cornerRadius: 12))
                                } else if app.useImage {
                                    Image(colorScheme == .dark ? "\(app.icon)Dark" : "\(app.icon)Light")
                                        .resizable().aspectRatio(contentMode: .fit).frame(width: 60, height: 60)
                                } else {
                                    Text(app.icon).font(.system(size: 48))
                                }
                            }.frame(width: 60, height: 60)
                            Text(app.name).font(.caption2).foregroundColor(.white)
                        }
                    }
                }
                .frame(width: itemWidth, height: itemHeight)
                .scaleEffect(1.15).opacity(0.85)
                .offset(draggingOffset)
                .zIndex(10000)
                .allowsHitTesting(false)
            }

            // 点击空白退出编辑 - 使用低 zIndex 避免阻挡拖拽
            if isEditMode {
                Color.clear.contentShape(Rectangle())
                    .onTapGesture {
                        isEditMode = false
                        saveLayout()
                    }
                    .zIndex(-1)
                    .allowsHitTesting(true)
            }

            if showSettings {
                SettingsView(showSettings: $showSettings, showActivation: $showActivation, showDisplaySettings: $showDisplaySettings, showChatApiPresets: $showChatApiPresets, showImageApiPresets: $showImageApiPresets, showVoiceApiPresets: $showVoiceApiPresets, isActivated: $isActivated, expiryDate: expiryDate)
                    .transition(.move(edge: .trailing)).zIndex(1)
                    .onDisappear {
                        loadLayout()
                        layoutReloadTrigger = UUID()
                        homeWallpaper = WallpaperManager.shared.getWallpaperImage(type: .home)
                    }
            }
            if showChatApiPresets {
                ApiPresetListView(type: "chat", title: "聊天API预设", showView: $showChatApiPresets)
                    .transition(.move(edge: .trailing)).zIndex(1.5)
            }
            if showImageApiPresets {
                ApiPresetListView(type: "image", title: "生图API预设", showView: $showImageApiPresets)
                    .transition(.move(edge: .trailing)).zIndex(1.5)
            }
            if showVoiceApiPresets {
                ApiPresetListView(type: "voice", title: "语音API预设", showView: $showVoiceApiPresets)
                    .transition(.move(edge: .trailing)).zIndex(1.5)
            }
            if showDisplaySettings {
                DisplaySettingsView(showDisplaySettings: $showDisplaySettings, showIconCustomization: $showIconCustomization, onIconChanged: {
                    layoutReloadTrigger = UUID()
                    loadCustomIcons()
                })
                    .transition(.move(edge: .trailing)).zIndex(1.5)
            }
            if showIconCustomization {
                IconCustomizationView(showIconCustomization: $showIconCustomization, onIconChanged: {
                    layoutReloadTrigger = UUID()
                    loadCustomIcons()
                })
                    .transition(.move(edge: .trailing)).zIndex(1.6)
            }
            if showActivation {
                ActivationView(showActivation: $showActivation, isActivated: $isActivated, expiryDate: $expiryDate)
                    .transition(.move(edge: .trailing)).zIndex(2)
            }
            if showChatApp {
                ChatAppView(isPresented: $showChatApp)
                    .transition(.move(edge: .trailing)).zIndex(3)
            }
            if showBrowserApp {
                BrowserAppView(isPresented: $showBrowserApp)
                    .transition(.move(edge: .trailing)).zIndex(4)
            }
            if showCalculatorApp {
                CalculatorAppView(isPresented: $showCalculatorApp)
                    .transition(.move(edge: .trailing)).zIndex(5)
            }
            if showWeatherApp {
                WeatherAppView(isPresented: $showWeatherApp)
                    .transition(.move(edge: .trailing)).zIndex(6)
            }
            if showCalendarApp {
                CalendarAppView(isPresented: $showCalendarApp)
                    .transition(.move(edge: .trailing)).zIndex(7)
            }
            if showCameraApp {
                CameraAppView(isPresented: $showCameraApp)
                    .transition(.move(edge: .trailing)).zIndex(8)
            }
            if showNotesApp {
                NotesAppView(isPresented: $showNotesApp)
                    .transition(.move(edge: .trailing)).zIndex(9)
            }
            if showPersonaBuilderApp {
                PersonaCardListView()
                    .transition(.move(edge: .trailing)).zIndex(10)
            }
        }
        .alert(isPresented: $showActivationAlert) {
            Alert(
                title: Text("未激活"),
                message: Text("请先激活软件"),
                primaryButton: .default(Text("去激活")) {
                    showSettings = true
                    showActivation = true
                },
                secondaryButton: .cancel()
            )
        }
        .onAppear {
            loadLayout()
            loadWeatherData()
            loadCustomIcons()
        }
    }
    
    func loadCustomIcons() {
        let records = iconManager.getAllCustomIcons()
        var icons: [String: UIImage] = [:]
        for record in records {
            if let image = iconManager.getCustomIconImage(appId: record.appId) {
                icons[record.appId] = image
            }
        }
        customIcons = icons
    }
    
    /// 带缓存的天气加载逻辑
    /// 1. 先查数据库缓存，如果今天已请求过则直接使用缓存
    /// 2. 如果没有缓存，则请求网络并保存到数据库
    func loadWeatherData() {
        let weatherCache = WeatherCacheManager.shared

        Task {
            let currentCity = await resolveCurrentCity(weatherCache: weatherCache)
            await MainActor.run {
                self.city = currentCity
            }

            if let cached = weatherCache.getTodayWeatherCache(city: currentCity),
               !isUnknownWeather(temp: cached.temp, description: cached.description) {
                let localizedDescription = localizedWeatherDescription(cached.description)
                await MainActor.run {
                    self.weather = WeatherInfo(
                        temp: cached.temp,
                        description: localizedDescription,
                        icon: weatherIcon(for: localizedDescription),
                        range: cached.range
                    )
                }
                print("WeatherCache: 使用今日缓存 - \(currentCity)")
            } else {
                let freshWeather = await fetchWeather(city: currentCity)
                    ?? WeatherInfo(temp: "--", description: "天气未知", icon: "❓", range: "风力 --")

                await MainActor.run {
                    self.weather = freshWeather
                }

                if !isUnknownWeather(temp: freshWeather.temp, description: freshWeather.description) {
                    let record = WeatherCacheRecord(
                        city: currentCity,
                        temp: freshWeather.temp,
                        description: freshWeather.description,
                        icon: freshWeather.icon,
                        range: freshWeather.range,
                        requestDate: WeatherCacheManager.getTodayDateString(),
                        updatedAt: 0
                    )
                    _ = weatherCache.saveWeatherCache(record)
                }
                _ = weatherCache.clearExpiredCache()
                print("WeatherCache: 已请求天气，未知结果不缓存 - \(currentCity)")
            }
        }
    }

    private func resolveCurrentCity(weatherCache: WeatherCacheManager) async -> String {
        if let manualCity = weatherCache.getManualLocation(), !manualCity.isEmpty {
            return manualCity
        }
        if let cachedCity = weatherCache.getCachedLocation(), !cachedCity.isEmpty {
            return cachedCity
        }
        if let cityFromIp = await fetchCityFromIp(), !cityFromIp.isEmpty {
            _ = weatherCache.saveLocationCache(city: cityFromIp)
            return cityFromIp
        }
        return "北京"
    }

    private func fetchCityFromIp() async -> String? {
        guard let url = URL(string: "https://myip.ipip.net/") else { return nil }

        var request = URLRequest(url: url)
        request.httpMethod = "GET"
        request.setValue("WangWangPhone/1.0", forHTTPHeaderField: "User-Agent")

        do {
            let (data, response) = try await URLSession.shared.data(for: request)
            guard let httpResponse = response as? HTTPURLResponse, (200...299).contains(httpResponse.statusCode) else {
                return nil
            }
            let text = String(data: data, encoding: .utf8) ?? String(decoding: data, as: UTF8.self)
            return parseCityFromIpResponse(text)
        } catch {
            print("Location fetch failed: \(error.localizedDescription)")
            return nil
        }
    }

    private func parseCityFromIpResponse(_ responseText: String) -> String? {
        let ignoreKeywords: Set<String> = ["中国", "电信", "移动", "联通", "铁通", "教育网", "鹏博士", "宽带", "公司", "网络"]
        let source: String
        if let range = responseText.range(of: "来自于：") {
            source = String(responseText[range.upperBound...])
        } else if let range = responseText.range(of: "来自于:") {
            source = String(responseText[range.upperBound...])
        } else {
            source = responseText
        }

        let separators = CharacterSet.whitespacesAndNewlines.union(CharacterSet(charactersIn: "，,:："))
        let tokens = source
            .components(separatedBy: separators)
            .map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }
            .filter { !$0.isEmpty }

        for token in tokens.reversed() {
            let cleaned = sanitizeCityName(token)
            if cleaned.isEmpty { continue }
            if ignoreKeywords.contains(cleaned) { continue }
            if cleaned.range(of: #"^\d+\.\d+\.\d+\.\d+$"#, options: .regularExpression) != nil { continue }
            return cleaned
        }
        return nil
    }

    private func sanitizeCityName(_ city: String) -> String {
        city
            .trimmingCharacters(in: .whitespacesAndNewlines)
            .replacingOccurrences(of: "特别行政区", with: "")
            .replacingOccurrences(of: "自治区", with: "")
            .replacingOccurrences(of: "市", with: "")
    }

    private func cityToPinyin(_ city: String) -> String {
        let cleaned = sanitizeCityName(city)
        let latin = cleaned.applyingTransform(.toLatin, reverse: false) ?? cleaned
        let noTone = latin.folding(options: [.diacriticInsensitive, .widthInsensitive], locale: Locale(identifier: "zh_CN"))
        let letters = noTone.replacingOccurrences(of: "[^A-Za-z\\s]", with: " ", options: .regularExpression)
        let compact = letters
            .components(separatedBy: .whitespacesAndNewlines)
            .filter { !$0.isEmpty }
            .joined()
            .lowercased()
        return compact.isEmpty ? cleaned.lowercased() : compact
    }

    private func normalizeTemperature(_ raw: String) -> String {
        if let range = raw.range(of: #"[-+]?\d+"#, options: .regularExpression) {
            let number = raw[range].replacingOccurrences(of: "+", with: "")
            return "\(number)°"
        }
        return raw.replacingOccurrences(of: " ", with: "")
    }

    private func containsChinese(_ text: String) -> Bool {
        text.unicodeScalars.contains { $0.value >= 0x4E00 && $0.value <= 0x9FFF }
    }

    private func localizedWeatherDescription(_ rawDescription: String) -> String {
        let cleaned = rawDescription.trimmingCharacters(in: .whitespacesAndNewlines)
        if cleaned.isEmpty { return "天气未知" }
        if containsChinese(cleaned) { return cleaned }

        let lower = cleaned.lowercased()
        if lower == "unknown" || lower == "n/a" || lower == "--" { return "天气未知" }

        let level: String
        if lower.contains("heavy") {
            level = "大"
        } else if lower.contains("moderate") {
            level = "中"
        } else if lower.contains("light") || lower.contains("patchy") {
            level = "小"
        } else {
            level = ""
        }

        func withLevel(_ base: String) -> String {
            level.isEmpty ? base : "\(level)\(base)"
        }

        if lower.contains("thunder") || lower.contains("storm") { return "雷暴" }
        if lower.contains("sleet") { return withLevel("雨夹雪") }
        if lower.contains("snow") || lower.contains("blizzard") { return withLevel("雪") }
        if lower.contains("hail") { return "冰雹" }
        if lower.contains("drizzle") { return level.isEmpty ? "毛毛雨" : withLevel("雨") }
        if lower.contains("shower") { return "阵雨" }
        if lower.contains("rain") { return withLevel("雨") }
        if lower.contains("fog") || lower.contains("mist") || lower.contains("haze") { return "有雾" }
        if lower.contains("overcast") { return "阴天" }
        if lower.contains("partly cloudy") { return "局部多云" }
        if lower.contains("cloudy") || lower.contains("cloud") { return "多云" }
        if lower.contains("clear") || lower.contains("sunny") || lower.contains("sun") { return "晴" }
        if lower.contains("wind") || lower.contains("breeze") { return "有风" }
        return cleaned
    }

    private func weatherIcon(for description: String) -> String {
        let text = description.lowercased()
        if text.contains("thunder") || text.contains("storm") || text.contains("雷") { return "⛈️" }
        if text.contains("snow") || text.contains("sleet") || text.contains("雪") { return "❄️" }
        if text.contains("rain") || text.contains("drizzle") || text.contains("shower") || text.contains("雨") { return "🌧️" }
        if text.contains("fog") || text.contains("mist") || text.contains("haze") || text.contains("雾") { return "🌫️" }
        if text.contains("cloud") || text.contains("overcast") || text.contains("云") || text.contains("阴") { return "⛅" }
        if text.contains("sun") || text.contains("clear") || text.contains("晴") { return "☀️" }
        return "🌤️"
    }

    private func formatWindKmph(_ raw: String) -> String {
        let cleaned = raw.trimmingCharacters(in: .whitespacesAndNewlines)
        if cleaned.isEmpty || cleaned == "--" { return "--" }
        if cleaned.lowercased().contains("km/h") {
            return cleaned.replacingOccurrences(of: "km/h", with: "公里/小时", options: [.caseInsensitive])
        }
        return "\(cleaned) 公里/小时"
    }

    private func buildRangeText(maxTemp: String, minTemp: String, windKmph: String) -> String {
        let maxText = maxTemp.isEmpty ? "--" : normalizeTemperature(maxTemp)
        let minText = minTemp.isEmpty ? "--" : normalizeTemperature(minTemp)
        let windText = formatWindKmph(windKmph)
        return "最高 \(maxText) 最低 \(minText) | 风速 \(windText)"
    }

    private func isUnknownWeather(temp: String, description: String) -> Bool {
        let normalizedDesc = description.trimmingCharacters(in: .whitespacesAndNewlines)
        if normalizedDesc.isEmpty { return true }
        let lowerDesc = normalizedDesc.lowercased()
        if normalizedDesc.contains("未知") || lowerDesc == "unknown" { return true }

        let normalizedTemp = temp.trimmingCharacters(in: .whitespacesAndNewlines)
        return normalizedTemp == "--" && (normalizedDesc == "--" || lowerDesc == "n/a")
    }

    private func fetchWeather(city: String) async -> WeatherInfo? {
        let cityPinyin = cityToPinyin(city)
        guard let encodedCity = cityPinyin.addingPercentEncoding(withAllowedCharacters: .urlPathAllowed),
              let url = URL(string: "https://wttr.in/\(encodedCity)?format=j1") else {
            return nil
        }

        var request = URLRequest(url: url)
        request.httpMethod = "GET"
        request.setValue("WangWangPhone/1.0", forHTTPHeaderField: "User-Agent")

        do {
            let (data, response) = try await URLSession.shared.data(for: request)
            guard let httpResponse = response as? HTTPURLResponse, (200...299).contains(httpResponse.statusCode) else {
                return nil
            }

            guard let jsonObject = try JSONSerialization.jsonObject(with: data) as? [String: Any] else {
                return nil
            }

            let current = (jsonObject["current_condition"] as? [[String: Any]])?.first
            let today = (jsonObject["weather"] as? [[String: Any]])?.first

            let zhDesc = ((current?["lang_zh"] as? [[String: Any]])?.first?["value"] as? String)?
                .trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
            let enDesc = ((current?["weatherDesc"] as? [[String: Any]])?.first?["value"] as? String)?
                .trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
            let rawDescription = zhDesc.isEmpty ? (enDesc.isEmpty ? "天气未知" : enDesc) : zhDesc
            let description = localizedWeatherDescription(rawDescription)

            let temperature = (current?["temp_C"] as? String)?
                .trimmingCharacters(in: .whitespacesAndNewlines) ?? "--"
            let windKmph = (current?["windspeedKmph"] as? String)?
                .trimmingCharacters(in: .whitespacesAndNewlines) ?? "--"
            let maxTemp = (today?["maxtempC"] as? String)?
                .trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
            let minTemp = (today?["mintempC"] as? String)?
                .trimmingCharacters(in: .whitespacesAndNewlines) ?? ""

            return WeatherInfo(
                temp: normalizeTemperature(temperature),
                description: description,
                icon: weatherIcon(for: description),
                range: buildRangeText(maxTemp: maxTemp, minTemp: minTemp, windKmph: windKmph)
            )
        } catch {
            print("Weather fetch failed: \(error.localizedDescription)")
            return nil
        }
    }
    
    func loadLayout() {
        let savedLayout = layoutManager.getLayout()
        var pages: [[Int: AnyGridItem]] = []
        
        if savedLayout.isEmpty {
            allPages = distributeItemsToPages(allApps: defaultApps, widgets: defaultWidgets)
            dockApps = []
            return
        }
        var orderedDock: [AppIconData] = []

        if !savedLayout.isEmpty {
            var pageMap: [Int: [Int: AnyGridItem]] = [:]
            
            let gridItems = savedLayout.filter { $0.area.hasPrefix("grid") }
            for li in gridItems {
                let pageIdx: Int
                if li.area == "grid" {
                    pageIdx = 0
                } else {
                    pageIdx = Int(li.area.replacingOccurrences(of: "grid_", with: "")) ?? 0
                }
                if pageMap[pageIdx] == nil { pageMap[pageIdx] = [:] }
                
                if let app = defaultApps.first(where: { $0.id == li.appId }) {
                    pageMap[pageIdx]![li.position] = AnyGridItem(item: app)
                } else if let widget = defaultWidgets.first(where: { $0.id == li.appId }) {
                    pageMap[pageIdx]![li.position] = AnyGridItem(item: widget)
                }
            }
            
            let dockItems = savedLayout.filter { $0.area == "dock" }.sorted { $0.position < $1.position }
            for li in dockItems {
                if let app = defaultApps.first(where: { $0.id == li.appId }) { orderedDock.append(app) }
            }
            
            let maxPage = pageMap.keys.max() ?? 0
            for i in 0...maxPage {
                pages.append(pageMap[i] ?? [:])
            }
            
            // 补充缺失的应用和组件
            let allSavedIds = Set(savedLayout.map { $0.appId })
            
            if pages.isEmpty { pages.append([:]) }
            
            for widget in defaultWidgets {
                if !allSavedIds.contains(widget.id) {
                    for i in 0..<totalCells {
                        if checkOccupancyGlobal(positions: pages[0], startCell: i, spanX: widget.spanX, spanY: widget.spanY, ignoreCell: nil) {
                            pages[0][i] = AnyGridItem(item: widget)
                            break
                        }
                    }
                }
            }
            
            for app in defaultApps {
                if !allSavedIds.contains(app.id) {
                    var placed = false
                    for pageIdx in 0..<pages.count {
                        for i in 0..<totalCells {
                            if pages[pageIdx][i] == nil {
                                pages[pageIdx][i] = AnyGridItem(item: app)
                                placed = true
                                break
                            }
                        }
                        if placed { break }
                    }
                    if !placed {
                        pages.append([0: AnyGridItem(item: app)])
                    }
                }
            }
        } else {
            pages = distributeItemsToPages(allApps: defaultApps, widgets: defaultWidgets)
        }
        
        allPages = pages
        dockApps = orderedDock
    }
    
    func saveLayout() {
        var items: [LayoutItem] = []
        for (pageIdx, page) in allPages.enumerated() {
            let areaName = pageIdx == 0 ? "grid" : "grid_\(pageIdx)"
            for (cellIndex, anyItem) in page {
                items.append(LayoutItem(appId: anyItem.item.id, position: cellIndex, area: areaName))
            }
        }
        items += dockApps.enumerated().map { LayoutItem(appId: $1.id, position: $0, area: "dock") }
        _ = layoutManager.saveLayout(items)
    }
}
