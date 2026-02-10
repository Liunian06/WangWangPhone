import SwiftUI
import AVFoundation

struct CameraAppView: View {
    @Binding var isPresented: Bool
    
    var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()
            
            // Camera Preview Placeholder
            Rectangle()
                .fill(Color.gray.opacity(0.3))
                .ignoresSafeArea()
                .overlay(
                    Text("相机预览区域")
                        .foregroundColor(.white.opacity(0.5))
                )
            
            VStack {
                // Top Bar
                HStack {
                    Image(systemName: "bolt.fill")
                    Spacer()
                    Button("完成") { isPresented = false }
                        .foregroundColor(.yellow)
                        .fontWeight(.bold)
                }
                .foregroundColor(.white)
                .padding()
                
                Spacer()
                
                // Bottom UI
                VStack(spacing: 20) {
                    // Modes
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 20) {
                            Text("延时摄影")
                            Text("慢动作")
                            Text("视频")
                            Text("照片").foregroundColor(.yellow).fontWeight(.bold)
                            Text("人像")
                            Text("全景")
                        }
                        .font(.caption)
                        .foregroundColor(.white)
                        .padding(.horizontal, 100)
                    }
                    
                    // Controls
                    HStack(spacing: 50) {
                        // Gallery
                        Circle()
                            .fill(Color.gray)
                            .frame(width: 50, height: 50)
                        
                        // Shutter
                        ZStack {
                            Circle()
                                .stroke(Color.white, lineWidth: 4)
                                .frame(width: 80, height: 80)
                            Circle()
                                .fill(Color.white)
                                .frame(width: 70, height: 70)
                        }
                        
                        // Flip
                        Image(systemName: "camera.rotate")
                            .font(.system(size: 30))
                            .foregroundColor(.white)
                    }
                    .padding(.bottom, 30)
                }
                .background(Color.black.opacity(0.5))
            }
        }
    }
}
