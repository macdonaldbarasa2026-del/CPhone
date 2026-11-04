package com.cphone.editor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextRange
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
import com.cphone.editor.tools.CodeDiagnostic
import com.cphone.editor.tools.DiagnosticSeverity
import kotlin.math.max

/**
 * Commercial-grade mobile code editing area.
 *
 * Features:
 *  - 6 IDE Theme Palettes (One Dark Pro, Dracula, Monokai, Tokyo Night, Solarized, GitHub Light)
 *  - Active line highlight & vertical indent guides
 *  - Live Bug & Warning gutter markers + Right-edge Bug Radar Minimap
 *  - Two-finger pinch-to-zoom font calibration
 *  - Smart block expansion ({|} -> indented block) & Python colon auto-indent
 */
@Composable
fun CodeEditorArea(
    textFieldValue: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    language: SupportedLanguage,
    settings: EditorSettings,
    diagnostics: List<CodeDiagnostic> = emptyList(),
    currentLine: Int = 1,
    searchQuery: String = "",
    onFontSizeCalibrated: (Float) -> Unit = {},
    onBugLineClicked: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val palette = remember(settings.editorTheme, settings.isDarkMode) {
        SyntaxHighlighter.resolvePalette(settings.editorTheme, settings.isDarkMode)
    }

    val editorBg        = palette.editorBg
    val rowEvenColor    = palette.editorBg
    val rowOddColor     = palette.rowBandOddBg
    val activeLineColor = palette.activeLineBg
    val gutterBg        = palette.gutterBg
    val gutterTextColor = palette.gutterText
    val gutterDivColor  = palette.borderColor
    val accentBar       = palette.accentColor
    val cursorColor     = palette.accentColor
    val codeTextColor   = palette.syntax.defaultText

    val text = textFieldValue.text
    val lineCount = remember(text) {
        if (text.isEmpty()) 1 else text.count { it == '\n' } + 1
    }

    // Map line -> highest severity diagnostic for fast gutter rendering
    val lineDiagnostics = remember(diagnostics) {
        val map = mutableMapOf<Int, DiagnosticSeverity>()
        diagnostics.forEach { d ->
            val existing = map[d.line]
            if (existing == null || d.severity.ordinal < existing.ordinal) {
                map[d.line] = d.severity
            }
        }
        map
    }

    val fontSize   = settings.fontSizeSp.sp
    val lineHeight = (settings.fontSizeSp * settings.lineSpacingMultiplier).sp

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
    val topPaddingPx = with(density) { 8.dp.toPx() }
    val charWidthPx  = with(density) { (settings.fontSizeSp * 0.6f).dp.toPx() }

    // ── Syntax highlighting VisualTransformation ─────────────────────────────
    val visualTransformation = remember(language, settings.isDarkMode, settings.editorTheme, searchQuery) {
        VisualTransformation { textToTransform ->
            val highlighted = SyntaxHighlighter.highlight(
                text = textToTransform.text,
                language = language,
                isDarkMode = settings.isDarkMode,
                themeId = settings.editorTheme,
                searchQuery = searchQuery
            )
            TransformedText(highlighted, OffsetMapping.Identity)
        }
    }

    val verticalScrollState   = rememberScrollState()
    val horizontalScrollState = rememberScrollState()

    // Auto-scroll vertically when cursor jumps significantly (e.g., Go to Line or Search)
    LaunchedEffect(currentLine) {
        if (lineHeightPx > 0f) {
            val targetY = ((currentLine - 1) * lineHeightPx).toInt()
            val viewportHeight = verticalScrollState.maxValue.coerceAtLeast(1)
            if (targetY < verticalScrollState.value || targetY > verticalScrollState.value + 600) {
                verticalScrollState.animateScrollTo((targetY - 160).coerceIn(0, viewportHeight))
            }
        }
    }

    val gutterWidth = when {
        lineCount < 100   -> 40.dp
        lineCount < 1000  -> 50.dp
        lineCount < 10000 -> 60.dp
        else              -> 70.dp
    }

    val pinchModifier = if (settings.pinchToZoom) {
        Modifier.pointerInput(settings.fontSizeSp) {
            detectTransformGestures { _, _, zoom, _ ->
                if (zoom != 1f && (zoom > 1.03f || zoom < 0.97f)) {
                    val next = (settings.fontSizeSp * zoom).coerceIn(9f, 26f)
                    onFontSizeCalibrated(next)
                }
            }
        }
    } else Modifier

    val editorBackgroundDraw = Modifier.drawBehind {
        if (lineHeightPx <= 0f) return@drawBehind
        val totalLines = max(lineCount, (size.height / lineHeightPx).toInt() + 6)
        for (i in 0 until totalLines) {
            val lineNum = i + 1
            val y = topPaddingPx + i * lineHeightPx
            val diagSeverity = lineDiagnostics[lineNum]
            val bgColor = when {
                diagSeverity == DiagnosticSeverity.ERROR -> Color(0x28E06C75)
                lineNum == currentLine && settings.showActiveLineHighlight -> activeLineColor
                settings.alternatingRowBands -> if (i % 2 == 0) rowEvenColor else rowOddColor
                else -> rowEvenColor
            }
            if (bgColor != rowEvenColor) {
                drawRect(color = bgColor, topLeft = Offset(0f, y), size = Size(size.width, lineHeightPx))
            }
        }

        // Draw subtle vertical indent guides
        if (settings.showIndentGuides && charWidthPx > 2f) {
            val stepPx = charWidthPx * settings.tabSize
            val guideColor = gutterDivColor.copy(alpha = 0.35f)
            val startX = with(density) { 8.dp.toPx() }
            for (level in 1..8) {
                val x = startX + level * stepPx
                if (x < size.width) {
                    drawLine(
                        color = guideColor,
                        start = Offset(x, topPaddingPx),
                        end = Offset(x, topPaddingPx + lineCount * lineHeightPx),
                        strokeWidth = 1f
                    )
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(editorBg)
            .then(pinchModifier)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(verticalScrollState)
        ) {
            // ── Left accent indicator bar ────────────────────────────────────
            Box(
                modifier = Modifier
                    .width(2.5.dp)
                    .defaultMinSize(minHeight = 900.dp)
                    .background(accentBar)
            )

            // ── Line-number gutter + Bug dots ────────────────────────────────
            if (settings.showLineNumbers) {
                Box(
                    modifier = Modifier
                        .width(gutterWidth)
                        .background(gutterBg)
                        .drawBehind {
                            if (lineHeightPx <= 0f) return@drawBehind
                            // Highlight current line in gutter
                            val activeY = topPaddingPx + (currentLine - 1) * lineHeightPx
                            drawRect(
                                color = activeLineColor,
                                topLeft = Offset(0f, activeY),
                                size = Size(size.width, lineHeightPx)
                            )
                            // Draw bug / warning dots on lines with diagnostics
                            lineDiagnostics.forEach { (lineNum, severity) ->
                                val cy = topPaddingPx + (lineNum - 0.5f) * lineHeightPx
                                val dotColor = when (severity) {
                                    DiagnosticSeverity.ERROR -> Color(0xFFE06C75)
                                    DiagnosticSeverity.WARNING -> Color(0xFFE5C07B)
                                    DiagnosticSeverity.INFO -> Color(0xFF61AFEF)
                                }
                                drawCircle(
                                    color = dotColor,
                                    radius = 3.5.dp.toPx(),
                                    center = Offset(5.dp.toPx(), cy)
                                )
                            }
                        }
                        .clickable {
                            if (diagnostics.isNotEmpty()) {
                                onBugLineClicked(diagnostics.first().line)
                            }
                        }
                ) {
                    val gutterStyle = textStyle.copy(color = gutterTextColor, textAlign = TextAlign.End)
                    val linesList = remember(text) { text.split('\n') }
                    val lineNumbers = remember(linesList) {
                        linesList.mapIndexed { idx, lineContent ->
                            val trimmed = lineContent.trimEnd()
                            val hasBlock = trimmed.endsWith("{") || (trimmed.endsWith(":") && trimmed.length > 1)
                            if (hasBlock) "${idx + 1} ⌄" else "${idx + 1}  "
                        }.joinToString("\n")
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 72.dp, end = 4.dp)
                    ) {
                        Text(
                            text     = lineNumbers,
                            style    = gutterStyle,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .defaultMinSize(minHeight = 900.dp)
                        .background(gutterDivColor)
                )
            }

            // ── Code editing area ─────────────────────────────────────────────
            val editorModifier = if (settings.wordWrap) {
                Modifier
                    .weight(1f)
                    .then(editorBackgroundDraw)
                    .padding(top = 8.dp, start = 8.dp, end = 14.dp, bottom = 72.dp)
            } else {
                Modifier
                    .weight(1f)
                    .then(editorBackgroundDraw)
                    .horizontalScroll(horizontalScrollState)
                    .padding(top = 8.dp, start = 8.dp, end = 24.dp, bottom = 72.dp)
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
                    capitalization = KeyboardCapitalization.None,
                    autoCorrect    = false,
                    keyboardType   = KeyboardType.Text,
                    imeAction      = ImeAction.Default
                ),
                maxLines  = Int.MAX_VALUE,
                singleLine = false
            )
        }

        // ── Right-Edge Minimap / Bug & Cursor Radar ──────────────────────────
        if (settings.showMinimapIndicator && lineCount > 1) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .width(5.dp)
                    .background(gutterBg.copy(alpha = 0.55f))
                    .drawBehind {
                        val total = lineCount.coerceAtLeast(1).toFloat()
                        // Current cursor line tick
                        val cursorRatio = ((currentLine - 1) / total).coerceIn(0f, 0.98f)
                        drawRect(
                            color = accentBar,
                            topLeft = Offset(0f, cursorRatio * size.height),
                            size = Size(size.width, 6.dp.toPx())
                        )
                        // Bug / warning ticks
                        lineDiagnostics.forEach { (lineNum, severity) ->
                            val ratio = ((lineNum - 1) / total).coerceIn(0f, 0.99f)
                            val c = when (severity) {
                                DiagnosticSeverity.ERROR -> Color(0xFFE06C75)
                                DiagnosticSeverity.WARNING -> Color(0xFFE5C07B)
                                DiagnosticSeverity.INFO -> Color(0xFF61AFEF)
                            }
                            drawRect(
                                color = c,
                                topLeft = Offset(0f, ratio * size.height),
                                size = Size(size.width, 4.dp.toPx())
                            )
                        }
                    }
            )
        }

        // ── Vertical "⚠ Problems" Tab on the Top-Right Edge (matching screenshot) ──
        Surface(
            color = Color(0xFF9A3412),
            shape = RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp),
            shadowElevation = 4.dp,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 8.dp)
                .clickable {
                    val targetLine = diagnostics.firstOrNull()?.line ?: currentLine
                    onBugLineClicked(targetLine)
                }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints)
                        layout(placeable.height, placeable.width) {
                            placeable.place(
                                x = -(placeable.width - placeable.height) / 2,
                                y = (placeable.width - placeable.height) / 2
                            )
                        }
                    }
                    .graphicsLayer { rotationZ = 90f }
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Problems",
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                val problemText = if (diagnostics.isNotEmpty()) {
                    "Problems (${diagnostics.size})"
                } else {
                    "Problems"
                }
                Text(
                    text = problemText,
                    color = Color.White,
                    fontSize = 11.sp
                )
            }
        }
    }
}

// ── Input processing: smart block expansion + auto-indent + bracket auto-close ──

private val OPEN_CLOSE_PAIRS = mapOf(
    '{' to '}',
    '(' to ')',
    '[' to ']',
    '"' to '"',
    '\'' to '\'',
    '`' to '`'
)

private fun processKeyInput(
    old: TextFieldValue,
    new: TextFieldValue,
    tabSize: Int,
    bracketAutoClose: Boolean
): TextFieldValue {
    val oldText = old.text
    val newText = new.text

    if (oldText == newText) return new

    // ── Case 1: Enter key → smart block expansion or auto-indentation ────────
    if (newText.length == oldText.length + 1 &&
        new.selection.collapsed &&
        new.selection.start >= 1 &&
        newText[new.selection.start - 1] == '\n'
    ) {
        return applySmartEnter(new, tabSize)
    }

    // ── Case 2: Single char typed ─────────────────────────────────────────────
    if (!bracketAutoClose) return new
    if (newText.length != oldText.length + 1) return new
    if (!new.selection.collapsed) return new

    val cursor = new.selection.start
    if (cursor < 1 || cursor > newText.length) return new

    val typed = newText[cursor - 1]

    // Sub-case 2a: opening bracket/quote → insert pair, put cursor between
    val closer = OPEN_CLOSE_PAIRS[typed]
    if (closer != null) {
        val nextChar = if (cursor < newText.length) newText[cursor] else '\u0000'
        if ((typed == '"' || typed == '\'' || typed == '`') && nextChar == typed) return new

        val updated = newText.substring(0, cursor) + closer + newText.substring(cursor)
        return new.copy(
            text      = updated,
            selection = TextRange(cursor)
        )
    }

    // Sub-case 2b: closing bracket/quote typed while already present ahead → skip over
    val closers = OPEN_CLOSE_PAIRS.values.toSet()
    if (typed in closers && cursor < newText.length) {
        val origNextChar = if (cursor - 1 < oldText.length) oldText[cursor - 1] else '\u0000'
        if (origNextChar == typed) {
            val restored = newText.substring(0, cursor - 1) + newText.substring(cursor)
            return new.copy(
                text      = restored,
                selection = TextRange(cursor)
            )
        }
    }

    return new
}

private fun applySmartEnter(new: TextFieldValue, tabSize: Int): TextFieldValue {
    val text   = new.text
    val cursor = new.selection.start

    val prevLineStart = text.lastIndexOf('\n', cursor - 2).let { if (it < 0) 0 else it + 1 }
    val prevLine = text.substring(prevLineStart, cursor - 1)
    val indent   = prevLine.takeWhile { it == ' ' || it == '\t' }
    val trimmed  = prevLine.trimEnd()
    val step     = " ".repeat(tabSize)

    val charBeforeEnter = trimmed.lastOrNull() ?: '\u0000'
    val charAfterEnter  = if (cursor < text.length) text[cursor] else '\u0000'

    // Smart expansion when pressing Enter between {|}, (|), or [|]
    if ((charBeforeEnter == '{' && charAfterEnter == '}') ||
        (charBeforeEnter == '[' && charAfterEnter == ']') ||
        (charBeforeEnter == '(' && charAfterEnter == ')')
    ) {
        val innerIndent = indent + step
        val insertion = "$innerIndent\n$indent"
        val updated = text.substring(0, cursor) + insertion + text.substring(cursor)
        return new.copy(
            text      = updated,
            selection = TextRange(cursor + innerIndent.length)
        )
    }

    val extra = if (trimmed.endsWith('{') || trimmed.endsWith('(') ||
        trimmed.endsWith(':') || trimmed.endsWith('[')) step else ""

    val fullIndent = indent + extra
    if (fullIndent.isEmpty()) return new

    val updated = text.substring(0, cursor) + fullIndent + text.substring(cursor)
    return new.copy(
        text      = updated,
        selection = TextRange(cursor + fullIndent.length)
    )
}
