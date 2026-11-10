package dev.tintwym.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
  primary = Color(0xFF34D399),
  onPrimary = Color(0xFF022C22),
  primaryContainer = Color(0xFF065F46),
  onPrimaryContainer = Color(0xFFD1FAE5),
  secondary = Color(0xFF94A3B8),
  onSecondary = Color(0xFF0F172A),
  secondaryContainer = Color(0xFF334155),
  onSecondaryContainer = Color(0xFFF1F5F9),
  tertiary = Color(0xFF38BDF8),
  onTertiary = Color(0xFF082F49),
  tertiaryContainer = Color(0xFF075985),
  onTertiaryContainer = Color(0xFFE0F2FE),
  background = DarkBackground,
  onBackground = DarkTextPrimary,
  surface = DarkSurface,
  onSurface = DarkTextPrimary,
  surfaceVariant = DarkSurfaceElevated,
  onSurfaceVariant = DarkTextSecondary,
  outline = DarkBorder,
  error = Color(0xFFF87171),
  onError = Color(0xFF450A0A),
)

private val LightColorScheme = lightColorScheme(
  primary = SleekPrimary,
  onPrimary = Color.White,
  primaryContainer = SleekPrimaryContainer,
  onPrimaryContainer = SleekOnPrimaryContainer,
  secondary = SleekSecondary,
  onSecondary = Color.White,
  secondaryContainer = SleekSecondaryContainer,
  onSecondaryContainer = SleekOnSecondaryContainer,
  tertiary = SleekTertiary,
  onTertiary = Color.White,
  tertiaryContainer = SleekTertiaryContainer,
  onTertiaryContainer = SleekOnTertiaryContainer,
  background = SleekBackground,
  onBackground = SleekTextPrimary,
  surface = SleekSurface,
  onSurface = SleekTextPrimary,
  surfaceVariant = SleekSurfaceVariant,
  onSurfaceVariant = SleekTextSecondary,
  outline = SleekBorder,
  error = Color(0xFFDC2626),
  onError = Color.White,
  errorContainer = Color(0xFFFEE2E2),
  onErrorContainer = Color(0xFF991B1B),
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
  // Bar colors come from enableEdgeToEdge() (transparent). Only icon contrast is set here.
  val view = LocalView.current
  if (!view.isInEditMode) {
    SideEffect {
      val window = (view.context as Activity).window
      WindowCompat.getInsetsController(window, view).apply {
        isAppearanceLightStatusBars = !darkTheme
        isAppearanceLightNavigationBars = !darkTheme
      }
    }
  }

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    shapes = CgShapes,
    content = content,
  )
}
