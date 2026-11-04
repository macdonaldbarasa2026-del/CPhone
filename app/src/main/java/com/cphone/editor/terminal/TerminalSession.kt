package com.cphone.editor.terminal

import android.content.Context
import com.cphone.editor.model.SupportedLanguage
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.*
import java.util.concurrent.atomic.AtomicBoolean

data class TerminalLine(
    val id: Long = System.nanoTime(),
    val text: String,
    val isCommand: Boolean = false,
    val isError: Boolean = false
)

/**
 * Embedded Interactive Terminal Session engine for CPhone IDE.
 * Runs an interactive Linux shell with full Termux PATH & environment integration,
 * ANSI output streaming, command history, and editor file synchronization.
 */
class TerminalSession(private val context: Context) {

    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var process: Process? = null
    private var stdinWriter: BufferedWriter? = null
    private val isRunning = AtomicBoolean(false)

    // Synced workspace folder
    val workspaceDir: File by lazy {
        File(context.filesDir, "workspace").apply {
            if (!exists()) mkdirs()
        }
    }

    private val _lines = MutableStateFlow<List<TerminalLine>>(emptyList())
    val lines: StateFlow<List<TerminalLine>> = _lines.asStateFlow()

    private val _currentWorkDir = MutableStateFlow(workspaceDir.absolutePath)
    val currentWorkDir: StateFlow<String> = _currentWorkDir.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    // Command History (Up / Down arrow support)
    private val commandHistory = mutableListOf<String>()
    private var historyIndex = -1

    init {
        startShell()
    }

    @Synchronized
    fun startShell() {
        stopShell()
        try {
            val shellPath = TermuxBridge.resolveShellPath()
            val pb = ProcessBuilder(shellPath)
            pb.directory(workspaceDir)

            val env = pb.environment()
            val existingPath = env["PATH"] ?: "/system/bin:/system/xbin"
            env["PATH"] = "${TermuxBridge.TERMUX_BIN}:${TermuxBridge.TERMUX_PREFIX}/bin/applets:$existingPath"
            env["LD_LIBRARY_PATH"] = "${TermuxBridge.TERMUX_PREFIX}/lib"
            env["HOME"] = workspaceDir.absolutePath
            env["TERM"] = "xterm-256color"
            env["LANG"] = "en_US.UTF-8"
            env["CPHONE_IDE"] = "1"

            pb.redirectErrorStream(false)
            val proc = pb.start()
            process = proc
            stdinWriter = BufferedWriter(OutputStreamWriter(proc.outputStream, Charsets.UTF_8))
            isRunning.set(true)

            appendSystemLine("⚡ CPhone Linux Shell started ($shellPath)")
            appendSystemLine("📂 Synced Workspace: ${workspaceDir.name}/")
            if (TermuxBridge.isTermuxInstalled(context)) {
                appendSystemLine("🔌 Termux Bridge: CONNECTED (Full Linux Toolchain Ready)")
            } else {
                appendSystemLine("ℹ️ Termux Bridge: Standalone Android Shell (Install Termux for clang/python/rust)")
            }

            // Read stdout
            coroutineScope.launch {
                val reader = BufferedReader(InputStreamReader(proc.inputStream, Charsets.UTF_8))
                var line: String? = null
                while (isRunning.get() && reader.readLine().also { line = it } != null) {
                    line?.let { appendLine(it, isError = false) }
                }
            }

            // Read stderr
            coroutineScope.launch {
                val errorReader = BufferedReader(InputStreamReader(proc.errorStream, Charsets.UTF_8))
                var errLine: String? = null
                while (isRunning.get() && errorReader.readLine().also { errLine = it } != null) {
                    errLine?.let { appendLine(it, isError = true) }
                }
            }

            // Send initial environment setup
            sendRawCommand("cd \"${workspaceDir.absolutePath}\"\n")

        } catch (e: Exception) {
            appendSystemLine("❌ Failed to spawn shell: ${e.message}")
        }
    }

    @Synchronized
    fun stopShell() {
        isRunning.set(false)
        try {
            stdinWriter?.close()
        } catch (_: Exception) {}
        stdinWriter = null

        process?.destroy()
        process = null
    }

    fun execute(rawCommand: String) {
        val cmd = rawCommand.trim()
        if (cmd.isEmpty()) return

        // Update history
        commandHistory.remove(cmd)
        commandHistory.add(cmd)
        historyIndex = commandHistory.size

        if (cmd == "clear") {
            clear()
            return
        }

        if (cmd == "exit" || cmd == "reset") {
            appendCommandLine(cmd)
            restart()
            return
        }

        appendCommandLine(cmd)
        _isBusy.value = true

        coroutineScope.launch {
            try {
                sendRawCommand("$cmd\n")
            } catch (e: Exception) {
                appendLine("Error executing command: ${e.message}", isError = true)
            } finally {
                delay(250)
                _isBusy.value = false
            }
        }
    }

    fun sendRawInput(text: String) {
        coroutineScope.launch {
            try {
                stdinWriter?.write(text)
                stdinWriter?.flush()
            } catch (_: Exception) {}
        }
    }

    private fun sendRawCommand(cmd: String) {
        stdinWriter?.let { writer ->
            writer.write(cmd)
            writer.flush()
        }
    }

    fun sendCtrlC() {
        appendLine("^C", isError = false)
        coroutineScope.launch {
            try {
                stdinWriter?.write("\u0003")
                stdinWriter?.flush()
                delay(100)
                // If stuck, kick subshell with Enter
                stdinWriter?.write("\n")
                stdinWriter?.flush()
            } catch (_: Exception) {
                restart()
            }
        }
    }

    fun sendCtrlD() {
        sendRawInput("\u0004")
    }

    fun sendTab() {
        sendRawInput("\t")
    }

    fun historyPrevious(): String? {
        if (commandHistory.isEmpty()) return null
        if (historyIndex > 0) {
            historyIndex--
            return commandHistory[historyIndex]
        }
        return commandHistory.firstOrNull()
    }

    fun historyNext(): String? {
        if (commandHistory.isEmpty()) return null
        if (historyIndex < commandHistory.size - 1) {
            historyIndex++
            return commandHistory[historyIndex]
        }
        historyIndex = commandHistory.size
        return ""
    }

    /**
     * Synchronizes the currently active editor file into the workspace directory
     * and runs the proper compilation/run command.
     */
    fun runEditorFile(
        fileName: String,
        code: String,
        language: SupportedLanguage
    ) {
        val targetFile = File(workspaceDir, fileName)
        try {
            targetFile.writeText(code, Charsets.UTF_8)
            appendSystemLine("💾 Synced '$fileName' to workspace (${targetFile.length()} bytes)")
        } catch (e: Exception) {
            appendLine("Failed to sync file: ${e.message}", isError = true)
            return
        }

        val runCommand = TermuxBridge.getCompileAndRunCommand(fileName, language)
        execute(runCommand)
    }

    fun clear() {
        _lines.value = emptyList()
    }

    fun restart() {
        appendSystemLine("🔄 Restarting CPhone Terminal Session...")
        startShell()
    }

    private fun appendCommandLine(cmd: String) {
        val prompt = "cphone@termux $ $cmd"
        appendLine(prompt, isCommand = true)
    }

    private fun appendSystemLine(msg: String) {
        appendLine(msg, isCommand = false, isError = false)
    }

    private fun appendLine(text: String, isCommand: Boolean = false, isError: Boolean = false) {
        val current = _lines.value.toMutableList()
        current.add(TerminalLine(text = text, isCommand = isCommand, isError = isError))
        if (current.size > 1200) {
            _lines.value = current.takeLast(1000)
        } else {
            _lines.value = current
        }
    }
}
