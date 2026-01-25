package app.meeplebook.feature.overview.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.meeplebook.feature.overview.GameHighlight
import app.meeplebook.ui.components.UiTextText
import app.meeplebook.ui.components.gameImageClip
import coil3.compose.AsyncImage

/** Aspect ratio for game thumbnail images (16:9). */
private const val GAME_THUMBNAIL_ASPECT_RATIO = 16f / 9f

@Composable
fun GameHighlightCard(
    highlight: GameHighlight,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.testTag("highlightCard_${highlight.id}"),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(GAME_THUMBNAIL_ASPECT_RATIO)
                    .gameImageClip(),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = highlight.thumbnailUrl,
                    contentDescription = highlight.gameName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = highlight.gameName,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            UiTextText(
                text = highlight.subtitleUiText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}