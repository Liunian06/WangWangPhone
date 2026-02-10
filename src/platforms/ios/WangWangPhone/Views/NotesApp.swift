import SwiftUI

struct Note: Identifiable {
    let id: String
    var title: String
    var content: String
    var date: String
}

struct NotesAppView: View {
    @Binding var isPresented: Bool
    @State private var notes = [
        Note(id: "1", title: "购物清单", content: "牛奶, 面包, 鸡蛋", date: "昨天"),
        Note(id: "2", title: "项目灵感", content: "开发一个跨平台的手机系统界面", date: "14:20"),
        Note(id: "3", title: "会议记录", content: "讨论关于 MVP 版本的发布计划", date: "周一")
    ]
    
    @State private var selectedNote: Note? = nil
    @State private var editTitle: String = ""
    @State private var editContent: String = ""
    
    var body: some View {
        NavigationView {
            if let note = selectedNote {
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
                    ToolbarItem(placement: .navigationBarTrailing) {
                        Button("完成") {
                            saveAndClose()
                        }
                        .fontWeight(.bold)
                    }
                }
            } else {
                // List
                VStack(spacing: 0) {
                    List {
                        Section {
                            ForEach(notes) { note in
                                Button(action: {
                                    selectedNote = note
                                    editTitle = note.title
                                    editContent = note.content
                                }) {
                                    VStack(alignment: .leading) {
                                        Text(note.title).font(.headline).foregroundColor(.primary)
                                        HStack {
                                            Text(note.date).foregroundColor(.secondary)
                                            Text(note.content).foregroundColor(.secondary).lineLimit(1)
                                        }
                                        .font(.subheadline)
                                    }
                                }
                            }
                        }
                    }
                    .listStyle(InsetGroupedListStyle())
                    
                    // Bottom Bar
                    HStack {
                        Spacer()
                        Button(action: {
                            let newNote = Note(id: UUID().uuidString, title: "", content: "", date: "刚刚")
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
            }
        }
    }
    
    private func saveAndClose() {
        if !editTitle.isEmpty || !editContent.isEmpty {
            if let index = notes.firstIndex(where: { $0.id == selectedNote?.id }) {
                notes[index].title = editTitle.isEmpty ? "无标题" : editTitle
                notes[index].content = editContent
            } else if let sn = selectedNote {
                let newNote = Note(id: sn.id, title: editTitle.isEmpty ? "无标题" : editTitle, content: editContent, date: "刚刚")
                notes.insert(newNote, at: 0)
            }
        }
        selectedNote = nil
    }
}
