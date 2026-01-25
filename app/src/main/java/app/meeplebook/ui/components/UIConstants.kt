package app.meeplebook.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

/**
 * Standard corner shape for board game cover / thumbnail images.
 */
val GameImageShape = RoundedCornerShape(6.dp)

/**
 * Applies the standard game image clip.
 */
fun Modifier.gameImageClip(): Modifier = clip(GameImageShape)