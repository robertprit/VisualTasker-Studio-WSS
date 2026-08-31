package com.visualtasker.wss

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.visualtasker.wss.screens.MainScreen
import com.visualtasker.wss.ui.theme.MultiPanelTheme
import com.visualtasker.wss.workspace.ui.WorkspaceScreen

private enum class StartupScreen {
    WORKSPACE,
    MAIN
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val prefs = remember { getSharedPreferences("panel_ui_options", MODE_PRIVATE) }
            var themeMode by remember { mutableStateOf(prefs.getString("theme_mode", "dark") ?: "dark") }
            var startupScreen by remember {
                mutableStateOf(
                    runCatching {
                        StartupScreen.valueOf(prefs.getString("startup_screen", StartupScreen.WORKSPACE.name) ?: StartupScreen.WORKSPACE.name)
                    }.getOrDefault(StartupScreen.WORKSPACE)
                )
            }
            val selectStartupScreen: (StartupScreen) -> Unit = { screen ->
                startupScreen = screen
                prefs.edit().putString("startup_screen", screen.name).apply()
            }
            MultiPanelTheme(themeMode = themeMode) {
                when (startupScreen) {
                    StartupScreen.WORKSPACE -> WorkspaceScreen(
                        themeMode = themeMode,
                        onThemeModeChange = { mode ->
                            themeMode = mode
                            prefs.edit().putString("theme_mode", mode).apply()
                        },
                        onMainScreenRequested = { selectStartupScreen(StartupScreen.MAIN) }
                    )
                    StartupScreen.MAIN -> MainScreen(
                        themeMode = themeMode,
                        onThemeModeChange = { mode ->
                            themeMode = mode
                            prefs.edit().putString("theme_mode", mode).apply()
                        },
                        onWorkspaceScreenRequested = { selectStartupScreen(StartupScreen.WORKSPACE) }
                    )
                }
            }
        }
    }
}
