import Foundation

enum WeatherRealtimeMemoryCache {
    private struct Snapshot {
        let cityKey: String
        let payload: String
        let savedAt: Date
    }

    private static let lock = NSLock()
    private static var snapshot: Snapshot?
    private static let defaultMaxAge: TimeInterval = 2 * 60 * 60

    static func save(city: String, payload: String) {
        let key = normalizedCity(city)
        guard !key.isEmpty else { return }
        guard !payload.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else { return }

        lock.lock()
        snapshot = Snapshot(cityKey: key, payload: payload, savedAt: Date())
        lock.unlock()
    }

    static func load(city: String, maxAge: TimeInterval = defaultMaxAge) -> String? {
        let key = normalizedCity(city)
        guard !key.isEmpty else { return nil }

        lock.lock()
        defer { lock.unlock() }

        guard let cached = snapshot else { return nil }
        guard cached.cityKey == key else { return nil }
        guard Date().timeIntervalSince(cached.savedAt) <= maxAge else { return nil }
        return cached.payload
    }

    private static func normalizedCity(_ city: String) -> String {
        city.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
    }
}
