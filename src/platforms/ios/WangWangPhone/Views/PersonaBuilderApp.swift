import SwiftUI

struct PersonaBuilderApp: View {
    @Environment(\.dismiss) var dismiss
    @Environment(\.colorScheme) var colorScheme
    
    @State private var name = ""
    @State private var gender = ""
    @State private var age = ""
    @State private var personality = ""
    @State private var background = ""
    @State private var appearance = ""
    @State private var occupation = ""
    @State private var hobbies = ""
    @State private var relationships = ""
    @State private var goals = ""
    @State private var speechStyle = ""
    @State private var specialTraits = ""
    
    @State private var showImportDialog = false
    @State private var showSuccessMessage = false
    @State private var importText = ""
    
    private let dbHelper = PersonaDbHelper()
    
    var body: some View {
        NavigationView {
            ZStack {
                (colorScheme == .dark ? Color(hex: "1C1C1E") : Color(hex: "F2F2F7"))
                    .ignoresSafeArea()
                
                ScrollView {
                    VStack(spacing: 12) {
                        Text("角色人设设计")
                            .font(.system(size: 20, weight: .bold))
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .padding(.bottom, 8)
                        
                        PersonaField(label: "姓名", text: $name)
                        PersonaField(label: "性别", text: $gender)
                        PersonaField(label: "年龄", text: $age)
                        PersonaField(label: "性格特点", text: $personality, multiline: true)
                        PersonaField(label: "背景故事", text: $background, multiline: true)
                        PersonaField(label: "外貌特征", text: $appearance, multiline: true)
                        PersonaField(label: "职业", text: $occupation)
                        PersonaField(label: "兴趣爱好", text: $hobbies, multiline: true)
                        PersonaField(label: "人际关系", text: $relationships, multiline: true)
                        PersonaField(label: "目标与动机", text: $goals, multiline: true)
                        PersonaField(label: "说话风格", text: $speechStyle, multiline: true)
                        PersonaField(label: "特殊技能或习惯", text: $specialTraits, multiline: true)
                        
                        HStack(spacing: 12) {
                            Button(action: copyPersona) {
                                Text("复制人设")
                                    .font(.system(size: 16))
                                    .foregroundColor(.white)
                                    .frame(maxWidth: .infinity)
                                    .frame(height: 48)
                                    .background(Color(hex: "34C759"))
                                    .cornerRadius(10)
                            }
                            
                            Button(action: { showImportDialog = true }) {
                                Text("导入联系人")
                                    .font(.system(size: 16))
                                    .foregroundColor(.white)
                                    .frame(maxWidth: .infinity)
                                    .frame(height: 48)
                                    .background(Color(hex: "5856D6"))
                                    .cornerRadius(10)
                            }
                        }
                        .padding(.top, 8)
                        
                        Button(action: savePersona) {
                            Text("保存人设")
                                .font(.system(size: 16))
                                .foregroundColor(.white)
                                .frame(maxWidth: .infinity)
                                .frame(height: 48)
                                .background(Color(hex: "007AFF"))
                                .cornerRadius(10)
                        }
                        
                        Spacer().frame(height: 20)
                    }
                    .padding(16)
                }
                
                if showSuccessMessage {
                    VStack {
                        Spacer()
                        Text("操作成功")
                            .font(.system(size: 16))
                            .foregroundColor(.white)
                            .padding(.horizontal, 24)
                            .padding(.vertical, 16)
                            .background(Color.black.opacity(0.8))
                            .cornerRadius(12)
                        Spacer()
                    }
                }
            }
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("关闭") {
                        dismiss()
                    }
                    .foregroundColor(Color(hex: "007AFF"))
                }
                ToolbarItem(placement: .principal) {
                    Text("神笔马良")
                        .font(.system(size: 18, weight: .semibold))
                }
            }
        }
        .onAppear(perform: loadPersona)
        .sheet(isPresented: $showImportDialog) {
            ImportContactDialog(importText: $importText, onImport: importPersona)
        }
    }
    
    private func loadPersona() {
        if let persona = dbHelper.getPersona() {
            name = persona.name
            gender = persona.gender
            age = persona.age
            personality = persona.personality
            background = persona.background
            appearance = persona.appearance
            occupation = persona.occupation
            hobbies = persona.hobbies
            relationships = persona.relationships
            goals = persona.goals
            speechStyle = persona.speechStyle
            specialTraits = persona.specialTraits
        }
    }
    
    private func savePersona() {
        let persona = PersonaRecord(
            name: name, gender: gender, age: age, personality: personality,
            background: background, appearance: appearance, occupation: occupation,
            hobbies: hobbies, relationships: relationships, goals: goals,
            speechStyle: speechStyle, specialTraits: specialTraits
        )
        dbHelper.savePersona(persona)
        showSuccessMessage = true
        DispatchQueue.main.asyncAfter(deadline: .now() + 2) {
            showSuccessMessage = false
        }
    }
    
    private func copyPersona() {
        let text = buildPersonaText()
        UIPasteboard.general.string = text
        showSuccessMessage = true
        DispatchQueue.main.asyncAfter(deadline: .now() + 2) {
            showSuccessMessage = false
        }
    }
    
    private func importPersona() {
        if let parsed = parseContactToPersona(importText) {
            name = parsed["name"] ?? name
            gender = parsed["gender"] ?? gender
            age = parsed["age"] ?? age
            personality = parsed["personality"] ?? personality
            background = parsed["background"] ?? background
            appearance = parsed["appearance"] ?? appearance
            occupation = parsed["occupation"] ?? occupation
            hobbies = parsed["hobbies"] ?? hobbies
            relationships = parsed["relationships"] ?? relationships
            goals = parsed["goals"] ?? goals
            speechStyle = parsed["speechStyle"] ?? speechStyle
            specialTraits = parsed["specialTraits"] ?? specialTraits
            showImportDialog = false
            showSuccessMessage = true
            DispatchQueue.main.asyncAfter(deadline: .now() + 2) {
                showSuccessMessage = false
            }
        }
    }
    
    private func buildPersonaText() -> String {
        var lines: [String] = []
        if !name.isEmpty { lines.append("姓名：\(name)") }
        if !gender.isEmpty { lines.append("性别：\(gender)") }
        if !age.isEmpty { lines.append("年龄：\(age)") }
        if !personality.isEmpty { lines.append("性格特点：\(personality)") }
        if !background.isEmpty { lines.append("背景故事：\(background)") }
        if !appearance.isEmpty { lines.append("外貌特征：\(appearance)") }
        if !occupation.isEmpty { lines.append("职业：\(occupation)") }
        if !hobbies.isEmpty { lines.append("兴趣爱好：\(hobbies)") }
        if !relationships.isEmpty { lines.append("人际关系：\(relationships)") }
        if !goals.isEmpty { lines.append("目标与动机：\(goals)") }
        if !speechStyle.isEmpty { lines.append("说话风格：\(speechStyle)") }
        if !specialTraits.isEmpty { lines.append("特殊技能或习惯：\(specialTraits)") }
        return lines.joined(separator: "\n")
    }
    
    private func parseContactToPersona(_ text: String) -> [String: String]? {
        guard !text.isEmpty else { return nil }
        var result: [String: String] = [:]
        let lines = text.components(separatedBy: .newlines)
        
        for line in lines {
            if line.hasPrefix("姓名：") || line.hasPrefix("姓名:") {
                result["name"] = line.replacingOccurrences(of: "姓名：", with: "").replacingOccurrences(of: "姓名:", with: "")
            } else if line.hasPrefix("性别：") || line.hasPrefix("性别:") {
                result["gender"] = line.replacingOccurrences(of: "性别：", with: "").replacingOccurrences(of: "性别:", with: "")
            } else if line.hasPrefix("年龄：") || line.hasPrefix("年龄:") {
                result["age"] = line.replacingOccurrences(of: "年龄：", with: "").replacingOccurrences(of: "年龄:", with: "")
            } else if line.hasPrefix("性格特点：") || line.hasPrefix("性格特点:") {
                result["personality"] = line.replacingOccurrences(of: "性格特点：", with: "").replacingOccurrences(of: "性格特点:", with: "")
            } else if line.hasPrefix("背景故事：") || line.hasPrefix("背景故事:") {
                result["background"] = line.replacingOccurrences(of: "背景故事：", with: "").replacingOccurrences(of: "背景故事:", with: "")
            } else if line.hasPrefix("外貌特征：") || line.hasPrefix("外貌特征:") {
                result["appearance"] = line.replacingOccurrences(of: "外貌特征：", with: "").replacingOccurrences(of: "外貌特征:", with: "")
            } else if line.hasPrefix("职业：") || line.hasPrefix("职业:") {
                result["occupation"] = line.replacingOccurrences(of: "职业：", with: "").replacingOccurrences(of: "职业:", with: "")
            } else if line.hasPrefix("兴趣爱好：") || line.hasPrefix("兴趣爱好:") {
                result["hobbies"] = line.replacingOccurrences(of: "兴趣爱好：", with: "").replacingOccurrences(of: "兴趣爱好:", with: "")
            } else if line.hasPrefix("人际关系：") || line.hasPrefix("人际关系:") {
                result["relationships"] = line.replacingOccurrences(of: "人际关系：", with: "").replacingOccurrences(of: "人际关系:", with: "")
            } else if line.hasPrefix("目标与动机：") || line.hasPrefix("目标与动机:") {
                result["goals"] = line.replacingOccurrences(of: "目标与动机：", with: "").replacingOccurrences(of: "目标与动机:", with: "")
            } else if line.hasPrefix("说话风格：") || line.hasPrefix("说话风格:") {
                result["speechStyle"] = line.replacingOccurrences(of: "说话风格：", with: "").replacingOccurrences(of: "说话风格:", with: "")
            } else if line.hasPrefix("特殊技能或习惯：") || line.hasPrefix("特殊技能或习惯:") {
                result["specialTraits"] = line.replacingOccurrences(of: "特殊技能或习惯：", with: "").replacingOccurrences(of: "特殊技能或习惯:", with: "")
            }
        }
        
        return result.isEmpty ? nil : result
    }
}

struct PersonaField: View {
    let label: String
    @Binding var text: String
    var multiline: Bool = false
    @Environment(\.colorScheme) var colorScheme
    
    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(label)
                .font(.system(size: 14))
                .foregroundColor(.gray)
            
            if multiline {
                TextEditor(text: $text)
                    .frame(height: 120)
                    .padding(8)
                    .background(colorScheme == .dark ? Color(hex: "2C2C2E") : Color.white)
                    .cornerRadius(10)
            } else {
                TextField("", text: $text)
                    .padding(12)
                    .background(colorScheme == .dark ? Color(hex: "2C2C2E") : Color.white)
                    .cornerRadius(10)
            }
        }
    }
}

struct ImportContactDialog: View {
    @Environment(\.dismiss) var dismiss
    @Binding var importText: String
    let onImport: () -> Void
    
    var body: some View {
        NavigationView {
            VStack(spacing: 16) {
                Text("粘贴联系人信息，系统将自动解析")
                    .font(.system(size: 14))
                    .foregroundColor(.gray)
                    .frame(maxWidth: .infinity, alignment: .leading)
                
                TextEditor(text: $importText)
                    .frame(height: 200)
                    .padding(8)
                    .background(Color(UIColor.systemGray6))
                    .cornerRadius(10)
                
                Spacer()
            }
            .padding()
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("取消") {
                        dismiss()
                    }
                }
                ToolbarItem(placement: .principal) {
                    Text("导入联系人")
                        .font(.system(size: 18, weight: .semibold))
                }
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("粘贴") {
                        if let clipboardText = UIPasteboard.general.string {
                            importText = clipboardText
                        }
                    }
                }
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("导入") {
                        onImport()
                    }
                }
            }
        }
    }
}

extension Color {
    init(hex: String) {
        let hex = hex.trimmingCharacters(in: CharacterSet.alphanumerics.inverted)
        var int: UInt64 = 0
        Scanner(string: hex).scanHexInt64(&int)
        let a, r, g, b: UInt64
        switch hex.count {
        case 3:
            (a, r, g, b) = (255, (int >> 8) * 17, (int >> 4 & 0xF) * 17, (int & 0xF) * 17)
        case 6:
            (a, r, g, b) = (255, int >> 16, int >> 8 & 0xFF, int & 0xFF)
        case 8:
            (a, r, g, b) = (int >> 24, int >> 16 & 0xFF, int >> 8 & 0xFF, int & 0xFF)
        default:
            (a, r, g, b) = (255, 0, 0, 0)
        }
        self.init(.sRGB, red: Double(r) / 255, green: Double(g) / 255, blue: Double(b) / 255, opacity: Double(a) / 255)
    }
}
