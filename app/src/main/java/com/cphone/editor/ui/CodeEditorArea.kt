package com.cphone.editor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cphone.editor.model.EditorSettings
import com.cphone.editor.model.SupportedLanguage
import com.cphone.editor.syntax.SyntaxHighlighter
import kotlin.math.max

@Composable
fun CodeEditorArea(
    textFieldValue: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    language: SupportedLanguage,
    settings: EditorSettings,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val isDarkMode = settings.isDarkMode

    // Exact alternating background row colors from original reference screenshot
    // Line 1 (even index 0) = pure white, Line 2 (odd index 1) = subtle pale ice blue
    val rowEvenColor = if (isDarkMode) Color(0xFF1E1E1E) else Color(0xFFFFFFFF)
    val rowOddColor = if (isDarkMode) Color(0xFF242830) else Color(0xFFEEF5FC)
    val gutterTextColor = if (isDarkMode) Color(0xFF757575) else Color(0xFF8E99A2)
    val gutterDividerColor = if (isDarkMode) Color(0xFF2C2C2C) else Color(0xFFE2E8F0)
    val blueIndicatorColor = Color(0xFF007AFF) // Clean vibrant blue left indicator
    val cursorColor = if (isDarkMode) Color(0xFF4FC3F7) else Color(0xFF007AFF)

    // Dynamic line count calculation
    val text = textFieldValue.text
    val lineCount = remember(text) {
        if (text.isEmpty()) 1 else text.count { it == '\n' } + 1
    }

    val verticalScrollState = rememberScrollState()
    val horizontalScrollState = rememberScrollState()

    val codeFontSize = settings.fontSizeSp.sp
    val lineHeight = (settings.fontSizeSp * 1.55f).sp
    val topPaddingDp = 10.dp
    val bottomPaddingDp = 48.dp

    val lineHeightPx = with(density) { lineHeight.toPx() }
    val topPaddingPx = with(density) { topPaddingDp.toPx() }

    // Consistent TextStyle with explicit LineHeightStyle and includeFontPadding = false
    val commonTextStyle = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontSize = codeFontSize,
        lineHeight = lineHeight,
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.None
        ),
        platformStyle = PlatformTextStyle(includeFontPadding = false)
    )

    // Syntax highlighting visual transformation
    val visualTransformation = remember(language, isDarkMode, text) {
        val highlighted = SyntaxHighlighter.highlight(text, language, isDarkMode)
        VisualTransformation {
            TransformedText(highlighted, OffsetMapping.Identity)
        }
    }

    Row(
        modifier = modifier
            .fillMaxSize()
            .background(rowEvenColor)
    ) {
        // 1. Blue vertical indicator along the extreme left edge
        Box(
            modifier = Modifier
                .width(10.dp)
                .fillMaxHeight()
                .background(blueIndicatorColor)
        )

        // 2. Gutter and Editor area with dynamic alternating horizontal background bands
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .verticalScroll(verticalScrollState)
                .drawBehind {
                    if (settings.alternatingRowBands && lineHeightPx > 0) {
                        // Calculate total rows to render (fill entire screen plus buffer)
                        val visibleLines = max(lineCount, (size.height / lineHeightPx).toInt() + 10)
                        for (i in 0 until visibleLines) {
                            val y = topPaddingPx + i * lineHeightPx
                            val rowColor = if (i % 2 == 1) rowOddColor else rowEvenColor
                            // Render subtle horizontal band extending across full editor width
                            drawRect(
                                color = rowColor,
                                topLeft = Offset(0f, y),
                                size = Size(size.width, lineHeightPx)
                            )
                        }
                    }
                }
        ) {
            // Line numbers gutter
            if (settings.showLineNumbers) {
                val gutterWidth = when {
                    lineCount < 100 -> 38.dp
                    lineCount < 1000 -> 48.dp
                    else -> 60.dp
                }

                val lineNumbersText = remember(lineCount) {
                    (1..lineCount).joinToString("\n")
                }

                Box(
                    modifier = Modifier
                        .width(gutterWidth)
                        .padding(top = topPaddingDp, bottom = bottomPaddingDp, end = 8.dp)
                ) {
                    Text(
                        text = lineNumbersText,
                        color = gutterTextColor,
                        style = commonTextStyle.copy(textAlign = TextAlign.End),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Subtle vertical divider line separating gutter from code area
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(gutterDividerColor)
                )
            }

            // Code Editing Area
            val editorModifier = if (settings.wordWrap) {
                Modifier
                    .weight(1f)
                    .padding(top = topPaddingDp, bottom = bottomPaddingDp, start = 8.dp, end = 16.dp)
            } else {
                Modifier
                    .weight(1f)
                    .horizontalScroll(horizontalScrollState)
                    .padding(top = topPaddingDp, bottom = bottomPaddingDp, start = 8.dp, end = 24.dp)
                    .defaultMinSize(minWidth = 900.dp)
            }

            BasicTextField(
                value = textFieldValue,
                onValueChange = { newValue ->
                    val handledValue = handleAutoIndent(textFieldValue, newValue, settings.tabSize)
                    onValueChange(handledValue)
                },
                modifier = editorModifier,
                textStyle = commonTextStyle.copy(
                    color = if (isDarkMode) Color(0xFFECEFF1) else Color(0xFF212121)
                ),
                cursorBrush = SolidColor(cursorColor),
                visualTransformation = visualTransformation
            )
        }
    }
}

// Auto-indentation logic
private fun handleAutoIndent(
    oldValue: TextFieldValue,
    newValue: TextFieldValue,
    tabSize: Int
): TextFieldValue {
    val oldText = oldValue.text
    val newText = newValue.text

    // Check if user pressed Enter (newline added)
    if (newText.length == oldText.length + 1 && newValue.selection.collapsed) {
        val cursor = newValue.selection.start
        if (cursor > 0 && newText[cursor - 1] == '\n') {
            val prevLineStart = newText.lastIndexOf('\n', cursor - 2).let { if (it == -1) 0 else it + 1 }
            val prevLine = newText.substring(prevLineStart, cursor - 1)

            val indent = prevLine.takeWhile { it == ' ' || it == '\t' }
            val extraIndent = if (prevLine.trimEnd().endsWith('{') || prevLine.trimEnd().endsWith('(') || prevLine.trimEnd().endsWith(':')) {
                " ".repeat(tabSize)
            } else {
                ""
            }

            val fullIndent = indent + extraIndent
            if (fullIndent.isNotEmpty()) {
                val updatedText = newText.substring(0, cursor) + fullIndent + newText.substring(cursor)
                return TextFieldValue(
                    text = updatedText,
                    selection = TextRange(cursor + fullIndent.length)
                )
            }
        }
    }

    return newValue
}
