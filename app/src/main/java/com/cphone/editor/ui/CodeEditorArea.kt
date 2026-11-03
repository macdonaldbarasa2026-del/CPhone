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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cphone.editor.model.EditorSettings
import com.cphone.editor.model.SupportedLanguage
import com.cphone.editor.syntax.SyntaxHighlighter

@Composable
fun CodeEditorArea(
    textFieldValue: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    language: SupportedLanguage,
    settings: EditorSettings,
    modifier: Modifier = Modifier
) {
    val isDarkMode = settings.isDarkMode
    val bgColor = if (isDarkMode) Color(0xFF1E1E1E) else Color(0xFFFFFFFF)
    val gutterBgColor = if (isDarkMode) Color(0xFF1E1E1E) else Color(0xFFFFFFFF)
    val gutterTextColor = if (isDarkMode) Color(0xFF757575) else Color(0xFF9E9E9E)
    val gutterDividerColor = if (isDarkMode) Color(0xFF2C2C2C) else Color(0xFFE8EAED)
    val blueIndicatorColor = Color(0xFF007AFF) // Clean vibrant blue from screenshot
    val cursorColor = if (isDarkMode) Color(0xFF4FC3F7) else Color(0xFF007AFF)

    // Calculate line count
    val text = textFieldValue.text
    val lineCount = remember(text) {
        if (text.isEmpty()) 1 else text.count { it == '\n' } + 1
    }

    val verticalScrollState = rememberScrollState()
    val horizontalScrollState = rememberScrollState()

    val codeFontSize = settings.fontSizeSp.sp
    val lineHeight = (settings.fontSizeSp * 1.55f).sp

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
            .background(bgColor)
    ) {
        // 1. Blue vertical indicator along the left edge of the editor (matches reference screenshot)
        Box(
            modifier = Modifier
                .width(10.dp)
                .fillMaxHeight()
                .background(blueIndicatorColor)
        )

        // Gutter and editor container with synchronized vertical scroll
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .verticalScroll(verticalScrollState)
        ) {
            // 2. Clearly visible line numbers in a light-grey gutter
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
                        .background(gutterBgColor)
                        .padding(top = 10.dp, bottom = 24.dp, end = 8.dp)
                ) {
                    Text(
                        text = lineNumbersText,
                        color = gutterTextColor,
                        fontSize = codeFontSize,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = lineHeight,
                        textAlign = TextAlign.End,
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

            // 3. Code Editing Area
            val editorModifier = if (settings.wordWrap) {
                Modifier
                    .weight(1f)
                    .padding(top = 10.dp, bottom = 48.dp, start = 8.dp, end = 16.dp)
            } else {
                Modifier
                    .weight(1f)
                    .horizontalScroll(horizontalScrollState)
                    .padding(top = 10.dp, bottom = 48.dp, start = 8.dp, end = 24.dp)
                    .defaultMinSize(minWidth = 800.dp)
            }

            BasicTextField(
                value = textFieldValue,
                onValueChange = { newValue ->
                    // Auto-indentation check on Enter
                    val handledValue = handleAutoIndent(textFieldValue, newValue, settings.tabSize)
                    onValueChange(handledValue)
                },
                modifier = editorModifier,
                textStyle = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = codeFontSize,
                    lineHeight = lineHeight,
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

    // Check if user pressed Enter (text grew by 1 character, and inserted char is '\n')
    if (newText.length == oldText.length + 1 && newValue.selection.collapsed) {
        val cursor = newValue.selection.start
        if (cursor > 0 && newText[cursor - 1] == '\n') {
            // Find start of the previous line
            val prevLineStart = newText.lastIndexOf('\n', cursor - 2).let { if (it == -1) 0 else it + 1 }
            val prevLine = newText.substring(prevLineStart, cursor - 1)

            // Calculate leading indentation (spaces / tabs)
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
