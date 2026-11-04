package com.cphone.editor.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.webkit.JsPromptResult
import android.webkit.JsResult
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.cphone.editor.model.EditorTab
import com.cphone.editor.model.SupportedLanguage
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ConsoleLogItem(
    val type: String, // "log", "info", "warn", "error"
    val message: String,
    val timestamp: String = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date())
)

/**
 * Thread-safe WebView JavascriptInterface that always marshals callbacks onto Android's Main UI Thread.
 * Fixes race conditions and state mutation crashes from WebView's background JavaBridge thread.
 */
class AndroidRunnerInterface(
    private val mainHandler: Handler = Handler(Looper.getMainLooper()),
    private val onStdoutCallback: (String) -> Unit,
    private val onErrorCallback: (String, String, Int) -> Unit,
    private val onFinishedCallback: (Int, String) -> Unit,
    private val onConsoleCallback: (String, String) -> Unit
) {
    @JavascriptInterface
    fun onStdout(text: String) {
        mainHandler.post { onStdoutCallback(text) }
    }

    @JavascriptInterface
    fun onError(error: String, elapsed: String) {
        val lineMatch = Regex("""line\s+(\d+)""", RegexOption.IGNORE_CASE).find(error)
            ?: Regex(""":(\d+):\d+""").find(error)
        val lineNum = lineMatch?.groupValues?.get(1)?.toIntOrNull() ?: -1
        mainHandler.post { onErrorCallback(error, elapsed, lineNum) }
    }

    @JavascriptInterface
    fun onFinished(exitCode: Int, elapsed: String) {
        mainHandler.post { onFinishedCallback(exitCode, elapsed) }
    }

    @JavascriptInterface
    fun onConsoleMessage(type: String, message: String) {
        mainHandler.post { onConsoleCallback(type, message) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CodeRunnerDialog(
    fileName: String,
    code: String,
    language: SupportedLanguage,
    openTabs: List<EditorTab> = emptyList(),
    isDarkMode: Boolean,
    onJumpToLine: (Int) -> Unit = {},
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Output/Preview, 1: Console/Stdin
    var runKey by remember { mutableIntStateOf(0) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    // Terminal / Console state
    var terminalOutput by remember { mutableStateOf("") }
    var isRunning by remember { mutableStateOf(false) }
    var executionStatus by remember { mutableStateOf("Ready") }
    var errorLineNumber by remember { mutableIntStateOf(-1) }
    val consoleLogs = remember { mutableStateListOf<ConsoleLogItem>() }

    // Interactive Stdin (for Python input()) & REPL expression evaluation
    var stdinText by remember { mutableStateOf("") }
    var showStdinBox by remember { mutableStateOf(code.contains("input(")) }
    var replCommand by remember { mutableStateOf("") }

    // Interactive JS prompt dialog state (intercepts window.prompt / Python input() live!)
    var activeJsPromptMessage by remember { mutableStateOf<String?>(null) }
    var activeJsPromptDefault by remember { mutableStateOf("") }
    var activeJsPromptResult by remember { mutableStateOf<JsPromptResult?>(null) }

    val terminalScrollState = rememberScrollState()

    val isHtml = language == SupportedLanguage.HTML || language == SupportedLanguage.XML ||
        fileName.endsWith(".html", true) || fileName.endsWith(".htm", true) || fileName.endsWith(".svg", true)
    val isPython = language == SupportedLanguage.PYTHON || fileName.endsWith(".py", true) || fileName.endsWith(".pyw", true)
    val isJsOrTs = language == SupportedLanguage.JAVASCRIPT || language == SupportedLanguage.TYPESCRIPT ||
        fileName.endsWith(".js", true) || fileName.endsWith(".mjs", true) ||
        fileName.endsWith(".ts", true) || fileName.endsWith(".tsx", true) || fileName.endsWith(".jsx", true)
    val isCss = language == SupportedLanguage.CSS || fileName.endsWith(".css", true)
    val isJson = language == SupportedLanguage.JSON || fileName.endsWith(".json", true)
    val isMarkdown = language == SupportedLanguage.MARKDOWN || fileName.endsWith(".md", true) || fileName.endsWith(".markdown", true)
    val usesTurtle = isPython && (code.contains("import turtle") || code.contains("from turtle"))

    val runnerTitle = when {
        isHtml -> "HTML + Multi-Tab Preview"
        isPython -> if (usesTurtle) "Python 3 + Turtle Canvas" else "Python 3 Engine (Skulpt)"
        isJsOrTs -> if (language == SupportedLanguage.TYPESCRIPT || fileName.endsWith(".ts", true)) "TypeScript + V8 Runner" else "JavaScript V8 Runner"
        isCss -> "CSS Live Sandbox"
        isJson -> "JSON Validator & Inspector"
        isMarkdown -> "Markdown Live Preview"
        else -> "${language.displayName} Inspector"
    }

    fun openInExternalBrowser(htmlContent: String) {
        try {
            val file = File(context.cacheDir, "preview_$fileName")
            file.writeText(htmlContent, Charsets.UTF_8)
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "text/html")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Open in Browser"))
        } catch (_: Exception) {
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, htmlContent)
                type = "text/html"
            }
            context.startActivity(Intent.createChooser(sendIntent, "Share HTML"))
        }
    }

    fun executeRunner() {
        errorLineNumber = -1
        if (isPython) {
            terminalOutput = ">>> python3 $fileName\n"
            isRunning = true
            executionStatus = "Running..."
        } else if (isJsOrTs) {
            val cmd = if (language == SupportedLanguage.TYPESCRIPT || fileName.endsWith(".ts", true)) "ts-node" else "node"
            terminalOutput = ">>> $cmd $fileName\n"
            isRunning = true
            executionStatus = "Running..."
        }
        runKey++
    }

    LaunchedEffect(Unit) {
        executeRunner()
    }

    val dialogBg = if (isDarkMode) Color(0xFF1A1B26) else Color(0xFFF8F9FA)
    val topBarBg = if (isDarkMode) Color(0xFF13141C) else Color(0xFFFFFFFF)
    val contentColor = if (isDarkMode) Color(0xFFE5E9F0) else Color(0xFF202124)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = dialogBg
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // ── Top Bar ──────────────────────────────────────────────────────
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = fileName,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = contentColor
                            )
                            Text(
                                text = runnerTitle,
                                fontSize = 11.sp,
                                color = Color(0xFF4CAF50)
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = contentColor)
                        }
                    },
                    actions = {
                        // Jump to Bug Line button if an error line was detected
                        if (errorLineNumber > 0) {
                            FilledTonalButton(
                                onClick = {
                                    onDismiss()
                                    onJumpToLine(errorLineNumber)
                                },
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = Color(0xFFE06C75).copy(alpha = 0.2f),
                                    contentColor = Color(0xFFE06C75)
                                ),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Text("Line $errorLineNumber Bug", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                        }

                        // Copy output button for terminal runners
                        if (isPython || isJsOrTs || isJson) {
                            IconButton(onClick = {
                                clipboard.setText(AnnotatedString(terminalOutput.ifEmpty { code }))
                            }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy Output", tint = contentColor)
                            }
                        }

                        // Stdin toggle for Python
                        if (isPython) {
                            IconButton(onClick = { showStdinBox = !showStdinBox }) {
                                Icon(
                                    imageVector = Icons.Default.Input,
                                    contentDescription = "Stdin Input",
                                    tint = if (showStdinBox) Color(0xFF61AFEF) else contentColor
                                )
                            }
                        }

                        // Re-run / Reload button
                        IconButton(onClick = {
                            consoleLogs.clear()
                            executeRunner()
                        }) {
                            Icon(
                                imageVector = if (isPython || isJsOrTs) Icons.Default.PlayArrow else Icons.Default.Refresh,
                                contentDescription = "Run / Refresh",
                                tint = Color(0xFF4CAF50)
                            )
                        }

                        if (isHtml || isMarkdown) {
                            IconButton(onClick = { openInExternalBrowser(code) }) {
                                Icon(Icons.Default.OpenInBrowser, contentDescription = "Open in Browser", tint = contentColor)
                            }
                        }

                        if (isPython || isJsOrTs) {
                            IconButton(onClick = {
                                terminalOutput = ""
                                errorLineNumber = -1
                                consoleLogs.clear()
                            }) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Clear Output", tint = contentColor)
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = topBarBg)
                )

                // ── Tabs for HTML (Preview / Console) ─────────────────────────
                if (isHtml) {
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = topBarBg,
                        contentColor = Color(0xFF61AFEF)
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("Live Preview", fontSize = 13.sp) }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("DevTools Console", fontSize = 13.sp)
                                    if (consoleLogs.isNotEmpty()) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Badge(containerColor = Color(0xFF61AFEF)) {
                                            Text(consoleLogs.size.toString())
                                        }
                                    }
                                }
                            }
                        )
                    }
                }

                // ── Main Content Area ─────────────────────────────────────────
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    when {
                        // ── 1. HTML + Multi-Tab Bundled Preview & Console ─────
                        isHtml -> {
                            val bundledHtml = remember(code, openTabs, runKey) {
                                bundleHtmlWithOpenTabs(code, openTabs)
                            }
                            if (selectedTab == 0) {
                                HtmlPreviewView(
                                    htmlCode = bundledHtml,
                                    runKey = runKey,
                                    onConsole = { type, msg ->
                                        consoleLogs.add(ConsoleLogItem(type, msg))
                                    },
                                    onPromptRequest = { msg, def, res ->
                                        activeJsPromptMessage = msg
                                        activeJsPromptDefault = def
                                        activeJsPromptResult = res
                                    },
                                    onWebViewReady = { webViewRef = it }
                                )
                            } else {
                                Column(modifier = Modifier.fillMaxSize()) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        ConsoleLogList(
                                            logs = consoleLogs,
                                            isDarkMode = isDarkMode
                                        )
                                    }
                                    // Live JS Console Expression Evaluator
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(topBarBg)
                                            .padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedTextField(
                                            value = replCommand,
                                            onValueChange = { replCommand = it },
                                            placeholder = { Text("Evaluate JS in page (e.g. document.title)...", fontSize = 12.sp) },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Button(
                                            onClick = {
                                                val expr = replCommand.trim()
                                                if (expr.isNotEmpty()) {
                                                    consoleLogs.add(ConsoleLogItem("info", "> $expr"))
                                                    webViewRef?.evaluateJavascript(
                                                        "(function(){ try { var r = eval(${JSONObject.quote(expr)}); console.log(r); } catch(e) { console.error(e.toString()); } })();",
                                                        null
                                                    )
                                                    replCommand = ""
                                                }
                                            }
                                        ) {
                                            Text("Eval")
                                        }
                                    }
                                }
                            }
                        }

                        // ── 2. Python 3 Runner (Skulpt + Stdin + Turtle) ──────
                        isPython -> {
                            Column(modifier = Modifier.fillMaxSize()) {
                                // Execution status header
                                Surface(
                                    color = if (isDarkMode) Color(0xFF16161E) else Color(0xFFEAEAEA),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Python 3.10 Compatible Runtime",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = contentColor
                                        )
                                        Spacer(modifier = Modifier.weight(1f))
                                        if (isRunning) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(14.dp),
                                                strokeWidth = 2.dp,
                                                color = Color(0xFF4CAF50)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                        }
                                        Text(
                                            text = executionStatus,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = when {
                                                isRunning -> Color(0xFFFFA000)
                                                executionStatus.contains("Error", true) -> Color(0xFFE06C75)
                                                else -> Color(0xFF4CAF50)
                                            }
                                        )
                                    }
                                }

                                // Optional Pre-Supplied Stdin Box for input() calls
                                if (showStdinBox) {
                                    Surface(
                                        color = if (isDarkMode) Color(0xFF1F2335) else Color(0xFFEEF2F6),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                            Text(
                                                text = "Stdin Input Queue (one line per input() call, or leave blank for interactive popup):",
                                                fontSize = 11.sp,
                                                color = contentColor.copy(alpha = 0.8f)
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            OutlinedTextField(
                                                value = stdinText,
                                                onValueChange = { stdinText = it },
                                                placeholder = { Text("Line 1 input\nLine 2 input...", fontSize = 12.sp) },
                                                maxLines = 3,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                }

                                // Terminal output box
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .background(Color(0xFF11121A))
                                        .verticalScroll(terminalScrollState)
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        text = terminalOutput.ifEmpty { "Waiting for Python output..." },
                                        color = Color(0xFFE5E9F0),
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 13.sp,
                                        lineHeight = 19.sp
                                    )
                                }

                                // Python Runner WebView (visible if Turtle graphics is used, 1px otherwise)
                                PythonWebViewRunner(
                                    code = code,
                                    stdinLines = stdinText,
                                    showTurtleCanvas = usesTurtle,
                                    runKey = runKey,
                                    onStdout = { text ->
                                        terminalOutput += text
                                    },
                                    onError = { err, elapsed, lineNum ->
                                        terminalOutput += "\nTraceback / Error:\n$err\n[Exited with error in ${elapsed}s]\n"
                                        isRunning = false
                                        executionStatus = if (lineNum > 0) "Error (Line $lineNum)" else "Error"
                                        errorLineNumber = lineNum
                                    },
                                    onFinished = { _, elapsed ->
                                        terminalOutput += "\n[Process completed in ${elapsed}s]\n"
                                        isRunning = false
                                        executionStatus = "Finished ($elapsed s)"
                                    },
                                    onPromptRequest = { msg, def, res ->
                                        activeJsPromptMessage = msg
                                        activeJsPromptDefault = def
                                        activeJsPromptResult = res
                                    }
                                )
                            }
                        }

                        // ── 3. JavaScript & TypeScript V8 Runner ──────────────
                        isJsOrTs -> {
                            Column(modifier = Modifier.fillMaxSize()) {
                                Surface(
                                    color = if (isDarkMode) Color(0xFF16161E) else Color(0xFFEAEAEA),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (language == SupportedLanguage.TYPESCRIPT || fileName.endsWith(".ts", true))
                                                "TypeScript + V8 Async Engine" else "JavaScript V8 Async Engine",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = contentColor
                                        )
                                        Spacer(modifier = Modifier.weight(1f))
                                        Text(
                                            text = executionStatus,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (executionStatus.contains("Error", true)) Color(0xFFE06C75) else Color(0xFF4CAF50)
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .background(Color(0xFF11121A))
                                        .verticalScroll(terminalScrollState)
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        text = terminalOutput.ifEmpty { "Running script..." },
                                        color = Color(0xFFE5E9F0),
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 13.sp,
                                        lineHeight = 19.sp
                                    )
                                }

                                HeadlessJsTsRunner(
                                    code = code,
                                    isTypeScript = language == SupportedLanguage.TYPESCRIPT ||
                                        fileName.endsWith(".ts", true) || fileName.endsWith(".tsx", true),
                                    runKey = runKey,
                                    onStdout = { text -> terminalOutput += text },
                                    onError = { err, elapsed, lineNum ->
                                        terminalOutput += "\nUncaught Exception: $err\n[Process exited in ${elapsed}s]\n"
                                        isRunning = false
                                        executionStatus = if (lineNum > 0) "Error (Line $lineNum)" else "Error"
                                        errorLineNumber = lineNum
                                    },
                                    onFinished = { _, elapsed ->
                                        terminalOutput += "\n[Process completed in ${elapsed}s]\n"
                                        isRunning = false
                                        executionStatus = "Finished ($elapsed s)"
                                    }
                                )
                            }
                        }

                        // ── 4. CSS Live Sandbox Preview ───────────────────────
                        isCss -> {
                            val cssSandboxHtml = remember(code, runKey) {
                                """
                                <!DOCTYPE html>
                                <html>
                                <head>
                                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                                    <style>$code</style>
                                </head>
                                <body>
                                    <div class="container card">
                                        <h1>CSS Live Sandbox Preview</h1>
                                        <p>Your stylesheet (<b>$fileName</b>) is applied live to these sample elements:</p>
                                        <button class="btn button primary">Sample Button</button>
                                        <hr style="margin:16px 0;opacity:0.2;"/>
                                        <div class="box">Box / Card Element (<code>.box</code>, <code>.card</code>)</div>
                                    </div>
                                </body>
                                </html>
                                """.trimIndent()
                            }
                            HtmlPreviewView(
                                htmlCode = cssSandboxHtml,
                                runKey = runKey,
                                onConsole = { _, _ -> },
                                onPromptRequest = { _, _, res -> res.cancel() },
                                onWebViewReady = { webViewRef = it }
                            )
                        }

                        // ── 5. JSON Validator & Pretty Inspector ──────────────
                        isJson -> {
                            val jsonReport = remember(code, runKey) {
                                try {
                                    val t = code.trim()
                                    if (t.startsWith("[")) {
                                        val arr = JSONArray(t)
                                        "✓ Valid JSON Array (${arr.length()} items)\n\n" + arr.toString(2)
                                    } else {
                                        val obj = JSONObject(t)
                                        "✓ Valid JSON Object (${obj.length()} top-level keys)\n\n" + obj.toString(2)
                                    }
                                } catch (e: Exception) {
                                    "✗ Invalid JSON Syntax:\n${e.message}\n\nRaw Source:\n$code"
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0xFF11121A))
                                    .verticalScroll(rememberScrollState())
                                    .padding(14.dp)
                            ) {
                                Text(
                                    text = jsonReport,
                                    color = if (jsonReport.startsWith("✓")) Color(0xFF98C379) else Color(0xFFE06C75),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 13.sp,
                                    lineHeight = 19.sp
                                )
                            }
                        }

                        // ── 6. Markdown Preview ───────────────────────────────
                        isMarkdown -> {
                            MarkdownPreviewView(
                                markdown = code,
                                isDarkMode = isDarkMode,
                                runKey = runKey,
                                onWebViewReady = { webViewRef = it }
                            )
                        }

                        // ── 7. Shell / Batch / SQL Static Execution Dry-Run ───
                        else -> {
                            val lines = remember(code) { code.split('\n') }
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0xFF11121A))
                                    .verticalScroll(rememberScrollState())
                                    .padding(14.dp)
                            ) {
                                Text(
                                    text = buildString {
                                        appendLine("=== ${language.displayName} Script Analysis ($fileName) ===")
                                        appendLine("Total Lines: ${lines.size} • Size: ${code.toByteArray().size} bytes")
                                        appendLine("------------------------------------------------")
                                        lines.forEachIndexed { idx, l ->
                                            val t = l.trim()
                                            if (t.isNotEmpty() && !t.startsWith("#") && !t.startsWith("--") && !t.startsWith("::")) {
                                                appendLine("[Step ${idx + 1}] $t")
                                            }
                                        }
                                    },
                                    color = Color(0xFFE5E9F0),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 13.sp,
                                    lineHeight = 19.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // ── Interactive Prompt Dialog for Python input() & JS prompt() ────────────
    if (activeJsPromptMessage != null && activeJsPromptResult != null) {
        var userInput by remember(activeJsPromptMessage) { mutableStateOf(activeJsPromptDefault) }
        AlertDialog(
            onDismissRequest = {
                activeJsPromptResult?.cancel()
                activeJsPromptMessage = null
                activeJsPromptResult = null
            },
            title = { Text("Program Input Required", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column {
                    Text(
                        text = activeJsPromptMessage ?: "Enter input:",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = userInput,
                        onValueChange = { userInput = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    activeJsPromptResult?.confirm(userInput)
                    activeJsPromptMessage = null
                    activeJsPromptResult = null
                }) {
                    Text("Submit")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    activeJsPromptResult?.cancel()
                    activeJsPromptMessage = null
                    activeJsPromptResult = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Resolves <link rel="stylesheet" href="..."> and <script src="..."> against open tabs
 * so multi-file HTML + CSS + JS projects work seamlessly inside CPhone!
 */
private fun bundleHtmlWithOpenTabs(html: String, openTabs: List<EditorTab>): String {
    var result = html
    openTabs.forEach { tab ->
        val name = Regex.escape(tab.fileName)
        if (tab.fileName.endsWith(".css", ignoreCase = true)) {
            val linkRegex = Regex("""<link[^>]*href=["'](?:\./)?$name["'][^>]*>""", RegexOption.IGNORE_CASE)
            if (linkRegex.containsMatchIn(result)) {
                result = linkRegex.replace(result, "<style>\n${tab.state.text}\n</style>")
            }
        } else if (tab.fileName.endsWith(".js", ignoreCase = true)) {
            val scriptRegex = Regex("""<script[^>]*src=["'](?:\./)?$name["'][^>]*>\s*</script>""", RegexOption.IGNORE_CASE)
            if (scriptRegex.containsMatchIn(result)) {
                result = scriptRegex.replace(result, "<script>\n${tab.state.text}\n</script>")
            }
        }
    }
    return result
}

// ── HTML Preview Component with Console & Prompt Interceptor ─────────────────

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun HtmlPreviewView(
    htmlCode: String,
    runKey: Int,
    onConsole: (String, String) -> Unit,
    onPromptRequest: (String, String, JsPromptResult) -> Unit,
    onWebViewReady: (WebView) -> Unit
) {
    val consoleInject = """
        <script>
        (function() {
            function send(type, args) {
                var str = Array.prototype.slice.call(args).map(function(a) {
                    try { return typeof a === 'object' ? JSON.stringify(a, null, 2) : String(a); }
                    catch(e) { return String(a); }
                }).join(' ');
                if (window.AndroidRunner) {
                    window.AndroidRunner.onConsoleMessage(type, str);
                }
            }
            var _log = console.log, _warn = console.warn, _error = console.error, _info = console.info;
            console.log = function() { send('log', arguments); _log && _log.apply(console, arguments); };
            console.warn = function() { send('warn', arguments); _warn && _warn.apply(console, arguments); };
            console.error = function() { send('error', arguments); _error && _error.apply(console, arguments); };
            console.info = function() { send('info', arguments); _info && _info.apply(console, arguments); };
            window.onerror = function(msg, url, line) {
                send('error', ['Uncaught Error: ' + msg + ' (line ' + line + ')']);
            };
        })();
        </script>
    """.trimIndent()

    val fullHtml = if (htmlCode.contains("<head>", ignoreCase = true)) {
        htmlCode.replaceFirst("<head>", "<head>\n$consoleInject", ignoreCase = true)
    } else {
        "$consoleInject\n$htmlCode"
    }

    key(runKey) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.allowFileAccess = true
                    settings.useWideViewPort = true
                    settings.loadWithOverviewMode = true
                    webChromeClient = object : WebChromeClient() {
                        override fun onJsPrompt(
                            view: WebView?,
                            url: String?,
                            message: String?,
                            defaultValue: String?,
                            result: JsPromptResult?
                        ): Boolean {
                            if (result != null) {
                                onPromptRequest(message ?: "Input:", defaultValue ?: "", result)
                                return true
                            }
                            return super.onJsPrompt(view, url, message, defaultValue, result)
                        }
                    }
                    webViewClient = WebViewClient()
                    addJavascriptInterface(
                        AndroidRunnerInterface(
                            onStdoutCallback = {},
                            onErrorCallback = { _, _, _ -> },
                            onFinishedCallback = { _, _ -> },
                            onConsoleCallback = onConsole
                        ),
                        "AndroidRunner"
                    )
                    loadDataWithBaseURL("file:///android_asset/", fullHtml, "text/html", "UTF-8", null)
                    onWebViewReady(this)
                }
            },
            update = { view ->
                onWebViewReady(view)
            }
        )
    }
}

// ── Python 3 Runner using Skulpt + Stdin Queue + Turtle Graphics ─────────────

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun PythonWebViewRunner(
    code: String,
    stdinLines: String,
    showTurtleCanvas: Boolean,
    runKey: Int,
    onStdout: (String) -> Unit,
    onError: (String, String, Int) -> Unit,
    onFinished: (Int, String) -> Unit,
    onPromptRequest: (String, String, JsPromptResult) -> Unit
) {
    val encodedCode = Base64.encodeToString(code.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
    val encodedStdin = Base64.encodeToString(stdinLines.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)

    val hostHtml = """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <script src="file:///android_asset/runner/skulpt.min.js"></script>
            <script src="file:///android_asset/runner/skulpt-stdlib.js"></script>
            <style>
                body { margin: 0; padding: 8px; background: #181825; color: #cdd6f4; display: flex; justify-content: center; }
                #turtle-canvas { background: #ffffff; border-radius: 8px; }
            </style>
        </head>
        <body>
        <div id="turtle-canvas"></div>
        <script>
            function decodeUtf8(b64) {
                var bin = atob(b64);
                var bytes = new Uint8Array(bin.length);
                for (var i = 0; i < bin.length; i++) {
                    bytes[i] = bin.charCodeAt(i);
                }
                return new TextDecoder("utf-8").decode(bytes);
            }

            var rawStdin = decodeUtf8("$encodedStdin");
            var stdinQueue = rawStdin.length > 0 ? rawStdin.split(/\r?\n/) : [];
            var stdinIndex = 0;

            function outf(text) {
                if (window.AndroidRunner) window.AndroidRunner.onStdout(text);
            }

            function builtinRead(x) {
                if (Sk.builtinFiles === undefined || Sk.builtinFiles["files"][x] === undefined)
                    throw "File not found: '" + x + "'";
                return Sk.builtinFiles["files"][x];
            }

            function inputFun(promptText) {
                if (promptText) outf(promptText);
                if (stdinIndex < stdinQueue.length) {
                    var val = stdinQueue[stdinIndex++];
                    outf(val + "\n");
                    return val;
                }
                var userVal = window.prompt(promptText || "Python input():", "");
                var finalVal = userVal !== null ? userVal : "";
                outf(finalVal + "\n");
                return finalVal;
            }

            function run() {
                var startTime = Date.now();
                Sk.pre = "output";
                Sk.execLimit = 25000;
                Sk.yieldLimit = 100;
                (Sk.TurtleGraphics || (Sk.TurtleGraphics = {})).target = 'turtle-canvas';
                Sk.TurtleGraphics.width = 340;
                Sk.TurtleGraphics.height = 240;

                Sk.configure({
                    output: outf,
                    read: builtinRead,
                    inputfun: inputFun,
                    inputfunTakesPrompt: true,
                    __future__: Sk.python3
                });

                try {
                    var prog = decodeUtf8("$encodedCode");
                    var myPromise = Sk.misceval.asyncToPromise(function() {
                        return Sk.importMainWithBody("<stdin>", false, prog, true);
                    });
                    myPromise.then(function(mod) {
                        var elapsed = ((Date.now() - startTime) / 1000).toFixed(2);
                        if (window.AndroidRunner) window.AndroidRunner.onFinished(0, elapsed);
                    }, function(err) {
                        var elapsed = ((Date.now() - startTime) / 1000).toFixed(2);
                        if (window.AndroidRunner) window.AndroidRunner.onError(err.toString(), elapsed);
                    });
                } catch(e) {
                    var elapsed = ((Date.now() - startTime) / 1000).toFixed(2);
                    if (window.AndroidRunner) window.AndroidRunner.onError(e.toString(), elapsed);
                }
            }
            window.onload = run;
        </script>
        </body>
        </html>
    """.trimIndent()

    key(runKey) {
        AndroidView(
            modifier = if (showTurtleCanvas) Modifier.fillMaxWidth().height(260.dp) else Modifier.size(1.dp),
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.allowFileAccess = true
                    webChromeClient = object : WebChromeClient() {
                        override fun onJsPrompt(
                            view: WebView?,
                            url: String?,
                            message: String?,
                            defaultValue: String?,
                            result: JsPromptResult?
                        ): Boolean {
                            if (result != null) {
                                onPromptRequest(message ?: "Python input():", defaultValue ?: "", result)
                                return true
                            }
                            return super.onJsPrompt(view, url, message, defaultValue, result)
                        }

                        override fun onJsAlert(
                            view: WebView?,
                            url: String?,
                            message: String?,
                            result: JsResult?
                        ): Boolean {
                            onStdout((message ?: "") + "\n")
                            result?.confirm()
                            return true
                        }
                    }
                    addJavascriptInterface(
                        AndroidRunnerInterface(
                            onStdoutCallback = onStdout,
                            onErrorCallback = onError,
                            onFinishedCallback = onFinished,
                            onConsoleCallback = { _, _ -> }
                        ),
                        "AndroidRunner"
                    )
                    loadDataWithBaseURL("file:///android_asset/runner/", hostHtml, "text/html", "UTF-8", null)
                }
            }
        )
    }
}

// ── Headless JavaScript & TypeScript V8 Runner ───────────────────────────────

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun HeadlessJsTsRunner(
    code: String,
    isTypeScript: Boolean,
    runKey: Int,
    onStdout: (String) -> Unit,
    onError: (String, String, Int) -> Unit,
    onFinished: (Int, String) -> Unit
) {
    val encoded = Base64.encodeToString(code.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)

    val hostHtml = """
        <!DOCTYPE html>
        <html>
        <body>
        <script>
            function decodeUtf8(b64) {
                var bin = atob(b64);
                var bytes = new Uint8Array(bin.length);
                for (var i = 0; i < bin.length; i++) {
                    bytes[i] = bin.charCodeAt(i);
                }
                return new TextDecoder("utf-8").decode(bytes);
            }

            function transpileTypeScript(ts) {
                return ts
                    .replace(/^\s*interface\s+\w+[\s\S]*?^\}/gm, '')
                    .replace(/^\s*type\s+\w+\s*=[^;]+;/gm, '')
                    .replace(/:\s*(?:string|number|boolean|any|void|unknown|never|Record<[^>]+>|Array<[^>]+>|\w+\[\]|"[^"]*"(?:\s*\|\s*"[^"]*")*)(?=\s*[=,)\];{])/g, '')
                    .replace(/\b(?:public|private|protected|readonly)\s+/g, '');
            }

            (async function() {
                var startTime = Date.now();
                var _log = function() {
                    var line = Array.prototype.slice.call(arguments).map(function(a) {
                        try { return typeof a === 'object' ? JSON.stringify(a, null, 2) : String(a); }
                        catch(e) { return String(a); }
                    }).join(' ');
                    if (window.AndroidRunner) window.AndroidRunner.onStdout(line + "\n");
                };
                console.log = _log;
                console.info = _log;
                console.warn = _log;
                console.error = _log;

                try {
                    var rawScript = decodeUtf8("$encoded");
                    var executable = $isTypeScript ? transpileTypeScript(rawScript) : rawScript;
                    var AsyncFunction = Object.getPrototypeOf(async function(){}).constructor;
                    var fn = new AsyncFunction(executable);
                    var result = await fn();
                    if (result !== undefined) {
                        if (window.AndroidRunner) window.AndroidRunner.onStdout("=> " + String(result) + "\n");
                    }
                    var elapsed = ((Date.now() - startTime) / 1000).toFixed(2);
                    if (window.AndroidRunner) window.AndroidRunner.onFinished(0, elapsed);
                } catch(e) {
                    var elapsed = ((Date.now() - startTime) / 1000).toFixed(2);
                    var errMsg = (e && e.stack) ? e.stack.toString() : String(e);
                    if (window.AndroidRunner) window.AndroidRunner.onError(errMsg, elapsed);
                }
            })();
        </script>
        </body>
        </html>
    """.trimIndent()

    key(runKey) {
        AndroidView(
            modifier = Modifier.size(1.dp),
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    addJavascriptInterface(
                        AndroidRunnerInterface(
                            onStdoutCallback = onStdout,
                            onErrorCallback = onError,
                            onFinishedCallback = onFinished,
                            onConsoleCallback = { _, _ -> }
                        ),
                        "AndroidRunner"
                    )
                    loadDataWithBaseURL("file:///android_asset/", hostHtml, "text/html", "UTF-8", null)
                }
            }
        )
    }
}

// ── Markdown Preview Component ───────────────────────────────────────────────

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun MarkdownPreviewView(
    markdown: String,
    isDarkMode: Boolean,
    runKey: Int,
    onWebViewReady: (WebView) -> Unit
) {
    val encoded = Base64.encodeToString(markdown.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
    val bodyBg = if (isDarkMode) "#1E1E2E" else "#FFFFFF"
    val textColor = if (isDarkMode) "#CDD6F4" else "#24292F"
    val codeBg = if (isDarkMode) "#181825" else "#F6F8FA"
    val borderColor = if (isDarkMode) "#313244" else "#D0D7DE"

    val hostHtml = """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <script src="file:///android_asset/runner/marked.min.js"></script>
            <style>
                body {
                    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
                    background-color: $bodyBg;
                    color: $textColor;
                    padding: 16px;
                    margin: 0;
                    line-height: 1.6;
                }
                pre {
                    background-color: $codeBg;
                    padding: 12px;
                    border-radius: 6px;
                    overflow-x: auto;
                    border: 1px solid $borderColor;
                }
                code {
                    font-family: monospace;
                    font-size: 85%;
                    background-color: $codeBg;
                    padding: 0.2em 0.4em;
                    border-radius: 4px;
                }
                pre code { background-color: transparent; padding: 0; }
                blockquote {
                    border-left: 4px solid #585b70;
                    margin: 0;
                    padding-left: 14px;
                    color: #a6adc8;
                }
                table { border-collapse: collapse; width: 100%; margin: 12px 0; }
                th, td { border: 1px solid $borderColor; padding: 6px 12px; }
                img { max-width: 100%; height: auto; }
                a { color: #89b4fa; }
                h1, h2, h3 { border-bottom: 1px solid $borderColor; padding-bottom: 0.3em; }
            </style>
        </head>
        <body>
        <div id="content"></div>
        <script>
            var bin = atob("$encoded");
            var bytes = new Uint8Array(bin.length);
            for (var i = 0; i < bin.length; i++) bytes[i] = bin.charCodeAt(i);
            var raw = new TextDecoder("utf-8").decode(bytes);
            document.getElementById('content').innerHTML = marked.parse(raw);
        </script>
        </body>
        </html>
    """.trimIndent()

    key(runKey) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.allowFileAccess = true
                    webChromeClient = WebChromeClient()
                    webViewClient = WebViewClient()
                    loadDataWithBaseURL("file:///android_asset/runner/", hostHtml, "text/html", "UTF-8", null)
                    onWebViewReady(this)
                }
            },
            update = { view -> onWebViewReady(view) }
        )
    }
}

// ── Console Log List ─────────────────────────────────────────────────────────

@Composable
private fun ConsoleLogList(
    logs: List<ConsoleLogItem>,
    isDarkMode: Boolean
) {
    if (logs.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No console output yet.\nUse console.log() or evaluate an expression below.",
                color = if (isDarkMode) Color(0xFF6C7086) else Color(0xFF8C9098),
                fontSize = 14.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            state = rememberLazyListState()
        ) {
            items(logs) { log ->
                val badgeColor = when (log.type.lowercase()) {
                    "error" -> Color(0xFFF38BA8)
                    "warn" -> Color(0xFFFAB387)
                    "info" -> Color(0xFF89B4FA)
                    else -> Color(0xFFA6ADC8)
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDarkMode) Color(0xFF181825) else Color(0xFFF0F2F5)
                    ),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "[${log.type.uppercase()}]",
                            color = badgeColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = log.message,
                            color = if (isDarkMode) Color(0xFFCDD6F4) else Color(0xFF1E1E2E),
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = log.timestamp,
                            color = if (isDarkMode) Color(0xFF585B70) else Color(0xFF9E9E9E),
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}
