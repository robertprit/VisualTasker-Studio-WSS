package com.visualtasker.chartgraph.demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.visualtasker.chartgraph.demo.ui.screens.DashboardScreen

// Aggr-inspirierte Farbpalette
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF00E5FF), // Neon Cyan
    secondary = Color(0xFFFF007F), // Neon Magenta
    tertiary = Color(0xFF00FF9D), // Neon Green
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    onPrimary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White
)

@Composable
fun AggrComposeTheme(content: @Composable () -> Unit) {
    val colorScheme = DarkColorScheme // Erzwinge Dark Mode für Aggr-Look

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AggrComposeTheme {
                Surface(
                    modifier = androidx.compose.ui.Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DashboardScreen()
                }
            }
        }
    }
}