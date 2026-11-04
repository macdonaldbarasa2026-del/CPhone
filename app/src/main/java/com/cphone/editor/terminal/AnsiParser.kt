package com.cphone.editor.terminal

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration

/**
 * High-performance ANSI escape sequence parser for Android terminal rendering.
 * Converts ANSI escape codes into Jetpack Compose AnnotatedString with proper colors and weights.
 */
object AnsiParser {

    private val ANSI_PATTERN = Regex("""\u001B\[([0-9;]*)m""")
    private val ANSI_STRIP_OTHER = Regex("""\u001B\[[0-9;]*[A-Za-z]|\u001B\][^\u0007\u001B]*[\u0007\u001B\\]|\r""")

    // Standard Terminal Palette (One Dark / Termux matching)
    private val COLOR_BLACK = Color(0xFF282C34)
    private val COLOR_RED = Color(0xFFE06C75)
    private val COLOR_GREEN = Color(0xFF98C379)
    private val COLOR_YELLOW = Color(0xFFE5C07B)
    private val COLOR_BLUE = Color(0xFF61AFEF)
    private val COLOR_MAGENTA = Color(0xFFC678DD)
    private val COLOR_CYAN = Color(0xFF56B6C2)
    private val COLOR_WHITE = Color(0xFFABB2BF)

    private val COLOR_BRIGHT_BLACK = Color(0xFF5C6370)
    private val COLOR_BRIGHT_RED = Color(0xFFBE5046)
    private val COLOR_BRIGHT_GREEN = Color(0xFFA5E075)
    private val COLOR_BRIGHT_YELLOW = Color(0xFFF0D18A)
    private val COLOR_BRIGHT_BLUE = Color(0xFF4FA6ED)
    private val COLOR_BRIGHT_MAGENTA = Color(0xFFD183E8)
    private val COLOR_BRIGHT_CYAN = Color(0xFF46C2D0)
    private val COLOR_BRIGHT_WHITE = Color(0xFFFFFFFF)

    fun parse(rawText: String, defaultColor: Color = Color(0xFFABB2BF)): AnnotatedString {
        if (!rawText.contains('\u001B')) {
            return AnnotatedString(rawText.replace("\r", ""))
        }

        val cleaned = rawText.replace(ANSI_STRIP_OTHER, "")
        return buildAnnotatedString {
            var currentIndex = 0
            var currentColor: Color = defaultColor
            var isBold = false
            var isUnderline = false

            val matches = ANSI_PATTERN.findAll(cleaned)

            for (match in matches) {
                val matchStart = match.range.first
                val matchEnd = match.range.last + 1

                if (matchStart > currentIndex) {
                    val segment = cleaned.substring(currentIndex, matchStart)
                    val style = SpanStyle(
                        color = currentColor,
                        fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
                        textDecoration = if (isUnderline) TextDecoration.Underline else TextDecoration.None
                    )
                    pushStyle(style)
                    append(segment)
                    pop()
                }

                // Process ANSI codes
                val codeStr = match.groupValues[1]
                val codes = if (codeStr.isEmpty()) listOf(0) else codeStr.split(';').mapNotNull { it.toIntOrNull() }

                for (code in codes) {
                    when (code) {
                        0 -> {
                            currentColor = defaultColor
                            isBold = false
                            isUnderline = false
                        }
                        1 -> isBold = true
                        4 -> isUnderline = true
                        22 -> isBold = false
                        24 -> isUnderline = false
                        // Standard Foreground
                        30 -> currentColor = COLOR_BLACK
                        31 -> currentColor = COLOR_RED
                        32 -> currentColor = COLOR_GREEN
                        33 -> currentColor = COLOR_YELLOW
                        34 -> currentColor = COLOR_BLUE
                        35 -> currentColor = COLOR_MAGENTA
                        36 -> currentColor = COLOR_CYAN
                        37 -> currentColor = COLOR_WHITE
                        39 -> currentColor = defaultColor
                        // Bright Foreground
                        90 -> currentColor = COLOR_BRIGHT_BLACK
                        91 -> currentColor = COLOR_BRIGHT_RED
                        92 -> currentColor = COLOR_BRIGHT_GREEN
                        93 -> currentColor = COLOR_BRIGHT_YELLOW
                        94 -> currentColor = COLOR_BRIGHT_BLUE
                        95 -> currentColor = COLOR_BRIGHT_MAGENTA
                        96 -> currentColor = COLOR_BRIGHT_CYAN
                        97 -> currentColor = COLOR_BRIGHT_WHITE
                    }
                }

                currentIndex = matchEnd
            }

            if (currentIndex < cleaned.length) {
                val remaining = cleaned.substring(currentIndex)
                val style = SpanStyle(
                    color = currentColor,
                    fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
                    textDecoration = if (isUnderline) TextDecoration.Underline else TextDecoration.None
                )
                pushStyle(style)
                append(remaining)
                pop()
            }
        }
    }
}
