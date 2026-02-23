package com.WangWangPhone.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class Note(val id: String, val title: String, val content: String, val date: String)

@Composable
fun NotesAppScreen(onClose: () -> Unit) {
    var notes by remember { mutableStateOf(listOf(
        Note("1", "购物清单", "牛奶, 面包, 鸡蛋, 苹果, 香蕉", "昨天"),
        Note("2", "项目灵感", "开发一个跨平台的手机系统界面\n\n核心功能：\n- 虚拟桌面\n- 应用模拟\n- 聊天系统", "14:20"),
        Note("3", "会议记录", "讨论关于 MVP 版本的发布计划\n\n要点：\n1. 完成核心功能\n2. 性能优化\n3. UI打磨", "周一"),
        Note("4", "读书笔记", "《代码整洁之道》第一章读书笔记\n\n关键概念：命名要有意义，函数要短小精悍", "周日"),
        Note("5", "旅行计划", "五一假期旅行计划\n\n目的地：成都\n预算：5000元\n天数：5天", "上周")
    )) }

    var selectedNote by remember { mutableStateOf<Note?>(null) }
    var editTitle by remember { mutableStateOf("") }
    var editContent by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var showDeleteConfirm by remember { mutableStateOf<Note?>(null) }

    BackHandler {
        if (selectedNote != null) {
            // 保存并返回列表
            if (editTitle.isNotEmpty() || editContent.isNotEmpty()) {
                val updatedNotes = notes.toMutableList()
                val index = updatedNotes.indexOfFirst { it.id == selectedNote!!.id }
                val newNote = selectedNote!!.copy(
                    title = if (editTitle.isEmpty()) "无标题" else editTitle,
                    content = editContent
                )
                if (index != -1) {
                    updatedNotes[index] = newNote
                } else {
                    updatedNotes.add(0, newNote)
                }
                notes = updatedNotes
            }
            selectedNote = null
        } else {
            onClose()
        }
    }

    // 删除确认对话框
    showDeleteConfirm?.let { noteToDelete ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("删除备忘录") },
            text = { Text("确定要删除「${noteToDelete.title}」吗？此操作无法撤销。") },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        notes = notes.filter { it.id != noteToDelete.id }
                        showDeleteConfirm = null
                    }
                ) { Text("删除", color = Color.Red) }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(
                    onClick = { showDeleteConfirm = null }
                ) { Text("取消") }
            }
        )
    }

    if (selectedNote == null) {
        // Notes List
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF2F2F7))
                .statusBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("备忘录", fontSize = 34.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                Text("完成", color = Color(0xFF007AFF), modifier = Modifier.clickable { onClose() }, fontSize = 17.sp)
            }

            // 搜索框
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 12.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFE5E5EA))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🔍", fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextStyle(fontSize = 16.sp, color = Color.Black),
                        singleLine = true,
                        decorationBox = { innerTextField ->
                            if (searchQuery.isEmpty()) {
                                Text("搜索", fontSize = 16.sp, color = Color.Gray)
                            }
                            innerTextField()
                        }
                    )
                }
            }

            // 笔记数量统计
            Text(
                "${notes.size} 个备忘录",
                modifier = Modifier.padding(horizontal = 26.dp, vertical = 4.dp),
                fontSize = 13.sp,
                color = Color.Gray
            )

            // 过滤笔记
            val filteredNotes = if (searchQuery.isEmpty()) notes else {
                notes.filter {
                    it.title.contains(searchQuery, ignoreCase = true) ||
                    it.content.contains(searchQuery, ignoreCase = true)
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White)
            ) {
                if (filteredNotes.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("📝", fontSize = 40.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            if (searchQuery.isEmpty()) "没有备忘录" else "没有找到匹配的备忘录",
                            color = Color.Gray,
                            fontSize = 16.sp
                        )
                    }
                } else {
                    LazyColumn {
                        items(filteredNotes, key = { it.id }) { note ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedNote = note
                                        editTitle = note.title
                                        editContent = note.content
                                    }
                                    .padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(note.title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(note.date, fontSize = 15.sp, color = Color.Gray)
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                note.content.replace("\n", " "),
                                                fontSize = 15.sp,
                                                color = Color.Gray,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                    // 删除按钮
                                    Text(
                                        "🗑️",
                                        fontSize = 18.sp,
                                        modifier = Modifier
                                            .clickable { showDeleteConfirm = note }
                                            .padding(start = 8.dp)
                                    )
                                }
                            }
                            Divider(
                                modifier = Modifier.padding(start = 16.dp),
                                color = Color(0xFFE5E5EA),
                                thickness = 0.5.dp
                            )
                        }
                    }
                }
            }

            // Bottom Bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF9F9F9).copy(alpha = 0.94f))
                    .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${notes.size} 个备忘录",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Text("📝", fontSize = 24.sp, modifier = Modifier.clickable {
                        val newNote = Note(java.util.UUID.randomUUID().toString(), "", "", "刚刚")
                        selectedNote = newNote
                        editTitle = ""
                        editContent = ""
                    })
                }
            }
        }
    } else {
        // Note Editor
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .statusBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("‹ 备忘录", color = Color(0xFF007AFF), fontSize = 17.sp, modifier = Modifier.clickable {
                    // Save and back
                    if (editTitle.isNotEmpty() || editContent.isNotEmpty()) {
                        val updatedNotes = notes.toMutableList()
                        val index = updatedNotes.indexOfFirst { it.id == selectedNote!!.id }
                        val newNote = selectedNote!!.copy(title = if (editTitle.isEmpty()) "无标题" else editTitle, content = editContent)
                        if (index != -1) {
                            updatedNotes[index] = newNote
                        } else {
                            updatedNotes.add(0, newNote)
                        }
                        notes = updatedNotes
                    }
                    selectedNote = null
                })
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    // 删除按钮
                    Text("🗑️", fontSize = 18.sp, modifier = Modifier.clickable {
                        val noteId = selectedNote!!.id
                        notes = notes.filter { it.id != noteId }
                        selectedNote = null
                    })
                    Text("完成", color = Color(0xFF007AFF), fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable {
                        if (editTitle.isNotEmpty() || editContent.isNotEmpty()) {
                            val updatedNotes = notes.toMutableList()
                            val index = updatedNotes.indexOfFirst { it.id == selectedNote!!.id }
                            val newNote = selectedNote!!.copy(title = if (editTitle.isEmpty()) "无标题" else editTitle, content = editContent)
                            if (index != -1) {
                                updatedNotes[index] = newNote
                            } else {
                                updatedNotes.add(0, newNote)
                            }
                            notes = updatedNotes
                        }
                        selectedNote = null
                    })
                }
            }

            BasicTextField(
                value = editTitle,
                onValueChange = { editTitle = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
                textStyle = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.Black),
                decorationBox = { innerTextField ->
                    if (editTitle.isEmpty()) Text("标题", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.LightGray)
                    innerTextField()
                }
            )

            BasicTextField(
                value = editContent,
                onValueChange = { editContent = it },
                modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                textStyle = TextStyle(fontSize = 17.sp, color = Color.Black),
                decorationBox = { innerTextField ->
                    if (editContent.isEmpty()) Text("开始输入...", fontSize = 17.sp, color = Color.LightGray)
                    innerTextField()
                }
            )
        }
    }
}
