package com.cphone.editor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SearchReplaceBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    replaceQuery: String,
    onReplaceQueryChange: (String) -> Unit,
    isReplaceMode: Boolean,
    matchCount: Int,
    currentMatchIndex: Int,
    onFindNext: () -> Unit,
    onFindPrevious: () -> Unit,
    onReplace: () -> Unit,
    onReplaceAll: () -> Unit,
    onClose: () -> Unit,
    isDarkMode: Boolean,
    modifier: Modifier = Modifier
) {
    val barBg = if (isDarkMode) Color(0xFF252526) else Color(0xFFF3F3F3)
    val inputBg = if (isDarkMode) Color(0xFF3C3C3C) else Color(0xFFFFFFFF)
    val textColor = if (isDarkMode) Color(0xFFFFFFFF) else Color(0xFF202124)
    val hintColor = if (isDarkMode) Color(0xFF888888) else Color(0xFF9E9E9E)
    val iconColor = if (isDarkMode) Color(0xFFCCCCCC) else Color(0xFF555555)

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = barBg,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            // Find Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .background(inputBg, shape = MaterialTheme.shapes.extraSmall)
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (searchQuery.isEmpty()) {
                        Text("Find...", color = hintColor, fontSize = 14.sp)
                    }
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        singleLine = true,
                        textStyle = TextStyle(color = textColor, fontSize = 14.sp),
                        cursorBrush = SolidColor(textColor),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Match count
                val countText = if (searchQuery.isNotEmpty()) {
                    if (matchCount > 0) "${currentMatchIndex + 1}/$matchCount" else "0/0"
                } else ""

                if (countText.isNotEmpty()) {
                    Text(
                        text = countText,
                        color = hintColor,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 6.dp)
                    )
                }

                // Up / Down navigation
                IconButton(onClick = onFindPrevious, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "Previous match",
                        tint = iconColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(onClick = onFindNext, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Next match",
                        tint = iconColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Close button
                IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close search",
                        tint = iconColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Replace Row (if enabled)
            if (isReplaceMode) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .background(inputBg, shape = MaterialTheme.shapes.extraSmall)
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (replaceQuery.isEmpty()) {
                            Text("Replace with...", color = hintColor, fontSize = 14.sp)
                        }
                        BasicTextField(
                            value = replaceQuery,
                            onValueChange = onReplaceQueryChange,
                            singleLine = true,
                            textStyle = TextStyle(color = textColor, fontSize = 14.sp),
                            cursorBrush = SolidColor(textColor),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    FilledTonalButton(
                        onClick = onReplace,
                        modifier = Modifier.height(34.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp)
                    ) {
                        Text("Replace", fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    FilledTonalButton(
                        onClick = onReplaceAll,
                        modifier = Modifier.height(34.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp)
                    ) {
                        Text("All", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
