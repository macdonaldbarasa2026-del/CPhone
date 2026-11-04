package com.cphone.editor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.cphone.editor.model.SupportedLanguage
import com.cphone.editor.syntax.SyntaxHighlighter
import com.cphone.editor.tools.*

data class IdeCommandItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val shortcutHint: String,
    val icon: ImageVector,
    val accent: Color = Color(0xFF61AFEF),
    val action: () -> Unit
)

@Composable
fun CommandPaletteDialog(
    isDarkMode: Boolean,
    onRunCode: () -> Unit,
    onFormatCode: () -> Unit,
    onToggleComment: () -> Unit,
    onDuplicateLine: () -> Unit,
    onDeleteLine: () -> Unit,
    onMoveLineUp: () -> Unit,
    onMoveLineDown: () -> Unit,
    onOpenGoToLine: () -> Unit,
    onOpenSnippets: () -> Unit,
    onOpenBugs: () -> Unit,
    onOpenOutline: () -> Unit,
    onOpenTrailHistory: () -> Unit,
    onSortLinesAsc: () -> Unit,
    onSortLinesDesc: () -> Unit,
    onTrimWhitespace: () -> Unit,
    onToggleZenMode: () -> Unit,
    onToggleWordWrap: () -> Unit,
    onSelectTheme: (String) -> Unit,
    onSelectLanguage: (SupportedLanguage) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var subMenu by remember { mutableStateOf("main") } // "main", "themes", "languages"

    val bg = if (isDarkMode) Color(0xFF1E1E2E) else Color(0xFFFFFFFF)
    val cardBg = if (isDarkMode) Color(0xFF262639) else Color(0xFFF3F4F6)
    val textColor = if (isDarkMode) Color(0xFFE5E9F0) else Color(0xFF1F2328)
    val subColor = if (isDarkMode) Color(0xFF8F96A8) else Color(0xFF6E7781)

    val commands = remember {
        listOf(
            IdeCommandItem("run", "Run / Compile Code", "Execute Python, JS/TS, HTML, CSS, JSON, Markdown", "▶ Run", Icons.Default.PlayArrow, Color(0xFF4CAF50)) {
                onDismiss(); onRunCode()
            },
            IdeCommandItem("bugs", "Inspect Code Bugs & Diagnostics", "View syntax errors, bracket mismatches & warnings", "Problems", Icons.Default.BugReport, Color(0xFFE06C75)) {
                onDismiss(); onOpenBugs()
            },
            IdeCommandItem("outline", "Code Trails: Symbol Outline", "Jump to classes, functions, methods & sections", "Outline", Icons.Default.AccountTree, Color(0xFF61AFEF)) {
                onDismiss(); onOpenOutline()
            },
            IdeCommandItem("trail", "Code Trails: Cursor History", "Retrace recent lines and files visited", "History", Icons.Default.History, Color(0xFFC678DD)) {
                onDismiss(); onOpenTrailHistory()
            },
            IdeCommandItem("format", "Format / Beautify Document", "Auto-indent and clean code structure", "Alt+Shift+F", Icons.Default.AutoFixHigh, Color(0xFF98C379)) {
                onDismiss(); onFormatCode()
            },
            IdeCommandItem("gotoline", "Go to Line...", "Jump directly to a line and column number", "Ctrl+G", Icons.Default.Numbers, Color(0xFFE5C07B)) {
                onDismiss(); onOpenGoToLine()
            },
            IdeCommandItem("snippets", "Insert Code Snippet / Template", "Insert ready-made functions, loops & boilerplates", "Snippets", Icons.Default.Code, Color(0xFF56B6C2)) {
                onDismiss(); onOpenSnippets()
            },
            IdeCommandItem("comment", "Toggle Line Comment", "Comment or uncomment the current line", "Ctrl+/", Icons.Default.Notes, Color(0xFF61AFEF)) {
                onDismiss(); onToggleComment()
            },
            IdeCommandItem("dup", "Duplicate Current Line", "Clone current line directly below", "Shift+Alt+↓", Icons.Default.ContentCopy, Color(0xFF98C379)) {
                onDismiss(); onDuplicateLine()
            },
            IdeCommandItem("del", "Delete Current Line", "Remove the entire current line", "Ctrl+Shift+K", Icons.Default.DeleteSweep, Color(0xFFE06C75)) {
                onDismiss(); onDeleteLine()
            },
            IdeCommandItem("moveup", "Move Line Up", "Swap current line with the line above", "Alt+↑", Icons.Default.KeyboardArrowUp) {
                onDismiss(); onMoveLineUp()
            },
            IdeCommandItem("movedown", "Move Line Down", "Swap current line with the line below", "Alt+↓", Icons.Default.KeyboardArrowDown) {
                onDismiss(); onMoveLineDown()
            },
            IdeCommandItem("theme", "Change Color Theme...", "One Dark Pro, Dracula, Monokai, Tokyo Night, Solarized, GitHub", "Themes →", Icons.Default.Palette, Color(0xFFC678DD)) {
                subMenu = "themes"
            },
            IdeCommandItem("lang", "Change Language Mode...", "Switch syntax highlighting & compiler mode", "Lang →", Icons.Default.Language, Color(0xFFE5C07B)) {
                subMenu = "languages"
            },
            IdeCommandItem("zen", "Toggle Zen Fullscreen Mode", "Maximize code viewport by hiding top/tab chrome", "Zen", Icons.Default.Fullscreen, Color(0xFF56B6C2)) {
                onDismiss(); onToggleZenMode()
            },
            IdeCommandItem("wrap", "Toggle Word Wrap", "Wrap long lines or scroll horizontally", "Alt+Z", Icons.Default.WrapText) {
                onDismiss(); onToggleWordWrap()
            },
            IdeCommandItem("trim", "Trim Trailing Whitespace", "Strip extra spaces at the end of every line", "Clean", Icons.Default.CleaningServices) {
                onDismiss(); onTrimWhitespace()
            },
            IdeCommandItem("sort_asc", "Sort Lines Ascending (A → Z)", "Alphabetically sort lines", "Sort ↑", Icons.Default.SortByAlpha) {
                onDismiss(); onSortLinesAsc()
            },
            IdeCommandItem("sort_desc", "Sort Lines Descending (Z → A)", "Reverse-alphabetically sort lines", "Sort ↓", Icons.Default.SortByAlpha) {
                onDismiss(); onSortLinesDesc()
            }
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.78f),
            shape = RoundedCornerShape(14.dp),
            color = bg,
            tonalElevation = 8.dp
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(14.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (subMenu != "main") {
                        IconButton(
                            onClick = { subMenu = "main" },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = textColor)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Icon(
                        imageVector = Icons.Default.Terminal,
                        contentDescription = null,
                        tint = Color(0xFF61AFEF),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when (subMenu) {
                            "themes" -> "Select IDE Color Theme"
                            "languages" -> "Select Language Mode"
                            else -> "Command Palette"
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = textColor,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = subColor)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (subMenu == "main") {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text("Type a command (e.g. format, bug, theme, line)...", fontSize = 13.sp) },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    val filtered = commands.filter {
                        query.isBlank() ||
                            it.title.contains(query, ignoreCase = true) ||
                            it.subtitle.contains(query, ignoreCase = true)
                    }

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(filtered, key = { it.id }) { cmd ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(cardBg, RoundedCornerShape(8.dp))
                                    .clickable { cmd.action() }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = cmd.icon,
                                    contentDescription = null,
                                    tint = cmd.accent,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = cmd.title,
                                        color = textColor,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = cmd.subtitle,
                                        color = subColor,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Surface(
                                    color = bg,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = cmd.shortcutHint,
                                        color = cmd.accent,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }
                } else if (subMenu == "themes") {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(SyntaxHighlighter.AllThemes, key = { it.id }) { theme ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(theme.editorBg, RoundedCornerShape(10.dp))
                                    .clickable {
                                        onSelectTheme(theme.id)
                                        onDismiss()
                                    }
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .background(theme.accentColor, RoundedCornerShape(8.dp))
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = theme.displayName,
                                        color = theme.syntax.defaultText,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "def main(): return \"${theme.id}\"",
                                        color = theme.syntax.keyword,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                } else if (subMenu == "languages") {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(SupportedLanguage.entries, key = { it.name }) { lang ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(cardBg, RoundedCornerShape(8.dp))
                                    .clickable {
                                        onSelectLanguage(lang)
                                        onDismiss()
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = Color(lang.badgeColorHex),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = lang.badge,
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = lang.displayName,
                                    color = textColor,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = lang.extensions.joinToString(", ") { ".$it" },
                                    color = subColor,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Code Trails & Bug Inspector Sheet:
 * Tab 0: Live Code Bugs & Diagnostics (Syntax Errors, Warnings, TODOs)
 * Tab 1: Symbol Outline (Functions, Classes, Sections, Tags)
 * Tab 2: Cursor Trail History (Recent Jumps across lines/files)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CodeTrailsSheet(
    initialTab: Int,
    fileName: String,
    diagnostics: List<CodeDiagnostic>,
    symbols: List<CodeSymbol>,
    cursorTrail: List<CursorTrailEntry>,
    canGoBack: Boolean,
    canGoForward: Boolean,
    onNavigateBack: () -> Unit,
    onNavigateForward: () -> Unit,
    onJumpToLine: (Int, Int) -> Unit,
    onFormatCode: () -> Unit,
    isDarkMode: Boolean,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(initialTab.coerceIn(0, 2)) }
    val sheetBg = if (isDarkMode) Color(0xFF1A1B26) else Color(0xFFFFFFFF)
    val cardBg = if (isDarkMode) Color(0xFF24283B) else Color(0xFFF3F4F6)
    val textColor = if (isDarkMode) Color(0xFFC0CAF5) else Color(0xFF1F2328)
    val subColor = if (isDarkMode) Color(0xFF7982A9) else Color(0xFF57606A)

    val errorCount = diagnostics.count { it.severity == DiagnosticSeverity.ERROR }
    val warnCount = diagnostics.count { it.severity == DiagnosticSeverity.WARNING }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = sheetBg,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.82f)
                .padding(horizontal = 14.dp)
        ) {
            // Header with Trail Navigation
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Code Trails & Bug Inspector",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = textColor
                    )
                    Text(
                        text = "$fileName • $errorCount Errors, $warnCount Warnings • ${symbols.size} Symbols",
                        fontSize = 11.sp,
                        color = subColor
                    )
                }

                // Trail Back / Forward buttons
                IconButton(onClick = onNavigateBack, enabled = canGoBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Trail Back",
                        tint = if (canGoBack) Color(0xFF61AFEF) else subColor.copy(alpha = 0.4f)
                    )
                }
                IconButton(onClick = onNavigateForward, enabled = canGoForward) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Trail Forward",
                        tint = if (canGoForward) Color(0xFF61AFEF) else subColor.copy(alpha = 0.4f)
                    )
                }
                FilledTonalButton(
                    onClick = { onFormatCode() },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("Auto-Format", fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = sheetBg,
                contentColor = Color(0xFF61AFEF)
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            "Bugs (${diagnostics.size})",
                            fontSize = 12.sp,
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            "Outline (${symbols.size})",
                            fontSize = 12.sp,
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = {
                        Text(
                            "Trail (${cursorTrail.size})",
                            fontSize = 12.sp,
                            fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            when (selectedTab) {
                // ── Tab 0: Bugs & Diagnostics ────────────────────────────────
                0 -> {
                    if (diagnostics.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF4CAF50),
                                    modifier = Modifier.size(44.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No syntax bugs or bracket errors detected!",
                                    color = textColor,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "$fileName passed live static checks.",
                                    color = subColor,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(diagnostics) { diag ->
                                val badgeColor = when (diag.severity) {
                                    DiagnosticSeverity.ERROR -> Color(0xFFE06C75)
                                    DiagnosticSeverity.WARNING -> Color(0xFFE5C07B)
                                    DiagnosticSeverity.INFO -> Color(0xFF61AFEF)
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(cardBg, RoundedCornerShape(8.dp))
                                        .clickable {
                                            onJumpToLine(diag.line, diag.column)
                                            onDismiss()
                                        }
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        color = badgeColor.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "Ln ${diag.line}:${diag.column}",
                                            color = badgeColor,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = diag.message,
                                            color = textColor,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = "Rule: ${diag.rule} • Tap to jump to line ${diag.line}",
                                            color = subColor,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Tab 1: Symbol Outline ────────────────────────────────────
                1 -> {
                    if (symbols.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No functions, classes, or sections found in $fileName.",
                                color = subColor,
                                fontSize = 13.sp
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(symbols) { sym ->
                                val badgeColor = when (sym.kind) {
                                    "class", "interface" -> Color(0xFFE5C07B)
                                    "function", "method" -> Color(0xFF61AFEF)
                                    else -> Color(0xFFC678DD)
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(cardBg, RoundedCornerShape(8.dp))
                                        .clickable {
                                            onJumpToLine(sym.line, 1)
                                            onDismiss()
                                        }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        color = badgeColor.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = sym.badge,
                                            color = badgeColor,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = sym.name,
                                        color = textColor,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "Line ${sym.line}",
                                        color = subColor,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }

                // ── Tab 2: Cursor Trail History ──────────────────────────────
                2 -> {
                    val reversedTrail = remember(cursorTrail) { cursorTrail.reversed() }
                    if (reversedTrail.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Your cursor navigation trail will appear here as you edit.",
                                color = subColor,
                                fontSize = 13.sp
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(reversedTrail) { entry ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(cardBg, RoundedCornerShape(8.dp))
                                        .clickable {
                                            onJumpToLine(entry.line, entry.column)
                                            onDismiss()
                                        }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MyLocation,
                                        contentDescription = null,
                                        tint = Color(0xFF98C379),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${entry.fileName} › ${entry.symbolName}",
                                            color = textColor,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "Line ${entry.line}, Column ${entry.column}",
                                            color = subColor,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SnippetsDialog(
    language: SupportedLanguage,
    isDarkMode: Boolean,
    onInsertSnippet: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val snippets = remember(language) { CodeAnalyzer.getSnippets(language) }
    val bg = if (isDarkMode) Color(0xFF1E1E2E) else Color(0xFFFFFFFF)
    val cardBg = if (isDarkMode) Color(0xFF282A3E) else Color(0xFFF3F4F6)
    val textColor = if (isDarkMode) Color(0xFFE5E9F0) else Color(0xFF1F2328)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = bg,
        title = {
            Text(
                text = "${language.displayName} Code Snippets",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = textColor
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 380.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(snippets) { snippet ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(cardBg, RoundedCornerShape(8.dp))
                            .clickable {
                                onInsertSnippet(snippet.body)
                                onDismiss()
                            }
                            .padding(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = snippet.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = textColor
                            )
                            Text(
                                text = snippet.trigger,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = Color(0xFF61AFEF)
                            )
                        }
                        Text(
                            text = snippet.description,
                            fontSize = 11.sp,
                            color = textColor.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun GoToLineDialog(
    currentLine: Int,
    totalLines: Int,
    onConfirm: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var input by remember { mutableStateOf(currentLine.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Go to Line (1 – $totalLines)", fontWeight = FontWeight.Bold, fontSize = 17.sp) },
        text = {
            Column {
                Text("Enter line number or line:column (e.g. 42 or 42:8):", fontSize = 13.sp)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val parts = input.trim().split(':')
                    val line = parts.getOrNull(0)?.toIntOrNull() ?: currentLine
                    val col = parts.getOrNull(1)?.toIntOrNull() ?: 1
                    onConfirm(line, col)
                    onDismiss()
                }
            ) {
                Text("Jump")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
