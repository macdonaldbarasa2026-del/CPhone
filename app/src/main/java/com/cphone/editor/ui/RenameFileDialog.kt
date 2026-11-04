package com.cphone.editor.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cphone.editor.model.SupportedLanguage

@Composable
fun RenameFileDialog(
    currentName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var fileName by remember { mutableStateOf(currentName) }
    val sanitizedPreview = remember(fileName) { SupportedLanguage.sanitizeFileName(fileName) }
    val detectedLanguage = remember(sanitizedPreview) { SupportedLanguage.detect(sanitizedPreview) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LanguageFileIcon(language = detectedLanguage, size = 24.dp)
                Spacer(modifier = Modifier.width(10.dp))
                Text("Rename / Change Extension", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Type a filename with extension (e.g. docks.html, script.py, app.js):",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = fileName,
                    onValueChange = { fileName = it },
                    leadingIcon = {
                        LanguageFileIcon(language = detectedLanguage, size = 22.dp)
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Live detected language badge preview
                Surface(
                    color = Color(detectedLanguage.badgeColorHex).copy(alpha = 0.14f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LanguageFileIcon(language = detectedLanguage, size = 20.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = sanitizedPreview.ifEmpty { "untitled" },
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Detected: ${detectedLanguage.displayName} (${detectedLanguage.badge})",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleaned = SupportedLanguage.sanitizeFileName(fileName)
                    if (cleaned.isNotEmpty()) {
                        onConfirm(cleaned)
                    }
                }
            ) {
                Text("Rename")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
