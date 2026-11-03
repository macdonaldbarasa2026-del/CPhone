package com.cphone.editor.syntax

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.cphone.editor.model.SupportedLanguage
import java.util.regex.Pattern

/**
 * VSCode-quality syntax highlighting engine.
 * Dark theme: One Dark Pro palette. Light theme: GitHub Light palette.
 */
object SyntaxHighlighter {

    data class SyntaxColors(
        val keyword: Color,
        val type: Color,
        val function: Color,
        val string: Color,
        val number: Color,
        val comment: Color,
        val variable: Color,
        val operator: Color,
        val decorator: Color,
        val builtin: Color,
        val tag: Color,
        val attribute: Color,
        val defaultText: Color
    )

    // Dark — One Dark Pro
    val DarkColors = SyntaxColors(
        keyword     = Color(0xFFC678DD),
        type        = Color(0xFFE5C07B),
        function    = Color(0xFF61AFEF),
        string      = Color(0xFF98C379),
        number      = Color(0xFFD19A66),
        comment     = Color(0xFF5C6370),
        variable    = Color(0xFFE06C75),
        operator    = Color(0xFF56B6C2),
        decorator   = Color(0xFFE5C07B),
        builtin     = Color(0xFF56B6C2),
        tag         = Color(0xFFE06C75),
        attribute   = Color(0xFFD19A66),
        defaultText = Color(0xFFABB2BF)
    )

    // Light — GitHub Light
    val LightColors = SyntaxColors(
        keyword     = Color(0xFFCF222E),
        type        = Color(0xFF953800),
        function    = Color(0xFF8250DF),
        string      = Color(0xFF0A3069),
        number      = Color(0xFF0550AE),
        comment     = Color(0xFF6E7781),
        variable    = Color(0xFF116329),
        operator    = Color(0xFF24292F),
        decorator   = Color(0xFF953800),
        builtin     = Color(0xFF0550AE),
        tag         = Color(0xFF116329),
        attribute   = Color(0xFF953800),
        defaultText = Color(0xFF24292F)
    )

    fun highlight(text: String, language: SupportedLanguage, isDarkMode: Boolean): AnnotatedString {
        if (text.isEmpty()) return AnnotatedString("")
        val colors = if (isDarkMode) DarkColors else LightColors

        return buildAnnotatedString {
            append(text)
            addStyle(SpanStyle(color = colors.defaultText), 0, text.length)
            try {
                when (language) {
                    SupportedLanguage.PYTHON     -> highlightPython(text, colors)
                    SupportedLanguage.KOTLIN     -> highlightKotlin(text, colors)
                    SupportedLanguage.JAVA       -> highlightJava(text, colors)
                    SupportedLanguage.JAVASCRIPT,
                    SupportedLanguage.TYPESCRIPT -> highlightJS(text, colors)
                    SupportedLanguage.CPP        -> highlightCpp(text, colors)
                    SupportedLanguage.HTML       -> highlightHTML(text, colors)
                    SupportedLanguage.XML        -> highlightXML(text, colors)
                    SupportedLanguage.CSS        -> highlightCSS(text, colors)
                    SupportedLanguage.JSON       -> highlightJSON(text, colors)
                    SupportedLanguage.SHELL      -> highlightShell(text, colors)
                    SupportedLanguage.PHP        -> highlightPHP(text, colors)
                    SupportedLanguage.MARKDOWN   -> highlightMarkdown(text, colors)
                    SupportedLanguage.YAML       -> highlightYAML(text, colors)
                    SupportedLanguage.BATCH      -> highlightBatch(text, colors)
                    SupportedLanguage.PLAINTEXT  -> { /* keep default */ }
                }
            } catch (_: Exception) { }
        }
    }

    // ── Python ───────────────────────────────────────────────────────────────
    private fun AnnotatedString.Builder.highlightPython(text: String, c: SyntaxColors) {
        applyRegex(text, """("{3}|'{3})[\s\S]*?\1""", SpanStyle(color = c.string))
        applyRegex(text, """"[^"\\\n]*(\\.[^"\\\n]*)*"""", SpanStyle(color = c.string))
        applyRegex(text, """'[^'\\\n]*(\\.[^'\\\n]*)*'""", SpanStyle(color = c.string))
        applyRegex(text, """\b\d+(\.\d+)?([eE][+-]?\d+)?\b|\b0x[0-9a-fA-F]+\b""", SpanStyle(color = c.number))
        applyRegex(text, """@[a-zA-Z_]\w*""", SpanStyle(color = c.decorator, fontWeight = FontWeight.SemiBold))
        applyRegex(text, """\b(False|None|True|and|as|assert|async|await|break|class|continue|def|del|elif|else|except|finally|for|from|global|if|import|in|is|lambda|nonlocal|not|or|pass|raise|return|try|while|with|yield)\b""",
            SpanStyle(color = c.keyword, fontWeight = FontWeight.SemiBold))
        applyRegex(text, """\b(abs|all|any|bin|bool|bytes|callable|chr|compile|complex|delattr|dict|dir|divmod|enumerate|eval|exec|filter|float|format|frozenset|getattr|globals|hasattr|hash|help|hex|id|input|int|isinstance|issubclass|iter|len|list|locals|map|max|memoryview|min|next|object|oct|open|ord|pow|print|property|range|repr|reversed|round|set|setattr|slice|sorted|staticmethod|str|sum|super|tuple|type|vars|zip)\b""",
            SpanStyle(color = c.builtin))
        applyRegex(text, """\b[A-Z][a-zA-Z0-9_]*\b""", SpanStyle(color = c.type))
        applyRegex(text, """(?<=def\s)[a-zA-Z_]\w*""", SpanStyle(color = c.function, fontWeight = FontWeight.SemiBold))
        applyRegex(text, """(?<=class\s)[a-zA-Z_]\w*""", SpanStyle(color = c.type, fontWeight = FontWeight.Bold))
        applyRegex(text, """\bself\b|\bcls\b""", SpanStyle(color = c.variable, fontStyle = FontStyle.Italic))
        applyRegex(text, """#.*$""", SpanStyle(color = c.comment, fontStyle = FontStyle.Italic))
    }

    // ── Kotlin ───────────────────────────────────────────────────────────────
    private fun AnnotatedString.Builder.highlightKotlin(text: String, c: SyntaxColors) {
        applyRegex(text, """""{3}[\s\S]*?"{3}""", SpanStyle(color = c.string))
        applyRegex(text, """"[^"\\\n]*(\\.[^"\\\n]*)*"""", SpanStyle(color = c.string))
        applyRegex(text, """'[^'\\\n]*(\\.[^'\\\n]*)*'""", SpanStyle(color = c.string))
        applyRegex(text, """\b\d+[lLfFdD]?(\.\d+[fFdD]?)?\b|\b0x[0-9a-fA-F]+\b""", SpanStyle(color = c.number))
        applyRegex(text, """@[a-zA-Z_]\w*""", SpanStyle(color = c.decorator))
        applyRegex(text, """\b(abstract|actual|annotation|as|break|by|catch|class|companion|const|constructor|continue|crossinline|data|delegate|do|else|enum|expect|external|false|field|file|final|finally|for|fun|get|if|import|in|infix|init|inline|inner|interface|internal|is|it|lateinit|noinline|null|object|open|operator|out|override|package|param|private|property|protected|public|receiver|reified|return|sealed|set|super|suspend|tailrec|this|throw|true|try|typealias|typeof|val|var|vararg|when|where|while)\b""",
            SpanStyle(color = c.keyword, fontWeight = FontWeight.SemiBold))
        applyRegex(text, """\b[A-Z][a-zA-Z0-9_]*\b""", SpanStyle(color = c.type))
        applyRegex(text, """(?<=fun\s)[a-zA-Z_]\w*""", SpanStyle(color = c.function, fontWeight = FontWeight.SemiBold))
        applyRegex(text, """(?<=class\s|object\s|interface\s)[a-zA-Z_]\w*""", SpanStyle(color = c.type, fontWeight = FontWeight.Bold))
        applyRegex(text, """\$\{[^}]*\}|\$[a-zA-Z_]\w*""", SpanStyle(color = c.variable))
        applyRegex(text, """//.*$|/\*[\s\S]*?\*/""", SpanStyle(color = c.comment, fontStyle = FontStyle.Italic))
    }

    // ── Java ─────────────────────────────────────────────────────────────────
    private fun AnnotatedString.Builder.highlightJava(text: String, c: SyntaxColors) {
        applyRegex(text, """"[^"\\\n]*(\\.[^"\\\n]*)*"""", SpanStyle(color = c.string))
        applyRegex(text, """'[^'\\\n]*(\\.[^'\\\n]*)*'""", SpanStyle(color = c.string))
        applyRegex(text, """\b\d+[lLfFdD]?(\.\d+[fFdD]?)?\b""", SpanStyle(color = c.number))
        applyRegex(text, """@[a-zA-Z_]\w*""", SpanStyle(color = c.decorator))
        applyRegex(text, """\b(abstract|assert|boolean|break|byte|case|catch|char|class|const|continue|default|do|double|else|enum|extends|final|finally|float|for|goto|if|implements|import|instanceof|int|interface|long|native|new|null|package|private|protected|public|return|short|static|strictfp|super|switch|synchronized|this|throw|throws|transient|true|false|try|void|volatile|while|record|sealed|permits|var)\b""",
            SpanStyle(color = c.keyword, fontWeight = FontWeight.SemiBold))
        applyRegex(text, """\b[A-Z][a-zA-Z0-9_]*\b""", SpanStyle(color = c.type))
        applyRegex(text, """(?<=class\s|interface\s|enum\s)[a-zA-Z_]\w*""", SpanStyle(color = c.type, fontWeight = FontWeight.Bold))
        applyRegex(text, """//.*$|/\*[\s\S]*?\*/""", SpanStyle(color = c.comment, fontStyle = FontStyle.Italic))
    }

    // ── JavaScript / TypeScript ───────────────────────────────────────────────
    private fun AnnotatedString.Builder.highlightJS(text: String, c: SyntaxColors) {
        applyRegex(text, """`[^`\\]*(\\.[^`\\]*)*`""", SpanStyle(color = c.string))
        applyRegex(text, """"[^"\\\n]*(\\.[^"\\\n]*)*"""", SpanStyle(color = c.string))
        applyRegex(text, """'[^'\\\n]*(\\.[^'\\\n]*)*'""", SpanStyle(color = c.string))
        applyRegex(text, """\b\d+(\.\d+)?\b|\b0x[0-9a-fA-F]+\b""", SpanStyle(color = c.number))
        applyRegex(text, """@[a-zA-Z_]\w*""", SpanStyle(color = c.decorator))
        applyRegex(text, """\b(abstract|arguments|async|await|break|case|catch|class|const|continue|debugger|default|delete|do|else|enum|eval|export|extends|false|finally|for|from|function|get|if|implements|import|in|instanceof|interface|let|new|null|of|package|private|protected|public|return|set|static|super|switch|this|throw|true|try|type|typeof|undefined|var|void|while|with|yield|declare|namespace|module|keyof|readonly|as|satisfies)\b""",
            SpanStyle(color = c.keyword, fontWeight = FontWeight.SemiBold))
        applyRegex(text, """\b(console|Math|Object|Array|String|Number|Boolean|Date|RegExp|Error|Map|Set|Promise|Symbol|JSON|parseInt|parseFloat|isNaN|isFinite|encodeURI|decodeURI|fetch|window|document|process|require|module|exports|setTimeout|clearTimeout|setInterval|clearInterval)\b""",
            SpanStyle(color = c.builtin))
        applyRegex(text, """\b[A-Z][a-zA-Z0-9_]*\b""", SpanStyle(color = c.type))
        applyRegex(text, """(?<=function\s)[a-zA-Z_]\w*""", SpanStyle(color = c.function, fontWeight = FontWeight.SemiBold))
        applyRegex(text, """\$\{[^}]*\}""", SpanStyle(color = c.variable))
        applyRegex(text, """//.*$|/\*[\s\S]*?\*/""", SpanStyle(color = c.comment, fontStyle = FontStyle.Italic))
    }

    // ── C / C++ ──────────────────────────────────────────────────────────────
    private fun AnnotatedString.Builder.highlightCpp(text: String, c: SyntaxColors) {
        applyRegex(text, """"[^"\\\n]*(\\.[^"\\\n]*)*"""", SpanStyle(color = c.string))
        applyRegex(text, """'[^'\\\n]*'""", SpanStyle(color = c.string))
        applyRegex(text, """\b\d+[uUlLfF]*(\.\d+[fF]?)?\b|\b0x[0-9a-fA-F]+\b""", SpanStyle(color = c.number))
        applyRegex(text, """(?m)^\s*#\w+""", SpanStyle(color = c.decorator, fontWeight = FontWeight.SemiBold))
        applyRegex(text, """\b(alignas|alignof|and|and_eq|asm|auto|bitand|bitor|bool|break|case|catch|char|char8_t|char16_t|char32_t|class|compl|concept|const|consteval|constexpr|constinit|const_cast|continue|co_await|co_return|co_yield|decltype|default|delete|do|double|dynamic_cast|else|enum|explicit|export|extern|false|float|for|friend|goto|if|inline|int|long|mutable|namespace|new|noexcept|not|not_eq|nullptr|operator|or|or_eq|private|protected|public|register|reinterpret_cast|requires|return|short|signed|sizeof|static|static_assert|static_cast|struct|switch|template|this|thread_local|throw|true|try|typedef|typeid|typename|union|unsigned|using|virtual|void|volatile|wchar_t|while|xor|xor_eq|override|final)\b""",
            SpanStyle(color = c.keyword, fontWeight = FontWeight.SemiBold))
        applyRegex(text, """\b(std|cout|cin|cerr|endl|vector|map|set|string|array|pair|tuple|unique_ptr|shared_ptr|optional|variant|printf|scanf|malloc|free|nullptr|NULL)\b""",
            SpanStyle(color = c.builtin))
        applyRegex(text, """\b[A-Z][a-zA-Z0-9_]*\b""", SpanStyle(color = c.type))
        applyRegex(text, """//.*$|/\*[\s\S]*?\*/""", SpanStyle(color = c.comment, fontStyle = FontStyle.Italic))
    }

    // ── HTML ─────────────────────────────────────────────────────────────────
    private fun AnnotatedString.Builder.highlightHTML(text: String, c: SyntaxColors) {
        applyRegex(text, """<!--[\s\S]*?-->""", SpanStyle(color = c.comment, fontStyle = FontStyle.Italic))
        applyRegex(text, """"[^"]*"|'[^']*'""", SpanStyle(color = c.string))
        applyRegex(text, """</?\w[\w.-]*""", SpanStyle(color = c.tag, fontWeight = FontWeight.SemiBold))
        applyRegex(text, """[/?]>|>""", SpanStyle(color = c.tag))
        applyRegex(text, """\b(id|class|style|href|src|alt|type|name|value|placeholder|onclick|onload|rel|lang|charset|content|method|action|for|target|width|height)\b""",
            SpanStyle(color = c.attribute))
        applyRegex(text, """<!DOCTYPE[^>]*>""", SpanStyle(color = c.keyword))
    }

    // ── XML ──────────────────────────────────────────────────────────────────
    private fun AnnotatedString.Builder.highlightXML(text: String, c: SyntaxColors) {
        applyRegex(text, """<!--[\s\S]*?-->""", SpanStyle(color = c.comment, fontStyle = FontStyle.Italic))
        applyRegex(text, """"[^"]*"|'[^']*'""", SpanStyle(color = c.string))
        applyRegex(text, """</?\w[\w:.-]*""", SpanStyle(color = c.tag, fontWeight = FontWeight.SemiBold))
        applyRegex(text, """[/?]>|>""", SpanStyle(color = c.tag))
        applyRegex(text, """\b[\w:]+(?=\s*=)""", SpanStyle(color = c.attribute))
        applyRegex(text, """<\?xml[^?]*\?>""", SpanStyle(color = c.keyword))
    }

    // ── CSS ───────────────────────────────────────────────────────────────────
    private fun AnnotatedString.Builder.highlightCSS(text: String, c: SyntaxColors) {
        applyRegex(text, """"[^"]*"|'[^']*'""", SpanStyle(color = c.string))
        applyRegex(text, """@[\w-]+""", SpanStyle(color = c.keyword, fontWeight = FontWeight.SemiBold))
        applyRegex(text, """[\w-]+(?=\s*:)""", SpanStyle(color = c.function))
        applyRegex(text, """\b\d+(\.\d+)?(px|em|rem|%|vh|vw|pt|fr|s|ms|deg)?\b""", SpanStyle(color = c.number))
        applyRegex(text, """#[0-9a-fA-F]{3,8}\b""", SpanStyle(color = c.number))
        applyRegex(text, """[.#][\w-]+""", SpanStyle(color = c.type))
        applyRegex(text, """::?[\w-]+""", SpanStyle(color = c.builtin))
        applyRegex(text, """/\*[\s\S]*?\*/""", SpanStyle(color = c.comment, fontStyle = FontStyle.Italic))
    }

    // ── JSON ─────────────────────────────────────────────────────────────────
    private fun AnnotatedString.Builder.highlightJSON(text: String, c: SyntaxColors) {
        applyRegex(text, """"[^"]*"(?=\s*:)""", SpanStyle(color = c.function, fontWeight = FontWeight.SemiBold))
        applyRegex(text, """(?<=:\s*)"[^"]*"""", SpanStyle(color = c.string))
        applyRegex(text, """\b-?\d+(\.\d+)?([eE][+-]?\d+)?\b""", SpanStyle(color = c.number))
        applyRegex(text, """\b(true|false|null)\b""", SpanStyle(color = c.builtin, fontWeight = FontWeight.SemiBold))
    }

    // ── Shell ─────────────────────────────────────────────────────────────────
    private fun AnnotatedString.Builder.highlightShell(text: String, c: SyntaxColors) {
        applyRegex(text, """"[^"]*"|'[^']*'""", SpanStyle(color = c.string))
        applyRegex(text, """\$\{[^}]+\}|\$[a-zA-Z_]\w*""", SpanStyle(color = c.variable))
        applyRegex(text, """\b\d+\b""", SpanStyle(color = c.number))
        applyRegex(text, """\b(if|then|else|elif|fi|case|esac|for|select|while|until|do|done|in|function|return|exit|break|continue|local|export|source|readonly|declare|typeset|unset|shift|getopts|trap)\b""",
            SpanStyle(color = c.keyword, fontWeight = FontWeight.SemiBold))
        applyRegex(text, """\b(echo|printf|read|cd|ls|mkdir|rm|cp|mv|cat|grep|sed|awk|cut|sort|uniq|head|tail|find|xargs|chmod|chown|wget|curl|tar|zip|ssh|scp|ps|kill|systemctl|apt|pip|npm|git)\b""",
            SpanStyle(color = c.builtin))
        applyRegex(text, """(?m)^\s*#.*$""", SpanStyle(color = c.comment, fontStyle = FontStyle.Italic))
    }

    // ── PHP ──────────────────────────────────────────────────────────────────
    private fun AnnotatedString.Builder.highlightPHP(text: String, c: SyntaxColors) {
        applyRegex(text, """"[^"]*"|'[^']*'""", SpanStyle(color = c.string))
        applyRegex(text, """\$[a-zA-Z_]\w*""", SpanStyle(color = c.variable))
        applyRegex(text, """\b\d+(\.\d+)?\b""", SpanStyle(color = c.number))
        applyRegex(text, """\b(abstract|array|as|break|callable|case|catch|class|clone|const|continue|declare|default|do|echo|else|elseif|empty|extends|final|finally|fn|for|foreach|function|global|goto|if|implements|include|include_once|instanceof|interface|isset|list|match|namespace|new|null|print|private|protected|public|readonly|require|require_once|return|static|switch|throw|trait|true|false|try|unset|use|var|while|yield|enum)\b""",
            SpanStyle(color = c.keyword, fontWeight = FontWeight.SemiBold))
        applyRegex(text, """//.*$|#.*$|/\*[\s\S]*?\*/""", SpanStyle(color = c.comment, fontStyle = FontStyle.Italic))
    }

    // ── Markdown ──────────────────────────────────────────────────────────────
    private fun AnnotatedString.Builder.highlightMarkdown(text: String, c: SyntaxColors) {
        applyRegex(text, """```[\s\S]*?```""", SpanStyle(color = c.string))
        applyRegex(text, """`[^`]+`""", SpanStyle(color = c.string))
        applyRegex(text, """(?m)^#{1,6}\s.*$""", SpanStyle(color = c.keyword, fontWeight = FontWeight.Bold))
        applyRegex(text, """\*\*[^*]+\*\*|__[^_]+__""", SpanStyle(fontWeight = FontWeight.Bold))
        applyRegex(text, """\*[^*\n]+\*|_[^_\n]+_""", SpanStyle(fontStyle = FontStyle.Italic))
        applyRegex(text, """\[[^\]]+\]\([^)]+\)""", SpanStyle(color = c.function))
        applyRegex(text, """(?m)^>\s.*$""", SpanStyle(color = c.comment))
        applyRegex(text, """(?m)^\s*[-*+]\s""", SpanStyle(color = c.operator))
        applyRegex(text, """(?m)^\s*\d+\.\s""", SpanStyle(color = c.operator))
    }

    // ── YAML ─────────────────────────────────────────────────────────────────
    private fun AnnotatedString.Builder.highlightYAML(text: String, c: SyntaxColors) {
        applyRegex(text, """"[^"]*"|'[^']*'""", SpanStyle(color = c.string))
        applyRegex(text, """(?m)^\s*[\w.-]+(?=\s*:)""", SpanStyle(color = c.function, fontWeight = FontWeight.SemiBold))
        applyRegex(text, """&\w+|\*\w+""", SpanStyle(color = c.decorator))
        applyRegex(text, """!!?\w+""", SpanStyle(color = c.type))
        applyRegex(text, """\b(true|false|yes|no|null)\b|\b\d+(\.\d+)?\b""", SpanStyle(color = c.builtin))
        applyRegex(text, """#.*$""", SpanStyle(color = c.comment, fontStyle = FontStyle.Italic))
    }

    // ── Batch / CMD ──────────────────────────────────────────────────────────
    private fun AnnotatedString.Builder.highlightBatch(text: String, c: SyntaxColors) {
        applyRegex(text, """"[^"]*"""", SpanStyle(color = c.string))
        applyRegex(text, """\b\d+(\.\d+)*\b""", SpanStyle(color = c.number))
        applyRegex(text, """%[a-zA-Z0-9_~]*%?|![a-zA-Z0-9_]+!""", SpanStyle(color = c.variable))
        applyRegex(text, """(?i)\b(echo|title|set|if|not|exist|defined|else|goto|call|start|exit|pause|cls|color|for|in|do|rem|setlocal|endlocal|mkdir|md|rmdir|rd|copy|xcopy|robocopy|del|erase|move|ren|rename|type|more|find|findstr|reg|sc|net|netsh|ping|ipconfig|nslookup|tracert|pushd|popd|shift|choice|timeout|attrib|dir|cd|chdir|tasklist|taskkill|wmic|powershell|cmd|runas|assoc|ftype|path|prompt|mode|ver)\b""",
            SpanStyle(color = c.keyword, fontWeight = FontWeight.SemiBold))
        applyRegex(text, """(?m)^\s*:[a-zA-Z_]\w*""", SpanStyle(color = c.function))
        applyRegex(text, """(?m)^\s*(rem\b|::).*$""", SpanStyle(color = c.comment, fontStyle = FontStyle.Italic))
    }

    // ── Regex helper ─────────────────────────────────────────────────────────
    private fun AnnotatedString.Builder.applyRegex(
        text: String,
        regexPattern: String,
        style: SpanStyle,
        flags: Int = Pattern.MULTILINE
    ) {
        try {
            val matcher = Pattern.compile(regexPattern, flags).matcher(text)
            while (matcher.find()) {
                val s = matcher.start()
                val e = matcher.end()
                if (s in 0 until text.length && e <= text.length && s < e) {
                    addStyle(style, s, e)
                }
            }
        } catch (_: Exception) { }
    }
}
