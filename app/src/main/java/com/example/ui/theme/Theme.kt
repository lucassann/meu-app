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
    primary = CineRed,
    secondary = CineGold,
    tertiary = CineLightGray,
    background = CineBlack,
    surface = CineDarkGray,
    onBackground = CineTextWhite,
    onSurface = CineTextWhite,
    onPrimary = CineTextWhite,
    onSecondary = CineBlack,
  )

private val LightColorScheme = DarkColorScheme // Always premium Dark for CinePremium!

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Force Dark theme by default for cinematic immersion
  dynamicColor: Boolean = false, // Disable dynamic colors to preserve CinePremium's branding
  content: @Composable () -> Unit,
) {
  val colorScheme = DarkColorScheme
  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
