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
import com.cphone.editor.EditorViewModel
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

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

    // Open File SAF Launcher
    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val contentResolver = context.contentResolver
                val inputStream = contentResolver.openInputStream(uri)
                val reader = BufferedReader(InputStreamReader(inputStream))
                val content = reader.readText()
                reader.close()
                inputStream?.close()

                var name = "opened_file.txt"
                contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1 && cursor.moveToFirst()) {
                        name = cursor.getString(nameIndex)
                    }
                }

                viewModel.openNewTab(name, content, uri)
                Toast.makeText(context, "Opened: $name", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to open file: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Save As SAF Launcher
    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*")
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val contentResolver = context.contentResolver
                contentResolver.openOutputStream(uri)?.use { os ->
                    os.write(activeTab.state.text.toByteArray(Charsets.UTF_8))
                }
                var savedName = activeTab.fileName
                contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1 && cursor.moveToFirst()) {
                        savedName = cursor.getString(nameIndex)
                    }
                }
                viewModel.markSaved(uri, savedName)
                Toast.makeText(context, "Saved as: $savedName", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to save file: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Direct Save Function
    fun saveCurrentFile() {
        if (activeTab.uri != null) {
            try {
                val contentResolver = context.contentResolver
                contentResolver.openOutputStream(activeTab.uri, "wt")?.use { os ->
                    os.write(activeTab.state.text.toByteArray(Charsets.UTF_8))
                }
                viewModel.markSaved(activeTab.uri, activeTab.fileName)
                Toast.makeText(context, "Saved: ${activeTab.fileName}", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to save: ${e.message}", Toast.LENGTH_LONG).show()
            }
        } else {
            createDocumentLauncher.launch(activeTab.fileName)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            Column {
                TopNavigationBar(
                    fileName = if (activeTab.isModified) "${activeTab.fileName} *" else activeTab.fileName,
                    isDarkMode = settings.isDarkMode,
                    onHomeClick = { viewModel.setFileExplorerOpen(true) },
                    onInfoClick = { viewModel.setInfoDialogOpen(true) },
                    onThemeToggleClick = { viewModel.toggleTheme() },
                    onAiAssistantClick = { viewModel.setAiAssistantOpen(true) },
                    onNewFileClick = { viewModel.setNewFileDialogOpen(true) },
                    onOpenFileClick = { openDocumentLauncher.launch(arrayOf("*/*")) },
                    onFileExplorerClick = { viewModel.setFileExplorerOpen(true) },
                    onSaveClick = { saveCurrentFile() },
                    onSaveAsClick = { createDocumentLauncher.launch(activeTab.fileName) },
                    onFindClick = { viewModel.openSearch(replaceMode = false) },
                    onReplaceClick = { viewModel.openSearch(replaceMode = true) },
                    onSelectAllClick = { viewModel.selectAll() },
                    onUndoClick = { viewModel.undo() },
                    onRedoClick = { viewModel.redo() },
                    onSettingsClick = { viewModel.setSettingsDialogOpen(true) },
                    onAboutClick = { viewModel.setAboutDialogOpen(true) }
                )

                // Multi-file tab bar (Acode style)
                if (settings.showTabBar && tabs.isNotEmpty()) {
                    EditorTabBar(
                        tabs = tabs,
                        activeTabId = activeTabId,
                        onTabSelected = { viewModel.selectTab(it) },
                        onTabClosed = { id ->
                            val tab = tabs.firstOrNull { it.id == id }
                            if (tab?.isModified == true) {
                                tabToCloseId = id
                            } else {
                                viewModel.closeTab(id)
                            }
                        },
                        onNewTabClick = { viewModel.setNewFileDialogOpen(true) },
                        isDarkMode = settings.isDarkMode
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search / Replace Bar if open
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
                    isDarkMode = settings.isDarkMode
                )
            }

            // Main Code Editor Area with alternating background bands
            CodeEditorArea(
                textFieldValue = activeTab.state,
                onValueChange = { viewModel.onTextChanged(it) },
                language = activeTab.language,
                settings = settings,
                modifier = Modifier.weight(1f)
            )
        }

        // Built-in File Explorer Sheet
        if (isFileExplorerOpen) {
            val startDir = Environment.getExternalStorageDirectory() ?: File("/sdcard")
            FileExplorerSheet(
                initialDirectory = startDir,
                onFileSelected = { file ->
                    try {
                        val content = file.readText(Charsets.UTF_8)
                        val uri = Uri.fromFile(file)
                        viewModel.openNewTab(file.name, content, uri)
                        Toast.makeText(context, "Opened: ${file.name}", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(context, "Cannot open: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                },
                onDismiss = { viewModel.setFileExplorerOpen(false) },
                isDarkMode = settings.isDarkMode
            )
        }

        // AI Assistant Dialog
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

        // File Information Dialog
        if (isInfoDialogOpen) {
            val text = activeTab.state.text
            val lineCount = if (text.isEmpty()) 1 else text.count { it == '\n' } + 1
            val charCount = text.length
            val byteSize = text.toByteArray(Charsets.UTF_8).size

            InfoDialog(
                fileName = activeTab.fileName,
                filePath = activeTab.uri?.path ?: "",
                language = activeTab.language,
                lineCount = lineCount,
                charCount = charCount,
                byteSize = byteSize,
                onDismiss = { viewModel.setInfoDialogOpen(false) }
            )
        }

        // Settings Dialog
        if (isSettingsDialogOpen) {
            SettingsDialog(
                settings = settings,
                onSettingsChanged = { viewModel.updateSettings(it) },
                onOpenAiSettings = { viewModel.setAiAssistantOpen(true) },
                onDismiss = { viewModel.setSettingsDialogOpen(false) }
            )
        }

        // New File Dialog
        if (isNewFileDialogOpen) {
            NewFileDialog(
                onConfirm = { name ->
                    viewModel.openNewTab(name, "")
                    viewModel.setNewFileDialogOpen(false)
                },
                onDismiss = { viewModel.setNewFileDialogOpen(false) }
            )
        }

        // About Dialog
        if (isAboutDialogOpen) {
            AboutDialog(onDismiss = { viewModel.setAboutDialogOpen(false) })
        }

        // Confirm Close Unsaved Tab Dialog
        tabToCloseId?.let { idToClose ->
            val tab = tabs.firstOrNull { it.id == idToClose }
            AlertDialog(
                onDismissRequest = { tabToCloseId = null },
                title = { Text("Unsaved Changes") },
                text = { Text("File '${tab?.fileName}' has unsaved changes. Close anyway?") },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.closeTab(idToClose)
                        tabToCloseId = null
                    }) {
                        Text("Discard & Close", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { tabToCloseId = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
