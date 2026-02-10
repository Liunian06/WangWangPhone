import SwiftUI

struct WeatherAppView: View {
    @Binding var isPresented: Bool
    
    let forecast = [
        ("今天", "⛅", 21, 29),
        ("明天", "🌧️", 20, 26),
        ("周四", "☀️", 22, 30),
        ("周五", "☀️", 23, 31),
        ("周六", "⛅", 22, 29),
        ("周日", "🌧️", 21, 27),
        ("下周一", "⛈️", 20, 25)
    ]
    
    var body: some View {
        ZStack {
            LinearGradient(colors: [Color(red: 0.31, green: 0.67, blue: 0.99), Color(red: 0.0, green: 0.95, blue: 0.99)], startPoint: .top, endPoint: .bottom)
                .ignoresSafeArea()
            
            VStack {
                HStack {
                    Spacer()
                    Button("完成") { isPresented = false }
                        .foregroundColor(.white)
                        .padding()
                        .background(Color.white.opacity(0.2))
                        .cornerRadius(10)
                }
                .padding(.horizontal)
                
                VStack(spacing: 8) {
                    Text("广州").font(.system(size: 34))
                    Text("25°").font(.system(size: 96, weight: .thin))
                    Text("多云").font(.title3).fontWeight(.medium)
                    Text("最高 29° 最低 21°").font(.title3).fontWeight(.medium)
                }
                .foregroundColor(.white)
                .padding(.top, 40)
                
                Spacer().frame(height: 50)
                
                // Forecast
                VStack(alignment: .leading, spacing: 15) {
                    Text("10 日天气预报")
                        .font(.caption)
                        .fontWeight(.bold)
                        .foregroundColor(.white.opacity(0.6))
                        .padding(.horizontal)
                    
                    Divider().background(Color.white.opacity(0.2)).padding(.horizontal)
                    
                    ScrollView {
                        VStack(spacing: 15) {
                            ForEach(forecast, id: \.0) { day, icon, low, high in
                                HStack {
                                    Text(day)
                                        .font(.title3)
                                        .fontWeight(.medium)
                                        .frame(width: 80, alignment: .leading)
                                    
                                    Text(icon).font(.title2).frame(width: 40)
                                    
                                    Spacer()
                                    
                                    HStack(spacing: 10) {
                                        Text("\(low)°").foregroundColor(.white.opacity(0.6))
                                        
                                        // Simple Temp Bar
                                        ZStack(alignment: .leading) {
                                            Capsule().fill(Color.white.opacity(0.2)).frame(width: 100, height: 4)
                                            Capsule().fill(Color.yellow).frame(width: 40, height: 4).offset(x: 30)
                                        }
                                        
                                        Text("\(high)°")
                                    }
                                    .font(.title3)
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
                }
                .padding(.vertical)
                .background(Color.black.opacity(0.1))
                .cornerRadius(15)
                .padding(.horizontal)
                
                Spacer()
            }
        }
    }
}
