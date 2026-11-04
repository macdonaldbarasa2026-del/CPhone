package com.cphone.editor.model

data class EditorSettings(
    val fontSizeSp: Float = 13.5f,
    val lineSpacingMultiplier: Float = 1.48f,
    val tabSize: Int = 4,
    val showLineNumbers: Boolean = true,
    val wordWrap: Boolean = false,
    val isDarkMode: Boolean = true,
    val editorTheme: String = "one_dark",    // "one_dark", "dracula", "monokai", "tokyo_night", "solarized_dark", "github_light"
    val alternatingRowBands: Boolean = false,
    val showTabBar: Boolean = true,
    val showCodeTrailBar: Boolean = true,
    val showActiveLineHighlight: Boolean = true,
    val showIndentGuides: Boolean = true,
    val showMinimapIndicator: Boolean = true,
    val compactBottomDock: Boolean = true,
    val pinchToZoom: Boolean = true,
    val autoSaveSession: Boolean = true,
    val bracketAutoClose: Boolean = true,
    // AI Settings
    val aiProvider: String = "gemini",       // "gemini","claude","openai","codex","kimi","antigravity","custom"
    val aiApiKey: String = "",
    val aiModel: String = "",
    val aiCustomEndpoint: String = ""
)
