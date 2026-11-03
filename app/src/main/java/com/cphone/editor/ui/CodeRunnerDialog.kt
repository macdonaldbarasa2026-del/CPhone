package com.cphone.editor.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.cphone.editor.model.SupportedLanguage
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ConsoleLogItem(
    val type: String, // "log", "info", "warn", "error"
    val message: String,
    val timestamp: String = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date())
)

class AndroidRunnerInterface(
    private val onStdoutCallback: (String) -> Unit,
    private val onErrorCallback: (String, String) -> Unit,
    private val onFinishedCallback: (Int, String) -> Unit,
    private val onConsoleCallback: (String, String) -> Unit
) {
    @JavascriptInterface
    fun onStdout(text: String) {
        onStdoutCallback(text)
    }

    @JavascriptInterface
    fun onError(error: String, elapsed: String) {
        onErrorCallback(error, elapsed)
    }

    @JavascriptInterface
    fun onFinished(exitCode: Int, elapsed: String) {
        onFinishedCallback(exitCode, elapsed)
    }

    @JavascriptInterface
    fun onConsoleMessage(type: String, message: String) {
        onConsoleCallback(type, message)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CodeRunnerDialog(
    fileName: String,
    code: String,
    language: SupportedLanguage,
    isDarkMode: Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Preview/Output, 1: Console
    var runKey by remember { mutableIntStateOf(0) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    // Terminal / Console state
    var terminalOutput by remember { mutableStateOf("") }
    var isRunning by remember { mutableStateOf(false) }
    var executionStatus by remember { mutableStateOf("Ready") }
    val consoleLogs = remember { mutableStateListOf<ConsoleLogItem>() }

    val terminalScrollState = rememberScrollState()

    // Determine runner mode
    val isHtml = language == SupportedLanguage.HTML || fileName.endsWith(".html", true) || fileName.endsWith(".htm", true)
    val isPython = language == SupportedLanguage.PYTHON || fileName.endsWith(".py", true) || fileName.endsWith(".pyw", true)
    val isJs = language == SupportedLanguage.JAVASCRIPT || fileName.endsWith(".js", true) || fileName.endsWith(".mjs", true)
    val isMarkdown = language == SupportedLanguage.MARKDOWN || fileName.endsWith(".md", true) || fileName.endsWith(".markdown", true)

    val runnerTitle = when {
        isHtml -> "HTML Live Preview"
        isPython -> "Python Runner"
        isJs -> "JavaScript Runner"
        isMarkdown -> "Markdown Preview"
        else -> "Code Runner"
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
        } catch (e: Exception) {
            // Fallback: share text
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, htmlContent)
                type = "text/html"
            }
            context.startActivity(Intent.createChooser(sendIntent, "Share HTML"))
        }
    }

    fun executeRunner() {
        if (isPython) {
            terminalOutput = ">>> python3 $fileName\n"
            isRunning = true
            executionStatus = "Running..."
        } else if (isJs) {
            terminalOutput = ">>> node $fileName\n"
            isRunning = true
            executionStatus = "Running..."
        }
        runKey++
    }

    LaunchedEffect(Unit) {
        executeRunner()
    }

    val dialogBg = if (isDarkMode) Color(0xFF1E1E2E) else Color(0xFFF8F9FA)
    val topBarBg = if (isDarkMode) Color(0xFF181824) else Color(0xFFFFFFFF)
    val contentColor = if (isDarkMode) Color(0xFFE0E0E0) else Color(0xFF202124)

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
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = contentColor
                            )
                            Text(
                                text = runnerTitle,
                                fontSize = 12.sp,
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
                        // Re-run / Reload button
                        IconButton(onClick = {
                            if (isHtml || isMarkdown) {
                                consoleLogs.clear()
                                webViewRef?.reload()
                            } else {
                                executeRunner()
                            }
                        }) {
                            Icon(
                                imageVector = if (isPython || isJs) Icons.Default.PlayArrow else Icons.Default.Refresh,
                                contentDescription = "Run / Refresh",
                                tint = Color(0xFF4CAF50)
                            )
                        }

                        // For HTML / Markdown: Open in Browser
                        if (isHtml || isMarkdown) {
                            IconButton(onClick = { openInExternalBrowser(code) }) {
                                Icon(Icons.Default.OpenInBrowser, contentDescription = "Open in Browser", tint = contentColor)
                            }
                        }

                        // For Python / JS: Clear button
                        if (isPython || isJs) {
                            IconButton(onClick = {
                                terminalOutput = ""
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
                        contentColor = Color(0xFF007AFF)
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("Preview") }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Console")
                                    if (consoleLogs.isNotEmpty()) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Badge(containerColor = Color(0xFF007AFF)) {
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
                        // ── HTML Preview & Console ────────────────────────────
                        isHtml -> {
                            if (selectedTab == 0) {
                                HtmlPreviewView(
                                    htmlCode = code,
                                    runKey = runKey,
                                    onConsole = { type, msg ->
                                        consoleLogs.add(ConsoleLogItem(type, msg))
                                    },
                                    onWebViewReady = { webViewRef = it }
                                )
                            } else {
                                ConsoleLogList(
                                    logs = consoleLogs,
                                    isDarkMode = isDarkMode,
                                    onClear = { consoleLogs.clear() }
                                )
                            }
                        }

                        // ── Python Runner (Skulpt offline engine) ─────────────
                        isPython -> {
                            Column(modifier = Modifier.fillMaxSize()) {
                                // Execution status header
                                Surface(
                                    color = if (isDarkMode) Color(0xFF181824) else Color(0xFFEAEAEA),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Python 3 Engine (Skulpt local)",
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
                                            color = when {
                                                isRunning -> Color(0xFFFFA000)
                                                executionStatus.contains("Error", true) -> Color(0xFFE53935)
                                                else -> Color(0xFF4CAF50)
                                            }
                                        )
                                    }
                                }

                                // Terminal output box
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .background(Color(0xFF14141E))
                                        .verticalScroll(terminalScrollState)
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        text = terminalOutput.ifEmpty { "Waiting for output..." },
                                        color = Color(0xFFE0E0E0),
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 13.sp,
                                        lineHeight = 18.sp
                                    )
                                }

                                // Headless WebView that executes Python via Skulpt
                                HeadlessPythonRunner(
                                    code = code,
                                    runKey = runKey,
                                    onStdout = { text ->
                                        terminalOutput += text
                                    },
                                    onError = { err, elapsed ->
                                        terminalOutput += "\nTraceback / Error:\n$err\n[Process exited with error in ${elapsed}s]\n"
                                        isRunning = false
                                        executionStatus = "Error"
                                    },
                                    onFinished = { _, elapsed ->
                                        terminalOutput += "\n[Process completed in ${elapsed}s]\n"
                                        isRunning = false
                                        executionStatus = "Finished ($elapsed s)"
                                    }
                                )
                            }
                        }

                        // ── JavaScript Runner ────────────────────────────────
                        isJs -> {
                            Column(modifier = Modifier.fillMaxSize()) {
                                Surface(
                                    color = if (isDarkMode) Color(0xFF181824) else Color(0xFFEAEAEA),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "JavaScript V8 Engine",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = contentColor
                                        )
                                        Spacer(modifier = Modifier.weight(1f))
                                        Text(
                                            text = executionStatus,
                                            fontSize = 12.sp,
                                            color = Color(0xFF4CAF50)
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .background(Color(0xFF14141E))
                                        .verticalScroll(terminalScrollState)
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        text = terminalOutput.ifEmpty { "Running JavaScript..." },
                                        color = Color(0xFFE0E0E0),
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 13.sp,
                                        lineHeight = 18.sp
                                    )
                                }

                                HeadlessJsRunner(
                                    code = code,
                                    runKey = runKey,
                                    onStdout = { text -> terminalOutput += text },
                                    onError = { err, elapsed ->
                                        terminalOutput += "\nUncaught Exception: $err\n[Process exited in ${elapsed}s]\n"
                                        isRunning = false
                                        executionStatus = "Error"
                                    },
                                    onFinished = { _, elapsed ->
                                        terminalOutput += "\n[Process completed in ${elapsed}s]\n"
                                        isRunning = false
                                        executionStatus = "Finished ($elapsed s)"
                                    }
                                )
                            }
                        }

                        // ── Markdown Preview ──────────────────────────────────
                        isMarkdown -> {
                            MarkdownPreviewView(
                                markdown = code,
                                isDarkMode = isDarkMode,
                                runKey = runKey,
                                onWebViewReady = { webViewRef = it }
                            )
                        }

                        // ── Fallback ──────────────────────────────────────────
                        else -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Preview / Runner is not available for $fileName",
                                    color = contentColor
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── HTML Preview Component with Console Interceptor ──────────────────────────

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun HtmlPreviewView(
    htmlCode: String,
    runKey: Int,
    onConsole: (String, String) -> Unit,
    onWebViewReady: (WebView) -> Unit
) {
    val consoleInject = """
        <script>
        (function() {
            function send(type, args) {
                var str = Array.prototype.slice.call(args).map(function(a) {
                    try { return typeof a === 'object' ? JSON.stringify(a) : String(a); }
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
                send('error', ['Uncaught error: ' + msg + ' (line ' + line + ')']);
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
                    webChromeClient = WebChromeClient()
                    webViewClient = WebViewClient()
                    addJavascriptInterface(
                        AndroidRunnerInterface(
                            onStdoutCallback = {},
                            onErrorCallback = { _, _ -> },
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

// ── Headless Python Runner using Skulpt offline engine ────────────────────────

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun HeadlessPythonRunner(
    code: String,
    runKey: Int,
    onStdout: (String) -> Unit,
    onError: (String, String) -> Unit,
    onFinished: (Int, String) -> Unit
) {
    val encoded = Base64.encodeToString(code.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)

    val hostHtml = """
        <!DOCTYPE html>
        <html>
        <head>
            <script src="file:///android_asset/runner/skulpt.min.js"></script>
            <script src="file:///android_asset/runner/skulpt-stdlib.js"></script>
        </head>
        <body>
        <script>
            function outf(text) {
                if (window.AndroidRunner) window.AndroidRunner.onStdout(text);
            }
            function builtinRead(x) {
                if (Sk.builtinFiles === undefined || Sk.builtinFiles["files"][x] === undefined)
                    throw "File not found: '" + x + "'";
                return Sk.builtinFiles["files"][x];
            }
            function run() {
                var startTime = Date.now();
                Sk.pre = "output";
                Sk.configure({
                    output: outf,
                    read: builtinRead,
                    __future__: Sk.python3
                });
                try {
                    var prog = decodeURIComponent(escape(atob("$encoded")));
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
            modifier = Modifier.size(0.dp),
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.allowFileAccess = true
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

// ── Headless JavaScript Runner ───────────────────────────────────────────────

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun HeadlessJsRunner(
    code: String,
    runKey: Int,
    onStdout: (String) -> Unit,
    onError: (String, String) -> Unit,
    onFinished: (Int, String) -> Unit
) {
    val encoded = Base64.encodeToString(code.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)

    val hostHtml = """
        <!DOCTYPE html>
        <html>
        <body>
        <script>
            (function() {
                var startTime = Date.now();
                var logStr = "";
                var _log = function() {
                    var line = Array.prototype.slice.call(arguments).map(function(a) {
                        try { return typeof a === 'object' ? JSON.stringify(a) : String(a); }
                        catch(e) { return String(a); }
                    }).join(' ');
                    if (window.AndroidRunner) window.AndroidRunner.onStdout(line + "\n");
                };
                console.log = _log;
                console.info = _log;
                console.warn = _log;
                console.error = _log;

                try {
                    var script = decodeURIComponent(escape(atob("$encoded")));
                    var result = window.eval(script);
                    if (result !== undefined) {
                        if (window.AndroidRunner) window.AndroidRunner.onStdout("=> " + String(result) + "\n");
                    }
                    var elapsed = ((Date.now() - startTime) / 1000).toFixed(2);
                    if (window.AndroidRunner) window.AndroidRunner.onFinished(0, elapsed);
                } catch(e) {
                    var elapsed = ((Date.now() - startTime) / 1000).toFixed(2);
                    if (window.AndroidRunner) window.AndroidRunner.onError(e.toString(), elapsed);
                }
            })();
        </script>
        </body>
        </html>
    """.trimIndent()

    key(runKey) {
        AndroidView(
            modifier = Modifier.size(0.dp),
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
            var raw = decodeURIComponent(escape(atob("$encoded")));
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
    isDarkMode: Boolean,
    onClear: () -> Unit
) {
    if (logs.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No console output.\nUse console.log() in your scripts.",
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
