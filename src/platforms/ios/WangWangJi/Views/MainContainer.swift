import SwiftUI

struct MainContainer: View {
    @State private var isLocked = true

    var body: some View {
        ZStack {
            if isLocked {
                LockScreen(onUnlock: {
                    withAnimation(.easeInOut(duration: 0.5)) {
                        isLocked = false
                    }
                })
                .transition(.move(edge: .top))
            } else {
                HomeScreen()
                    .transition(.opacity)
            }
        }
    }
}

struct MainContainer_Previews: PreviewProvider {
    static var previews: some View {
        MainContainer()
    }
}