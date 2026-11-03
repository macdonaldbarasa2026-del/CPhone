package com.cphone.editor.ui

import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.cphone.editor.EditorViewModel
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CPhoneApp(
    viewModel: EditorViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val tabs by viewModel.tabs.collectAsState()
    val activeTabId by viewModel.activeTabId.collectAsState()
    val activeTab = viewModel.currentTab
    val settings by viewModel.settings.collectAsState()

    // Dialog & Bar states
    val isSearchOpen by viewModel.isSearchOpen.collectAsState()
    val isReplaceMode by viewModel.isReplaceMode.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val replaceQuery by viewModel.replaceQuery.collectAsState()
    val searchMatches by viewModel.searchMatches.collectAsState()
    val currentMatchIndex by viewModel.currentMatchIndex.collectAsState()

    val isInfoDialogOpen by viewModel.isInfoDialogOpen.collectAsState()
    val isSettingsDialogOpen by viewModel.isSettingsDialogOpen.collectAsState()
    val isNewFileDialogOpen by viewModel.isNewFileDialogOpen.collectAsState()
    val isFileExplorerOpen by viewModel.isFileExplorerOpen.collectAsState()
    val isAiAssistantOpen by viewModel.isAiAssistantOpen.collectAsState()
    val isAboutDialogOpen by viewModel.isAboutDialogOpen.collectAsState()

    var tabToCloseId by remember { mutableStateOf<String?>(null) }

    // ── SAF: Open File ───────────────────────────────────────────────────────
    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val cr = context.contentResolver
                val content = cr.openInputStream(uri)?.bufferedReader()?.readText() ?: ""
                var name = "file.txt"
                cr.query(uri, null, null, null, null)?.use { c ->
                    val i = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (i != -1 && c.moveToFirst()) name = c.getString(i)
                }
                viewModel.openNewTab(name, content, uri)
                Toast.makeText(context, "Opened: $name", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Cannot open: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // ── SAF: Save As ─────────────────────────────────────────────────────────
    val saveAsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*")
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val cr = context.contentResolver
                cr.openOutputStream(uri)?.use { it.write(activeTab.state.text.toByteArray()) }
                var name = activeTab.fileName
                cr.query(uri, null, null, null, null)?.use { c ->
                    val i = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (i != -1 && c.moveToFirst()) name = c.getString(i)
                }
                viewModel.markSaved(uri, name)
                Toast.makeText(context, "Saved: $name", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Save failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // ── Save current file ────────────────────────────────────────────────────
    fun saveCurrentFile() {
        val tab = viewModel.currentTab
        if (tab.uri != null) {
            try {
                context.contentResolver.openOutputStream(tab.uri, "wt")
                    ?.use { it.write(tab.state.text.toByteArray()) }
                viewModel.markSaved(tab.uri, tab.fileName)
                Toast.makeText(context, "Saved ✓", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Save failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        } else {
            saveAsLauncher.launch(tab.fileName)
        }
    }

    // ── Insert tab at cursor ─────────────────────────────────────────────────
    fun insertTab() {
        viewModel.insertTextAtCursor(" ".repeat(settings.tabSize))
    }

    // ── Move cursor left/right (selection update) ────────────────────────────
    fun moveCursorLeft() {
        val tab = viewModel.currentTab
        val pos = (tab.state.selection.start - 1).coerceAtLeast(0)
        viewModel.onTextChanged(tab.state.copy(selection = androidx.compose.ui.text.TextRange(pos)))
    }
    fun moveCursorRight() {
        val tab = viewModel.currentTab
        val pos = (tab.state.selection.start + 1).coerceAtMost(tab.state.text.length)
        viewModel.onTextChanged(tab.state.copy(selection = androidx.compose.ui.text.TextRange(pos)))
    }

    // ── Theme colours ────────────────────────────────────────────────────────
    val isDark = settings.isDarkMode

    // ── Scaffold with proper WindowInsets ───────────────────────────────────
    // contentWindowInsets = WindowInsets(0) so we handle insets manually
    // This prevents Scaffold from double-applying padding.
    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            // TopBar sits inside Scaffold's top slot; Scaffold automatically
            // places it below the status bar when contentWindowInsets is handled.
            // We add statusBarsPadding() explicitly so it always clears the notch.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()   // ← clears notch / status bar
                    .displayCutoutPadding() // ← clears camera cutout on all sides
            ) {
                TopNavigationBar(
                    fileName = if (activeTab.isModified) "${activeTab.fileName} ●" else activeTab.fileName,
                    isDarkMode = isDark,
                    onHomeClick = { viewModel.setFileExplorerOpen(true) },
                    onInfoClick = { viewModel.setInfoDialogOpen(true) },
                    onThemeToggleClick = { viewModel.toggleTheme() },
                    onAiAssistantClick = { viewModel.setAiAssistantOpen(true) },
                    onNewFileClick = { viewModel.setNewFileDialogOpen(true) },
                    onOpenFileClick = { openDocumentLauncher.launch(arrayOf("*/*")) },
                    onFileExplorerClick = { viewModel.setFileExplorerOpen(true) },
                    onSaveClick = { saveCurrentFile() },
                    onSaveAsClick = { saveAsLauncher.launch(activeTab.fileName) },
                    onFindClick = { viewModel.openSearch(false) },
                    onReplaceClick = { viewModel.openSearch(true) },
                    onSelectAllClick = { viewModel.selectAll() },
                    onUndoClick = { viewModel.undo() },
                    onRedoClick = { viewModel.redo() },
                    onSettingsClick = { viewModel.setSettingsDialogOpen(true) },
                    onAboutClick = { viewModel.setAboutDialogOpen(true) }
                )

                // Multi-file tab bar
                if (settings.showTabBar && tabs.isNotEmpty()) {
                    EditorTabBar(
                        tabs = tabs,
                        activeTabId = activeTabId,
                        onTabSelected = { viewModel.selectTab(it) },
                        onTabClosed = { id ->
                            val tab = tabs.firstOrNull { it.id == id }
                            if (tab?.isModified == true) tabToCloseId = id
                            else viewModel.closeTab(id)
                        },
                        onNewTabClick = { viewModel.setNewFileDialogOpen(true) },
                        isDarkMode = isDark
                    )
                }
            }
        },
        bottomBar = {
            // Bottom action bar clears the navigation bar / gesture bar
            EditorBottomBar(
                isDarkMode = isDark,
                onUndo = { viewModel.undo() },
                onRedo = { viewModel.redo() },
                onTab = { insertTab() },
                onFind = { viewModel.openSearch(false) },
                onSave = { saveCurrentFile() },
                onMoveCursorLeft = { moveCursorLeft() },
                onMoveCursorRight = { moveCursorRight() },
                modifier = Modifier.navigationBarsPadding() // ← clears nav bar / gesture area
            )
        }
    ) { innerPadding ->
        // innerPadding already accounts for topBar + bottomBar heights set by Scaffold
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()  // ← pushes content up when keyboard opens
        ) {
            // Search / Replace Bar
            if (isSearchOpen) {
                SearchReplaceBar(
                    searchQuery = searchQuery,
                    onSearchQueryChange = { viewModel.onSearchQueryChanged(it) },
                    replaceQuery = replaceQuery,
                    onReplaceQueryChange = { viewModel.onReplaceQueryChanged(it) },
                    isReplaceMode = isReplaceMode,
                    matchCount = searchMatches.size,
                    currentMatchIndex = currentMatchIndex,
                    onFindNext = { viewModel.findNext() },
                    onFindPrevious = { viewModel.findPrevious() },
                    onReplace = { viewModel.replaceCurrent() },
                    onReplaceAll = { viewModel.replaceAll() },
                    onClose = { viewModel.closeSearch() },
                    isDarkMode = isDark
                )
            }

            // Code editor area — fills all remaining space
            CodeEditorArea(
                textFieldValue = activeTab.state,
                onValueChange = { viewModel.onTextChanged(it) },
                language = activeTab.language,
                settings = settings,
                modifier = Modifier.weight(1f)
            )
        }

        // ── Overlays & Dialogs ───────────────────────────────────────────────

        if (isFileExplorerOpen) {
            val startDir = Environment.getExternalStorageDirectory() ?: File("/sdcard")
            FileExplorerSheet(
                initialDirectory = startDir,
                onFileSelected = { file ->
                    try {
                        val content = file.readText(Charsets.UTF_8)
                        viewModel.openNewTab(file.name, content, android.net.Uri.fromFile(file))
                        Toast.makeText(context, "Opened: ${file.name}", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(context, "Cannot open: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                },
                onDismiss = { viewModel.setFileExplorerOpen(false) },
                isDarkMode = isDark
            )
        }

        if (isAiAssistantOpen) {
            val sel = activeTab.state.selection
            val selectedCode = if (sel.collapsed) "" else activeTab.state.text.substring(sel.min, sel.max)
            AiAssistantDialog(
                selectedCode = selectedCode,
                fullCode = activeTab.state.text,
                settings = settings,
                onSaveSettings = { viewModel.updateSettings(it) },
                onInsertCode = { viewModel.insertTextAtCursor(it) },
                onReplaceFile = { viewModel.replaceEntireFile(it) },
                onDismiss = { viewModel.setAiAssistantOpen(false) }
            )
        }

        if (isInfoDialogOpen) {
            val t = activeTab.state.text
            InfoDialog(
                fileName = activeTab.fileName,
                filePath = activeTab.uri?.path ?: "(unsaved)",
                language = activeTab.language,
                lineCount = if (t.isEmpty()) 1 else t.count { it == '\n' } + 1,
                charCount = t.length,
                byteSize = t.toByteArray().size,
                onDismiss = { viewModel.setInfoDialogOpen(false) }
            )
        }

        if (isSettingsDialogOpen) {
            SettingsDialog(
                settings = settings,
                onSettingsChanged = { viewModel.updateSettings(it) },
                onOpenAiSettings = { viewModel.setAiAssistantOpen(true) },
                onDismiss = { viewModel.setSettingsDialogOpen(false) }
            )
        }

        if (isNewFileDialogOpen) {
            NewFileDialog(
                onConfirm = { name ->
                    viewModel.openNewTab(name, "")
                    viewModel.setNewFileDialogOpen(false)
                },
                onDismiss = { viewModel.setNewFileDialogOpen(false) }
            )
        }

        if (isAboutDialogOpen) {
            AboutDialog(onDismiss = { viewModel.setAboutDialogOpen(false) })
        }

        // Confirm close unsaved tab
        tabToCloseId?.let { id ->
            val tab = tabs.firstOrNull { it.id == id }
            AlertDialog(
                onDismissRequest = { tabToCloseId = null },
                title = { Text("Unsaved Changes") },
                text = { Text("'${tab?.fileName}' has unsaved changes. Close anyway?") },
                confirmButton = {
                    TextButton(onClick = { viewModel.closeTab(id); tabToCloseId = null }) {
                        Text("Discard & Close", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { tabToCloseId = null }) { Text("Cancel") }
                }
            )
        }
    }
}
