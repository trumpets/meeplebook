package app.meeplebook.feature.home

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Casino
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import app.meeplebook.R
import app.meeplebook.ui.theme.MeepleBookTheme

/** Aspect ratio for game thumbnail images (16:9). */
private const val GAME_THUMBNAIL_ASPECT_RATIO = 16f / 9f

/**
 * Represents a navigation destination in the bottom navigation bar.
 */
enum class HomeNavigationDestination(
    val labelResId: Int,
    val icon: ImageVector,
    val contentDescriptionResId: Int
) {
    HOME(R.string.nav_home, Icons.Default.Home, R.string.nav_home),
    COLLECTION(R.string.nav_collection, Icons.Default.CollectionsBookmark, R.string.nav_collection),
    PLAYS(R.string.nav_plays, Icons.Default.BarChart, R.string.nav_plays),
    PROFILE(R.string.nav_profile, Icons.Default.Person, R.string.nav_profile)
}

/**
 * HomeScreen entry point that wires the ViewModel to the UI.
 *
 * @param viewModel The HomeViewModel (injected by Hilt)
 * @param shouldRefresh Flag to trigger immediate refresh (e.g., after login)
 */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    shouldRefresh: Boolean = false
) {
    val uiState by viewModel.uiState.collectAsState()

    // Trigger refresh when shouldRefresh is true (e.g., after login)
    LaunchedEffect(shouldRefresh) {
        if (shouldRefresh) {
            viewModel.refresh()
        }
    }

    HomeScreenContent(
        uiState = uiState,
        onRefresh = { viewModel.refresh() }
        // Top bar buttons (profile, more) do nothing - ignore them as per requirements
        // Bottom navigation tabs do nothing - ignore them as per requirements
        // Game highlights do nothing - ignore them as per requirements
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenContent(
    uiState: HomeUiState,
    selectedNavItem: HomeNavigationDestination = HomeNavigationDestination.HOME,
    onNavItemClick: (HomeNavigationDestination) -> Unit = {},
    onLogPlayClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onMoreClick: () -> Unit = {},
    onRecentPlayClick: (RecentPlay) -> Unit = {},
    onRecentlyAddedClick: () -> Unit = {},
    onSuggestedGameClick: () -> Unit = {},
    onRefresh: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(
                        onClick = onProfileClick,
                        modifier = Modifier.testTag("profileButton")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = stringResource(R.string.profile_action_description)
                        )
                    }
                    IconButton(
                        onClick = onMoreClick,
                        modifier = Modifier.testTag("moreButton")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = stringResource(R.string.more_options_description)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
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
        },
        bottomBar = {
            NavigationBar {
                HomeNavigationDestination.entries.forEach { destination ->
                    NavigationBarItem(
                        selected = selectedNavItem == destination,
                        onClick = { onNavItemClick(destination) },
                        icon = {
                            Icon(
                                imageVector = destination.icon,
                                contentDescription = stringResource(destination.contentDescriptionResId)
                            )
                        },
                        label = { Text(stringResource(destination.labelResId)) }
                    )
                }
            }
        }
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (uiState.isLoading) {
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
                            text = stringResource(R.string.loading_message),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("homeContent"),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Quick Stats Card
                    item {
                        StatsCard(
                            stats = uiState.stats,
                            lastSyncedText = uiState.lastSyncedText
                        )
                    }

                    // Recent Activity Section
                    item {
                        Text(
                            text = stringResource(R.string.recent_activity_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
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
                            Text(
                                text = stringResource(R.string.collection_highlights_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
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

@Composable
private fun StatsCard(
    stats: HomeStats,
    lastSyncedText: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("statsCard"),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
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
            if (lastSyncedText.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = lastSyncedText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StatItem(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EmptyStateMessage(
    message: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun RecentPlayCard(
    play: RecentPlay,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .testTag("recentPlayCard_${play.id}"),
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
                    .size(56.dp)
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
                    text = play.gameName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(R.string.play_date_players, play.dateText, play.playerCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = play.playerNames,
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
private fun GameHighlightCard(
    highlight: GameHighlight,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.testTag("highlightCard_${highlight.id}"),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            // Game thumbnail placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(GAME_THUMBNAIL_ASPECT_RATIO)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Casino,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = highlight.gameName,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = highlight.subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Provides preview parameter states for [HomeScreenContent]:
 * 1. Default state with sample data
 * 2. Empty state
 * 3. Loading state
 * 4. Refreshing state
 */
class HomeUiStatePreviewParameterProvider : PreviewParameterProvider<HomeUiState> {
    override val values: Sequence<HomeUiState> = sequenceOf(
        // Full state with sample data
        HomeUiState(
            stats = HomeStats(
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
                    dateText = "Today, 8:30 PM",
                    playerCount = 4,
                    playerNames = "You, Alex, Jordan, Sam"
                ),
                RecentPlay(
                    id = 2,
                    gameName = "Wingspan",
                    thumbnailUrl = null,
                    dateText = "Yesterday",
                    playerCount = 2,
                    playerNames = "You, Chris"
                ),
                RecentPlay(
                    id = 3,
                    gameName = "7 Wonders Duel",
                    thumbnailUrl = null,
                    dateText = "Dec 2",
                    playerCount = 2,
                    playerNames = "You, Morgan"
                )
            ),
            recentlyAddedGame = GameHighlight(
                id = 100,
                gameName = "Azul",
                thumbnailUrl = null,
                subtitle = "Added 2 days ago"
            ),
            suggestedGame = GameHighlight(
                id = 101,
                gameName = "Ticket to Ride",
                thumbnailUrl = null,
                subtitle = "Try Tonight?"
            ),
            lastSyncedText = "Last synced: 5 min ago"
        ),
        // Empty state
        HomeUiState(
            stats = HomeStats(),
            lastSyncedText = "Never synced"
        ),
        // Loading state
        HomeUiState(
            isLoading = true
        ),
        // Refreshing state
        HomeUiState(
            stats = HomeStats(
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
                    dateText = "Today",
                    playerCount = 4,
                    playerNames = "You, Alex, Jordan, Sam"
                )
            ),
            isRefreshing = true
        )
    )
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun HomeScreenPreview(
    @PreviewParameter(HomeUiStatePreviewParameterProvider::class) uiState: HomeUiState
) {
    MeepleBookTheme {
        HomeScreenContent(uiState = uiState)
    }
}
