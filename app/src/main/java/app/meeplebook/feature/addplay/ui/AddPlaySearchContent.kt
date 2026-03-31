package app.meeplebook.feature.addplay.ui

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import app.meeplebook.R
import app.meeplebook.feature.addplay.AddPlayEvent
import app.meeplebook.feature.addplay.AddPlayUiState
import app.meeplebook.feature.addplay.SearchResultGameItem
import app.meeplebook.ui.components.RowItemImage
import app.meeplebook.ui.components.ScreenPadding
import app.meeplebook.ui.theme.MeepleBookTheme

@Composable
fun GameSearchContent(
    state: AddPlayUiState.GameSearch,
    onEvent: (AddPlayEvent) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = state.gameSearchQuery,
            onValueChange = {
                onEvent(AddPlayEvent.GameSearchEvent.GameSearchQueryChanged(it))
            },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            label = { Text(stringResource(R.string.add_play_search_game_label)) },
            placeholder = { Text(stringResource(R.string.add_play_search_game_placeholder)) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = ScreenPadding.Horizontal, vertical = ScreenPadding.Small)
                .testTag("gameSearchField")
        )

        if (state.gameSearchQuery.isNotEmpty() && state.gameSearchResults.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.add_play_no_results),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(vertical = ScreenPadding.Small),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("gameSearchResults")
            ) {
                items(
                    items = state.gameSearchResults,
                    key = { it.gameId }
                ) { game ->
                    GameSearchResultItem(
                        game = game,
                        onClick = {
                            onEvent(
                                AddPlayEvent.GameSearchEvent.GameSelected(
                                    gameId = game.gameId,
                                    gameName = game.name
                                )
                            )
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = ScreenPadding.Horizontal))
                }
            }
        }
    }
}

@Composable
private fun GameSearchResultItem(
    game: SearchResultGameItem,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(
                horizontal = ScreenPadding.Horizontal,
                vertical = ScreenPadding.ItemSpacing
            )
            .testTag("gameSearchResult_${game.gameId}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RowItemImage(
            thumbnailUrl = game.thumbnailUrl,
            contentDescription = game.name
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = game.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            game.yearPublished?.let { year ->
                Text(
                    text = year.toString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ── Previews ──────────────────────────────────────────────────────────────────

private class GameSearchContentPreviewProvider :
    PreviewParameterProvider<AddPlayUiState.GameSearch> {
    override val values = sequenceOf(
        previewGameSearchState(),
        previewGameSearchState(query = "Cat", hasResults = true),
    )
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun GameSearchContentPreview(
    @PreviewParameter(GameSearchContentPreviewProvider::class) state: AddPlayUiState.GameSearch,
) {
    MeepleBookTheme {
        GameSearchContent(state = state, onEvent = {})
    }
}
