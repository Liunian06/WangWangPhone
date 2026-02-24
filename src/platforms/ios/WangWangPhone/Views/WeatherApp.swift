import SwiftUI
import Foundation

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

private struct WeatherAppRealtimeData {
    let city: String
    let temp: String
    let description: String
    let icon: String
    let range: String
    let summary: String
    let hourly: [HourlyForecastItem]
    let daily: [(String, String, Int, Int)]
    let details: [WeatherDetailItem]
}

struct WeatherAppView: View {
    @Binding var isPresented: Bool

    @State private var city: String = "定位中..."
    @State private var currentTemp: String = "--"
    @State private var condition: String = "加载中..."
    @State private var rangeText: String = "最高 -- 最低 -- | 风速 --"
    @State private var summaryText: String = "正在获取天气数据..."

    @State private var hourlyForecast: [HourlyForecastItem] = Self.defaultHourlyForecast()
    @State private var forecast: [(String, String, Int, Int)] = Self.defaultDailyForecast()
    @State private var weatherDetails: [WeatherDetailItem] = Self.defaultWeatherDetails()
    @State private var isRefreshing: Bool = false

    var body: some View {
        ZStack {
            LinearGradient(colors: [Color(red: 0.31, green: 0.67, blue: 0.99), Color(red: 0.0, green: 0.95, blue: 0.99)], startPoint: .top, endPoint: .bottom)
                .ignoresSafeArea()

            ScrollView(showsIndicators: false) {
                VStack(spacing: 0) {
                    // 关闭按钮
                    HStack {
                        Spacer()
                        Button(isRefreshing ? "更新中..." : "强制更新") {
                            loadWeatherData(forceRefresh: true)
                        }
                        .disabled(isRefreshing)
                        .foregroundColor(.white)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 6)
                        .background(Color.white.opacity(isRefreshing ? 0.12 : 0.2))
                        .cornerRadius(10)

                        Spacer().frame(width: 8)

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
                        Text(city).font(.system(size: 34))
                        Text(currentTemp).font(.system(size: 96, weight: .thin))
                        Text(condition).font(.title3).fontWeight(.medium)
                        Text(rangeText).font(.title3).fontWeight(.medium)
                    }
                    .foregroundColor(.white)
                    .padding(.top, 30)

                    Spacer().frame(height: 30)

                    // 小时预报
                    VStack(alignment: .leading, spacing: 10) {
                        Text(summaryText)
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

                    // 未来预报
                    VStack(alignment: .leading, spacing: 15) {
                        Text("📅  未来天气预报")
                            .font(.caption)
                            .fontWeight(.bold)
                            .foregroundColor(.white.opacity(0.6))
                            .padding(.horizontal)

                        Divider().background(Color.white.opacity(0.2)).padding(.horizontal)

                        VStack(spacing: 12) {
                            ForEach(Array(forecast.enumerated()), id: \.offset) { index, item in
                                let day = item.0
                                let icon = item.1
                                let low = item.2
                                let high = item.3
                                HStack {
                                    Text(day)
                                        .font(.callout)
                                        .fontWeight(.medium)
                                        .frame(width: 70, alignment: .leading)

                                    Text(icon).font(.title3).frame(width: 35)

                                    Spacer()

                                    HStack(spacing: 8) {
                                        Text("\(low)°").foregroundColor(.white.opacity(0.6))

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

                                if index < forecast.count - 1 {
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
        .onAppear {
            loadWeatherData()
        }
    }

    private static func defaultHourlyForecast(temp: String = "--") -> [HourlyForecastItem] {
        [
            HourlyForecastItem(time: "现在", icon: "🌤️", temp: temp),
            HourlyForecastItem(time: "03时", icon: "🌙", temp: "--"),
            HourlyForecastItem(time: "06时", icon: "⛅", temp: "--"),
            HourlyForecastItem(time: "09时", icon: "⛅", temp: "--"),
            HourlyForecastItem(time: "12时", icon: "☀️", temp: "--"),
            HourlyForecastItem(time: "15时", icon: "🌤️", temp: "--"),
            HourlyForecastItem(time: "18时", icon: "⛅", temp: "--"),
            HourlyForecastItem(time: "21时", icon: "🌙", temp: "--")
        ]
    }

    private static func defaultDailyForecast() -> [(String, String, Int, Int)] {
        [
            ("今天", "🌤️", 20, 30),
            ("明天", "🌤️", 20, 30),
            ("周三", "🌤️", 20, 30)
        ]
    }

    private static func defaultWeatherDetails(temp: String = "--") -> [WeatherDetailItem] {
        [
            WeatherDetailItem(title: "体感温度", value: temp, subtitle: "人体感知温度"),
            WeatherDetailItem(title: "湿度", value: "--", subtitle: "空气湿度"),
            WeatherDetailItem(title: "能见度", value: "--", subtitle: "当前视线范围"),
            WeatherDetailItem(title: "紫外线指数", value: "--", subtitle: "紫外线强度"),
            WeatherDetailItem(title: "风速", value: "--", subtitle: "实时风向风速"),
            WeatherDetailItem(title: "气压", value: "--", subtitle: "大气压强")
        ]
    }

    private func containsChinese(_ text: String) -> Bool {
        text.unicodeScalars.contains { $0.value >= 0x4E00 && $0.value <= 0x9FFF }
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
        let cleaned = raw.trimmingCharacters(in: .whitespacesAndNewlines)
        if cleaned.isEmpty || cleaned == "--" { return "--" }
        if let range = cleaned.range(of: #"[-+]?\d+"#, options: .regularExpression) {
            let number = cleaned[range].replacingOccurrences(of: "+", with: "")
            return "\(number)°"
        }
        return cleaned.replacingOccurrences(of: " ", with: "")
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

    private func hourLabel(_ rawTime: String) -> String {
        let trimmed = rawTime.trimmingCharacters(in: .whitespacesAndNewlines)
        guard let value = Int(trimmed) else { return "--" }
        let hour = max(0, min(23, value / 100))
        return String(format: "%02d时", hour)
    }

    private func dayLabel(dateText: String, index: Int) -> String {
        if index == 0 { return "今天" }
        if index == 1 { return "明天" }
        guard let date = DateFormatter.shortDate.date(from: dateText) else {
            return "第\(index + 1)天"
        }
        let weekday = Calendar.current.component(.weekday, from: date)
        switch weekday {
        case 1: return "周日"
        case 2: return "周一"
        case 3: return "周二"
        case 4: return "周三"
        case 5: return "周四"
        case 6: return "周五"
        case 7: return "周六"
        default: return "第\(index + 1)天"
        }
    }

    private func temperatureNumber(from raw: String) -> Int? {
        guard let match = raw.range(of: #"[-+]?\d+"#, options: .regularExpression) else {
            return nil
        }
        return Int(raw[match])
    }

    private func temperatureInt(from raw: String, fallback: Int) -> Int {
        return temperatureNumber(from: raw) ?? fallback
    }

    private func dailyRangeFromHourly(_ hours: [[String: Any]]) -> (Int, Int)? {
        guard !hours.isEmpty else { return nil }
        var minTemp: Int?
        var maxTemp: Int?

        for item in hours {
            let rawTemp = (item["tempC"] as? String) ?? "--"
            guard let value = temperatureNumber(from: rawTemp) else { continue }
            minTemp = minTemp.map { min($0, value) } ?? value
            maxTemp = maxTemp.map { max($0, value) } ?? value
        }

        guard let minTemp, let maxTemp else { return nil }
        return (minTemp, maxTemp)
    }

    private func isUnknownWeather(temp: String, description: String) -> Bool {
        let normalizedDesc = description.trimmingCharacters(in: .whitespacesAndNewlines)
        if normalizedDesc.isEmpty { return true }
        let lowerDesc = normalizedDesc.lowercased()
        if normalizedDesc.contains("未知") || lowerDesc == "unknown" { return true }

        let normalizedTemp = temp.trimmingCharacters(in: .whitespacesAndNewlines)
        return normalizedTemp == "--" && (normalizedDesc == "--" || lowerDesc == "n/a")
    }

    private func parseCityFromIpResponse(_ responseText: String) -> String? {
        let ignoreKeywords: Set<String> = ["中国", "电信", "移动", "联通", "铁通", "教育网", "宽带", "公司", "网络"]
        let source: String
        if let range = responseText.range(of: "来自于：") {
            source = String(responseText[range.upperBound...])
        } else if let range = responseText.range(of: "来自于") {
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
            print("WeatherApp location fetch failed: \(error.localizedDescription)")
            return nil
        }
    }

    private func resolveCurrentCity(weatherCache: WeatherCacheManager, forceRefresh: Bool = false) async -> String {
        if let manualCity = weatherCache.getManualLocation(), !manualCity.isEmpty {
            return manualCity
        }
        if !forceRefresh, let cachedCity = weatherCache.getCachedLocation(), !cachedCity.isEmpty {
            return cachedCity
        }
        if let cityFromIp = await fetchCityFromIp(), !cityFromIp.isEmpty {
            _ = weatherCache.saveLocationCache(city: cityFromIp)
            return cityFromIp
        }
        if forceRefresh, let cachedCity = weatherCache.getCachedLocation(), !cachedCity.isEmpty {
            return cachedCity
        }
        return "北京"
    }

    private func parseRealtimeWeather(city: String, jsonObject: [String: Any]) -> WeatherAppRealtimeData? {
        let current = (jsonObject["current_condition"] as? [[String: Any]])?.first
        let weatherDays = (jsonObject["weather"] as? [[String: Any]]) ?? []
        let today = weatherDays.first

        if current == nil && weatherDays.isEmpty {
            return nil
        }

        let zhDesc = ((current?["lang_zh"] as? [[String: Any]])?.first?["value"] as? String)?
            .trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        let enDesc = ((current?["weatherDesc"] as? [[String: Any]])?.first?["value"] as? String)?
            .trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        let description = localizedWeatherDescription(zhDesc.isEmpty ? (enDesc.isEmpty ? "天气未知" : enDesc) : zhDesc)

        let temp = normalizeTemperature((current?["temp_C"] as? String) ?? "--")
        let feelsLike = normalizeTemperature((current?["FeelsLikeC"] as? String) ?? "--")
        let windKmph = (current?["windspeedKmph"] as? String) ?? "--"
        let maxTemp = (today?["maxtempC"] as? String) ?? ""
        let minTemp = (today?["mintempC"] as? String) ?? ""
        let icon = weatherIcon(for: description)
        let range = buildRangeText(maxTemp: maxTemp, minTemp: minTemp, windKmph: windKmph)
        let summary = "当前\(description)，气温\(temp)，体感\(feelsLike)"

        var hourlyItems: [HourlyForecastItem] = [
            HourlyForecastItem(time: "现在", icon: icon, temp: temp)
        ]
        if let hours = today?["hourly"] as? [[String: Any]] {
            for item in hours.prefix(9) {
                let hourTemp = normalizeTemperature((item["tempC"] as? String) ?? "--")
                let time = hourLabel((item["time"] as? String) ?? "")
                let hourZh = ((item["lang_zh"] as? [[String: Any]])?.first?["value"] as? String)?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
                let hourEn = ((item["weatherDesc"] as? [[String: Any]])?.first?["value"] as? String)?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
                let hourDesc = localizedWeatherDescription(hourZh.isEmpty ? (hourEn.isEmpty ? "天气未知" : hourEn) : hourZh)
                hourlyItems.append(HourlyForecastItem(time: time, icon: weatherIcon(for: hourDesc), temp: hourTemp))
            }
        }

        var dailyItems: [(String, String, Int, Int)] = []
        for (index, dayItem) in weatherDays.prefix(10).enumerated() {
            let dateText = (dayItem["date"] as? String) ?? ""
            let hourly = (dayItem["hourly"] as? [[String: Any]]) ?? []

            var low = temperatureInt(from: (dayItem["mintempC"] as? String) ?? "--", fallback: 20)
            var high = temperatureInt(from: (dayItem["maxtempC"] as? String) ?? "--", fallback: 30)
            if ((dayItem["mintempC"] as? String) ?? "--") == "--" || ((dayItem["maxtempC"] as? String) ?? "--") == "--" {
                if let hourlyRange = dailyRangeFromHourly(hourly) {
                    low = hourlyRange.0
                    high = hourlyRange.1
                }
            }

            let iconSource = hourly.indices.contains(4) ? hourly[4] : hourly.first
            let dayZh = ((iconSource?["lang_zh"] as? [[String: Any]])?.first?["value"] as? String)?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
            let dayEn = ((iconSource?["weatherDesc"] as? [[String: Any]])?.first?["value"] as? String)?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
            let dayDesc = localizedWeatherDescription(dayZh.isEmpty ? (dayEn.isEmpty ? description : dayEn) : dayZh)

            dailyItems.append((dayLabel(dateText: dateText, index: index), weatherIcon(for: dayDesc), low, high))
        }

        if dailyItems.isEmpty {
            let fallbackLow = temperatureInt(from: minTemp, fallback: temperatureInt(from: temp, fallback: 20))
            let fallbackHigh = temperatureInt(from: maxTemp, fallback: temperatureInt(from: temp, fallback: 30))
            dailyItems = [
                ("今天", icon, fallbackLow, fallbackHigh),
                ("明天", icon, fallbackLow, fallbackHigh),
                ("后天", icon, fallbackLow, fallbackHigh)
            ]
        }

        let humidityRaw = ((current?["humidity"] as? String) ?? "--").trimmingCharacters(in: .whitespacesAndNewlines)
        let visibilityRaw = ((current?["visibility"] as? String) ?? "--").trimmingCharacters(in: .whitespacesAndNewlines)
        let uvRaw = ((current?["uvIndex"] as? String) ?? "--").trimmingCharacters(in: .whitespacesAndNewlines)
        let pressureRaw = ((current?["pressure"] as? String) ?? "--").trimmingCharacters(in: .whitespacesAndNewlines)
        let windDirRaw = ((current?["winddir16Point"] as? String) ?? "").trimmingCharacters(in: .whitespacesAndNewlines)

        let humidity = (humidityRaw.isEmpty || humidityRaw == "--") ? "--" : "\(humidityRaw)%"
        let visibility = (visibilityRaw.isEmpty || visibilityRaw == "--") ? "--" : "\(visibilityRaw) 公里"
        let uvValue = uvRaw.isEmpty ? "--" : uvRaw
        let pressure = (pressureRaw.isEmpty || pressureRaw == "--") ? "--" : "\(pressureRaw) hPa"
        let wind = formatWindKmph(windKmph)
        let windValue = wind == "--" ? "--" : (windDirRaw.isEmpty ? wind : "\(windDirRaw)风 \(wind)")

        let details = [
            WeatherDetailItem(title: "体感温度", value: feelsLike, subtitle: "人体感知温度"),
            WeatherDetailItem(title: "湿度", value: humidity, subtitle: "空气湿度"),
            WeatherDetailItem(title: "能见度", value: visibility, subtitle: "当前视线范围"),
            WeatherDetailItem(title: "紫外线指数", value: uvValue, subtitle: "紫外线强度"),
            WeatherDetailItem(title: "风速", value: windValue, subtitle: "实时风向风速"),
            WeatherDetailItem(title: "气压", value: pressure, subtitle: "大气压强")
        ]

        return WeatherAppRealtimeData(
            city: city,
            temp: temp,
            description: description,
            icon: icon,
            range: range,
            summary: summary,
            hourly: hourlyItems,
            daily: dailyItems,
            details: details
        )
    }

    private func fetchRealtimeWeather(city: String) async -> WeatherAppRealtimeData? {
        let cityPinyin = cityToPinyin(city)
        guard let encodedCity = cityPinyin.addingPercentEncoding(withAllowedCharacters: .urlPathAllowed),
              let url = URL(string: "https://wttr.in/\(encodedCity)?format=j1") else {
            return nil
        }

        var request = URLRequest(url: url)
        request.httpMethod = "GET"
        request.setValue("WangWangPhone/1.0", forHTTPHeaderField: "User-Agent")

        for attempt in 0..<2 {
            do {
                let (data, response) = try await URLSession.shared.data(for: request)
                guard let httpResponse = response as? HTTPURLResponse, (200...299).contains(httpResponse.statusCode) else {
                    continue
                }

                let payload = String(data: data, encoding: .utf8) ?? String(decoding: data, as: UTF8.self)
                WeatherRealtimeMemoryCache.save(city: city, payload: payload)

                guard let jsonObject = try JSONSerialization.jsonObject(with: data) as? [String: Any] else {
                    continue
                }
                if let parsed = parseRealtimeWeather(city: city, jsonObject: jsonObject) {
                    return parsed
                }
            } catch {
                print("WeatherApp fetch attempt \(attempt + 1) failed: \(error.localizedDescription)")
            }

            if attempt == 0 {
                try? await Task.sleep(nanoseconds: 300_000_000)
            }
        }

        if let memoryPayload = WeatherRealtimeMemoryCache.load(city: city),
           let memoryData = memoryPayload.data(using: .utf8),
           let anyObject = try? JSONSerialization.jsonObject(with: memoryData),
           let jsonObject = anyObject as? [String: Any] {
            return parseRealtimeWeather(city: city, jsonObject: jsonObject)
        }

        return nil
    }

    private func loadWeatherData(forceRefresh: Bool = false) {
        if isRefreshing { return }
        isRefreshing = true

        if forceRefresh {
            condition = "更新中..."
            summaryText = "正在重新获取定位和天气..."
        }

        Task {
            let weatherCache = WeatherCacheManager.shared
            let currentCity = await resolveCurrentCity(weatherCache: weatherCache, forceRefresh: forceRefresh)
            await MainActor.run {
                self.city = currentCity
            }

            if !forceRefresh,
               let cached = weatherCache.getTodayWeatherCache(city: currentCity),
               !isUnknownWeather(temp: cached.temp, description: cached.description) {
                let cachedDescription = localizedWeatherDescription(cached.description)
                await MainActor.run {
                    self.currentTemp = cached.temp
                    self.condition = cachedDescription
                    self.rangeText = cached.range
                    self.summaryText = "当前\(cachedDescription)，气温\(cached.temp)"
                }
            }

            if let realtime = await fetchRealtimeWeather(city: currentCity) {
                await MainActor.run {
                    self.city = realtime.city
                    self.currentTemp = realtime.temp
                    self.condition = realtime.description
                    self.rangeText = realtime.range
                    self.summaryText = realtime.summary
                    self.hourlyForecast = realtime.hourly.isEmpty ? Self.defaultHourlyForecast(temp: realtime.temp) : realtime.hourly
                    self.forecast = realtime.daily.isEmpty ? Self.defaultDailyForecast() : realtime.daily
                    self.weatherDetails = realtime.details.isEmpty ? Self.defaultWeatherDetails(temp: realtime.temp) : realtime.details
                }

                if !isUnknownWeather(temp: realtime.temp, description: realtime.description) {
                    let record = WeatherCacheRecord(
                        city: currentCity,
                        temp: realtime.temp,
                        description: realtime.description,
                        icon: realtime.icon,
                        range: realtime.range,
                        requestDate: WeatherCacheManager.getTodayDateString(),
                        updatedAt: 0
                    )
                    _ = weatherCache.saveWeatherCache(record)
                }
                _ = weatherCache.clearExpiredCache()
            } else {
                await MainActor.run {
                    if self.condition == "加载中..." || self.condition == "更新中..." {
                        self.condition = "天气未知"
                        self.summaryText = "天气数据暂不可用"
                    }
                }
            }

            await MainActor.run {
                self.isRefreshing = false
            }
        }
    }
}

private extension DateFormatter {
    static let shortDate: DateFormatter = {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd"
        formatter.locale = Locale(identifier: "zh_CN")
        return formatter
    }()
}
