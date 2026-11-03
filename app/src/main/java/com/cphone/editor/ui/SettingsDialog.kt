package com.cphone.editor.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cphone.editor.model.EditorSettings

@Composable
fun SettingsDialog(
    settings: EditorSettings,
    onSettingsChanged: (EditorSettings) -> Unit,
    onOpenAiSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    var fontSize by remember { mutableFloatStateOf(settings.fontSizeSp) }
    var tabSize by remember { mutableIntStateOf(settings.tabSize) }
    var showLineNumbers by remember { mutableStateOf(settings.showLineNumbers) }
    var wordWrap by remember { mutableStateOf(settings.wordWrap) }
    var alternatingBands by remember { mutableStateOf(settings.alternatingRowBands) }
    var showTabBar by remember { mutableStateOf(settings.showTabBar) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Editor Settings", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Font Size
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Font Size", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text("${fontSize.toInt()} sp", fontSize = 14.sp)
                    }
                    Slider(
                        value = fontSize,
                        onValueChange = { fontSize = it },
                        valueRange = 10f..26f,
                        steps = 7
                    )
                }

                // Tab Size
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Tab Size (spaces)", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = tabSize == 2,
                            onClick = { tabSize = 2 },
                            label = { Text("2") }
                        )
                        FilterChip(
                            selected = tabSize == 4,
                            onClick = { tabSize = 4 },
                            label = { Text("4") }
                        )
                    }
                }

                // Alternating Background Bands (Ruled Paper lines)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Alternating Row Bands", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text("Subtle pale-blue ruled paper stripes", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                    }
                    Switch(
                        checked = alternatingBands,
                        onCheckedChange = { alternatingBands = it }
                    )
                }

                // Show Line Numbers
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Show Line Numbers", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Switch(
                        checked = showLineNumbers,
                        onCheckedChange = { showLineNumbers = it }
                    )
                }

                // Show Multi-file Tab Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Multi-file Tab Bar", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Switch(
                        checked = showTabBar,
                        onCheckedChange = { showTabBar = it }
                    )
                }

                // Word Wrap
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Word Wrap", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Switch(
                        checked = wordWrap,
                        onCheckedChange = { wordWrap = it }
                    )
                }

                HorizontalDivider()

                // AI Assistant Setup shortcut
                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onOpenAiSettings()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Configure AI Assistant API")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSettingsChanged(
                        settings.copy(
                            fontSizeSp = fontSize,
                            tabSize = tabSize,
                            showLineNumbers = showLineNumbers,
                            wordWrap = wordWrap,
                            alternatingRowBands = alternatingBands,
                            showTabBar = showTabBar
                        )
                    )
                    onDismiss()
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
