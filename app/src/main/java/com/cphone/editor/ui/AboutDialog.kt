package com.cphone.editor.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("About CPhone", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("CPhone Code Editor", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text("Version 1.1.0", fontSize = 13.sp, color = MaterialTheme.colorScheme.outline)
                Text(
                    "A professional mobile code editor inspired by Acode and designed with pixel-accurate fidelity to the original NetFetch editor interface.",
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text("Key Capabilities:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Text("• Alternating ruled paper row bands", fontSize = 12.sp)
                Text("• Full multi-file tab system", fontSize = 12.sp)
                Text("• Built-in storage File Explorer", fontSize = 12.sp)
                Text("• Integrated AI Coding Assistant (Gemini / OpenAI / Claude)", fontSize = 12.sp)
                Text("• 16 programming languages syntax highlighting", fontSize = 12.sp)
                Text("• 200-level Undo/Redo & Find/Replace", fontSize = 12.sp)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}
