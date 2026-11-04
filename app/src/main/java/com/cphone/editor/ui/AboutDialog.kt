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
            Text("About CPhone Studio", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("CPhone Commercial Mobile IDE", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text("Version 2.0.0 (Pro Edition)", fontSize = 13.sp, color = MaterialTheme.colorScheme.outline)
                Text(
                    "Engineered specifically for Android developers with high-density code viewport calibration, real-time bug diagnostics, and multi-language execution.",
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text("Core Capabilities:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Text("• Code Trails: Symbol Outline, Breadcrumbs & Cursor History", fontSize = 12.sp)
                Text("• Live Bug Inspector & Gutter Error Radar", fontSize = 12.sp)
                Text("• Fixed Python 3 (Skulpt + Stdin + Turtle), JS/TS V8 & Multi-Tab HTML Bundler", fontSize = 12.sp)
                Text("• Command Palette (⌘), Auto-Formatter & Code Snippets", fontSize = 12.sp)
                Text("• 6 Commercial IDE Themes (One Dark, Dracula, Monokai, Tokyo Night, Solarized, GitHub)", fontSize = 12.sp)
                Text("• 20 Programming Languages & Session Auto-Persistence", fontSize = 12.sp)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}
