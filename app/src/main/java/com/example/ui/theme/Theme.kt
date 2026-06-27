package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = SophisticatedPrimary,
    onPrimary = SophisticatedOnPrimary,
    primaryContainer = SophisticatedPrimaryContainer,
    onPrimaryContainer = SophisticatedOnPrimaryContainer,
    secondary = SophisticatedSecondary,
    secondaryContainer = SophisticatedSecondaryContainer,
    background = SophisticatedBackground,
    surface = SophisticatedSurface,
    onBackground = SophisticatedText,
    onSurface = SophisticatedText,
    onSurfaceVariant = SophisticatedSubText,
    outline = SophisticatedBorder
  )

private val LightColorScheme =
  lightColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFF6750A4),
    onPrimary = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
    primaryContainer = androidx.compose.ui.graphics.Color(0xFFEADDFF),
    onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFF21005D),
    secondary = androidx.compose.ui.graphics.Color(0xFF625B71),
    secondaryContainer = androidx.compose.ui.graphics.Color(0xFFE8DEF8),
    background = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
    surface = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
    onBackground = androidx.compose.ui.graphics.Color(0xFF1C1B1F),
    onSurface = androidx.compose.ui.graphics.Color(0xFF1C1B1F),
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFF49454F),
    outline = androidx.compose.ui.graphics.Color(0xFF79747E)
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Force sophisticated dark mode
  dynamicColor: Boolean = false, // Disable dynamicColor so we preserve the gorgeous theme exactly as designed
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

