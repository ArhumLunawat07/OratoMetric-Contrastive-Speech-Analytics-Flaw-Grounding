package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val OratorIqMinimalLightScheme = lightColorScheme(
  primary = SignalBaselineCyan,
  onPrimary = Color.White,
  primaryContainer = Color(0xFFE0F2FE),
  onPrimaryContainer = Color(0xFF0369A1),
  secondary = SignalParticipantAmber,
  onSecondary = Color.White,
  secondaryContainer = Color(0xFFFEF3C7),
  onSecondaryContainer = Color(0xFFB45309),
  tertiary = SignalDtwViolet,
  onTertiary = Color.White,
  error = SignalFlawCrimson,
  onError = Color.White,
  errorContainer = SignalFlawCrimsonBg,
  onErrorContainer = SignalFlawCrimson,
  background = LabBgObsidian,
  onBackground = TextPrimary,
  surface = LabSurfaceDark,
  onSurface = TextPrimary,
  surfaceVariant = LabSurfaceElevated,
  onSurfaceVariant = TextSecondary,
  outline = LabBorderSubtle
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = false,
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  MaterialTheme(
    colorScheme = OratorIqMinimalLightScheme,
    typography = Typography,
    content = content
  )
}
