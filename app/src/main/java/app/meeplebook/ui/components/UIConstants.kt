package app.meeplebook.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Standard padding values for consistent screen layouts.
 */
object ScreenPadding {
    /** Standard horizontal padding from screen edges. */
    val Horizontal: Dp = 16.dp

    /** Standard vertical spacing between sections. */
    val SectionSpacing: Dp = 16.dp

    /** Standard content padding for scrollable lists. */
    val ContentPadding: Dp = 16.dp

    /** Standard spacing between items in a list. */
    val ItemSpacing: Dp = 12.dp

    /** Standard card internal padding. */
    val CardInternal: Dp = 16.dp

    /** Small padding for compact elements. */
    val Small: Dp = 8.dp
}

/**
 * Standard corner shape for board game cover / thumbnail images.
 */
val GameImageShape = RoundedCornerShape(6.dp)

/**
 * Applies the standard game image clip.
 */
fun Modifier.gameImageClip(): Modifier = clip(GameImageShape)