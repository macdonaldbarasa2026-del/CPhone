package com.cphone.editor.ui

import android.graphics.Color as AndroidColor
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebViewAssetLoader
import com.cphone.editor.model.EditorSettings
import com.cphone.editor.model.SupportedLanguage
import com.cphone.editor.tools.CodeDiagnostic
import org.json.JSONArray
import org.json.JSONObject

@Composable
fun CodeMirrorEditorView(
    documentId: String,
    textFieldValue: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    language: SupportedLanguage,
    settings: EditorSettings,
    diagnostics: List<CodeDiagnostic>,
    searchQuery: String,
    onFontSizeCalibrated: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentValueChange = rememberUpdatedState(onValueChange)
    val currentFontSizeChange = rememberUpdatedState(onFontSizeCalibrated)
    var editorWebView by remember { mutableStateOf<WebView?>(null) }
    var editorReady by remember { mutableStateOf(false) }
    val assetLoader = remember(context) {
        WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(context))
            .build()
    }
    val bridge = remember {
        CodeMirrorBridge(
            onReady = { editorReady = true },
            onEditorUpdate = { text, anchor, head ->
                currentValueChange.value(TextFieldValue(text, TextRange(anchor, head)))
            },
            onFontSizeChanged = { currentFontSizeChange.value(it) }
        )
    }

    SideEffect {
        val webView = editorWebView
        if (editorReady && webView != null) {
            val config = JSONObject().apply {
                put("language", language.name)
                put("theme", settings.editorTheme)
                put("dark", settings.isDarkMode)
                put("fontSize", settings.fontSizeSp)
                put("lineSpacing", settings.lineSpacingMultiplier)
                put("tabSize", settings.tabSize)
                put("showLineNumbers", settings.showLineNumbers)
                put("wordWrap", settings.wordWrap)
                put("bracketAutoClose", settings.bracketAutoClose)
                put("showActiveLineHighlight", settings.showActiveLineHighlight)
                put("pinchToZoom", settings.pinchToZoom)
                put("search", searchQuery)
                put("diagnostics", JSONArray().apply {
                    diagnostics.forEach { diagnostic ->
                        put(JSONObject().apply {
                            put("line", diagnostic.line)
                            put("column", diagnostic.column)
                            put("severity", diagnostic.severity.name)
                            put("message", diagnostic.message)
                        })
                    }
                })
            }
            val document = JSONObject.quote(documentId)
            val text = JSONObject.quote(textFieldValue.text)
            val anchor = textFieldValue.selection.start.coerceIn(0, textFieldValue.text.length)
            val head = textFieldValue.selection.end.coerceIn(0, textFieldValue.text.length)
            webView.evaluateJavascript(
                "window.cphoneConfigure($config);window.cphoneSetDocument($document,$text,$anchor,$head);",
                null
            )
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { viewContext ->
            WebView(viewContext).apply {
                editorWebView = this
                setBackgroundColor(AndroidColor.TRANSPARENT)
                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false
                isFocusable = true
                isFocusableInTouchMode = true
                this.settings.javaScriptEnabled = true
                this.settings.domStorageEnabled = false
                this.settings.allowFileAccess = false
                this.settings.allowContentAccess = false
                this.settings.allowFileAccessFromFileURLs = false
                this.settings.allowUniversalAccessFromFileURLs = false
                this.settings.javaScriptCanOpenWindowsAutomatically = false
                this.settings.setSupportMultipleWindows(false)
                addJavascriptInterface(bridge, "AndroidEditor")
                webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(
                        view: WebView?,
                        request: WebResourceRequest
                    ) = assetLoader.shouldInterceptRequest(request.url)

                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest
                    ): Boolean = request.url.scheme != "https" ||
                        request.url.host != "appassets.androidplatform.net" ||
                        !request.url.path.orEmpty().startsWith("/assets/editor/")
                }
                loadUrl("https://appassets.androidplatform.net/assets/editor/index.html")
            }
        },
        onRelease = { webView ->
            editorReady = false
            editorWebView = null
            webView.removeJavascriptInterface("AndroidEditor")
            webView.stopLoading()
            webView.destroy()
        }
    )
}

private class CodeMirrorBridge(
    private val onReady: () -> Unit,
    private val onEditorUpdate: (String, Int, Int) -> Unit,
    private val onFontSizeChanged: (Float) -> Unit
) {
    private val mainHandler = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun onEditorReady() {
        mainHandler.post(onReady)
    }

    @JavascriptInterface
    fun onEditorUpdate(text: String, anchor: Int, head: Int) {
        mainHandler.post { onEditorUpdate(text, anchor, head) }
    }

    @JavascriptInterface
    fun onFontSizeChanged(fontSize: Float) {
        mainHandler.post { onFontSizeChanged(fontSize) }
    }
}
