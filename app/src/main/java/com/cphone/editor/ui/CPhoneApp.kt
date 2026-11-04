package com.cphone.editor.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.dp
import com.cphone.editor.EditorViewModel
import com.cphone.editor.model.SupportedLanguage
import com.cphone.editor.syntax.SyntaxHighlighter
import com.cphone.editor.tools.DiagnosticSeverity
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CPhoneApp(
    viewModel: EditorViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val tabs by viewModel.tabs.collectAsState()
    val activeTabId by viewModel.activeTabId.collectAsState()
    val activeTab = tabs.firstOrNull { it.id == activeTabId } ?: tabs.firstOrNull() ?: viewModel.currentTab
    val settings by viewModel.settings.collectAsState()
    val isZenMode by viewModel.isZenMode.collectAsState()

    // Code Trails & Diagnostics states
    val diagnostics by viewModel.diagnostics.collectAsState()
    val symbols by viewModel.symbols.collectAsState()
    val activeSymbol by viewModel.activeSymbol.collectAsState()
    val cursorLine by viewModel.cursorLine.collectAsState()
    val cursorColumn by viewModel.cursorColumn.collectAsState()
    val cursorTrail by viewModel.cursorTrail.collectAsState()
    val trailIndex by viewModel.trailIndex.collectAsState()

    // Search & Replace states
    val isSearchOpen by viewModel.isSearchOpen.collectAsState()
    val isReplaceMode by viewModel.isReplaceMode.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val replaceQuery by viewModel.replaceQuery.collectAsState()
    val searchMatches by viewModel.searchMatches.collectAsState()
    val currentMatchIndex by viewModel.currentMatchIndex.collectAsState()

    // Dialog & Sheet states
    val isInfoDialogOpen by viewModel.isInfoDialogOpen.collectAsState()
    val isSettingsDialogOpen by viewModel.isSettingsDialogOpen.collectAsState()
    val isNewFileDialogOpen by viewModel.isNewFileDialogOpen.collectAsState()
    val isFileExplorerOpen by viewModel.isFileExplorerOpen.collectAsState()
    val isAiAssistantOpen by viewModel.isAiAssistantOpen.collectAsState()
    val isAboutDialogOpen by viewModel.isAboutDialogOpen.collectAsState()
    val isCodeRunnerOpen by viewModel.isCodeRunnerOpen.collectAsState()
    val isRenameDialogOpen by viewModel.isRenameDialogOpen.collectAsState()
    val isCommandPaletteOpen by viewModel.isCommandPaletteOpen.collectAsState()
    val isCodeTrailsOpen by viewModel.isCodeTrailsOpen.collectAsState()
    val codeTrailsInitialTab by viewModel.codeTrailsInitialTab.collectAsState()
    val isSnippetsOpen by viewModel.isSnippetsOpen.collectAsState()
    val isGoToLineOpen by viewModel.isGoToLineOpen.collectAsState()

    var tabToCloseId by remember { mutableStateOf<String?>(null) }
    // By default, symbol buttons ({}, [], etc.) are hidden; tapping the bottom-right '^' circle button reveals/hides them
    var isSymbolsRevealed by remember { mutableStateOf(false) }

    val bugCount = remember(diagnostics) { diagnostics.count { it.severity == DiagnosticSeverity.ERROR } }
    val warnCount = remember(diagnostics) { diagnostics.count { it.severity == DiagnosticSeverity.WARNING } }

    // ── Clipboard Helpers for Pencil Menu (Paste, Copy, Cut) ─────────────────
    fun copyToClipboard() {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val textToCopy = viewModel.getSelectedOrCurrentLineText()
        if (textToCopy.isNotEmpty()) {
            clipboard?.setPrimaryClip(ClipData.newPlainText("code", textToCopy))
            Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
        }
    }

    fun cutToClipboard() {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val cutText = viewModel.cutSelectedOrCurrentLineText()
        if (cutText.isNotEmpty()) {
            clipboard?.setPrimaryClip(ClipData.newPlainText("code", cutText))
            Toast.makeText(context, "Cut to clipboard", Toast.LENGTH_SHORT).show()
        }
    }

    fun pasteFromClipboard() {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val item = clipboard?.primaryClip?.getItemAt(0)
        val pasted = item?.coerceToText(context)?.toString() ?: ""
        if (pasted.isNotEmpty()) {
            viewModel.insertTextAtCursor(pasted)
        } else {
            Toast.makeText(context, "Clipboard is empty", Toast.LENGTH_SHORT).show()
        }
    }

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

    fun insertTab() {
        viewModel.insertTextAtCursor(" ".repeat(settings.tabSize))
    }

    fun moveCursorLeft() {
        val tab = viewModel.currentTab
        val pos = (tab.state.selection.start - 1).coerceAtLeast(0)
        viewModel.onTextChanged(tab.state.copy(selection = TextRange(pos)))
    }

    fun moveCursorRight() {
        val tab = viewModel.currentTab
        val pos = (tab.state.selection.start + 1).coerceAtMost(tab.state.text.length)
        viewModel.onTextChanged(tab.state.copy(selection = TextRange(pos)))
    }

    val isDark = settings.isDarkMode
    val palette = remember(settings.editorTheme, isDark) {
        SyntaxHighlighter.resolvePalette(settings.editorTheme, isDark)
    }

    // Unified full-screen layout with zero white gap above the IME keyboard (fixes Image 3)
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(palette.editorBg)
            .statusBarsPadding()
            .displayCutoutPadding()
            .imePadding()
            .navigationBarsPadding()
    ) {
        val subtitleText = when {
            activeTab.uri == null && activeTab.isModified -> "New file • Modified"
            activeTab.uri == null -> "New file"
            activeTab.isModified -> "${activeTab.uri.lastPathSegment ?: activeTab.fileName} • Modified"
            else -> activeTab.uri.path ?: "Saved file"
        }

        val isRunnable = remember(activeTab.fileName, activeTab.language, activeTab.state.text) {
            SupportedLanguage.isRunnable(activeTab.fileName, activeTab.state.text) || activeTab.language.isRunnable
        }

        TopNavigationBar(
            fileName = activeTab.fileName,
            subtitle = subtitleText,
            language = activeTab.language,
            isDarkMode = isDark,
            themeId = settings.editorTheme,
            isRunnable = isRunnable,
            isZenMode = isZenMode,
            bugCount = bugCount,
            warningCount = warnCount,
            onPlayClick = { viewModel.setCodeRunnerOpen(true) },
            onTitleClick = { viewModel.setRenameDialogOpen(true) },
            onBugPillClick = { viewModel.setCodeTrailsOpen(true, initialTab = 0) },
            onCommandPaletteClick = { viewModel.setCommandPaletteOpen(true) },
            onToggleZenClick = { viewModel.toggleZenMode() },
            onFormatClick = {
                viewModel.formatCurrentCode()
                Toast.makeText(context, "Code formatted ✓", Toast.LENGTH_SHORT).show()
            },
            onSnippetsClick = { viewModel.setSnippetsOpen(true) },
            onGoToLineClick = { viewModel.setGoToLineOpen(true) },
            onPasteClick = { pasteFromClipboard() },
            onCopyClick = { copyToClipboard() },
            onCutClick = { cutToClipboard() },
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

        if (!isZenMode && settings.showTabBar && tabs.isNotEmpty()) {
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
                isDarkMode = isDark,
                themeId = settings.editorTheme,
                showCodeTrail = settings.showCodeTrailBar,
                activeSymbol = activeSymbol,
                cursorLine = cursorLine,
                cursorColumn = cursorColumn,
                fontSizeSp = settings.fontSizeSp,
                canNavigateBack = trailIndex > 0,
                canNavigateForward = trailIndex in 0 until cursorTrail.lastIndex,
                onNavigateBack = { viewModel.navigateTrailBack() },
                onNavigateForward = { viewModel.navigateTrailForward() },
                onOpenOutline = { viewModel.setCodeTrailsOpen(true, initialTab = 1) },
                onOpenGoToLine = { viewModel.setGoToLineOpen(true) },
                onFontSizeDelta = { viewModel.adjustFontSize(it) }
            )
        }

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

        // Main Code Editor Canvas + Floating Bottom-Right '^' Circle Button
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            CodeEditorArea(
                textFieldValue = activeTab.state,
                onValueChange = { viewModel.onTextChanged(it) },
                language = activeTab.language,
                settings = settings,
                diagnostics = diagnostics,
                currentLine = cursorLine,
                searchQuery = if (isSearchOpen) searchQuery else "",
                onFontSizeCalibrated = { newSize ->
                    viewModel.updateSettings(settings.copy(fontSizeSp = newSize))
                },
                onBugLineClicked = { viewModel.setCodeTrailsOpen(true, initialTab = 0) },
                modifier = Modifier.fillMaxSize()
            )

            // Floating Circular '^' / 'v' Button at Bottom-Right
            // Default state is hidden; tapping reveals {}, [], (), etc. Tapping again hides them.
            Surface(
                onClick = { isSymbolsRevealed = !isSymbolsRevealed },
                shape = CircleShape,
                color = if (isDark) Color(0xFF2C313C) else Color.White,
                contentColor = if (isDark) Color.White else Color(0xFF1F2937),
                shadowElevation = 6.dp,
                border = BorderStroke(
                    width = 1.dp,
                    color = if (isDark) Color(0xFF4B5563) else Color(0xFFD1D5DB)
                ),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 18.dp, bottom = 14.dp)
                    .size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isSymbolsRevealed) {
                            Icons.Default.KeyboardArrowDown
                        } else {
                            Icons.Default.KeyboardArrowUp
                        },
                        contentDescription = if (isSymbolsRevealed) "Hide Symbol Buttons" else "Reveal Symbol Buttons ({} [])",
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }

        // Bottom Bar sits flush above the keyboard with zero gap
        EditorBottomBar(
            isDarkMode = isDark,
            themeId = settings.editorTheme,
            isRunnable = isRunnable,
            isSymbolsRevealed = isSymbolsRevealed,
            onPlay = { viewModel.setCodeRunnerOpen(true) },
            onUndo = { viewModel.undo() },
            onRedo = { viewModel.redo() },
            onTab = { insertTab() },
            onFind = { viewModel.openSearch(false) },
            onSave = { saveCurrentFile() },
            onEsc = {
                if (isSearchOpen) viewModel.closeSearch()
                else if (isSymbolsRevealed) isSymbolsRevealed = false
            },
            onCtrlShortcut = { viewModel.setCommandPaletteOpen(true) },
            onMoveCursorLeft = { moveCursorLeft() },
            onMoveCursorRight = { moveCursorRight() },
            onMoveCursorUp = { viewModel.moveCursorUp() },
            onMoveCursorDown = { viewModel.moveCursorDown() },
            onMoveLineStart = { viewModel.moveCursorToLineStart() },
            onMoveLineEnd = { viewModel.moveCursorToLineEnd() },
            onToggleComment = { viewModel.toggleLineComment() },
            onDuplicateLine = { viewModel.duplicateLine() },
            onDeleteLine = { viewModel.deleteLine() },
            onFormatCode = {
                viewModel.formatCurrentCode()
                Toast.makeText(context, "Formatted ✓", Toast.LENGTH_SHORT).show()
            },
            onOpenSnippets = { viewModel.setSnippetsOpen(true) },
            onSelectAll = { viewModel.selectAll() },
            onInsertText = { viewModel.insertTextAtCursor(it) }
        )
    }

    // ── Overlays & Dialogs ───────────────────────────────────────────────

    if (isCommandPaletteOpen) {
        CommandPaletteDialog(
            isDarkMode = isDark,
            onRunCode = { viewModel.setCodeRunnerOpen(true) },
            onFormatCode = {
                viewModel.formatCurrentCode()
                Toast.makeText(context, "Code formatted ✓", Toast.LENGTH_SHORT).show()
            },
            onToggleComment = { viewModel.toggleLineComment() },
            onDuplicateLine = { viewModel.duplicateLine() },
            onDeleteLine = { viewModel.deleteLine() },
            onMoveLineUp = { viewModel.moveLineUp() },
            onMoveLineDown = { viewModel.moveLineDown() },
            onOpenGoToLine = { viewModel.setGoToLineOpen(true) },
            onOpenSnippets = { viewModel.setSnippetsOpen(true) },
            onOpenBugs = { viewModel.setCodeTrailsOpen(true, initialTab = 0) },
            onOpenOutline = { viewModel.setCodeTrailsOpen(true, initialTab = 1) },
            onOpenTrailHistory = { viewModel.setCodeTrailsOpen(true, initialTab = 2) },
            onSortLinesAsc = { viewModel.sortLines(true) },
            onSortLinesDesc = { viewModel.sortLines(false) },
            onTrimWhitespace = {
                viewModel.trimTrailingWhitespace()
                Toast.makeText(context, "Trimmed trailing whitespace ✓", Toast.LENGTH_SHORT).show()
            },
            onToggleZenMode = { viewModel.toggleZenMode() },
            onToggleWordWrap = {
                viewModel.updateSettings(settings.copy(wordWrap = !settings.wordWrap))
            },
            onSelectTheme = { viewModel.setEditorTheme(it) },
            onSelectLanguage = { viewModel.changeLanguage(it) },
            onDismiss = { viewModel.setCommandPaletteOpen(false) }
        )
    }

    if (isCodeTrailsOpen) {
        CodeTrailsSheet(
            initialTab = codeTrailsInitialTab,
            fileName = activeTab.fileName,
            diagnostics = diagnostics,
            symbols = symbols,
            cursorTrail = cursorTrail,
            canGoBack = trailIndex > 0,
            canGoForward = trailIndex in 0 until cursorTrail.lastIndex,
            onNavigateBack = { viewModel.navigateTrailBack() },
            onNavigateForward = { viewModel.navigateTrailForward() },
            onJumpToLine = { line, col -> viewModel.goToLine(line, col) },
            onFormatCode = {
                viewModel.formatCurrentCode()
                Toast.makeText(context, "Auto-formatted ✓", Toast.LENGTH_SHORT).show()
            },
            isDarkMode = isDark,
            onDismiss = { viewModel.setCodeTrailsOpen(false) }
        )
    }

    if (isSnippetsOpen) {
        SnippetsDialog(
            language = activeTab.language,
            isDarkMode = isDark,
            onInsertSnippet = { viewModel.insertTextAtCursor(it) },
            onDismiss = { viewModel.setSnippetsOpen(false) }
        )
    }

    if (isGoToLineOpen) {
        val totalLines = activeTab.state.text.count { it == '\n' } + 1
        GoToLineDialog(
            currentLine = cursorLine,
            totalLines = totalLines,
            onConfirm = { line, col -> viewModel.goToLine(line, col) },
            onDismiss = { viewModel.setGoToLineOpen(false) }
        )
    }

    if (isFileExplorerOpen) {
        val startDir = Environment.getExternalStorageDirectory() ?: File("/sdcard")
        FileExplorerSheet(
            initialDirectory = startDir,
            onFileSelected = { file ->
                try {
                    val content = file.readText(Charsets.UTF_8)
                    viewModel.openNewTab(file.name, content, Uri.fromFile(file))
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
            filePath = activeTab.uri?.path ?: "(unsaved / in-session)",
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
            onConfirm = { name, useStarter ->
                viewModel.openNewTab(name, "", useStarterIfBlank = useStarter)
                viewModel.setNewFileDialogOpen(false)
            },
            onDismiss = { viewModel.setNewFileDialogOpen(false) }
        )
    }

    if (isAboutDialogOpen) {
        AboutDialog(onDismiss = { viewModel.setAboutDialogOpen(false) })
    }

    if (isCodeRunnerOpen) {
        CodeRunnerDialog(
            fileName = activeTab.fileName,
            code = activeTab.state.text,
            language = activeTab.language,
            openTabs = tabs,
            isDarkMode = isDark,
            onJumpToLine = { errLine -> viewModel.goToLine(errLine, 1) },
            onDismiss = { viewModel.setCodeRunnerOpen(false) }
        )
    }

    if (isRenameDialogOpen) {
        RenameFileDialog(
            currentName = activeTab.fileName,
            onConfirm = { newName ->
                viewModel.renameCurrentTab(newName)
                viewModel.setRenameDialogOpen(false)
            },
            onDismiss = { viewModel.setRenameDialogOpen(false) }
        )
    }

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
