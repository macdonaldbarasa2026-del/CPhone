package com.cphone.editor.model

import android.net.Uri
import androidx.compose.ui.text.input.TextFieldValue
import java.util.UUID

data class EditorTab(
    val id: String = UUID.randomUUID().toString(),
    val fileName: String,
    val uri: Uri? = null,
    val state: TextFieldValue,
    val isModified: Boolean = false,
    val language: SupportedLanguage
)
