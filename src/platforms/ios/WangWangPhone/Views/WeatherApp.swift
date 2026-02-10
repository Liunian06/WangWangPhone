import SwiftUI

struct HourlyForecastItem {
    let time: String
    let icon: String
    let temp: String
}

struct WeatherDetailItem {
    let title: String
    let value: String
    let subtitle: String
}

struct WeatherAppView: View {
    @Binding var isPresented: Bool
    
    let hourlyForecast = [
        HourlyForecastItem(time: "现在", icon: "⛅", temp: "25°"),
        HourlyForecastItem(time: "14时", icon: "🌤️", temp: "27°"),
        HourlyForecastItem(time: "15时", icon: "☀️", temp: "28°"),
        HourlyForecastItem(time: "16时", icon: "☀️", temp: "29°"),
        HourlyForecastItem(time: "17时", icon: "⛅", temp: "28°"),
        HourlyForecastItem(time: "18时", icon: "🌥️", temp: "26°"),
        HourlyForecastItem(time: "19时", icon: "🌙", temp: "24°"),
        HourlyForecastItem(time: "20时", icon: "🌙", temp: "23°"),
        HourlyForecastItem(time: "21时", icon: "🌙", temp: "22°"),
        HourlyForecastItem(time: "22时", icon: "🌙", temp: "21°")
    ]
    
    let forecast = [
        ("今天", "⛅", 21, 29),
        ("明天", "🌧️", 20, 26),
        ("周四", "☀️", 22, 30),
        ("周五", "☀️", 23, 31),
        ("周六", "⛅", 22, 29),
        ("周日", "🌧️", 21, 27),
        ("下周一", "⛈️", 20, 25),
        ("下周二", "🌤️", 22, 28),
        ("下周三", "☀️", 23, 30),
        ("下周四", "⛅", 22, 29)
    ]
    
    let weatherDetails = [
        WeatherDetailItem(title: "体感温度", value: "27°", subtitle: "湿度使体感温度更高"),
        WeatherDetailItem(title: "湿度", value: "68%", subtitle: "露点温度 18°"),
        WeatherDetailItem(title: "能见度", value: "16 公里", subtitle: "能见度良好"),
        WeatherDetailItem(title: "紫外线指数", value: "5", subtitle: "中等"),
        WeatherDetailItem(title: "风速", value: "东南 12", subtitle: "阵风 20 km/h"),
        WeatherDetailItem(title: "气压", value: "1013 hPa", subtitle: "正常")
    ]
    
    var body: some View {
        ZStack {
            LinearGradient(colors: [Color(red: 0.31, green: 0.67, blue: 0.99), Color(red: 0.0, green: 0.95, blue: 0.99)], startPoint: .top, endPoint: .bottom)
                .ignoresSafeArea()
            
            ScrollView(showsIndicators: false) {
                VStack(spacing: 0) {
                    // 关闭按钮
                    HStack {
                        Spacer()
                        Button("完成") { isPresented = false }
                            .foregroundColor(.white)
                            .padding(.horizontal, 12)
                            .padding(.vertical, 6)
                            .background(Color.white.opacity(0.2))
                            .cornerRadius(10)
                    }
                    .padding(.horizontal)
                    .padding(.top, 8)
                    
                    // 城市和温度
                    VStack(spacing: 8) {
                        Text("广州").font(.system(size: 34))
                        Text("25°").font(.system(size: 96, weight: .thin))
                        Text("多云").font(.title3).fontWeight(.medium)
                        Text("最高 29° 最低 21°").font(.title3).fontWeight(.medium)
                    }
                    .foregroundColor(.white)
                    .padding(.top, 30)
                    
                    Spacer().frame(height: 30)
                    
                    // 小时预报
                    VStack(alignment: .leading, spacing: 10) {
                        Text("今天下午将会以多云为主，当前气温 25°。")
                            .font(.caption)
                            .foregroundColor(.white.opacity(0.8))
                            .padding(.horizontal)
                        
                        Divider().background(Color.white.opacity(0.2)).padding(.horizontal)
                        
                        ScrollView(.horizontal, showsIndicators: false) {
                            HStack(spacing: 20) {
                                ForEach(hourlyForecast.indices, id: \.self) { index in
                                    let item = hourlyForecast[index]
                                    VStack(spacing: 6) {
                                        Text(item.time)
                                            .font(.caption)
                                            .fontWeight(.medium)
                                        Text(item.icon)
                                            .font(.title2)
                                        Text(item.temp)
                                            .font(.callout)
                                            .fontWeight(.medium)
                                    }
                                    .foregroundColor(.white)
                                    .frame(width: 50)
                                }
                            }
                            .padding(.horizontal)
                        }
                    }
                    .padding(.vertical, 15)
                    .background(Color.black.opacity(0.1))
                    .cornerRadius(15)
                    .padding(.horizontal)
                    
                    Spacer().frame(height: 12)
                    
                    // 10天预报
                    VStack(alignment: .leading, spacing: 15) {
                        Text("📅  10 日天气预报")
                            .font(.caption)
                            .fontWeight(.bold)
                            .foregroundColor(.white.opacity(0.6))
                            .padding(.horizontal)
                        
                        Divider().background(Color.white.opacity(0.2)).padding(.horizontal)
                        
                        VStack(spacing: 12) {
                            ForEach(forecast, id: \.0) { day, icon, low, high in
                                HStack {
                                    Text(day)
                                        .font(.callout)
                                        .fontWeight(.medium)
                                        .frame(width: 70, alignment: .leading)
                                    
                                    Text(icon).font(.title3).frame(width: 35)
                                    
                                    Spacer()
                                    
                                    HStack(spacing: 8) {
                                        Text("\(low)°").foregroundColor(.white.opacity(0.6))
                                        
                                        // Temp Bar
                                        ZStack(alignment: .leading) {
                                            Capsule().fill(Color.white.opacity(0.2)).frame(width: 80, height: 4)
                                            let startFrac = CGFloat(max(0, min(1, Float(low - 18) / 15.0)))
                                            let endFrac = CGFloat(max(0, min(1, Float(high - 18) / 15.0)))
                                            Capsule()
                                                .fill(LinearGradient(
                                                    colors: [.blue, .yellow, .orange],
                                                    startPoint: .leading,
                                                    endPoint: .trailing
                                                ))
                                                .frame(width: 80 * (endFrac - startFrac), height: 4)
                                                .offset(x: 80 * startFrac)
                                        }
                                        
                                        Text("\(high)°")
                                    }
                                    .font(.callout)
                                    .fontWeight(.medium)
                                }
                                .foregroundColor(.white)
                                .padding(.horizontal)
                                
                                if day != forecast.last?.0 {
                                    Divider().background(Color.white.opacity(0.1)).padding(.horizontal)
                                }
                            }
                        }
                    }
                    .padding(.vertical, 15)
                    .background(Color.black.opacity(0.1))
                    .cornerRadius(15)
                    .padding(.horizontal)
                    
                    Spacer().frame(height: 12)
                    
                    // 天气详情网格
                    LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 12) {
                        ForEach(weatherDetails.indices, id: \.self) { index in
                            let detail = weatherDetails[index]
                            VStack(alignment: .leading, spacing: 8) {
                                Text(detail.title)
                                    .font(.caption)
                                    .fontWeight(.bold)
                                    .foregroundColor(.white.opacity(0.6))
                                Text(detail.value)
                                    .font(.title)
                                    .fontWeight(.medium)
                                    .foregroundColor(.white)
                                Text(detail.subtitle)
                                    .font(.caption)
                                    .foregroundColor(.white.opacity(0.6))
                            }
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .padding(15)
                            .background(Color.black.opacity(0.1))
                            .cornerRadius(15)
                        }
                    }
                    .padding(.horizontal, 20)
                    
                    Spacer().frame(height: 40)
                }
            }
        }
    }
}
