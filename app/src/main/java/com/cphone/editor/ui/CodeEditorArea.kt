package com.cphone.editor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cphone.editor.model.EditorSettings
import com.cphone.editor.model.SupportedLanguage
import com.cphone.editor.syntax.SyntaxHighlighter
import kotlin.math.max

/**
 * Main code editing composable.
 *
 * Features:
 *  - VSCode One Dark Pro / GitHub Light colour scheme
 *  - Syntax highlighting via VisualTransformation
 *  - Line numbers gutter (synced scroll)
 *  - Alternating row bands
 *  - Auto-indentation on Enter
 *  - Bracket / quote auto-close
 *  - Phone-keyboard friendly: no auto-correct, no capitalisation, Ascii keyboard
 */
@Composable
fun CodeEditorArea(
    textFieldValue: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    language: SupportedLanguage,
    settings: EditorSettings,
    modifier: Modifier = Modifier
) {
    val density    = LocalDensity.current
    val isDarkMode = settings.isDarkMode

    // ── VSCode-matching editor colours ────────────────────────────────────────
    val editorBg        = if (isDarkMode) Color(0xFF1E1E2E) else Color(0xFFFFFFFF)
    val rowEvenColor    = if (isDarkMode) Color(0xFF1E1E2E) else Color(0xFFFFFFFF)
    val rowOddColor     = if (isDarkMode) Color(0xFF252535) else Color(0xFFF0F6FF)
    val gutterBg        = if (isDarkMode) Color(0xFF1A1A2A) else Color(0xFFF5F8FC)
    val gutterTextColor = if (isDarkMode) Color(0xFF636D83) else Color(0xFF8E99A2)
    val gutterDivColor  = if (isDarkMode) Color(0xFF2D2D3F) else Color(0xFFDDE4EC)
    val accentBar       = if (isDarkMode) Color(0xFF528BFF) else Color(0xFF1565C0)
    val cursorColor     = if (isDarkMode) Color(0xFFAEAFFD) else Color(0xFF1565C0)
    val codeTextColor   = if (isDarkMode) Color(0xFFABB2BF)  else Color(0xFF24292F)

    val text      = textFieldValue.text
    val lineCount = remember(text) {
        if (text.isEmpty()) 1 else text.count { it == '\n' } + 1
    }

    val fontSize   = settings.fontSizeSp.sp
    val lineHeight = (settings.fontSizeSp * 1.65f).sp

    val textStyle = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontSize   = fontSize,
        lineHeight = lineHeight,
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim      = LineHeightStyle.Trim.None
        ),
        platformStyle = PlatformTextStyle(includeFontPadding = false),
        color = codeTextColor
    )

    val lineHeightPx = with(density) { lineHeight.toPx() }
    val topPaddingPx = with(density) { 10.dp.toPx() }

    // ── Syntax highlighting ───────────────────────────────────────────────────
    val highlightedText = remember(text, language, isDarkMode) {
        SyntaxHighlighter.highlight(text, language, isDarkMode)
    }
    val visualTransformation = remember(highlightedText) {
        VisualTransformation { TransformedText(highlightedText, OffsetMapping.Identity) }
    }

    val verticalScrollState   = rememberScrollState()
    val horizontalScrollState = rememberScrollState()

    val gutterWidth = when {
        lineCount < 100  -> 40.dp
        lineCount < 1000 -> 52.dp
        else             -> 64.dp
    }

    val rowBandModifier = if (settings.alternatingRowBands && lineHeightPx > 0f) {
        Modifier.drawBehind {
            val totalLines = max(lineCount, (size.height / lineHeightPx).toInt() + 4)
            for (i in 0 until totalLines) {
                val y     = topPaddingPx + i * lineHeightPx
                val color = if (i % 2 == 0) rowEvenColor else rowOddColor
                drawRect(color = color, topLeft = Offset(0f, y), size = Size(size.width, lineHeightPx))
            }
        }
    } else Modifier

    Row(
        modifier = modifier
            .fillMaxSize()
            .background(editorBg)
    ) {
        // ── Left accent bar (VSCode-style) ────────────────────────────────────
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight()
                .background(accentBar)
        )

        // ── Line-number gutter ────────────────────────────────────────────────
        if (settings.showLineNumbers) {
            Box(
                modifier = Modifier
                    .width(gutterWidth)
                    .fillMaxHeight()
                    .background(gutterBg)
                    .then(rowBandModifier)
            ) {
                val gutterStyle  = textStyle.copy(color = gutterTextColor, textAlign = TextAlign.End)
                val lineNumbers  = remember(lineCount) { (1..lineCount).joinToString("\n") }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 10.dp, bottom = 48.dp, end = 8.dp)
                ) {
                    androidx.compose.material3.Text(
                        text     = lineNumbers,
                        style    = gutterStyle,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(gutterDivColor)
            )
        }

        // ── Code editing area ─────────────────────────────────────────────────
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
            value          = textFieldValue,
            onValueChange  = { new ->
                val processed = processKeyInput(
                    old             = textFieldValue,
                    new             = new,
                    tabSize         = settings.tabSize,
                    bracketAutoClose= settings.bracketAutoClose
                )
                onValueChange(processed)
            },
            modifier       = editorModifier,
            textStyle      = textStyle,
            cursorBrush    = SolidColor(cursorColor),
            visualTransformation = visualTransformation,
            keyboardOptions = KeyboardOptions(
                // Critical for phone coding:
                capitalization = KeyboardCapitalization.None,   // no auto-caps
                autoCorrect    = false,                          // no auto-correct
                keyboardType   = KeyboardType.Ascii,             // code keyboard on phones that support it
                imeAction      = ImeAction.Default               // Enter = actual newline
            ),
            maxLines  = Int.MAX_VALUE,
            singleLine = false
        )
    }
}

// ── Input processing: auto-indent + bracket auto-close ───────────────────────

private val OPEN_CLOSE_PAIRS = mapOf(
    '{' to '}',
    '(' to ')',
    '[' to ']',
    '"' to '"',
    '\'' to '\''
)

/**
 * Handles:
 * 1. Auto-indentation when Enter is pressed (matching previous line indent + extra if line ends with { ( [ :)
 * 2. Bracket/quote auto-closing: typing { inserts {} and places cursor between them
 * 3. Skip-over: if cursor is right before a closing bracket and user types it, just move over
 */
private fun processKeyInput(
    old: TextFieldValue,
    new: TextFieldValue,
    tabSize: Int,
    bracketAutoClose: Boolean
): TextFieldValue {
    val oldText = old.text
    val newText = new.text

    // ── Case 1: Enter key → auto-indentation ─────────────────────────────────
    if (newText.length == oldText.length + 1 &&
        new.selection.collapsed &&
        new.selection.start >= 1 &&
        newText[new.selection.start - 1] == '\n'
    ) {
        return applyAutoIndent(new, tabSize)
    }

    // ── Case 2: Single char typed ─────────────────────────────────────────────
    if (!bracketAutoClose) return new
    if (newText.length != oldText.length + 1) return new
    if (!new.selection.collapsed) return new

    val cursor = new.selection.start
    val typed  = newText[cursor - 1]

    // Sub-case 2a: opening bracket/quote → insert pair, put cursor between
    val closer = OPEN_CLOSE_PAIRS[typed]
    if (closer != null) {
        // Don't double-close a quote that's already the next char
        val nextChar = if (cursor < newText.length) newText[cursor] else '\u0000'
        if (typed == '"' && nextChar == '"') return new
        if (typed == '\'' && nextChar == '\'') return new

        val updated = newText.substring(0, cursor) + closer + newText.substring(cursor)
        return new.copy(
            text      = updated,
            selection = androidx.compose.ui.text.TextRange(cursor)
        )
    }

    // Sub-case 2b: closing bracket/quote typed while already present ahead → skip over
    val closers = OPEN_CLOSE_PAIRS.values.toSet()
    if (typed in closers) {
        val charBeforeCursor = newText[cursor - 1]  // the char we just inserted
        // Look at original text: was the char before insertion the same closer?
        val origNextChar = if (cursor - 1 < oldText.length) oldText[cursor - 1] else '\u0000'
        if (origNextChar == typed) {
            // Remove the typed duplicate, move cursor forward instead
            val restored = newText.substring(0, cursor - 1) + newText.substring(cursor)
            return new.copy(
                text      = restored,
                selection = androidx.compose.ui.text.TextRange(cursor)
            )
        }
    }

    return new
}

private fun applyAutoIndent(new: TextFieldValue, tabSize: Int): TextFieldValue {
    val text   = new.text
    val cursor = new.selection.start

    val prevLineStart = text.lastIndexOf('\n', cursor - 2).let { if (it < 0) 0 else it + 1 }
    val prevLine = text.substring(prevLineStart, cursor - 1)
    val indent   = prevLine.takeWhile { it == ' ' || it == '\t' }
    val trimmed  = prevLine.trimEnd()
    val extra    = if (trimmed.endsWith('{') || trimmed.endsWith('(') ||
        trimmed.endsWith(':') || trimmed.endsWith('[')) " ".repeat(tabSize) else ""

    val fullIndent = indent + extra
    if (fullIndent.isEmpty()) return new

    val updated = text.substring(0, cursor) + fullIndent + text.substring(cursor)
    return new.copy(
        text      = updated,
        selection = androidx.compose.ui.text.TextRange(cursor + fullIndent.length)
    )
}
