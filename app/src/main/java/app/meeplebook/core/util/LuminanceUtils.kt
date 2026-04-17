package app.meeplebook.core.util

import androidx.compose.ui.graphics.Color

object LuminanceUtils {

    val black = Color.Black.copy(alpha = 0.7f)

    val white = Color.White.copy(alpha = 0.9f)

    /**
     * Returns true if the given [color] is perceptually light, used to decide checkmark contrast.
     * Uses the standard luminance formula (WCAG).
     */
    fun isLightColor(color: Color): Boolean {
        val r = color.red
        val g = color.green
        val b = color.blue
        val luminance = 0.2126f * r + 0.7152f * g + 0.0722f * b
        return luminance > 0.5f
    }

    fun contrastColorFor(color: Color): Color {
        return if (isLightColor(color))
            black
        else
            white
    }
}