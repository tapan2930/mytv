package com.localcast.tv.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

val TvBackground = Color(0xFF050508)
val TvSurface = Color(0xFF0E0E12)
val TvSurfaceVariant = Color(0xFF16161B)

val TvBrandIndigo = Color(0xFF6366F1)
val TvBrandPurple = Color(0xFF8B5CF6)
val TvAccentPrimary = Color(0xFF6366F1)
val TvAccentPurple = Color(0xFFA855F7)
val TvAccentCyan = Color(0xFFA855F7) // Cyan completely removed from the client app; mapped to luminous aurora purple
val TvAccentBlue = Color(0xFF3B82F6)
val TvAccentGreen = Color(0xFF10B981)
val TvAccentRed = Color(0xFFEF4444)
val TvTextPrimary = Color(0xFFF8FAFC)
val TvTextSecondary = Color(0xFF94A3B8)

// Modern rich gradients for TV D-Pad focus & cards
val TvBrandGradient = Brush.horizontalGradient(listOf(Color(0xFF6366F1), Color(0xFF8B5CF6)))
val TvStreamGradient = Brush.horizontalGradient(listOf(Color(0xFF6366F1), Color(0xFFA855F7)))
val TvCardGradient = Brush.verticalGradient(listOf(Color(0xFF16161C), Color(0xFF0E0E13)))

// Focus Glowing Halo tints
val TvFocusHaloCyan = Color(0xFFA855F7).copy(alpha = 0.45f)
val TvFocusHaloPurple = Color(0xFF8B5CF6).copy(alpha = 0.45f)
val TvFocusHaloGreen = Color(0xFF10B981).copy(alpha = 0.45f)
val TvFocusHaloWhite = Color.White.copy(alpha = 0.85f)

@OptIn(ExperimentalTvMaterial3Api::class)
private val TvDarkColors = darkColorScheme(
    primary = TvAccentPrimary,
    secondary = TvAccentBlue,
    background = TvBackground,
    surface = TvSurface,
    surfaceVariant = TvSurfaceVariant,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = TvTextPrimary,
    onSurface = TvTextPrimary,
    error = TvAccentRed
)

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun LocalCastTvTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = TvDarkColors,
        content = content
    )
}
