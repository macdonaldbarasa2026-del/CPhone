package com.cphone.editor.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cphone.editor.model.SupportedLanguage

/**
 * Renders the authentic language icon matching the Acode / VS Code Mobile design:
 * - HTML (.html / .htm): Orange HTML5 shield with crisp white '5' inside
 * - CSS (.css): Blue CSS3 shield with crisp white '3' inside
 * - C++ (.cpp / .c / .h): Bold crimson/purple 'C++' emblem
 * - Kotlin (.kt): Geometric blue/cyan 'K' ribbon icon
 * - Python (.py): Official blue & yellow dual-serpent icon
 * - JS / TS / JSON / Java / SQL / Shell / Markdown / etc.
 */
@Composable
fun LanguageFileIcon(
    fileName: String,
    size: Dp = 18.dp,
    modifier: Modifier = Modifier
) {
    LanguageFileIcon(
        language = SupportedLanguage.detect(fileName),
        size = size,
        modifier = modifier
    )
}

@Composable
fun LanguageFileIcon(
    language: SupportedLanguage,
    size: Dp = 18.dp,
    modifier: Modifier = Modifier
) {
    val scale = size.value / 18f

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            when (language) {
                SupportedLanguage.HTML -> drawShieldBase(
                    outerColor = Color(0xFFE44D26),
                    innerHighlight = Color(0xFFF16529)
                )

                SupportedLanguage.CSS -> drawShieldBase(
                    outerColor = Color(0xFF264DE4),
                    innerHighlight = Color(0xFF2965F1)
                )

                SupportedLanguage.CPP -> {
                    // Clean transparent/subtle canvas; text overlay draws the signature 'C++' glyph
                }

                SupportedLanguage.KOTLIN -> drawKotlinRibbonIcon()

                SupportedLanguage.PYTHON -> drawPythonDualSnakeIcon()

                SupportedLanguage.JAVASCRIPT -> {
                    val r = this.size.width * 0.20f
                    drawRoundRect(
                        color = Color(0xFFF7DF1E),
                        cornerRadius = CornerRadius(r, r)
                    )
                }

                SupportedLanguage.TYPESCRIPT -> {
                    val r = this.size.width * 0.20f
                    drawRoundRect(
                        color = Color(0xFF3178C6),
                        cornerRadius = CornerRadius(r, r)
                    )
                }

                SupportedLanguage.JSON -> {
                    // Crisp golden braces emblem
                }

                SupportedLanguage.JAVA -> drawJavaCoffeeIcon()

                SupportedLanguage.MARKDOWN -> {
                    val w = this.size.width
                    val h = this.size.height
                    val r = w * 0.18f
                    drawRoundRect(
                        color = Color(0xFF2563EB),
                        topLeft = Offset(0f, h * 0.14f),
                        size = Size(w, h * 0.72f),
                        cornerRadius = CornerRadius(r, r)
                    )
                }

                SupportedLanguage.SHELL, SupportedLanguage.BATCH -> {
                    val w = this.size.width
                    val h = this.size.height
                    val r = w * 0.20f
                    val accent = if (language == SupportedLanguage.SHELL) Color(0xFF4EAA25) else Color(0xFF38BDF8)
                    drawRoundRect(
                        color = Color(0xFF1E293B),
                        cornerRadius = CornerRadius(r, r)
                    )
                    val stroke = (w * 0.10f).coerceAtLeast(1.4f)
                    val chevron = Path().apply {
                        moveTo(w * 0.24f, h * 0.32f)
                        lineTo(w * 0.46f, h * 0.50f)
                        lineTo(w * 0.24f, h * 0.68f)
                    }
                    drawPath(
                        path = chevron,
                        color = accent,
                        style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                    drawLine(
                        color = Color.White,
                        start = Offset(w * 0.52f, h * 0.68f),
                        end = Offset(w * 0.76f, h * 0.68f),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round
                    )
                }

                SupportedLanguage.SQL -> drawSqlCylinderIcon()

                SupportedLanguage.PHP -> {
                    val w = this.size.width
                    val h = this.size.height
                    drawOval(
                        color = Color(0xFF777BB4),
                        topLeft = Offset(0f, h * 0.15f),
                        size = Size(w, h * 0.70f)
                    )
                }

                SupportedLanguage.XML -> {
                    val w = this.size.width
                    val h = this.size.height
                    val stroke = (w * 0.10f).coerceAtLeast(1.4f)
                    val left = Path().apply {
                        moveTo(w * 0.34f, h * 0.28f)
                        lineTo(w * 0.12f, h * 0.50f)
                        lineTo(w * 0.34f, h * 0.72f)
                    }
                    val right = Path().apply {
                        moveTo(w * 0.66f, h * 0.28f)
                        lineTo(w * 0.88f, h * 0.50f)
                        lineTo(w * 0.66f, h * 0.72f)
                    }
                    drawPath(left, Color(0xFFE37933), style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
                    drawPath(right, Color(0xFFE37933), style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
                    drawLine(
                        color = Color(0xFFE37933),
                        start = Offset(w * 0.55f, h * 0.24f),
                        end = Offset(w * 0.45f, h * 0.76f),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round
                    )
                }

                SupportedLanguage.YAML -> {
                    val r = this.size.width * 0.20f
                    drawRoundRect(
                        color = Color(0xFFCB171E),
                        cornerRadius = CornerRadius(r, r)
                    )
                }

                SupportedLanguage.RUST -> {
                    val w = this.size.width
                    drawCircle(
                        color = Color(0xFFDEA584),
                        radius = w * 0.46f,
                        center = Offset(w / 2f, w / 2f)
                    )
                }

                SupportedLanguage.GO -> {
                    val r = this.size.width * 0.24f
                    drawRoundRect(
                        color = Color(0xFF00ADD8),
                        cornerRadius = CornerRadius(r, r)
                    )
                }

                SupportedLanguage.DART -> {
                    val r = this.size.width * 0.22f
                    drawRoundRect(
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFF01579B), Color(0xFF00B4AB))
                        ),
                        cornerRadius = CornerRadius(r, r)
                    )
                }

                SupportedLanguage.PLAINTEXT -> drawPlainDocumentIcon()
            }
        }

        // Exact typographic symbols matching the screenshot:
        // '5' inside HTML5 shield, '3' inside CSS3 shield, 'C++' in crimson, etc.
        when (language) {
            SupportedLanguage.HTML -> Text(
                text = "5",
                color = Color.White,
                fontSize = (10.5f * scale).sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.SansSerif
            )
            SupportedLanguage.CSS -> Text(
                text = "3",
                color = Color.White,
                fontSize = (10.5f * scale).sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.SansSerif
            )
            SupportedLanguage.CPP -> Text(
                text = "C++",
                color = Color(0xFFB02A5B),
                fontSize = (8.8f * scale).sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.SansSerif,
                maxLines = 1,
                softWrap = false
            )
            SupportedLanguage.JAVASCRIPT -> Text(
                text = "JS",
                color = Color(0xFF18181B),
                fontSize = (8.5f * scale).sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.SansSerif
            )
            SupportedLanguage.TYPESCRIPT -> Text(
                text = "TS",
                color = Color.White,
                fontSize = (8.5f * scale).sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.SansSerif
            )
            SupportedLanguage.JSON -> Text(
                text = "{ }",
                color = Color(0xFFD97706),
                fontSize = (9.5f * scale).sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                softWrap = false
            )
            SupportedLanguage.MARKDOWN -> Text(
                text = "M↓",
                color = Color.White,
                fontSize = (7.5f * scale).sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.SansSerif
            )
            SupportedLanguage.PHP -> Text(
                text = "php",
                color = Color.White,
                fontSize = (6.5f * scale).sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.SansSerif
            )
            SupportedLanguage.YAML -> Text(
                text = "YML",
                color = Color.White,
                fontSize = (6.0f * scale).sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.SansSerif
            )
            SupportedLanguage.RUST -> Text(
                text = "R",
                color = Color(0xFF1E1B18),
                fontSize = (9.5f * scale).sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Serif
            )
            SupportedLanguage.GO -> Text(
                text = "GO",
                color = Color.White,
                fontSize = (7.8f * scale).sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.SansSerif
            )
            SupportedLanguage.DART -> Text(
                text = "Dt",
                color = Color.White,
                fontSize = (7.8f * scale).sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.SansSerif
            )
            else -> Unit
        }
    }
}

private fun DrawScope.drawShieldBase(
    outerColor: Color,
    innerHighlight: Color
) {
    val w = size.width
    val h = size.height

    val shieldPath = Path().apply {
        moveTo(w * 0.10f, h * 0.05f)
        lineTo(w * 0.90f, h * 0.05f)
        lineTo(w * 0.82f, h * 0.79f)
        lineTo(w * 0.50f, h * 0.96f)
        lineTo(w * 0.18f, h * 0.79f)
        close()
    }
    drawPath(path = shieldPath, color = outerColor)

    val rightHalfHighlight = Path().apply {
        moveTo(w * 0.50f, h * 0.12f)
        lineTo(w * 0.82f, h * 0.12f)
        lineTo(w * 0.76f, h * 0.74f)
        lineTo(w * 0.50f, h * 0.88f)
        close()
    }
    drawPath(path = rightHalfHighlight, color = innerHighlight)
}

private fun DrawScope.drawKotlinRibbonIcon() {
    val w = size.width
    val h = size.height
    val m = w * 0.10f

    // Matches the blue geometric Kotlin 'K' icon in the screenshot
    val kotlinK = Path().apply {
        moveTo(m, m)
        lineTo(w - m, m)
        lineTo(w * 0.52f, h * 0.50f)
        lineTo(w - m, h - m)
        lineTo(m, h - m)
        close()
    }
    drawPath(
        path = kotlinK,
        brush = Brush.linearGradient(
            colors = listOf(
                Color(0xFF0095D5),
                Color(0xFF2376E6),
                Color(0xFF6B57FF)
            ),
            start = Offset(0f, h),
            end = Offset(w, 0f)
        )
    )
}

private fun DrawScope.drawPythonDualSnakeIcon() {
    val w = size.width
    val h = size.height

    // Top-left blue Python serpent
    drawRoundRect(
        color = Color(0xFF3776AB),
        topLeft = Offset(w * 0.10f, h * 0.08f),
        size = Size(w * 0.54f, h * 0.48f),
        cornerRadius = CornerRadius(w * 0.16f, w * 0.16f)
    )
    // Bottom-right yellow Python serpent
    drawRoundRect(
        color = Color(0xFFFFD43B),
        topLeft = Offset(w * 0.36f, h * 0.44f),
        size = Size(w * 0.54f, h * 0.48f),
        cornerRadius = CornerRadius(w * 0.16f, w * 0.16f)
    )

    // Serpent eyes
    drawCircle(
        color = Color.White,
        radius = w * 0.05f,
        center = Offset(w * 0.26f, h * 0.24f)
    )
    drawCircle(
        color = Color(0xFF1E293B),
        radius = w * 0.05f,
        center = Offset(w * 0.74f, h * 0.76f)
    )
}

private fun DrawScope.drawJavaCoffeeIcon() {
    val w = size.width
    val h = size.height
    val stroke = (w * 0.09f).coerceAtLeast(1.3f)

    // Orange steam waves
    drawLine(
        color = Color(0xFFF89820),
        start = Offset(w * 0.38f, h * 0.12f),
        end = Offset(w * 0.34f, h * 0.34f),
        strokeWidth = stroke,
        cap = StrokeCap.Round
    )
    drawLine(
        color = Color(0xFFF89820),
        start = Offset(w * 0.58f, h * 0.10f),
        end = Offset(w * 0.54f, h * 0.34f),
        strokeWidth = stroke,
        cap = StrokeCap.Round
    )

    // Blue cup
    drawRoundRect(
        color = Color(0xFF5382A1),
        topLeft = Offset(w * 0.18f, h * 0.42f),
        size = Size(w * 0.52f, h * 0.38f),
        cornerRadius = CornerRadius(w * 0.12f, w * 0.12f)
    )
    drawCircle(
        color = Color(0xFF5382A1),
        radius = w * 0.12f,
        center = Offset(w * 0.72f, h * 0.58f),
        style = Stroke(width = stroke)
    )
    drawLine(
        color = Color(0xFFE76F00),
        start = Offset(w * 0.14f, h * 0.88f),
        end = Offset(w * 0.82f, h * 0.88f),
        strokeWidth = stroke,
        cap = StrokeCap.Round
    )
}

private fun DrawScope.drawSqlCylinderIcon() {
    val w = size.width
    val h = size.height
    val gold = Color(0xFFE38C00)
    val darkGold = Color(0xFFB45309)

    drawRoundRect(
        color = darkGold,
        topLeft = Offset(w * 0.14f, h * 0.22f),
        size = Size(w * 0.72f, h * 0.62f),
        cornerRadius = CornerRadius(w * 0.20f, w * 0.20f)
    )
    drawOval(
        color = Color(0xFFFBBF24),
        topLeft = Offset(w * 0.14f, h * 0.10f),
        size = Size(w * 0.72f, h * 0.26f)
    )
    drawOval(
        color = gold,
        topLeft = Offset(w * 0.14f, h * 0.38f),
        size = Size(w * 0.72f, h * 0.24f)
    )
    drawOval(
        color = gold,
        topLeft = Offset(w * 0.14f, h * 0.64f),
        size = Size(w * 0.72f, h * 0.24f)
    )
}

private fun DrawScope.drawPlainDocumentIcon() {
    val w = size.width
    val h = size.height
    val doc = Path().apply {
        moveTo(w * 0.16f, h * 0.06f)
        lineTo(w * 0.62f, h * 0.06f)
        lineTo(w * 0.84f, h * 0.28f)
        lineTo(w * 0.84f, h * 0.94f)
        lineTo(w * 0.16f, h * 0.94f)
        close()
    }
    drawPath(doc, color = Color(0xFF64748B))
    val stroke = (w * 0.08f).coerceAtLeast(1.1f)
    drawLine(
        color = Color.White,
        start = Offset(w * 0.28f, h * 0.44f),
        end = Offset(w * 0.72f, h * 0.44f),
        strokeWidth = stroke,
        cap = StrokeCap.Round
    )
    drawLine(
        color = Color.White,
        start = Offset(w * 0.28f, h * 0.62f),
        end = Offset(w * 0.72f, h * 0.62f),
        strokeWidth = stroke,
        cap = StrokeCap.Round
    )
}
