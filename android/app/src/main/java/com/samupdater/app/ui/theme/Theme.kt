package com.samupdater.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

/*
 * One UI look: a fixed Samsung palette instead of wallpaper colors, flat cards with big corners
 * on a light grey (or black) page. Navy sits in secondary and drives the pill buttons and nav bar.
 */
private val LightColors = lightColorScheme(
    primary = OneUiPalette.Blue,
    onPrimary = OneUiPalette.White,
    primaryContainer = OneUiPalette.BlueTint,
    onPrimaryContainer = OneUiPalette.Navy,
    secondary = OneUiPalette.Navy,
    onSecondary = OneUiPalette.White,
    secondaryContainer = OneUiPalette.BlueTint,
    onSecondaryContainer = OneUiPalette.Navy,
    tertiary = OneUiPalette.Green,
    background = OneUiPalette.PageLight,
    onBackground = OneUiPalette.Ink,
    surface = OneUiPalette.White,
    onSurface = OneUiPalette.Ink,
    surfaceVariant = OneUiPalette.BlueTint,
    onSurfaceVariant = OneUiPalette.Grey,
    surfaceContainer = OneUiPalette.White,
    surfaceContainerHigh = OneUiPalette.White,
    surfaceContainerHighest = OneUiPalette.FieldLight,
    outline = OneUiPalette.Grey,
    outlineVariant = OneUiPalette.DividerLight,
)

private val DarkColors = darkColorScheme(
    primary = OneUiPalette.BlueDark,
    onPrimary = OneUiPalette.White,
    primaryContainer = OneUiPalette.BlueTintDark,
    onPrimaryContainer = OneUiPalette.BlueSoft,
    secondary = OneUiPalette.BlueDark,
    onSecondary = OneUiPalette.White,
    secondaryContainer = OneUiPalette.BlueTintDark,
    onSecondaryContainer = OneUiPalette.BlueSoft,
    tertiary = OneUiPalette.GreenDark,
    background = OneUiPalette.Black,
    onBackground = OneUiPalette.InkDark,
    surface = OneUiPalette.CardDark,
    onSurface = OneUiPalette.InkDark,
    surfaceVariant = OneUiPalette.BlueTintDark,
    onSurfaceVariant = OneUiPalette.GreyDark,
    surfaceContainer = OneUiPalette.CardDark,
    surfaceContainerHigh = OneUiPalette.CardDark,
    surfaceContainerHighest = OneUiPalette.FieldDark,
    outline = OneUiPalette.GreyDark,
    outlineVariant = OneUiPalette.DividerDark,
)

private val OneUiShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun SamUpdaterTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        shapes = OneUiShapes,
        content = content,
    )
}
