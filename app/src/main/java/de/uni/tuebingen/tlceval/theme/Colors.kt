package de.uni.tuebingen.tlceval.theme

import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.ui.graphics.Color

const val base_opacity = 0.87f;
const val accent_opacity = 0.54f;
const val disabled_opacity = 0.38f;

private val primary = Color(0xFF75996a);
private val primaryLight = Color(0xFFD6E0D2);
private val primaryDark = Color(0xFF587C4D);
private val error = Color(0xFFC95B24);
private val errorLight = Color(0xFFEFCEBD);
private val errorDark = Color(0xFFB54016);
private val dark = Color(0xFF1C211B);
private val light = Color(0xFFE7F2E4);

val DarkColors = darkColors(
    primary = primary,
    primaryVariant = primaryLight,
    secondary = alterValueOfColor(primary, 1.1f),
    secondaryVariant = alterValueOfColor(primaryLight, 1.1f),
    surface = alterValueOfColor(dark, 1.1f),
    background = dark,
    error = error,
//    onPrimary = light.copy(alpha = base_opacity),
//    onSecondary = light.copy(alpha = accent_opacity),
    onSurface = light.copy(alpha = base_opacity),
    onBackground = light.copy(alpha = base_opacity),
    onError = light.copy(alpha = base_opacity),
)
val LightColors = lightColors(
    primary = primary,
    primaryVariant = primaryDark,
    secondary = alterValueOfColor(primary, 1.1f),
    secondaryVariant = alterValueOfColor(primaryDark, 1.1f),
    surface = alterValueOfColor(light, 1.1f),
    background = light,
    error = error,
//    onPrimary = dark.copy(alpha = base_opacity),
//    onSecondary = dark.copy(alpha = accent_opacity),
    onSurface = dark.copy(alpha = base_opacity),
    onBackground = dark.copy(alpha = base_opacity),
    onError = dark.copy(alpha = base_opacity),
)

//Color.Black.copy(alpha = 0.6f)