package com.cphone.editor

import android.net.Uri
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import com.cphone.editor.model.EditorSettings
import com.cphone.editor.model.EditorTab
import com.cphone.editor.model.SupportedLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

class EditorViewModel : ViewModel() {

    companion object {
        val DEFAULT_CODE = """
@echo off
title NetFetch High-Speed Broadband Connection Setup
echo ========================================
echo    NetFetch High-Speed Broadband Connection Setup
echo ========================================
echo.
echo [1/4] Enabling Windows Network Services...
reg add "HKLM\SYSTEM\CurrentControlSet\Services\RasMan\Parameters" /v AllowL2TPWeakCrypto /t REG_DWORD /d 1 /f
reg add "HKCU\Software\Microsoft\Windows\CurrentVersion\Internet Settings" /v ProxyEnable /t REG_DWORD /d 0 /f
reg add "HKCU\Software\Microsoft\Windows\CurrentVersion\Internet Settings" /v AutoDetect /t REG_DWORD /d 0 /f

echo [2/4] Setting NetFetch High-Speed Hotspot Profile...
reg add "HKCU\Software\Microsoft\Windows\CurrentVersion\Internet Settings" /v MigrateProxy /t REG_DWORD /d 1 /f
reg add "HKCU\Software\Microsoft\Windows\CurrentVersion\Internet Settings" /v ProxyHttp1.1 /t REG_DWORD /d 1 /f
reg add "HKCU\Software\Microsoft\Windows\CurrentVersion\Internet Settings" /v ProxyOverride /t REG_SZ /d "<local>" /f

echo [3/4] Registering Windows Broadband VPN/Tether Profile...
set "PBK_DIR=%APPDATA%\Microsoft\Network\Connections\Pbk"
if not exist "%PBK_DIR%" mkdir "%PBK_DIR%"
set "PBK_FILE=%PBK_DIR%\rasphone.pbk"

(
echo [NetFetch Broadband Internet]
echo MEDIA=rastapi
echo Port=VPN2-0
echo Device=WAN Miniport (IKEv2)
echo DEVICE=vpn
echo PhoneNumber=192.168.49.1
echo Type=2
echo AutoLogon=1
echo UseRasCredentials=0
) >> "%PBK_FILE%" 2>nul

echo [4/4] Refreshing Windows Network Settings...
""".trimIndent()
    }

    private val undoStacks = mutableMapOf<String, MutableList<TextFieldValue>>()
    private val redoStacks = mutableMapOf<String, MutableList<TextFieldValue>>()

    private val initialTab = EditorTab(
        id = UUID.randomUUID().toString(),
        fileName = "netfetch-broadband-setup.bat",
        uri = null,
        state = TextFieldValue(DEFAULT_CODE, TextRange(0)),
        isModified = false,
        language = SupportedLanguage.BATCH
    )

    private val _tabs = MutableStateFlow(listOf(initialTab))
    val tabs: StateFlow<List<EditorTab>> = _tabs.asStateFlow()

    private val _activeTabId = MutableStateFlow(initialTab.id)
    val activeTabId: StateFlow<String> = _activeTabId.asStateFlow()

    private val _settings = MutableStateFlow(EditorSettings())
    val settings: StateFlow<EditorSettings> = _settings.asStateFlow()

    // Dialogs & Bars
    private val _isSearchOpen = MutableStateFlow(false)
    val isSearchOpen: StateFlow<Boolean> = _isSearchOpen.asStateFlow()

    private val _isReplaceMode = MutableStateFlow(false)
    val isReplaceMode: StateFlow<Boolean> = _isReplaceMode.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _replaceQuery = MutableStateFlow("")
    val replaceQuery: StateFlow<String> = _replaceQuery.asStateFlow()

    private val _searchMatches = MutableStateFlow<List<IntRange>>(emptyList())
    val searchMatches: StateFlow<List<IntRange>> = _searchMatches.asStateFlow()

    private val _currentMatchIndex = MutableStateFlow(0)
    val currentMatchIndex: StateFlow<Int> = _currentMatchIndex.asStateFlow()

    private val _isInfoDialogOpen = MutableStateFlow(false)
    val isInfoDialogOpen: StateFlow<Boolean> = _isInfoDialogOpen.asStateFlow()

    private val _isSettingsDialogOpen = MutableStateFlow(false)
    val isSettingsDialogOpen: StateFlow<Boolean> = _isSettingsDialogOpen.asStateFlow()

    private val _isNewFileDialogOpen = MutableStateFlow(false)
    val isNewFileDialogOpen: StateFlow<Boolean> = _isNewFileDialogOpen.asStateFlow()

    private val _isFileExplorerOpen = MutableStateFlow(false)
    val isFileExplorerOpen: StateFlow<Boolean> = _isFileExplorerOpen.asStateFlow()

    private val _isAiAssistantOpen = MutableStateFlow(false)
    val isAiAssistantOpen: StateFlow<Boolean> = _isAiAssistantOpen.asStateFlow()

    private val _isAboutDialogOpen = MutableStateFlow(false)
    val isAboutDialogOpen: StateFlow<Boolean> = _isAboutDialogOpen.asStateFlow()

    val currentTab: EditorTab
        get() = _tabs.value.firstOrNull { it.id == _activeTabId.value } ?: _tabs.value.first()

    val currentText: String
        get() = currentTab.state.text

    init {
        getUndoStack(initialTab.id).add(initialTab.state)
    }

    private fun getUndoStack(id: String): MutableList<TextFieldValue> {
        return undoStacks.getOrPut(id) { mutableListOf() }
    }

    private fun getRedoStack(id: String): MutableList<TextFieldValue> {
        return redoStacks.getOrPut(id) { mutableListOf() }
    }

    fun onTextChanged(newValue: TextFieldValue) {
        val tab = currentTab
        val old = tab.state
        if (old.text != newValue.text) {
            val uStack = getUndoStack(tab.id)
            if (uStack.size > 200) uStack.removeAt(0)
            uStack.add(old)
            getRedoStack(tab.id).clear()

            updateCurrentTab { it.copy(state = newValue, isModified = true) }

            if (_isSearchOpen.value && _searchQuery.value.isNotEmpty()) {
                updateSearchMatches(_searchQuery.value, newValue.text)
            }
        } else {
            updateCurrentTab { it.copy(state = newValue) }
        }
    }

    private fun updateCurrentTab(transform: (EditorTab) -> EditorTab) {
        val activeId = _activeTabId.value
        _tabs.update { list ->
            list.map { if (it.id == activeId) transform(it) else it }
        }
    }

    fun undo() {
        val tab = currentTab
        val uStack = getUndoStack(tab.id)
        if (uStack.isNotEmpty()) {
            val current = tab.state
            getRedoStack(tab.id).add(current)
            val previous = uStack.removeAt(uStack.lastIndex)
            updateCurrentTab { it.copy(state = previous, isModified = true) }
        }
    }

    fun redo() {
        val tab = currentTab
        val rStack = getRedoStack(tab.id)
        if (rStack.isNotEmpty()) {
            val current = tab.state
            getUndoStack(tab.id).add(current)
            val next = rStack.removeAt(rStack.lastIndex)
            updateCurrentTab { it.copy(state = next, isModified = true) }
        }
    }

    fun selectAll() {
        val tab = currentTab
        val len = tab.state.text.length
        updateCurrentTab { it.copy(state = it.state.copy(selection = TextRange(0, len))) }
    }

    fun toggleTheme() {
        _settings.update { it.copy(isDarkMode = !it.isDarkMode) }
    }

    fun updateSettings(newSettings: EditorSettings) {
        _settings.value = newSettings
    }

    // Tabs Management
    fun selectTab(id: String) {
        _activeTabId.value = id
    }

    fun closeTab(id: String) {
        val currentList = _tabs.value
        if (currentList.size <= 1) return

        val closeIndex = currentList.indexOfFirst { it.id == id }
        val newList = currentList.filterNot { it.id == id }
        _tabs.value = newList

        if (_activeTabId.value == id) {
            val newActiveIndex = if (closeIndex >= newList.size) newList.size - 1 else closeIndex
            _activeTabId.value = newList[newActiveIndex].id
        }

        undoStacks.remove(id)
        redoStacks.remove(id)
    }

    fun openFile(name: String, content: String, uri: Uri? = null) = openNewTab(name, content, uri)

    fun openNewTab(name: String, content: String = "", uri: Uri? = null) {
        // If file is already open, switch to that tab
        if (uri != null) {
            val existing = _tabs.value.firstOrNull { it.uri == uri }
            if (existing != null) {
                _activeTabId.value = existing.id
                return
            }
        }

        val newTab = EditorTab(
            id = UUID.randomUUID().toString(),
            fileName = name,
            uri = uri,
            state = TextFieldValue(content, TextRange(0)),
            isModified = false,
            language = SupportedLanguage.detect(name)
        )
        getUndoStack(newTab.id).add(newTab.state)

        _tabs.update { it + newTab }
        _activeTabId.value = newTab.id
    }

    fun markSaved(uri: Uri? = null, name: String? = null) {
        updateCurrentTab { tab ->
            tab.copy(
                isModified = false,
                uri = uri ?: tab.uri,
                fileName = name ?: tab.fileName,
                language = if (name != null) SupportedLanguage.detect(name) else tab.language
            )
        }
    }

    fun resetToDefault() {
        openNewTab("netfetch-broadband-setup.bat", DEFAULT_CODE, null)
    }

    fun insertTextAtCursor(textToInsert: String) {
        val tab = currentTab
        val currentText = tab.state.text
        val selection = tab.state.selection
        val newText = if (selection.collapsed) {
            currentText.substring(0, selection.start) + textToInsert + currentText.substring(selection.start)
        } else {
            currentText.replaceRange(selection.min, selection.max, textToInsert)
        }
        val newCursor = selection.min + textToInsert.length
        onTextChanged(TextFieldValue(newText, TextRange(newCursor)))
    }

    fun replaceEntireFile(newContent: String) {
        onTextChanged(TextFieldValue(newContent, TextRange(0)))
    }

    // Search & Replace
    fun openSearch(replaceMode: Boolean = false) {
        _isReplaceMode.value = replaceMode
        _isSearchOpen.value = true
    }

    fun closeSearch() {
        _isSearchOpen.value = false
        _searchMatches.value = emptyList()
        _currentMatchIndex.value = 0
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        updateSearchMatches(query, currentTab.state.text)
    }

    fun onReplaceQueryChanged(query: String) {
        _replaceQuery.value = query
    }

    private fun updateSearchMatches(query: String, text: String) {
        if (query.isEmpty()) {
            _searchMatches.value = emptyList()
            _currentMatchIndex.value = 0
            return
        }

        val matches = mutableListOf<IntRange>()
        var index = 0
        while (index < text.length) {
            val found = text.indexOf(query, index, ignoreCase = true)
            if (found == -1) break
            matches.add(found until (found + query.length))
            index = found + query.length
        }
        _searchMatches.value = matches
        if (matches.isNotEmpty()) {
            _currentMatchIndex.value = 0
            highlightCurrentMatch()
        }
    }

    fun findNext() {
        val matches = _searchMatches.value
        if (matches.isEmpty()) return
        val next = (_currentMatchIndex.value + 1) % matches.size
        _currentMatchIndex.value = next
        highlightCurrentMatch()
    }

    fun findPrevious() {
        val matches = _searchMatches.value
        if (matches.isEmpty()) return
        val prev = if (_currentMatchIndex.value - 1 < 0) matches.size - 1 else _currentMatchIndex.value - 1
        _currentMatchIndex.value = prev
        highlightCurrentMatch()
    }

    private fun highlightCurrentMatch() {
        val matches = _searchMatches.value
        val idx = _currentMatchIndex.value
        if (idx in matches.indices) {
            val range = matches[idx]
            updateCurrentTab { tab ->
                tab.copy(state = tab.state.copy(selection = TextRange(range.first, range.last + 1)))
            }
        }
    }

    fun replaceCurrent() {
        val matches = _searchMatches.value
        val idx = _currentMatchIndex.value
        if (matches.isNotEmpty() && idx in matches.indices) {
            val range = matches[idx]
            val text = currentTab.state.text
            val newText = text.replaceRange(range.first, range.last + 1, _replaceQuery.value)
            onTextChanged(
                TextFieldValue(
                    text = newText,
                    selection = TextRange(range.first + _replaceQuery.value.length)
                )
            )
            updateSearchMatches(_searchQuery.value, newText)
        }
    }

    fun replaceAll() {
        val query = _searchQuery.value
        if (query.isEmpty()) return
        val text = currentTab.state.text
        val newText = text.replace(query, _replaceQuery.value, ignoreCase = true)
        onTextChanged(TextFieldValue(newText, TextRange(0)))
        updateSearchMatches(query, newText)
    }

    // Dialog toggles
    fun setInfoDialogOpen(open: Boolean) { _isInfoDialogOpen.value = open }
    fun setSettingsDialogOpen(open: Boolean) { _isSettingsDialogOpen.value = open }
    fun setNewFileDialogOpen(open: Boolean) { _isNewFileDialogOpen.value = open }
    fun setFileExplorerOpen(open: Boolean) { _isFileExplorerOpen.value = open }
    fun setAiAssistantOpen(open: Boolean) { _isAiAssistantOpen.value = open }
    fun setAboutDialogOpen(open: Boolean) { _isAboutDialogOpen.value = open }
}
