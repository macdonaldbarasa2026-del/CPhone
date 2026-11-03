package com.cphone.editor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cphone.editor.model.EditorTab

@Composable
fun EditorTabBar(
    tabs: List<EditorTab>,
    activeTabId: String,
    onTabSelected: (String) -> Unit,
    onTabClosed: (String) -> Unit,
    onNewTabClick: () -> Unit,
    isDarkMode: Boolean,
    modifier: Modifier = Modifier
) {
    val barBg = if (isDarkMode) Color(0xFF181818) else Color(0xFFF1F3F4)
    val activeTabBg = if (isDarkMode) Color(0xFF1E1E1E) else Color(0xFFFFFFFF)
    val inactiveTabBg = if (isDarkMode) Color(0xFF252526) else Color(0xFFE8EAED)
    val textColor = if (isDarkMode) Color(0xFFE0E0E0) else Color(0xFF202124)
    val dividerColor = if (isDarkMode) Color(0xFF2C2C2C) else Color(0xFFD3D3D3)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(38.dp)
            .background(barBg)
            .horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically
    ) {
        tabs.forEach { tab ->
            val isActive = tab.id == activeTabId
            val currentBg = if (isActive) activeTabBg else inactiveTabBg

            Row(
                modifier = Modifier
                    .fillMaxHeight()
                    .background(currentBg)
                    .clickable { onTabSelected(tab.id) }
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Modified dot indicator
                if (tab.isModified) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(Color(0xFF007AFF), shape = MaterialTheme.shapes.small)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }

                // File name
                Text(
                    text = tab.fileName,
                    fontSize = 13.sp,
                    fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Close button (if more than 1 tab)
                if (tabs.size > 1) {
                    IconButton(
                        onClick = { onTabClosed(tab.id) },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close tab",
                            tint = textColor.copy(alpha = 0.7f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            // Divider between tabs
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(dividerColor)
            )
        }

        // Add new tab button
        IconButton(
            onClick = onNewTabClick,
            modifier = Modifier
                .size(36.dp)
                .padding(horizontal = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "New Tab",
                tint = textColor,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
