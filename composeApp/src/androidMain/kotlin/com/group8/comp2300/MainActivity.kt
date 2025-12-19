package com.group8.comp2300

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewDynamicColors
import androidx.compose.ui.tooling.preview.PreviewScreenSizes

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent { AppTheme { App() } }
    }
}

/* ------------------------------------------------------------------
 * Theme resolution
 * ------------------------------------------------------------------ */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AppTheme(content: @Composable () -> Unit) {
    val colorScheme = rememberAppColorScheme()
    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        motionScheme = MotionScheme.expressive(),
        content = content,
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun rememberAppColorScheme(): ColorScheme {
    val dark = isSystemInDarkTheme()
    val dynamic = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val context = LocalContext.current

    return when {
        dynamic && dark -> dynamicDarkColorScheme(context)
        dynamic && !dark -> dynamicLightColorScheme(context)
        dark -> darkColorScheme(primary = Color(0xFF66ffc7))
        else -> expressiveLightColorScheme()
    }
}

/* ------------------------------------------------------------------
 * Previews (Needs to be here because Previews in commonMain is bugged)
 * ------------------------------------------------------------------ */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@PreviewDynamicColors
@PreviewScreenSizes
@Preview
@Composable
private fun MainAppPreview() = AppTheme { PreviewMainApp() }

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@PreviewDynamicColors
@PreviewScreenSizes
@Preview
@Composable
private fun PreviewMainAppPreview() = AppTheme { PreviewNavigationTabs() }
