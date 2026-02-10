import SwiftUI
import AVFoundation
import Photos

// MARK: - Camera Permission Manager
class CameraPermissionManager: ObservableObject {
    @Published var authorizationStatus: AVAuthorizationStatus = .notDetermined
    
    init() {
        authorizationStatus = AVCaptureDevice.authorizationStatus(for: .video)
    }
    
    func requestPermission() {
        AVCaptureDevice.requestAccess(for: .video) { [weak self] granted in
            DispatchQueue.main.async {
                self?.authorizationStatus = AVCaptureDevice.authorizationStatus(for: .video)
            }
        }
    }
}

// MARK: - Camera Manager for Photo Capture
class CameraManager: NSObject, ObservableObject {
    var session: AVCaptureSession?
    var photoOutput: AVCapturePhotoOutput?
    var previewLayer: AVCaptureVideoPreviewLayer?
    
    @Published var photosTaken: Int = 0
    @Published var showFlash: Bool = false
    
    func capturePhoto() {
        guard let photoOutput = photoOutput else { return }
        let settings = AVCapturePhotoSettings()
        photoOutput.capturePhoto(with: settings, delegate: self)
    }
}

extension CameraManager: AVCapturePhotoCaptureDelegate {
    func photoOutput(_ output: AVCapturePhotoOutput, didFinishProcessingPhoto photo: AVCapturePhoto, error: Error?) {
        guard error == nil, let data = photo.fileDataRepresentation() else { return }
        
        // Save to photo library
        PHPhotoLibrary.requestAuthorization { status in
            if status == .authorized {
                PHPhotoLibrary.shared().performChanges({
                    let request = PHAssetCreationRequest.forAsset()
                    request.addResource(with: .photo, data: data, options: nil)
                }) { success, error in
                    DispatchQueue.main.async {
                        if success {
                            self.photosTaken += 1
                            self.showFlash = true
                            DispatchQueue.main.asyncAfter(deadline: .now() + 0.15) {
                                self.showFlash = false
                            }
                        }
                    }
                }
            }
        }
    }
}

// MARK: - Camera Preview UIViewRepresentable
struct CameraPreviewView: UIViewRepresentable {
    let useFrontCamera: Bool
    @ObservedObject var cameraManager: CameraManager
    
    func makeUIView(context: Context) -> UIView {
        let view = UIView(frame: .zero)
        view.backgroundColor = .black
        
        let session = AVCaptureSession()
        session.sessionPreset = .high
        
        let position: AVCaptureDevice.Position = useFrontCamera ? .front : .back
        guard let device = AVCaptureDevice.default(.builtInWideAngleCamera, for: .video, position: position),
              let input = try? AVCaptureDeviceInput(device: device) else {
            return view
        }
        
        if session.canAddInput(input) {
            session.addInput(input)
        }
        
        // Add photo output
        let photoOutput = AVCapturePhotoOutput()
        if session.canAddOutput(photoOutput) {
            session.addOutput(photoOutput)
        }
        
        let previewLayer = AVCaptureVideoPreviewLayer(session: session)
        previewLayer.videoGravity = .resizeAspectFill
        view.layer.addSublayer(previewLayer)
        
        cameraManager.session = session
        cameraManager.photoOutput = photoOutput
        cameraManager.previewLayer = previewLayer
        
        context.coordinator.previewLayer = previewLayer
        context.coordinator.session = session
        
        DispatchQueue.global(qos: .userInitiated).async {
            session.startRunning()
        }
        
        return view
    }
    
    func updateUIView(_ uiView: UIView, context: Context) {
        DispatchQueue.main.async {
            context.coordinator.previewLayer?.frame = uiView.bounds
        }
    }
    
    static func dismantleUIView(_ uiView: UIView, coordinator: Coordinator) {
        DispatchQueue.global(qos: .userInitiated).async {
            coordinator.session?.stopRunning()
        }
    }
    
    func makeCoordinator() -> Coordinator {
        Coordinator()
    }
    
    class Coordinator {
        var previewLayer: AVCaptureVideoPreviewLayer?
        var session: AVCaptureSession?
    }
}

// MARK: - Camera App View
struct CameraAppView: View {
    @Binding var isPresented: Bool
    @StateObject private var permissionManager = CameraPermissionManager()
    @StateObject private var cameraManager = CameraManager()
    
    // Camera mode state
    let cameraModes = ["延时摄影", "慢动作", "视频", "照片", "人像", "全景"]
    @State private var selectedModeIndex: Int = 3 // Default "照片"
    
    // Front/Back camera state
    @State private var useFrontCamera: Bool = false
    
    // Shutter animation
    @State private var isShutterPressed: Bool = false
    
    var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()
            
            // Camera Preview or Permission Placeholder
            switch permissionManager.authorizationStatus {
            case .authorized:
                CameraPreviewView(useFrontCamera: useFrontCamera, cameraManager: cameraManager)
                    .ignoresSafeArea()
                    .id(useFrontCamera) // Force re-create when switching camera
                
            case .notDetermined:
                VStack(spacing: 16) {
                    Image(systemName: "camera.fill")
                        .font(.system(size: 48))
                        .foregroundColor(.white)
                    Text("需要相机权限才能使用")
                        .foregroundColor(.white)
                        .font(.body)
                    Button("点击授权") {
                        permissionManager.requestPermission()
                    }
                    .foregroundColor(.yellow)
                    .fontWeight(.bold)
                }
                
            case .denied, .restricted:
                VStack(spacing: 16) {
                    Image(systemName: "camera.fill")
                        .font(.system(size: 48))
                        .foregroundColor(.white)
                    Text("相机权限已被拒绝")
                        .foregroundColor(.white)
                        .font(.body)
                    Text("请在系统设置中开启相机权限")
                        .foregroundColor(.white.opacity(0.7))
                        .font(.caption)
                    Button("打开设置") {
                        if let url = URL(string: UIApplication.openSettingsURLString) {
                            UIApplication.shared.open(url)
                        }
                    }
                    .foregroundColor(.yellow)
                    .fontWeight(.bold)
                }
                
            @unknown default:
                EmptyView()
            }
            
            // Flash overlay
            if cameraManager.showFlash {
                Color.white.ignoresSafeArea()
            }
            
            // UI Overlay
            VStack {
                // Top Bar - with safe area padding
                HStack {
                    Image(systemName: "bolt.fill")
                    Spacer()
                    if cameraManager.photosTaken > 0 {
                        Text("已拍 \(cameraManager.photosTaken) 张")
                            .foregroundColor(.white.opacity(0.7))
                            .font(.caption)
                    }
                    Spacer()
                    Button("完成") { isPresented = false }
                        .foregroundColor(.yellow)
                        .fontWeight(.bold)
                }
                .foregroundColor(.white)
                .padding(.horizontal)
                .padding(.top, 8)
                
                Spacer()
                
                // Bottom UI
                VStack(spacing: 20) {
                    // Camera Mode Selector
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 20) {
                            ForEach(0..<cameraModes.count, id: \.self) { index in
                                Text(cameraModes[index])
                                    .foregroundColor(index == selectedModeIndex ? .yellow : .white)
                                    .fontWeight(index == selectedModeIndex ? .bold : .regular)
                                    .onTapGesture {
                                        withAnimation(.easeInOut(duration: 0.2)) {
                                            selectedModeIndex = index
                                        }
                                    }
                            }
                        }
                        .font(.caption)
                        .padding(.horizontal, 100)
                    }
                    
                    // Controls
                    HStack(spacing: 50) {
                        // Gallery
                        ZStack {
                            Circle()
                                .fill(Color.gray)
                                .frame(width: 50, height: 50)
                            if cameraManager.photosTaken > 0 {
                                Text("\(cameraManager.photosTaken)")
                                    .foregroundColor(.white)
                                    .font(.caption)
                            }
                        }
                        
                        // Shutter
                        ZStack {
                            Circle()
                                .stroke(Color.white, lineWidth: 4)
                                .frame(width: 80, height: 80)
                            Circle()
                                .fill(selectedModeIndex == 2 ? Color.red : Color.white) // Video mode = red
                                .frame(width: 70, height: 70)
                        }
                        .scaleEffect(isShutterPressed ? 0.85 : 1.0)
                        .animation(.easeInOut(duration: 0.1), value: isShutterPressed)
                        .onTapGesture {
                            isShutterPressed = true
                            UIImpactFeedbackGenerator(style: .medium).impactOccurred()
                            
                            // 拍照
                            cameraManager.capturePhoto()
                            
                            DispatchQueue.main.asyncAfter(deadline: .now() + 0.15) {
                                isShutterPressed = false
                            }
                        }
                        
                        // Flip Camera
                        Button(action: {
                            useFrontCamera.toggle()
                        }) {
                            Image(systemName: "camera.rotate")
                                .font(.system(size: 30))
                                .foregroundColor(.white)
                        }
                    }
                    .padding(.bottom, 30)
                }
                .background(Color.black.opacity(0.5))
            }
            .padding(.top) // Respect safe area at top for status bar
        }
        .onAppear {
            if permissionManager.authorizationStatus == .notDetermined {
                permissionManager.requestPermission()
            }
        }
    }
}
