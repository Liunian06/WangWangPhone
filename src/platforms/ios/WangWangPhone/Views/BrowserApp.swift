import SwiftUI
import WebKit

struct BrowserAppView: View {
    @Binding var isPresented: Bool
    @State private var urlString: String = "https://www.baidu.com"
    @State private var inputUrl: String = "https://www.baidu.com"
    @State private var canGoBack: Bool = false
    @State private var canGoForward: Bool = false
    @State private var isLoading: Bool = false
    @State private var progress: Double = 0
    
    var body: some View {
        VStack(spacing: 0) {
            // Address Bar
            HStack(spacing: 12) {
                HStack {
                    Image(systemName: "lock.fill")
                        .font(.system(size: 12))
                        .foregroundColor(.secondary)
                    
                    TextField("搜索或输入网站名称", text: $inputUrl)
                        .font(.system(size: 16))
                        .keyboardType(.webSearch)
                        .onSubmit {
                            navigateTo(inputUrl)
                        }
                    
                    if isLoading {
                        ProgressView()
                            .scaleEffect(0.8)
                    }
                }
                .padding(.horizontal, 10)
                .padding(.vertical, 8)
                .background(Color(.systemGray6))
                .cornerRadius(10)
                
                Button("完成") {
                    isPresented = false
                }
                .foregroundColor(.blue)
            }
            .padding(.horizontal)
            .padding(.top, 10)
            .padding(.bottom, 8)
            .background(Color(.systemBackground))
            
            // Progress Bar
            if isLoading && progress < 1.0 {
                GeometryReader { geo in
                    Rectangle()
                        .fill(Color.blue)
                        .frame(width: geo.size.width * CGFloat(progress), height: 2)
                }
                .frame(height: 2)
            } else {
                Divider()
            }
            
            // WebView
            WebViewContainer(
                urlString: $urlString,
                canGoBack: $canGoBack,
                canGoForward: $canGoForward,
                isLoading: $isLoading,
                progress: $progress
            )
            .edgesIgnoringSafeArea(.bottom)
            
            // Toolbar
            Divider()
            HStack {
                Button(action: { /* Go back logic handled by coordinator */ }) {
                    Image(systemName: "chevron.left")
                        .font(.system(size: 20, weight: .semibold))
                        .foregroundColor(canGoBack ? .blue : .gray)
                }
                .disabled(!canGoBack)
                
                Spacer()
                
                Button(action: { /* Go forward logic handled by coordinator */ }) {
                    Image(systemName: "chevron.right")
                        .font(.system(size: 20, weight: .semibold))
                        .foregroundColor(canGoForward ? .blue : .gray)
                }
                .disabled(!canGoForward)
                
                Spacer()
                
                Button(action: {}) {
                    Image(systemName: "square.and.arrow.up")
                        .font(.system(size: 20, weight: .semibold))
                        .foregroundColor(.blue)
                }
                
                Spacer()
                
                Button(action: {}) {
                    Image(systemName: "book")
                        .font(.system(size: 20, weight: .semibold))
                        .foregroundColor(.blue)
                }
                
                Spacer()
                
                Button(action: {}) {
                    Image(systemName: "square.on.square")
                        .font(.system(size: 20, weight: .semibold))
                        .foregroundColor(.blue)
                }
            }
            .padding(.horizontal, 25)
            .padding(.vertical, 10)
            .background(Color(.systemBackground))
        }
        .background(Color(.systemBackground))
    }
    
    private func navigateTo(_ input: String) {
        var target = input.trimmingCharacters(in: .whitespaces)
        if target.isEmpty { return }
        if !target.lowercased().hasPrefix("http://") && !target.lowercased().hasPrefix("https://") {
            if target.contains(".") && !target.contains(" ") {
                target = "https://" + target
            } else {
                let query = target.addingPercentEncoding(withAllowedCharacters: .urlQueryValueAllowed) ?? ""
                target = "https://www.baidu.com/s?wd=" + query
            }
        }
        urlString = target
    }
}

extension CharacterSet {
    static let urlQueryValueAllowed: CharacterSet = {
        let generalDelimitersToEncode = ":#[]@" // does not include "?" or "/"
        let subDelimitersToEncode = "!$&'()*+,;="
        var allowed = CharacterSet.urlQueryAllowed
        allowed.remove(charactersIn: "\(generalDelimitersToEncode)\(subDelimitersToEncode)")
        return allowed
    }()
}

struct WebViewContainer: UIViewRepresentable {
    @Binding var urlString: String
    @Binding var canGoBack: Bool
    @Binding var canGoForward: Bool
    @Binding var isLoading: Bool
    @Binding var progress: Double
    
    func makeUIView(context: Context) -> WKWebView {
        let webView = WKWebView()
        webView.navigationDelegate = context.coordinator
        webView.addObserver(context.coordinator, forKeyPath: #keyPath(WKWebView.estimatedProgress), options: .new, context: nil)
        webView.addObserver(context.coordinator, forKeyPath: #keyPath(WKWebView.canGoBack), options: .new, context: nil)
        webView.addObserver(context.coordinator, forKeyPath: #keyPath(WKWebView.canGoForward), options: .new, context: nil)
        return webView
    }
    
    func updateUIView(_ uiView: WKWebView, context: Context) {
        if let url = URL(string: urlString), uiView.url?.absoluteString != urlString {
            let request = URLRequest(url: url)
            uiView.load(request)
        }
    }
    
    func makeCoordinator() -> Coordinator {
        Coordinator(self)
    }
    
    class Coordinator: NSObject, WKNavigationDelegate {
        var parent: WebViewContainer
        
        init(_ parent: WebViewContainer) {
            self.parent = parent
        }
        
        override func observeValue(forKeyPath keyPath: String?, of object: Any?, change: [NSKeyValueChangeKey : Any]?, context: UnsafeMutableRawPointer?) {
            if keyPath == "estimatedProgress" {
                parent.progress = (object as? WKWebView)?.estimatedProgress ?? 0
            } else if keyPath == "canGoBack" {
                parent.canGoBack = (object as? WKWebView)?.canGoBack ?? false
            } else if keyPath == "canGoForward" {
                parent.canGoForward = (object as? WKWebView)?.canGoForward ?? false
            }
        }
        
        func webView(_ webView: WKWebView, didStartProvisionalNavigation navigation: WKNavigation!) {
            parent.isLoading = true
        }
        
        func webView(_ webView: WKWebView, didFinish navigation: WKNavigation!) {
            parent.isLoading = false
            if let url = webView.url?.absoluteString {
                parent.urlString = url
            }
        }
        
        func webView(_ webView: WKWebView, didFail navigation: WKNavigation!, withError error: Error) {
            parent.isLoading = false
        }
    }
}
