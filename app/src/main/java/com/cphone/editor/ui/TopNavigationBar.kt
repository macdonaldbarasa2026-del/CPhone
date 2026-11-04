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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cphone.editor.R
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
    onTerminalClick: () -> Unit = {},
    onTitleClick: () -> Unit = {},
    onBugPillClick: () -> Unit = {},
    onCommandPaletteClick: () -> Unit = {},
    onToggleZenClick: () -> Unit = {},
    onFormatClick: () -> Unit = {},
    onSnippetsClick: () -> Unit = {},
    onGoToLineClick: () -> Unit = {},
    onPasteClick: () -> Unit = {},
    onCopyClick: () -> Unit = {},
    onCutClick: () -> Unit = {},
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
    var pencilMenuExpanded by remember { mutableStateOf(false) }

    val palette = remember(themeId, isDarkMode) {
        SyntaxHighlighter.resolvePalette(themeId, isDarkMode)
    }

    val barBg = if (isDarkMode) palette.topBarBg else Color.White
    val contentColor = if (isDarkMode) Color(0xFFF3F4F6) else Color(0xFF111827)
    val subtitleColor = if (isDarkMode) Color(0xFF9CA3AF) else Color(0xFF6B7280)
    val dividerColor = palette.borderColor
    val menuBgColor = if (isDarkMode) Color(0xFF252936) else Color(0xFFFFFFFF)
    val menuTextColor = if (isDarkMode) Color(0xFFF9FAFB) else Color(0xFF111827)
    val menuItemColors = MenuDefaults.itemColors(
        textColor = menuTextColor,
        leadingIconColor = menuTextColor,
        trailingIconColor = menuTextColor
    )

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
            // Left House Icon (ic_home_outline) replacing ☰ as requested in Image 2
            IconButton(
                onClick = onHomeClick,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_home_outline),
                    contentDescription = "Home / File Explorer",
                    tint = contentColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(2.dp))

            // Language Icon + Two-line File Title ("index.html" / "New file")
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

                Spacer(modifier = Modifier.width(8.dp))

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

            // Play / Run / Quick Compile button when runnable
            if (isRunnable) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF4CAF50).copy(alpha = 0.16f),
                    modifier = Modifier.size(36.dp)
                ) {
                    IconButton(
                        onClick = onPlayClick,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Run / Quick Compile",
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(3.dp))
            }

            // Terminal / Termux Shell Button
            IconButton(
                onClick = onTerminalClick,
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Terminal,
                    contentDescription = "Terminal / Termux Shell",
                    tint = contentColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Pencil / Quick Edit Menu (Paste, Copy, Cut, Select All, Go to Line, Format)
            Box {
                IconButton(
                    onClick = { pencilMenuExpanded = true },
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Tools (Paste, Copy, Cut, Select All, Go to Line, Format)",
                        tint = contentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                DropdownMenu(
                    expanded = pencilMenuExpanded,
                    onDismissRequest = { pencilMenuExpanded = false },
                    modifier = Modifier.background(menuBgColor)
                ) {
                    DropdownMenuItem(
                        text = { Text("Paste", color = menuTextColor, fontWeight = FontWeight.Medium) },
                        leadingIcon = {
                            Icon(Icons.Default.ContentPaste, contentDescription = null, tint = menuTextColor, modifier = Modifier.size(18.dp))
                        },
                        colors = menuItemColors,
                        onClick = { pencilMenuExpanded = false; onPasteClick() }
                    )
                    DropdownMenuItem(
                        text = { Text("Copy", color = menuTextColor, fontWeight = FontWeight.Medium) },
                        leadingIcon = {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, tint = menuTextColor, modifier = Modifier.size(18.dp))
                        },
                        colors = menuItemColors,
                        onClick = { pencilMenuExpanded = false; onCopyClick() }
                    )
                    DropdownMenuItem(
                        text = { Text("Cut", color = menuTextColor, fontWeight = FontWeight.Medium) },
                        leadingIcon = {
                            Icon(Icons.Default.ContentCut, contentDescription = null, tint = menuTextColor, modifier = Modifier.size(18.dp))
                        },
                        colors = menuItemColors,
                        onClick = { pencilMenuExpanded = false; onCutClick() }
                    )
                    DropdownMenuItem(
                        text = { Text("Select All", color = menuTextColor, fontWeight = FontWeight.Medium) },
                        leadingIcon = {
                            Icon(Icons.Default.SelectAll, contentDescription = null, tint = menuTextColor, modifier = Modifier.size(18.dp))
                        },
                        colors = menuItemColors,
                        onClick = { pencilMenuExpanded = false; onSelectAllClick() }
                    )
                    HorizontalDivider(color = dividerColor)
                    DropdownMenuItem(
                        text = { Text("Go to Line", color = menuTextColor, fontWeight = FontWeight.Medium) },
                        leadingIcon = {
                            Icon(Icons.Default.FormatListNumbered, contentDescription = null, tint = menuTextColor, modifier = Modifier.size(18.dp))
                        },
                        colors = menuItemColors,
                        onClick = { pencilMenuExpanded = false; onGoToLineClick() }
                    )
                    DropdownMenuItem(
                        text = { Text("Format", color = menuTextColor, fontWeight = FontWeight.Medium) },
                        leadingIcon = {
                            Icon(Icons.Default.AutoFixHigh, contentDescription = null, tint = menuTextColor, modifier = Modifier.size(18.dp))
                        },
                        colors = menuItemColors,
                        onClick = { pencilMenuExpanded = false; onFormatClick() }
                    )
                    HorizontalDivider(color = dividerColor)
                    DropdownMenuItem(
                        text = { Text("Rename File", color = menuTextColor, fontWeight = FontWeight.Medium) },
                        leadingIcon = {
                            Icon(Icons.Default.DriveFileRenameOutline, contentDescription = null, tint = menuTextColor, modifier = Modifier.size(18.dp))
                        },
                        colors = menuItemColors,
                        onClick = { pencilMenuExpanded = false; onTitleClick() }
                    )
                }
            }

            // Theme Droplet Icon (ic_theme_droplet) circled in Image 2 for instant theme change
            IconButton(
                onClick = onThemeToggleClick,
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_theme_droplet),
                    contentDescription = "Change Theme (Dark / Light)",
                    tint = contentColor,
                    modifier = Modifier.size(21.dp)
                )
            }

            // Three-dot overflow menu with high-contrast visible text (fixing Image 1)
            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.size(38.dp)
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
                    modifier = Modifier.background(menuBgColor)
                ) {
                    if (isRunnable) {
                        DropdownMenuItem(
                            text = { Text("Run / Compile (▶)", color = menuTextColor) },
                            colors = menuItemColors,
                            onClick = { menuExpanded = false; onPlayClick() }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Terminal / Termux Shell ( >_ )", color = menuTextColor, fontWeight = FontWeight.Medium) },
                        leadingIcon = {
                            Icon(Icons.Default.Terminal, contentDescription = null, tint = Color(0xFF61AFEF), modifier = Modifier.size(20.dp))
                        },
                        colors = menuItemColors,
                        onClick = { menuExpanded = false; onTerminalClick() }
                    )
                    DropdownMenuItem(
                        text = { Text("Command Palette (⌘)", color = menuTextColor) },
                        colors = menuItemColors,
                        onClick = { menuExpanded = false; onCommandPaletteClick() }
                    )
                    DropdownMenuItem(
                        text = {
                            val badge = if (bugCount + warningCount > 0) " (${bugCount + warningCount})" else ""
                            Text("Problems & Code Trails$badge", color = menuTextColor)
                        },
                        colors = menuItemColors,
                        onClick = { menuExpanded = false; onBugPillClick() }
                    )
                    DropdownMenuItem(
                        text = { Text("Format / Beautify Code", color = menuTextColor) },
                        colors = menuItemColors,
                        onClick = { menuExpanded = false; onFormatClick() }
                    )
                    DropdownMenuItem(
                        text = { Text("Insert Code Snippet", color = menuTextColor) },
                        colors = menuItemColors,
                        onClick = { menuExpanded = false; onSnippetsClick() }
                    )
                    DropdownMenuItem(
                        text = { Text("Go to Line...", color = menuTextColor) },
                        colors = menuItemColors,
                        onClick = { menuExpanded = false; onGoToLineClick() }
                    )
                    DropdownMenuItem(
                        text = { Text(if (isZenMode) "Exit Fullscreen Mode" else "Fullscreen Code Mode", color = menuTextColor) },
                        colors = menuItemColors,
                        onClick = { menuExpanded = false; onToggleZenClick() }
                    )
                    HorizontalDivider(color = dividerColor)
                    DropdownMenuItem(
                        text = { Text("New File", color = menuTextColor) },
                        colors = menuItemColors,
                        onClick = { menuExpanded = false; onNewFileClick() }
                    )
                    DropdownMenuItem(
                        text = { Text("Open File (Storage)", color = menuTextColor) },
                        colors = menuItemColors,
                        onClick = { menuExpanded = false; onOpenFileClick() }
                    )
                    DropdownMenuItem(
                        text = { Text("File Explorer", color = menuTextColor) },
                        colors = menuItemColors,
                        onClick = { menuExpanded = false; onFileExplorerClick() }
                    )
                    DropdownMenuItem(
                        text = { Text("Rename / Change Extension", color = menuTextColor) },
                        colors = menuItemColors,
                        onClick = { menuExpanded = false; onTitleClick() }
                    )
                    DropdownMenuItem(
                        text = { Text("Save", color = menuTextColor) },
                        colors = menuItemColors,
                        onClick = { menuExpanded = false; onSaveClick() }
                    )
                    DropdownMenuItem(
                        text = { Text("Save As", color = menuTextColor) },
                        colors = menuItemColors,
                        onClick = { menuExpanded = false; onSaveAsClick() }
                    )
                    HorizontalDivider(color = dividerColor)
                    DropdownMenuItem(
                        text = { Text("Find & Replace", color = menuTextColor) },
                        colors = menuItemColors,
                        onClick = { menuExpanded = false; onReplaceClick() }
                    )
                    DropdownMenuItem(
                        text = { Text("Select All", color = menuTextColor) },
                        colors = menuItemColors,
                        onClick = { menuExpanded = false; onSelectAllClick() }
                    )
                    DropdownMenuItem(
                        text = { Text("AI Coding Assistant (✦)", color = menuTextColor) },
                        colors = menuItemColors,
                        onClick = { menuExpanded = false; onAiAssistantClick() }
                    )
                    DropdownMenuItem(
                        text = { Text("Toggle Dark / Light Theme", color = menuTextColor) },
                        colors = menuItemColors,
                        onClick = { menuExpanded = false; onThemeToggleClick() }
                    )
                    DropdownMenuItem(
                        text = { Text("File Info (ⓘ)", color = menuTextColor) },
                        colors = menuItemColors,
                        onClick = { menuExpanded = false; onInfoClick() }
                    )
                    DropdownMenuItem(
                        text = { Text("Settings", color = menuTextColor) },
                        colors = menuItemColors,
                        onClick = { menuExpanded = false; onSettingsClick() }
                    )
                    DropdownMenuItem(
                        text = { Text("About CPhone", color = menuTextColor) },
                        colors = menuItemColors,
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
