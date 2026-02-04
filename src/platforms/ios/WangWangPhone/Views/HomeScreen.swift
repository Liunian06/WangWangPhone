import SwiftUI

struct AppIconData: Identifiable {
    let id = UUID()
    let name: String
    let icon: String
    let colors: [Color]
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

struct HomeScreen: View {
    let apps = [
        AppIconData(name: "电话", icon: "📞", colors: [.pink, .orange]),
        AppIconData(name: "信息", icon: "💬", colors: [.blue, .cyan]),
        AppIconData(name: "Safari", icon: "🧭", colors: [.green, .blue]),
        AppIconData(name: "音乐", icon: "🎵", colors: [.yellow, .red]),
        AppIconData(name: "相机", icon: "📷", colors: [.white, .gray]),
        AppIconData(name: "日历", icon: "📅", colors: [.white, .gray]),
        AppIconData(name: "设置", icon: "⚙️", colors: [.white, .gray]),
        AppIconData(name: "汪汪", icon: "🐶", colors: [.white, .gray])
    ]

    let columns = [
        GridItem(.flexible()),
        GridItem(.flexible()),
        GridItem(.flexible()),
        GridItem(.flexible())
    ]

    @State private var city: String = "..."
    @State private var weather: WeatherInfo? = nil

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
                }

                // 应用网格
                LazyVGrid(columns: columns, spacing: 25) {
                    ForEach(apps) { app in
                        VStack(spacing: 8) {
                            ZStack {
                                RoundedRectangle(cornerRadius: 14)
                                    .fill(LinearGradient(colors: app.colors, startPoint: .topLeading, endPoint: .bottomTrailing))
                                    .frame(width: 60, height: 60)
                                Text(app.icon)
                                    .font(.system(size: 30))
                            }
                            Text(app.name)
                                .font(.caption)
                                .foregroundColor(.white)
                        }
                    }
                }
                .padding(20)

                Spacer()

                // Dock 栏
                ZStack {
                    // 磨砂玻璃背景层
                    RoundedRectangle(cornerRadius: 30)
                        .fill(Color.white.opacity(0.3))
                        .background(.ultraThinMaterial)
                        .clipShape(RoundedRectangle(cornerRadius: 30))
                        .frame(height: 90)
                        .padding(.horizontal, 15)
                    
                    // 应用图标层 (确保在蒙版之上)
                    HStack(spacing: 25) {
                        ForEach(apps.prefix(4)) { app in
                            ZStack {
                                RoundedRectangle(cornerRadius: 14)
                                    .fill(LinearGradient(colors: app.colors, startPoint: .topLeading, endPoint: .bottomTrailing))
                                    .frame(width: 55, height: 55)
                                Text(app.icon)
                                    .font(.system(size: 28))
                            }
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
        }
    }
    
    func loadData() {
        // Mock Async Data Loading
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
}