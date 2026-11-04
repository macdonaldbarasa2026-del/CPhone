package com.cphone.editor.tools

import com.cphone.editor.model.SupportedLanguage
import org.json.JSONArray
import org.json.JSONObject

enum class DiagnosticSeverity {
    ERROR,
    WARNING,
    INFO
}

data class CodeDiagnostic(
    val line: Int,          // 1-indexed
    val column: Int,        // 1-indexed
    val severity: DiagnosticSeverity,
    val message: String,
    val rule: String = "syntax"
)

data class CodeSymbol(
    val name: String,
    val kind: String,       // "class", "function", "method", "section", "tag", "rule", "variable"
    val badge: String,      // "C", "ƒ", "M", "#", "<>", "{}"
    val line: Int           // 1-indexed
)

data class CursorTrailEntry(
    val tabId: String,
    val fileName: String,
    val line: Int,
    val column: Int,
    val symbolName: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class CodeSnippet(
    val title: String,
    val trigger: String,
    val description: String,
    val body: String
)

object CodeAnalyzer {

    // ── 1. Real-Time Bug & Syntax Diagnostics ────────────────────────────────
    fun analyzeDiagnostics(code: String, language: SupportedLanguage): List<CodeDiagnostic> {
        if (code.isBlank()) return emptyList()
        val diagnostics = mutableListOf<CodeDiagnostic>()
        val lines = code.split('\n')

        // Universal bracket balance check (ignoring comments and strings)
        if (language != SupportedLanguage.MARKDOWN && language != SupportedLanguage.PLAINTEXT) {
            checkBracketAndQuoteBalance(code, lines, language, diagnostics)
        }

        // Language-specific bug checks
        when (language) {
            SupportedLanguage.PYTHON -> checkPythonBugs(lines, diagnostics)
            SupportedLanguage.JSON -> checkJsonBugs(code, lines, diagnostics)
            SupportedLanguage.HTML, SupportedLanguage.XML -> checkHtmlXmlBugs(lines, diagnostics)
            SupportedLanguage.JAVASCRIPT, SupportedLanguage.TYPESCRIPT -> checkJsTsBugs(lines, diagnostics)
            SupportedLanguage.KOTLIN, SupportedLanguage.JAVA, SupportedLanguage.CPP,
            SupportedLanguage.RUST, SupportedLanguage.GO, SupportedLanguage.DART -> checkCFamilyBugs(lines, diagnostics)
            else -> {}
        }

        // Universal TODO / FIXME / BUG trail markers
        lines.forEachIndexed { idx, rawLine ->
            val todoMatch = Regex("""\b(TODO|FIXME|BUG|HACK)\b[:\s]*(.*)""", RegexOption.IGNORE_CASE).find(rawLine)
            if (todoMatch != null) {
                val tag = todoMatch.groupValues[1].uppercase()
                val note = todoMatch.groupValues[2].trim().ifEmpty { "$tag marker" }
                diagnostics.add(
                    CodeDiagnostic(
                        line = idx + 1,
                        column = todoMatch.range.first + 1,
                        severity = if (tag == "BUG" || tag == "FIXME") DiagnosticSeverity.WARNING else DiagnosticSeverity.INFO,
                        message = "$tag: $note",
                        rule = "trail-marker"
                    )
                )
            }
        }

        return diagnostics.sortedWith(compareBy({ it.severity.ordinal }, { it.line }, { it.column })).take(60)
    }

    private data class BracketPos(val char: Char, val line: Int, val col: Int)

    private fun checkBracketAndQuoteBalance(
        code: String,
        lines: List<String>,
        language: SupportedLanguage,
        out: MutableList<CodeDiagnostic>
    ) {
        val stack = ArrayDeque<BracketPos>()
        val pairs = mapOf('(' to ')', '[' to ']', '{' to '}')
        val closers = mapOf(')' to '(', ']' to '[', '}' to '{')

        var inBlockComment = false
        var inTripleQuote: String? = null

        lines.forEachIndexed { lineIdx, line ->
            val lineNum = lineIdx + 1
            var i = 0
            var inStringChar: Char? = null
            var stringStartCol = 1

            while (i < line.length) {
                // Handle Python triple quotes
                if (language == SupportedLanguage.PYTHON) {
                    if (inTripleQuote != null) {
                        val endIdx = line.indexOf(inTripleQuote!!, i)
                        if (endIdx != -1) {
                            i = endIdx + 3
                            inTripleQuote = null
                            continue
                        } else {
                            break
                        }
                    } else if (inStringChar == null && (line.startsWith("\"\"\"", i) || line.startsWith("'''", i))) {
                        val tq = line.substring(i, i + 3)
                        val closeIdx = line.indexOf(tq, i + 3)
                        if (closeIdx != -1) {
                            i = closeIdx + 3
                            continue
                        } else {
                            inTripleQuote = tq
                            break
                        }
                    }
                }

                // Handle C-style block comments
                if (inBlockComment) {
                    val closeIdx = line.indexOf("*/", i)
                    if (closeIdx != -1) {
                        i = closeIdx + 2
                        inBlockComment = false
                        continue
                    } else {
                        break
                    }
                }

                val ch = line[i]
                val next = if (i + 1 < line.length) line[i + 1] else '\u0000'

                // Inside single-line string
                if (inStringChar != null) {
                    if (ch == '\\') {
                        i += 2
                        continue
                    }
                    if (ch == inStringChar) {
                        inStringChar = null
                    }
                    i++
                    continue
                }

                // Line comments
                if ((ch == '/' && next == '/') ||
                    (ch == '#' && language in listOf(SupportedLanguage.PYTHON, SupportedLanguage.SHELL, SupportedLanguage.YAML)) ||
                    (ch == '-' && next == '-' && language == SupportedLanguage.SQL)
                ) {
                    break
                }

                // Block comment start
                if (ch == '/' && next == '*') {
                    inBlockComment = true
                    i += 2
                    continue
                }

                // String start
                if (ch == '"' || ch == '\'' || (ch == '`' && language in listOf(SupportedLanguage.JAVASCRIPT, SupportedLanguage.TYPESCRIPT, SupportedLanguage.GO))) {
                    // Ignore apostrophes inside plain text inside HTML tags
                    if ((language == SupportedLanguage.HTML || language == SupportedLanguage.XML) && ch == '\'') {
                        i++
                        continue
                    }
                    inStringChar = ch
                    stringStartCol = i + 1
                    i++
                    continue
                }

                if (ch in pairs) {
                    stack.addLast(BracketPos(ch, lineNum, i + 1))
                } else if (ch in closers) {
                    val expectedOpen = closers[ch]!!
                    if (stack.isEmpty()) {
                        out.add(
                            CodeDiagnostic(
                                line = lineNum,
                                column = i + 1,
                                severity = DiagnosticSeverity.ERROR,
                                message = "Unmatched closing '$ch' with no matching '$expectedOpen'",
                                rule = "bracket-balance"
                            )
                        )
                    } else {
                        val top = stack.removeLast()
                        if (top.char != expectedOpen) {
                            out.add(
                                CodeDiagnostic(
                                    line = lineNum,
                                    column = i + 1,
                                    severity = DiagnosticSeverity.ERROR,
                                    message = "Mismatched '$ch' (opened '${top.char}' on line ${top.line}:${top.col})",
                                    rule = "bracket-balance"
                                )
                            )
                        }
                    }
                }
                i++
            }

            if (inStringChar != null && inStringChar != '`') {
                out.add(
                    CodeDiagnostic(
                        line = lineNum,
                        column = stringStartCol,
                        severity = DiagnosticSeverity.ERROR,
                        message = "Unclosed string literal ($inStringChar)",
                        rule = "unclosed-string"
                    )
                )
            }
        }

        while (stack.isNotEmpty()) {
            val unclosed = stack.removeLast()
            val closer = pairs[unclosed.char] ?: ')'
            out.add(
                CodeDiagnostic(
                    line = unclosed.line,
                    column = unclosed.col,
                    severity = DiagnosticSeverity.ERROR,
                    message = "Unclosed '${unclosed.char}' — missing '$closer'",
                    rule = "bracket-balance"
                )
            )
        }
    }

    private fun checkPythonBugs(lines: List<String>, out: MutableList<CodeDiagnostic>) {
        var openParenDepth = 0
        var inTriple = false
        val blockHeaderRegex = Regex("""^\s*(def\s+\w+.*|class\s+\w+.*|if\s+.*|elif\s+.*|else|for\s+.*|while\s+.*|try|except(\s+.*)?|finally|with\s+.*)$""")

        lines.forEachIndexed { idx, rawLine ->
            val lineNum = idx + 1
            val trimmed = rawLine.trim()
            if (trimmed.contains("\"\"\"") || trimmed.contains("'''")) {
                val count = Regex("""("{3}|'{3})""").findAll(trimmed).count()
                if (count % 2 == 1) inTriple = !inTriple
            }
            if (inTriple || trimmed.isEmpty() || trimmed.startsWith("#")) return@forEachIndexed

            // Check mixed tabs and spaces in leading indentation
            val leading = rawLine.takeWhile { it == ' ' || it == '\t' }
            if (leading.contains(' ') && leading.contains('\t')) {
                out.add(
                    CodeDiagnostic(
                        line = lineNum,
                        column = 1,
                        severity = DiagnosticSeverity.ERROR,
                        message = "TabError: Inconsistent use of tabs and spaces in indentation",
                        rule = "python-indent"
                    )
                )
            }

            // Strip inline comment
            val codePart = rawLine.substringBefore('#').trimEnd()
            val prevDepth = openParenDepth
            openParenDepth += codePart.count { it == '(' || it == '[' || it == '{' } -
                codePart.count { it == ')' || it == ']' || it == '}' }
            openParenDepth = openParenDepth.coerceAtLeast(0)

            // Check missing colon on Python compound statements (only when parens are balanced on line)
            if (prevDepth == 0 && openParenDepth == 0 && blockHeaderRegex.matches(codePart)) {
                if (!codePart.endsWith(":") && !codePart.endsWith("\\")) {
                    out.add(
                        CodeDiagnostic(
                            line = lineNum,
                            column = codePart.length,
                            severity = DiagnosticSeverity.ERROR,
                            message = "SyntaxError: Expected ':' at end of Python block statement",
                            rule = "python-colon"
                        )
                    )
                }
            }

            // Check Python 2 print statement without parentheses
            if (Regex("""^\s*print\s+["'\w]""").containsMatchIn(codePart)) {
                out.add(
                    CodeDiagnostic(
                        line = lineNum,
                        column = rawLine.indexOf("print") + 1,
                        severity = DiagnosticSeverity.ERROR,
                        message = "SyntaxError: Missing parentheses in call to 'print(...)'",
                        rule = "python3-print"
                    )
                )
            }

            // Check accidental single '=' inside if/elif/while condition
            val condMatch = Regex("""^\s*(if|elif|while)\s+([^:=]+)=([^=].*):""").find(codePart)
            if (condMatch != null) {
                out.add(
                    CodeDiagnostic(
                        line = lineNum,
                        column = condMatch.range.first + 1,
                        severity = DiagnosticSeverity.ERROR,
                        message = "SyntaxError: Cannot assign '=' inside condition (use '==' for comparison)",
                        rule = "python-cond-assign"
                    )
                )
            }
        }
    }

    private fun checkJsonBugs(code: String, lines: List<String>, out: MutableList<CodeDiagnostic>) {
        val trimmed = code.trim()
        try {
            if (trimmed.startsWith("[")) {
                JSONArray(trimmed)
            } else {
                JSONObject(trimmed)
            }
        } catch (e: Exception) {
            val msg = e.message ?: "Invalid JSON syntax"
            // Check trailing commas to give exact line number
            lines.forEachIndexed { idx, line ->
                val clean = line.trim()
                if (clean.endsWith(",")) {
                    val nextNonBlank = lines.drop(idx + 1).firstOrNull { it.isNotBlank() }?.trim() ?: ""
                    if (nextNonBlank.startsWith("}") || nextNonBlank.startsWith("]")) {
                        out.add(
                            CodeDiagnostic(
                                line = idx + 1,
                                column = line.lastIndexOf(',') + 1,
                                severity = DiagnosticSeverity.ERROR,
                                message = "JSON Error: Trailing comma before closing bracket",
                                rule = "json-trailing-comma"
                            )
                        )
                    }
                }
            }
            if (out.none { it.rule == "json-trailing-comma" }) {
                out.add(
                    CodeDiagnostic(
                        line = 1,
                        column = 1,
                        severity = DiagnosticSeverity.ERROR,
                        message = "JSON Parse Error: $msg",
                        rule = "json-parse"
                    )
                )
            }
        }
    }

    private fun checkHtmlXmlBugs(lines: List<String>, out: MutableList<CodeDiagnostic>) {
        val voidTags = setOf(
            "area", "base", "br", "col", "embed", "hr", "img", "input",
            "link", "meta", "param", "source", "track", "wbr", "!doctype"
        )
        data class TagOpen(val name: String, val line: Int, val col: Int)
        val tagStack = ArrayDeque<TagOpen>()
        val tagRegex = Regex("""<(/?)([a-zA-Z][a-zA-Z0-9:-]*)([^>]*?)(/?)>""")
        var inScriptOrStyle = false

        lines.forEachIndexed { idx, line ->
            val lineNum = idx + 1
            for (match in tagRegex.findAll(line)) {
                val isClosing = match.groupValues[1] == "/"
                val tagName = match.groupValues[2].lowercase()
                val isSelfClosing = match.groupValues[4] == "/" || tagName in voidTags
                val col = match.range.first + 1

                if (tagName == "script" || tagName == "style") {
                    inScriptOrStyle = !isClosing && !isSelfClosing
                } else if (inScriptOrStyle) {
                    continue
                }

                if (!isClosing && !isSelfClosing) {
                    tagStack.addLast(TagOpen(tagName, lineNum, col))
                } else if (isClosing && tagName !in voidTags) {
                    val lastIdx = tagStack.indexOfLast { it.name == tagName }
                    if (lastIdx == -1) {
                        out.add(
                            CodeDiagnostic(
                                line = lineNum,
                                column = col,
                                severity = DiagnosticSeverity.WARNING,
                                message = "Unexpected closing tag </$tagName> with no matching opening tag",
                                rule = "html-tag"
                            )
                        )
                    } else {
                        while (tagStack.size > lastIdx) {
                            val popped = tagStack.removeLast()
                            if (popped.name != tagName) {
                                out.add(
                                    CodeDiagnostic(
                                        line = popped.line,
                                        column = popped.col,
                                        severity = DiagnosticSeverity.WARNING,
                                        message = "Unclosed <${popped.name}> before </$tagName> on line $lineNum",
                                        rule = "html-tag"
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun checkJsTsBugs(lines: List<String>, out: MutableList<CodeDiagnostic>) {
        lines.forEachIndexed { idx, line ->
            val lineNum = idx + 1
            val codePart = line.substringBefore("//")
            if (Regex("""\bconsol\.(log|warn|error)\b""").containsMatchIn(codePart)) {
                out.add(
                    CodeDiagnostic(
                        line = lineNum,
                        column = line.indexOf("consol.") + 1,
                        severity = DiagnosticSeverity.ERROR,
                        message = "ReferenceError: 'consol' is not defined (did you mean 'console'?)",
                        rule = "js-typo"
                    )
                )
            }
            if (Regex("""^\s*(if|while)\s*\([^=)]*=[^=].*\)""").containsMatchIn(codePart)) {
                out.add(
                    CodeDiagnostic(
                        line = lineNum,
                        column = 1,
                        severity = DiagnosticSeverity.WARNING,
                        message = "Suspicious assignment '=' inside condition (use '===' or '==')",
                        rule = "js-cond-assign"
                    )
                )
            }
        }
    }

    private fun checkCFamilyBugs(lines: List<String>, out: MutableList<CodeDiagnostic>) {
        lines.forEachIndexed { idx, line ->
            val lineNum = idx + 1
            val codePart = line.substringBefore("//")
            if (Regex("""^\s*(if|while)\s*\([^=)]*=[^=].*\)""").containsMatchIn(codePart)) {
                out.add(
                    CodeDiagnostic(
                        line = lineNum,
                        column = 1,
                        severity = DiagnosticSeverity.WARNING,
                        message = "Suspicious assignment '=' inside condition (use '==' for equality)",
                        rule = "c-cond-assign"
                    )
                )
            }
        }
    }

    // ── 2. Code Trails: Symbol Outline & Breadcrumb Extractor ────────────────
    fun extractSymbols(code: String, language: SupportedLanguage): List<CodeSymbol> {
        if (code.isBlank()) return emptyList()
        val symbols = mutableListOf<CodeSymbol>()
        val lines = code.split('\n')

        lines.forEachIndexed { idx, rawLine ->
            val lineNum = idx + 1
            val trimmed = rawLine.trim()
            if (trimmed.isEmpty()) return@forEachIndexed

            when (language) {
                SupportedLanguage.PYTHON -> {
                    Regex("""^class\s+([A-Za-z_]\w*)""").find(trimmed)?.let {
                        symbols.add(CodeSymbol(it.groupValues[1], "class", "C", lineNum))
                    }
                    Regex("""^(?:async\s+)?def\s+([A-Za-z_]\w*)\s*\(""").find(trimmed)?.let {
                        val isMethod = rawLine.startsWith(" ") || rawLine.startsWith("\t")
                        symbols.add(
                            CodeSymbol(
                                name = "${it.groupValues[1]}()",
                                kind = if (isMethod) "method" else "function",
                                badge = if (isMethod) "M" else "ƒ",
                                line = lineNum
                            )
                        )
                    }
                }

                SupportedLanguage.JAVASCRIPT, SupportedLanguage.TYPESCRIPT -> {
                    Regex("""^(?:export\s+)?(?:default\s+)?class\s+([A-Za-z_$][\w$]*)""").find(trimmed)?.let {
                        symbols.add(CodeSymbol(it.groupValues[1], "class", "C", lineNum))
                    }
                    Regex("""^(?:export\s+)?(?:interface|type)\s+([A-Za-z_$][\w$]*)""").find(trimmed)?.let {
                        symbols.add(CodeSymbol(it.groupValues[1], "interface", "I", lineNum))
                    }
                    Regex("""^(?:export\s+)?(?:async\s+)?function\s+([A-Za-z_$][\w$]*)\s*\(""").find(trimmed)?.let {
                        symbols.add(CodeSymbol("${it.groupValues[1]}()", "function", "ƒ", lineNum))
                    }
                    Regex("""^(?:export\s+)?(?:const|let|var)\s+([A-Za-z_$][\w$]*)\s*=\s*(?:async\s*)?\([^)]*\)\s*=>""").find(trimmed)?.let {
                        symbols.add(CodeSymbol("${it.groupValues[1]}()", "function", "ƒ", lineNum))
                    }
                }

                SupportedLanguage.KOTLIN, SupportedLanguage.JAVA, SupportedLanguage.CPP,
                SupportedLanguage.RUST, SupportedLanguage.GO, SupportedLanguage.DART -> {
                    Regex("""(?:class|interface|object|struct|enum)\s+([A-Za-z_]\w*)""").find(trimmed)?.let {
                        symbols.add(CodeSymbol(it.groupValues[1], "class", "C", lineNum))
                    }
                    Regex("""(?:fun|func|fn|void|int|String|boolean|double|float)\s+([A-Za-z_]\w*)\s*\(""").find(trimmed)?.let {
                        val name = it.groupValues[1]
                        if (name !in listOf("if", "for", "while", "switch", "when", "catch")) {
                            symbols.add(CodeSymbol("$name()", "function", "ƒ", lineNum))
                        }
                    }
                }

                SupportedLanguage.HTML, SupportedLanguage.XML -> {
                    Regex("""<(header|nav|main|section|article|footer|form|table|div\s+id=["'][^"']+["']|h[1-3])[^>]*>""", RegexOption.IGNORE_CASE)
                        .find(trimmed)?.let {
                            val rawTag = it.value.take(36)
                            symbols.add(CodeSymbol(rawTag, "tag", "<>", lineNum))
                        }
                }

                SupportedLanguage.CSS -> {
                    if (trimmed.endsWith("{") && !trimmed.startsWith("/*")) {
                        val selector = trimmed.removeSuffix("{").trim()
                        if (selector.isNotEmpty()) {
                            symbols.add(CodeSymbol(selector, "rule", "#", lineNum))
                        }
                    }
                }

                SupportedLanguage.MARKDOWN -> {
                    Regex("""^(#{1,4})\s+(.+)$""").find(trimmed)?.let {
                        val level = it.groupValues[1].length
                        symbols.add(CodeSymbol(it.groupValues[2].trim(), "section", "H$level", lineNum))
                    }
                }

                SupportedLanguage.BATCH -> {
                    Regex("""^:([A-Za-z_]\w*)""").find(trimmed)?.let {
                        symbols.add(CodeSymbol(":${it.groupValues[1]}", "section", ":", lineNum))
                    }
                }

                else -> {}
            }
        }
        return symbols
    }

    fun findActiveSymbol(symbols: List<CodeSymbol>, currentLine: Int): CodeSymbol? {
        return symbols.lastOrNull { it.line <= currentLine }
    }

    // ── 3. Smart Code Formatter / Beautifier ─────────────────────────────────
    fun formatCode(code: String, language: SupportedLanguage, tabSize: Int): String {
        if (code.isBlank()) return code
        val indentUnit = " ".repeat(tabSize.coerceIn(2, 8))

        return try {
            when (language) {
                SupportedLanguage.JSON -> {
                    val trimmed = code.trim()
                    if (trimmed.startsWith("[")) {
                        JSONArray(trimmed).toString(tabSize) + "\n"
                    } else {
                        JSONObject(trimmed).toString(tabSize) + "\n"
                    }
                }

                SupportedLanguage.PYTHON, SupportedLanguage.YAML -> {
                    // Normalize tabs to spaces and strip trailing whitespace per line
                    code.split('\n').joinToString("\n") { line ->
                        line.replace("\t", indentUnit).trimEnd()
                    }.trimEnd() + "\n"
                }

                SupportedLanguage.HTML, SupportedLanguage.XML -> {
                    formatHtmlLike(code, indentUnit)
                }

                SupportedLanguage.JAVASCRIPT, SupportedLanguage.TYPESCRIPT,
                SupportedLanguage.CSS, SupportedLanguage.KOTLIN,
                SupportedLanguage.JAVA, SupportedLanguage.CPP,
                SupportedLanguage.RUST, SupportedLanguage.GO,
                SupportedLanguage.DART, SupportedLanguage.PHP -> {
                    formatBraceLanguage(code, indentUnit)
                }

                else -> {
                    code.split('\n').joinToString("\n") { it.trimEnd() }.trimEnd() + "\n"
                }
            }
        } catch (_: Exception) {
            code
        }
    }

    private fun formatBraceLanguage(code: String, indentUnit: String): String {
        val lines = code.split('\n')
        val out = StringBuilder()
        var depth = 0

        for (raw in lines) {
            val trimmed = raw.trim()
            if (trimmed.isEmpty()) {
                out.append('\n')
                continue
            }
            if (trimmed.startsWith("}") || trimmed.startsWith("]") || trimmed.startsWith(")")) {
                depth = (depth - 1).coerceAtLeast(0)
            }
            out.append(indentUnit.repeat(depth)).append(trimmed).append('\n')

            val codeOnly = trimmed.substringBefore("//")
            val opens = codeOnly.count { it == '{' || it == '[' }
            val closes = codeOnly.count { it == '}' || it == ']' }
            val net = opens - closes
            val startedWithClose = trimmed.startsWith("}") || trimmed.startsWith("]")
            depth = (depth + net + if (startedWithClose) 1 else 0).coerceAtLeast(0)
        }
        return out.toString().trimEnd() + "\n"
    }

    private fun formatHtmlLike(code: String, indentUnit: String): String {
        val voidTags = setOf("area", "base", "br", "col", "embed", "hr", "img", "input", "link", "meta", "param", "source", "track", "wbr", "!doctype")
        val lines = code.split('\n')
        val out = StringBuilder()
        var depth = 0

        for (raw in lines) {
            val trimmed = raw.trim()
            if (trimmed.isEmpty()) {
                out.append('\n')
                continue
            }
            val startsWithClose = trimmed.startsWith("</")
            if (startsWithClose) {
                depth = (depth - 1).coerceAtLeast(0)
            }
            out.append(indentUnit.repeat(depth)).append(trimmed).append('\n')

            val openMatch = Regex("""^<([a-zA-Z0-9!-]+)""").find(trimmed)
            val tagName = openMatch?.groupValues?.get(1)?.lowercase() ?: ""
            val hasInlineClose = trimmed.contains("</") && !startsWithClose
            val isSelfClose = trimmed.endsWith("/>") || tagName in voidTags || tagName.startsWith("!")
            if (openMatch != null && !startsWithClose && !hasInlineClose && !isSelfClose) {
                depth++
            }
        }
        return out.toString().trimEnd() + "\n"
    }

    // ── 4. Commercial Snippets Library ───────────────────────────────────────
    fun getSnippets(language: SupportedLanguage): List<CodeSnippet> {
        return when (language) {
            SupportedLanguage.PYTHON -> listOf(
                CodeSnippet("Main Entrypoint", "main", "if __name__ == '__main__' guard", "def main():\n    print(\"Running...\")\n\nif __name__ == \"__main__\":\n    main()\n"),
                CodeSnippet("Class Definition", "class", "Python class with __init__", "class MyClass:\n    def __init__(self, name: str):\n        self.name = name\n\n    def greet(self) -> str:\n        return f\"Hello, {self.name}\"\n"),
                CodeSnippet("Try / Except Block", "try", "Exception handling block", "try:\n    result = 10 / 2\n    print(result)\nexcept Exception as err:\n    print(f\"Error: {err}\")\n"),
                CodeSnippet("List Comprehension", "comp", "Filter and transform list", "squares = [x * x for x in range(1, 11) if x % 2 == 0]\nprint(squares)\n"),
                CodeSnippet("Interactive Input", "input", "Prompt user for input", "name = input(\"Enter your name: \")\nprint(f\"Welcome, {name}!\")\n"),
                CodeSnippet("Turtle Graphics Star", "turtle", "Draw a star using Skulpt Turtle", "import turtle\n\nt = turtle.Turtle()\nt.speed(6)\nfor i in range(5):\n    t.forward(120)\n    t.right(144)\n")
            )

            SupportedLanguage.JAVASCRIPT, SupportedLanguage.TYPESCRIPT -> listOf(
                CodeSnippet("Async Fetch JSON", "fetch", "Async/await HTTP JSON request", "async function fetchData(url) {\n    try {\n        const res = await fetch(url);\n        const data = await res.json();\n        console.log(\"Data:\", data);\n        return data;\n    } catch (err) {\n        console.error(\"Fetch error:\", err);\n    }\n}\n"),
                CodeSnippet("Arrow Function", "fn", "ES6 arrow function", "const compute = (items = []) => {\n    return items.map((item, idx) => ({ idx, item }));\n};\n"),
                CodeSnippet("Promise Timer", "sleep", "Awaitable delay helper", "const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms));\n"),
                CodeSnippet("Array Reduce / Filter", "arr", "Functional array pipeline", "const numbers = [1, 2, 3, 4, 5, 6];\nconst sumEven = numbers\n    .filter((n) => n % 2 === 0)\n    .reduce((acc, n) => acc + n, 0);\nconsole.log(\"Sum of evens:\", sumEven);\n")
            )

            SupportedLanguage.HTML -> listOf(
                CodeSnippet("HTML5 Responsive Page", "html5", "Complete modern HTML5 template", SupportedLanguage.starterTemplate("index.html")),
                CodeSnippet("Flexbox Card Grid", "grid", "Responsive card layout", "<div style=\"display:grid;grid-template-columns:repeat(auto-fit,minmax(220px,1fr));gap:16px;\">\n    <div class=\"card\">Card 1</div>\n    <div class=\"card\">Card 2</div>\n</div>\n"),
                CodeSnippet("Interactive Form", "form", "Styled input + button form", "<form onsubmit=\"event.preventDefault(); console.log('Submitted:', this.query.value);\">\n    <input name=\"query\" placeholder=\"Type something...\" style=\"padding:8px 12px;border-radius:6px;border:1px solid #475569;\" />\n    <button type=\"submit\">Submit</button>\n</form>\n")
            )

            SupportedLanguage.KOTLIN -> listOf(
                CodeSnippet("Data Class", "data", "Kotlin immutable data class", "data class User(\n    val id: String,\n    val name: String,\n    val isActive: Boolean = true\n)\n"),
                CodeSnippet("Coroutine Launch", "launch", "Coroutine scope launch", "viewModelScope.launch {\n    try {\n        // suspend work\n    } catch (e: Exception) {\n        e.printStackTrace()\n    }\n}\n")
            )

            else -> listOf(
                CodeSnippet("Header Banner", "banner", "File header comment", "// ==========================================\n// Created with CPhone IDE\n// ==========================================\n")
            )
        }
    }
}
