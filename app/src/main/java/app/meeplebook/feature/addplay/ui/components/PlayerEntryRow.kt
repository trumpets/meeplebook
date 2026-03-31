package app.meeplebook.feature.addplay.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import app.meeplebook.R
import app.meeplebook.core.plays.domain.PlayerIdentity
import app.meeplebook.core.plays.model.PlayerColor
import app.meeplebook.core.util.LuminanceUtils
import app.meeplebook.core.util.ScoreFormatter
import app.meeplebook.feature.addplay.PLAYER_ROW_HEIGHT
import app.meeplebook.feature.addplay.PlayerEntryUi
import app.meeplebook.ui.theme.MeepleBookTheme

@Composable
fun PlayerEntryRow(
    player: PlayerEntryUi,
    onWinnerToggle: () -> Unit,
    onScoreClick: () -> Unit,
    onColorClick: () -> Unit,
    onDragStart: () -> Unit,
    onDrag: (deltaY: Float) -> Unit,
    onDragEnd: () -> Unit,
) {
    val colorEnum = PlayerColor.fromString(player.color)
    val winnerTint = Color(0xFFFFAB40)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(PLAYER_ROW_HEIGHT)
            .background(
                if (player.isWinner) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                else MaterialTheme.colorScheme.surface,
            )
            .padding(horizontal = 4.dp)
            .testTag("playerEntry_${player.playerIdentity.name}"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Position badge — filled with the player's color when assigned, tappable to open
        // the color picker. Falls back to the theme's primaryContainer when no color is set.
        val badgeColor = colorEnum
            ?.let { Color(it.hexColor.toColorInt()) }
            ?: MaterialTheme.colorScheme.primaryContainer

        val badgeTextColor = if (colorEnum != null) {
            LuminanceUtils.contrastColorFor(badgeColor)
        } else {
            MaterialTheme.colorScheme.onPrimaryContainer
        }

        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(badgeColor)
                .border(
                    width = if (colorEnum != null) 0.dp else 1.dp,
                    color = if (colorEnum != null) Color.Transparent
                    else MaterialTheme.colorScheme.outlineVariant,
                    shape = CircleShape,
                )
                .clickable(onClick = onColorClick),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = player.startPosition.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = badgeTextColor,
                fontWeight = FontWeight.Bold,
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Name + username
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = player.playerIdentity.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (player.isWinner) FontWeight.Bold else FontWeight.Normal,
            )
            player.playerIdentity.username?.let { uname ->
                Text(
                    text = "@$uname",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Score (tappable — opens ScoreInputDialog)
        Text(
            text = player.score?.let { score ->
                ScoreFormatter.format(score)
            } ?: stringResource(R.string.score_input_placeholder),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .clickable(onClick = onScoreClick)
                .padding(horizontal = 8.dp, vertical = 4.dp),
        )

        // Winner star toggle
        IconButton(onClick = onWinnerToggle) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = if (player.isWinner) winnerTint else MaterialTheme.colorScheme.outlineVariant,
            )
        }

        // Drag handle — use rememberUpdatedState so pointerInput(Unit) always
        // calls the latest lambdas even after list reorders
        val latestOnDragStart by rememberUpdatedState(onDragStart)
        val latestOnDrag by rememberUpdatedState(onDrag)
        val latestOnDragEnd by rememberUpdatedState(onDragEnd)

        Icon(
            imageVector = Icons.Default.DragHandle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(horizontal = 4.dp)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { latestOnDragStart() },
                        onDrag = { change, amount ->
                            change.consume()
                            latestOnDrag(amount.y)
                        },
                        onDragEnd = { latestOnDragEnd() },
                        onDragCancel = { latestOnDragEnd() },
                    )
                },
        )
    }
}
// ── Previews ──────────────────────────────────────────────────────────────────

private class PlayerEntryRowPreviewProvider : PreviewParameterProvider<PlayerEntryUi> {
    override val values = sequenceOf(
        PlayerEntryUi(
            playerIdentity = PlayerIdentity("Alice", username = "alicebgg", userId = null),
            startPosition = 1,
            color = PlayerColor.BLUE.colorString,
            score = 42.0,
            isWinner = true,
        ),
        PlayerEntryUi(
            playerIdentity = PlayerIdentity("Bob", username = null, userId = null),
            startPosition = 2,
            color = PlayerColor.RED.colorString,
            score = 38.0,
            isWinner = false,
        ),
        PlayerEntryUi(
            playerIdentity = PlayerIdentity("Charlie", username = null, userId = null),
            startPosition = 3,
            color = null,
            score = null,
            isWinner = false,
        ),
        PlayerEntryUi(
            playerIdentity = PlayerIdentity("Diana", username = "dianabgg", userId = null),
            startPosition = 4,
            color = PlayerColor.WHITE.colorString,
            score = -5.5,
            isWinner = false,
        ),
    )
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PlayerEntryRowPreview(
    @PreviewParameter(PlayerEntryRowPreviewProvider::class) player: PlayerEntryUi,
) {
    MeepleBookTheme {
        PlayerEntryRow(
            player = player,
            onWinnerToggle = {},
            onScoreClick = {},
            onColorClick = {},
            onDragStart = {},
            onDrag = {},
            onDragEnd = {},
        )
    }
}
