import SwiftUI
import PhotosUI

struct IconCustomizationView: View {
    @Binding var showIconCustomization: Bool
    var onIconChanged: () -> Void
    
    @State private var customIcons: [String: UIImage] = [:]
    @State private var selectedAppId: String?
    @State private var showImagePicker = false
    
    @Environment(\.colorScheme) var colorScheme
    
    private let iconManager = IconCustomizationManager.shared
    private let defaultApps = getDefaultApps()
    
    var body: some View {
        NavigationView {
            ZStack {
                Color(UIColor.systemGroupedBackground).ignoresSafeArea()
                
                ScrollView {
                    VStack(spacing: 12) {
                        ForEach(defaultApps, id: \.id) { app in
                            IconCustomizationRow(
                                app: app,
                                customIcon: customIcons[app.id],
                                colorScheme: colorScheme,
                                onTap: {
                                    selectedAppId = app.id
                                    showImagePicker = true
                                },
                                onReset: {
                                    _ = iconManager.clearCustomIcon(appId: app.id)
                                    customIcons.removeValue(forKey: app.id)
                                    onIconChanged()
                                }
                            )
                        }
                    }
                    .padding(.vertical)
                }
            }
            .navigationTitle("桌面图标设置")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("返回") {
                        showIconCustomization = false
                    }
                }
            }
            .sheet(isPresented: $showImagePicker) {
                IconImagePicker(selectedAppId: $selectedAppId, customIcons: $customIcons, onIconChanged: onIconChanged)
            }
            .onAppear {
                loadCustomIcons()
            }
        }
    }
    
    private func loadCustomIcons() {
        let records = iconManager.getAllCustomIcons()
        for record in records {
            if let image = iconManager.getCustomIconImage(appId: record.appId) {
                customIcons[record.appId] = image
            }
        }
    }
}

struct IconCustomizationRow: View {
    let app: AppIconData
    let customIcon: UIImage?
    let colorScheme: ColorScheme
    let onTap: () -> Void
    let onReset: () -> Void
    
    var body: some View {
        Button(action: onTap) {
            HStack(spacing: 12) {
                // 图标预览
                ZStack {
                    if let customIcon = customIcon {
                        Image(uiImage: customIcon)
                            .resizable()
                            .aspectRatio(contentMode: .fill)
                            .frame(width: 50, height: 50)
                            .clipShape(RoundedRectangle(cornerRadius: 10))
                    } else {
                        if app.useImage {
                            Image(colorScheme == .dark ? "\(app.icon)Dark" : "\(app.icon)Light")
                                .resizable()
                                .aspectRatio(contentMode: .fit)
                                .frame(width: 50, height: 50)
                        } else {
                            Text(app.icon)
                                .font(.system(size: 40))
                                .frame(width: 50, height: 50)
                        }
                    }
                }
                
                VStack(alignment: .leading, spacing: 4) {
                    Text(app.name)
                        .font(.system(size: 16))
                        .foregroundColor(.primary)
                    Text(customIcon != nil ? "已自定义" : "使用默认图标")
                        .font(.system(size: 12))
                        .foregroundColor(.gray)
                }
                
                Spacer()
                
                if customIcon != nil {
                    Button(action: onReset) {
                        Text("恢复默认")
                            .font(.system(size: 14))
                            .foregroundColor(.blue)
                    }
                    .buttonStyle(PlainButtonStyle())
                } else {
                    Image(systemName: "chevron.right")
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundColor(.gray)
                }
            }
            .padding()
            .background(Color(UIColor.secondarySystemGroupedBackground))
            .cornerRadius(10)
        }
        .buttonStyle(PlainButtonStyle())
        .padding(.horizontal)
    }
}

struct IconImagePicker: UIViewControllerRepresentable {
    @Binding var selectedAppId: String?
    @Binding var customIcons: [String: UIImage]
    var onIconChanged: () -> Void
    @Environment(\.presentationMode) var presentationMode
    
    func makeUIViewController(context: Context) -> PHPickerViewController {
        var config = PHPickerConfiguration()
        config.filter = .images
        config.selectionLimit = 1
        
        let picker = PHPickerViewController(configuration: config)
        picker.delegate = context.coordinator
        return picker
    }
    
    func updateUIViewController(_ uiViewController: PHPickerViewController, context: Context) {}
    
    func makeCoordinator() -> Coordinator {
        Coordinator(self)
    }
    
    class Coordinator: NSObject, PHPickerViewControllerDelegate {
        let parent: IconImagePicker
        
        init(_ parent: IconImagePicker) {
            self.parent = parent
        }
        
        func picker(_ picker: PHPickerViewController, didFinishPicking results: [PHPickerResult]) {
            parent.presentationMode.wrappedValue.dismiss()
            
            guard let result = results.first else { return }
            guard let appId = parent.selectedAppId else { return }
            
            result.itemProvider.loadObject(ofClass: UIImage.self) { [weak self] object, error in
                guard let self = self else { return }
                
                if let image = object as? UIImage {
                    DispatchQueue.main.async {
                        let iconManager = IconCustomizationManager.shared
                        if let fileName = iconManager.copyImageToStorage(image) {
                            if iconManager.saveCustomIcon(appId: appId, fileName: fileName) {
                                self.parent.customIcons[appId] = image
                                self.parent.onIconChanged()
                            }
                        }
                        self.parent.selectedAppId = nil
                    }
                }
            }
        }
    }
}
