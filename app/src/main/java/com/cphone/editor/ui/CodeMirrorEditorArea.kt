package com.cphone.editor.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.input.TextFieldValue
import com.cphone.editor.model.EditorSettings
import com.cphone.editor.model.SupportedLanguage
import com.cphone.editor.syntax.SyntaxHighlighter
import com.cphone.editor.tools.CodeDiagnostic
import com.cphone.editor.tools.DiagnosticSeverity

@Composable
fun CodeMirrorEditorArea(
    documentId: String,
    textFieldValue: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    language: SupportedLanguage,
    settings: EditorSettings,
    diagnostics: List<CodeDiagnostic>,
    currentLine: Int,
    searchQuery: String,
    onFontSizeCalibrated: (Float) -> Unit,
    onBugLineClicked: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = remember(settings.editorTheme, settings.isDarkMode) {
        SyntaxHighlighter.resolvePalette(settings.editorTheme, settings.isDarkMode)
    }
    val lineCount = remember(textFieldValue.text) {
        textFieldValue.text.count { it == '\n' } + 1
    }
    val lineDiagnostics = remember(diagnostics) {
        diagnostics.associateBy { it.line }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(palette.editorBg)
    ) {
        CodeMirrorEditorView(
            documentId = documentId,
            textFieldValue = textFieldValue,
            onValueChange = onValueChange,
            language = language,
            settings = settings,
            diagnostics = diagnostics,
            searchQuery = searchQuery,
            onFontSizeCalibrated = onFontSizeCalibrated,
            modifier = Modifier.fillMaxSize()
        )

        if (settings.showMinimapIndicator && lineCount > 1) {
            Canvas(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .width(5.dp)
                    .background(palette.gutterBg.copy(alpha = 0.55f))
            ) {
                val total = lineCount.toFloat()
                val cursorRatio = ((currentLine - 1) / total).coerceIn(0f, 0.98f)
                drawRect(
                    color = palette.accentColor,
                    topLeft = Offset(0f, cursorRatio * size.height),
                    size = androidx.compose.ui.geometry.Size(size.width, 6.dp.toPx())
                )
                lineDiagnostics.forEach { (line, diagnostic) ->
                    val ratio = ((line - 1) / total).coerceIn(0f, 0.99f)
                    val color = when (diagnostic.severity) {
                        DiagnosticSeverity.ERROR -> Color(0xFFE06C75)
                        DiagnosticSeverity.WARNING -> Color(0xFFE5C07B)
                        DiagnosticSeverity.INFO -> Color(0xFF61AFEF)
                    }
                    drawRect(
                        color = color,
                        topLeft = Offset(0f, ratio * size.height),
                        size = androidx.compose.ui.geometry.Size(size.width, 4.dp.toPx())
                    )
                }
            }
        }

        Surface(
            color = Color(0xFF9A3412),
            shape = RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 8.dp)
                .clickable {
                    onBugLineClicked(diagnostics.firstOrNull()?.line ?: currentLine)
                }
        ) {
            androidx.compose.foundation.layout.Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Problems",
                    tint = Color.White,
                    modifier = Modifier.width(14.dp)
                )
                Text(
                    text = if (diagnostics.isEmpty()) "Problems" else "Problems (${diagnostics.size})",
                    color = Color.White,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
    }
}
