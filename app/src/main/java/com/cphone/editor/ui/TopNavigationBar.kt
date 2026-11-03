package com.cphone.editor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Info
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

@Composable
fun TopNavigationBar(
    fileName: String,
    isDarkMode: Boolean,
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

    val barBg = if (isDarkMode) Color(0xFF1E1E1E) else Color(0xFFFFFFFF)
    val contentColor = if (isDarkMode) Color(0xFFE0E0E0) else Color(0xFF202124)
    val dividerColor = if (isDarkMode) Color(0xFF2C2C2C) else Color(0xFFE8EAED)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(barBg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Home icon on the far left
            IconButton(
                onClick = onHomeClick,
                modifier = Modifier.size(42.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_home_outline),
                    contentDescription = "Home / File Explorer",
                    tint = contentColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Current filename center-left with ellipsis
            Text(
                text = fileName,
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 6.dp)
            )

            // AI Assistant action icon
            IconButton(
                onClick = onAiAssistantClick,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "AI Coding Assistant",
                    tint = Color(0xFF007AFF),
                    modifier = Modifier.size(22.dp)
                )
            }

            // Info (ⓘ) icon
            IconButton(
                onClick = onInfoClick,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = "File Info",
                    tint = contentColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Theme toggle icon (droplet)
            IconButton(
                onClick = onThemeToggleClick,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_theme_droplet),
                    contentDescription = "Toggle Theme",
                    tint = contentColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Three-dot overflow menu on the far right
            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More Options",
                        tint = contentColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    modifier = Modifier.background(if (isDarkMode) Color(0xFF2A2A2A) else Color(0xFFFFFFFF))
                ) {
                    DropdownMenuItem(
                        text = { Text("New File") },
                        onClick = { menuExpanded = false; onNewFileClick() }
                    )
                    DropdownMenuItem(
                        text = { Text("Open File") },
                        onClick = { menuExpanded = false; onOpenFileClick() }
                    )
                    DropdownMenuItem(
                        text = { Text("File Explorer") },
                        onClick = { menuExpanded = false; onFileExplorerClick() }
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
                        text = { Text("Find") },
                        onClick = { menuExpanded = false; onFindClick() }
                    )
                    DropdownMenuItem(
                        text = { Text("Replace") },
                        onClick = { menuExpanded = false; onReplaceClick() }
                    )
                    DropdownMenuItem(
                        text = { Text("Select All") },
                        onClick = { menuExpanded = false; onSelectAllClick() }
                    )
                    HorizontalDivider(color = dividerColor)
                    DropdownMenuItem(
                        text = { Text("Undo") },
                        onClick = { menuExpanded = false; onUndoClick() }
                    )
                    DropdownMenuItem(
                        text = { Text("Redo") },
                        onClick = { menuExpanded = false; onRedoClick() }
                    )
                    HorizontalDivider(color = dividerColor)
                    DropdownMenuItem(
                        text = { Text("AI Assistant") },
                        onClick = { menuExpanded = false; onAiAssistantClick() }
                    )
                    DropdownMenuItem(
                        text = { Text("Settings") },
                        onClick = { menuExpanded = false; onSettingsClick() }
                    )
                    DropdownMenuItem(
                        text = { Text("About") },
                        onClick = { menuExpanded = false; onAboutClick() }
                    )
                }
            }
        }

        // Subtle light-grey divider underneath
        HorizontalDivider(
            color = dividerColor,
            thickness = 1.dp,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
