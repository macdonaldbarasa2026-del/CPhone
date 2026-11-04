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
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.cphone.editor.model.EditorTab
import com.cphone.editor.terminal.AnsiParser
import com.cphone.editor.terminal.TerminalSession
import com.cphone.editor.terminal.TermuxBridge

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

    val isTermuxInstalled = remember { TermuxBridge.isTermuxInstalled(context) }

    // Auto-scroll to bottom when new terminal output arrives
    LaunchedEffect(lines.size) {
        if (lines.isNotEmpty()) {
            listState.animateScrollToItem(lines.size - 1)
        }
    }

    val terminalBg = Color(0xFF0F111A)
    val headerBg = Color(0xFF161824)
    val keyBg = Color(0xFF222638)
    val textColor = Color(0xFFE5E9F0)
    val greenAccent = Color(0xFF4CAF50)
    val cyanAccent = Color(0xFF61AFEF)

    fun submitCurrentInput() {
        val cmd = inputCommand.trim()
        if (cmd.isNotEmpty()) {
            terminalSession.execute(cmd)
            inputCommand = ""
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            color = terminalBg
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                // ── Terminal Header Bar ───────────────────────────────────────────
                Surface(
                    color = headerBg,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Terminal Icon + Pulse Dot
                        Box(contentAlignment = Alignment.Center) {
                            Surface(
                                shape = CircleShape,
                                color = greenAccent.copy(alpha = 0.2f),
                                modifier = Modifier.size(28.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .background(if (isBusy) Color(0xFFE5C07B) else greenAccent, CircleShape)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "CPhone Terminal",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (isTermuxInstalled) greenAccent.copy(alpha = 0.2f) else Color(0xFF5C6370).copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = if (isTermuxInstalled) "Termux Linked" else "Local Shell",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isTermuxInstalled) greenAccent else Color(0xFFABB2BF),
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Text(
                                text = "~/workspace • ${activeTab.fileName}",
                                fontSize = 11.sp,
                                color = Color(0xFF7E8494),
                                maxLines = 1
                            )
                        }

                        // 1-Tap Quick Run active editor file
                        FilledTonalButton(
                            onClick = {
                                terminalSession.runEditorFile(
                                    fileName = activeTab.fileName,
                                    code = activeTab.state.text,
                                    language = activeTab.language
                                )
                            },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = greenAccent.copy(alpha = 0.2f),
                                contentColor = greenAccent
                            ),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Run File", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        // Termux App Bridge Button
                        if (isTermuxInstalled) {
                            IconButton(
                                onClick = {
                                    val cmd = TermuxBridge.getCompileAndRunCommand(activeTab.fileName, activeTab.language)
                                    TermuxBridge.runInTermux(
                                        context = context,
                                        command = "cd \"${terminalSession.workspaceDir.absolutePath}\" && $cmd",
                                        workDir = terminalSession.workspaceDir.absolutePath
                                    )
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.OpenInNew,
                                    contentDescription = "Run in Termux App",
                                    tint = cyanAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Clear Button
                        IconButton(
                            onClick = { terminalSession.clear() },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Clear", tint = textColor, modifier = Modifier.size(18.dp))
                        }

                        // Restart Button
                        IconButton(
                            onClick = { terminalSession.restart() },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Restart Shell", tint = textColor, modifier = Modifier.size(18.dp))
                        }

                        // Close Button
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = textColor, modifier = Modifier.size(20.dp))
                        }
                    }
                }

                // ── Terminal Scrollable Canvas ────────────────────────────────────
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    SelectionContainer {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            items(lines, key = { it.id }) { line ->
                                when {
                                    line.isCommand -> {
                                        Text(
                                            text = line.text,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = cyanAccent,
                                            lineHeight = 17.sp
                                        )
                                    }
                                    line.isError -> {
                                        Text(
                                            text = AnsiParser.parse(line.text, defaultColor = Color(0xFFE06C75)),
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 12.5.sp,
                                            lineHeight = 17.sp
                                        )
                                    }
                                    else -> {
                                        Text(
                                            text = AnsiParser.parse(line.text, defaultColor = textColor),
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 12.5.sp,
                                            lineHeight = 17.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Termux Extra Keys Row (ESC, TAB, CTRL, ALT, |, /, -, ~, ^C, ^D, ↑, ↓) ─
                Surface(
                    color = headerBg,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TerminalKeyBadge("ESC", keyBg, textColor) { terminalSession.sendRawInput("\u001B") }
                        TerminalKeyBadge("TAB", keyBg, textColor) { terminalSession.sendTab() }
                        TerminalKeyBadge("CTRL", keyBg, textColor) { inputCommand += "^" }
                        TerminalKeyBadge("ALT", keyBg, textColor) {}

                        // Special Shell Symbols
                        TerminalKeyBadge("|", keyBg, textColor) { inputCommand += "|" }
                        TerminalKeyBadge("/", keyBg, textColor) { inputCommand += "/" }
                        TerminalKeyBadge("-", keyBg, textColor) { inputCommand += "-" }
                        TerminalKeyBadge("~", keyBg, textColor) { inputCommand += "~" }
                        TerminalKeyBadge("$", keyBg, textColor) { inputCommand += "$" }
                        TerminalKeyBadge("&", keyBg, textColor) { inputCommand += "&" }
                        TerminalKeyBadge(";", keyBg, textColor) { inputCommand += ";" }

                        // Process Control Signals
                        TerminalKeyBadge(
                            label = "^C",
                            bg = Color(0xFFE06C75).copy(alpha = 0.25f),
                            textColor = Color(0xFFE06C75)
                        ) {
                            terminalSession.sendCtrlC()
                        }

                        TerminalKeyBadge("^D", keyBg, textColor) { terminalSession.sendCtrlD() }

                        // Command History Navigation
                        TerminalKeyBadge("↑", keyBg, cyanAccent) {
                            terminalSession.historyPrevious()?.let { inputCommand = it }
                        }
                        TerminalKeyBadge("↓", keyBg, cyanAccent) {
                            terminalSession.historyNext()?.let { inputCommand = it }
                        }

                        TerminalKeyBadge("CLEAR", keyBg, textColor) { terminalSession.clear() }
                    }
                }

                // ── Interactive Command Input Row ─────────────────────────────────
                Surface(
                    color = headerBg,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$ ",
                            color = greenAccent,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )

                        OutlinedTextField(
                            value = inputCommand,
                            onValueChange = { inputCommand = it },
                            placeholder = {
                                Text(
                                    text = "type bash/termux command (e.g. python3 main.py)...",
                                    fontSize = 12.sp,
                                    color = Color(0xFF5C6370),
                                    fontFamily = FontFamily.Monospace
                                )
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = { submitCurrentInput() }),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = textColor,
                                unfocusedTextColor = textColor,
                                focusedBorderColor = greenAccent.copy(alpha = 0.6f),
                                unfocusedBorderColor = Color(0xFF3E4452),
                                cursorColor = greenAccent
                            ),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        IconButton(
                            onClick = { submitCurrentInput() },
                            modifier = Modifier
                                .size(40.dp)
                                .background(greenAccent.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Execute Command",
                                tint = greenAccent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TerminalKeyBadge(
    label: String,
    bg: Color,
    textColor: Color,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = bg,
        modifier = Modifier
            .height(28.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                color = textColor,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
