package com.WangWangPhone.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.WangWangPhone.core.PersonaDbHelper

@Composable
fun PersonaBuilderApp(onClose: () -> Unit) {
    BackHandler { onClose() }
    val isDark = isSystemInDarkTheme()
    val bg = if (isDark) Color(0xFF1C1C1E) else Color(0xFFF2F2F7)
    val card = if (isDark) Color(0xFF2C2C2E) else Color.White
    val txt = if (isDark) Color.White else Color.Black
    val context = LocalContext.current
    val dbHelper = remember { PersonaDbHelper(context) }
    val clipboardManager = LocalClipboardManager.current
    
    var name by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var personality by remember { mutableStateOf("") }
    var background by remember { mutableStateOf("") }
    var appearance by remember { mutableStateOf("") }
    var occupation by remember { mutableStateOf("") }
    var hobbies by remember { mutableStateOf("") }
    var relationships by remember { mutableStateOf("") }
    var goals by remember { mutableStateOf("") }
    var speechStyle by remember { mutableStateOf("") }
    var specialTraits by remember { mutableStateOf("") }
    
    var showImportDialog by remember { mutableStateOf(false) }
    var showSuccessMessage by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        val persona = dbHelper.getPersona()
        persona?.let {
            name = it.name
            gender = it.gender
            age = it.age
            personality = it.personality
            background = it.background
            appearance = it.appearance
            occupation = it.occupation
            hobbies = it.hobbies
            relationships = it.relationships
            goals = it.goals
            speechStyle = it.speechStyle
            specialTraits = it.specialTraits
        }
    }
    
    Column(modifier = Modifier.fillMaxSize().background(bg).statusBarsPadding()) {
        Box(
            modifier = Modifier.fillMaxWidth().height(56.dp).background(card).padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text("关闭", color = Color(0xFF007AFF), modifier = Modifier.clickable { onClose() })
            Text("神笔马良", modifier = Modifier.align(Alignment.Center), fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = txt)
        }
        
        LazyColumn(
            modifier = Modifier.fillMaxSize().weight(1f).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("角色人设设计", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = txt)
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            item { PersonaField("姓名", name, { name = it }, card, txt) }
            item { PersonaField("性别", gender, { gender = it }, card, txt) }
            item { PersonaField("年龄", age, { age = it }, card, txt) }
            item { PersonaField("性格特点", personality, { personality = it }, card, txt, multiline = true) }
            item { PersonaField("背景故事", background, { background = it }, card, txt, multiline = true) }
            item { PersonaField("外貌特征", appearance, { appearance = it }, card, txt, multiline = true) }
            item { PersonaField("职业", occupation, { occupation = it }, card, txt) }
            item { PersonaField("兴趣爱好", hobbies, { hobbies = it }, card, txt, multiline = true) }
            item { PersonaField("人际关系", relationships, { relationships = it }, card, txt, multiline = true) }
            item { PersonaField("目标与动机", goals, { goals = it }, card, txt, multiline = true) }
            item { PersonaField("说话风格", speechStyle, { speechStyle = it }, card, txt, multiline = true) }
            item { PersonaField("特殊技能或习惯", specialTraits, { specialTraits = it }, card, txt, multiline = true) }
            
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = {
                            val personaText = buildPersonaText(name, gender, age, personality, background, appearance, occupation, hobbies, relationships, goals, speechStyle, specialTraits)
                            clipboardManager.setText(AnnotatedString(personaText))
                            showSuccessMessage = true
                        },
                        modifier = Modifier.weight(1f).height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF34C759)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("复制人设", color = Color.White, fontSize = 16.sp)
                    }
                    
                    Button(
                        onClick = { showImportDialog = true },
                        modifier = Modifier.weight(1f).height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5856D6)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("导入联系人", color = Color.White, fontSize = 16.sp)
                    }
                }
            }
            
            item {
                Button(
                    onClick = {
                        dbHelper.savePersona(
                            com.WangWangPhone.core.PersonaRecord(
                                name = name,
                                gender = gender,
                                age = age,
                                personality = personality,
                                background = background,
                                appearance = appearance,
                                occupation = occupation,
                                hobbies = hobbies,
                                relationships = relationships,
                                goals = goals,
                                speechStyle = speechStyle,
                                specialTraits = specialTraits
                            )
                        )
                        showSuccessMessage = true
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("保存人设", color = Color.White, fontSize = 16.sp)
                }
            }
            
            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }
    
    if (showImportDialog) {
        ImportContactDialog(
            onDismiss = { showImportDialog = false },
            onImport = { contactText ->
                parseContactToPersona(contactText)?.let { parsed ->
                    name = parsed["name"] ?: name
                    gender = parsed["gender"] ?: gender
                    age = parsed["age"] ?: age
                    personality = parsed["personality"] ?: personality
                    background = parsed["background"] ?: background
                    appearance = parsed["appearance"] ?: appearance
                    occupation = parsed["occupation"] ?: occupation
                    hobbies = parsed["hobbies"] ?: hobbies
                    relationships = parsed["relationships"] ?: relationships
                    goals = parsed["goals"] ?: goals
                    speechStyle = parsed["speechStyle"] ?: speechStyle
                    specialTraits = parsed["specialTraits"] ?: specialTraits
                    showImportDialog = false
                    showSuccessMessage = true
                }
            }
        )
    }
    
    if (showSuccessMessage) {
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(2000)
            showSuccessMessage = false
        }
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.8f))
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Text("操作成功", color = Color.White, fontSize = 16.sp)
            }
        }
    }
}

@Composable
fun PersonaField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    cardColor: Color,
    textColor: Color,
    multiline: Boolean = false
) {
    Column {
        Text(label, fontSize = 14.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 4.dp))
        TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth().then(if (multiline) Modifier.height(120.dp) else Modifier),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = cardColor,
                unfocusedContainerColor = cardColor,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor = textColor,
                unfocusedTextColor = textColor
            ),
            shape = RoundedCornerShape(10.dp),
            maxLines = if (multiline) 5 else 1
        )
    }
}

@Composable
fun ImportContactDialog(onDismiss: () -> Unit, onImport: (String) -> Unit) {
    val clipboardManager = LocalClipboardManager.current
    var importText by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("导入联系人") },
        text = {
            Column {
                Text("粘贴联系人信息，系统将自动解析", fontSize = 14.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = importText,
                    onValueChange = { importText = it },
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    placeholder = { Text("粘贴联系人信息...") }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onImport(importText) }) {
                Text("导入")
            }
        },
        dismissButton = {
            TextButton(onClick = {
                clipboardManager.getText()?.let { importText = it.text }
            }) {
                Text("粘贴")
            }
        }
    )
}

fun buildPersonaText(
    name: String, gender: String, age: String, personality: String,
    background: String, appearance: String, occupation: String, hobbies: String,
    relationships: String, goals: String, speechStyle: String, specialTraits: String
): String {
    return buildString {
        if (name.isNotBlank()) appendLine("姓名：$name")
        if (gender.isNotBlank()) appendLine("性别：$gender")
        if (age.isNotBlank()) appendLine("年龄：$age")
        if (personality.isNotBlank()) appendLine("性格特点：$personality")
        if (background.isNotBlank()) appendLine("背景故事：$background")
        if (appearance.isNotBlank()) appendLine("外貌特征：$appearance")
        if (occupation.isNotBlank()) appendLine("职业：$occupation")
        if (hobbies.isNotBlank()) appendLine("兴趣爱好：$hobbies")
        if (relationships.isNotBlank()) appendLine("人际关系：$relationships")
        if (goals.isNotBlank()) appendLine("目标与动机：$goals")
        if (speechStyle.isNotBlank()) appendLine("说话风格：$speechStyle")
        if (specialTraits.isNotBlank()) appendLine("特殊技能或习惯：$specialTraits")
    }.trim()
}

fun parseContactToPersona(text: String): Map<String, String>? {
    if (text.isBlank()) return null
    val result = mutableMapOf<String, String>()
    val lines = text.lines()
    
    for (line in lines) {
        when {
            line.startsWith("姓名：") || line.startsWith("姓名:") -> result["name"] = line.substringAfter("：").substringAfter(":")
            line.startsWith("性别：") || line.startsWith("性别:") -> result["gender"] = line.substringAfter("：").substringAfter(":")
            line.startsWith("年龄：") || line.startsWith("年龄:") -> result["age"] = line.substringAfter("：").substringAfter(":")
            line.startsWith("性格特点：") || line.startsWith("性格特点:") -> result["personality"] = line.substringAfter("：").substringAfter(":")
            line.startsWith("背景故事：") || line.startsWith("背景故事:") -> result["background"] = line.substringAfter("：").substringAfter(":")
            line.startsWith("外貌特征：") || line.startsWith("外貌特征:") -> result["appearance"] = line.substringAfter("：").substringAfter(":")
            line.startsWith("职业：") || line.startsWith("职业:") -> result["occupation"] = line.substringAfter("：").substringAfter(":")
            line.startsWith("兴趣爱好：") || line.startsWith("兴趣爱好:") -> result["hobbies"] = line.substringAfter("：").substringAfter(":")
            line.startsWith("人际关系：") || line.startsWith("人际关系:") -> result["relationships"] = line.substringAfter("：").substringAfter(":")
            line.startsWith("目标与动机：") || line.startsWith("目标与动机:") -> result["goals"] = line.substringAfter("：").substringAfter(":")
            line.startsWith("说话风格：") || line.startsWith("说话风格:") -> result["speechStyle"] = line.substringAfter("：").substringAfter(":")
            line.startsWith("特殊技能或习惯：") || line.startsWith("特殊技能或习惯:") -> result["specialTraits"] = line.substringAfter("：").substringAfter(":")
        }
    }
    
    return if (result.isNotEmpty()) result else null
}
