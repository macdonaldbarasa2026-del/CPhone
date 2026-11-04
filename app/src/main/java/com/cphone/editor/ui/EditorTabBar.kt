package com.cphone.editor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cphone.editor.model.EditorTab
import com.cphone.editor.model.SupportedLanguage
import com.cphone.editor.syntax.SyntaxHighlighter
import com.cphone.editor.tools.CodeSymbol

@Composable
fun EditorTabBar(
    tabs: List<EditorTab>,
    activeTabId: String,
    onTabSelected: (String) -> Unit,
    onTabClosed: (String) -> Unit,
    onNewTabClick: () -> Unit,
    isDarkMode: Boolean,
    themeId: String = "one_dark",
    showCodeTrail: Boolean = true,
    activeSymbol: CodeSymbol? = null,
    cursorLine: Int = 1,
    cursorColumn: Int = 1,
    fontSizeSp: Float = 13.5f,
    canNavigateBack: Boolean = false,
    canNavigateForward: Boolean = false,
    onNavigateBack: () -> Unit = {},
    onNavigateForward: () -> Unit = {},
    onOpenOutline: () -> Unit = {},
    onOpenGoToLine: () -> Unit = {},
    onFontSizeDelta: (Float) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val palette = remember(themeId, isDarkMode) {
        SyntaxHighlighter.resolvePalette(themeId, isDarkMode)
    }

    val barBg = if (isDarkMode) palette.tabBarBg else Color(0xFFF9FAFB)
    val activeTabBg = if (isDarkMode) Color(0xFF2C313C) else Color(0xFFD6DCE5)
    val inactiveTabBg = if (isDarkMode) palette.tabBarBg else Color(0xFFFFFFFF)
    val textColor = if (isDarkMode) palette.syntax.defaultText else Color(0xFF1F2937)
    val dividerColor = palette.borderColor
    val activeIndicatorBlue = Color(0xFF2196F3)
    val closeCircleBg = if (isDarkMode) Color(0xFF4B5563) else Color(0xFF1F2937)

    Column(modifier = modifier.fillMaxWidth()) {
        // ── Acode-Style Multi-File Tab Strip matching the screenshot ─────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp)
                .background(barBg)
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEach { tab ->
                val isActive = tab.id == activeTabId
                val currentBg = if (isActive) activeTabBg else inactiveTabBg
                val resolvedLang = remember(tab.fileName, tab.language) {
                    val detected = SupportedLanguage.detect(tab.fileName)
                    if (detected != SupportedLanguage.PLAINTEXT) detected else tab.language
                }

                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .widthIn(min = 110.dp, max = 165.dp)
                        .background(currentBg)
                        .clickable { onTabSelected(tab.id) }
                ) {
                    // Top blue accent bar on the active tab (exact match to screenshot)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.5.dp)
                            .background(if (isActive) activeIndicatorBlue else Color.Transparent)
                    )

                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Small yellow/amber dot on the left when modified or unsaved new file
                        if (tab.isModified || tab.uri == null) {
                            Box(
                                modifier = Modifier
                                    .size(5.5.dp)
                                    .background(Color(0xFFF59E0B), shape = CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }

                        // Authentic Language Icon (HTML5 '5' shield, 'C++', Kotlin 'K', Python, etc.)
                        LanguageFileIcon(
                            language = resolvedLang,
                            size = 16.dp
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        Text(
                            text = tab.fileName,
                            fontSize = 13.sp,
                            fontWeight = if (isActive) FontWeight.Medium else FontWeight.Normal,
                            color = textColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        // Dark circular close button (⊗) matching the screenshot
                        Box(
                            modifier = Modifier
                                .size(15.dp)
                                .background(closeCircleBg, shape = CircleShape)
                                .clickable { onTabClosed(tab.id) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close tab",
                                tint = Color.White,
                                modifier = Modifier.size(10.dp)
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .width(0.8.dp)
                        .fillMaxHeight()
                        .background(dividerColor)
                )
            }

            // New tab '+' button at end of tab strip
            IconButton(
                onClick = onNewTabClick,
                modifier = Modifier
                    .size(34.dp)
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

        HorizontalDivider(color = dividerColor, thickness = 0.8.dp)
    }
}
