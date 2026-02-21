package com.WangWangPhone.core

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
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
            val result = service.sendChatRequestInternal(preset, messages, systemPrompt)
            result ?: throw Exception("API 返回空响应")
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
    ): String? = withContext(Dispatchers.IO) {
        try {
            val messageHistory = messages.map { msg ->
                JSONObject().apply {
                    put("role", msg["role"] ?: "user")
                    put("content", msg["content"] ?: "")
                }
            }
            
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
     * 发送聊天请求到LLM API
     *
     * @param preset API预设配置
     * @param aiPersona AI角色人设
     * @param userPersona 用户人设
     * @param messages 历史消息列表
     * @param userMessage 用户当前消息
     * @return LLM的响应文本
     */
    suspend fun sendChatRequest(
        preset: ApiPreset,
        aiPersona: String,
        userPersona: String,
        messages: List<MessageData>,
        userMessage: String
    ): String? = withContext(Dispatchers.IO) {
        try {
            // 构建系统提示词，包含角色人设和用户人设
            val systemPrompt = buildSystemPrompt(aiPersona, userPersona)
            
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
     * 构建系统提示词
     */
    private fun buildSystemPrompt(aiPersona: String, userPersona: String): String {
        return """
            你正在扮演一个角色进行对话。以下是角色设定：
            
            【AI角色人设】
            $aiPersona
            
            【用户人设】
            $userPersona
            
            请根据以上人设进行沉浸式角色扮演对话，保持角色的一致性和个性特征。
            回答要自然、生动，符合角色的性格和背景。
            不要提及你是AI或大语言模型，完全沉浸在角色中。
            如果用户的问题超出你的知识范围，请基于角色设定合理回应。
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
        messages: List<JSONObject>
    ): String? {
        val requestBody = JSONObject().apply {
            put("model", preset.model)
            put("messages", JSONArray().apply {
                // 添加系统消息
                put(
                    JSONObject().apply {
                        put("role", "system")
                        put("content", systemPrompt)
                    }
                )
                // 添加对话历史
                messages.forEach { put(it) }
            })
            
            // 解析额外参数并添加到请求体
            try {
                val extraParams = JSONObject(preset.extraParams)
                val keys = extraParams.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
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
            
        return executeRequest(request)
    }
    
    /**
     * 发送Gemini API请求
     */
    private fun sendGeminiRequest(
        preset: ApiPreset,
        systemPrompt: String,
        messages: List<JSONObject>
    ): String? {
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
        
        val url = "${preset.baseUrl}/models/${preset.model}:generateContent?key=${preset.apiKey}"
        val request = Request.Builder()
            .url(url)
            .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
            .addHeader("Content-Type", "application/json")
            .build()
            
        return executeRequest(request)
    }
    
    /**
     * 执行HTTP请求并解析响应
     */
    private fun executeRequest(request: Request): String? {
        var response: Response? = null
        return try {
            response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.e(TAG, "API request failed: ${response.code} - ${response.body?.string()}")
                return null
            }
            
            val responseBody = response.body?.string()
            if (responseBody.isNullOrEmpty()) {
                Log.e(TAG, "Empty response body")
                return null
            }
            
            parseResponse(responseBody, request.url.toString())
        } catch (e: IOException) {
            Log.e(TAG, "Network error", e)
            null
        } finally {
            response?.close()
        }
    }
    
    /**
     * 解析API响应
     */
    private fun parseResponse(responseBody: String, url: String): String? {
        return try {
            val json = JSONObject(responseBody)
            
            if (url.contains("openai")) {
                // OpenAI 格式
                val choices = json.getJSONArray("choices")
                if (choices.length() > 0) {
                    val message = choices.getJSONObject(0).getJSONObject("message")
                    message.getString("content").trim()
                } else {
                    null
                }
            } else if (url.contains("generativelanguage.googleapis.com")) {
                // Gemini 格式
                val candidates = json.getJSONArray("candidates")
                if (candidates.length() > 0) {
                    val content = candidates.getJSONObject(0).getJSONObject("content")
                    val parts = content.getJSONArray("parts")
                    if (parts.length() > 0) {
                        parts.getJSONObject(0).getString("text").trim()
                    } else {
                        null
                    }
                } else {
                    null
                }
            } else {
                Log.e(TAG, "Unknown API format")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing response: $responseBody", e)
            null
        }
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