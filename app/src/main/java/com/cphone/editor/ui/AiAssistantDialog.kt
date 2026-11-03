package com.cphone.editor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cphone.editor.ai.AiAssistantService
import com.cphone.editor.model.EditorSettings
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAssistantDialog(
    selectedCode: String,
    fullCode: String,
    settings: EditorSettings,
    onSaveSettings: (EditorSettings) -> Unit,
    onInsertCode: (String) -> Unit,
    onReplaceFile: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    var isConfigMode by remember { mutableStateOf(settings.aiApiKey.isBlank()) }
    var provider by remember { mutableStateOf(settings.aiProvider) }
    var apiKey by remember { mutableStateOf(settings.aiApiKey) }
    var model by remember { mutableStateOf(settings.aiModel) }
    var customEndpoint by remember { mutableStateOf(settings.aiCustomEndpoint) }

    var promptInput by remember { mutableStateOf("") }
    var aiResult by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val quickActions = listOf(
        "Generate Code",
        "Fix Bugs",
        "Explain Code",
        "Refactor",
        "Add Comments",
        "Optimize",
        "Write Tests",
        "Convert Language",
        "Add Types"
    )

    fun runTask(action: String) {
        isLoading = true
        errorMessage = null
        aiResult = ""

        val contextCode = if (selectedCode.isNotBlank()) selectedCode else fullCode
        val instruction = if (promptInput.isNotBlank()) {
            "$action: $promptInput"
        } else {
            action
        }

        coroutineScope.launch {
            val result = AiAssistantService.executeAiTask(
                instruction = instruction,
                codeContext = contextCode,
                settings = settings.copy(
                    aiProvider = provider,
                    aiApiKey = apiKey,
                    aiModel = model,
                    aiCustomEndpoint = customEndpoint
                )
            )
            isLoading = false
            result.onSuccess { text ->
                aiResult = text
            }.onFailure { err ->
                errorMessage = err.localizedMessage ?: "Unknown AI error"
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color(0xFF007AFF),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isConfigMode) "AI API Configuration" else "AI Coding Assistant",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }

                IconButton(onClick = { isConfigMode = !isConfigMode }) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Configure API",
                        tint = if (isConfigMode) Color(0xFF007AFF) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            if (isConfigMode) {
                // API Setup Form
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Set up your AI API key to code directly with AI in CPhone:",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Provider selector chips
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("gemini", "claude", "openai", "codex", "kimi", "antigravity", "custom").forEach { prov ->
                            FilterChip(
                                selected = provider.equals(prov, ignoreCase = true),
                                onClick = {
                                    provider = prov
                                    // Pre-fill sensible default models
                                    when (prov) {
                                        "gemini"      -> if (model.isBlank()) model = "gemini-2.0-flash"
                                        "openai",
                                        "codex"       -> if (model.isBlank()) model = "gpt-4o"
                                        "claude"      -> if (model.isBlank()) model = "claude-3-5-sonnet-20241022"
                                        "kimi"        -> if (model.isBlank()) model = "moonshot-v1-8k"
                                        "antigravity" -> if (model.isBlank()) model = "gemini-2.0-flash"
                                    }
                                },
                                label = {
                                    Text(
                                        when (prov) {
                                            "gemini"      -> "Gemini"
                                            "claude"      -> "Claude"
                                            "openai"      -> "OpenAI"
                                            "codex"       -> "Codex"
                                            "kimi"        -> "Kimi"
                                            "antigravity" -> "Antigravity"
                                            else          -> "Custom"
                                        },
                                        fontSize = 12.sp
                                    )
                                }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it },
                        label = { Text("API Key") },
                        placeholder = { Text("AIzaSy... or sk-...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = model,
                        onValueChange = { model = it },
                        label = { Text("Model Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (provider == "custom") {
                        OutlinedTextField(
                            value = customEndpoint,
                            onValueChange = { customEndpoint = it },
                            label = { Text("Custom Endpoint URL") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Button(
                        onClick = {
                            onSaveSettings(
                                settings.copy(
                                    aiProvider = provider,
                                    aiApiKey = apiKey,
                                    aiModel = model,
                                    aiCustomEndpoint = customEndpoint
                                )
                            )
                            isConfigMode = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save & Continue")
                    }
                }
            } else {
                // AI Prompt & Output View
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Quick Action Chips
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        quickActions.forEach { action ->
                            FilledTonalButton(
                                onClick = { runTask(action) },
                                enabled = !isLoading,
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text(action, fontSize = 12.sp)
                            }
                        }
                    }

                    // Prompt text area
                    OutlinedTextField(
                        value = promptInput,
                        onValueChange = { promptInput = it },
                        placeholder = { Text("Describe what you want to build or fix...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 72.dp, max = 120.dp)
                    )

                    Button(
                        onClick = { runTask("Code Assistant") },
                        enabled = !isLoading && (promptInput.isNotBlank() || fullCode.isNotBlank()),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generating...")
                        } else {
                            Text("Generate / Execute")
                        }
                    }

                    // Error display
                    errorMessage?.let { err ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                        ) {
                            Text(
                                text = err,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }

                    // AI Result Display
                    if (aiResult.isNotBlank()) {
                        Text("Generated Code / Response:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF6F8FA), shape = MaterialTheme.shapes.small)
                                .padding(10.dp)
                        ) {
                            Text(
                                text = aiResult,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = Color(0xFF1F2328)
                            )
                        }

                        // Code actions
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilledTonalButton(
                                onClick = {
                                    onInsertCode(aiResult)
                                    onDismiss()
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Insert Code", fontSize = 12.sp)
                            }

                            FilledTonalButton(
                                onClick = {
                                    onReplaceFile(aiResult)
                                    onDismiss()
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Replace File", fontSize = 12.sp)
                            }

                            IconButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(aiResult))
                                }
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = "Copy")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
