package app.meeplebook.feature.addplay.ui.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.meeplebook.R
import app.meeplebook.feature.addplay.AddPlayEvent
import app.meeplebook.feature.addplay.AddPlayUiState
import app.meeplebook.feature.addplay.ui.dialogs.MorePlayersDialog
import app.meeplebook.ui.components.ScreenPadding

@Composable
fun SuggestedPlayersSection(
    state: AddPlayUiState.GameSelected,
    onEvent: (AddPlayEvent) -> Unit
) {
    var showMorePlayersDialog by remember { mutableStateOf(false) }

    val addedIdentities = state.players.players.map { it.playerIdentity }.toSet()
    val availableSuggestions = state.playersByLocation
        .filter { it.playerIdentity !in addedIdentities }

    if (availableSuggestions.isNotEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = ScreenPadding.Horizontal,
                    vertical = ScreenPadding.Small
                )
        ) {
            Text(
                text = stringResource(R.string.add_play_suggested_players_label),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))

            val chipSuggestions = availableSuggestions.take(10)
            val hasMore = availableSuggestions.size > 10

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 4.dp),
                modifier = Modifier.testTag("playerSuggestionChips")
            ) {
                items(
                    items = chipSuggestions,
                    key = { it.playerIdentity.name + (it.playerIdentity.username ?: "") }
                ) { suggestion ->
                    SuggestionChip(
                        onClick = {
                            onEvent(
                                AddPlayEvent.PlayerListEvent.AddPlayerFromSuggestion(
                                    playerIdentity = suggestion.playerIdentity,
                                    startPosition = state.players.players.size + 1
                                )
                            )
                        },
                        label = { Text(suggestion.playerIdentity.name) }
                    )
                }

                if (hasMore) {
                    item {
                        AssistChip(
                            onClick = { showMorePlayersDialog = true },
                            label = { Text(stringResource(R.string.add_play_more_players)) },
                            modifier = Modifier.testTag("morePlayersChip")
                        )
                    }
                }
            }
        }
    }

    if (showMorePlayersDialog) {
        val allAvailable = state.playersByLocation
            .filter { it.playerIdentity !in addedIdentities }

        MorePlayersDialog(
            suggestions = allAvailable,
            onPlayerSelected = { playerIdentity ->
                onEvent(
                    AddPlayEvent.PlayerListEvent.AddPlayerFromSuggestion(
                        playerIdentity = playerIdentity,
                        startPosition = state.players.players.size + 1
                    )
                )
                showMorePlayersDialog = false
            },
            onDismiss = { showMorePlayersDialog = false }
        )
    }
}