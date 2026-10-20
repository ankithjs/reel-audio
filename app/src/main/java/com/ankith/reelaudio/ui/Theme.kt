package com.ankith.reelaudio.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/*
 * A true neutral dark, after .claude/skills/taste-skill (app screens): grey steps for surfaces,
 * white for the one main control, and a single amber hue kept for what is playing (the progress,
 * the current clip and the line of lyrics being said).
 */
internal val Ground = Color(0xFF0A0A0A)
internal val Panel = Color(0xFF141414)
internal val Raised = Color(0xFF1E1E1E)
internal val Line = Color(0xFF2A2A2A)
internal val ControlLine = Color(0xFF444444)
internal val Muted = Color(0xFFB4B4B4)
internal val Faint = Color(0xFF949494)
internal val Accent = Color(0xFFF2A93B)

private val Colors = darkColorScheme(
    primary = Color.White,
    onPrimary = Color.Black,
    primaryContainer = Raised,
    onPrimaryContainer = Color.White,
    inversePrimary = Color.Black,
    secondary = Color.White,
    onSecondary = Color.Black,
    secondaryContainer = Line,
    onSecondaryContainer = Color.White,
    tertiary = Accent,
    onTertiary = Color.Black,
    background = Ground,
    onBackground = Color.White,
    surface = Ground,
    onSurface = Color.White,
    surfaceVariant = Raised,
    onSurfaceVariant = Muted,
    surfaceTint = Color.White,
    outline = ControlLine,
    outlineVariant = Line,
    surfaceBright = Raised,
    surfaceDim = Ground,
    surfaceContainerLowest = Ground,
    surfaceContainerLow = Panel,
    surfaceContainer = Panel,
    surfaceContainerHigh = Raised,
    surfaceContainerHighest = Color(0xFF262626),
    error = Color(0xFFE5566F),
)

@Composable
fun ReelTheme(content: @Composable () -> Unit) = MaterialTheme(colorScheme = Colors, content = content)
