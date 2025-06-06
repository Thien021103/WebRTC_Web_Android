package io.getstream.webrtc.sample.compose.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable

private val DarkColorPalette = darkColors(
  primary = Cyan200,
  primaryVariant = Cyan1200,
  secondary = BluePrimary,
  secondaryVariant = Teal200,
  background = DarkBackground,
  surface = DarkSurface,
  error = RedError,
  onPrimary = Black, // Text/icon color on primary
  onSecondary = White,
  onBackground = White,
  onSurface = White,
  onError = White
)

private val LightColorPalette = lightColors(
  primary = Cyan500,
  primaryVariant = Cyan900,
  secondary = BluePrimary,
  secondaryVariant = Teal200,
  background = LightGrayBackground, // Use Color.White for white background
  surface = White, // For cards, dialogs
  error = RedError,
  onPrimary = White,
  onSecondary = White,
  onBackground = Black,
  onSurface = Black,
  onError = White
)

@Composable
fun WebrtcSampleComposeTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit
) {
  val colors = if (darkTheme) {
    DarkColorPalette
  } else {
    LightColorPalette
  }

  MaterialTheme(
    colors = colors,
    typography = Typography,
    shapes = Shapes,
    content = content
  )
}
