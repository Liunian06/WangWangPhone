import Foundation

struct LlmApiResponse {
    let content: String
    let isError: Bool
    let errorMessage: String?
}

class LlmApiService {
    static let shared = LlmApiService()
    
    private init() {}
    
    func callLlmApi(
        preset: ApiPreset,
        userMessage: String,
        aiPersona: String,
        userPersona: String,
        completion: @escaping (LlmApiResponse) -> Void
    ) {
        // 构建系统提示，包含AI角色人设和用户人设
        var systemPrompt = "你正在与用户进行角色扮演对话。"
        
        if !aiPersona.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            systemPrompt += "\n\n你的角色人设：\(aiPersona)"
        }
        
        if !userPersona.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            systemPrompt += "\n\n用户的人设：\(userPersona)"
        }
        
        systemPrompt += "\n\n请根据以上人设信息进行沉浸式的角色扮演对话。"
        
        // 根据提供商类型构建请求
        switch preset.provider {
        case "openai":
            callOpenAIApi(preset: preset, systemPrompt: systemPrompt, userMessage: userMessage, completion: completion)
        case "gemini":
            callGeminiApi(preset: preset, systemPrompt: systemPrompt, userMessage: userMessage, completion: completion)
        default:
            completion(LlmApiResponse(
                content: "",
                isError: true,
                errorMessage: "不支持的API提供商: \(preset.provider)"
            ))
        }
    }
    
    private func callOpenAIApi(
        preset: ApiPreset,
        systemPrompt: String,
        userMessage: String,
        completion: @escaping (LlmApiResponse) -> Void
    ) {
        guard let url = URL(string: "\(preset.baseUrl)/chat/completions") else {
            completion(LlmApiResponse(
                content: "",
                isError: true,
                errorMessage: "无效的API URL"
            ))
            return
        }
        
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue("Bearer \(preset.apiKey)", forHTTPHeaderField: "Authorization")
        
        // 解析额外参数
        var params: [String: Any] = [:]
        if let extraParamsData = preset.extraParams.data(using: .utf8),
           let extraParamsDict = try? JSONSerialization.jsonObject(with: extraParamsData) as? [String: Any] {
            params = extraParamsDict
        }
        
        // 构建消息数组
        var messages: [[String: Any]] = []
        messages.append(["role": "system", "content": systemPrompt])
        messages.append(["role": "user", "content": userMessage])
        
        // 构建请求体
        var requestBody: [String: Any] = [
            "model": preset.model,
            "messages": messages
        ]
        
        // 添加额外参数
        for (key, value) in params {
            requestBody[key] = value
        }
        
        // 移除流式输出参数（我们使用同步请求）
        requestBody["stream"] = false
        
        do {
            let jsonData = try JSONSerialization.data(withJSONObject: requestBody, options: [])
            request.httpBody = jsonData
            
            let task = URLSession.shared.dataTask(with: request) { data, response, error in
                if let error = error {
                    DispatchQueue.main.async {
                        completion(LlmApiResponse(
                            content: "",
                            isError: true,
                            errorMessage: "网络错误: \(error.localizedDescription)"
                        ))
                    }
                    return
                }
                
                guard let data = data else {
                    DispatchQueue.main.async {
                        completion(LlmApiResponse(
                            content: "",
                            isError: true,
                            errorMessage: "无响应数据"
                        ))
                    }
                    return
                }
                
                do {
                    if let json = try JSONSerialization.jsonObject(with: data) as? [String: Any],
                       let choices = json["choices"] as? [[String: Any]],
                       let firstChoice = choices.first,
                       let message = firstChoice["message"] as? [String: Any],
                       let content = message["content"] as? String {
                        DispatchQueue.main.async {
                            completion(LlmApiResponse(
                                content: content.trimmingCharacters(in: .whitespacesAndNewlines),
                                isError: false,
                                errorMessage: nil
                            ))
                        }
                    } else {
                        let errorResponse = String(data: data, encoding: .utf8) ?? "未知错误"
                        DispatchQueue.main.async {
                            completion(LlmApiResponse(
                                content: "",
                                isError: true,
                                errorMessage: "API响应格式错误: \(errorResponse)"
                            ))
                        }
                    }
                } catch {
                    let errorResponse = String(data: data, encoding: .utf8) ?? "JSON解析错误"
                    DispatchQueue.main.async {
                        completion(LlmApiResponse(
                            content: "",
                            isError: true,
                            errorMessage: "JSON解析失败: \(errorResponse)"
                        ))
                    }
                }
            }
            task.resume()
        } catch {
            completion(LlmApiResponse(
                content: "",
                isError: true,
                errorMessage: "请求构建失败: \(error.localizedDescription)"
            ))
        }
    }
    
    private func callGeminiApi(
        preset: ApiPreset,
        systemPrompt: String,
        userMessage: String,
        completion: @escaping (LlmApiResponse) -> Void
    ) {
        // Gemini API URL 需要包含 API key
        guard var urlComponents = URLComponents(string: "\(preset.baseUrl)/models/\(preset.model):generateContent") else {
            completion(LlmApiResponse(
                content: "",
                isError: true,
                errorMessage: "无效的API URL"
            ))
            return
        }
        
        urlComponents.queryItems = [URLQueryItem(name: "key", value: preset.apiKey)]
        
        guard let url = urlComponents.url else {
            completion(LlmApiResponse(
                content: "",
                isError: true,
                errorMessage: "无效的API URL"
            ))
            return
        }
        
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        
        // 解析额外参数
        var params: [String: Any] = [:]
        if let extraParamsData = preset.extraParams.data(using: .utf8),
           let extraParamsDict = try? JSONSerialization.jsonObject(with: extraParamsData) as? [String: Any] {
            params = extraParamsDict
        }
        
        // 构建Gemini请求体
        var requestBody: [String: Any] = [
            "contents": [
                ["role": "user", "parts": [["text": "\(systemPrompt)\n\n用户消息: \(userMessage)"]]]
            ]
        ]
        
        // 添加生成配置
        var generationConfig: [String: Any] = [:]
        if let temperature = params["temperature"] as? Double {
            generationConfig["temperature"] = temperature
        }
        if let maxTokens = params["max_tokens"] as? Int {
            generationConfig["maxOutputTokens"] = maxTokens
        }
        if let topP = params["top_p"] as? Double {
            generationConfig["topP"] = topP
        }
        if let topK = params["top_k"] as? Int {
            generationConfig["topK"] = topK
        }
        
        if !generationConfig.isEmpty {
            requestBody["generationConfig"] = generationConfig
        }
        
        do {
            let jsonData = try JSONSerialization.data(withJSONObject: requestBody, options: [])
            request.httpBody = jsonData
            
            let task = URLSession.shared.dataTask(with: request) { data, response, error in
                if let error = error {
                    DispatchQueue.main.async {
                        completion(LlmApiResponse(
                            content: "",
                            isError: true,
                            errorMessage: "网络错误: \(error.localizedDescription)"
                        ))
                    }
                    return
                }
                
                guard let data = data else {
                    DispatchQueue.main.async {
                        completion(LlmApiResponse(
                            content: "",
                            isError: true,
                            errorMessage: "无响应数据"
                        ))
                    }
                    return
                }
                
                do {
                    if let json = try JSONSerialization.jsonObject(with: data) as? [String: Any],
                       let candidates = json["candidates"] as? [[String: Any]],
                       let firstCandidate = candidates.first,
                       let content = firstCandidate["content"] as? [String: Any],
                       let parts = content["parts"] as? [[String: Any]],
                       let firstPart = parts.first,
                       let text = firstPart["text"] as? String {
                        DispatchQueue.main.async {
                            completion(LlmApiResponse(
                                content: text.trimmingCharacters(in: .whitespacesAndNewlines),
                                isError: false,
                                errorMessage: nil
                            ))
                        }
                    } else {
                        let errorResponse = String(data: data, encoding: .utf8) ?? "未知错误"
                        DispatchQueue.main.async {
                            completion(LlmApiResponse(
                                content: "",
                                isError: true,
                                errorMessage: "API响应格式错误: \(errorResponse)"
                            ))
                        }
                    }
                } catch {
                    let errorResponse = String(data: data, encoding: .utf8) ?? "JSON解析错误"
                    DispatchQueue.main.async {
                        completion(LlmApiResponse(
                            content: "",
                            isError: true,
                            errorMessage: "JSON解析失败: \(errorResponse)"
                        ))
                    }
                }
            }
            task.resume()
        } catch {
            completion(LlmApiResponse(
                content: "",
                isError: true,
                errorMessage: "请求构建失败: \(error.localizedDescription)"
            ))
        }
    }
    
    // MARK: - 流式聊天请求
    
    func sendChatRequestStream(
        preset: ApiPreset,
        messages: [[String: String]],
        systemPrompt: String
    ) -> AsyncThrowingStream<String, Error> {
        return AsyncThrowingStream { continuation in
            Task {
                do {
                    switch preset.provider {
                    case "openai":
                        try await callOpenAIStreamApi(
                            preset: preset,
                            messages: messages,
                            systemPrompt: systemPrompt,
                            continuation: continuation
                        )
                    case "gemini":
                        try await callGeminiStreamApi(
                            preset: preset,
                            messages: messages,
                            systemPrompt: systemPrompt,
                            continuation: continuation
                        )
                    default:
                        continuation.finish(throwing: NSError(
                            domain: "LlmApiService",
                            code: -1,
                            userInfo: [NSLocalizedDescriptionKey: "不支持的API提供商: \(preset.provider)"]
                        ))
                    }
                } catch {
                    continuation.finish(throwing: error)
                }
            }
        }
    }
    
    private func callOpenAIStreamApi(
        preset: ApiPreset,
        messages: [[String: String]],
        systemPrompt: String,
        continuation: AsyncThrowingStream<String, Error>.Continuation
    ) async throws {
        guard let url = URL(string: "\(preset.baseUrl)/chat/completions") else {
            throw NSError(domain: "LlmApiService", code: -1, userInfo: [NSLocalizedDescriptionKey: "无效的API URL"])
        }
        
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue("Bearer \(preset.apiKey)", forHTTPHeaderField: "Authorization")
        
        var params: [String: Any] = [:]
        if let extraParamsData = preset.extraParams.data(using: .utf8),
           let extraParamsDict = try? JSONSerialization.jsonObject(with: extraParamsData) as? [String: Any] {
            params = extraParamsDict
        }
        
        var apiMessages: [[String: Any]] = [["role": "system", "content": systemPrompt]]
        for msg in messages {
            apiMessages.append(msg)
        }
        
        var requestBody: [String: Any] = [
            "model": preset.model,
            "messages": apiMessages,
            "stream": true
        ]
        
        for (key, value) in params where key != "stream" {
            requestBody[key] = value
        }
        
        request.httpBody = try JSONSerialization.data(withJSONObject: requestBody)
        
        let (bytes, response) = try await URLSession.shared.bytes(for: request)
        
        guard let httpResponse = response as? HTTPURLResponse, httpResponse.statusCode == 200 else {
            throw NSError(domain: "LlmApiService", code: -1, userInfo: [NSLocalizedDescriptionKey: "HTTP错误"])
        }
        
        for try await line in bytes.lines {
            if line.hasPrefix("data: ") {
                let data = line.dropFirst(6)
                if data == "[DONE]" {
                    continuation.finish()
                    return
                }
                
                if let jsonData = data.data(using: .utf8),
                   let json = try? JSONSerialization.jsonObject(with: jsonData) as? [String: Any],
                   let choices = json["choices"] as? [[String: Any]],
                   let firstChoice = choices.first,
                   let delta = firstChoice["delta"] as? [String: Any],
                   let content = delta["content"] as? String {
                    continuation.yield(content)
                }
            }
        }
        
        continuation.finish()
    }
    
    private func callGeminiStreamApi(
        preset: ApiPreset,
        messages: [[String: String]],
        systemPrompt: String,
        continuation: AsyncThrowingStream<String, Error>.Continuation
    ) async throws {
        guard var urlComponents = URLComponents(string: "\(preset.baseUrl)/models/\(preset.model):streamGenerateContent") else {
            throw NSError(domain: "LlmApiService", code: -1, userInfo: [NSLocalizedDescriptionKey: "无效的API URL"])
        }
        
        urlComponents.queryItems = [URLQueryItem(name: "key", value: preset.apiKey), URLQueryItem(name: "alt", value: "sse")]
        
        guard let url = urlComponents.url else {
            throw NSError(domain: "LlmApiService", code: -1, userInfo: [NSLocalizedDescriptionKey: "无效的API URL"])
        }
        
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        
        var params: [String: Any] = [:]
        if let extraParamsData = preset.extraParams.data(using: .utf8),
           let extraParamsDict = try? JSONSerialization.jsonObject(with: extraParamsData) as? [String: Any] {
            params = extraParamsDict
        }
        
        var fullPrompt = systemPrompt
        for msg in messages {
            if let role = msg["role"], let content = msg["content"] {
                fullPrompt += "\n\n\(role): \(content)"
            }
        }
        
        var requestBody: [String: Any] = [
            "contents": [
                ["role": "user", "parts": [["text": fullPrompt]]]
            ]
        ]
        
        var generationConfig: [String: Any] = [:]
        if let temperature = params["temperature"] as? Double {
            generationConfig["temperature"] = temperature
        }
        if let maxTokens = params["max_tokens"] as? Int {
            generationConfig["maxOutputTokens"] = maxTokens
        }
        if let topP = params["top_p"] as? Double {
            generationConfig["topP"] = topP
        }
        if let topK = params["top_k"] as? Int {
            generationConfig["topK"] = topK
        }
        
        if !generationConfig.isEmpty {
            requestBody["generationConfig"] = generationConfig
        }
        
        request.httpBody = try JSONSerialization.data(withJSONObject: requestBody)
        
        let (bytes, response) = try await URLSession.shared.bytes(for: request)
        
        guard let httpResponse = response as? HTTPURLResponse, httpResponse.statusCode == 200 else {
            throw NSError(domain: "LlmApiService", code: -1, userInfo: [NSLocalizedDescriptionKey: "HTTP错误"])
        }
        
        for try await line in bytes.lines {
            if line.hasPrefix("data: ") {
                let data = line.dropFirst(6)
                
                if let jsonData = data.data(using: .utf8),
                   let json = try? JSONSerialization.jsonObject(with: jsonData) as? [String: Any],
                   let candidates = json["candidates"] as? [[String: Any]],
                   let firstCandidate = candidates.first,
                   let content = firstCandidate["content"] as? [String: Any],
                   let parts = content["parts"] as? [[String: Any]],
                   let firstPart = parts.first,
                   let text = firstPart["text"] as? String {
                    continuation.yield(text)
                }
            }
        }
        
        continuation.finish()
    }
    
    // MARK: - 测试连通性
    
    func testConnection(preset: ApiPreset, completion: @escaping (Bool, String) -> Void) {
        let prompt = "你好，你是什么模型？你是哪个公司研发的？"
        if shouldUseStreamForTest(extraParams: preset.extraParams) {
            Task {
                do {
                    let maxLength = 4000
                    var content = ""
                    var truncated = false
                    let stream = sendChatRequestStream(
                        preset: preset,
                        messages: [["role": "user", "content": prompt]],
                        systemPrompt: ""
                    )
                    for try await chunk in stream {
                        if chunk.isEmpty || truncated { continue }
                        let remaining = maxLength - content.count
                        if remaining <= 0 {
                            truncated = true
                            continue
                        }
                        if chunk.count <= remaining {
                            content += chunk
                        } else {
                            content += String(chunk.prefix(remaining))
                            truncated = true
                        }
                    }

                    let result = content.trimmingCharacters(in: .whitespacesAndNewlines)
                    DispatchQueue.main.async {
                        if result.isEmpty {
                            completion(false, "API返回空响应")
                        } else {
                            completion(true, truncated ? "\(result)\n...(测试输出已截断)" : result)
                        }
                    }
                } catch {
                    DispatchQueue.main.async {
                        completion(false, "连接失败: \(error.localizedDescription)")
                    }
                }
            }
            return
        }

        let handleResult: (LlmApiResponse) -> Void = { response in
            if response.isError {
                completion(false, response.errorMessage ?? "连接失败")
            } else if response.content.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                completion(false, "API返回空响应")
            } else {
                completion(true, response.content)
            }
        }

        switch preset.provider {
        case "openai":
            callOpenAIApi(
                preset: preset,
                systemPrompt: "",
                userMessage: prompt,
                completion: handleResult
            )
        case "gemini":
            callGeminiApi(
                preset: preset,
                systemPrompt: "",
                userMessage: prompt,
                completion: handleResult
            )
        default:
            completion(false, "不支持的API提供商: \(preset.provider)")
        }
    }

    private func shouldUseStreamForTest(extraParams: String) -> Bool {
        guard
            let data = extraParams.data(using: .utf8),
            let dict = try? JSONSerialization.jsonObject(with: data) as? [String: Any]
        else {
            return false
        }
        if let boolValue = dict["stream"] as? Bool {
            return boolValue
        }
        if let numberValue = dict["stream"] as? NSNumber {
            return numberValue.boolValue
        }
        if let stringValue = dict["stream"] as? String {
            let normalized = stringValue.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
            return normalized == "true" || normalized == "1"
        }
        return false
    }
    
    // MARK: - 获取模型列表
    
    static func fetchModels(
        provider: String,
        apiKey: String,
        baseUrl: String,
        completion: @escaping ([String]) -> Void
    ) {
        DispatchQueue.global(qos: .userInitiated).async {
            let models: [String]
            switch provider {
            case "openai":
                models = fetchOpenAIModels(apiKey: apiKey, baseUrl: baseUrl)
            case "gemini":
                models = fetchGeminiModels(apiKey: apiKey, baseUrl: baseUrl)
            default:
                models = []
            }
            DispatchQueue.main.async {
                completion(models)
            }
        }
    }
    
    private static func fetchOpenAIModels(apiKey: String, baseUrl: String) -> [String] {
        guard let url = URL(string: "\(baseUrl)/models") else {
            return []
        }
        
        var request = URLRequest(url: url)
        request.httpMethod = "GET"
        request.setValue("Bearer \(apiKey)", forHTTPHeaderField: "Authorization")
        
        let semaphore = DispatchSemaphore(value: 0)
        var result: [String] = []
        
        let task = URLSession.shared.dataTask(with: request) { data, response, error in
            defer { semaphore.signal() }
            
            guard error == nil, let data = data else {
                return
            }
            
            do {
                if let json = try JSONSerialization.jsonObject(with: data) as? [String: Any],
                   let dataArray = json["data"] as? [[String: Any]] {
                    result = dataArray.compactMap { $0["id"] as? String }
                }
            } catch {
                print("Error parsing OpenAI models: \(error)")
            }
        }
        task.resume()
        semaphore.wait()
        
        return result.sorted()
    }
    
    private static func fetchGeminiModels(apiKey: String, baseUrl: String) -> [String] {
        guard var urlComponents = URLComponents(string: "\(baseUrl)/models") else {
            return []
        }
        
        urlComponents.queryItems = [URLQueryItem(name: "key", value: apiKey)]
        
        guard let url = urlComponents.url else {
            return []
        }
        
        var request = URLRequest(url: url)
        request.httpMethod = "GET"
        
        let semaphore = DispatchSemaphore(value: 0)
        var result: [String] = []
        
        let task = URLSession.shared.dataTask(with: request) { data, response, error in
            defer { semaphore.signal() }
            
            guard error == nil, let data = data else {
                return
            }
            
            do {
                if let json = try JSONSerialization.jsonObject(with: data) as? [String: Any],
                   let models = json["models"] as? [[String: Any]] {
                    result = models.compactMap { model in
                        if let name = model["name"] as? String {
                            return name.replacingOccurrences(of: "models/", with: "")
                        }
                        return nil
                    }
                }
            } catch {
                print("Error parsing Gemini models: \(error)")
            }
        }
        task.resume()
        semaphore.wait()
        
        return result.sorted()
    }
}
