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

