package app.meeplebook.feature.overview

import android.annotation.SuppressLint
import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.meeplebook.R
import app.meeplebook.core.plays.model.PlayId
import app.meeplebook.core.ui.UiText
import app.meeplebook.core.ui.isNotEmpty
import app.meeplebook.core.ui.uiText
import app.meeplebook.core.ui.uiTextRes
import app.meeplebook.feature.home.navigation.HomeNavigator
import app.meeplebook.feature.overview.effect.OverviewUiEffect
import app.meeplebook.feature.overview.ui.EmptyStateMessage
import app.meeplebook.feature.overview.ui.GameHighlightCard
import app.meeplebook.feature.overview.ui.RecentPlayCard
import app.meeplebook.ui.components.SectionHeader
import app.meeplebook.ui.components.StatItem
import app.meeplebook.ui.components.StatsCard
import app.meeplebook.ui.components.UiTextText
import app.meeplebook.ui.components.screenstates.ErrorState
import app.meeplebook.ui.components.screenstates.LoadingState
import app.meeplebook.ui.theme.MeepleBookTheme

@Composable
fun OverviewScreen(
    viewModel: OverviewViewModel = hiltViewModel(),
    homeNavigator: HomeNavigator
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.onEvent(OverviewEvent.ActionEvent.ScreenOpened)
    }

    LaunchedEffect(viewModel) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                OverviewUiEffect.OpenAddPlay ->
                    homeNavigator.openAddPlay(preselectedGame = null)

                is OverviewUiEffect.NavigateToPlay -> {
                    // TODO: Navigate to play details when route exists.
                }

                is OverviewUiEffect.NavigateToGame -> {
                    // TODO: Navigate to game details when route exists.
                }
            }
        }
    }

    OverviewScreenRoot(
        uiState = uiState,
        onEvent = viewModel::onEvent
    )
}

@Composable
fun OverviewScreenRoot(
    uiState: OverviewUiState,
    onEvent: (OverviewEvent) -> Unit
) {
    when (uiState) {
        OverviewUiState.Loading ->
            LoadingState(loadingMessageUiText = uiTextRes(R.string.loading_message))

        is OverviewUiState.Error ->
            ErrorState(errorMessageUiText = uiState.errorMessageUiText)

        is OverviewUiState.Content ->
            OverviewContent(
                uiState = uiState,
                onEvent = onEvent
            )
    }
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun OverviewContent(
    uiState: OverviewUiState.Content,
    modifier: Modifier = Modifier,
    onEvent: (OverviewEvent) -> Unit
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onEvent(OverviewEvent.ActionEvent.LogPlayClicked) },
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag("logPlayFab")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.log_play_description)
                )
            }
        }
    ) {
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = { onEvent(OverviewEvent.ActionEvent.Refresh) },
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                modifier = modifier.testTag("overviewContent"),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    OverviewStatsCard(
                        stats = uiState.stats,
                        syncStatusUiText = uiState.syncStatusUiText
                    )
                }

                item {
                    SectionHeader(title = stringResource(R.string.recent_activity_title))
                }

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
                        key = { it.playId.localId }
                    ) { play ->
                        RecentPlayCard(
                            play = play,
                            onClick = { onEvent(OverviewEvent.ActionEvent.RecentPlayClicked(play.playId)) }
                        )
                    }
                }

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
                                    onClick = { onEvent(OverviewEvent.ActionEvent.RecentlyAddedClicked(game.id)) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            uiState.suggestedGame?.let { game ->
                                GameHighlightCard(
                                    highlight = game,
                                    onClick = { onEvent(OverviewEvent.ActionEvent.SuggestedGameClicked(game.id)) },
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

@Composable
fun OverviewStatsCard(
    stats: OverviewStats,
    syncStatusUiText: UiText,
) {
    StatsCard(
        modifier = Modifier.testTag("overviewStatsCard")
    ) {
        Text(
            text = stringResource(R.string.your_stats_title),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            StatItem(
                value = stats.gamesCount.toString(),
                label = stringResource(R.string.stat_games)
            )
            StatItem(
                value = stats.totalPlays.toString(),
                label = stringResource(R.string.stat_total_plays)
            )
            StatItem(
                value = stats.playsThisMonth.toString(),
                label = stringResource(R.string.stat_this_month)
            )
            StatItem(
                value = stats.unplayedCount.toString(),
                label = stringResource(R.string.stat_unplayed)
            )
        }
        if (syncStatusUiText.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            UiTextText(
                text = syncStatusUiText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

class OverviewUiStatePreviewParameterProvider : PreviewParameterProvider<OverviewUiState> {
    override val values: Sequence<OverviewUiState> = sequenceOf(
        OverviewUiState.Content(
            stats = OverviewStats(
                gamesCount = 127,
                totalPlays = 342,
                playsThisMonth = 18,
                unplayedCount = 23
            ),
            recentPlays = listOf(
                RecentPlay(
                    playId = PlayId.Local(1L),
                    gameName = "Catan",
                    thumbnailUrl = null,
                    dateUiText = uiText("Today, 8:30 PM"),
                    playerCount = 4,
                    playerNamesUiText = uiText("You, Alex, Jordan, Sam")
                ),
                RecentPlay(
                    playId = PlayId.Local(2L),
                    gameName = "Wingspan",
                    thumbnailUrl = null,
                    dateUiText = uiText("Yesterday"),
                    playerCount = 2,
                    playerNamesUiText = uiText("You, Chris")
                ),
                RecentPlay(
                    playId = PlayId.Local(3L),
                    gameName = "Azul",
                    thumbnailUrl = null,
                    dateUiText = uiText("Dec 1"),
                    playerCount = 3,
                    playerNamesUiText = uiText("You, Taylor, Morgan")
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
            syncStatusUiText = uiText("Last synced: 5 min ago")
        ),
        OverviewUiState.Content(
            stats = OverviewStats(),
            syncStatusUiText = uiText("Never synced")
        ),
        OverviewUiState.Loading,
        OverviewUiState.Content(
            stats = OverviewStats(
                gamesCount = 50,
                totalPlays = 100,
                playsThisMonth = 5,
                unplayedCount = 10
            ),
            recentPlays = listOf(
                RecentPlay(
                    playId = PlayId.Local(1L),
                    gameName = "Catan",
                    thumbnailUrl = null,
                    dateUiText = uiText("Today"),
                    playerCount = 4,
                    playerNamesUiText = uiText("You, Alex, Jordan, Sam")
                )
            ),
            isRefreshing = true
        ),
        OverviewUiState.Error(
            errorMessageUiText = uiText("Sync plays failed")
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
        OverviewScreenRoot(
            uiState = uiState,
            onEvent = {}
        )
    }
}
