package com.cphone.editor.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
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

    // Colors
    val rowEvenColor = if (isDarkMode) Color(0xFF1E1E1E) else Color(0xFFFFFFFF)
    val rowOddColor  = if (isDarkMode) Color(0xFF242830) else Color(0xFFEEF5FC)
    val gutterTextColor = if (isDarkMode) Color(0xFF757575) else Color(0xFF8E99A2)
    val gutterDividerColor = if (isDarkMode) Color(0xFF333333) else Color(0xFFDDE4EC)
    val blueIndicatorColor = Color(0xFF1565C0)
    val cursorColor = if (isDarkMode) Color(0xFF4FC3F7) else Color(0xFF1565C0)
    val codeTextColor = if (isDarkMode) Color(0xFFECEFF1) else Color(0xFF1A1A2E)

    val text = textFieldValue.text
    val lineCount = remember(text) {
        if (text.isEmpty()) 1 else text.count { it == '\n' } + 1
    }

    val fontSize = settings.fontSizeSp.sp
    val lineHeight = (settings.fontSizeSp * 1.6f).sp

    // Shared text style — must be identical for gutter and editor
    val textStyle = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontSize = fontSize,
        lineHeight = lineHeight,
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.None
        ),
        platformStyle = PlatformTextStyle(includeFontPadding = false),
        color = codeTextColor
    )

    // Pixel measurements
    val lineHeightPx = with(density) { lineHeight.toPx() }
    val topPaddingPx = with(density) { 10.dp.toPx() }

    // Syntax highlighting visual transformation
    val highlightedText = remember(text, language, isDarkMode) {
        SyntaxHighlighter.highlight(text, language, isDarkMode)
    }
    val visualTransformation = remember(highlightedText) {
        VisualTransformation {
            TransformedText(highlightedText, OffsetMapping.Identity)
        }
    }

    // Shared vertical scroll state — drives both gutter and editor in sync
    val verticalScrollState = rememberScrollState()
    val horizontalScrollState = rememberScrollState()

    // Gutter width based on line count
    val gutterWidth = when {
        lineCount < 100  -> 40.dp
        lineCount < 1000 -> 52.dp
        else             -> 64.dp
    }

    // Build the row background draw modifier
    val rowBandModifier = if (settings.alternatingRowBands && lineHeightPx > 0f) {
        Modifier.drawBehind {
            val totalLines = max(lineCount, (size.height / lineHeightPx).toInt() + 4)
            for (i in 0 until totalLines) {
                val y = topPaddingPx + i * lineHeightPx
                val color = if (i % 2 == 0) rowEvenColor else rowOddColor
                drawRect(
                    color = color,
                    topLeft = Offset(0f, y),
                    size = Size(size.width, lineHeightPx)
                )
            }
        }
    } else Modifier

    Row(
        modifier = modifier
            .fillMaxSize()
            .background(rowEvenColor)
    ) {
        // ── Blue left indicator ──────────────────────────────────────────────
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(blueIndicatorColor)
        )

        // ── Gutter (line numbers) ────────────────────────────────────────────
        if (settings.showLineNumbers) {
            Box(
                modifier = Modifier
                    .width(gutterWidth)
                    .fillMaxHeight()
                    .background(if (isDarkMode) Color(0xFF1A1A1A) else Color(0xFFF5F8FC))
                    .then(rowBandModifier)
            ) {
                // Line number text — uses same vertical scroll as editor
                val gutterStyle = textStyle.copy(
                    color = gutterTextColor,
                    textAlign = TextAlign.End
                )
                val lineNumbers = remember(lineCount) {
                    (1..lineCount).joinToString("\n")
                }
                // We render line numbers as a BasicTextField (non-editable) so it
                // inherits exactly the same line-height rendering as the editor.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 10.dp, bottom = 48.dp, end = 8.dp)
                ) {
                    androidx.compose.material3.Text(
                        text = lineNumbers,
                        style = gutterStyle,
                        modifier = Modifier
                            .fillMaxWidth()
                    )
                }
            }

            // Gutter divider
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(gutterDividerColor)
            )
        }

        // ── Code editing area ────────────────────────────────────────────────
        val editorModifier = if (settings.wordWrap) {
            Modifier
                .weight(1f)
                .fillMaxHeight()
                .then(rowBandModifier)
                .padding(top = 10.dp, start = 8.dp, end = 16.dp, bottom = 48.dp)
        } else {
            Modifier
                .weight(1f)
                .fillMaxHeight()
                .then(rowBandModifier)
                .horizontalScroll(horizontalScrollState)
                .padding(top = 10.dp, start = 8.dp, end = 24.dp, bottom = 48.dp)
                .defaultMinSize(minWidth = 1200.dp)
        }

        BasicTextField(
            value = textFieldValue,
            onValueChange = { new ->
                val handled = handleAutoIndent(textFieldValue, new, settings.tabSize)
                onValueChange(handled)
            },
            modifier = editorModifier,
            textStyle = textStyle,
            cursorBrush = SolidColor(cursorColor),
            visualTransformation = visualTransformation,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrect = false,
                keyboardType = KeyboardType.Ascii,
                imeAction = ImeAction.Default
            ),
            maxLines = Int.MAX_VALUE,
            singleLine = false
        )
    }
}

// ── Auto-indentation ─────────────────────────────────────────────────────────
private fun handleAutoIndent(
    old: TextFieldValue,
    new: TextFieldValue,
    tabSize: Int
): TextFieldValue {
    val oldText = old.text
    val newText = new.text

    // Only act when exactly one character was inserted and it's a newline
    if (newText.length != oldText.length + 1) return new
    if (!new.selection.collapsed) return new
    val cursor = new.selection.start
    if (cursor < 1 || newText[cursor - 1] != '\n') return new

    // Find indentation of previous line
    val prevLineStart = newText.lastIndexOf('\n', cursor - 2).let { if (it < 0) 0 else it + 1 }
    val prevLine = newText.substring(prevLineStart, cursor - 1)
    val indent = prevLine.takeWhile { it == ' ' || it == '\t' }
    val extra = if (prevLine.trimEnd().endsWith('{') ||
        prevLine.trimEnd().endsWith('(') ||
        prevLine.trimEnd().endsWith(':') ||
        prevLine.trimEnd().endsWith('[')) " ".repeat(tabSize) else ""

    val fullIndent = indent + extra
    if (fullIndent.isEmpty()) return new

    val updated = newText.substring(0, cursor) + fullIndent + newText.substring(cursor)
    return new.copy(
        text = updated,
        selection = androidx.compose.ui.text.TextRange(cursor + fullIndent.length)
    )
}
