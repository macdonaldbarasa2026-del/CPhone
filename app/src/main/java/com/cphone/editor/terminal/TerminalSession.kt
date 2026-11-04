package com.cphone.editor.terminal

import android.content.Context
import com.cphone.editor.model.SupportedLanguage
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.*
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.atomic.AtomicBoolean

data class TerminalLine(
    val id: Long = System.nanoTime(),
    val text: String,
    val isCommand: Boolean = false,
    val isError: Boolean = false
)

enum class ExecutionBackend(val displayName: String) {
    AUTO("Auto (Termux)"),
    DIRECT_PROCESS("Direct Shell"),
    TERMUX_RUN_COMMAND("Termux Service"),
    SOCKET_BRIDGE("Socket Bridge (127.0.0.1)")
}

/**
 * Embedded Interactive Terminal Session engine for CPhone IDE.
 * Runs an interactive Linux shell with full Termux PATH & environment integration,
 * direct process execution, Termux RUN_COMMAND intent service bridge,
 * TCP socket connection to Termux server (127.0.0.1), and editor file synchronization.
 */
class TerminalSession(private val context: Context) {

    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var process: Process? = null
    private var stdinWriter: BufferedWriter? = null
    private val isRunning = AtomicBoolean(false)
    private var isDirectTermuxProcess = false

    // Socket Bridge support (127.0.0.1:8022 or custom port)
    private var socket: Socket? = null
    private var socketWriter: BufferedWriter? = null
    private var socketJob: Job? = null

    // Execution backend selection
    private val _currentBackend = MutableStateFlow(ExecutionBackend.AUTO)
    val currentBackend: StateFlow<ExecutionBackend> = _currentBackend.asStateFlow()

    // Synced workspace folder
    val workspaceDir: File by lazy {
        File(context.filesDir, "workspace").apply {
            if (!exists()) mkdirs()
        }
    }

    private val _lines = MutableStateFlow<List<TerminalLine>>(emptyList())
    val lines: StateFlow<List<TerminalLine>> = _lines.asStateFlow()

    private val _currentWorkDir = MutableStateFlow(TermuxBridge.TERMUX_HOME)
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
            var pb: ProcessBuilder
            var proc: Process

            try {
                // Primary: target Termux bash/zsh with full environment
                pb = ProcessBuilder(shellPath)
                configureProcessEnvironment(pb, shellPath)
                proc = pb.start()
                isDirectTermuxProcess = true
            } catch (e: Exception) {
                // Secondary: fall back to system shell for local commands while bridging Termux via RUN_COMMAND
                val fallbackShell = TermuxBridge.getFallbackShellPath()
                pb = ProcessBuilder(fallbackShell)
                configureProcessEnvironment(pb, fallbackShell)
                proc = pb.start()
                isDirectTermuxProcess = false
            }

            process = proc
            stdinWriter = BufferedWriter(OutputStreamWriter(proc.outputStream, Charsets.UTF_8))
            isRunning.set(true)

            val activeShell = if (isDirectTermuxProcess) shellPath else TermuxBridge.getFallbackShellPath()
            appendSystemLine("⚡ CPhone Linux Shell started ($activeShell)")
            appendSystemLine("📂 Home: ${TermuxBridge.TERMUX_HOME} | Prefix: ${TermuxBridge.TERMUX_PREFIX}")
            appendSystemLine("🛠️ PATH: ${TermuxBridge.TERMUX_BIN}:${TermuxBridge.TERMUX_APPLETS}")

            val termuxInstalled = TermuxBridge.isTermuxInstalled(context)
            if (termuxInstalled) {
                if (isDirectTermuxProcess) {
                    appendSystemLine("🔌 Termux Bridge: DIRECT (Native Termux binaries active)")
                } else {
                    appendSystemLine("🔌 Termux Bridge: LINKED (com.termux.RUN_COMMAND intent bridge active for pkg/apt/git/python)")
                }
            } else {
                appendSystemLine("ℹ️ Termux Bridge: Standalone Android Shell (Install Termux for clang/python/pkg/git)")
            }

            // Export environment variables inside shell session
            val initScript = buildString {
                append("export HOME=\"${TermuxBridge.TERMUX_HOME}\"\n")
                append("export PREFIX=\"${TermuxBridge.TERMUX_PREFIX}\"\n")
                append("export PATH=\"${TermuxBridge.TERMUX_BIN}:${TermuxBridge.TERMUX_APPLETS}:\$PATH\"\n")
                append("export LD_LIBRARY_PATH=\"${TermuxBridge.TERMUX_LIB}\"\n")
                append("export TMPDIR=\"${TermuxBridge.TERMUX_TMP}\"\n")
                append("export SHELL=\"$shellPath\"\n")
                append("export TERM=\"xterm-256color\"\n")
                append("export LANG=\"en_US.UTF-8\"\n")
                append("export COLORTERM=\"truecolor\"\n")
            }
            sendRawCommand(initScript)

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

            // Check if local socket server is running on 127.0.0.1:8022
            coroutineScope.launch {
                delay(300)
                if (TermuxBridge.checkLocalSocketServer("127.0.0.1", 8022, timeoutMs = 300)) {
                    appendSystemLine("💡 Termux SSH/Socket server detected at 127.0.0.1:8022. Type '/socket' to connect directly.")
                }
            }

        } catch (e: Exception) {
            appendSystemLine("❌ Failed to spawn shell: ${e.message}")
        }
    }

    private fun configureProcessEnvironment(pb: ProcessBuilder, shellPath: String) {
        val termuxHome = File(TermuxBridge.TERMUX_HOME)
        if (termuxHome.exists() && termuxHome.isDirectory && termuxHome.canRead()) {
            pb.directory(termuxHome)
            _currentWorkDir.value = termuxHome.absolutePath
        } else {
            pb.directory(workspaceDir)
            _currentWorkDir.value = workspaceDir.absolutePath
        }

        val env = pb.environment()
        // Inherit all host environment variables
        try {
            System.getenv().forEach { (k, v) ->
                env[k] = v
            }
        } catch (_: Exception) {}

        val existingPath = env["PATH"] ?: "/system/bin:/system/xbin"
        env["HOME"] = TermuxBridge.TERMUX_HOME
        env["PREFIX"] = TermuxBridge.TERMUX_PREFIX
        env["PATH"] = "${TermuxBridge.TERMUX_BIN}:${TermuxBridge.TERMUX_APPLETS}:$existingPath"
        env["LD_LIBRARY_PATH"] = TermuxBridge.TERMUX_LIB
        env["TMPDIR"] = TermuxBridge.TERMUX_TMP
        env["SHELL"] = shellPath
        env["TERM"] = "xterm-256color"
        env["COLORTERM"] = "truecolor"
        env["LANG"] = "en_US.UTF-8"
        env["CPHONE_IDE"] = "1"
        pb.redirectErrorStream(false)
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

    fun setBackend(backend: ExecutionBackend) {
        _currentBackend.value = backend
        appendSystemLine("🔄 Switched execution backend to: ${backend.displayName}")
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

        // Handle slash commands
        if (cmd.startsWith("/")) {
            handleSlashCommand(cmd)
            return
        }

        appendCommandLine(cmd)
        _isBusy.value = true

        val activeBackend = _currentBackend.value
        when {
            // 1. Socket Bridge mode
            activeBackend == ExecutionBackend.SOCKET_BRIDGE && socketWriter != null -> {
                coroutineScope.launch {
                    try {
                        socketWriter?.write("$cmd\n")
                        socketWriter?.flush()
                    } catch (e: Exception) {
                        appendLine("Socket write error: ${e.message}", isError = true)
                    } finally {
                        delay(250)
                        _isBusy.value = false
                    }
                }
            }

            // 2. Direct Process execution (when direct process is explicitly set or natively running)
            activeBackend == ExecutionBackend.DIRECT_PROCESS || (activeBackend == ExecutionBackend.AUTO && isDirectTermuxProcess) -> {
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

            // 3. Termux RUN_COMMAND Intent service (for pkg/apt/git/python when direct exec cross-sandbox is restricted)
            else -> {
                coroutineScope.launch {
                    val sent = TermuxBridge.executeViaRunCommand(
                        context = context,
                        command = cmd,
                        workDir = _currentWorkDir.value,
                        background = true
                    ) { stdout, stderr, exitCode, errmsg ->
                        if (!stdout.isNullOrEmpty()) {
                            stdout.trimEnd().split('\n').forEach { line ->
                                appendLine(line, isError = false)
                            }
                        }
                        if (!stderr.isNullOrEmpty()) {
                            stderr.trimEnd().split('\n').forEach { line ->
                                appendLine(line, isError = true)
                            }
                        }
                        if (exitCode != 0 && errmsg != null) {
                            appendLine("Process exited with code $exitCode ($errmsg)", isError = true)
                        }
                        _isBusy.value = false
                    }

                    if (!sent) {
                        // Fall back to direct stdinWriter if RUN_COMMAND service dispatch failed
                        sendRawCommand("$cmd\n")
                        delay(250)
                        _isBusy.value = false
                    }
                }
            }
        }
    }

    private fun handleSlashCommand(cmd: String) {
        val parts = cmd.split(" ", limit = 2)
        val action = parts[0].lowercase()
        val arg = parts.getOrNull(1)?.trim() ?: ""

        when (action) {
            "/help" -> {
                appendSystemLine("📖 CPhone Terminal Commands:")
                appendSystemLine("  /backend [auto|direct|service|socket]  — Switch execution backend")
                appendSystemLine("  /socket [port]                         — Connect to 127.0.0.1 socket (default: 8022)")
                appendSystemLine("  /disconnect                            — Disconnect socket bridge")
                appendSystemLine("  /env                                   — Show Termux environment variables")
                appendSystemLine("  /termux <cmd>                          — Run command in Termux foreground")
                appendSystemLine("  clear / reset                          — Clear or restart session")
            }
            "/backend" -> {
                when (arg.lowercase()) {
                    "auto" -> setBackend(ExecutionBackend.AUTO)
                    "direct" -> setBackend(ExecutionBackend.DIRECT_PROCESS)
                    "service", "intent" -> setBackend(ExecutionBackend.TERMUX_RUN_COMMAND)
                    "socket" -> connectSocket("127.0.0.1", 8022)
                    else -> appendSystemLine("Current Backend: ${_currentBackend.value.displayName}. Options: auto, direct, service, socket")
                }
            }
            "/socket", "/connect" -> {
                val port = arg.toIntOrNull() ?: 8022
                connectSocket("127.0.0.1", port)
            }
            "/disconnect" -> {
                disconnectSocket()
            }
            "/env" -> {
                appendSystemLine("📦 Termux Shell Environment:")
                appendSystemLine("  HOME = ${TermuxBridge.TERMUX_HOME}")
                appendSystemLine("  PREFIX = ${TermuxBridge.TERMUX_PREFIX}")
                appendSystemLine("  PATH = ${TermuxBridge.TERMUX_BIN}:${TermuxBridge.TERMUX_APPLETS}")
                appendSystemLine("  LD_LIBRARY_PATH = ${TermuxBridge.TERMUX_LIB}")
                appendSystemLine("  SHELL = ${TermuxBridge.resolveShellPath()}")
                appendSystemLine("  Active Backend = ${_currentBackend.value.displayName}")
                appendSystemLine("  Direct Process Native = $isDirectTermuxProcess")
            }
            "/termux" -> {
                if (arg.isEmpty()) {
                    appendSystemLine("Usage: /termux <command>")
                } else {
                    appendCommandLine("/termux $arg")
                    TermuxBridge.runInTermux(context, arg, _currentWorkDir.value, background = false)
                    appendSystemLine("🚀 Dispatched to Termux app foreground session")
                }
            }
            else -> {
                appendSystemLine("Unknown terminal command: $cmd. Type '/help' for available commands.")
            }
        }
    }

    fun connectSocket(host: String = "127.0.0.1", port: Int = 8022) {
        coroutineScope.launch {
            try {
                appendSystemLine("🔌 Connecting to Termux socket at $host:$port...")
                val sock = Socket()
                sock.connect(InetSocketAddress(host, port), 3000)
                socket = sock
                val writer = BufferedWriter(OutputStreamWriter(sock.getOutputStream(), Charsets.UTF_8))
                socketWriter = writer
                _currentBackend.value = ExecutionBackend.SOCKET_BRIDGE
                appendSystemLine("✅ Connected to Termux server on $host:$port")

                socketJob = launch(Dispatchers.IO) {
                    val reader = BufferedReader(InputStreamReader(sock.getInputStream(), Charsets.UTF_8))
                    var line: String? = null
                    try {
                        while (reader.readLine().also { line = it } != null) {
                            line?.let { appendLine(it, isError = false) }
                        }
                    } catch (_: Exception) {}
                    appendSystemLine("🔌 Socket connection closed by remote host")
                    disconnectSocket(notify = false)
                }
            } catch (e: Exception) {
                appendLine("❌ Socket connection to $host:$port failed: ${e.message}", isError = true)
                appendSystemLine("ℹ️ Ensure SSH or socket server is running in Termux (e.g. run 'sshd' in Termux on port 8022)")
            }
        }
    }

    fun disconnectSocket(notify: Boolean = true) {
        socketJob?.cancel()
        socketJob = null
        try {
            socketWriter?.close()
        } catch (_: Exception) {}
        socketWriter = null
        try {
            socket?.close()
        } catch (_: Exception) {}
        socket = null
        _currentBackend.value = ExecutionBackend.AUTO
        if (notify) appendSystemLine("🔌 Disconnected from socket server")
    }

    fun sendRawInput(text: String) {
        coroutineScope.launch {
            try {
                if (_currentBackend.value == ExecutionBackend.SOCKET_BRIDGE && socketWriter != null) {
                    socketWriter?.write(text)
                    socketWriter?.flush()
                } else {
                    stdinWriter?.write(text)
                    stdinWriter?.flush()
                }
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
                if (_currentBackend.value == ExecutionBackend.SOCKET_BRIDGE && socketWriter != null) {
                    socketWriter?.write("\u0003")
                    socketWriter?.flush()
                } else {
                    stdinWriter?.write("\u0003")
                    stdinWriter?.flush()
                    delay(100)
                    stdinWriter?.write("\n")
                    stdinWriter?.flush()
                }
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
        disconnectSocket(notify = false)
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
