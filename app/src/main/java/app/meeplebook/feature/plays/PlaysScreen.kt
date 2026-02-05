package app.meeplebook.feature.plays

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.meeplebook.R
import app.meeplebook.core.plays.model.PlaySyncStatus
import app.meeplebook.core.ui.UiText
import app.meeplebook.core.ui.asString
import app.meeplebook.core.ui.uiText
import app.meeplebook.ui.components.ScreenPadding
import app.meeplebook.ui.components.UiTextText
import app.meeplebook.ui.components.gameImageClip
import app.meeplebook.ui.theme.MeepleBookTheme
import coil3.compose.AsyncImage
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Plays screen entry point that wires the ViewModel to the UI.
 *
 * @param viewModel The PlaysViewModel (injected by Hilt)
 */
@Composable
fun PlaysScreen(
    viewModel: PlaysViewModel = hiltViewModel()
) {
    val currentState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is PlaysUiEffects.NavigateToPlay -> {
                    // Navigation will be handled by parent
                }
                is PlaysUiEffects.ShowSnackbar -> {
                    snackbarState.showSnackbar(effect.messageUiText.asString())
                }
            }
        }
    }

    PlaysScreenRoot(
        currentState = currentState,
        snackbarState = snackbarState,
        onUserAction = viewModel::onEvent
    )
}

@Composable
fun PlaysScreenRoot(
    currentState: PlaysUiState,
    snackbarState: SnackbarHostState,
    onUserAction: (PlaysEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.testTag("playsScreen"),
        snackbarHost = { SnackbarHost(hostState = snackbarState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentState) {
                PlaysUiState.Loading -> PlaysLoadingIndicator()

                is PlaysUiState.Empty -> PlaysScaffoldLayout(
                    commonState = currentState.common,
                    onUserAction = onUserAction
                ) {
                    PlaysEmptyMessage(emptyReason = currentState.reason)
                }

                is PlaysUiState.Error -> PlaysScaffoldLayout(
                    commonState = currentState.common,
                    onUserAction = onUserAction
                ) {
                    PlaysErrorMessage(errorText = currentState.errorMessageUiText)
                }

                is PlaysUiState.Content -> PlaysScaffoldLayout(
                    commonState = currentState.common,
                    onUserAction = onUserAction
                ) {
                    PlaysListContent(
                        sections = currentState.sections,
                        isRefreshing = currentState.common.isRefreshing,
                        onUserAction = onUserAction
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaysLoadingIndicator() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("playsLoadingIndicator"),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(ScreenPadding.ContentPadding))
            Text(
                text = stringResource(R.string.plays_loading),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PlaysScaffoldLayout(
    commonState: PlaysCommonState,
    onUserAction: (PlaysEvent) -> Unit,
    mainContent: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        PlaysStatsDisplay(statistics = commonState.playStats)

        PlaysSearchInput(
            currentQuery = commonState.searchQuery,
            onQueryUpdate = { onUserAction(PlaysEvent.SearchChanged(it)) }
        )

        mainContent()
    }
}

@Composable
private fun PlaysStatsDisplay(
    statistics: PlayStats,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(ScreenPadding.ContentPadding)
            .testTag("playsStatsCard"),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(ScreenPadding.CardInternal),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            SingleStatDisplay(
                numericValue = statistics.uniqueGamesCount.toString(),
                labelText = stringResource(R.string.plays_stat_unique_games)
            )
            SingleStatDisplay(
                numericValue = statistics.totalPlays.toString(),
                labelText = stringResource(R.string.plays_stat_total_plays)
            )
            SingleStatDisplay(
                numericValue = statistics.playsThisYear.toString(),
                labelText = stringResource(R.string.plays_stat_this_year, statistics.currentYear)
            )
        }
    }
}

@Composable
private fun SingleStatDisplay(
    numericValue: String,
    labelText: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = numericValue,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = labelText,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PlaysSearchInput(
    currentQuery: String,
    onQueryUpdate: (String) -> Unit
) {
    TextField(
        value = currentQuery,
        onValueChange = onQueryUpdate,
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        placeholder = { Text(stringResource(R.string.plays_search_placeholder)) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ScreenPadding.ContentPadding)
            .testTag("playsSearchInput"),
        singleLine = true,
        shape = RoundedCornerShape(12.dp)
    )
}

@Composable
private fun PlaysEmptyMessage(emptyReason: EmptyReason) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("playsEmptyState"),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(emptyReason.descriptionResId),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(ScreenPadding.ContentPadding * 2)
        )
    }
}

@Composable
private fun PlaysErrorMessage(errorText: UiText) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("playsErrorState"),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = errorText.asString(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(ScreenPadding.ContentPadding * 2)
        )
    }
}

@Composable
private fun PlaysListContent(
    sections: List<PlaysSection>,
    isRefreshing: Boolean,
    onUserAction: (PlaysEvent) -> Unit
) {
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { onUserAction(PlaysEvent.Refresh) },
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
                item(key = "header_${section.monthYearDate}") {
                    MonthSectionHeader(
                        monthYear = section.monthYearDate,
                        playCount = section.plays.size
                    )
                }

                items(
                    items = section.plays,
                    key = { playItem -> playItem.id }
                ) { playItem ->
                    PlayItemCard(
                        playItem = playItem,
                        onCardTap = { onUserAction(PlaysEvent.PlayClicked(playItem.id)) }
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
private fun PlayItemCard(
    playItem: PlayItem,
    onCardTap: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onCardTap)
            .testTag("playCard_${playItem.id}"),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(ScreenPadding.ItemSpacing)
                .fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .gameImageClip(),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = playItem.thumbnailUrl,
                    contentDescription = playItem.gameName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.width(ScreenPadding.ItemSpacing))

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
                    SyncStatusBadge(syncStatus = playItem.syncStatus)
                }

                UiTextText(
                    text = playItem.dateUiText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                UiTextText(
                    text = playItem.playerSummaryUiText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
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
            }
        }
    }
}

@Composable
private fun SyncStatusBadge(syncStatus: PlaySyncStatus) {
    val (badgeIcon, badgeColor) = when (syncStatus) {
        PlaySyncStatus.SYNCED -> Icons.Default.CheckCircle to Color(0xFF4CAF50)
        PlaySyncStatus.PENDING -> Icons.Default.Schedule to Color(0xFFFF9800)
        PlaySyncStatus.FAILED -> Icons.Default.Error to Color(0xFFF44336)
    }

    Box(
        modifier = Modifier
            .size(20.dp)
            .background(badgeColor.copy(alpha = 0.15f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = badgeIcon,
            contentDescription = syncStatus.name,
            tint = badgeColor,
            modifier = Modifier.size(14.dp)
        )
    }
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
            currentState = previewState,
            snackbarState = remember { SnackbarHostState() },
            onUserAction = {}
        )
    }
}