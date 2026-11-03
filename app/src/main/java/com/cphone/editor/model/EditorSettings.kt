package com.cphone.editor.model

data class EditorSettings(
    val fontSizeSp: Float = 14f,
    val tabSize: Int = 4,
    val showLineNumbers: Boolean = true,
    val wordWrap: Boolean = false,
    val isDarkMode: Boolean = false
)
