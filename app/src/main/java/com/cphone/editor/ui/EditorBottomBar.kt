package com.cphone.editor.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cphone.editor.syntax.SyntaxHighlighter

/**
 * Acode-style Bottom Bar + Revealable Symbol Rows (`{}`, `[]`, `()`, etc.).
 *
 * - By default, `isSymbolsRevealed` is `false` (hidden!) so only the clean bottom bar
 *   (`CTRL`, `→|`, `SHFT`, `↶`, `↷`, `🔍`, `💾`, `ESC`) is shown.
 * - When the user taps the circular `^` button at the bottom-right, `isSymbolsRevealed`
 *   becomes `true` and reveals the symbol buttons (`{`, `}`, `[`, `]`, `(`, `)`, `<`, `>`, etc.).
 * - Tapping the circular button again hides the symbol buttons back to the default state.
 */
@Composable
fun EditorBottomBar(
    isDarkMode: Boolean,
    themeId: String = "one_dark",
    isSymbolsRevealed: Boolean = false,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onTab: () -> Unit,
    onFind: () -> Unit,
    onSave: () -> Unit,
    onEsc: () -> Unit = {},
    onCtrlShortcut: () -> Unit = {},
    onMoveCursorLeft: () -> Unit,
    onMoveCursorRight: () -> Unit,
    onMoveCursorUp: () -> Unit = {},
    onMoveCursorDown: () -> Unit = {},
    onMoveLineStart: () -> Unit = {},
    onMoveLineEnd: () -> Unit = {},
    onToggleComment: () -> Unit = {},
    onDuplicateLine: () -> Unit = {},
    onDeleteLine: () -> Unit = {},
    onFormatCode: () -> Unit = {},
    onOpenSnippets: () -> Unit = {},
    onSelectAll: () -> Unit = {},
    onInsertText: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var ctrlActive by remember { mutableStateOf(false) }
    var shiftActive by remember { mutableStateOf(false) }

    val palette = remember(themeId, isDarkMode) {
        SyntaxHighlighter.resolvePalette(themeId, isDarkMode)
    }

    val barBg        = if (isDarkMode) palette.bottomDockBg else Color.White
    val symbolRowBg  = if (isDarkMode) palette.topBarBg else Color(0xFFF3F4F6)
    val keyBg        = if (isDarkMode) palette.activeLineBg else Color(0xFFE5E7EB)
    val textColor    = if (isDarkMode) palette.syntax.defaultText else Color(0xFF111827)
    val accentColor  = palette.accentColor
    val dividerColor = palette.borderColor

    val symbolRow1 = listOf(
        "{" to "{",
        "}" to "}",
        "[" to "[",
        "]" to "]",
        "(" to "(",
        ")" to ")",
        "<" to "<",
        ">" to ">",
        ";" to ";",
        ":" to ":",
        "\"" to "\"",
        "'" to "'",
        "=" to "=",
        "+" to "+",
        "-" to "-",
        "*" to "*",
        "/" to "/",
        "\\" to "\\"
    )

    val symbolRow2 = listOf(
        "&" to "&",
        "|" to "|",
        "!" to "!",
        "?" to "?",
        "#" to "#",
        "$" to "$",
        "_" to "_",
        "." to ".",
        "," to ",",
        "`" to "`",
        "~" to "~",
        "^" to "^",
        "%" to "%",
        "@" to "@",
        "==" to "==",
        "!=" to "!=",
        "->" to "->",
        "=>" to "=>"
    )

    Surface(
        modifier        = modifier.fillMaxWidth(),
        color           = barBg,
        shadowElevation = 8.dp
    ) {
        Column {
            HorizontalDivider(color = dividerColor, thickness = 0.8.dp)

            // ── Revealable Symbol Rows (Hidden by default; revealed when bottom-right '^' is tapped) ──
            AnimatedVisibility(
                visible = isSymbolsRevealed,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(symbolRowBg)
                ) {
                    // Row 1: Primary Brackets & Code Symbols: { } [ ] ( ) < > ; : " ' = + - * /
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp)
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        symbolRow1.forEach { (label, value) ->
                            val isBracket = label in listOf("{", "}", "[", "]", "(", ")", "<", ">")
                            SymbolButton(
                                label = label,
                                bg = keyBg,
                                textColor = if (isBracket) accentColor else textColor,
                                onClick = { onInsertText(value) }
                            )
                        }
                    }

                    HorizontalDivider(color = dividerColor, thickness = 0.5.dp)

                    // Row 2: Cursor Arrows + Operators & Quick Line Tools
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp)
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        SymbolButton("←", keyBg, accentColor, onMoveCursorLeft)
                        SymbolButton("↓", keyBg, accentColor, onMoveCursorDown)
                        SymbolButton("↑", keyBg, accentColor, onMoveCursorUp)
                        SymbolButton("→", keyBg, accentColor, onMoveCursorRight)
                        SymbolButton("Home", keyBg, textColor, onMoveLineStart)
                        SymbolButton("End", keyBg, textColor, onMoveLineEnd)
                        SymbolButton("//", keyBg, accentColor, onToggleComment)
                        SymbolButton("Dup", keyBg, textColor, onDuplicateLine)
                        SymbolButton("Fmt", keyBg, Color(0xFF10B981), onFormatCode)

                        symbolRow2.forEach { (label, value) ->
                            SymbolButton(
                                label = label,
                                bg = keyBg,
                                textColor = textColor,
                                onClick = { onInsertText(value) }
                            )
                        }
                    }

                    HorizontalDivider(color = dividerColor, thickness = 0.8.dp)
                }
            }

            // ── Default Bottom Bar matching screenshot: CTRL | →| | SHFT | ↶ | ↷ | 🔍 | 💾 | ESC ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .background(barBg)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // CTRL button
                BottomBarTextAction(
                    label = "CTRL",
                    isActive = ctrlActive,
                    textColor = textColor,
                    accentColor = accentColor,
                    onClick = {
                        ctrlActive = !ctrlActive
                        if (ctrlActive) onCtrlShortcut()
                    }
                )

                // →| (Tab) icon button
                BottomBarTextAction(
                    label = "→|",
                    isActive = false,
                    textColor = textColor,
                    accentColor = accentColor,
                    onClick = onTab
                )

                // SHFT button
                BottomBarTextAction(
                    label = "SHFT",
                    isActive = shiftActive,
                    textColor = textColor,
                    accentColor = accentColor,
                    onClick = {
                        shiftActive = !shiftActive
                        if (shiftActive) onSelectAll()
                    }
                )

                // ↶ Undo
                BottomBarIconAction(
                    icon = Icons.AutoMirrored.Filled.Undo,
                    desc = "Undo",
                    tint = textColor,
                    onClick = onUndo
                )

                // ↷ Redo
                BottomBarIconAction(
                    icon = Icons.AutoMirrored.Filled.Redo,
                    desc = "Redo",
                    tint = textColor,
                    onClick = onRedo
                )

                // 🔍 Search
                BottomBarIconAction(
                    icon = Icons.Default.Search,
                    desc = "Search",
                    tint = textColor,
                    onClick = onFind
                )

                // 💾 Save
                BottomBarIconAction(
                    icon = Icons.Default.Save,
                    desc = "Save",
                    tint = textColor,
                    onClick = onSave
                )

                // ESC button
                BottomBarTextAction(
                    label = "ESC",
                    isActive = false,
                    textColor = textColor,
                    accentColor = accentColor,
                    onClick = {
                        ctrlActive = false
                        shiftActive = false
                        onEsc()
                    }
                )
            }
        }
    }
}

@Composable
private fun BottomBarTextAction(
    label: String,
    isActive: Boolean,
    textColor: Color,
    accentColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .height(36.dp)
            .defaultMinSize(minWidth = 42.dp)
            .background(
                color = if (isActive) accentColor.copy(alpha = 0.18f) else Color.Transparent,
                shape = RoundedCornerShape(6.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isActive) accentColor else textColor,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.SansSerif
        )
    }
}

@Composable
private fun BottomBarIconAction(
    icon: ImageVector,
    desc: String,
    tint: Color,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(38.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = desc,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun SymbolButton(
    label: String,
    bg: Color,
    textColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .height(30.dp)
            .defaultMinSize(minWidth = 34.dp)
            .background(bg, RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 13.5.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
    }
}
