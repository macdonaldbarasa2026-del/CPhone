package com.cphone.editor.model

data class EditorSettings(
    val fontSizeSp: Float = 14f,
    val tabSize: Int = 4,
    val showLineNumbers: Boolean = true,
    val wordWrap: Boolean = false,
    val isDarkMode: Boolean = true,          // default dark like VS Code
    val alternatingRowBands: Boolean = true,
    val showTabBar: Boolean = true,
    val bracketAutoClose: Boolean = true,
    // AI Settings
    val aiProvider: String = "gemini",       // "gemini","claude","openai","codex","kimi","antigravity","custom"
    val aiApiKey: String = "",
    val aiModel: String = "",
    val aiCustomEndpoint: String = ""
)
