package app.meeplebook.feature.collection

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.outlined.Casino
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import app.meeplebook.R
import app.meeplebook.ui.theme.MeepleBookTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionScreenContent(
    uiState: CollectionUiState,
    onGameClick: (CollectionGameItem) -> Unit = {},
    onSortChange: (CollectionSort) -> Unit = {},
    onFilterClick: () -> Unit = {},
    onRefresh: () -> Unit = {}
) {
    var showSortMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("collectionScreen")
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Text(
                    text = stringResource(R.string.collection_title),
                    fontWeight = FontWeight.Bold
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primary,
                titleContentColor = MaterialTheme.colorScheme.onPrimary
            ),
            modifier = Modifier.testTag("collectionTopBar")
        )

        // Controls Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag("collectionControls"),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Game count
            if (uiState.games.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.collection_game_count, uiState.games.size),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("gameCount")
                )
            } else {
                Spacer(modifier = Modifier.width(1.dp))
            }

            Row(
                horizontalArrangement = Arrangement.End
            ) {
                // Filter button
                FilledTonalButton(
                    onClick = onFilterClick,
                    modifier = Modifier.testTag("filterButton")
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = stringResource(R.string.collection_filter_button),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.collection_filter_button))
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Sort button with dropdown
                Box {
                    FilledTonalButton(
                        onClick = { showSortMenu = true },
                        modifier = Modifier.testTag("sortButton")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sort,
                            contentDescription = stringResource(R.string.collection_sort_button),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.collection_sort_button))
                    }

                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false },
                        modifier = Modifier.testTag("sortMenu")
                    ) {
                        CollectionSort.entries.forEach { sort ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = getSortLabel(sort),
                                        fontWeight = if (sort == uiState.currentSort) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    onSortChange(sort)
                                    showSortMenu = false
                                },
                                modifier = Modifier.testTag("sortMenuItem_${sort.name}")
                            )
                        }
                    }
                }
            }
        }

        // Main content with pull-to-refresh
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier
                .fillMaxSize()
                .testTag("collectionRefreshBox")
        ) {
            when {
                uiState.isLoading -> {
                    // Loading state
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("loadingIndicator"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = stringResource(R.string.collection_loading),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                uiState.games.isEmpty() -> {
                    // Empty state
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("emptyState"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.collection_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(32.dp)
                        )
                    }
                }
                else -> {
                    // Collection list
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("collectionList"),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = uiState.games,
                            key = { it.gameId }
                        ) { game ->
                            CollectionGameCard(
                                game = game,
                                onClick = { onGameClick(game) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CollectionGameCard(
    game: CollectionGameItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .testTag("gameCard_${game.gameId}"),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Game thumbnail placeholder
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Casino,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = game.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    game.yearPublished?.let { year ->
                        Text(
                            text = stringResource(R.string.collection_year_published, year),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (game.playCount > 0) {
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(R.string.collection_plays_count, game.playCount),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun getSortLabel(sort: CollectionSort): String {
    return when (sort) {
        CollectionSort.ALPHABETICAL -> stringResource(R.string.collection_sort_alphabetical)
        CollectionSort.YEAR_PUBLISHED_OLDEST -> stringResource(R.string.collection_sort_year_oldest)
        CollectionSort.YEAR_PUBLISHED_NEWEST -> stringResource(R.string.collection_sort_year_newest)
        CollectionSort.MOST_PLAYED -> stringResource(R.string.collection_sort_most_played)
        CollectionSort.LEAST_PLAYED -> stringResource(R.string.collection_sort_least_played)
        CollectionSort.MOST_RECENTLY_PLAYED -> stringResource(R.string.collection_sort_recently_played)
        CollectionSort.LEAST_RECENTLY_PLAYED -> stringResource(R.string.collection_sort_least_recently_played)
    }
}

/**
 * Provides preview parameter states for [CollectionScreenContent]:
 * 1. Default state with sample data
 * 2. Empty state
 * 3. Loading state
 * 4. Refreshing state
 */
class CollectionUiStatePreviewParameterProvider : PreviewParameterProvider<CollectionUiState> {
    override val values: Sequence<CollectionUiState> = sequenceOf(
        // Full state with sample data
        CollectionUiState(
            games = listOf(
                CollectionGameItem(
                    gameId = 1,
                    name = "Catan",
                    yearPublished = 1995,
                    thumbnail = null,
                    playCount = 42,
                    lastPlayed = "2024-12-01"
                ),
                CollectionGameItem(
                    gameId = 2,
                    name = "Wingspan",
                    yearPublished = 2019,
                    thumbnail = null,
                    playCount = 18,
                    lastPlayed = "2024-12-05"
                ),
                CollectionGameItem(
                    gameId = 3,
                    name = "7 Wonders Duel",
                    yearPublished = 2015,
                    thumbnail = null,
                    playCount = 25,
                    lastPlayed = "2024-11-28"
                ),
                CollectionGameItem(
                    gameId = 4,
                    name = "Azul",
                    yearPublished = 2017,
                    thumbnail = null,
                    playCount = 0,
                    lastPlayed = null
                ),
                CollectionGameItem(
                    gameId = 5,
                    name = "Ticket to Ride",
                    yearPublished = 2004,
                    thumbnail = null,
                    playCount = 33,
                    lastPlayed = "2024-12-03"
                )
            ),
            currentSort = CollectionSort.ALPHABETICAL
        ),
        // Empty state
        CollectionUiState(
            games = emptyList()
        ),
        // Loading state
        CollectionUiState(
            isLoading = true
        ),
        // Refreshing state
        CollectionUiState(
            games = listOf(
                CollectionGameItem(
                    gameId = 1,
                    name = "Catan",
                    yearPublished = 1995,
                    thumbnail = null,
                    playCount = 42,
                    lastPlayed = "2024-12-01"
                )
            ),
            isRefreshing = true
        )
    )
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun CollectionScreenPreview(
    @PreviewParameter(CollectionUiStatePreviewParameterProvider::class) uiState: CollectionUiState
) {
    MeepleBookTheme {
        CollectionScreenContent(uiState = uiState)
    }
}
