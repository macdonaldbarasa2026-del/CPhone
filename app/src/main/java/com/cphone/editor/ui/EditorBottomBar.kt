package com.cphone.editor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Bottom action bar — gives quick access to editor actions that are hard on mobile keyboards.
 * Like VS Code's bottom toolbar but optimised for touch.
 */
@Composable
fun EditorBottomBar(
    isDarkMode: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onTab: () -> Unit,
    onFind: () -> Unit,
    onSave: () -> Unit,
    onMoveCursorLeft: () -> Unit,
    onMoveCursorRight: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (isDarkMode) Color(0xFF252526) else Color(0xFFF0F4F8)
    val iconColor = if (isDarkMode) Color(0xFFCCCCCC) else Color(0xFF37474F)
    val dividerColor = if (isDarkMode) Color(0xFF333333) else Color(0xFFDDE4EC)

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = bg,
        shadowElevation = 8.dp
    ) {
        Column {
            HorizontalDivider(color = dividerColor, thickness = 1.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BottomBarButton("←", iconColor, onMoveCursorLeft)
                BottomBarButton("→", iconColor, onMoveCursorRight)
                BottomBarDivider(dividerColor)
                BottomBarIconButton(Icons.AutoMirrored.Filled.Undo, "Undo", iconColor, onUndo)
                BottomBarIconButton(Icons.AutoMirrored.Filled.Redo, "Redo", iconColor, onRedo)
                BottomBarDivider(dividerColor)
                BottomBarButton("⇥ Tab", iconColor, onTab)
                BottomBarButton("{}", iconColor) { /* bracket pair */ }
                BottomBarButton("\"\"", iconColor) { /* quote wrap */ }
                BottomBarDivider(dividerColor)
                BottomBarIconButton(Icons.Default.Search, "Find", iconColor, onFind)
                BottomBarIconButton(Icons.Default.Save, "Save", iconColor, onSave)
            }
        }
    }
}

@Composable
private fun BottomBarButton(label: String, color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(40.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = color,
            fontSize = 14.sp,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
        )
    }
}

@Composable
private fun BottomBarIconButton(icon: ImageVector, desc: String, color: Color, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(40.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = desc,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun BottomBarDivider(color: Color) {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(24.dp)
            .background(color)
    )
}
