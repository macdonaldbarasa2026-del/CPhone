package com.cphone.editor

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import com.cphone.editor.ui.CPhoneApp
import java.io.BufferedReader
import java.io.InputStreamReader

class MainActivity : ComponentActivity() {

    private val viewModel: EditorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Edge-to-edge: our Compose layout draws behind status bar & nav bar.
        // The Compose layer then uses statusBarsPadding / navigationBarsPadding
        // to move content into safe areas.
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        handleIntent(intent)

        setContent {
            val settings by viewModel.settings.collectAsState()
            val isDark = settings.isDarkMode

            // Keep status bar / nav bar icon colours in sync with theme
            SideEffect {
                val ctrl = WindowCompat.getInsetsController(window, window.decorView)
                ctrl.isAppearanceLightStatusBars = !isDark
                ctrl.isAppearanceLightNavigationBars = !isDark
            }

            CPhoneApp(viewModel = viewModel)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        val uri: Uri = intent.data ?: return
        if (intent.action != Intent.ACTION_VIEW && intent.action != Intent.ACTION_EDIT) return

        try {
            val cr = contentResolver
            val content = cr.openInputStream(uri)?.use {
                BufferedReader(InputStreamReader(it)).readText()
            } ?: return

            var name = uri.lastPathSegment ?: "file"
            cr.query(uri, null, null, null, null)?.use { c ->
                val i = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (i != -1 && c.moveToFirst()) name = c.getString(i)
            }

            viewModel.openNewTab(name, content, uri)
        } catch (_: Exception) { /* keep default tab on error */ }
    }
}
