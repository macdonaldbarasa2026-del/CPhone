package com.cphone.editor.ui

import android.net.Uri
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
import java.io.InputStreamReader

@Composable
fun CPhoneApp(
    viewModel: EditorViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val editorState by viewModel.editorState.collectAsState()
    val fileName by viewModel.fileName.collectAsState()
    val currentUri by viewModel.currentFileUri.collectAsState()
    val currentLanguage by viewModel.currentLanguage.collectAsState()
    val isModified by viewModel.isModified.collectAsState()
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

                // Extract file name
                var name = "opened_file.txt"
                contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1 && cursor.moveToFirst()) {
                        name = cursor.getString(nameIndex)
                    }
                }

                viewModel.openFile(name, content, uri)
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
                    os.write(editorState.text.toByteArray(Charsets.UTF_8))
                }
                var savedName = fileName
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
        if (currentUri != null) {
            try {
                val contentResolver = context.contentResolver
                contentResolver.openOutputStream(currentUri!!, "wt")?.use { os ->
                    os.write(editorState.text.toByteArray(Charsets.UTF_8))
                }
                viewModel.markSaved(currentUri, fileName)
                Toast.makeText(context, "Saved: $fileName", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to save: ${e.message}", Toast.LENGTH_LONG).show()
            }
        } else {
            // Trigger Save As
            createDocumentLauncher.launch(fileName)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopNavigationBar(
                fileName = if (isModified) "$fileName *" else fileName,
                isDarkMode = settings.isDarkMode,
                onHomeClick = {
                    viewModel.resetToDefault()
                    Toast.makeText(context, "Reset to default workspace", Toast.LENGTH_SHORT).show()
                },
                onInfoClick = { viewModel.setInfoDialogOpen(true) },
                onThemeToggleClick = { viewModel.toggleTheme() },
                onNewFileClick = { viewModel.setNewFileDialogOpen(true) },
                onOpenFileClick = {
                    openDocumentLauncher.launch(arrayOf("*/*"))
                },
                onSaveClick = { saveCurrentFile() },
                onSaveAsClick = { createDocumentLauncher.launch(fileName) },
                onFindClick = { viewModel.openSearch(replaceMode = false) },
                onReplaceClick = { viewModel.openSearch(replaceMode = true) },
                onSelectAllClick = { viewModel.selectAll() },
                onUndoClick = { viewModel.undo() },
                onRedoClick = { viewModel.redo() },
                onSettingsClick = { viewModel.setSettingsDialogOpen(true) }
            )
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

            // Main Code Editor Area (occupies almost entire screen)
            CodeEditorArea(
                textFieldValue = editorState,
                onValueChange = { viewModel.onTextChanged(it) },
                language = currentLanguage,
                settings = settings,
                modifier = Modifier.weight(1f)
            )
        }

        // Dialogs
        if (isInfoDialogOpen) {
            val text = editorState.text
            val lineCount = if (text.isEmpty()) 1 else text.count { it == '\n' } + 1
            val charCount = text.length
            val byteSize = text.toByteArray(Charsets.UTF_8).size

            InfoDialog(
                fileName = fileName,
                filePath = currentUri?.path ?: "",
                language = currentLanguage,
                lineCount = lineCount,
                charCount = charCount,
                byteSize = byteSize,
                onDismiss = { viewModel.setInfoDialogOpen(false) }
            )
        }

        if (isSettingsDialogOpen) {
            SettingsDialog(
                settings = settings,
                onSettingsChanged = { viewModel.updateSettings(it) },
                onDismiss = { viewModel.setSettingsDialogOpen(false) }
            )
        }

        if (isNewFileDialogOpen) {
            NewFileDialog(
                onConfirm = { name ->
                    viewModel.newFile(name)
                    viewModel.setNewFileDialogOpen(false)
                },
                onDismiss = { viewModel.setNewFileDialogOpen(false) }
            )
        }
    }
}
