package com.cphone.editor.ui

import androidx.compose.foundation.horizontalScroll
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
import com.cphone.editor.syntax.SyntaxHighlighter

@Composable
fun SettingsDialog(
    settings: EditorSettings,
    onSettingsChanged: (EditorSettings) -> Unit,
    onOpenAiSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    var editorTheme by remember { mutableStateOf(settings.editorTheme) }
    var fontSize by remember { mutableFloatStateOf(settings.fontSizeSp) }
    var lineSpacing by remember { mutableFloatStateOf(settings.lineSpacingMultiplier) }
    var tabSize by remember { mutableIntStateOf(settings.tabSize) }
    var showLineNumbers by remember { mutableStateOf(settings.showLineNumbers) }
    var wordWrap by remember { mutableStateOf(settings.wordWrap) }
    var alternatingBands by remember { mutableStateOf(settings.alternatingRowBands) }
    var showTabBar by remember { mutableStateOf(settings.showTabBar) }
    var showCodeTrailBar by remember { mutableStateOf(settings.showCodeTrailBar) }
    var showActiveLine by remember { mutableStateOf(settings.showActiveLineHighlight) }
    var showIndentGuides by remember { mutableStateOf(settings.showIndentGuides) }
    var showMinimap by remember { mutableStateOf(settings.showMinimapIndicator) }
    var compactDock by remember { mutableStateOf(settings.compactBottomDock) }
    var pinchToZoom by remember { mutableStateOf(settings.pinchToZoom) }
    var bracketAutoClose by remember { mutableStateOf(settings.bracketAutoClose) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "IDE Calibration & Settings", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // IDE Theme Selector
                Text("Color Theme Palette", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SyntaxHighlighter.AllThemes.forEach { theme ->
                        FilterChip(
                            selected = editorTheme == theme.id,
                            onClick = { editorTheme = theme.id },
                            label = { Text(theme.displayName, fontSize = 12.sp) }
                        )
                    }
                }

                // Font Size Calibration
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Font Size Calibration", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text("${String.format("%.1f", fontSize)} sp", fontSize = 13.sp)
                    }
                    Slider(
                        value = fontSize,
                        onValueChange = { fontSize = it },
                        valueRange = 9f..24f
                    )
                }

                // Line Spacing Calibration
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Vertical Line Density", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text("${String.format("%.2f", lineSpacing)}x", fontSize = 13.sp)
                    }
                    Slider(
                        value = lineSpacing,
                        onValueChange = { lineSpacing = it },
                        valueRange = 1.25f..1.85f
                    )
                }

                // Tab Size
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Tab Indentation (spaces)", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(2, 4, 8).forEach { sz ->
                            FilterChip(
                                selected = tabSize == sz,
                                onClick = { tabSize = sz },
                                label = { Text("$sz") }
                            )
                        }
                    }
                }

                HorizontalDivider()

                SettingToggleRow(
                    title = "Compact Bottom Dock",
                    subtitle = "Single-row 36dp dock to maximize code view space",
                    checked = compactDock,
                    onCheckedChange = { compactDock = it }
                )

                SettingToggleRow(
                    title = "Code Trail Breadcrumb Bar",
                    subtitle = "Show symbol outline, trail arrows & quick zoom",
                    checked = showCodeTrailBar,
                    onCheckedChange = { showCodeTrailBar = it }
                )

                SettingToggleRow(
                    title = "Active Line Highlight & Bug Radar",
                    subtitle = "Highlight current line & show right-edge bug minimap",
                    checked = showActiveLine,
                    onCheckedChange = {
                        showActiveLine = it
                        showMinimap = it
                    }
                )

                SettingToggleRow(
                    title = "Vertical Indent Guides",
                    subtitle = "Draw block scope lines every tab stop",
                    checked = showIndentGuides,
                    onCheckedChange = { showIndentGuides = it }
                )

                SettingToggleRow(
                    title = "Two-Finger Pinch to Zoom",
                    subtitle = "Calibrate code font size directly with fingers",
                    checked = pinchToZoom,
                    onCheckedChange = { pinchToZoom = it }
                )

                SettingToggleRow(
                    title = "Auto-Close Brackets & Quotes",
                    subtitle = "Automatically pair {}, (), [], \"\", '' and expand on Enter",
                    checked = bracketAutoClose,
                    onCheckedChange = { bracketAutoClose = it }
                )

                SettingToggleRow(
                    title = "Word Wrap",
                    subtitle = "Wrap long lines to fit screen width",
                    checked = wordWrap,
                    onCheckedChange = { wordWrap = it }
                )

                SettingToggleRow(
                    title = "Show Line Numbers",
                    subtitle = "Display line numbers & live bug dots in left gutter",
                    checked = showLineNumbers,
                    onCheckedChange = { showLineNumbers = it }
                )

                SettingToggleRow(
                    title = "Alternating Row Bands",
                    subtitle = "Ruled-paper style alternating line stripes",
                    checked = alternatingBands,
                    onCheckedChange = { alternatingBands = it }
                )

                SettingToggleRow(
                    title = "Multi-File Tab Bar",
                    subtitle = "Show open file tabs below top bar",
                    checked = showTabBar,
                    onCheckedChange = { showTabBar = it }
                )

                HorizontalDivider()

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
            Button(
                onClick = {
                    val isDark = editorTheme != "github_light"
                    onSettingsChanged(
                        settings.copy(
                            editorTheme = editorTheme,
                            isDarkMode = isDark,
                            fontSizeSp = fontSize,
                            lineSpacingMultiplier = lineSpacing,
                            tabSize = tabSize,
                            showLineNumbers = showLineNumbers,
                            wordWrap = wordWrap,
                            alternatingRowBands = alternatingBands,
                            showTabBar = showTabBar,
                            showCodeTrailBar = showCodeTrailBar,
                            showActiveLineHighlight = showActiveLine,
                            showIndentGuides = showIndentGuides,
                            showMinimapIndicator = showMinimap,
                            compactBottomDock = compactDock,
                            pinchToZoom = pinchToZoom,
                            bracketAutoClose = bracketAutoClose
                        )
                    )
                    onDismiss()
                }
            ) {
                Text("Save Calibration")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(title, fontSize = 13.5.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
