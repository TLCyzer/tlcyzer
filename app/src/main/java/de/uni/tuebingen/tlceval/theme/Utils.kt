package de.uni.tuebingen.tlceval.theme

import android.graphics.Color
import androidx.compose.ui.graphics.Color as ComposeColor

fun alterValueOfColor(color: ComposeColor, factor: Float): ComposeColor {
    val alpha = color.alpha
    return ComposeColor(
        Color.HSVToColor(FloatArray(3).apply {
            Color.RGBToHSV(
                (color.red * 255).toInt(), (color.green * 255).toInt(),
                (color.blue * 255).toInt(), this
            )
            this[2] *= factor
        })
    ).copy(alpha = alpha)
}