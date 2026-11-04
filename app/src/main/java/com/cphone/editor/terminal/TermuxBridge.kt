package com.cphone.editor.terminal

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.cphone.editor.model.SupportedLanguage
import java.io.File

/**
 * Termux Integration Bridge for CPhone IDE.
 * Allows bi-directional syncing, binary detection, and remote execution via Termux RUN_COMMAND.
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

    const val TERMUX_PREFIX = "/data/data/com.termux/files/usr"
    const val TERMUX_BIN = "$TERMUX_PREFIX/bin"
    const val TERMUX_HOME = "/data/data/com.termux/files/home"

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
     * Finds the preferred shell binary (Termux bash/sh, or Android /system/bin/sh).
     */
    fun resolveShellPath(): String {
        val candidates = listOf(
            "$TERMUX_BIN/bash",
            "$TERMUX_BIN/login",
            "$TERMUX_BIN/sh",
            "/system/bin/sh",
            "/system/xbin/sh"
        )
        return candidates.firstOrNull { File(it).canExecute() } ?: "/system/bin/sh"
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
     * Dispatches a command directly into Termux via RUN_COMMAND Intent service.
     * Starts an interactive or background session in Termux with the exact working directory.
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
                putExtra(EXTRA_RUN_COMMAND_PATH, "$TERMUX_BIN/bash")
                putExtra(EXTRA_RUN_COMMAND_ARGUMENTS, arrayOf("-c", command))
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
