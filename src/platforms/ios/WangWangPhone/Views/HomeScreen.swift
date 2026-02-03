import SwiftUI

struct AppIconData: Identifiable {
    let id = UUID()
    let name: String
    let icon: String
    let colors: [Color]
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

    var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()
            
            VStack {
                // 虚拟状态栏
                HStack {
                    Text("09:41")
                        .fontWeight(.bold)
                    Spacer()
                    HStack(spacing: 5) {
                        Text("📶")
                        Text("5G")
                        Text("🔋")
                    }
                }
                .foregroundColor(.white)
                .padding(.horizontal, 20)
                .padding(.top, 10)

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
                    RoundedRectangle(cornerRadius: 30)
                        .fill(Color.white.opacity(0.3))
                        .background(.ultraThinMaterial)
                        .clipShape(RoundedRectangle(cornerRadius: 30))
                        .frame(height: 90)
                        .padding(.horizontal, 15)
                    
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
}