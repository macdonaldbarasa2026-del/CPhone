package com.cphone.editor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cphone.editor.model.SupportedLanguage
import com.cphone.editor.syntax.SyntaxHighlighter

@Composable
fun TopNavigationBar(
    fileName: String,
    subtitle: String = "New file",
    language: SupportedLanguage = SupportedLanguage.PLAINTEXT,
    isDarkMode: Boolean,
    themeId: String = "one_dark",
    isRunnable: Boolean = false,
    isZenMode: Boolean = false,
    bugCount: Int = 0,
    warningCount: Int = 0,
    onPlayClick: () -> Unit = {},
    onTitleClick: () -> Unit = {},
    onBugPillClick: () -> Unit = {},
    onCommandPaletteClick: () -> Unit = {},
    onToggleZenClick: () -> Unit = {},
    onFormatClick: () -> Unit = {},
    onSnippetsClick: () -> Unit = {},
    onGoToLineClick: () -> Unit = {},
    onHomeClick: () -> Unit,
    onInfoClick: () -> Unit,
    onThemeToggleClick: () -> Unit,
    onAiAssistantClick: () -> Unit,
    onNewFileClick: () -> Unit,
    onOpenFileClick: () -> Unit,
    onFileExplorerClick: () -> Unit,
    onSaveClick: () -> Unit,
    onSaveAsClick: () -> Unit,
    onFindClick: () -> Unit,
    onReplaceClick: () -> Unit,
    onSelectAllClick: () -> Unit,
    onUndoClick: () -> Unit,
    onRedoClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onAboutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val palette = remember(themeId, isDarkMode) {
        SyntaxHighlighter.resolvePalette(themeId, isDarkMode)
    }

    val barBg = if (isDarkMode) palette.topBarBg else Color.White
    val contentColor = if (isDarkMode) palette.syntax.defaultText else Color(0xFF111827)
    val subtitleColor = if (isDarkMode) palette.gutterText else Color(0xFF6B7280)
    val dividerColor = palette.borderColor

    val cleanFileName = remember(fileName) {
        fileName.removeSuffix(" ●").trim()
    }
    val resolvedLanguage = remember(cleanFileName, language) {
        val detected = SupportedLanguage.detect(cleanFileName)
        if (detected != SupportedLanguage.PLAINTEXT) detected else language
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(barBg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Hamburger Menu Icon (☰) matching the screenshot
            IconButton(
                onClick = onHomeClick,
                modifier = Modifier.size(42.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Menu / File Explorer",
                    tint = contentColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(2.dp))

            // Language Icon + Two-line File Title ("wifi_client.cpp" / "New file")
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onTitleClick() }
                    .padding(vertical = 4.dp, horizontal = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LanguageFileIcon(
                    language = resolvedLanguage,
                    size = 22.dp
                )

                Spacer(modifier = Modifier.width(10.dp))

                Column(
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = cleanFileName,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = contentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = subtitle,
                        fontSize = 11.5.sp,
                        color = subtitleColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Play / Run button when runnable
            if (isRunnable) {
                IconButton(
                    onClick = onPlayClick,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Run / Preview",
                        tint = Color(0xFF2E7D32),
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            // Pencil / Edit Icon matching the screenshot
            IconButton(
                onClick = onTitleClick,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Rename / Quick Edit",
                    tint = contentColor,
                    modifier = Modifier.size(21.dp)
                )
            }

            // Three-dot overflow menu with the small red notification dot matching the screenshot
            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(modifier = Modifier.size(24.dp)) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More Options",
                            tint = contentColor,
                            modifier = Modifier
                                .size(22.dp)
                                .align(Alignment.Center)
                        )
                        // Small red notification dot at bottom-right of the 3-dot icon
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .align(Alignment.BottomEnd)
                                .background(Color(0xFFEF5350), shape = CircleShape)
                        )
                    }
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    modifier = Modifier.background(if (isDarkMode) Color(0xFF242636) else Color(0xFFFFFFFF))
                ) {
                    if (isRunnable) {
                        DropdownMenuItem(
                            text = { Text("Run / Compile (▶)") },
                            onClick = { menuExpanded = false; onPlayClick() }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Command Palette (⌘)") },
                        onClick = { menuExpanded = false; onCommandPaletteClick() }
                    )
                    DropdownMenuItem(
                        text = {
                            val badge = if (bugCount + warningCount > 0) " (${bugCount + warningCount})" else ""
                            Text("Problems & Code Trails$badge")
                        },
                        onClick = { menuExpanded = false; onBugPillClick() }
                    )
                    DropdownMenuItem(
                        text = { Text("Format / Beautify Code") },
                        onClick = { menuExpanded = false; onFormatClick() }
                    )
                    DropdownMenuItem(
                        text = { Text("Insert Code Snippet") },
                        onClick = { menuExpanded = false; onSnippetsClick() }
                    )
                    DropdownMenuItem(
                        text = { Text("Go to Line...") },
                        onClick = { menuExpanded = false; onGoToLineClick() }
                    )
                    DropdownMenuItem(
                        text = { Text(if (isZenMode) "Exit Fullscreen Mode" else "Fullscreen Code Mode") },
                        onClick = { menuExpanded = false; onToggleZenClick() }
                    )
                    HorizontalDivider(color = dividerColor)
                    DropdownMenuItem(
                        text = { Text("New File") },
                        onClick = { menuExpanded = false; onNewFileClick() }
                    )
                    DropdownMenuItem(
                        text = { Text("Open File (Storage)") },
                        onClick = { menuExpanded = false; onOpenFileClick() }
                    )
                    DropdownMenuItem(
                        text = { Text("File Explorer") },
                        onClick = { menuExpanded = false; onFileExplorerClick() }
                    )
                    DropdownMenuItem(
                        text = { Text("Rename / Change Extension") },
                        onClick = { menuExpanded = false; onTitleClick() }
                    )
                    DropdownMenuItem(
                        text = { Text("Save") },
                        onClick = { menuExpanded = false; onSaveClick() }
                    )
                    DropdownMenuItem(
                        text = { Text("Save As") },
                        onClick = { menuExpanded = false; onSaveAsClick() }
                    )
                    HorizontalDivider(color = dividerColor)
                    DropdownMenuItem(
                        text = { Text("Find & Replace") },
                        onClick = { menuExpanded = false; onReplaceClick() }
                    )
                    DropdownMenuItem(
                        text = { Text("Select All") },
                        onClick = { menuExpanded = false; onSelectAllClick() }
                    )
                    DropdownMenuItem(
                        text = { Text("AI Coding Assistant (✦)") },
                        onClick = { menuExpanded = false; onAiAssistantClick() }
                    )
                    DropdownMenuItem(
                        text = { Text("Toggle Dark / Light Theme") },
                        onClick = { menuExpanded = false; onThemeToggleClick() }
                    )
                    DropdownMenuItem(
                        text = { Text("File Info (ⓘ)") },
                        onClick = { menuExpanded = false; onInfoClick() }
                    )
                    DropdownMenuItem(
                        text = { Text("Settings") },
                        onClick = { menuExpanded = false; onSettingsClick() }
                    )
                    DropdownMenuItem(
                        text = { Text("About CPhone") },
                        onClick = { menuExpanded = false; onAboutClick() }
                    )
                }
            }
        }

        HorizontalDivider(
            color = dividerColor,
            thickness = 0.8.dp,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
