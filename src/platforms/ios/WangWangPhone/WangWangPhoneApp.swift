import SwiftUI

@main
struct WangWangPhoneApp: App {
    
    init() {
        // 初始化授权管理器，从数据库恢复激活状态
        _ = LicenseManager.shared.initialize()
    }
    
    var body: some Scene {
        WindowGroup {
            MainContainer()
        }
    }
}
