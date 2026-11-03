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

/**
 * AI assistant network service.
 *
 * Supported providers:
 *   gemini      – Google Gemini via native API
 *   claude      – Anthropic Claude (Messages API)
 *   openai      – OpenAI Chat Completions
 *   codex       – OpenAI (same API, alias for clarity)
 *   kimi        – Moonshot AI (OpenAI-compatible, api.moonshot.cn)
 *   antigravity – Antigravity / Google AI Studio OpenAI-compat endpoint
 *   custom      – Any OpenAI-compatible endpoint (user provides URL)
 */
object AiAssistantService {

    suspend fun executeAiTask(
        instruction: String,
        codeContext: String,
        settings: EditorSettings
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val apiKey = settings.aiApiKey.trim()
            if (apiKey.isEmpty()) {
                return@withContext Result.failure(
                    Exception("API Key is empty. Open AI Settings (⚙) and enter your key.")
                )
            }

            when (settings.aiProvider.lowercase().trim()) {
                "gemini"      -> callGemini(
                    apiKey      = apiKey,
                    model       = settings.aiModel.ifBlank { "gemini-2.0-flash" },
                    instruction = instruction,
                    codeContext = codeContext
                )
                "claude"      -> callClaude(
                    apiKey      = apiKey,
                    model       = settings.aiModel.ifBlank { "claude-3-5-sonnet-20241022" },
                    instruction = instruction,
                    codeContext = codeContext
                )
                "kimi"        -> callOpenAiCompat(
                    apiKey      = apiKey,
                    model       = settings.aiModel.ifBlank { "moonshot-v1-8k" },
                    endpoint    = "https://api.moonshot.cn/v1/chat/completions",
                    instruction = instruction,
                    codeContext = codeContext
                )
                "antigravity" -> callOpenAiCompat(
                    apiKey      = apiKey,
                    model       = settings.aiModel.ifBlank { "gemini-2.0-flash" },
                    endpoint    = "https://generativelanguage.googleapis.com/v1beta/openai/chat/completions",
                    instruction = instruction,
                    codeContext = codeContext
                )
                "openai",
                "codex"       -> callOpenAiCompat(
                    apiKey      = apiKey,
                    model       = settings.aiModel.ifBlank { "gpt-4o" },
                    endpoint    = "https://api.openai.com/v1/chat/completions",
                    instruction = instruction,
                    codeContext = codeContext
                )
                else          -> callOpenAiCompat(
                    apiKey      = apiKey,
                    model       = settings.aiModel,
                    endpoint    = settings.aiCustomEndpoint.ifBlank { "https://api.openai.com/v1/chat/completions" },
                    instruction = instruction,
                    codeContext = codeContext
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Gemini native API ─────────────────────────────────────────────────────
    private fun callGemini(
        apiKey: String,
        model: String,
        instruction: String,
        codeContext: String
    ): Result<String> {
        val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
        val prompt   = buildPrompt(instruction, codeContext)

        val body = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", prompt))
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.2)
                put("maxOutputTokens", 8192)
            })
        }

        val resp = postJson(endpoint, body.toString(), mapOf("Content-Type" to "application/json"))
        val json = JSONObject(resp)
        val candidates = json.optJSONArray("candidates")
        if (candidates != null && candidates.length() > 0) {
            val content = candidates.getJSONObject(0).optJSONObject("content")
            val parts   = content?.optJSONArray("parts")
            if (parts != null && parts.length() > 0) {
                return Result.success(cleanCodeBlocks(parts.getJSONObject(0).optString("text")))
            }
        }
        val err = json.optJSONObject("error")?.optString("message") ?: "Unknown Gemini error"
        return Result.failure(Exception(err))
    }

    // ── Claude Messages API ───────────────────────────────────────────────────
    private fun callClaude(
        apiKey: String,
        model: String,
        instruction: String,
        codeContext: String
    ): Result<String> {
        val body = JSONObject().apply {
            put("model", model)
            put("max_tokens", 8192)
            put("system", SYSTEM_PROMPT)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", buildPrompt(instruction, codeContext))
                })
            })
        }
        val headers = mapOf(
            "Content-Type"      to "application/json",
            "x-api-key"         to apiKey,
            "anthropic-version" to "2023-06-01"
        )
        val resp = postJson("https://api.anthropic.com/v1/messages", body.toString(), headers)
        val json = JSONObject(resp)
        val content = json.optJSONArray("content")
        if (content != null && content.length() > 0) {
            return Result.success(cleanCodeBlocks(content.getJSONObject(0).optString("text")))
        }
        val err = json.optJSONObject("error")?.optString("message") ?: "Unknown Claude error"
        return Result.failure(Exception(err))
    }

    // ── OpenAI-compatible (OpenAI / Kimi / Antigravity / Custom) ─────────────
    private fun callOpenAiCompat(
        apiKey: String,
        model: String,
        endpoint: String,
        instruction: String,
        codeContext: String
    ): Result<String> {
        val body = JSONObject().apply {
            put("model", model)
            put("temperature", 0.2)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", SYSTEM_PROMPT)
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", buildPrompt(instruction, codeContext))
                })
            })
        }
        val headers = mapOf(
            "Content-Type"  to "application/json",
            "Authorization" to "Bearer $apiKey"
        )
        val resp = postJson(endpoint, body.toString(), headers)
        val json = JSONObject(resp)
        val choices = json.optJSONArray("choices")
        if (choices != null && choices.length() > 0) {
            val text = choices.getJSONObject(0).optJSONObject("message")?.optString("content") ?: ""
            return Result.success(cleanCodeBlocks(text))
        }
        val err = json.optJSONObject("error")?.optString("message") ?: "Unknown API error"
        return Result.failure(Exception(err))
    }

    // ── Prompt helpers ────────────────────────────────────────────────────────

    private const val SYSTEM_PROMPT =
        "You are an expert AI programming assistant embedded in CPhone, a professional mobile code editor. " +
        "Always respond with clean, complete, working code. " +
        "Do NOT wrap code in markdown fences unless the user explicitly asks for explanation. " +
        "Be concise and production-ready."

    private fun buildPrompt(instruction: String, codeContext: String): String {
        return if (codeContext.isNotBlank()) {
            "Task: $instruction\n\nCode:\n```\n$codeContext\n```\n\nRespond with the complete updated code only."
        } else {
            "Task: $instruction"
        }
    }

    private fun cleanCodeBlocks(raw: String): String {
        val t = raw.trim()
        // Strip leading ```lang and trailing ```
        if (t.startsWith("```")) {
            val nl = t.indexOf('\n')
            if (nl != -1 && t.endsWith("```")) {
                return t.substring(nl + 1, t.length - 3).trim()
            }
        }
        return t
    }

    // ── HTTP POST ─────────────────────────────────────────────────────────────
    private fun postJson(urlString: String, jsonPayload: String, headers: Map<String, String>): String {
        val conn = URL(urlString).openConnection() as HttpURLConnection
        conn.requestMethod  = "POST"
        conn.doOutput       = true
        conn.connectTimeout = 30_000
        conn.readTimeout    = 90_000

        headers.forEach { (k, v) -> conn.setRequestProperty(k, v) }

        OutputStreamWriter(conn.outputStream, "UTF-8").use { it.write(jsonPayload); it.flush() }

        val code   = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val body   = BufferedReader(InputStreamReader(stream, "UTF-8")).readText()

        if (code !in 200..299) {
            val msg = try {
                JSONObject(body).optJSONObject("error")?.optString("message") ?: body
            } catch (_: Exception) { body }
            throw Exception("API Error $code: $msg")
        }

        return body
    }
}
