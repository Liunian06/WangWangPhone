import UIKit

final class BackgroundRequestKeepAlive {
    static let shared = BackgroundRequestKeepAlive()

    private let lock = NSLock()
    private var activeRequestCount = 0
    private var backgroundTaskId: UIBackgroundTaskIdentifier = .invalid

    private init() {}

    func begin() {
        lock.lock()
        defer { lock.unlock() }

        activeRequestCount += 1
        if activeRequestCount == 1 {
            startBackgroundTaskLocked()
        }
    }

    func end() {
        lock.lock()
        defer { lock.unlock() }

        activeRequestCount = max(0, activeRequestCount - 1)
        if activeRequestCount == 0 {
            endBackgroundTaskLocked()
        }
    }

    private func startBackgroundTaskLocked() {
        guard backgroundTaskId == .invalid else { return }
        backgroundTaskId = UIApplication.shared.beginBackgroundTask(withName: "WangWangPhoneApiRequest") { [weak self] in
            self?.forceEndDueToExpiration()
        }
    }

    private func endBackgroundTaskLocked() {
        guard backgroundTaskId != .invalid else { return }
        UIApplication.shared.endBackgroundTask(backgroundTaskId)
        backgroundTaskId = .invalid
    }

    private func forceEndDueToExpiration() {
        lock.lock()
        defer { lock.unlock() }
        activeRequestCount = 0
        endBackgroundTaskLocked()
    }
}
