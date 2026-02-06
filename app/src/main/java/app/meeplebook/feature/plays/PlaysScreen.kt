package app.meeplebook.feature.plays

import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.meeplebook.R
import app.meeplebook.core.plays.model.PlaySyncStatus
import app.meeplebook.core.ui.asString
import app.meeplebook.core.ui.uiText
import app.meeplebook.core.ui.uiTextJoin
import app.meeplebook.core.ui.uiTextRes
import app.meeplebook.ui.components.RowItemImage
import app.meeplebook.ui.components.ScreenPadding
import app.meeplebook.ui.components.SearchBar
import app.meeplebook.ui.components.StatItem
import app.meeplebook.ui.components.StatsCard
import app.meeplebook.ui.components.UiTextText
import app.meeplebook.ui.components.screenstates.EmptyState
import app.meeplebook.ui.components.screenstates.ErrorState
import app.meeplebook.ui.components.screenstates.LoadingState
import app.meeplebook.ui.theme.Green500
import app.meeplebook.ui.theme.MeepleBookTheme
import app.meeplebook.ui.theme.Orange500
import app.meeplebook.ui.theme.Red500
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun PlaysScreen(
    viewModel: PlaysViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val context = LocalContext.current

    // TODO Consistent padding between screen contents. Overview list is a bit narrower than collections list

    val resources = LocalResources.current

    LaunchedEffect(viewModel) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {

                is PlaysUiEffects.NavigateToPlay -> {
//                    onNavigateToPlay(effect.playId)
                }

                is PlaysUiEffects.ShowSnackbar -> {
                    Toast.makeText(context, effect.messageUiText.asString(resources), Toast.LENGTH_SHORT).show()
//                    scaffoldState.snackbarHostState.showSnackbar(stringResource(effect.messageResId))
                }
            }
        }
    }

    PlaysScreenRoot(
        uiState = uiState,
        onEvent = { viewModel.onEvent(it) }
    )
}

@Composable
fun PlaysScreenRoot(
    uiState: PlaysUiState,
    onEvent: (PlaysEvent) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("playsScreen")
    ) {
        when (uiState) {
            PlaysUiState.Loading ->
                LoadingState(loadingMessageUiText = uiTextRes(R.string.plays_loading))

            is PlaysUiState.Empty ->
                PlaysScaffold(
                    commonState = uiState.common,
                    onEvent = onEvent
                ) {
                    EmptyState(reasonMessageUiText = uiTextRes(uiState.reason.descriptionResId))
                }

            is PlaysUiState.Error ->
                PlaysScaffold(
                    commonState = uiState.common,
                    onEvent = onEvent
                ) {
                    ErrorState(uiState.errorMessageUiText)
                }

            is PlaysUiState.Content ->
                PlaysScaffold(
                    commonState = uiState.common,
                    onEvent = onEvent
                ) {
                    PlaysScreenContent(
                        sections = uiState.sections,
                        isRefreshing = uiState.common.isRefreshing,
                        onEvent = onEvent
                    )
                }
        }
    }
}

@Composable
private fun PlaysScaffold(
    commonState: PlaysCommonState,
    onEvent: (PlaysEvent) -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        PlaysStatsCard(stats = commonState.playStats)

        SearchBar(
            query = commonState.searchQuery,
            placeholderResId = R.string.plays_search_placeholder,
            onQueryChanged = { onEvent(PlaysEvent.SearchChanged(it)) }
        )

        content()
    }
}

@Composable
fun PlaysStatsCard(
    stats: PlayStats
) {
    StatsCard(
        modifier = Modifier.padding(ScreenPadding.ContentPadding)
            .testTag("playsStatsCard")
    ) {
        Text(
            text = stringResource(R.string.plays_stats_all_time),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            StatItem(
                value = stats.totalPlays.toString(),
                label = stringResource(R.string.plays_stat_total_plays)
            )
            StatItem(
                value = stats.playsThisYear.toString(),
                label = stringResource(R.string.plays_stat_this_year, stats.currentYear)
            )
            StatItem(
                value = stats.uniqueGamesCount.toString(),
                label = stringResource(R.string.plays_stat_unique_games)
            )
        }
        // TODO maybe show Up to Date here or Unsynced 5 lets say
//        if (lastSyncedUiText.isNotEmpty()) {
//            Spacer(modifier = Modifier.height(12.dp))
//            UiTextText(
//                text = lastSyncedUiText,
//                style = MaterialTheme.typography.bodySmall,
//                color = MaterialTheme.colorScheme.onSurfaceVariant
//            )
//        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun PlaysScreenContent(
    sections: List<PlaysSection>,
    isRefreshing: Boolean,
    onEvent: (PlaysEvent) -> Unit
) {
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { onEvent(PlaysEvent.Refresh) },
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("playsListContent"),
            contentPadding = PaddingValues(ScreenPadding.ContentPadding),
            verticalArrangement = Arrangement.spacedBy(ScreenPadding.Small)
        ) {
            sections.forEach { section ->
                stickyHeader(key = "header_${section.monthYearDate}") {
                    MonthSectionHeader(
                        monthYear = section.monthYearDate,
                        playCount = section.plays.size
                    )
                }

                items(
                    items = section.plays,
                    key = { it.id }
                ) { playItem ->
                    PlayItemRow(
                        playItem = playItem,
                        onClick = { onEvent(PlaysEvent.PlayClicked(playItem.id)) }
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthSectionHeader(
    monthYear: YearMonth,
    playCount: Int
) {
    val formattedMonth = remember(monthYear) {
        monthYear.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()))
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = ScreenPadding.Small)
            .testTag("monthHeader_${monthYear}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = formattedMonth,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = pluralStringResource(
                    R.plurals.plays_section_count,
                    playCount,
                    playCount
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        HorizontalDivider(modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun PlayItemRow(
    playItem: PlayItem,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(ScreenPadding.ItemSpacing)
            .testTag("playCard_${playItem.id}"),
        verticalAlignment = Alignment.Top
    ) {
        RowItemImage(
            thumbnailUrl = playItem.thumbnailUrl,
            contentDescription = playItem.gameName
        )

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = playItem.gameName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (playItem.syncStatus != PlaySyncStatus.SYNCED) {
                    SyncStatusBadge(syncStatus = playItem.syncStatus)
                }
            }

            UiTextText(
                text = uiTextJoin(
                    separator = " • ",
                    playItem.dateUiText,
                    playItem.durationUiText
                ),
                style = MaterialTheme.typography.bodySmall,
            )

            UiTextText(
                text = playItem.playerSummaryUiText,
                style = MaterialTheme.typography.bodySmall,
            )

            playItem.location?.let { loc ->
                Text(
                    text = loc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            playItem.comments?.let { comments ->
                Text(
                    text = comments,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun SyncStatusBadge(syncStatus: PlaySyncStatus) {
    val badgeColor = when (syncStatus) {
        PlaySyncStatus.SYNCED -> Green500
        PlaySyncStatus.PENDING -> Orange500
        PlaySyncStatus.FAILED -> Red500
    }

    Box(
        modifier = Modifier
            .size(6.dp)
            .background(badgeColor, CircleShape)
    )
}

/**
 * Preview parameter provider for PlaysScreen states.
 */
class PlaysUiStatePreviewProvider : PreviewParameterProvider<PlaysUiState> {
    override val values: Sequence<PlaysUiState> = sequenceOf(
        PlaysUiState.Loading,
        createSampleContentState(),
        PlaysUiState.Empty(
            reason = EmptyReason.NO_PLAYS,
            common = PlaysCommonState(playStats = samplePlayStats())
        ),
        PlaysUiState.Empty(
            reason = EmptyReason.NO_SEARCH_RESULTS,
            common = PlaysCommonState(
                searchQuery = "monopoly",
                playStats = samplePlayStats()
            )
        ),
        createSampleContentState(isRefreshing = true)
    )

    private fun createSampleContentState(isRefreshing: Boolean = false): PlaysUiState.Content {
        val samplePlays = listOf(
            PlayItem(
                id = 1L,
                gameName = "Catan",
                thumbnailUrl = null,
                dateUiText = uiText("27/01/2026"),
                durationUiText = uiText("90min"),
                playerSummaryUiText = uiText("4 players: Alice, Bob, Charlie, You"),
                location = "Home",
                comments = null,
                syncStatus = PlaySyncStatus.SYNCED
            ),
            PlayItem(
                id = 2L,
                gameName = "Wingspan",
                thumbnailUrl = null,
                dateUiText = uiText("25/01/2026"),
                durationUiText = uiText("75min"),
                playerSummaryUiText = uiText("2 players: You, Partner"),
                location = "Game Café",
                comments = "Great session!",
                syncStatus = PlaySyncStatus.PENDING
            ),
            PlayItem(
                id = 3L,
                gameName = "Azul",
                thumbnailUrl = null,
                dateUiText = uiText("15/12/2025"),
                durationUiText = uiText("45min"),
                playerSummaryUiText = uiText("3 players"),
                location = null,
                comments = null,
                syncStatus = PlaySyncStatus.FAILED
            )
        )

        val sections = listOf(
            PlaysSection(
                monthYearDate = YearMonth.of(2026, 1),
                plays = samplePlays.take(2)
            ),
            PlaysSection(
                monthYearDate = YearMonth.of(2025, 12),
                plays = samplePlays.drop(2)
            )
        )

        return PlaysUiState.Content(
            sections = sections,
            common = PlaysCommonState(
                playStats = samplePlayStats(),
                isRefreshing = isRefreshing
            )
        )
    }

    private fun samplePlayStats() = PlayStats(
        uniqueGamesCount = 42,
        totalPlays = 187,
        playsThisYear = 23,
        currentYear = 2026
    )
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PlaysScreenPreview(
    @PreviewParameter(PlaysUiStatePreviewProvider::class) previewState: PlaysUiState
) {
    MeepleBookTheme {
        PlaysScreenRoot(
            uiState = previewState,
            onEvent = {}
        )
    }
}