package app.meeplebook.feature.plays

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
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
import app.meeplebook.core.ui.asString
import app.meeplebook.core.ui.uiText
import app.meeplebook.core.ui.uiTextRes
import app.meeplebook.ui.components.UiTextText
import app.meeplebook.ui.components.gameImageClip
import app.meeplebook.ui.theme.MeepleBookTheme
import coil3.compose.AsyncImage
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun PlaysScreen(
    viewModel: PlaysViewModel = hiltViewModel()
) {
    val currentState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHost = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is PlaysUiEffects.NavigateToPlay -> {
                    // TODO: Navigate to play details
                }
                is PlaysUiEffects.ShowSnackbar -> {
                    snackbarHost.showSnackbar(effect.messageUiText.asString())
                }
            }
        }
    }

    PlaysContent(
        state = currentState,
        onEventDispatched = viewModel::onEvent
    )
}

@Composable
fun PlaysContent(
    state: PlaysUiState,
    onEventDispatched: (PlaysEvent) -> Unit = {}
) {
    PullToRefreshBox(
        isRefreshing = state.common.isRefreshing,
        onRefresh = { onEventDispatched(PlaysEvent.Refresh) },
        modifier = Modifier
            .fillMaxSize()
            .testTag("playsContent")
    ) {
        when (state) {
            is PlaysUiState.Loading -> {
                Box(
                    Modifier
                        .fillMaxSize()
                        .testTag("loadingBox"),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is PlaysUiState.Empty -> {
                Column(Modifier.fillMaxSize()) {
                    Spacer(Modifier.height(16.dp))
                    buildStatsCard(state.common.playStats)
                    Spacer(Modifier.height(16.dp))
                    buildSearchBox(
                        state.common.searchQuery,
                        { onEventDispatched(PlaysEvent.SearchChanged(it)) }
                    )
                    Spacer(Modifier.height(32.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(32.dp)
                            .testTag("emptyBox"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            stringResource(state.reason.descriptionResId),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            is PlaysUiState.Content -> {
                LazyColumn(
                    Modifier
                        .fillMaxSize()
                        .testTag("contentList"),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    item("statsCard") {
                        buildStatsCard(state.common.playStats)
                        Spacer(Modifier.height(16.dp))
                    }
                    item("searchBox") {
                        buildSearchBox(
                            state.common.searchQuery,
                            { onEventDispatched(PlaysEvent.SearchChanged(it)) }
                        )
                        Spacer(Modifier.height(16.dp))
                    }
                    items(state.sections, key = { it.monthYearDate.toString() }) { section ->
                        buildMonthSection(
                            section,
                            { onEventDispatched(PlaysEvent.PlayClicked(it)) }
                        )
                    }
                }
            }
            is PlaysUiState.Error -> {
                Column(Modifier.fillMaxSize()) {
                    Spacer(Modifier.height(16.dp))
                    buildStatsCard(state.common.playStats)
                    Spacer(Modifier.height(16.dp))
                    buildSearchBox(
                        state.common.searchQuery,
                        { onEventDispatched(PlaysEvent.SearchChanged(it)) }
                    )
                    Spacer(Modifier.height(32.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(32.dp)
                            .testTag("errorBox"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            state.errorMessageUiText.asString(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun buildStatsCard(stats: PlayStats) {
    Card(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .testTag("statsCard"),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                stringResource(R.string.plays_stats_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(16.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(
                    Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        stats.totalPlays.toString(),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        stringResource(R.string.plays_stats_total),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(
                    Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        stats.playsThisYear.toString(),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        stringResource(R.string.plays_stats_this_year, stats.currentYear),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(
                    Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        stats.uniqueGamesCount.toString(),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        stringResource(R.string.plays_stats_unique),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun buildSearchBox(
    searchText: String,
    onSearchChange: (String) -> Unit
) {
    val kbCtrl = LocalSoftwareKeyboardController.current
    
    OutlinedTextField(
        value = searchText,
        onValueChange = onSearchChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .testTag("searchBox"),
        leadingIcon = {
            Icon(
                Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        placeholder = { Text(stringResource(R.string.plays_search_hint)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { kbCtrl?.hide() }),
        shape = MaterialTheme.shapes.medium
    )
}

@Composable
private fun buildMonthSection(
    section: PlaysSection,
    onPlayClick: (Long) -> Unit
) {
    val monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
    val monthText = section.monthYearDate.format(monthFormatter)
    
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .testTag("monthHeader"),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                monthText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                stringResource(R.string.plays_section_count, section.plays.size),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant,
            thickness = 1.dp
        )
        Spacer(Modifier.height(8.dp))
        
        section.plays.forEach { play ->
            buildPlayCard(play, { onPlayClick(play.id) })
            Spacer(Modifier.height(4.dp))
        }
        
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun buildPlayCard(
    play: PlayItem,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .testTag("playCard_${play.id}"),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            Modifier
                .background(MaterialTheme.colorScheme.surface)
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                Modifier
                    .size(72.dp)
                    .gameImageClip(),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = play.thumbnailUrl,
                    contentDescription = play.gameName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    play.gameName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Row {
                    UiTextText(
                        play.dateUiText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        " • ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    UiTextText(
                        play.durationUiText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(2.dp))
                UiTextText(
                    play.playerSummaryUiText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                play.location?.takeIf { it.isNotBlank() }?.let { loc ->
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "📍 $loc",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                play.comments?.takeIf { it.isNotBlank() }?.let { comment ->
                    Spacer(Modifier.height(2.dp))
                    Text(
                        comment,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            val (icon, color) = when (play.syncStatus) {
                PlaySyncStatus.SYNCED -> Icons.Default.CheckCircle to androidx.compose.ui.graphics.Color(0xFF4CAF50)
                PlaySyncStatus.PENDING -> Icons.Default.Schedule to androidx.compose.ui.graphics.Color(0xFFFF9800)
                PlaySyncStatus.FAILED -> Icons.Default.Error to androidx.compose.ui.graphics.Color(0xFFF44336)
            }
            Box(
                Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = play.syncStatus.name,
                    tint = color,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

class PlaysPreviewProvider : PreviewParameterProvider<PlaysUiState> {
    override val values = sequenceOf(
        PlaysUiState.Loading,
        PlaysUiState.Empty(
            reason = EmptyReason.NO_PLAYS,
            common = PlaysCommonState(playStats = PlayStats(0, 0, 0, 2026))
        ),
        PlaysUiState.Content(
            sections = listOf(
                PlaysSection(
                    YearMonth.of(2026, 1),
                    listOf(
                        PlayItem(
                            1, "Wingspan", null,
                            uiText("27/01/2026"), uiText("90 min"),
                            uiText("3 players: Alice, Bob, Charlie"),
                            "Home", "Great game!", PlaySyncStatus.SYNCED
                        ),
                        PlayItem(
                            2, "Ticket to Ride", null,
                            uiText("26/01/2026"), uiText("60 min"),
                            uiText("4 players"), null, null, PlaySyncStatus.PENDING
                        )
                    )
                )
            ),
            common = PlaysCommonState(playStats = PlayStats(89, 342, 127, 2026))
        ),
        PlaysUiState.Error(
            uiTextRes(R.string.sync_plays_failed_error),
            PlaysCommonState()
        )
    )
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PlaysScreenPreviews(@PreviewParameter(PlaysPreviewProvider::class) state: PlaysUiState) {
    MeepleBookTheme {
        PlaysContent(state = state)
    }
}