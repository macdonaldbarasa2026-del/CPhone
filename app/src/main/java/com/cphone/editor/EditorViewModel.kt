package com.cphone.editor

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.AndroidViewModel
import com.cphone.editor.model.EditorSettings
import com.cphone.editor.model.EditorTab
import com.cphone.editor.model.RecentFile
import com.cphone.editor.model.SupportedLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class EditorViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("cphone_prefs", Context.MODE_PRIVATE)

    // ── Settings (persisted) ─────────────────────────────────────────────────
    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<EditorSettings> = _settings.asStateFlow()

    private fun loadSettings(): EditorSettings {
        return EditorSettings(
            isDarkMode        = prefs.getBoolean("dark_mode", true),
            fontSizeSp        = prefs.getFloat("font_size", 14f),
            tabSize           = prefs.getInt("tab_size", 4),
            showLineNumbers   = prefs.getBoolean("show_line_numbers", true),
            wordWrap          = prefs.getBoolean("word_wrap", false),
            alternatingRowBands = prefs.getBoolean("row_bands", true),
            showTabBar        = prefs.getBoolean("show_tab_bar", true),
            bracketAutoClose  = prefs.getBoolean("bracket_auto_close", true),
            aiProvider        = prefs.getString("ai_provider", "gemini") ?: "gemini",
            aiApiKey          = prefs.getString("ai_api_key", "") ?: "",
            aiModel           = prefs.getString("ai_model", "") ?: "",
            aiCustomEndpoint  = prefs.getString("ai_endpoint", "") ?: ""
        )
    }

    private fun saveSettings(s: EditorSettings) {
        prefs.edit()
            .putBoolean("dark_mode", s.isDarkMode)
            .putFloat("font_size", s.fontSizeSp)
            .putInt("tab_size", s.tabSize)
            .putBoolean("show_line_numbers", s.showLineNumbers)
            .putBoolean("word_wrap", s.wordWrap)
            .putBoolean("row_bands", s.alternatingRowBands)
            .putBoolean("show_tab_bar", s.showTabBar)
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
        updateSettings(_settings.value.copy(isDarkMode = !_settings.value.isDarkMode))
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
        list.take(20).forEach { f ->
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
                RecentFile(name, path, uri)).takeLast(20).reversed()
        _recentFiles.value = updated
        saveRecentFiles(updated)
    }

    // ── Tabs ─────────────────────────────────────────────────────────────────
    private val emptyTab = EditorTab(
        id       = UUID.randomUUID().toString(),
        fileName = "untitled.txt",
        uri      = null,
        state    = TextFieldValue("", TextRange(0)),
        isModified = false,
        language = SupportedLanguage.PLAINTEXT
    )

    private val _tabs = MutableStateFlow(listOf(emptyTab))
    val tabs: StateFlow<List<EditorTab>> = _tabs.asStateFlow()

    private val _activeTabId = MutableStateFlow(emptyTab.id)
    val activeTabId: StateFlow<String> = _activeTabId.asStateFlow()

    val currentTab: EditorTab
        get() = _tabs.value.firstOrNull { it.id == _activeTabId.value } ?: _tabs.value.first()

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
    }

    fun undo() {
        val tab = currentTab
        val u = undoStack(tab.id)
        if (u.isNotEmpty()) {
            redoStack(tab.id).addLast(tab.state)
            updateCurrentTab { it.copy(state = u.removeLast(), isModified = true) }
        }
    }

    fun redo() {
        val tab = currentTab
        val r = redoStack(tab.id)
        if (r.isNotEmpty()) {
            undoStack(tab.id).addLast(tab.state)
            updateCurrentTab { it.copy(state = r.removeLast(), isModified = true) }
        }
    }

    fun selectAll() {
        val tab = currentTab
        updateCurrentTab { it.copy(state = it.state.copy(selection = TextRange(0, it.state.text.length))) }
    }

    // ── Tab management ───────────────────────────────────────────────────────
    fun openNewTab(name: String, content: String, uri: Uri? = null) {
        // If the only tab is the blank untitled, replace it
        val current = _tabs.value
        if (current.size == 1 && current[0].fileName == "untitled.txt" &&
            current[0].state.text.isEmpty() && !current[0].isModified) {
            val tab = EditorTab(
                id       = current[0].id,
                fileName = name,
                uri      = uri,
                state    = TextFieldValue(content, TextRange(0)),
                isModified = false,
                language = SupportedLanguage.detect(name)
            )
            _tabs.value = listOf(tab)
            _activeTabId.value = tab.id
        } else {
            // Check if already open
            val existing = current.firstOrNull { it.uri != null && it.uri == uri }
            if (existing != null) {
                _activeTabId.value = existing.id
                return
            }
            val tab = EditorTab(
                id       = UUID.randomUUID().toString(),
                fileName = name,
                uri      = uri,
                state    = TextFieldValue(content, TextRange(0)),
                isModified = false,
                language = SupportedLanguage.detect(name)
            )
            _tabs.update { it + tab }
            _activeTabId.value = tab.id
        }
        if (uri != null) addToRecent(name, uri.path ?: name, uri)
    }

    fun openFile(name: String, content: String, uri: Uri? = null) = openNewTab(name, content, uri)

    fun selectTab(id: String) { _activeTabId.value = id }

    fun closeTab(id: String) {
        val list = _tabs.value
        if (list.size == 1) {
            // Reset to blank instead of closing last tab
            _tabs.value = listOf(emptyTab.copy(id = UUID.randomUUID().toString()))
            _activeTabId.value = _tabs.value[0].id
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
    }

    fun markSaved(uri: Uri? = null, name: String? = null) {
        updateCurrentTab { tab ->
            val newUri = uri ?: tab.uri
            val newName = name ?: tab.fileName
            if (newUri != null) addToRecent(newName, newUri.path ?: newName, newUri)
            tab.copy(isModified = false, uri = newUri, fileName = newName,
                language = if (name != null) SupportedLanguage.detect(name) else tab.language)
        }
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

    val isSearchOpen:    StateFlow<Boolean>       = _isSearchOpen.asStateFlow()
    val isReplaceMode:   StateFlow<Boolean>       = _isReplaceMode.asStateFlow()
    val searchQuery:     StateFlow<String>        = _searchQuery.asStateFlow()
    val replaceQuery:    StateFlow<String>        = _replaceQuery.asStateFlow()
    val searchMatches:   StateFlow<List<IntRange>>= _searchMatches.asStateFlow()
    val currentMatchIndex: StateFlow<Int>         = _currentMatchIdx.asStateFlow()

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

    // ── Dialog toggles ───────────────────────────────────────────────────────
    private val _isInfoOpen     = MutableStateFlow(false)
    private val _isSettingsOpen = MutableStateFlow(false)
    private val _isNewFileOpen  = MutableStateFlow(false)
    private val _isExplorerOpen = MutableStateFlow(false)
    private val _isAiOpen       = MutableStateFlow(false)
    private val _isAboutOpen    = MutableStateFlow(false)
    private val _isRecentOpen   = MutableStateFlow(false)

    val isInfoDialogOpen:     StateFlow<Boolean> = _isInfoOpen.asStateFlow()
    val isSettingsDialogOpen: StateFlow<Boolean> = _isSettingsOpen.asStateFlow()
    val isNewFileDialogOpen:  StateFlow<Boolean> = _isNewFileOpen.asStateFlow()
    val isFileExplorerOpen:   StateFlow<Boolean> = _isExplorerOpen.asStateFlow()
    val isAiAssistantOpen:    StateFlow<Boolean> = _isAiOpen.asStateFlow()
    val isAboutDialogOpen:    StateFlow<Boolean> = _isAboutOpen.asStateFlow()
    val isRecentFilesOpen:    StateFlow<Boolean> = _isRecentOpen.asStateFlow()

    fun setInfoDialogOpen(v: Boolean)     { _isInfoOpen.value = v }
    fun setSettingsDialogOpen(v: Boolean) { _isSettingsOpen.value = v }
    fun setNewFileDialogOpen(v: Boolean)  { _isNewFileOpen.value = v }
    fun setFileExplorerOpen(v: Boolean)   { _isExplorerOpen.value = v }
    fun setAiAssistantOpen(v: Boolean)    { _isAiOpen.value = v }
    fun setAboutDialogOpen(v: Boolean)    { _isAboutOpen.value = v }
    fun setRecentFilesOpen(v: Boolean)    { _isRecentOpen.value = v }
}
