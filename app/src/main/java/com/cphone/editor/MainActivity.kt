package com.cphone.editor

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import com.cphone.editor.ui.CPhoneApp
import java.io.BufferedReader
import java.io.InputStreamReader

private val DarkEditorColorScheme = darkColorScheme(
    surface = Color(0xFF21252B),
    onSurface = Color(0xFFF3F4F6),
    surfaceVariant = Color(0xFF2C313C),
    onSurfaceVariant = Color(0xFFD1D5DB),
    background = Color(0xFF1E222A),
    onBackground = Color(0xFFF3F4F6),
    primary = Color(0xFF61AFEF),
    onPrimary = Color.White
)

private val LightEditorColorScheme = lightColorScheme(
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF111827),
    surfaceVariant = Color(0xFFF3F4F6),
    onSurfaceVariant = Color(0xFF4B5563),
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF111827),
    primary = Color(0xFF2563EB),
    onPrimary = Color.White
)

class MainActivity : ComponentActivity() {

    private val viewModel: EditorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        handleIntent(intent)

        setContent {
            val settings by viewModel.settings.collectAsState()
            val isDark = settings.isDarkMode

            SideEffect {
                val ctrl = WindowCompat.getInsetsController(window, window.decorView)
                ctrl.isAppearanceLightStatusBars = !isDark
                ctrl.isAppearanceLightNavigationBars = !isDark
            }

            MaterialTheme(
                colorScheme = if (isDark) DarkEditorColorScheme else LightEditorColorScheme
            ) {
                CPhoneApp(viewModel = viewModel)
            }
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
