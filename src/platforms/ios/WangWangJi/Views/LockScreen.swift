import SwiftUI

struct LockScreen: View {
    var onUnlock: () -> Void
    @State private var currentTime = Date()
    let timer = Timer.publish(every: 1, on: .main, in: .common).autoconnect()

    var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()
            
            VStack(spacing: 0) {
                Text(currentTime, style: .time)
                    .font(.system(size: 80, weight: .thin))
                    .foregroundColor(.white)
                    .padding(.top, 100)
                
                Text(formatDate(currentTime))
                    .font(.title3)
                    .foregroundColor(.white)
            }
            
            VStack {
                Spacer()
                Text("点击屏幕解锁")
                    .font(.headline)
                    .foregroundColor(.white.opacity(0.6))
                    .padding(.bottom, 50)
                    .onTapGesture {
                        onUnlock()
                    }
            }
        }
        .onReceive(timer) { input in
            currentTime = input
        }
        .contentShape(Rectangle())
        .onTapGesture {
            onUnlock()
        }
    }
    
    func formatDate(_ date: Date) -> String {
        let formatter = DateFormatter()
        formatter.dateFormat = "M月d日 EEEE"
        formatter.locale = Locale(identifier: "zh_CN")
        return formatter.string(from: date)
    }
}