package app.meeplebook.feature.overview

import android.annotation.SuppressLint
import android.content.res.Configuration
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.meeplebook.R
import app.meeplebook.core.ui.uiText
import app.meeplebook.feature.overview.ui.EmptyStateMessage
import app.meeplebook.feature.overview.ui.GameHighlightCard
import app.meeplebook.feature.overview.ui.RecentPlayCard
import app.meeplebook.feature.overview.ui.StatsCard
import app.meeplebook.ui.components.SectionHeader
import app.meeplebook.ui.theme.MeepleBookTheme


/**
 * Overview screen (home tab) entry point that wires the ViewModel to the UI.
 *
 * @param viewModel The OverviewViewModel (injected by Hilt)
 */
@Composable
fun OverviewScreen(
    refreshOnLogin: Boolean,
    viewModel: OverviewViewModel = hiltViewModel()
) {
    LaunchedEffect(refreshOnLogin) {
        if (refreshOnLogin) {
            viewModel.refresh()
        }
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    OverviewContent(
        uiState = uiState,
        onRefresh = { viewModel.refresh() },
        onErrorShown = { viewModel.clearError() },
        onRecentPlayClick = { /* TODO: Navigate to play details */ },
        onRecentlyAddedClick = { /* TODO: Navigate to game details */ },
        onSuggestedGameClick = { /* TODO: Navigate to game details */ },
        onLogPlayClick = { /* TODO: Navigate to record play screen */ }
    )
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter") // TODO remove once you rework screen with scaffold stuff
@Composable
fun OverviewContent(
    uiState: OverviewUiState,
    modifier: Modifier = Modifier,
    onRefresh: () -> Unit = {},
    onErrorShown: () -> Unit = {},
    onRecentPlayClick: (RecentPlay) -> Unit = {},
    onRecentlyAddedClick: () -> Unit = {},
    onSuggestedGameClick: () -> Unit = {},
    onLogPlayClick: () -> Unit = {}
) {
    val snackbarHostState = remember { SnackbarHostState() }

    // Resolve error message string when errorMessageResId is not null
    val errorMessage = uiState.errorMessageResId?.let { stringResource(id = it) }

    // Show snackbar when there's an error
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(message = it)
            onErrorShown()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .padding(16.dp)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onLogPlayClick,
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag("logPlayFab")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.log_play_description)
                )
            }
        }
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize()
        ) {
            if (uiState.isLoading) {
                // Loading state
                Box(
                    modifier = modifier
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
                            text = stringResource(R.string.loading_message),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = modifier
                        .testTag("overviewContent"),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Quick Stats Card
                    item {
                        StatsCard(
                            stats = uiState.stats,
                            lastSyncedUiText = uiState.lastSyncedUiText
                        )
                    }

                    // Recent Activity Section
                    item {
                        SectionHeader(title = stringResource(R.string.recent_activity_title))
                    }

                    // Empty state for recent plays
                    if (uiState.recentPlays.isEmpty()) {
                        item {
                            EmptyStateMessage(
                                message = stringResource(R.string.empty_recent_plays_message),
                                modifier = Modifier.testTag("emptyRecentPlays")
                            )
                        }
                    } else {
                        items(
                            items = uiState.recentPlays,
                            key = { it.id }
                        ) { play ->
                            RecentPlayCard(
                                play = play,
                                onClick = { onRecentPlayClick(play) }
                            )
                        }
                    }

                    // Collection Highlights Section
                    if (uiState.recentlyAddedGame != null || uiState.suggestedGame != null) {
                        item {
                            SectionHeader(title = stringResource(R.string.collection_highlights_title))
                        }

                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                uiState.recentlyAddedGame?.let { game ->
                                    GameHighlightCard(
                                        highlight = game,
                                        onClick = onRecentlyAddedClick,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                uiState.suggestedGame?.let { game ->
                                    GameHighlightCard(
                                        highlight = game,
                                        onClick = onSuggestedGameClick,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Provides preview parameter states for [OverviewContent]:
 * 1. Default state with sample data
 * 2. Empty state
 * 3. Loading state
 * 4. Refreshing state
 */
class OverviewUiStatePreviewParameterProvider : PreviewParameterProvider<OverviewUiState> {
    override val values: Sequence<OverviewUiState> = sequenceOf(
        // Full state with sample data
        OverviewUiState(
            stats = OverviewStats(
                gamesCount = 127,
                totalPlays = 342,
                playsThisMonth = 18,
                unplayedCount = 23
            ),
            recentPlays = listOf(
                RecentPlay(
                    id = 1,
                    gameName = "Catan",
                    thumbnailUrl = null,
                    dateUiText = uiText("Today, 8:30 PM"),
                    playerCount = 4,
                    playerNamesUiText = uiText("You, Alex, Jordan, Sam")
                ),
                RecentPlay(
                    id = 2,
                    gameName = "Wingspan",
                    thumbnailUrl = null,
                    dateUiText = uiText("Yesterday"),
                    playerCount = 2,
                    playerNamesUiText = uiText("You, Chris")
                ),
                RecentPlay(
                    id = 3,
                    gameName = "7 Wonders Duel",
                    thumbnailUrl = null,
                    dateUiText = uiText("Dec 2"),
                    playerCount = 2,
                    playerNamesUiText = uiText("You, Morgan")
                )
            ),
            recentlyAddedGame = GameHighlight(
                id = 100,
                gameName = "Azul",
                thumbnailUrl = null,
                subtitleUiText = uiText("Recently Added")
            ),
            suggestedGame = GameHighlight(
                id = 101,
                gameName = "Ticket to Ride",
                thumbnailUrl = null,
                subtitleUiText = uiText("Try tonight")
            ),
            lastSyncedUiText = uiText("Last synced: 5 min ago")
        ),
        // Empty state
        OverviewUiState(
            stats = OverviewStats(),
            lastSyncedUiText = uiText("Never synced")
        ),
        // Loading state
        OverviewUiState(
            isLoading = true
        ),
        // Refreshing state
        OverviewUiState(
            stats = OverviewStats(
                gamesCount = 50,
                totalPlays = 100,
                playsThisMonth = 5,
                unplayedCount = 10
            ),
            recentPlays = listOf(
                RecentPlay(
                    id = 1,
                    gameName = "Catan",
                    thumbnailUrl = null,
                    dateUiText = uiText("Today"),
                    playerCount = 4,
                    playerNamesUiText = uiText("You, Alex, Jordan, Sam")
                )
            ),
            isRefreshing = true
        ),
        // Error state
        OverviewUiState(
            stats = OverviewStats(
                gamesCount = 50,
                totalPlays = 100,
                playsThisMonth = 5,
                unplayedCount = 10
            ),
            recentPlays = listOf(
                RecentPlay(
                    id = 1,
                    gameName = "Catan",
                    thumbnailUrl = null,
                    dateUiText = uiText("Today"),
                    playerCount = 4,
                    playerNamesUiText = uiText("You, Alex, Jordan, Sam")
                )
            ),
            errorMessageResId = R.string.sync_plays_failed_error
        )
    )
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun OverviewScreenPreview(
    @PreviewParameter(OverviewUiStatePreviewParameterProvider::class) uiState: OverviewUiState
) {
    MeepleBookTheme {
        OverviewContent(uiState = uiState)
    }
}