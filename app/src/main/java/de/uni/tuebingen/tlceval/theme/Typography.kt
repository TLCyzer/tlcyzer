package de.uni.tuebingen.tlceval.theme

import androidx.compose.material.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import de.uni.tuebingen.tlceval.R

val Oxygen = FontFamily(
    Font(R.font.oxygen_regular),
    Font(R.font.oxygen_bold, FontWeight.Bold),
    Font(R.font.oxygen_light, FontWeight.Light)
)

val Alegreya = FontFamily(
    Font(R.font.alegreya_regular),
    Font(R.font.alegreya_italic, style = FontStyle.Italic),
    Font(R.font.alegreya_medium, FontWeight.Medium),
    Font(R.font.alegreya_mediumitalic, FontWeight.Medium, FontStyle.Italic),
    Font(R.font.alegreya_semibold, FontWeight.SemiBold)
)


val AppTypography = Typography(
    h1 = TextStyle(
        fontFamily = Oxygen,
        fontWeight = FontWeight.Light,
        fontSize = 94.sp,
        letterSpacing = (-1.5).sp,
    ),
    h2 = TextStyle(
        fontFamily = Oxygen,
        fontWeight = FontWeight.Light,
        fontSize = 59.sp,
        letterSpacing = (-0.5).sp,
    ),
    h3 = TextStyle(
        fontFamily = Oxygen,
        fontWeight = FontWeight.Normal,
        fontSize = 47.sp,
        letterSpacing = 0.sp,
    ),
    h4 = TextStyle(
        fontFamily = Oxygen,
        fontWeight = FontWeight.Normal,
        fontSize = 33.sp,
        letterSpacing = 0.25.sp,
    ),
    h5 = TextStyle(
        fontFamily = Oxygen,
        fontWeight = FontWeight.Normal,
        fontSize = 24.sp,
        letterSpacing = 0.sp,
    ),
    h6 = TextStyle(
        fontFamily = Oxygen,
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp,
        letterSpacing = 0.15.sp,
    ),
    subtitle1 = TextStyle(
        fontFamily = Alegreya,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        letterSpacing = 0.15.sp,
    ),
    subtitle2 = TextStyle(
        fontFamily = Alegreya,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        letterSpacing = 0.1.sp,
    ),
    body1 = TextStyle(
        fontFamily = Alegreya,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
        letterSpacing = 0.5.sp,
    ),
    body2 = TextStyle(
        fontFamily = Alegreya,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        letterSpacing = 0.25.sp,
    ),
    caption = TextStyle(
        fontFamily = Alegreya,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        letterSpacing = 0.4.sp,
    ),
    overline = TextStyle(
        fontFamily = Alegreya,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        letterSpacing = 1.5.sp,
    ),
    button = TextStyle(
        fontFamily = Alegreya,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        letterSpacing = 1.25.sp,
    ),
)