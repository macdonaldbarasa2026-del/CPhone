package com.cphone.editor.ai

import com.cphone.editor.model.EditorSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object AiAssistantService {

    suspend fun executeAiTask(
        instruction: String,
        codeContext: String,
        settings: EditorSettings
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val apiKey = settings.aiApiKey.trim()
            if (apiKey.isEmpty()) {
                return@withContext Result.failure(Exception("AI API Key is empty. Please enter your API Key in AI Settings."))
            }

            when (settings.aiProvider.lowercase()) {
                "gemini" -> callGemini(apiKey, settings.aiModel, instruction, codeContext)
                "claude" -> callClaude(apiKey, settings.aiModel, instruction, codeContext)
                else -> callOpenAiCompatible(apiKey, settings.aiModel, settings.aiCustomEndpoint, instruction, codeContext)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun callGemini(
        apiKey: String,
        model: String,
        instruction: String,
        codeContext: String
    ): Result<String> {
        val modelName = if (model.isBlank()) "gemini-1.5-flash" else model
        val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"

        val prompt = buildPrompt(instruction, codeContext)

        val jsonBody = JSONObject().apply {
            val contents = JSONArray().apply {
                put(JSONObject().apply {
                    val parts = JSONArray().apply {
                        put(JSONObject().put("text", prompt))
                    }
                    put("parts", parts)
                })
            }
            put("contents", contents)
        }

        val response = postJson(endpoint, jsonBody.toString(), mapOf("Content-Type" to "application/json"))
        val jsonResp = JSONObject(response)
        val candidates = jsonResp.optJSONArray("candidates")
        if (candidates != null && candidates.length() > 0) {
            val content = candidates.getJSONObject(0).optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            if (parts != null && parts.length() > 0) {
                val text = parts.getJSONObject(0).optString("text")
                return Result.success(cleanCodeBlocks(text))
            }
        }

        val error = jsonResp.optJSONObject("error")?.optString("message") ?: "Unknown Gemini API error"
        return Result.failure(Exception(error))
    }

    private fun callOpenAiCompatible(
        apiKey: String,
        model: String,
        endpointUrl: String,
        instruction: String,
        codeContext: String
    ): Result<String> {
        val urlStr = if (endpointUrl.isBlank()) "https://api.openai.com/v1/chat/completions" else endpointUrl
        val modelName = if (model.isBlank()) "gpt-4o-mini" else model

        val jsonBody = JSONObject().apply {
            put("model", modelName)
            val messages = JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", "You are an expert AI programming assistant in a professional mobile code editor. Answer with clean, complete code and minimal conversational fluff.")
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", buildPrompt(instruction, codeContext))
                })
            }
            put("messages", messages)
        }

        val headers = mapOf(
            "Content-Type" to "application/json",
            "Authorization" to "Bearer $apiKey"
        )

        val response = postJson(urlStr, jsonBody.toString(), headers)
        val jsonResp = JSONObject(response)
        val choices = jsonResp.optJSONArray("choices")
        if (choices != null && choices.length() > 0) {
            val msg = choices.getJSONObject(0).optJSONObject("message")
            val text = msg?.optString("content") ?: ""
            return Result.success(cleanCodeBlocks(text))
        }

        val error = jsonResp.optJSONObject("error")?.optString("message") ?: "Unknown OpenAI API error"
        return Result.failure(Exception(error))
    }

    private fun callClaude(
        apiKey: String,
        model: String,
        instruction: String,
        codeContext: String
    ): Result<String> {
        val endpoint = "https://api.anthropic.com/v1/messages"
        val modelName = if (model.isBlank()) "claude-3-5-sonnet-20241022" else model

        val jsonBody = JSONObject().apply {
            put("model", modelName)
            put("max_tokens", 4096)
            val messages = JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", buildPrompt(instruction, codeContext))
                })
            }
            put("messages", messages)
        }

        val headers = mapOf(
            "Content-Type" to "application/json",
            "x-api-key" to apiKey,
            "anthropic-version" to "2023-06-01"
        )

        val response = postJson(endpoint, jsonBody.toString(), headers)
        val jsonResp = JSONObject(response)
        val contentArray = jsonResp.optJSONArray("content")
        if (contentArray != null && contentArray.length() > 0) {
            val text = contentArray.getJSONObject(0).optString("text")
            return Result.success(cleanCodeBlocks(text))
        }

        val error = jsonResp.optJSONObject("error")?.optString("message") ?: "Unknown Claude API error"
        return Result.failure(Exception(error))
    }

    private fun buildPrompt(instruction: String, codeContext: String): String {
        return if (codeContext.isNotBlank()) {
            """
Task Instruction: $instruction

Source Code:
```
$codeContext
```
""".trimIndent()
        } else {
            instruction
        }
    }

    private fun cleanCodeBlocks(text: String): String {
        val trimmed = text.trim()
        if (trimmed.startsWith("```") && trimmed.endsWith("```")) {
            val firstNewline = trimmed.indexOf('\n')
            if (firstNewline != -1) {
                return trimmed.substring(firstNewline + 1, trimmed.length - 3).trim()
            }
        }
        return trimmed
    }

    private fun postJson(urlString: String, jsonPayload: String, headers: Map<String, String>): String {
        val url = URL(urlString)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.doOutput = true
        conn.connectTimeout = 30000
        conn.readTimeout = 60000

        headers.forEach { (k, v) -> conn.setRequestProperty(k, v) }

        OutputStreamWriter(conn.outputStream, "UTF-8").use { writer ->
            writer.write(jsonPayload)
            writer.flush()
        }

        val responseCode = conn.responseCode
        val stream = if (responseCode in 200..299) conn.inputStream else conn.errorStream
        val reader = BufferedReader(InputStreamReader(stream, "UTF-8"))
        val response = reader.readText()
        reader.close()

        if (responseCode !in 200..299) {
            try {
                val json = JSONObject(response)
                val msg = json.optJSONObject("error")?.optString("message") ?: response
                throw Exception("API Error ($responseCode): $msg")
            } catch (_: Exception) {
                throw Exception("API Error ($responseCode): $response")
            }
        }

        return response
    }
}
