package com.cphone.editor

import android.net.Uri
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import com.cphone.editor.model.EditorSettings
import com.cphone.editor.model.SupportedLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

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

    private val undoStack = mutableListOf<TextFieldValue>()
    private val redoStack = mutableListOf<TextFieldValue>()
    private var lastRecordedText: String = DEFAULT_CODE

    private val _editorState = MutableStateFlow(TextFieldValue(DEFAULT_CODE, TextRange(0)))
    val editorState: StateFlow<TextFieldValue> = _editorState.asStateFlow()

    private val _fileName = MutableStateFlow("netfetch-broadband-setup.bat")
    val fileName: StateFlow<String> = _fileName.asStateFlow()

    private val _currentFileUri = MutableStateFlow<Uri?>(null)
    val currentFileUri: StateFlow<Uri?> = _currentFileUri.asStateFlow()

    private val _currentLanguage = MutableStateFlow(SupportedLanguage.BATCH)
    val currentLanguage: StateFlow<SupportedLanguage> = _currentLanguage.asStateFlow()

    private val _isModified = MutableStateFlow(false)
    val isModified: StateFlow<Boolean> = _isModified.asStateFlow()

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

    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()

    init {
        undoStack.add(_editorState.value)
    }

    fun onTextChanged(newValue: TextFieldValue) {
        val old = _editorState.value
        if (old.text != newValue.text) {
            _isModified.value = true
            // Push to undo stack
            if (undoStack.size > 200) {
                undoStack.removeAt(0)
            }
            undoStack.add(old)
            redoStack.clear()
            lastRecordedText = newValue.text

            // If search is open, update search matches
            if (_isSearchOpen.value && _searchQuery.value.isNotEmpty()) {
                updateSearchMatches(_searchQuery.value, newValue.text)
            }
        }
        _editorState.value = newValue
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            val current = _editorState.value
            redoStack.add(current)
            val previous = undoStack.removeAt(undoStack.lastIndex)
            _editorState.value = previous
            _isModified.value = true
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            val current = _editorState.value
            undoStack.add(current)
            val next = redoStack.removeAt(redoStack.lastIndex)
            _editorState.value = next
            _isModified.value = true
        }
    }

    fun selectAll() {
        val len = _editorState.value.text.length
        _editorState.value = _editorState.value.copy(selection = TextRange(0, len))
    }

    fun toggleTheme() {
        _settings.update { it.copy(isDarkMode = !it.isDarkMode) }
    }

    fun updateSettings(newSettings: EditorSettings) {
        _settings.value = newSettings
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
        updateSearchMatches(query, _editorState.value.text)
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
            _editorState.value = _editorState.value.copy(
                selection = TextRange(range.first, range.last + 1)
            )
        }
    }

    fun replaceCurrent() {
        val matches = _searchMatches.value
        val idx = _currentMatchIndex.value
        if (matches.isNotEmpty() && idx in matches.indices) {
            val range = matches[idx]
            val text = _editorState.value.text
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
        val text = _editorState.value.text
        val newText = text.replace(query, _replaceQuery.value, ignoreCase = true)
        onTextChanged(TextFieldValue(newText, TextRange(0)))
        updateSearchMatches(query, newText)
    }

    // File handling
    fun openFile(name: String, content: String, uri: Uri?) {
        _fileName.value = name
        _currentFileUri.value = uri
        _currentLanguage.value = SupportedLanguage.detect(name)
        _editorState.value = TextFieldValue(content, TextRange(0))
        undoStack.clear()
        redoStack.clear()
        undoStack.add(_editorState.value)
        _isModified.value = false
    }

    fun newFile(name: String) {
        val actualName = if (name.isBlank()) "untitled.bat" else name
        _fileName.value = actualName
        _currentFileUri.value = null
        _currentLanguage.value = SupportedLanguage.detect(actualName)
        _editorState.value = TextFieldValue("", TextRange(0))
        undoStack.clear()
        redoStack.clear()
        undoStack.add(_editorState.value)
        _isModified.value = false
    }

    fun markSaved(uri: Uri? = null, name: String? = null) {
        _isModified.value = false
        if (uri != null) _currentFileUri.value = uri
        if (name != null) {
            _fileName.value = name
            _currentLanguage.value = SupportedLanguage.detect(name)
        }
    }

    fun resetToDefault() {
        _fileName.value = "netfetch-broadband-setup.bat"
        _currentFileUri.value = null
        _currentLanguage.value = SupportedLanguage.BATCH
        _editorState.value = TextFieldValue(DEFAULT_CODE, TextRange(0))
        undoStack.clear()
        redoStack.clear()
        undoStack.add(_editorState.value)
        _isModified.value = false
    }

    // Dialog toggles
    fun setInfoDialogOpen(open: Boolean) { _isInfoDialogOpen.value = open }
    fun setSettingsDialogOpen(open: Boolean) { _isSettingsDialogOpen.value = open }
    fun setNewFileDialogOpen(open: Boolean) { _isNewFileDialogOpen.value = open }
}
