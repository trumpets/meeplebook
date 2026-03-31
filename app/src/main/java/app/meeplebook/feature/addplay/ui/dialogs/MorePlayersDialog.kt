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
import androidx.compose.ui.unit.dp
import app.meeplebook.R
import app.meeplebook.core.plays.domain.PlayerIdentity
import app.meeplebook.feature.addplay.PlayerSuggestion

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