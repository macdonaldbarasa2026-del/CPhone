package com.cphone.editor.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
fun NewFileDialog(
    onConfirm: (String, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var fileName by remember { mutableStateOf("script.py") }
    var useStarterTemplate by remember { mutableStateOf(true) }

    val sanitizedPreview = remember(fileName) { SupportedLanguage.sanitizeFileName(fileName) }
    val detectedLanguage = remember(sanitizedPreview) { SupportedLanguage.detect(sanitizedPreview) }

    val quickTemplates = listOf(
        "script.py",
        "docks.html",
        "app.js",
        "main.ts",
        "style.css",
        "config.json",
        "README.md",
        "Main.kt",
        "main.cpp",
        "setup.sh",
        "netfetch.bat",
        "schema.sql"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LanguageFileIcon(language = detectedLanguage, size = 24.dp)
                Spacer(modifier = Modifier.width(10.dp))
                Text("Create New File", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Quick file presets:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    quickTemplates.forEach { preset ->
                        FilterChip(
                            selected = sanitizedPreview.equals(preset, ignoreCase = true),
                            onClick = { fileName = preset },
                            leadingIcon = {
                                LanguageFileIcon(fileName = preset, size = 16.dp)
                            },
                            label = { Text(preset, fontSize = 12.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = fileName,
                    onValueChange = { fileName = it },
                    label = { Text("Filename with extension") },
                    leadingIcon = {
                        LanguageFileIcon(language = detectedLanguage, size = 22.dp)
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Live detected language card
                Surface(
                    color = Color(detectedLanguage.badgeColorHex).copy(alpha = 0.14f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LanguageFileIcon(language = detectedLanguage, size = 18.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${detectedLanguage.displayName} (${detectedLanguage.badge})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Populate Starter Template", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Text("Pre-fill with runnable boilerplate code", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                    }
                    Switch(
                        checked = useStarterTemplate,
                        onCheckedChange = { useStarterTemplate = it }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleaned = SupportedLanguage.sanitizeFileName(fileName).ifEmpty { "untitled.py" }
                    onConfirm(cleaned, useStarterTemplate)
                }
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
