package com.cphone.editor.model

enum class SupportedLanguage(
    val displayName: String,
    val badge: String,
    val badgeColorHex: Long,
    val commentPrefix: String,
    val extensions: List<String>
) {
    PYTHON("Python", "PY", 0xFF3572A5, "#", listOf("py", "pyw", "pyi")),
    JAVASCRIPT("JavaScript", "JS", 0xFFF1E05A, "//", listOf("js", "jsx", "mjs", "cjs")),
    TYPESCRIPT("TypeScript", "TS", 0xFF3178C6, "//", listOf("ts", "tsx")),
    HTML("HTML", "HTML", 0xFFE34C26, "<!--", listOf("html", "htm", "xhtml")),
    CSS("CSS", "CSS", 0xFF563D7C, "/*", listOf("css", "scss", "sass", "less")),
    JSON("JSON", "{ }", 0xFFCBCB41, "//", listOf("json", "jsonc", "geojson")),
    MARKDOWN("Markdown", "MD", 0xFF083FA1, "<!--", listOf("md", "markdown", "mdx")),
    KOTLIN("Kotlin", "KT", 0xFFA97BFF, "//", listOf("kt", "kts")),
    JAVA("Java", "JV", 0xFFB07219, "//", listOf("java", "jav")),
    CPP("C / C++", "C+", 0xFFF34B7D, "//", listOf("c", "cpp", "h", "hpp", "cc", "cxx", "ino")),
    SHELL("Shell Script", "SH", 0xFF89E051, "#", listOf("sh", "bash", "zsh", "fish", "env")),
    BATCH("Batch / CMD", "BAT", 0xFF4EAA25, "REM ", listOf("bat", "cmd")),
    PHP("PHP", "PHP", 0xFF4F5D95, "//", listOf("php", "phtml")),
    XML("XML / SVG", "XML", 0xFF0060AC, "<!--", listOf("xml", "svg", "xaml", "plist", "manifest")),
    YAML("YAML", "YML", 0xFFCB171E, "#", listOf("yaml", "yml", "toml")),
    SQL("SQL", "SQL", 0xFFE38C00, "--", listOf("sql", "sqlite", "ddl")),
    RUST("Rust", "RS", 0xFFDEA584, "//", listOf("rs")),
    GO("Go", "GO", 0xFF00ADD8, "//", listOf("go")),
    DART("Dart", "DT", 0xFF00B4AB, "//", listOf("dart")),
    PLAINTEXT("Plain Text", "TXT", 0xFF7F8C8D, "#", listOf("txt", "log", "conf", "ini", "cfg", "csv"));

    val isRunnable: Boolean
        get() = this in listOf(
            HTML, PYTHON, JAVASCRIPT, TYPESCRIPT, CSS, JSON, MARKDOWN, XML, SQL, SHELL, BATCH
        )

    companion object {
        /**
         * Cleans stray quotes or accidental trailing characters after quotes (e.g. "DOCKS.HTM"L -> docks.html)
         */
        fun sanitizeFileName(raw: String): String {
            return raw.trim().replace("\"", "").replace("'", "").trim()
        }

        fun detect(fileName: String, code: String = ""): SupportedLanguage {
            val cleaned = sanitizeFileName(fileName).removeSuffix("●").trim()
            val ext = cleaned.substringAfterLast('.', "").lowercase().trim()
            if (ext.isNotEmpty() && ext != "txt") {
                entries.firstOrNull { it.extensions.contains(ext) }?.let { return it }
                val detected = when {
                    ext.startsWith("htm") -> HTML
                    ext.startsWith("py") -> PYTHON
                    ext.startsWith("js") && ext != "json" -> JAVASCRIPT
                    ext.startsWith("ts") -> TYPESCRIPT
                    ext.startsWith("css") || ext.startsWith("scss") -> CSS
                    ext.startsWith("json") -> JSON
                    ext.startsWith("md") || ext.startsWith("mark") -> MARKDOWN
                    ext.startsWith("kt") -> KOTLIN
                    ext.startsWith("jav") -> JAVA
                    ext.startsWith("cpp") || ext.startsWith("cxx") -> CPP
                    ext.startsWith("bat") || ext.startsWith("cmd") -> BATCH
                    ext.startsWith("sh") || ext.startsWith("bash") -> SHELL
                    ext.startsWith("yml") || ext.startsWith("yaml") -> YAML
                    ext.startsWith("xml") || ext.startsWith("svg") -> XML
                    ext.startsWith("sql") -> SQL
                    ext.startsWith("php") -> PHP
                    ext.startsWith("rs") -> RUST
                    ext.startsWith("go") -> GO
                    ext.startsWith("dart") -> DART
                    else -> null
                }
                if (detected != null) return detected
            }

            // Inspect code content for quick auto-detection if extension is absent or .txt
            if (code.isNotEmpty()) {
                val trimmed = code.trim()
                return when {
                    trimmed.startsWith("<!DOCTYPE", ignoreCase = true) ||
                    trimmed.startsWith("<html", ignoreCase = true) ||
                    trimmed.contains("<head", ignoreCase = true) ||
                    trimmed.contains("<body", ignoreCase = true) ||
                    trimmed.contains("<script", ignoreCase = true) ||
                    trimmed.contains("</", ignoreCase = true) -> HTML

                    trimmed.startsWith("<?xml", ignoreCase = true) ||
                    trimmed.startsWith("<svg", ignoreCase = true) -> XML

                    trimmed.startsWith("import ") || trimmed.startsWith("from ") ||
                    trimmed.contains("def ") || trimmed.contains("print(") ||
                    trimmed.contains("if __name__ ==") -> PYTHON

                    trimmed.contains("console.log") || trimmed.contains("function ") ||
                    trimmed.contains("const ") || trimmed.contains("let ") ||
                    trimmed.contains("var ") -> JAVASCRIPT

                    trimmed.startsWith("{") || trimmed.startsWith("[") -> {
                        if (trimmed.endsWith("}") || trimmed.endsWith("]")) JSON else PLAINTEXT
                    }

                    trimmed.startsWith("# ") || trimmed.startsWith("## ") || trimmed.startsWith("```") -> MARKDOWN

                    else -> PLAINTEXT
                }
            }

            return PLAINTEXT
        }

        fun isRunnable(fileName: String, code: String = ""): Boolean {
            return detect(fileName, code).isRunnable
        }

        fun starterTemplate(fileName: String): String {
            return when (detect(fileName)) {
                PYTHON -> """
                    # $fileName
                    def main():
                        name = "Developer"
                        print(f"Hello, {name}! Welcome to CPhone IDE.")
                        numbers = [x * x for x in range(1, 6)]
                        print("Squares:", numbers)

                    if __name__ == "__main__":
                        main()
                """.trimIndent() + "\n"

                JAVASCRIPT -> """
                    // $fileName
                    function greet(name = "Developer") {
                        console.log(`Hello, ${'$'}{name}! Running on CPhone JS Engine.`);
                        return { status: "ok", timestamp: new Date().toISOString() };
                    }

                    const result = greet();
                    console.log("Result:", JSON.stringify(result, null, 2));
                """.trimIndent() + "\n"

                TYPESCRIPT -> """
                    // $fileName
                    interface User {
                        id: number;
                        name: string;
                        role: "admin" | "developer";
                    }

                    function formatUser(user: User): string {
                        return `[#${'$'}{user.id}] ${'$'}{user.name} (${'$'}{user.role})`;
                    }

                    const dev: User = { id: 1, name: "CPhone Coder", role: "developer" };
                    console.log(formatUser(dev));
                """.trimIndent() + "\n"

                HTML -> """
                    <!DOCTYPE html>
                    <html lang="en">
                    <head>
                        <meta charset="UTF-8">
                        <meta name="viewport" content="width=device-width, initial-scale=1.0">
                        <title>$fileName</title>
                        <style>
                            body {
                                font-family: system-ui, -apple-system, sans-serif;
                                margin: 0;
                                padding: 24px;
                                background: #0f172a;
                                color: #f8fafc;
                            }
                            .card {
                                background: #1e293b;
                                border-radius: 12px;
                                padding: 20px;
                                border: 1px solid #334155;
                            }
                            button {
                                background: #3b82f6;
                                color: white;
                                border: none;
                                padding: 10px 18px;
                                border-radius: 8px;
                                font-weight: 600;
                                cursor: pointer;
                            }
                        </style>
                    </head>
                    <body>
                        <div class="card">
                            <h2>CPhone Live Preview</h2>
                            <p>Edit HTML, CSS, and JS with instant preview and console.</p>
                            <button onclick="console.log('Button clicked at ' + new Date().toLocaleTimeString())">
                                Test Console Log
                            </button>
                        </div>
                    </body>
                    </html>
                """.trimIndent() + "\n"

                CSS -> """
                    /* $fileName */
                    :root {
                        --primary: #3b82f6;
                        --bg: #0f172a;
                        --text: #f8fafc;
                    }

                    body {
                        background: var(--bg);
                        color: var(--text);
                        font-family: system-ui, sans-serif;
                        padding: 20px;
                    }

                    .btn {
                        background: var(--primary);
                        color: #ffffff;
                        padding: 10px 16px;
                        border-radius: 8px;
                    }
                """.trimIndent() + "\n"

                JSON -> """
                    {
                        "name": "cphone-project",
                        "version": "2.0.0",
                        "private": true,
                        "features": ["syntax-highlighting", "code-trails", "live-runner"]
                    }
                """.trimIndent() + "\n"

                MARKDOWN -> """
                    # $fileName

                    Welcome to **CPhone IDE**.

                    ## Features
                    - Live **Python 3**, **JavaScript/TypeScript**, and **HTML/CSS** execution
                    - Real-time **Bug & Syntax Diagnostics**
                    - **Code Trails** (Breadcrumbs, Symbol Outline & Cursor History)

                    ```python
                    print("Hello from Markdown!")
                    ```
                """.trimIndent() + "\n"

                KOTLIN -> """
                    package com.example

                    fun main() {
                        val items = listOf("Kotlin", "Compose", "Android")
                        items.forEachIndexed { idx, item ->
                            println("${'$'}{idx + 1}. ${'$'}item")
                        }
                    }
                """.trimIndent() + "\n"

                else -> ""
            }
        }
    }
}
