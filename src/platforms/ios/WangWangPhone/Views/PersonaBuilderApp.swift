import SwiftUI

struct PersonaBuilderAppView: View {
    @Binding var isPresented: Bool
    @State private var personaName = ""
    @State private var personaDescription = ""
    @State private var generatedPrompt = ""
    @State private var isGenerating = false
    @State private var showCopySuccess = false
    @Environment(\.colorScheme) var colorScheme
    
    var body: some View {
        NavigationView {
            ZStack {
                Color(UIColor.systemGroupedBackground).ignoresSafeArea()
                
                ScrollView {
                    VStack(spacing: 20) {
                        // 标题说明
                        VStack(alignment: .leading, spacing: 8) {
                            Text("✨ 神笔马良")
                                .font(.title2)
                                .fontWeight(.bold)
                            Text("输入角色名称和描述，AI将为你生成完整的人设提示词")
                                .font(.caption)
                                .foregroundColor(.gray)
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding()
                        .background(Color(UIColor.secondarySystemGroupedBackground))
                        .cornerRadius(12)
                        .padding(.horizontal)
                        
                        // 输入区域
                        VStack(alignment: .leading, spacing: 15) {
                            Text("角色名称")
                                .font(.headline)
                                .foregroundColor(.primary)
                            
                            TextField("例如：小红书博主、技术专家、心理咨询师", text: $personaName)
                                .textFieldStyle(RoundedBorderTextFieldStyle())
                                .padding(.horizontal)
                            
                            Text("角色描述")
                                .font(.headline)
                                .foregroundColor(.primary)
                                .padding(.top, 10)
                            
                            TextEditor(text: $personaDescription)
                                .frame(height: 150)
                                .padding(8)
                                .background(Color(UIColor.systemBackground))
                                .cornerRadius(8)
                                .overlay(
                                    RoundedRectangle(cornerRadius: 8)
                                        .stroke(Color.gray.opacity(0.3), lineWidth: 1)
                                )
                                .padding(.horizontal)
                            
                            Text("提示：描述角色的性格、专业领域、说话风格等特征")
                                .font(.caption)
                                .foregroundColor(.gray)
                                .padding(.horizontal)
                        }
                        .padding()
                        .background(Color(UIColor.secondarySystemGroupedBackground))
                        .cornerRadius(12)
                        .padding(.horizontal)
                        
                        // 生成按钮
                        Button(action: generatePrompt) {
                            HStack {
                                if isGenerating {
                                    ProgressView()
                                        .progressViewStyle(CircularProgressViewStyle(tint: .white))
                                } else {
                                    Image(systemName: "wand.and.stars")
                                }
                                Text(isGenerating ? "生成中..." : "生成人设提示词")
                                    .fontWeight(.semibold)
                            }
                            .frame(maxWidth: .infinity)
                            .padding()
                            .background(
                                LinearGradient(
                                    colors: [Color(red: 1.0, green: 0.84, blue: 0.0), Color(red: 1.0, green: 0.65, blue: 0.0)],
                                    startPoint: .leading,
                                    endPoint: .trailing
                                )
                            )
                            .foregroundColor(.white)
                            .cornerRadius(12)
                        }
                        .disabled(personaName.isEmpty || personaDescription.isEmpty || isGenerating)
                        .padding(.horizontal)
                        
                        // 生成结果
                        if !generatedPrompt.isEmpty {
                            VStack(alignment: .leading, spacing: 15) {
                                HStack {
                                    Text("生成的提示词")
                                        .font(.headline)
                                        .foregroundColor(.primary)
                                    Spacer()
                                    Button(action: copyToClipboard) {
                                        HStack(spacing: 4) {
                                            Image(systemName: showCopySuccess ? "checkmark.circle.fill" : "doc.on.doc")
                                            Text(showCopySuccess ? "已复制" : "复制")
                                        }
                                        .font(.caption)
                                        .foregroundColor(showCopySuccess ? .green : .blue)
                                    }
                                }
                                
                                ScrollView {
                                    Text(generatedPrompt)
                                        .font(.body)
                                        .foregroundColor(.primary)
                                        .frame(maxWidth: .infinity, alignment: .leading)
                                        .padding()
                                        .background(Color(UIColor.systemBackground))
                                        .cornerRadius(8)
                                }
                                .frame(height: 200)
                                
                                Text("提示：可以将此提示词复制到聊天应用中使用")
                                    .font(.caption)
                                    .foregroundColor(.gray)
                            }
                            .padding()
                            .background(Color(UIColor.secondarySystemGroupedBackground))
                            .cornerRadius(12)
                            .padding(.horizontal)
                        }
                        
                        Spacer(minLength: 20)
                    }
                    .padding(.vertical)
                }
            }
            .navigationTitle("神笔马良")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("关闭") {
                        isPresented = false
                    }
                }
            }
        }
    }
    
    private func generatePrompt() {
        guard !personaName.isEmpty && !personaDescription.isEmpty else { return }
        
        isGenerating = true
        
        // 模拟生成过程
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.5) {
            let prompt = buildPersonaPrompt(name: personaName, description: personaDescription)
            generatedPrompt = prompt
            isGenerating = false
        }
    }
    
    private func buildPersonaPrompt(name: String, description: String) -> String {
        return """
        # 角色设定
        
        你是一位\(name)，具有以下特征：
        
        \(description)
        
        # 行为准则
        
        1. **专业性**：始终保持专业态度，提供准确、有价值的信息
        2. **个性化**：根据用户需求调整回答风格，展现独特的个人魅力
        3. **同理心**：理解用户的情感需求，给予温暖和支持
        4. **创造性**：在专业范围内发挥创意，提供新颖的见解
        
        # 沟通风格
        
        - 使用符合角色身份的语言和表达方式
        - 保持友好、耐心的态度
        - 适当使用emoji增加亲和力
        - 回答简洁明了，重点突出
        
        # 互动原则
        
        - 主动询问用户需求，确保理解准确
        - 提供具体、可操作的建议
        - 鼓励用户提问和互动
        - 在专业领域内深入探讨
        
        请始终以\(name)的身份与用户交流，展现你的专业能力和独特魅力。
        """
    }
    
    private func copyToClipboard() {
        UIPasteboard.general.string = generatedPrompt
        showCopySuccess = true
        
        // 震动反馈
        let generator = UINotificationFeedbackGenerator()
        generator.notificationOccurred(.success)
        
        // 2秒后恢复按钮状态
        DispatchQueue.main.asyncAfter(deadline: .now() + 2) {
            showCopySuccess = false
        }
    }
}

struct PersonaBuilderAppView_Previews: PreviewProvider {
    static var previews: some View {
        PersonaBuilderAppView(isPresented: .constant(true))
    }
}
