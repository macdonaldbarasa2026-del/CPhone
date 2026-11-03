package com.cphone.editor.model

data class EditorSettings(
    val fontSizeSp: Float = 14f,
    val tabSize: Int = 4,
    val showLineNumbers: Boolean = true,
    val wordWrap: Boolean = false,
    val isDarkMode: Boolean = false,
    val alternatingRowBands: Boolean = true,
    val showTabBar: Boolean = true,
    // AI Settings
    val aiProvider: String = "gemini", // "gemini", "openai", "claude", "custom"
    val aiApiKey: String = "",
    val aiModel: String = "gemini-1.5-flash",
    val aiCustomEndpoint: String = "https://api.openai.com/v1/chat/completions"
)
