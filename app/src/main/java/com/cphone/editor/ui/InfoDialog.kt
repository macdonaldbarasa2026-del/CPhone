package com.cphone.editor.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cphone.editor.model.SupportedLanguage

@Composable
fun InfoDialog(
    fileName: String,
    filePath: String,
    language: SupportedLanguage,
    lineCount: Int,
    charCount: Int,
    byteSize: Int,
    onDismiss: () -> Unit
) {
    val sizeText = when {
        byteSize < 1024 -> "$byteSize B"
        byteSize < 1024 * 1024 -> String.format("%.2f KB", byteSize / 1024.0)
        else -> String.format("%.2f MB", byteSize / (1024.0 * 1024.0))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "File Information", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                InfoRow("File Name", fileName)
                InfoRow("Language", language.displayName)
                InfoRow("Path", filePath.ifEmpty { "In-Memory / Unsaved" })
                InfoRow("Total Lines", lineCount.toString())
                InfoRow("Characters", charCount.toString())
                InfoRow("File Size", sizeText)
                InfoRow("Encoding", "UTF-8")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = value, fontSize = 14.sp)
    }
}
