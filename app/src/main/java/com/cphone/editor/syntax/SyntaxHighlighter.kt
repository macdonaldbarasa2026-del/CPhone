package com.cphone.editor.syntax

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import com.cphone.editor.model.SupportedLanguage
import java.util.regex.Pattern

object SyntaxHighlighter {

    data class SyntaxColors(
        val keyword: Color,
        val string: Color,
        val number: Color,
        val comment: Color,
        val variable: Color,
        val operator: Color,
        val defaultText: Color
    )

    val LightColors = SyntaxColors(
        keyword = Color(0xFF0288D1),
        string = Color(0xFF2E7D32),
        number = Color(0xFFD32F2F),
        comment = Color(0xFF757575),
        variable = Color(0xFF00838F),
        operator = Color(0xFF37474F),
        defaultText = Color(0xFF212121)
    )

    val DarkColors = SyntaxColors(
        keyword = Color(0xFF4FC3F7),
        string = Color(0xFF81C784),
        number = Color(0xFFE57373),
        comment = Color(0xFF9E9E9E),
        variable = Color(0xFF4DD0E1),
        operator = Color(0xFFCFD8DC),
        defaultText = Color(0xFFECEFF1)
    )

    fun highlight(
        text: String,
        language: SupportedLanguage,
        isDarkMode: Boolean
    ): AnnotatedString {
        if (text.isEmpty()) return AnnotatedString("")

        val colors = if (isDarkMode) DarkColors else LightColors

        return buildAnnotatedString {
            append(text)

            // Default style
            addStyle(
                SpanStyle(color = colors.defaultText),
                0,
                text.length
            )

            try {
                when (language) {
                    SupportedLanguage.BATCH -> highlightBatch(text, colors)
                    SupportedLanguage.PYTHON -> highlightPython(text, colors)
                    SupportedLanguage.JAVA,
                    SupportedLanguage.KOTLIN,
                    SupportedLanguage.CPP -> highlightCStyle(text, colors)
                    SupportedLanguage.JAVASCRIPT,
                    SupportedLanguage.TYPESCRIPT -> highlightJS(text, colors)
                    SupportedLanguage.HTML,
                    SupportedLanguage.XML -> highlightMarkup(text, colors)
                    SupportedLanguage.CSS -> highlightCSS(text, colors)
                    SupportedLanguage.JSON -> highlightJSON(text, colors)
                    SupportedLanguage.SHELL -> highlightShell(text, colors)
                }
            } catch (_: Exception) {
                // Keep default style on any regex parsing edge cases
            }
        }
    }

    private fun AnnotatedString.Builder.highlightBatch(text: String, colors: SyntaxColors) {
        // Strings
        highlightRegex(text, "\"[^\"]*\"|'[^']*'", SpanStyle(color = colors.string))

        // Numbers & IP addresses (e.g., 192.168.49.1 or isolated integers)
        highlightRegex(text, "\\b\\d+(\\.\\d+)*\\b", SpanStyle(color = colors.number))

        // Variables: %VAR%, %1, !VAR!
        highlightRegex(text, "%[a-zA-Z0-9_]+%|%~?[a-zA-Z0-9_]+|![a-zA-Z0-9_]+!", SpanStyle(color = colors.variable))

        // Keywords and Commands (case insensitive in batch)
        val batchKeywords = "(?i)\\b(@echo|echo|title|reg|add|set|if|not|exist|mkdir|rmdir|copy|xcopy|del|move|ren|call|goto|start|exit|pause|cls|color|find|findstr|for|in|do|nul|defined|else|shift|pushd|popd)\\b"
        highlightRegex(text, batchKeywords, SpanStyle(color = colors.keyword, fontWeight = FontWeight.Normal))

        // Comments: rem ... or :: ...
        highlightRegex(text, "(?m)^\\s*(::|rem\\b).*$", SpanStyle(color = colors.comment))
    }

    private fun AnnotatedString.Builder.highlightPython(text: String, colors: SyntaxColors) {
        // Strings
        highlightRegex(text, "\"\"\"[\\s\\S]*?\"\"\"|'''[\\s\\S]*?'''|\"[^\"]*\"|'[^']*'", SpanStyle(color = colors.string))

        // Numbers
        highlightRegex(text, "\\b\\d+(\\.\\d+)?\\b", SpanStyle(color = colors.number))

        // Keywords
        val pyKeywords = "\\b(def|class|import|from|as|return|if|elif|else|while|for|in|try|except|finally|with|raise|assert|yield|lambda|pass|break|continue|None|True|False|async|await|and|or|not|is|self)\\b"
        highlightRegex(text, pyKeywords, SpanStyle(color = colors.keyword, fontWeight = FontWeight.SemiBold))

        // Comments
        highlightRegex(text, "#.*$", SpanStyle(color = colors.comment))
    }

    private fun AnnotatedString.Builder.highlightCStyle(text: String, colors: SyntaxColors) {
        // Strings
        highlightRegex(text, "\"[^\"]*\"|'[^']*'", SpanStyle(color = colors.string))

        // Numbers
        highlightRegex(text, "\\b\\d+(\\.\\d+)?([fFlLuU])?\\b", SpanStyle(color = colors.number))

        // Keywords
        val cKeywords = "\\b(fun|val|var|class|interface|object|enum|public|private|protected|internal|override|return|if|else|when|while|for|do|try|catch|finally|throw|import|package|null|true|false|this|super|void|int|float|double|boolean|char|long|short|byte|static|final|new|const|struct|typedef|switch|case|break|continue|default|namespace|using)\\b"
        highlightRegex(text, cKeywords, SpanStyle(color = colors.keyword, fontWeight = FontWeight.SemiBold))

        // Line and block comments
        highlightRegex(text, "//.*$|/\\*[\\s\\S]*?\\*/", SpanStyle(color = colors.comment))
    }

    private fun AnnotatedString.Builder.highlightJS(text: String, colors: SyntaxColors) {
        // Strings
        highlightRegex(text, "`[\\s\\S]*?`|\"[^\"]*\"|'[^']*'", SpanStyle(color = colors.string))

        // Numbers
        highlightRegex(text, "\\b\\d+(\\.\\d+)?\\b", SpanStyle(color = colors.number))

        // Keywords
        val jsKeywords = "\\b(const|let|var|function|return|if|else|for|while|do|switch|case|break|continue|default|try|catch|finally|throw|new|class|extends|super|this|import|export|from|as|default|async|await|yield|typeof|instanceof|null|undefined|true|false|interface|type)\\b"
        highlightRegex(text, jsKeywords, SpanStyle(color = colors.keyword, fontWeight = FontWeight.SemiBold))

        // Comments
        highlightRegex(text, "//.*$|/\\*[\\s\\S]*?\\*/", SpanStyle(color = colors.comment))
    }

    private fun AnnotatedString.Builder.highlightMarkup(text: String, colors: SyntaxColors) {
        // Strings (attributes)
        highlightRegex(text, "\"[^\"]*\"|'[^']*'", SpanStyle(color = colors.string))

        // Tags <tag> and </tag>
        highlightRegex(text, "<[/]?[a-zA-Z0-9_-]+", SpanStyle(color = colors.keyword))
        highlightRegex(text, "[/]?>", SpanStyle(color = colors.keyword))

        // Comments <!-- ... -->
        highlightRegex(text, "<!--[\\s\\S]*?-->", SpanStyle(color = colors.comment))
    }

    private fun AnnotatedString.Builder.highlightCSS(text: String, colors: SyntaxColors) {
        // Strings
        highlightRegex(text, "\"[^\"]*\"|'[^']*'", SpanStyle(color = colors.string))

        // Selectors & properties
        highlightRegex(text, "[a-zA-Z-]+(?=\\s*:)", SpanStyle(color = colors.keyword))

        // Values & units
        highlightRegex(text, "\\b\\d+(px|em|rem|%|vh|vw|pt)?\\b|#[0-9a-fA-F]{3,8}", SpanStyle(color = colors.number))

        // Comments
        highlightRegex(text, "/\\*[\\s\\S]*?\\*/", SpanStyle(color = colors.comment))
    }

    private fun AnnotatedString.Builder.highlightJSON(text: String, colors: SyntaxColors) {
        // Keys: "key":
        highlightRegex(text, "\"[^\"]*\"(?=\\s*:)", SpanStyle(color = colors.keyword))

        // String values
        highlightRegex(text, "(?<=:\\s*)\"[^\"]*\"", SpanStyle(color = colors.string))

        // Numbers
        highlightRegex(text, "\\b-?\\d+(\\.\\d+)?\\b", SpanStyle(color = colors.number))

        // Booleans & null
        highlightRegex(text, "\\b(true|false|null)\\b", SpanStyle(color = colors.variable))
    }

    private fun AnnotatedString.Builder.highlightShell(text: String, colors: SyntaxColors) {
        // Strings
        highlightRegex(text, "\"[^\"]*\"|'[^']*'", SpanStyle(color = colors.string))

        // Variables: $VAR, ${VAR}
        highlightRegex(text, "\\$[a-zA-Z0-9_]+|\\$\\{[^}]+\\}", SpanStyle(color = colors.variable))

        // Numbers
        highlightRegex(text, "\\b\\d+\\b", SpanStyle(color = colors.number))

        // Keywords
        val shKeywords = "\\b(if|then|else|elif|fi|case|esac|for|select|while|until|do|done|in|function|return|exit|echo|set|export|local|read|cd|mkdir|rm|cp|mv|cat|grep|sed|awk)\\b"
        highlightRegex(text, shKeywords, SpanStyle(color = colors.keyword, fontWeight = FontWeight.Normal))

        // Comments
        highlightRegex(text, "(?m)^\\s*#.*$|(?<=\\s)#.*$", SpanStyle(color = colors.comment))
    }

    private fun AnnotatedString.Builder.highlightRegex(
        text: String,
        regexPattern: String,
        style: SpanStyle
    ) {
        try {
            val matcher = Pattern.compile(regexPattern).matcher(text)
            while (matcher.find()) {
                val start = matcher.start()
                val end = matcher.end()
                if (start in 0 until text.length && end <= text.length && start < end) {
                    addStyle(style, start, end)
                }
            }
        } catch (_: Exception) {
            // Ignore pattern errors
        }
    }
}
