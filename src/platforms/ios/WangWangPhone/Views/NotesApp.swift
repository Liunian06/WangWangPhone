import SwiftUI

struct NoteItem: Identifiable {
    let id: String
    var title: String
    var content: String
    var date: String
}

struct NotesAppView: View {
    @Binding var isPresented: Bool
    @State private var notes = [
        NoteItem(id: "1", title: "购物清单", content: "牛奶, 面包, 鸡蛋, 苹果, 香蕉", date: "昨天"),
        NoteItem(id: "2", title: "项目灵感", content: "开发一个跨平台的手机系统界面\n\n核心功能：\n- 虚拟桌面\n- 应用模拟\n- 聊天系统", date: "14:20"),
        NoteItem(id: "3", title: "会议记录", content: "讨论关于 MVP 版本的发布计划\n\n要点：\n1. 完成核心功能\n2. 性能优化\n3. UI打磨", date: "周一"),
        NoteItem(id: "4", title: "读书笔记", content: "《代码整洁之道》第一章读书笔记\n\n关键概念：命名要有意义，函数要短小精悍", date: "周日"),
        NoteItem(id: "5", title: "旅行计划", content: "五一假期旅行计划\n\n目的地：成都\n预算：5000元\n天数：5天", date: "上周")
    ]
    
    @State private var selectedNote: NoteItem? = nil
    @State private var editTitle: String = ""
    @State private var editContent: String = ""
    @State private var searchQuery: String = ""
    @State private var showDeleteAlert = false
    @State private var noteToDelete: NoteItem? = nil
    
    var filteredNotes: [NoteItem] {
        if searchQuery.isEmpty {
            return notes
        }
        return notes.filter {
            $0.title.localizedCaseInsensitiveContains(searchQuery) ||
            $0.content.localizedCaseInsensitiveContains(searchQuery)
        }
    }
    
    var body: some View {
        NavigationView {
            if selectedNote != nil {
                // Editor
                VStack(alignment: .leading) {
                    TextField("标题", text: $editTitle)
                        .font(.title)
                        .fontWeight(.bold)
                        .padding(.horizontal)
                    
                    TextEditor(text: $editContent)
                        .font(.body)
                        .padding(.horizontal)
                }
                .navigationTitle("")
                .navigationBarTitleDisplayMode(.inline)
                .toolbar {
                    ToolbarItem(placement: .navigationBarLeading) {
                        Button("‹ 备忘录") {
                            saveAndClose()
                        }
                    }
                    ToolbarItemGroup(placement: .navigationBarTrailing) {
                        // 删除按钮
                        Button(action: {
                            let noteId = selectedNote?.id ?? ""
                            notes.removeAll { $0.id == noteId }
                            selectedNote = nil
                        }) {
                            Image(systemName: "trash")
                                .foregroundColor(.red)
                        }
                        
                        Button("完成") {
                            saveAndClose()
                        }
                        .fontWeight(.bold)
                    }
                }
            } else {
                // List
                VStack(spacing: 0) {
                    // 搜索框
                    HStack(spacing: 8) {
                        Image(systemName: "magnifyingglass")
                            .foregroundColor(.gray)
                        TextField("搜索", text: $searchQuery)
                            .font(.callout)
                    }
                    .padding(10)
                    .background(Color(.systemGray5))
                    .cornerRadius(10)
                    .padding(.horizontal)
                    .padding(.bottom, 4)
                    
                    // 笔记数量
                    Text("\(notes.count) 个备忘录")
                        .font(.caption)
                        .foregroundColor(.gray)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(.horizontal, 24)
                        .padding(.bottom, 4)
                    
                    if filteredNotes.isEmpty {
                        VStack(spacing: 8) {
                            Spacer()
                            Text("📝").font(.system(size: 40))
                            Text(searchQuery.isEmpty ? "没有备忘录" : "没有找到匹配的备忘录")
                                .foregroundColor(.gray)
                            Spacer()
                        }
                    } else {
                        List {
                            Section {
                                ForEach(filteredNotes) { note in
                                    Button(action: {
                                        selectedNote = note
                                        editTitle = note.title
                                        editContent = note.content
                                    }) {
                                        HStack {
                                            VStack(alignment: .leading) {
                                                Text(note.title).font(.headline).foregroundColor(.primary)
                                                HStack {
                                                    Text(note.date).foregroundColor(.secondary)
                                                    Text(note.content.replacingOccurrences(of: "\n", with: " "))
                                                        .foregroundColor(.secondary)
                                                        .lineLimit(1)
                                                }
                                                .font(.subheadline)
                                            }
                                            Spacer()
                                        }
                                    }
                                    .swipeActions(edge: .trailing) {
                                        Button(role: .destructive) {
                                            noteToDelete = note
                                            showDeleteAlert = true
                                        } label: {
                                            Label("删除", systemImage: "trash")
                                        }
                                    }
                                }
                            }
                        }
                        .listStyle(InsetGroupedListStyle())
                    }
                    
                    // Bottom Bar
                    HStack {
                        Text("\(notes.count) 个备忘录")
                            .font(.caption)
                            .foregroundColor(.gray)
                        Spacer()
                        Button(action: {
                            let newNote = NoteItem(id: UUID().uuidString, title: "", content: "", date: "刚刚")
                            selectedNote = newNote
                            editTitle = ""
                            editContent = ""
                        }) {
                            Image(systemName: "square.and.pencil")
                                .font(.title2)
                        }
                    }
                    .padding()
                    .background(Color(.systemGray6).opacity(0.94))
                }
                .navigationTitle("备忘录")
                .toolbar {
                    ToolbarItem(placement: .navigationBarTrailing) {
                        Button("完成") { isPresented = false }
                    }
                }
                .alert(isPresented: $showDeleteAlert) {
                    Alert(
                        title: Text("删除备忘录"),
                        message: Text("确定要删除「\(noteToDelete?.title ?? "")」吗？此操作无法撤销。"),
                        primaryButton: .destructive(Text("删除")) {
                            if let note = noteToDelete {
                                notes.removeAll { $0.id == note.id }
                            }
                            noteToDelete = nil
                        },
                        secondaryButton: .cancel {
                            noteToDelete = nil
                        }
                    )
                }
            }
        }
    }
    
    private func saveAndClose() {
        if !editTitle.isEmpty || !editContent.isEmpty {
            if let index = notes.firstIndex(where: { $0.id == selectedNote?.id }) {
                notes[index].title = editTitle.isEmpty ? "无标题" : editTitle
                notes[index].content = editContent
            } else if let sn = selectedNote {
                let newNote = NoteItem(id: sn.id, title: editTitle.isEmpty ? "无标题" : editTitle, content: editContent, date: "刚刚")
                notes.insert(newNote, at: 0)
            }
        }
        selectedNote = nil
    }
}
