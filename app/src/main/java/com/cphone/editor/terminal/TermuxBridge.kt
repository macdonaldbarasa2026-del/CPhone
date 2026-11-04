package com.cphone.editor.terminal

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import com.cphone.editor.model.SupportedLanguage
import java.io.File
import java.net.InetSocketAddress
import java.net.Socket
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Termux Integration Bridge for CPhone IDE.
 * Allows bi-directional syncing, binary detection, direct process execution,
 * socket bridge connection to Termux server (127.0.0.1), and remote execution via Termux RUN_COMMAND.
 */
object TermuxBridge {

    const val TERMUX_PACKAGE = "com.termux"
    const val TERMUX_SERVICE = "com.termux.app.RunCommandService"
    const val ACTION_RUN_COMMAND = "com.termux.RUN_COMMAND"

    const val EXTRA_RUN_COMMAND_PATH = "com.termux.RUN_COMMAND_PATH"
    const val EXTRA_RUN_COMMAND_ARGUMENTS = "com.termux.RUN_COMMAND_ARGUMENTS"
    const val EXTRA_RUN_COMMAND_WORKDIR = "com.termux.RUN_COMMAND_WORKDIR"
    const val EXTRA_RUN_COMMAND_BACKGROUND = "com.termux.RUN_COMMAND_BACKGROUND"
    const val EXTRA_RUN_COMMAND_SESSION_ACTION = "com.termux.RUN_COMMAND_SESSION_ACTION"
    const val EXTRA_RUN_COMMAND_PENDING_INTENT = "com.termux.RUN_COMMAND_PENDING_INTENT"

    const val ACTION_COMMAND_RESULT = "com.cphone.editor.termux.RESULT"

    const val TERMUX_PREFIX = "/data/data/com.termux/files/usr"
    const val TERMUX_BIN = "$TERMUX_PREFIX/bin"
    const val TERMUX_APPLETS = "$TERMUX_BIN/applets"
    const val TERMUX_HOME = "/data/data/com.termux/files/home"
    const val TERMUX_LIB = "$TERMUX_PREFIX/lib"
    const val TERMUX_TMP = "$TERMUX_PREFIX/tmp"

    typealias ResultCallback = (stdout: String?, stderr: String?, exitCode: Int, errmsg: String?) -> Unit
    private val callbacks = ConcurrentHashMap<String, ResultCallback>()
    var globalResultListener: ((commandId: String, stdout: String?, stderr: String?, exitCode: Int, errmsg: String?) -> Unit)? = null

    fun registerCallback(commandId: String, callback: ResultCallback) {
        callbacks[commandId] = callback
    }

    fun dispatchResult(
        commandId: String,
        stdout: String?,
        stderr: String?,
        exitCode: Int,
        errCode: Int,
        errmsg: String?
    ) {
        val errorDetail = if (errCode != 0 && errmsg != null) "Termux Error ($errCode): $errmsg" else errmsg
        callbacks.remove(commandId)?.invoke(stdout, stderr, exitCode, errorDetail)
        globalResultListener?.invoke(commandId, stdout, stderr, exitCode, errorDetail)
    }

    /**
     * Checks if Termux is installed on this Android device.
     */
    fun isTermuxInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo(TERMUX_PACKAGE, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }

    /**
     * Checks if Termux binaries are directly readable/accessible from CPhone sandbox.
     */
    fun isTermuxBinAccessible(): Boolean {
        val binDir = File(TERMUX_BIN)
        return binDir.exists() && binDir.isDirectory && (binDir.canExecute() || binDir.canRead())
    }

    /**
     * Finds the preferred shell binary path (Termux zsh, Termux bash, login, or fallback).
     * Strictly targets Termux binaries first as required.
     */
    fun resolveShellPath(): String {
        val zsh = File("$TERMUX_BIN/zsh")
        if (zsh.exists()) return "$TERMUX_BIN/zsh"

        val bash = File("$TERMUX_BIN/bash")
        if (bash.exists()) return "$TERMUX_BIN/bash"

        // Default directly to Termux bash path for ProcessBuilder
        return "$TERMUX_BIN/bash"
    }

    /**
     * Fallback standard Android shell when direct Termux execution is blocked by sandbox permissions.
     */
    fun getFallbackShellPath(): String {
        val candidates = listOf(
            "/system/bin/sh",
            "/system/xbin/sh",
            "sh"
        )
        return candidates.firstOrNull { File(it).exists() } ?: "/system/bin/sh"
    }

    /**
     * Checks if a local socket/SSH server is listening on Termux's 127.0.0.1.
     */
    fun checkLocalSocketServer(host: String = "127.0.0.1", port: Int = 8022, timeoutMs: Int = 500): Boolean {
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, port), timeoutMs)
                true
            }
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Launches the Termux main application.
     */
    fun openTermux(context: Context): Boolean {
        return try {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(TERMUX_PACKAGE)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                true
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Dispatches a command directly into Termux via RUN_COMMAND Intent service in background,
     * delivering stdout, stderr, and exitCode back via PendingIntent result receiver.
     */
    fun executeViaRunCommand(
        context: Context,
        command: String,
        workDir: String = TERMUX_HOME,
        background: Boolean = true,
        commandId: String = UUID.randomUUID().toString(),
        onResult: ResultCallback? = null
    ): Boolean {
        if (onResult != null) {
            callbacks[commandId] = onResult
        }
        return try {
            val intent = Intent(ACTION_RUN_COMMAND).apply {
                setClassName(TERMUX_PACKAGE, TERMUX_SERVICE)
                putExtra(EXTRA_RUN_COMMAND_PATH, resolveShellPath())
                putExtra(EXTRA_RUN_COMMAND_ARGUMENTS, arrayOf("-l", "-c", command))
                putExtra(EXTRA_RUN_COMMAND_WORKDIR, workDir)
                putExtra(EXTRA_RUN_COMMAND_BACKGROUND, background)
                putExtra(EXTRA_RUN_COMMAND_SESSION_ACTION, if (background) "2" else "0")

                val resultIntent = Intent(context, TermuxCommandResultReceiver::class.java).apply {
                    action = ACTION_COMMAND_RESULT
                    putExtra("command_id", commandId)
                }
                val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                } else {
                    PendingIntent.FLAG_UPDATE_CURRENT
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    commandId.hashCode(),
                    resultIntent,
                    flags
                )
                putExtra(EXTRA_RUN_COMMAND_PENDING_INTENT, pendingIntent)
            }
            context.startService(intent)
            true
        } catch (e: Exception) {
            callbacks.remove(commandId)
            false
        }
    }

    /**
     * Dispatches a command directly into Termux via RUN_COMMAND Intent service (opens session in Termux app).
     */
    fun runInTermux(
        context: Context,
        command: String,
        workDir: String = TERMUX_HOME,
        background: Boolean = false
    ): Boolean {
        return try {
            val intent = Intent(ACTION_RUN_COMMAND).apply {
                setClassName(TERMUX_PACKAGE, TERMUX_SERVICE)
                putExtra(EXTRA_RUN_COMMAND_PATH, resolveShellPath())
                putExtra(EXTRA_RUN_COMMAND_ARGUMENTS, arrayOf("-l", "-c", command))
                putExtra(EXTRA_RUN_COMMAND_WORKDIR, workDir)
                putExtra(EXTRA_RUN_COMMAND_BACKGROUND, background)
                putExtra(EXTRA_RUN_COMMAND_SESSION_ACTION, "0") // 0 = open foreground session
            }
            context.startService(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Generates standard compile & run commands for full Linux/Termux toolchains.
     */
    fun getCompileAndRunCommand(fileName: String, language: SupportedLanguage): String {
        val baseName = fileName.substringBeforeLast('.')
        val escapedName = "\"$fileName\""
        val escapedOut = "\"$baseName.out\""

        return when (language) {
            SupportedLanguage.PYTHON -> "python3 -u $escapedName"
            SupportedLanguage.CPP -> "(g++ -std=c++17 -O2 $escapedName -o $escapedOut 2>&1 || clang++ -std=c++17 $escapedName -o $escapedOut 2>&1) && ./$escapedOut"
            SupportedLanguage.JAVA -> "javac $escapedName && java \"$baseName\""
            SupportedLanguage.KOTLIN -> "kotlinc $escapedName -include-runtime -d \"$baseName.jar\" && java -jar \"$baseName.jar\""
            SupportedLanguage.JAVASCRIPT -> "node $escapedName"
            SupportedLanguage.TYPESCRIPT -> "(ts-node $escapedName || (tsc $escapedName && node \"$baseName.js\"))"
            SupportedLanguage.RUST -> "rustc $escapedName && ./\"$baseName\""
            SupportedLanguage.GO -> "go run $escapedName"
            SupportedLanguage.SHELL, SupportedLanguage.BATCH -> "bash $escapedName"
            SupportedLanguage.PHP -> "php $escapedName"
            SupportedLanguage.SQL -> "sqlite3 < $escapedName"
            SupportedLanguage.HTML -> "python3 -m http.server 8080 || echo 'Serving on http://localhost:8080'"
            else -> "cat $escapedName"
        }
    }
}
