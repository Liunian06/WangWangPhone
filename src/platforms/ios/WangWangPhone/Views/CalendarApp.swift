import SwiftUI

struct CalendarEvent: Identifiable {
    let id: String
    let title: String
    let location: String
    let time: String
    let color: Color
    let date: Date
}

struct CalendarAppView: View {
    @Binding var isPresented: Bool
    @State private var selectedDate = Date()
    @State private var currentMonth = Date()
    
    let calendar = Calendar.current
    let daysOfWeek = ["日", "一", "二", "三", "四", "五", "六"]
    
    // 示例事件数据
    var events: [CalendarEvent] {
        let today = Date()
        let tomorrow = calendar.date(byAdding: .day, value: 1, to: today)!
        let dayAfter = calendar.date(byAdding: .day, value: 2, to: today)!
        let threeDaysLater = calendar.date(byAdding: .day, value: 3, to: today)!
        
        return [
            CalendarEvent(id: "1", title: "开发任务", location: "广州", time: "全天", color: .red, date: today),
            CalendarEvent(id: "2", title: "团队周会", location: "线上会议", time: "10:00 - 11:00", color: .blue, date: today),
            CalendarEvent(id: "3", title: "午餐约会", location: "天河区", time: "12:00 - 13:00", color: .orange, date: today),
            CalendarEvent(id: "4", title: "代码评审", location: "办公室", time: "14:00 - 15:30", color: .purple, date: today),
            CalendarEvent(id: "5", title: "项目发布会", location: "会议室A", time: "09:00 - 10:00", color: .green, date: tomorrow),
            CalendarEvent(id: "6", title: "客户会议", location: "线上", time: "15:00 - 16:00", color: .pink, date: tomorrow),
            CalendarEvent(id: "7", title: "健身", location: "健身房", time: "18:00 - 19:00", color: .blue, date: dayAfter),
            CalendarEvent(id: "8", title: "读书会", location: "图书馆", time: "19:30 - 21:00", color: .purple, date: threeDaysLater)
        ]
    }
    
    var selectedEvents: [CalendarEvent] {
        events.filter { calendar.isDate($0.date, inSameDayAs: selectedDate) }
    }
    
    var eventDates: Set<Date> {
        Set(events.map { calendar.startOfDay(for: $0.date) })
    }
    
    var body: some View {
        VStack(spacing: 0) {
            // Header with month navigation
            HStack {
                HStack(spacing: 16) {
                    Button(action: {
                        withAnimation {
                            currentMonth = calendar.date(byAdding: .month, value: -1, to: currentMonth)!
                        }
                    }) {
                        Image(systemName: "chevron.left")
                            .foregroundColor(.red)
                            .fontWeight(.medium)
                    }
                    
                    Text(monthYearString(from: currentMonth))
                        .font(.title2)
                        .fontWeight(.bold)
                    
                    Button(action: {
                        withAnimation {
                            currentMonth = calendar.date(byAdding: .month, value: 1, to: currentMonth)!
                        }
                    }) {
                        Image(systemName: "chevron.right")
                            .foregroundColor(.red)
                            .fontWeight(.medium)
                    }
                }
                
                Spacer()
                
                HStack(spacing: 16) {
                    Button("今天") {
                        withAnimation {
                            currentMonth = Date()
                            selectedDate = Date()
                        }
                    }
                    .foregroundColor(.red)
                    .fontWeight(.medium)
                    
                    Button("完成") { isPresented = false }
                        .foregroundColor(.red)
                        .fontWeight(.medium)
                }
            }
            .padding()
            
            // Weekdays
            HStack {
                ForEach(daysOfWeek, id: \.self) { day in
                    Text(day)
                        .font(.caption)
                        .fontWeight(.bold)
                        .foregroundColor(.gray)
                        .frame(maxWidth: .infinity)
                }
            }
            .padding(.horizontal)
            
            // Calendar Grid
            let days = daysInMonth(for: currentMonth)
            let firstDay = firstWeekdayOfMonth(for: currentMonth)
            
            LazyVGrid(columns: Array(repeating: SwiftUI.GridItem(.flexible()), count: 7)) {
                ForEach(0..<firstDay, id: \.self) { _ in
                    Text("")
                }
                
                ForEach(1...days, id: \.self) { day in
                    let date = dateFromDay(day, in: currentMonth)
                    let isSelected = calendar.isDate(date, inSameDayAs: selectedDate)
                    let isToday = calendar.isDateInToday(date)
                    let hasEvent = eventDates.contains(calendar.startOfDay(for: date))
                    
                    VStack(spacing: 2) {
                        ZStack {
                            if isSelected {
                                Circle().fill(Color.red).frame(width: 35, height: 35)
                            }
                            
                            Text("\(day)")
                                .font(.callout)
                                .fontWeight(isSelected || isToday ? .bold : .regular)
                                .foregroundColor(isSelected ? .white : (isToday ? .red : .primary))
                        }
                        
                        // 事件指示器小圆点
                        Circle()
                            .fill(hasEvent && !isSelected ? Color.red : Color.clear)
                            .frame(width: 4, height: 4)
                    }
                    .frame(height: 45)
                    .onTapGesture { selectedDate = date }
                }
            }
            .padding(.horizontal)
            
            Divider().padding(.vertical, 10)
            
            // 事件列表
            ScrollView {
                VStack(alignment: .leading, spacing: 12) {
                    if selectedEvents.isEmpty {
                        VStack(spacing: 8) {
                            Text("📅").font(.system(size: 40))
                            Text("没有日程")
                                .foregroundColor(.gray)
                                .font(.callout)
                        }
                        .frame(maxWidth: .infinity)
                        .padding(.top, 40)
                    } else {
                        let allDayEvents = selectedEvents.filter { $0.time == "全天" }
                        let timedEvents = selectedEvents.filter { $0.time != "全天" }.sorted { $0.time < $1.time }
                        
                        if !allDayEvents.isEmpty {
                            Text("全天").font(.subheadline).bold()
                            
                            ForEach(allDayEvents) { event in
                                EventRowView(event: event, showTime: false)
                            }
                            
                            Divider().padding(.vertical, 4)
                        }
                        
                        if !timedEvents.isEmpty {
                            Text("日程").font(.subheadline).bold()
                            
                            ForEach(timedEvents) { event in
                                EventRowView(event: event, showTime: true)
                            }
                        }
                    }
                }
                .padding(.horizontal)
            }
            
            Spacer()
        }
        .background(Color(.systemBackground))
    }
    
    private func monthYearString(from date: Date) -> String {
        let formatter = DateFormatter()
        formatter.dateFormat = "MMMM yyyy"
        formatter.locale = Locale(identifier: "zh_CN")
        return formatter.string(from: date)
    }
    
    private func daysInMonth(for date: Date) -> Int {
        return calendar.range(of: .day, in: .month, for: date)?.count ?? 0
    }
    
    private func firstWeekdayOfMonth(for date: Date) -> Int {
        let components = calendar.dateComponents([.year, .month], from: date)
        let firstDate = calendar.date(from: components)!
        return calendar.component(.weekday, from: firstDate) - 1
    }
    
    private func dateFromDay(_ day: Int, in date: Date) -> Date {
        var components = calendar.dateComponents([.year, .month], from: date)
        components.day = day
        return calendar.date(from: components)!
    }
}

struct EventRowView: View {
    let event: CalendarEvent
    let showTime: Bool
    
    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            RoundedRectangle(cornerRadius: 2)
                .fill(event.color)
                .frame(width: 4, height: showTime ? 50 : 40)
            
            VStack(alignment: .leading, spacing: 2) {
                Text(event.title)
                    .font(.callout)
                    .fontWeight(.medium)
                
                if showTime {
                    Text(event.time)
                        .font(.caption)
                        .foregroundColor(.gray)
                }
                
                if !event.location.isEmpty {
                    Text("📍 \(event.location)")
                        .font(.caption)
                        .foregroundColor(.gray)
                }
            }
            
            Spacer()
        }
        .padding(12)
        .background(event.color.opacity(0.08))
        .cornerRadius(8)
    }
}
