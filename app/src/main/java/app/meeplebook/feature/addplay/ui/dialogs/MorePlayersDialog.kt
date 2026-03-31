package app.meeplebook.feature.addplay.ui.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import app.meeplebook.R
import app.meeplebook.core.plays.domain.PlayerIdentity
import app.meeplebook.feature.addplay.PlayerSuggestion
import app.meeplebook.ui.theme.MeepleBookTheme

/**
 * Dialog that lists all available [PlayerSuggestion]s so the user can add a player
 * that did not appear in the inline suggestion chips (e.g. when there are more than the
 * visible limit).
 *
 * Shows a scrollable list of player names (with optional BGG username). Tapping a row
 * calls [onPlayerSelected] and the caller is responsible for dismissing the dialog.
 * When [suggestions] is empty a "no suggestions" message is shown instead.
 *
 * Dismissed by tapping the Cancel button or by a back-gesture / outside tap via
 * [onDismiss].
 *
 * @param suggestions Players available to add, already filtered to exclude those already
 *   in the current play.
 * @param onPlayerSelected Called with the chosen [PlayerIdentity] when the user taps a row.
 * @param onDismiss Called when the dialog should be closed without a selection.
 */
@Composable
fun MorePlayersDialog(
    suggestions: List<PlayerSuggestion>,
    onPlayerSelected: (PlayerIdentity) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_play_more_players_title)) },
        text = {
            if (suggestions.isEmpty()) {
                Text(
                    text = stringResource(R.string.add_play_no_player_suggestions),
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                LazyColumn {
                    items(
                        items = suggestions,
                        key = { it.playerIdentity.name + (it.playerIdentity.username ?: "") }
                    ) { suggestion ->
                        Text(
                            text = suggestion.playerIdentity.username?.let { username ->
                                stringResource(
                                    R.string.player_name_with_username,
                                    suggestion.playerIdentity.name,
                                    username
                                )
                            } ?: suggestion.playerIdentity.name,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPlayerSelected(suggestion.playerIdentity) }
                                .padding(vertical = 12.dp)
                                .testTag("morePlayerItem_${suggestion.playerIdentity.name}")
                        )
                        HorizontalDivider()
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    )
}
// ── Previews ──────────────────────────────────────────────────────────────────

private class MorePlayersDialogPreviewProvider :
    PreviewParameterProvider<List<PlayerSuggestion>> {
    override val values = sequenceOf(
        emptyList(),
        listOf(
            PlayerSuggestion(PlayerIdentity("Alice", username = "alicebgg", userId = null)),
            PlayerSuggestion(PlayerIdentity("Bob", username = null, userId = null)),
            PlayerSuggestion(PlayerIdentity("Charlie", username = "charlie99", userId = null)),
        ),
    )
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MorePlayersDialogPreview(
    @PreviewParameter(MorePlayersDialogPreviewProvider::class) suggestions: List<PlayerSuggestion>,
) {
    MeepleBookTheme {
        MorePlayersDialog(
            suggestions = suggestions,
            onPlayerSelected = {},
            onDismiss = {},
        )
    }
}
