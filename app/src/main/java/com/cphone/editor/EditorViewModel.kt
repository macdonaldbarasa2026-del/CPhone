package com.cphone.editor

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cphone.editor.model.EditorSettings
import com.cphone.editor.model.EditorTab
import com.cphone.editor.model.RecentFile
import com.cphone.editor.model.SupportedLanguage
import com.cphone.editor.terminal.TerminalSession
import com.cphone.editor.terminal.TermuxBridge
import com.cphone.editor.tools.CodeAnalyzer
import com.cphone.editor.tools.CodeDiagnostic
import com.cphone.editor.tools.CodeSymbol
import com.cphone.editor.tools.CursorTrailEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID
import kotlin.math.abs

class EditorViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("cphone_prefs", Context.MODE_PRIVATE)

    // ── Settings (persisted) ─────────────────────────────────────────────────
    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<EditorSettings> = _settings.asStateFlow()

    private fun loadSettings(): EditorSettings {
        return EditorSettings(
            isDarkMode            = prefs.getBoolean("dark_mode", true),
            editorTheme           = prefs.getString("editor_theme", "one_dark") ?: "one_dark",
            fontSizeSp            = prefs.getFloat("font_size", 13.5f),
            lineSpacingMultiplier = prefs.getFloat("line_spacing", 1.48f),
            tabSize               = prefs.getInt("tab_size", 4),
            showLineNumbers       = prefs.getBoolean("show_line_numbers", true),
            wordWrap              = prefs.getBoolean("word_wrap", false),
            alternatingRowBands   = prefs.getBoolean("row_bands", false),
            showTabBar            = prefs.getBoolean("show_tab_bar", true),
            showCodeTrailBar      = prefs.getBoolean("show_code_trail", true),
            showActiveLineHighlight = prefs.getBoolean("active_line_highlight", true),
            showIndentGuides      = prefs.getBoolean("indent_guides", true),
            showMinimapIndicator  = prefs.getBoolean("minimap_indicator", true),
            compactBottomDock     = prefs.getBoolean("compact_bottom_dock", true),
            pinchToZoom           = prefs.getBoolean("pinch_to_zoom", true),
            autoSaveSession       = prefs.getBoolean("auto_save_session", true),
            bracketAutoClose      = prefs.getBoolean("bracket_auto_close", true),
            aiProvider            = prefs.getString("ai_provider", "gemini") ?: "gemini",
            aiApiKey              = prefs.getString("ai_api_key", "") ?: "",
            aiModel               = prefs.getString("ai_model", "") ?: "",
            aiCustomEndpoint      = prefs.getString("ai_endpoint", "") ?: ""
        )
    }

    private fun saveSettings(s: EditorSettings) {
        prefs.edit()
            .putBoolean("dark_mode", s.isDarkMode)
            .putString("editor_theme", s.editorTheme)
            .putFloat("font_size", s.fontSizeSp)
            .putFloat("line_spacing", s.lineSpacingMultiplier)
            .putInt("tab_size", s.tabSize)
            .putBoolean("show_line_numbers", s.showLineNumbers)
            .putBoolean("word_wrap", s.wordWrap)
            .putBoolean("row_bands", s.alternatingRowBands)
            .putBoolean("show_tab_bar", s.showTabBar)
            .putBoolean("show_code_trail", s.showCodeTrailBar)
            .putBoolean("active_line_highlight", s.showActiveLineHighlight)
            .putBoolean("indent_guides", s.showIndentGuides)
            .putBoolean("minimap_indicator", s.showMinimapIndicator)
            .putBoolean("compact_bottom_dock", s.compactBottomDock)
            .putBoolean("pinch_to_zoom", s.pinchToZoom)
            .putBoolean("auto_save_session", s.autoSaveSession)
            .putBoolean("bracket_auto_close", s.bracketAutoClose)
            .putString("ai_provider", s.aiProvider)
            .putString("ai_api_key", s.aiApiKey)
            .putString("ai_model", s.aiModel)
            .putString("ai_endpoint", s.aiCustomEndpoint)
            .apply()
    }

    fun updateSettings(new: EditorSettings) {
        _settings.value = new
        saveSettings(new)
    }

    fun toggleTheme() {
        val nextDark = !_settings.value.isDarkMode
        val nextTheme = if (!nextDark) "github_light" else if (_settings.value.editorTheme == "github_light") "one_dark" else _settings.value.editorTheme
        updateSettings(_settings.value.copy(isDarkMode = nextDark, editorTheme = nextTheme))
    }

    fun setEditorTheme(themeId: String) {
        val isDark = themeId != "github_light"
        updateSettings(_settings.value.copy(editorTheme = themeId, isDarkMode = isDark))
    }

    fun adjustFontSize(delta: Float) {
        val nextSize = (_settings.value.fontSizeSp + delta).coerceIn(9f, 26f)
        updateSettings(_settings.value.copy(fontSizeSp = nextSize))
    }

    // ── Zen / Fullscreen Code View Mode ──────────────────────────────────────
    private val _isZenMode = MutableStateFlow(false)
    val isZenMode: StateFlow<Boolean> = _isZenMode.asStateFlow()

    fun toggleZenMode() {
        _isZenMode.value = !_isZenMode.value
    }

    // ── Recent files (persisted) ─────────────────────────────────────────────
    private val _recentFiles = MutableStateFlow(loadRecentFiles())
    val recentFiles: StateFlow<List<RecentFile>> = _recentFiles.asStateFlow()

    private fun loadRecentFiles(): List<RecentFile> {
        val json = prefs.getString("recent_files", "[]") ?: "[]"
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                RecentFile(
                    name = obj.getString("name"),
                    path = obj.getString("path"),
                    uri  = Uri.parse(obj.getString("uri"))
                )
            }
        } catch (_: Exception) { emptyList() }
    }

    private fun saveRecentFiles(list: List<RecentFile>) {
        val arr = JSONArray()
        list.take(25).forEach { f ->
            arr.put(JSONObject().apply {
                put("name", f.name)
                put("path", f.path)
                put("uri", f.uri.toString())
            })
        }
        prefs.edit().putString("recent_files", arr.toString()).apply()
    }

    private fun addToRecent(name: String, path: String, uri: Uri) {
        val updated = (_recentFiles.value.filter { it.path != path } +
                RecentFile(name, path, uri)).takeLast(25).reversed()
        _recentFiles.value = updated
        saveRecentFiles(updated)
    }

    // ── Tabs & Session Persistence (Starts completely blank — no pre-coded code) ──
    private val emptyTab = EditorTab(
        id       = UUID.randomUUID().toString(),
        fileName = "untitled.txt",
        uri      = null,
        state    = TextFieldValue("", TextRange(0)),
        isModified = false,
        language = SupportedLanguage.PLAINTEXT
    )

    private val _tabs = MutableStateFlow(loadSessionTabs())
    val tabs: StateFlow<List<EditorTab>> = _tabs.asStateFlow()

    private val _activeTabId = MutableStateFlow(
        prefs.getString("active_tab_id", null)?.takeIf { id -> _tabs.value.any { it.id == id } }
            ?: _tabs.value.first().id
    )
    val activeTabId: StateFlow<String> = _activeTabId.asStateFlow()

    val currentTab: EditorTab
        get() = _tabs.value.firstOrNull { it.id == _activeTabId.value } ?: _tabs.value.first()

    private fun loadSessionTabs(): List<EditorTab> {
        val raw = prefs.getString("session_tabs_v3", null) ?: return listOf(emptyTab)
        return try {
            val arr = JSONArray(raw)
            if (arr.length() == 0) return listOf(emptyTab)
            val list = mutableListOf<EditorTab>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val name = obj.optString("fileName", "untitled.txt")
                val text = obj.optString("text", "")
                val cursor = obj.optInt("cursor", 0).coerceIn(0, text.length)
                val uriStr = obj.optString("uri", "")
                val langName = obj.optString("language", "")
                val lang = SupportedLanguage.entries.firstOrNull { it.name == langName }
                    ?: SupportedLanguage.detect(name)
                list.add(
                    EditorTab(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        fileName = name,
                        uri = if (uriStr.isNotEmpty()) Uri.parse(uriStr) else null,
                        state = TextFieldValue(text, TextRange(cursor)),
                        isModified = obj.optBoolean("isModified", false),
                        language = lang
                    )
                )
            }
            list.ifEmpty { listOf(emptyTab) }
        } catch (_: Exception) {
            listOf(emptyTab)
        }
    }

    private fun saveSessionTabs() {
        if (!_settings.value.autoSaveSession) return
        try {
            val arr = JSONArray()
            _tabs.value.take(15).forEach { tab ->
                // Only persist tabs under 200KB to keep SharedPreferences fast
                if (tab.state.text.length < 200_000) {
                    arr.put(JSONObject().apply {
                        put("id", tab.id)
                        put("fileName", tab.fileName)
                        put("text", tab.state.text)
                        put("cursor", tab.state.selection.start)
                        put("uri", tab.uri?.toString() ?: "")
                        put("isModified", tab.isModified)
                        put("language", tab.language.name)
                    })
                }
            }
            prefs.edit()
                .putString("session_tabs_v3", arr.toString())
                .putString("active_tab_id", _activeTabId.value)
                .apply()
        } catch (_: Exception) {}
    }

    // ── Code Trails, Live Diagnostics & Symbol Outline ───────────────────────
    private val _diagnostics = MutableStateFlow<List<CodeDiagnostic>>(emptyList())
    val diagnostics: StateFlow<List<CodeDiagnostic>> = _diagnostics.asStateFlow()

    private val _symbols = MutableStateFlow<List<CodeSymbol>>(emptyList())
    val symbols: StateFlow<List<CodeSymbol>> = _symbols.asStateFlow()

    private val _activeSymbol = MutableStateFlow<CodeSymbol?>(null)
    val activeSymbol: StateFlow<CodeSymbol?> = _activeSymbol.asStateFlow()

    private val _cursorLine = MutableStateFlow(1)
    val cursorLine: StateFlow<Int> = _cursorLine.asStateFlow()

    private val _cursorColumn = MutableStateFlow(1)
    val cursorColumn: StateFlow<Int> = _cursorColumn.asStateFlow()

    private val _cursorTrail = MutableStateFlow<List<CursorTrailEntry>>(emptyList())
    val cursorTrail: StateFlow<List<CursorTrailEntry>> = _cursorTrail.asStateFlow()

    private val _trailIndex = MutableStateFlow(-1)
    val trailIndex: StateFlow<Int> = _trailIndex.asStateFlow()

    private var isNavigatingTrail = false
    private var analysisJob: Job? = null

    init {
        refreshAnalysisForCurrentTab()
    }

    private fun refreshAnalysisForCurrentTab(recordTrailIfJump: Boolean = true) {
        val tab = currentTab
        val text = tab.state.text
        val pos = tab.state.selection.start.coerceIn(0, text.length)
        val shouldRecordTrail = recordTrailIfJump && !isNavigatingTrail
        analysisJob?.cancel()
        analysisJob = viewModelScope.launch {
            delay(80)
            val analysis = withContext(Dispatchers.Default) {
                val beforeCursor = text.substring(0, pos)
                val line = beforeCursor.count { it == '\n' } + 1
                val lastNl = beforeCursor.lastIndexOf('\n')
                val column = if (lastNl == -1) pos + 1 else pos - lastNl
                val symbols = CodeAnalyzer.extractSymbols(text, tab.language)
                val activeSymbol = CodeAnalyzer.findActiveSymbol(symbols, line)
                val diagnostics = CodeAnalyzer.analyzeDiagnostics(text, tab.language)
                CurrentDocumentAnalysis(line, column, symbols, activeSymbol, diagnostics)
            }

            val latestTab = currentTab
            if (latestTab.id != tab.id ||
                latestTab.state.text != text ||
                latestTab.state.selection.start != pos
            ) return@launch

            _cursorLine.value = analysis.line
            _cursorColumn.value = analysis.column
            _symbols.value = analysis.symbols
            _activeSymbol.value = analysis.activeSymbol
            _diagnostics.value = analysis.diagnostics

            if (shouldRecordTrail) {
                val currentList = _cursorTrail.value
                val lastEntry = currentList.lastOrNull()
                if (lastEntry == null || lastEntry.tabId != tab.id ||
                    abs(lastEntry.line - analysis.line) >= 4
                ) {
                    val entry = CursorTrailEntry(
                        tabId = tab.id,
                        fileName = tab.fileName,
                        line = analysis.line,
                        column = analysis.column,
                        symbolName = analysis.activeSymbol?.name ?: "Ln ${analysis.line}"
                    )
                    val trimmed = (
                        currentList.take((_trailIndex.value + 1).coerceAtLeast(0)) + entry
                    ).takeLast(40)
                    _cursorTrail.value = trimmed
                    _trailIndex.value = trimmed.lastIndex
                }
            }
        }
    }

    private data class CurrentDocumentAnalysis(
        val line: Int,
        val column: Int,
        val symbols: List<CodeSymbol>,
        val activeSymbol: CodeSymbol?,
        val diagnostics: List<CodeDiagnostic>
    )

    fun navigateTrailBack() {
        val idx = _trailIndex.value
        val trail = _cursorTrail.value
        if (idx > 0 && trail.isNotEmpty()) {
            val targetIdx = idx - 1
            _trailIndex.value = targetIdx
            jumpToTrailEntry(trail[targetIdx])
        }
    }

    fun navigateTrailForward() {
        val idx = _trailIndex.value
        val trail = _cursorTrail.value
        if (idx in 0 until trail.lastIndex) {
            val targetIdx = idx + 1
            _trailIndex.value = targetIdx
            jumpToTrailEntry(trail[targetIdx])
        }
    }

    private fun jumpToTrailEntry(entry: CursorTrailEntry) {
        isNavigatingTrail = true
        if (_tabs.value.any { it.id == entry.tabId }) {
            _activeTabId.value = entry.tabId
            goToLine(entry.line, entry.column)
        }
        isNavigatingTrail = false
    }

    // ── Undo / Redo ──────────────────────────────────────────────────────────
    private val undoStacks = mutableMapOf<String, ArrayDeque<TextFieldValue>>()
    private val redoStacks = mutableMapOf<String, ArrayDeque<TextFieldValue>>()

    private fun undoStack(id: String) = undoStacks.getOrPut(id) { ArrayDeque() }
    private fun redoStack(id: String) = redoStacks.getOrPut(id) { ArrayDeque() }

    fun onTextChanged(new: TextFieldValue) {
        val tab = currentTab
        val old = tab.state
        if (old.text != new.text) {
            val u = undoStack(tab.id)
            if (u.size > 300) u.removeFirst()
            u.addLast(old)
            redoStack(tab.id).clear()
            updateCurrentTab { it.copy(state = new, isModified = true) }
            if (_isSearchOpen.value && _searchQuery.value.isNotEmpty()) {
                updateSearchMatches(_searchQuery.value, new.text)
            }
        } else {
            updateCurrentTab { it.copy(state = new) }
        }
        refreshAnalysisForCurrentTab()
        saveSessionTabs()
    }

    fun undo() {
        val tab = currentTab
        val u = undoStack(tab.id)
        if (u.isNotEmpty()) {
            redoStack(tab.id).addLast(tab.state)
            updateCurrentTab { it.copy(state = u.removeLast(), isModified = true) }
            refreshAnalysisForCurrentTab()
            saveSessionTabs()
        }
    }

    fun redo() {
        val tab = currentTab
        val r = redoStack(tab.id)
        if (r.isNotEmpty()) {
            undoStack(tab.id).addLast(tab.state)
            updateCurrentTab { it.copy(state = r.removeLast(), isModified = true) }
            refreshAnalysisForCurrentTab()
            saveSessionTabs()
        }
    }

    fun selectAll() {
        updateCurrentTab { it.copy(state = it.state.copy(selection = TextRange(0, it.state.text.length))) }
    }

    // ── Cursor & Line Manipulation (Acode Power Features) ────────────────────
    fun goToLine(targetLine: Int, targetCol: Int = 1) {
        val tab = currentTab
        val text = tab.state.text
        val lines = text.split('\n')
        val clampedLine = targetLine.coerceIn(1, lines.size.coerceAtLeast(1))
        var offset = 0
        for (i in 0 until clampedLine - 1) {
            offset += lines[i].length + 1
        }
        val lineLen = lines.getOrNull(clampedLine - 1)?.length ?: 0
        val colOffset = (targetCol - 1).coerceIn(0, lineLen)
        val finalPos = (offset + colOffset).coerceIn(0, text.length)
        onTextChanged(tab.state.copy(selection = TextRange(finalPos)))
    }

    fun moveCursorToLineStart() {
        val tab = currentTab
        val text = tab.state.text
        val pos = tab.state.selection.start.coerceIn(0, text.length)
        val lineStart = text.lastIndexOf('\n', (pos - 1).coerceAtLeast(0)).let { if (it < 0) 0 else it + 1 }
        val firstNonSpace = (lineStart until text.length)
            .firstOrNull { text[it] != ' ' && text[it] != '\t' || text[it] == '\n' } ?: lineStart
        val target = if (pos == firstNonSpace) lineStart else firstNonSpace
        onTextChanged(tab.state.copy(selection = TextRange(target)))
    }

    fun moveCursorToLineEnd() {
        val tab = currentTab
        val text = tab.state.text
        val pos = tab.state.selection.start.coerceIn(0, text.length)
        val lineEnd = text.indexOf('\n', pos).let { if (it < 0) text.length else it }
        onTextChanged(tab.state.copy(selection = TextRange(lineEnd)))
    }

    fun moveCursorUp() {
        val tab = currentTab
        val text = tab.state.text
        val pos = tab.state.selection.start
        val lineStart = text.lastIndexOf('\n', (pos - 1).coerceAtLeast(0)).let { if (it < 0) 0 else it + 1 }
        val col = pos - lineStart
        if (lineStart > 0) {
            val prevLineEnd = lineStart - 1
            val prevLineStart = text.lastIndexOf('\n', (prevLineEnd - 1).coerceAtLeast(0)).let { if (it < 0) 0 else it + 1 }
            val prevLineLen = prevLineEnd - prevLineStart
            val newPos = prevLineStart + minOf(col, prevLineLen)
            onTextChanged(tab.state.copy(selection = TextRange(newPos.coerceIn(0, text.length))))
        }
    }

    fun moveCursorDown() {
        val tab = currentTab
        val text = tab.state.text
        val pos = tab.state.selection.start
        val lineStart = text.lastIndexOf('\n', (pos - 1).coerceAtLeast(0)).let { if (it < 0) 0 else it + 1 }
        val col = pos - lineStart
        val nextLineStart = text.indexOf('\n', pos)
        if (nextLineStart != -1) {
            val nextLineEnd = text.indexOf('\n', nextLineStart + 1).let { if (it < 0) text.length else it }
            val nextLineLen = nextLineEnd - (nextLineStart + 1)
            val newPos = (nextLineStart + 1) + minOf(col, nextLineLen)
            onTextChanged(tab.state.copy(selection = TextRange(newPos.coerceIn(0, text.length))))
        }
    }

    fun getSelectedOrCurrentLineText(): String {
        val tab = currentTab
        val text = tab.state.text
        if (text.isEmpty()) return ""
        val sel = tab.state.selection
        if (!sel.collapsed) {
            return text.substring(sel.min.coerceAtLeast(0), sel.max.coerceAtMost(text.length))
        }
        val cursor = sel.start.coerceIn(0, text.length)
        val lineStart = text.lastIndexOf('\n', (cursor - 1).coerceAtLeast(0)).let { if (it == -1) 0 else it + 1 }
        val lineEnd = text.indexOf('\n', cursor).let { if (it == -1) text.length else it }
        return text.substring(lineStart, lineEnd)
    }

    fun cutSelectedOrCurrentLineText(): String {
        val tab = currentTab
        val text = tab.state.text
        if (text.isEmpty()) return ""
        val sel = tab.state.selection
        if (!sel.collapsed) {
            val min = sel.min.coerceAtLeast(0)
            val max = sel.max.coerceAtMost(text.length)
            val cutText = text.substring(min, max)
            val updated = text.removeRange(min, max)
            onTextChanged(TextFieldValue(updated, TextRange(min)))
            return cutText
        }
        val cutLine = getSelectedOrCurrentLineText()
        deleteLine()
        return cutLine
    }

    fun toggleLineComment() {
        val tab = currentTab
        val text = tab.state.text
        val pos = tab.state.selection.start.coerceIn(0, text.length)
        val lineStart = text.lastIndexOf('\n', (pos - 1).coerceAtLeast(0)).let { if (it < 0) 0 else it + 1 }
        val lineEnd = text.indexOf('\n', pos).let { if (it < 0) text.length else it }
        val currentLineText = text.substring(lineStart, lineEnd)
        val prefix = tab.language.commentPrefix

        val updatedLine = when {
            prefix == "<!--" -> {
                val trimmed = currentLineText.trim()
                if (trimmed.startsWith("<!--") && trimmed.endsWith("-->")) {
                    currentLineText.replaceFirst("<!--", "").replaceLast("-->", "").trimEnd()
                } else {
                    "<!-- $currentLineText -->"
                }
            }
            prefix == "/*" -> {
                val trimmed = currentLineText.trim()
                if (trimmed.startsWith("/*") && trimmed.endsWith("*/")) {
                    currentLineText.replaceFirst("/*", "").replaceLast("*/", "").trimEnd()
                } else {
                    "/* $currentLineText */"
                }
            }
            else -> {
                val leadingSpaces = currentLineText.takeWhile { it == ' ' || it == '\t' }
                val content = currentLineText.substring(leadingSpaces.length)
                if (content.startsWith("$prefix ")) {
                    leadingSpaces + content.removePrefix("$prefix ")
                } else if (content.startsWith(prefix)) {
                    leadingSpaces + content.removePrefix(prefix)
                } else {
                    "$leadingSpaces$prefix $content"
                }
            }
        }

        val newText = text.substring(0, lineStart) + updatedLine + text.substring(lineEnd)
        val newCursor = (lineStart + updatedLine.length).coerceIn(0, newText.length)
        onTextChanged(TextFieldValue(newText, TextRange(newCursor)))
    }

    private fun String.replaceLast(oldValue: String, newValue: String): String {
        val lastIndex = lastIndexOf(oldValue)
        if (lastIndex == -1) return this
        return substring(0, lastIndex) + newValue + substring(lastIndex + oldValue.length)
    }

    fun duplicateLine() {
        val tab = currentTab
        val text = tab.state.text
        val pos = tab.state.selection.start.coerceIn(0, text.length)
        val lineStart = text.lastIndexOf('\n', (pos - 1).coerceAtLeast(0)).let { if (it < 0) 0 else it + 1 }
        val lineEnd = text.indexOf('\n', pos).let { if (it < 0) text.length else it }
        val lineContent = text.substring(lineStart, lineEnd)
        val newText = text.substring(0, lineEnd) + "\n" + lineContent + text.substring(lineEnd)
        val newCursor = (pos + lineContent.length + 1).coerceIn(0, newText.length)
        onTextChanged(TextFieldValue(newText, TextRange(newCursor)))
    }

    fun deleteLine() {
        val tab = currentTab
        val text = tab.state.text
        if (text.isEmpty()) return
        val pos = tab.state.selection.start.coerceIn(0, text.length)
        val lineStart = text.lastIndexOf('\n', (pos - 1).coerceAtLeast(0)).let { if (it < 0) 0 else it + 1 }
        val lineEnd = text.indexOf('\n', pos).let { if (it < 0) text.length else it + 1 }
        val actualStart = if (lineEnd == text.length && lineStart > 0) lineStart - 1 else lineStart
        val newText = text.removeRange(actualStart, lineEnd)
        onTextChanged(TextFieldValue(newText, TextRange(actualStart.coerceIn(0, newText.length))))
    }

    fun moveLineUp() {
        val tab = currentTab
        val lines = tab.state.text.split('\n').toMutableList()
        val lineIdx = (_cursorLine.value - 1).coerceIn(0, lines.lastIndex)
        if (lineIdx <= 0) return
        val temp = lines[lineIdx - 1]
        lines[lineIdx - 1] = lines[lineIdx]
        lines[lineIdx] = temp
        val newText = lines.joinToString("\n")
        onTextChanged(TextFieldValue(newText, tab.state.selection))
        goToLine(lineIdx, _cursorColumn.value)
    }

    fun moveLineDown() {
        val tab = currentTab
        val lines = tab.state.text.split('\n').toMutableList()
        val lineIdx = (_cursorLine.value - 1).coerceIn(0, lines.lastIndex)
        if (lineIdx >= lines.lastIndex) return
        val temp = lines[lineIdx + 1]
        lines[lineIdx + 1] = lines[lineIdx]
        lines[lineIdx] = temp
        val newText = lines.joinToString("\n")
        onTextChanged(TextFieldValue(newText, tab.state.selection))
        goToLine(lineIdx + 2, _cursorColumn.value)
    }

    fun formatCurrentCode() {
        val tab = currentTab
        val formatted = CodeAnalyzer.formatCode(tab.state.text, tab.language, _settings.value.tabSize)
        if (formatted != tab.state.text) {
            onTextChanged(TextFieldValue(formatted, TextRange(tab.state.selection.start.coerceIn(0, formatted.length))))
        }
    }

    fun sortLines(ascending: Boolean = true) {
        val tab = currentTab
        val sorted = tab.state.text.split('\n').let {
            if (ascending) it.sorted() else it.sortedDescending()
        }.joinToString("\n")
        onTextChanged(TextFieldValue(sorted, TextRange(0)))
    }

    fun trimTrailingWhitespace() {
        val tab = currentTab
        val cleaned = tab.state.text.split('\n').joinToString("\n") { it.trimEnd() }
        onTextChanged(TextFieldValue(cleaned, TextRange(tab.state.selection.start.coerceIn(0, cleaned.length))))
    }

    fun changeLanguage(language: SupportedLanguage) {
        updateCurrentTab { it.copy(language = language) }
        refreshAnalysisForCurrentTab(recordTrailIfJump = false)
        saveSessionTabs()
    }

    fun renameCurrentTab(newName: String) {
        val cleaned = SupportedLanguage.sanitizeFileName(newName)
        if (cleaned.isEmpty()) return
        val newLang = SupportedLanguage.detect(cleaned)
        updateCurrentTab { it.copy(fileName = cleaned, language = newLang, isModified = true) }
        refreshAnalysisForCurrentTab(recordTrailIfJump = false)
        saveSessionTabs()
    }

    // ── Tab management ───────────────────────────────────────────────────────
    fun openNewTab(name: String, content: String, uri: Uri? = null, useStarterIfBlank: Boolean = false) {
        val cleanedName = SupportedLanguage.sanitizeFileName(name).ifEmpty { "untitled.py" }
        val finalContent = if (content.isEmpty() && useStarterIfBlank) {
            SupportedLanguage.starterTemplate(cleanedName)
        } else {
            content
        }
        val current = _tabs.value
        if (current.size == 1 && current[0].fileName == "untitled.txt" &&
            current[0].state.text.isEmpty() && !current[0].isModified) {
            val tab = EditorTab(
                id       = current[0].id,
                fileName = cleanedName,
                uri      = uri,
                state    = TextFieldValue(finalContent, TextRange(0)),
                isModified = false,
                language = SupportedLanguage.detect(cleanedName)
            )
            _tabs.value = listOf(tab)
            _activeTabId.value = tab.id
        } else {
            val existing = current.firstOrNull { it.uri != null && it.uri == uri }
            if (existing != null) {
                _activeTabId.value = existing.id
                refreshAnalysisForCurrentTab()
                return
            }
            val tab = EditorTab(
                id       = UUID.randomUUID().toString(),
                fileName = cleanedName,
                uri      = uri,
                state    = TextFieldValue(finalContent, TextRange(0)),
                isModified = false,
                language = SupportedLanguage.detect(cleanedName)
            )
            _tabs.update { it + tab }
            _activeTabId.value = tab.id
        }
        if (uri != null) addToRecent(cleanedName, uri.path ?: cleanedName, uri)
        refreshAnalysisForCurrentTab()
        saveSessionTabs()
    }

    fun openFile(name: String, content: String, uri: Uri? = null) = openNewTab(name, content, uri)

    fun selectTab(id: String) {
        _activeTabId.value = id
        refreshAnalysisForCurrentTab()
        saveSessionTabs()
    }

    fun closeTab(id: String) {
        val list = _tabs.value
        if (list.size == 1) {
            val fresh = EditorTab(
                id = UUID.randomUUID().toString(),
                fileName = "untitled.txt",
                uri = null,
                state = TextFieldValue("", TextRange(0)),
                isModified = false,
                language = SupportedLanguage.PLAINTEXT
            )
            _tabs.value = listOf(fresh)
            _activeTabId.value = fresh.id
            refreshAnalysisForCurrentTab()
            saveSessionTabs()
            return
        }
        val idx = list.indexOfFirst { it.id == id }
        val newList = list.filter { it.id != id }
        _tabs.value = newList
        if (_activeTabId.value == id) {
            _activeTabId.value = newList.getOrElse(idx.coerceAtMost(newList.lastIndex)) { newList.last() }.id
        }
        undoStacks.remove(id)
        redoStacks.remove(id)
        refreshAnalysisForCurrentTab()
        saveSessionTabs()
    }

    fun markSaved(uri: Uri? = null, name: String? = null) {
        updateCurrentTab { tab ->
            val newUri = uri ?: tab.uri
            val newName = name ?: tab.fileName
            if (newUri != null) addToRecent(newName, newUri.path ?: newName, newUri)
            tab.copy(isModified = false, uri = newUri, fileName = newName,
                language = if (name != null) SupportedLanguage.detect(name) else tab.language)
        }
        refreshAnalysisForCurrentTab(recordTrailIfJump = false)
        saveSessionTabs()
    }

    fun insertTextAtCursor(text: String) {
        val tab = currentTab
        val sel = tab.state.selection
        val cur = tab.state.text
        val newText = if (sel.collapsed)
            cur.substring(0, sel.start) + text + cur.substring(sel.start)
        else
            cur.replaceRange(sel.min, sel.max, text)
        val newCursor = sel.min + text.length
        onTextChanged(TextFieldValue(newText, TextRange(newCursor)))
    }

    fun replaceEntireFile(content: String) {
        onTextChanged(TextFieldValue(content, TextRange(0)))
    }

    private fun updateCurrentTab(transform: (EditorTab) -> EditorTab) {
        val id = _activeTabId.value
        _tabs.update { list -> list.map { if (it.id == id) transform(it) else it } }
    }

    // ── Search & Replace ─────────────────────────────────────────────────────
    private val _isSearchOpen    = MutableStateFlow(false)
    private val _isReplaceMode   = MutableStateFlow(false)
    private val _searchQuery     = MutableStateFlow("")
    private val _replaceQuery    = MutableStateFlow("")
    private val _searchMatches   = MutableStateFlow<List<IntRange>>(emptyList())
    private val _currentMatchIdx = MutableStateFlow(0)

    val isSearchOpen:      StateFlow<Boolean>        = _isSearchOpen.asStateFlow()
    val isReplaceMode:     StateFlow<Boolean>        = _isReplaceMode.asStateFlow()
    val searchQuery:       StateFlow<String>         = _searchQuery.asStateFlow()
    val replaceQuery:      StateFlow<String>         = _replaceQuery.asStateFlow()
    val searchMatches:     StateFlow<List<IntRange>> = _searchMatches.asStateFlow()
    val currentMatchIndex: StateFlow<Int>            = _currentMatchIdx.asStateFlow()

    fun openSearch(replaceMode: Boolean = false) { _isReplaceMode.value = replaceMode; _isSearchOpen.value = true }
    fun closeSearch() { _isSearchOpen.value = false; _searchMatches.value = emptyList(); _currentMatchIdx.value = 0 }

    fun onSearchQueryChanged(q: String) { _searchQuery.value = q; updateSearchMatches(q, currentTab.state.text) }
    fun onReplaceQueryChanged(q: String) { _replaceQuery.value = q }

    private fun updateSearchMatches(q: String, text: String) {
        if (q.isEmpty()) { _searchMatches.value = emptyList(); _currentMatchIdx.value = 0; return }
        val matches = mutableListOf<IntRange>()
        var i = 0
        while (i < text.length) {
            val f = text.indexOf(q, i, ignoreCase = true)
            if (f == -1) break
            matches.add(f until f + q.length)
            i = f + q.length
        }
        _searchMatches.value = matches
        if (matches.isNotEmpty()) { _currentMatchIdx.value = 0; highlightMatch() }
    }

    fun findNext() {
        val m = _searchMatches.value; if (m.isEmpty()) return
        _currentMatchIdx.value = (_currentMatchIdx.value + 1) % m.size; highlightMatch()
    }
    fun findPrevious() {
        val m = _searchMatches.value; if (m.isEmpty()) return
        _currentMatchIdx.value = if (_currentMatchIdx.value - 1 < 0) m.lastIndex else _currentMatchIdx.value - 1
        highlightMatch()
    }
    private fun highlightMatch() {
        val r = _searchMatches.value.getOrNull(_currentMatchIdx.value) ?: return
        updateCurrentTab { it.copy(state = it.state.copy(selection = TextRange(r.first, r.last + 1))) }
        refreshAnalysisForCurrentTab()
    }
    fun replaceCurrent() {
        val matches = _searchMatches.value; val idx = _currentMatchIdx.value
        if (matches.isEmpty() || idx !in matches.indices) return
        val r = matches[idx]; val text = currentTab.state.text
        val newText = text.replaceRange(r.first, r.last + 1, _replaceQuery.value)
        onTextChanged(TextFieldValue(newText, TextRange(r.first + _replaceQuery.value.length)))
        updateSearchMatches(_searchQuery.value, newText)
    }
    fun replaceAll() {
        val q = _searchQuery.value; if (q.isEmpty()) return
        val newText = currentTab.state.text.replace(q, _replaceQuery.value, ignoreCase = true)
        onTextChanged(TextFieldValue(newText, TextRange(0)))
        updateSearchMatches(q, newText)
    }

    // ── Dialog & Sheet Toggles ───────────────────────────────────────────────
    private val _isInfoOpen           = MutableStateFlow(false)
    private val _isSettingsOpen       = MutableStateFlow(false)
    private val _isNewFileOpen        = MutableStateFlow(false)
    private val _isExplorerOpen       = MutableStateFlow(false)
    private val _isAiOpen             = MutableStateFlow(false)
    private val _isAboutOpen          = MutableStateFlow(false)
    private val _isRecentOpen         = MutableStateFlow(false)
    private val _isCodeRunnerOpen     = MutableStateFlow(false)
    private val _isRenameOpen         = MutableStateFlow(false)
    private val _isCommandPaletteOpen = MutableStateFlow(false)
    private val _isCodeTrailsOpen     = MutableStateFlow(false)
    private val _codeTrailsInitialTab = MutableStateFlow(0) // 0 = Bugs/Problems, 1 = Symbols, 2 = Trail History
    private val _isSnippetsOpen       = MutableStateFlow(false)
    private val _isGoToLineOpen       = MutableStateFlow(false)

    val isInfoDialogOpen:     StateFlow<Boolean> = _isInfoOpen.asStateFlow()
    val isSettingsDialogOpen: StateFlow<Boolean> = _isSettingsOpen.asStateFlow()
    val isNewFileDialogOpen:  StateFlow<Boolean> = _isNewFileOpen.asStateFlow()
    val isFileExplorerOpen:   StateFlow<Boolean> = _isExplorerOpen.asStateFlow()
    val isAiAssistantOpen:    StateFlow<Boolean> = _isAiOpen.asStateFlow()
    val isAboutDialogOpen:    StateFlow<Boolean> = _isAboutOpen.asStateFlow()
    val isRecentFilesOpen:    StateFlow<Boolean> = _isRecentOpen.asStateFlow()
    val isCodeRunnerOpen:     StateFlow<Boolean> = _isCodeRunnerOpen.asStateFlow()
    val isRenameDialogOpen:   StateFlow<Boolean> = _isRenameOpen.asStateFlow()
    val isCommandPaletteOpen: StateFlow<Boolean> = _isCommandPaletteOpen.asStateFlow()
    val isCodeTrailsOpen:     StateFlow<Boolean> = _isCodeTrailsOpen.asStateFlow()
    val codeTrailsInitialTab: StateFlow<Int>     = _codeTrailsInitialTab.asStateFlow()
    val isSnippetsOpen:       StateFlow<Boolean> = _isSnippetsOpen.asStateFlow()
    val isGoToLineOpen:       StateFlow<Boolean> = _isGoToLineOpen.asStateFlow()

    // ── Interactive Terminal Engine & Termux Bridge ──────────────────────────
    val terminalSession = TerminalSession(application)
    private val _isTerminalOpen = MutableStateFlow(false)
    val isTerminalOpen: StateFlow<Boolean> = _isTerminalOpen.asStateFlow()

    fun setTerminalOpen(v: Boolean) { _isTerminalOpen.value = v }

    fun runCurrentFileInTerminal(useTermuxApp: Boolean = false) {
        val tab = currentTab
        if (useTermuxApp) {
            val cmd = TermuxBridge.getCompileAndRunCommand(tab.fileName, tab.language)
            val file = File(terminalSession.workspaceDir, tab.fileName)
            try { file.writeText(tab.state.text, Charsets.UTF_8) } catch (_: Exception) {}
            TermuxBridge.runInTermux(
                context = getApplication(),
                command = "cd \"${terminalSession.workspaceDir.absolutePath}\" && $cmd",
                workDir = terminalSession.workspaceDir.absolutePath
            )
        } else {
            terminalSession.runEditorFile(tab.fileName, tab.state.text, tab.language)
            _isTerminalOpen.value = true
        }
    }

    override fun onCleared() {
        super.onCleared()
        terminalSession.stopShell()
    }

    fun setInfoDialogOpen(v: Boolean)       { _isInfoOpen.value = v }
    fun setSettingsDialogOpen(v: Boolean)   { _isSettingsOpen.value = v }
    fun setNewFileDialogOpen(v: Boolean)    { _isNewFileOpen.value = v }
    fun setFileExplorerOpen(v: Boolean)     { _isExplorerOpen.value = v }
    fun setAiAssistantOpen(v: Boolean)      { _isAiOpen.value = v }
    fun setAboutDialogOpen(v: Boolean)      { _isAboutOpen.value = v }
    fun setRecentFilesOpen(v: Boolean)      { _isRecentOpen.value = v }
    fun setCodeRunnerOpen(v: Boolean)       { _isCodeRunnerOpen.value = v }
    fun setRenameDialogOpen(v: Boolean)     { _isRenameOpen.value = v }
    fun setCommandPaletteOpen(v: Boolean)   { _isCommandPaletteOpen.value = v }
    fun setCodeTrailsOpen(v: Boolean, initialTab: Int = 0) {
        _codeTrailsInitialTab.value = initialTab
        _isCodeTrailsOpen.value = v
    }
    fun setSnippetsOpen(v: Boolean)         { _isSnippetsOpen.value = v }
    fun setGoToLineOpen(v: Boolean)         { _isGoToLineOpen.value = v }
}
