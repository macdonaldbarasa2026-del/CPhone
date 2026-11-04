package com.cphone.editor.terminal

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Receives execution results from Termux RUN_COMMAND service.
 * When Termux completes a background command dispatched with a PendingIntent,
 * it returns stdout, stderr, exitCode, errCode, and errmsg.
 */
class TermuxCommandResultReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent == null) return
        val stdout = intent.getStringExtra("stdout")
        val stderr = intent.getStringExtra("stderr")
        val exitCode = intent.getIntExtra("exitCode", 0)
        val errCode = intent.getIntExtra("errCode", 0)
        val errmsg = intent.getStringExtra("errmsg")
        val commandId = intent.getStringExtra("command_id") ?: ""

        TermuxBridge.dispatchResult(
            commandId = commandId,
            stdout = stdout,
            stderr = stderr,
            exitCode = exitCode,
            errCode = errCode,
            errmsg = errmsg
        )
    }
}
