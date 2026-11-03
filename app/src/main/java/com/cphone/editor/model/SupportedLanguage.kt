package com.cphone.editor.model

enum class SupportedLanguage(
    val displayName: String,
    val extensions: List<String>
) {
    BATCH("Batch / CMD", listOf("bat", "cmd")),
    PYTHON("Python", listOf("py", "pyw")),
    JAVA("Java", listOf("java")),
    KOTLIN("Kotlin", listOf("kt", "kts")),
    JAVASCRIPT("JavaScript", listOf("js", "jsx", "mjs")),
    TYPESCRIPT("TypeScript", listOf("ts", "tsx")),
    HTML("HTML", listOf("html", "htm")),
    CSS("CSS", listOf("css", "scss", "sass")),
    JSON("JSON", listOf("json")),
    XML("XML", listOf("xml", "svg", "xaml")),
    SHELL("Shell Script", listOf("sh", "bash", "zsh")),
    CPP("C / C++", listOf("c", "cpp", "h", "hpp", "cc", "cxx")),
    PHP("PHP", listOf("php", "phtml")),
    MARKDOWN("Markdown", listOf("md", "markdown")),
    YAML("YAML", listOf("yaml", "yml")),
    PLAINTEXT("Plain Text", listOf("txt", "log", "conf", "ini"));

    val isRunnable: Boolean
        get() = this in listOf(HTML, PYTHON, JAVASCRIPT, MARKDOWN)

    companion object {
        fun detect(fileName: String): SupportedLanguage {
            val ext = fileName.substringAfterLast('.', "").lowercase()
            return entries.firstOrNull { it.extensions.contains(ext) } ?: PLAINTEXT
        }

        fun isRunnable(fileName: String): Boolean {
            val ext = fileName.substringAfterLast('.', "").lowercase()
            return ext in listOf("html", "htm", "py", "pyw", "js", "mjs", "md", "markdown")
        }
    }
}
