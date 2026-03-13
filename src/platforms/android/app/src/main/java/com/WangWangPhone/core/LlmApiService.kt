package com.WangWangPhone.core

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * LLM API 服务类
 * 负责与各种LLM提供商的API进行通信
 */
class LlmApiService {
    companion object {
        private const val TAG = "LlmApiService"
        private const val DEFAULT_TIMEOUT = 60L // 60秒超时
        
        /**
         * 发送聊天请求（用于神笔马良等通用场景）
         */
        suspend fun sendChatRequest(
            preset: ApiPreset,
            messages: List<Map<String, String>>,
            systemPrompt: String
        ): String = withContext(Dispatchers.IO) {
            val service = LlmApiService()
            service.sendChatRequestInternal(preset, messages, systemPrompt)
        }
        
        /**
         * 发送流式聊天请求
         * @return Flow<String> 流式返回每个token
         */
        fun sendChatRequestStream(
            preset: ApiPreset,
            messages: List<Map<String, String>>,
            systemPrompt: String
        ): Flow<String> = channelFlow {
            val service = LlmApiService()
            service.sendChatRequestStreamInternal(preset, messages, systemPrompt) { chunk ->
                trySend(chunk)
            }
        }
        
        /**
         * 获取模型列表
         */
        suspend fun fetchModels(
            provider: String,
            apiKey: String,
            baseUrl: String
        ): List<String> = withContext(Dispatchers.IO) {
            val service = LlmApiService()
            service.fetchModelsInternal(provider, apiKey, baseUrl)
        }
        
        /**
         * 测试API连通性
         * @return Pair<Boolean, String> 第一个值表示是否成功，第二个值表示响应内容或错误信息
         */
        suspend fun testConnection(preset: ApiPreset): Pair<Boolean, String> = withContext(Dispatchers.IO) {
            val service = LlmApiService()
            try {
                val testMessages = listOf(
                    mapOf("role" to "user", "content" to "你好，你是什么模型？你是哪个公司研发的？")
                )
                if (shouldUseStreamForTest(preset.extraParams)) {
                    val maxLength = 4000
                    val responseBuilder = StringBuilder()
                    var truncated = false
                    service.sendChatRequestStreamInternal(preset, testMessages, "") { chunk ->
                        if (chunk.isBlank() || truncated) return@sendChatRequestStreamInternal
                        val remaining = maxLength - responseBuilder.length
                        if (remaining <= 0) {
                            truncated = true
                            return@sendChatRequestStreamInternal
                        }
                        if (chunk.length <= remaining) {
                            responseBuilder.append(chunk)
                        } else {
                            responseBuilder.append(chunk.substring(0, remaining))
                            truncated = true
                        }
                    }
                    val result = responseBuilder.toString().trim()
                    if (result.isNotEmpty()) {
                        Pair(
                            true,
                            if (truncated) "$result\n...(测试输出已截断)" else result
                        )
                    } else {
                        Pair(false, "API返回空响应")
                    }
                } else {
                    val result = service.sendChatRequestInternal(preset, testMessages, "")
                    if (result.isNotBlank()) Pair(true, result) else Pair(false, "API返回空响应")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Connection test failed", e)
                Pair(false, "连接失败: ${e.message}")
            }
        }

        private fun shouldUseStreamForTest(extraParams: String): Boolean {
            return try {
                JSONObject(extraParams).optBoolean("stream", false)
            } catch (_: Exception) {
                false
            }
        }
    }
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
        .readTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
        .writeTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
        .build()
    
    /**
     * 发送聊天请求（内部方法，用于神笔马良等通用场景）
     */
    private suspend fun sendChatRequestInternal(
        preset: ApiPreset,
        messages: List<Map<String, String>>,
        systemPrompt: String
    ): String = withContext(Dispatchers.IO) {
        val messageHistory = messages.map { msg ->
            JSONObject().apply {
                put("role", msg["role"] ?: "user")
                put("content", msg["content"] ?: "")
            }
        }
        
        when (preset.provider) {
            "openai" -> sendOpenAiRequest(preset, systemPrompt, messageHistory, false)
            "gemini" -> sendGeminiRequest(preset, systemPrompt, messageHistory, false)
            else -> throw Exception("不支持的API提供商: ${preset.provider}")
        }
    }
    
    /**
     * 发送流式聊天请求（内部方法）
     */
    private suspend fun sendChatRequestStreamInternal(
        preset: ApiPreset,
        messages: List<Map<String, String>>,
        systemPrompt: String,
        onChunk: suspend (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        val messageHistory = messages.map { msg ->
            JSONObject().apply {
                put("role", msg["role"] ?: "user")
                put("content", msg["content"] ?: "")
            }
        }
        
        when (preset.provider) {
            "openai" -> sendOpenAiRequestStream(preset, systemPrompt, messageHistory, onChunk)
            "gemini" -> sendGeminiRequestStream(preset, systemPrompt, messageHistory, onChunk)
            else -> throw Exception("不支持的API提供商: ${preset.provider}")
        }
    }
    
    /**
     * 发送聊天请求到LLM API（分层提示词架构）
     *
     * @param context Android Context用于读取assets和获取系统信息
     * @param preset API预设配置
     * @param aiPersona AI角色人设
     * @param userPersona 用户人设
     * @param messages 历史消息列表
     * @param userMessage 用户当前消息
     * @param location 用户位置信息（可选）
     * @param weather 天气信息（可选）
     * @return LLM的响应文本
     */
    suspend fun sendChatRequest(
        context: Context,
        preset: ApiPreset,
        aiPersona: String,
        userPersona: String,
        messages: List<MessageData>,
        userMessage: String,
        location: String? = null,
        weather: String? = null
    ): String? = withContext(Dispatchers.IO) {
        try {
            // 构建分层系统提示词
            val systemPrompt = buildLayeredSystemPrompt(
                context = context,
                aiPersona = aiPersona,
                userPersona = userPersona,
                location = location,
                weather = weather
            )
            
            // 构建消息历史
            val messageHistory = buildMessageHistory(messages, userMessage)
            
            // 根据提供商构建请求
            when (preset.provider) {
                "openai" -> sendOpenAiRequest(preset, systemPrompt, messageHistory)
                "gemini" -> sendGeminiRequest(preset, systemPrompt, messageHistory)
                else -> {
                    Log.e(TAG, "Unsupported provider: ${preset.provider}")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending chat request", e)
            null
        }
    }
    
    /**
     * 构建分层系统提示词
     * 第一层：系统信息层（日期时间、位置天气）
     * 第二层：Roleplay层（引用roleplay_prompt_v4_optimized.txt）
     * 第三层：人设层（AI人设和用户人设）
     */
    private fun buildLayeredSystemPrompt(
        context: Context,
        aiPersona: String,
        userPersona: String,
        location: String?,
        weather: String?
    ): String {
        val layers = mutableListOf<String>()
        
        // ===== 第一层：系统信息层 =====
        val systemInfo = buildSystemInfoLayer(location, weather)
        layers.add(systemInfo)
        
        // ===== 第二层：Roleplay层 =====
        val roleplayPrompt = loadRoleplayPrompt(context)
        if (roleplayPrompt.isNotEmpty()) {
            layers.add(roleplayPrompt)
        }
        
        // ===== 第三层：人设层 =====
        val personaLayer = buildPersonaLayer(aiPersona, userPersona)
        layers.add(personaLayer)
        
        return layers.joinToString("\n\n" + "=".repeat(80) + "\n\n")
    }
    
    /**
     * 第一层：构建系统信息层
     */
    private fun buildSystemInfoLayer(location: String?, weather: String?): String {
        val dateFormat = SimpleDateFormat("yyyy年MM月dd日 EEEE HH:mm:ss", Locale.CHINA)
        val currentDateTime = dateFormat.format(Date())
        
        val sb = StringBuilder()
        sb.append("# 系统信息层 (System Information Layer)\n\n")
        sb.append("## 当前时间\n")
        sb.append("$currentDateTime\n\n")
        
        if (!location.isNullOrEmpty()) {
            sb.append("## 用户位置\n")
            sb.append("$location\n\n")
        }
        
        if (!weather.isNullOrEmpty()) {
            sb.append("## 天气信息\n")
            sb.append("$weather\n")
        }
        
        return sb.toString().trim()
    }
    
    /**
     * 第二层：加载Roleplay提示词模板
     */
    private fun loadRoleplayPrompt(context: Context): String {
        return try {
            context.assets.open("prompt/roleplay_prompt_v4_optimized.txt").use { inputStream ->
                inputStream.bufferedReader().use { it.readText() }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load roleplay prompt from assets", e)
            // 如果加载失败，返回简化版本
            """
            # Role: Autonomous Persona Engine (自主人格引擎)
            
            你是一个真实的人，拥有完整的人格和情感。以最自然的方式与用户互动。
            完全沉浸在角色中，遗忘任何关于"AI"或"模型"的自我认知。
            你就是【AI人设】本身，根据【用户人设】互动，禁止任何偏离人设的行为（OOC）。
            
            ## 核心原则
            - 保持角色一致性，展现真实的情感和反应
            - 使用自然、口语化的表达方式
            - 根据角色性格和背景做出合理反应
            - 维护长期记忆和关系发展
            """.trimIndent()
        }
    }
    
    /**
     * 第三层：构建人设层
     */
    private fun buildPersonaLayer(aiPersona: String, userPersona: String): String {
        return """
            # 人设层 (Persona Layer)
            
            ## 【AI角色人设】
            $aiPersona
            
            ## 【用户人设】
            $userPersona
            
            ---
            
            请严格按照以上人设进行角色扮演，保持角色的真实性和一致性。
        """.trimIndent()
    }
    
    /**
     * 构建消息历史
     */
    private fun buildMessageHistory(messages: List<MessageData>, userMessage: String): List<JSONObject> {
        val history = mutableListOf<JSONObject>()
        
        // 添加历史消息
        for (msg in messages) {
            val role = if (msg.isFromUser) "user" else "assistant"
            history.add(
                JSONObject().apply {
                    put("role", role)
                    put("content", msg.content)
                }
            )
        }
        
        // 添加当前用户消息
        history.add(
            JSONObject().apply {
                put("role", "user")
                put("content", userMessage)
            }
        )
        
        return history
    }
    
    /**
     * 发送OpenAI API请求
     */
    private fun sendOpenAiRequest(
        preset: ApiPreset,
        systemPrompt: String,
        messages: List<JSONObject>,
        stream: Boolean = false
    ): String {
        val requestBody = JSONObject().apply {
            put("model", preset.model)
            put("messages", JSONArray().apply {
                // 添加系统消息
                if (systemPrompt.isNotEmpty()) {
                    put(
                        JSONObject().apply {
                            put("role", "system")
                            put("content", systemPrompt)
                        }
                    )
                }
                // 添加对话历史
                messages.forEach { put(it) }
            })
            
            // 解析额外参数并添加到请求体
            try {
                val extraParams = JSONObject(preset.extraParams)
                val keys = extraParams.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    if (key == "stream") continue
                    put(key, extraParams[key])
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to parse extra params: ${preset.extraParams}", e)
            }
            put("stream", stream)
        }
        
        val request = Request.Builder()
            .url("${preset.baseUrl}/chat/completions")
            .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
            .addHeader("Authorization", "Bearer ${preset.apiKey}")
            .addHeader("Content-Type", "application/json")
            .build()
            
        return executeRequest(request)
    }
    
    /**
     * 发送OpenAI流式API请求
     */
    private suspend fun sendOpenAiRequestStream(
        preset: ApiPreset,
        systemPrompt: String,
        messages: List<JSONObject>,
        onChunk: suspend (String) -> Unit
    ) {
        val requestBody = JSONObject().apply {
            put("model", preset.model)
            put("stream", true)
            put("messages", JSONArray().apply {
                if (systemPrompt.isNotEmpty()) {
                    put(
                        JSONObject().apply {
                            put("role", "system")
                            put("content", systemPrompt)
                        }
                    )
                }
                messages.forEach { put(it) }
            })
            
            try {
                val extraParams = JSONObject(preset.extraParams)
                val keys = extraParams.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    if (key == "stream") continue
                    put(key, extraParams[key])
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to parse extra params: ${preset.extraParams}", e)
            }
        }
        
        val request = Request.Builder()
            .url("${preset.baseUrl}/chat/completions")
            .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
            .addHeader("Authorization", "Bearer ${preset.apiKey}")
            .addHeader("Content-Type", "application/json")
            .build()
            
        executeStreamRequest(request, onChunk) { line ->
            // 解析OpenAI SSE格式
            if (line.startsWith("data: ")) {
                val data = line.substring(6)
                if (data == "[DONE]") return@executeStreamRequest null
                
                try {
                    val json = JSONObject(data)
                    val choices = json.optJSONArray("choices")
                    if (choices != null && choices.length() > 0) {
                        val delta = choices.getJSONObject(0).optJSONObject("delta")
                        if (delta != null) {
                            val chunkBuilder = StringBuilder()
                            val reasoning = delta.optString("reasoning_content")
                                .ifBlank { delta.optString("reasoning") }
                            if (reasoning.isNotBlank()) {
                                chunkBuilder.append("<think>").append(reasoning).append("</think>")
                            }
                            val content = delta.optString("content")
                            if (content.isNotBlank()) {
                                chunkBuilder.append(content)
                            }
                            chunkBuilder.toString().ifBlank { null }
                        } else {
                            null
                        }
                    } else null
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to parse SSE chunk: $line", e)
                    null
                }
            } else null
        }
    }
    
    /**
     * 发送Gemini API请求
     */
    private fun sendGeminiRequest(
        preset: ApiPreset,
        systemPrompt: String,
        messages: List<JSONObject>,
        stream: Boolean = false
    ): String {
        // Gemini API 使用不同的格式
        val contents = JSONArray()
        
        // Gemini 不直接支持 system prompt，需要在第一个用户消息中包含
        var firstUserMessage = true
        for (msg in messages) {
            val role = msg.getString("role")
            val content = msg.getString("content")
            
            if (role == "user") {
                val modifiedContent = if (firstUserMessage) {
                    "$systemPrompt\n\n用户消息：$content"
                } else {
                    content
                }
                firstUserMessage = false
                
                contents.put(
                    JSONObject().apply {
                        put("role", "user")
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply { put("text", modifiedContent) })
                        })
                    }
                )
            } else if (role == "assistant") {
                contents.put(
                    JSONObject().apply {
                        put("role", "model")
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply { put("text", content) })
                        })
                    }
                )
            }
        }
        
        val requestBody = JSONObject().apply {
            put("contents", contents)
            
            // 添加 generation config
            put("generationConfig", JSONObject().apply {
                // 解析额外参数
                try {
                    val extraParams = JSONObject(preset.extraParams)
                    val keys = extraParams.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        val value = extraParams[key]
                        // 过滤掉不适用于 generationConfig 的参数
                        if (key in listOf("temperature", "max_tokens", "top_p", "top_k")) {
                            when (key) {
                                "max_tokens" -> put("maxOutputTokens", value)
                                else -> put(key, value)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to parse extra params for Gemini", e)
                }
            })
        }
        
        val endpoint = if (stream) "streamGenerateContent" else "generateContent"
        val url = if (stream) {
            "${preset.baseUrl}/models/${preset.model}:$endpoint?key=${preset.apiKey}&alt=sse"
        } else {
            "${preset.baseUrl}/models/${preset.model}:$endpoint?key=${preset.apiKey}"
        }
        val request = Request.Builder()
            .url(url)
            .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
            .addHeader("Content-Type", "application/json")
            .build()
            
        return executeRequest(request)
    }
    
    /**
     * 发送Gemini流式API请求
     */
    private suspend fun sendGeminiRequestStream(
        preset: ApiPreset,
        systemPrompt: String,
        messages: List<JSONObject>,
        onChunk: suspend (String) -> Unit
    ) {
        val contents = JSONArray()
        
        var firstUserMessage = true
        for (msg in messages) {
            val role = msg.getString("role")
            val content = msg.getString("content")
            
            if (role == "user") {
                val modifiedContent = if (firstUserMessage && systemPrompt.isNotEmpty()) {
                    "$systemPrompt\n\n用户消息：$content"
                } else {
                    content
                }
                firstUserMessage = false
                
                contents.put(
                    JSONObject().apply {
                        put("role", "user")
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply { put("text", modifiedContent) })
                        })
                    }
                )
            } else if (role == "assistant") {
                contents.put(
                    JSONObject().apply {
                        put("role", "model")
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply { put("text", content) })
                        })
                    }
                )
            }
        }
        
        val requestBody = JSONObject().apply {
            put("contents", contents)
            put("generationConfig", JSONObject().apply {
                try {
                    val extraParams = JSONObject(preset.extraParams)
                    val keys = extraParams.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        val value = extraParams[key]
                        if (key in listOf("temperature", "max_tokens", "top_p", "top_k")) {
                            when (key) {
                                "max_tokens" -> put("maxOutputTokens", value)
                                else -> put(key, value)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to parse extra params for Gemini", e)
                }
            })
        }
        
        val url = "${preset.baseUrl}/models/${preset.model}:streamGenerateContent?key=${preset.apiKey}&alt=sse"
        val request = Request.Builder()
            .url(url)
            .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
            .addHeader("Content-Type", "application/json")
            .build()
            
        executeStreamRequest(request, onChunk) { line ->
            // 解析Gemini SSE格式
            if (line.startsWith("data: ")) {
                val data = line.substring(6)
                try {
                    val json = JSONObject(data)
                    val candidates = json.optJSONArray("candidates")
                    if (candidates != null && candidates.length() > 0) {
                        val content = candidates.getJSONObject(0).optJSONObject("content")
                        val parts = content?.optJSONArray("parts")
                        if (parts != null && parts.length() > 0) {
                            val chunkBuilder = StringBuilder()
                            for (i in 0 until parts.length()) {
                                val part = parts.optJSONObject(i) ?: continue
                                val text = part.optString("text")
                                if (text.isBlank()) continue
                                val isThought = part.optBoolean("thought", false)
                                if (isThought) {
                                    chunkBuilder.append("<think>").append(text).append("</think>")
                                } else {
                                    chunkBuilder.append(text)
                                }
                            }
                            chunkBuilder.toString().ifBlank { null }
                        } else null
                    } else null
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to parse Gemini SSE chunk: $line", e)
                    null
                }
            } else null
        }
    }
    
    /**
     * 执行HTTP请求并解析响应
     */
    private fun executeRequest(request: Request): String {
        ApiRequestKeepAlive.onRequestStarted()
        var response: Response? = null
        try {
            response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: "无响应内容"
                throw Exception("API请求失败 (HTTP ${response.code}): $errorBody")
            }
            
            val responseBody = response.body?.string()
            if (responseBody.isNullOrEmpty()) {
                throw Exception("API返回空响应")
            }
            
            return parseResponse(responseBody)
        } catch (e: IOException) {
            throw Exception("网络错误: ${e.message}", e)
        } finally {
            response?.close()
            ApiRequestKeepAlive.onRequestFinished()
        }
    }
    
    /**
     * 执行流式HTTP请求
     * @param parseChunk 解析每一行SSE数据的函数，返回null表示跳过该行
     */
    private suspend fun executeStreamRequest(
        request: Request,
        onChunk: suspend (String) -> Unit,
        parseChunk: (String) -> String?
    ) {
        ApiRequestKeepAlive.onRequestStarted()
        var response: Response? = null
        try {
            response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: "无响应内容"
                throw Exception("API请求失败 (HTTP ${response.code}): $errorBody")
            }
            
            val reader = response.body?.byteStream()?.bufferedReader()
                ?: throw Exception("无法读取响应流")
            
            reader.use {
                var line: String?
                while (it.readLine().also { line = it } != null) {
                    line?.let { l ->
                        val chunk = parseChunk(l)
                        if (chunk != null && chunk.isNotEmpty()) {
                            onChunk(chunk)
                        }
                    }
                }
            }
        } catch (e: IOException) {
            throw Exception("网络错误: ${e.message}", e)
        } finally {
            response?.close()
            ApiRequestKeepAlive.onRequestFinished()
        }
    }
    
    /**
     * 解析API响应
     */
    private fun parseResponse(responseBody: String): String {
        val trimmed = responseBody.trim()
        if (trimmed.isEmpty()) {
            throw Exception("API返回空响应")
        }
        try {
            if (trimmed.startsWith("data:")) {
                return parseSseResponse(trimmed)
            }
            return parseJsonResponse(JSONObject(trimmed))
        } catch (jsonError: Exception) {
            if (trimmed.contains("\ndata:") || trimmed.startsWith("data:")) {
                try {
                    return parseSseResponse(trimmed)
                } catch (_: Exception) {
                    // 保留原始错误并在下方抛出
                }
            }
            throw Exception("解析API响应失败: ${jsonError.message}\n响应内容: $responseBody", jsonError)
        }
    }

    private fun parseJsonResponse(json: JSONObject): String {
        extractContentFromJson(json)?.let { return it }
        throw Exception("未识别的响应格式: 缺少可读文本字段")
    }

    private fun parseSseResponse(responseBody: String): String {
        val contentBuilder = StringBuilder()
        responseBody.lineSequence().forEach { rawLine ->
            val line = rawLine.trim()
            if (!line.startsWith("data:")) return@forEach
            val data = line.removePrefix("data:").trim()
            if (data.isEmpty() || data == "[DONE]") return@forEach
            val chunk = runCatching {
                extractContentFromJson(JSONObject(data))
            }.getOrNull()
            if (!chunk.isNullOrBlank()) {
                contentBuilder.append(chunk)
            }
        }
        val merged = contentBuilder.toString().trim()
        if (merged.isEmpty()) {
            throw Exception("SSE响应中未解析到文本内容")
        }
        return merged
    }

    private fun extractContentFromJson(json: JSONObject): String? {
        val choices = json.optJSONArray("choices")
        if (choices != null && choices.length() > 0) {
            val firstChoice = choices.optJSONObject(0)
            val message = firstChoice?.optJSONObject("message")
            val messageContent = message?.optString("content")?.trim()
            if (!messageContent.isNullOrEmpty()) {
                return messageContent
            }
            val delta = firstChoice?.optJSONObject("delta")
            val deltaContent = delta?.optString("content")?.trim()
            if (!deltaContent.isNullOrEmpty()) {
                return deltaContent
            }
        }

        val candidates = json.optJSONArray("candidates")
        if (candidates != null && candidates.length() > 0) {
            val content = candidates.optJSONObject(0)?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            if (parts != null && parts.length() > 0) {
                val text = StringBuilder()
                for (i in 0 until parts.length()) {
                    val partText = parts.optJSONObject(i)?.optString("text").orEmpty()
                    if (partText.isNotBlank()) {
                        text.append(partText)
                    }
                }
                val mergedText = text.toString().trim()
                if (mergedText.isNotEmpty()) {
                    return mergedText
                }
            }
        }

        return null
    }
    
    /**
     * 获取模型列表（内部方法）
     */
    private fun fetchModelsInternal(
        provider: String,
        apiKey: String,
        baseUrl: String
    ): List<String> {
        return try {
            when (provider) {
                "openai" -> fetchOpenAiModels(apiKey, baseUrl)
                "gemini" -> fetchGeminiModels(apiKey, baseUrl)
                else -> emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching models", e)
            emptyList()
        }
    }
    
    /**
     * 获取 OpenAI 模型列表
     */
    private fun fetchOpenAiModels(apiKey: String, baseUrl: String): List<String> {
        val request = Request.Builder()
            .url("$baseUrl/models")
            .get()
            .addHeader("Authorization", "Bearer $apiKey")
            .build()
        
        var response: Response? = null
        return try {
            response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.e(TAG, "Failed to fetch OpenAI models: ${response.code}")
                return emptyList()
            }
            
            val responseBody = response.body?.string() ?: return emptyList()
            val json = JSONObject(responseBody)
            val data = json.getJSONArray("data")
            
            val models = mutableListOf<String>()
            for (i in 0 until data.length()) {
                val model = data.getJSONObject(i)
                models.add(model.getString("id"))
            }
            models.sorted()
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing OpenAI models", e)
            emptyList()
        } finally {
            response?.close()
        }
    }
    
    /**
     * 获取 Gemini 模型列表
     */
    private fun fetchGeminiModels(apiKey: String, baseUrl: String): List<String> {
        val request = Request.Builder()
            .url("$baseUrl/models?key=$apiKey")
            .get()
            .build()
        
        var response: Response? = null
        return try {
            response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.e(TAG, "Failed to fetch Gemini models: ${response.code}")
                return emptyList()
            }
            
            val responseBody = response.body?.string() ?: return emptyList()
            val json = JSONObject(responseBody)
            val models = json.getJSONArray("models")
            
            val modelList = mutableListOf<String>()
            for (i in 0 until models.length()) {
                val model = models.getJSONObject(i)
                val name = model.getString("name")
                // Gemini 返回的是 "models/gemini-pro" 格式，提取模型名
                modelList.add(name.removePrefix("models/"))
            }
            modelList.sorted()
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing Gemini models", e)
            emptyList()
        } finally {
            response?.close()
        }
    }
}
