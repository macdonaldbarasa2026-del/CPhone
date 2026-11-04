package com.cphone.editor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cphone.editor.model.EditorTab
import com.cphone.editor.terminal.AnsiParser
import com.cphone.editor.terminal.ExecutionBackend
import com.cphone.editor.terminal.TerminalSession
import com.cphone.editor.terminal.TermuxBridge

/**
 * Full-screen terminal dialog that behaves like a real Termux-style shell on phone.
 * Layout (top → bottom):
 *   1. Header bar  — title + status badge + quick action icons
 *   2. Output area — scrollable ANSI-rendered output (weight=1)
 *   3. Extra keys  — horizontally scrollable shortcut keys row
 *   4. Input row   — $ prompt + TextField + Send button
 *
 * Key phone fixes:
 *  - Uses Box(fillMaxSize) + imePadding() so keyboard pushes the input row up
 *  - FocusRequester auto-focuses the input field when the terminal opens
 *  - KeyboardOptions set to no autocorrect / no caps — proper terminal keyboard
 *  - SelectionContainer removed from LazyColumn (causes focus theft)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalDialog(
    terminalSession: TerminalSession,
    activeTab: EditorTab,
    isDarkMode: Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lines by terminalSession.lines.collectAsState()
    val isBusy by terminalSession.isBusy.collectAsState()

    var inputCommand by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    val isTermuxInstalled = remember { TermuxBridge.isTermuxInstalled(context) }
    val backend by terminalSession.currentBackend.collectAsState()

    // Scroll to bottom whenever new output lines arrive
    LaunchedEffect(lines.size) {
        if (lines.isNotEmpty()) {
            listState.animateScrollToItem(lines.size - 1)
        }
    }

    // Auto-focus input + show keyboard when dialog opens
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboard?.show()
    }

    // ── Colour palette ────────────────────────────────────────────────────────
    val terminalBg  = Color(0xFF0D1117)
    val headerBg    = Color(0xFF161B22)
    val inputRowBg  = Color(0xFF161B22)
    val keyRowBg    = Color(0xFF1C2128)
    val keyBg       = Color(0xFF2D333B)
    val textColor   = Color(0xFFCDD9E5)
    val greenAccent = Color(0xFF3FB950)
    val cyanAccent  = Color(0xFF58A6FF)
    val redAccent   = Color(0xFFF85149)

    fun submitCurrentInput() {
        val cmd = inputCommand.trim()
        if (cmd.isNotEmpty()) {
            terminalSession.execute(cmd)
            inputCommand = ""
        }
    }

    // Full-screen overlay — NOT using Dialog() to avoid keyboard inset issues
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(terminalBg)
            .imePadding()           // ← pushes content above software keyboard
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── 1. Header Bar ─────────────────────────────────────────────────
            Surface(color = headerBg, modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Status dot
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(
                                color = if (isBusy) Color(0xFFD29922) else greenAccent,
                                shape = CircleShape
                            )
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Title + status badge — kept on one horizontal line, no wrapping
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 4.dp)
                    ) {
                        // Title row: "CPhone Terminal" + inline status pill
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "CPhone Terminal",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = textColor,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                            // Termux Linked / Local Shell status badge — always on same line
                            val badgeText = when {
                                backend == ExecutionBackend.SOCKET_BRIDGE -> "Termux Socket"
                                backend == ExecutionBackend.TERMUX_RUN_COMMAND -> "Termux Service"
                                isTermuxInstalled -> "Termux Linked"
                                else -> "Local Shell"
                            }
                            val badgeColor = if (isTermuxInstalled || backend == ExecutionBackend.SOCKET_BRIDGE) greenAccent else Color(0xFFABB2BF)
                            val badgeBg = if (isTermuxInstalled || backend == ExecutionBackend.SOCKET_BRIDGE)
                                greenAccent.copy(alpha = 0.18f)
                            else
                                Color(0xFF5C6370).copy(alpha = 0.18f)

                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = badgeBg,
                                modifier = Modifier.clickable {
                                    terminalSession.execute("/backend")
                                }
                            ) {
                                Text(
                                    text = badgeText,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = badgeColor,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Clip,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }
                        // Subtitle: workspace path + file name
                        Text(
                            text = "~/workspace • ${activeTab.fileName}",
                            fontSize = 10.sp,
                            color = Color(0xFF768390),
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // ▶ Run File
                    TextButton(
                        onClick = {
                            terminalSession.runEditorFile(
                                fileName = activeTab.fileName,
                                code = activeTab.state.text,
                                language = activeTab.language
                            )
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = greenAccent),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("Run", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    // Open in Termux App (only if installed)
                    if (isTermuxInstalled) {
                        IconButton(
                            onClick = {
                                val cmd = TermuxBridge.getCompileAndRunCommand(
                                    activeTab.fileName, activeTab.language
                                )
                                TermuxBridge.runInTermux(
                                    context = context,
                                    command = "cd \"${terminalSession.workspaceDir.absolutePath}\" && $cmd",
                                    workDir = terminalSession.workspaceDir.absolutePath
                                )
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.OpenInNew,
                                contentDescription = "Open in Termux",
                                tint = cyanAccent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Clear
                    IconButton(
                        onClick = { terminalSession.clear() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = "Clear",
                            tint = Color(0xFF768390),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Close
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close Terminal",
                            tint = Color(0xFF768390),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // ── 2. Output Area ────────────────────────────────────────────────
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                items(lines, key = { it.id }) { line ->
                    when {
                        line.isCommand -> Text(
                            text = line.text,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = cyanAccent,
                            lineHeight = 18.sp
                        )
                        line.isError -> Text(
                            text = AnsiParser.parse(line.text, defaultColor = redAccent),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                        else -> Text(
                            text = AnsiParser.parse(line.text, defaultColor = textColor),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // ── 3. Extra Keys Row ─────────────────────────────────────────────
            Surface(color = keyRowBg, modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // History navigation — most used, so first
                    TerminalKey("↑", keyBg, cyanAccent) {
                        terminalSession.historyPrevious()?.let { inputCommand = it }
                    }
                    TerminalKey("↓", keyBg, cyanAccent) {
                        terminalSession.historyNext()?.let { inputCommand = it }
                    }
                    TerminalKey("TAB", keyBg, textColor) { terminalSession.sendTab() }

                    // Shell symbols
                    TerminalKey("/",  keyBg, textColor) { inputCommand += "/" }
                    TerminalKey("~",  keyBg, textColor) { inputCommand += "~" }
                    TerminalKey("-",  keyBg, textColor) { inputCommand += "-" }
                    TerminalKey("|",  keyBg, textColor) { inputCommand += "|" }
                    TerminalKey("&",  keyBg, textColor) { inputCommand += "&" }
                    TerminalKey(";",  keyBg, textColor) { inputCommand += ";" }
                    TerminalKey("$",  keyBg, textColor) { inputCommand += "$" }
                    TerminalKey(">",  keyBg, textColor) { inputCommand += ">" }
                    TerminalKey("\"", keyBg, textColor) { inputCommand += "\"" }
                    TerminalKey("'",  keyBg, textColor) { inputCommand += "'" }

                    // Signals
                    TerminalKey("^C", Color(0xFFF85149).copy(alpha = 0.18f), redAccent) {
                        terminalSession.sendCtrlC()
                    }
                    TerminalKey("^D", keyBg, textColor) { terminalSession.sendCtrlD() }
                    TerminalKey("ESC", keyBg, textColor) { terminalSession.sendRawInput("\u001B") }
                    TerminalKey("CLEAR", keyBg, textColor) { terminalSession.clear() }
                    TerminalKey("RESTART", keyBg, Color(0xFFD29922)) { terminalSession.restart() }
                }
            }

            // ── 4. Command Input Row ──────────────────────────────────────────
            Surface(
                color = inputRowBg,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 10.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // $ prompt label
                    Text(
                        text = "$ ",
                        color = greenAccent,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )

                    // Command input field
                    BasicTerminalInput(
                        value = inputCommand,
                        onValueChange = { inputCommand = it },
                        onSend = { submitCurrentInput() },
                        focusRequester = focusRequester,
                        textColor = textColor,
                        cursorColor = greenAccent,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    // Send button
                    IconButton(
                        onClick = { submitCurrentInput() },
                        modifier = Modifier
                            .size(42.dp)
                            .background(
                                color = if (inputCommand.isNotBlank())
                                    greenAccent.copy(alpha = 0.2f)
                                else
                                    Color(0xFF2D333B),
                                shape = RoundedCornerShape(10.dp)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send Command",
                            tint = if (inputCommand.isNotBlank()) greenAccent else Color(0xFF444C56),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Minimal text field for the terminal input — no autocorrect, no caps,
 * monospace font, transparent background (we draw our own row background).
 */
@Composable
private fun BasicTerminalInput(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    focusRequester: FocusRequester,
    textColor: Color,
    cursorColor: Color,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(
                text = "enter command…",
                fontSize = 13.sp,
                color = Color(0xFF444C56),
                fontFamily = FontFamily.Monospace
            )
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.None,
            autoCorrectEnabled = false,
            keyboardType = KeyboardType.Ascii,
            imeAction = ImeAction.Send
        ),
        keyboardActions = KeyboardActions(onSend = { onSend() }),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = textColor,
            unfocusedTextColor = textColor,
            focusedBorderColor = Color(0xFF388BFD).copy(alpha = 0.6f),
            unfocusedBorderColor = Color(0xFF30363D),
            cursorColor = cursorColor,
            focusedContainerColor = Color(0xFF0D1117),
            unfocusedContainerColor = Color(0xFF0D1117)
        ),
        textStyle = androidx.compose.ui.text.TextStyle(
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp
        ),
        modifier = modifier
            .focusRequester(focusRequester)
    )
}

/**
 * A single extra-key badge in the terminal's shortcut bar.
 */
@Composable
private fun TerminalKey(
    label: String,
    bg: Color,
    textColor: Color,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(5.dp),
        color = bg,
        modifier = Modifier
            .height(30.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                color = textColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
