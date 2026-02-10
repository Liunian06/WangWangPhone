import SwiftUI

struct CalendarAppView: View {
    @Binding var isPresented: Bool
    @State private var selectedDate = Date()
    @State private var currentMonth = Date()
    
    let calendar = Calendar.current
    let daysOfWeek = ["日", "一", "二", "三", "四", "五", "六"]
    
    var body: some View {
        VStack(spacing: 0) {
            // Header
            HStack {
                VStack(alignment: .leading) {
                    Text(monthYearString(from: currentMonth))
                        .font(.title)
                        .fontWeight(.bold)
                }
                Spacer()
                Button("完成") { isPresented = false }
                    .foregroundColor(.red)
                    .fontWeight(.medium)
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
            
            LazyVGrid(columns: Array(repeating: GridItem(.flexible()), count: 7)) {
                ForEach(0..<firstDay, id: \.self) { _ in
                    Text("")
                }
                
                ForEach(1...days, id: \.self) { day in
                    let date = dateFromDay(day, in: currentMonth)
                    let isSelected = calendar.isDate(date, inSameDayAs: selectedDate)
                    let isToday = calendar.isDateInToday(date)
                    
                    ZStack {
                        if isSelected {
                            Circle().fill(Color.red).frame(width: 35, height: 35)
                        }
                        
                        Text("\(day)")
                            .font(.title3)
                            .fontWeight(isSelected || isToday ? .bold : .normal)
                            .foregroundColor(isSelected ? .white : (isToday ? .red : .primary))
                    }
                    .frame(height: 45)
                    .onTapGesture { selectedDate = date }
                }
            }
            .padding()
            
            Divider().padding(.vertical)
            
            // Schedule
            VStack(alignment: .leading, spacing: 15) {
                Text("全天").font(.headline).bold()
                
                HStack(spacing: 15) {
                    Rectangle().fill(Color.red).frame(width: 4, height: 40).cornerRadius(2)
                    VStack(alignment: .leading) {
                        Text("开发任务").font(.body).bold()
                        Text("广州").font(.subheadline).foregroundColor(.gray)
                    }
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.horizontal)
            
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
