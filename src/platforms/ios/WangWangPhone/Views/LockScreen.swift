import SwiftUI

struct LockScreen: View {
    var onUnlock: () -> Void
    @State private var currentTime = Date()
    @State private var dragOffset: CGFloat = 0
    @State private var lockWallpaper: UIImage? = WallpaperManager.shared.getWallpaperImage(type: .lock)
    let timer = Timer.publish(every: 1, on: .main, in: .common).autoconnect()

    var body: some View {
        ZStack {
            if let wallpaper = lockWallpaper {
                Image(uiImage: wallpaper)
                    .resizable()
                    .aspectRatio(contentMode: .fill)
                    .ignoresSafeArea()
            } else {
                Color.black.ignoresSafeArea()
            }
            
            VStack(spacing: 0) {
                Text(currentTime, style: .time)
                    .font(.system(size: 80, weight: .thin))
                    .foregroundColor(.white)
                    .padding(.top, 100)
                
                Text(formatDate(currentTime))
                    .font(.title3)
                    .foregroundColor(.white)
            }
            .offset(y: dragOffset / 3) // 视觉反馈
            
            VStack {
                Spacer()
                Text("向上滑动解锁")
                    .font(.headline)
                    .foregroundColor(.white.opacity(0.6))
                    .padding(.bottom, 50)
            }
        }
        .contentShape(Rectangle())
        .gesture(
            DragGesture()
                .onChanged { value in
                    if value.translation.height < 0 {
                        dragOffset = value.translation.height
                    }
                }
                .onEnded { value in
                    if value.translation.height < -150 { // 滑动超过一定距离解锁
                        onUnlock()
                    } else {
                        withAnimation(.spring()) {
                            dragOffset = 0
                        }
                    }
                }
        )
        .onReceive(timer) { input in
            currentTime = input
        }
        .onReceive(NotificationCenter.default.publisher(for: NSNotification.Name("WallpaperChanged"))) { _ in
            self.lockWallpaper = WallpaperManager.shared.getWallpaperImage(type: .lock)
        }
    }
    
    func formatDate(_ date: Date) -> String {
        let formatter = DateFormatter()
        formatter.dateFormat = "M月d日 EEEE"
        formatter.locale = Locale(identifier: "zh_CN")
        return formatter.string(from: date)
    }
}