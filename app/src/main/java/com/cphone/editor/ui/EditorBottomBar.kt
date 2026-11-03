package com.cphone.editor.ui

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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Phone-optimised 3-tier bottom action bar inspired by Acode and mobile IDEs.
 *
 * Row 1 – Editor actions: cursor movement (left, right, up, down), undo/redo, find, save, select all.
 * Row 2 – Special characters (brackets, parens, braces, quotes, semicolon).
 * Row 3 – Common operators and symbols (=, ==, !=, +, -, *, /, <, >, &, |, etc.).
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
    onMoveCursorUp: () -> Unit = {},
    onMoveCursorDown: () -> Unit = {},
    onSelectAll: () -> Unit = {},
    onInsertText: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val bg          = if (isDarkMode) Color(0xFF21252B) else Color(0xFFF0F4F8)
    val rowBg       = if (isDarkMode) Color(0xFF282C34) else Color(0xFFE8EDF4)
    val keyBg       = if (isDarkMode) Color(0xFF3E4451) else Color(0xFFDDE5EF)
    val iconColor   = if (isDarkMode) Color(0xFFABB2BF) else Color(0xFF37474F)
    val accentColor = Color(0xFF61AFEF)   // blue – braces/parens stand out
    val bracketColor= Color(0xFFC678DD)   // purple – square brackets
    val dividerColor= if (isDarkMode) Color(0xFF3E4451) else Color(0xFFCDD5E0)

    val row1Chars = listOf(
        "⇥ Tab" to "\t",
        "{" to "{",
        "}" to "}",
        "(" to "(",
        ")" to ")",
        "[" to "[",
        "]" to "]",
        ";" to ";",
        ":" to ":",
        "," to ",",
        "." to ".",
        "\"" to "\"",
        "'" to "'",
        "`" to "`"
    )
    val row2Chars = listOf(
        "=" to "=",
        "==" to "==",
        "!=" to "!=",
        "+=" to "+=",
        "-=" to "-=",
        "&&" to "&&",
        "||" to "||",
        "=>" to "=>",
        "->" to "->",
        "::" to "::",
        "!" to "!",
        "&" to "&",
        "|" to "|",
        "^" to "^",
        "~" to "~",
        "#" to "#",
        "@" to "@",
        "\$" to "\$",
        "%" to "%",
        "\\" to "\\",
        "/" to "/",
        "*" to "*",
        "+" to "+",
        "-" to "-",
        "<" to "<",
        ">" to ">",
        "_" to "_"
    )

    Surface(
        modifier  = modifier.fillMaxWidth(),
        color     = bg,
        shadowElevation = 8.dp
    ) {
        Column {
            HorizontalDivider(color = dividerColor, thickness = 1.dp)

            // ── Row A: Editor action icons ──────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(rowBg)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Cursor movement
                ActionIconBtn(Icons.Default.KeyboardArrowLeft,  "Move Left",  iconColor, onMoveCursorLeft)
                ActionIconBtn(Icons.Default.KeyboardArrowRight, "Move Right", iconColor, onMoveCursorRight)
                ActionIconBtn(Icons.Default.KeyboardArrowUp,    "Move Up",    iconColor, onMoveCursorUp)
                ActionIconBtn(Icons.Default.KeyboardArrowDown,  "Move Down",  iconColor, onMoveCursorDown)
                BarDivider(dividerColor)
                // Undo / Redo
                ActionIconBtn(Icons.AutoMirrored.Filled.Undo, "Undo", iconColor, onUndo)
                ActionIconBtn(Icons.AutoMirrored.Filled.Redo, "Redo", iconColor, onRedo)
                BarDivider(dividerColor)
                // Find & Save
                ActionIconBtn(Icons.Default.Search, "Find",  iconColor, onFind)
                ActionIconBtn(Icons.Default.Save,   "Save",  iconColor, onSave)
                BarDivider(dividerColor)
                // Select all
                ActionIconBtn(Icons.Default.SelectAll, "Select All", iconColor, onSelectAll)
            }

            HorizontalDivider(color = dividerColor, thickness = 0.5.dp)

            // ── Row B: Special chars — row 1 ───────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(bg)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 4.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                row1Chars.forEach { (label, value) ->
                    val textColor = when (label) {
                        "{", "}" -> accentColor
                        "(", ")" -> accentColor
                        "[", "]" -> bracketColor
                        else     -> iconColor
                    }
                    SpecialKey(label, keyBg, textColor) {
                        if (value == "\t") onTab() else onInsertText(value)
                    }
                }
            }

            HorizontalDivider(color = dividerColor, thickness = 0.5.dp)

            // ── Row C: Special chars — row 2 (operators) ───────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(bg)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 4.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                row2Chars.forEach { (label, value) ->
                    SpecialKey(label, keyBg, iconColor) { onInsertText(value) }
                }
            }
        }
    }
}

@Composable
private fun ActionIconBtn(
    icon: ImageVector,
    desc: String,
    tint: Color,
    onClick: () -> Unit
) {
    IconButton(
        onClick   = onClick,
        modifier  = Modifier.size(42.dp)
    ) {
        Icon(
            imageVector        = icon,
            contentDescription = desc,
            tint               = tint,
            modifier           = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun SpecialKey(
    label: String,
    bg: Color,
    textColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .height(34.dp)
            .background(bg, RoundedCornerShape(5.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text       = label,
            color      = textColor,
            fontSize   = 13.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun BarDivider(color: Color) {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(26.dp)
            .background(color)
    )
}
