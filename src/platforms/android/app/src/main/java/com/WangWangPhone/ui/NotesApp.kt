package com.WangWangPhone.ui

import androidx.activity.compose.BackHandler
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
        Note("1", "购物清单", "牛奶, 面包, 鸡蛋", "昨天"),
        Note("2", "项目灵感", "开发一个跨平台的手机系统界面", "14:20"),
        Note("3", "会议记录", "讨论关于 MVP 版本的发布计划", "周一")
    )) }
    
    var selectedNote by remember { mutableStateOf<Note?>(null) }
    var editTitle by remember { mutableStateOf("") }
    var editContent by remember { mutableStateOf("") }

    BackHandler {
        if (selectedNote != null) {
            selectedNote = null
        } else {
            onClose()
        }
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

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White)
            ) {
                Column {
                    notes.forEachIndexed { index, note ->
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
                            Text(note.title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(note.date, fontSize = 15.sp, color = Color.Gray)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(note.content, fontSize = 15.sp, color = Color.Gray, maxLines = 1)
                            }
                        }
                        if (index < notes.size - 1) {
                            Divider(modifier = Modifier.padding(start = 16.dp), color = Color(0xFFE5E5EA), thickness = 0.5.dp)
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Bottom Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .background(Color(0xFFF9F9F9).copy(alpha = 0.94f))
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("📝", fontSize = 24.sp, modifier = Modifier.clickable {
                    val newNote = Note(java.util.UUID.randomUUID().toString(), "", "", "刚刚")
                    selectedNote = newNote
                    editTitle = ""
                    editContent = ""
                })
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
                Text("完成", color = Color(0xFF007AFF), fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { selectedNote = null })
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
