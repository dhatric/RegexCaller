package com.regexcaller.callblocker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val RingBlockColorScheme = darkColorScheme(
    primary = RingBlockOrange,
    onPrimary = RingBlockBackground,
    primaryContainer = RingBlockAccentContainer,
    onPrimaryContainer = RingBlockOrangeGlow,
    inversePrimary = RingBlockOrangeGlow,
    secondary = RingBlockGray,
    onSecondary = RingBlockBackground,
    secondaryContainer = RingBlockSurfaceHigh,
    onSecondaryContainer = RingBlockWhite,
    tertiary = RingBlockOrangeGlow,
    onTertiary = RingBlockBackground,
    tertiaryContainer = RingBlockSurfaceHigh,
    onTertiaryContainer = RingBlockWhite,
    background = RingBlockBackground,
    onBackground = RingBlockWhite,
    surface = RingBlockBackground,
    onSurface = RingBlockWhite,
    surfaceVariant = RingBlockSurface,
    onSurfaceVariant = RingBlockGray,
    surfaceTint = RingBlockOrange,
    inverseSurface = RingBlockWhite,
    inverseOnSurface = RingBlockBackground,
    error = RingBlockError,
    onError = RingBlockBackground,
    errorContainer = RingBlockErrorContainer,
    onErrorContainer = RingBlockWhite,
    outline = RingBlockOutline,
    outlineVariant = RingBlockSurfaceHigh,
    scrim = RingBlockBackground,
    surfaceBright = RingBlockSurface,
    surfaceDim = RingBlockBackground,
    surfaceContainer = RingBlockSurface,
    surfaceContainerHigh = RingBlockSurfaceHigh,
    surfaceContainerHighest = RingBlockSurfaceHighest,
    surfaceContainerLow = RingBlockSurfaceLow,
    surfaceContainerLowest = RingBlockBackground
)

@Composable
fun CallBlockerTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = RingBlockColorScheme,
        content = content
    )
}
