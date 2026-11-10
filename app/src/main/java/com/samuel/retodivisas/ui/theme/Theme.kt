package com.samuel.retodivisas.ui.theme

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

val BgDark = Color(0xFF0F172A)
val SurfaceCard = Color(0xFF1E293B)
val SurfaceElevated = Color(0xFF283548)
val BorderSubtle = Color(0xFF334155)
val TextPrimary = Color(0xFFF8FAFC)
val TextSecondary = Color(0xFF94A3B8)

// Alias mantenidos por compatibilidad con todas las pantallas
val CardGray = SurfaceCard
val CardText = TextPrimary

val BluePrimary = Color(0xFF38BDF8)
val AuditRed = Color(0xFFEF4444)
val AuditRedBg = Color(0xFF3B181E)
val SuccessGreen = Color(0xFF10B981)
val SuccessGreenBg = Color(0xFF0E3A2F)

val DarkColors = darkColorScheme(
    primary = BluePrimary,
    onPrimary = Color(0xFF082F49),
    secondary = SuccessGreen,
    onSecondary = Color(0xFF052E16),
    background = BgDark,
    onBackground = TextPrimary,
    surface = SurfaceCard,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = BorderSubtle,
    error = AuditRed
)

@Composable
fun RetoDivisasTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onBackground,
            content = content
        )
    }
}
