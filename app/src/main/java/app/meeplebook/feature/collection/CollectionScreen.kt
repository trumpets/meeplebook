package app.meeplebook.feature.collection

import android.content.res.Configuration
import androidx.annotation.StringRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
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
import app.meeplebook.core.collection.model.CollectionSort
import app.meeplebook.core.collection.model.QuickFilter
import app.meeplebook.ui.components.gameImageClip
import app.meeplebook.ui.theme.MeepleBookTheme
import coil3.compose.AsyncImage

/**
 * Collection screen entry point that wires the ViewModel to the UI.
 *
 * @param viewModel The CollectionViewModel (injected by Hilt)
 */
@Composable
fun CollectionScreen(
    viewModel: CollectionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val gridState = rememberLazyGridState()

    LaunchedEffect(viewModel) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {

                is CollectionUiEffects.ScrollToLetter -> {
                    val content = uiState as? CollectionUiState.Content ?: return@collect
                    val index = content.sectionIndices[effect.letter] ?: return@collect

                    when (content.viewMode) {
                        CollectionViewMode.LIST ->
                            listState.animateScrollToItem(index)

                        CollectionViewMode.GRID ->
                            gridState.animateScrollToItem(index)
                    }
                }

                is CollectionUiEffects.NavigateToGame -> {
//                    onNavigateToGame(effect.gameId)
                }

                CollectionUiEffects.OpenSortSheet -> {
                    // showModalBottomSheet()
                }

                CollectionUiEffects.DismissSortSheet -> {
                    // hideModalBottomSheet()
                }
            }
        }
    }

    CollectionScreenRoot(
        uiState = uiState,
        onEvent = { viewModel.onEvent(it) },
        listState = listState,
        gridState = gridState
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionScreenRoot(
    uiState: CollectionUiState,
    onEvent: (CollectionEvent) -> Unit,
    listState: LazyListState,
    gridState: LazyGridState
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("collectionScreen")
    ) {
        when (uiState) {
            CollectionUiState.Loading ->
                LoadingState()

            is CollectionUiState.Empty ->
                CollectionScaffold(
                    uiState = uiState,
                    onEvent = onEvent
                ) {
                    EmptyState(reason = uiState.reason)
                }

            is CollectionUiState.Error ->
                CollectionScaffold(
                    uiState = uiState,
                    onEvent = onEvent
                ) {
                    ErrorState(uiState.errorMessageResId)
                }

            is CollectionUiState.Content ->
                CollectionScaffold(
                    uiState = uiState,
                    onEvent = onEvent
                ) {
                    CollectionScreenContent(
                        uiState = uiState,
                        onEvent = onEvent,
                        listState = listState,
                        gridState = gridState
                    )
                }
        }
    }
}

@Composable
fun LoadingState() {
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

@Composable
private fun CollectionScaffold(
    uiState: CollectionUiState,
    onEvent: (CollectionEvent) -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Box {
        Column {

            /* --- SEARCH (always visible) --- */
            SearchBar(
                query = uiState.searchQuery,
                onQueryChanged = { onEvent(CollectionEvent.SearchChanged(it)) }

            )

            /* --- QUICK FILTERS (always visible) --- */
            QuickFiltersRow(
                state = uiState,
                onFilterSelected = { onEvent(CollectionEvent.QuickFilterSelected(it)) }
            )

            content()
        }
    }
}

@Composable
fun EmptyState(reason: EmptyReason) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("emptyState"),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(reason.descriptionResId),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(32.dp)
        )
    }
}

@Composable
fun ErrorState(@StringRes errorMessageResId: Int) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("errorState"),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(errorMessageResId),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(32.dp)
        )
    }
}

/* ---------- ROOT ---------- */

@Composable
fun CollectionScreenContent(
    uiState: CollectionUiState.Content,
    onEvent: (CollectionEvent) -> Unit,
    listState: LazyListState,
    gridState: LazyGridState
) {
    Box {
        Column {
            CollectionToolbar(
                selectedViewMode = uiState.viewMode,
                onViewModeChanged = {
                    onEvent(CollectionEvent.ViewModeSelected(it))
                },
                onSortClicked = {
                    onEvent(CollectionEvent.OpenSortSheet)
                }
            )

            CollectionContent(
                state = uiState,
                onEvent = onEvent,
                listState = listState,
                gridState = gridState
            )
        }

        if (uiState.showAlphabetJump) {
            AlphabetJumpBar(
                onLetterSelected = {
                    onEvent(CollectionEvent.JumpToLetter(it))
                }
            )
        }
    }

    if (uiState.isSortSheetVisible) {
        SortBottomSheet(
            uiState = uiState,
            onDismiss = { onEvent(CollectionEvent.DismissSortSheet) },
            onSortSelected = {
                onEvent(CollectionEvent.SortSelected(it))
            }
        )
    }
}

/* ---------- SEARCH ---------- */

@Composable
private fun SearchBar(
    query: String,
    onQueryChanged: (String) -> Unit
) {
    TextField(
        value = query,
        onValueChange = onQueryChanged,
        leadingIcon = { Icon(Icons.Default.Search, null) },
        placeholder = { Text(stringResource(R.string.collection_search_games)) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
            .testTag("collectionSearchField"),
        singleLine = true,
        shape = RoundedCornerShape(12.dp)
    )
}

/* ---------- QUICK FILTERS ---------- */

@Composable
private fun QuickFiltersRow(
    state: CollectionUiState,
    onFilterSelected: (QuickFilter) -> Unit
) {
    LazyRow(
        modifier = Modifier.padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // TODO: I don't like this. it's too hardcoded for my taste
        item {
            FilterChip(
                selected = state.activeQuickFilter == QuickFilter.ALL,
                onClick = { onFilterSelected(QuickFilter.ALL) },
                label = { Text(stringResource(R.string.collection_filter_all, state.totalGameCount)) }
            )
        }

        item {
            FilterChip(
                selected = state.activeQuickFilter == QuickFilter.UNPLAYED,
                onClick = { onFilterSelected(QuickFilter.UNPLAYED) },
                label = { Text(stringResource(R.string.collection_filter_unplayed, state.unplayedGameCount)) }
            )
        }
    }
}

/* ---------- TOOLBAR ---------- */

@Composable
private fun CollectionToolbar(
    selectedViewMode: CollectionViewMode,
    onViewModeChanged: (CollectionViewMode) -> Unit,
    onSortClicked: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        val selectedColor = MaterialTheme.colorScheme.primary
        val unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(
                onClick = { onViewModeChanged(CollectionViewMode.LIST) },
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = if (selectedViewMode == CollectionViewMode.LIST) selectedColor else unselectedColor
                )
            ) {
                Icon(Icons.AutoMirrored.Outlined.ViewList, contentDescription = stringResource(id = R.string.collection_view_list))
            }
            IconButton(
                onClick = { onViewModeChanged(CollectionViewMode.GRID) },
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = if (selectedViewMode == CollectionViewMode.GRID) selectedColor else unselectedColor
                )
            ) {
                Icon(Icons.Outlined.GridView, contentDescription = stringResource(id = R.string.collection_view_grid))
            }
        }

        IconButton(onClick = onSortClicked) {
            Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = stringResource(id = R.string.collection_sort))
        }
    }
}

/* ---------- CONTENT ---------- */

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CollectionContent(
    state: CollectionUiState.Content,
    onEvent: (CollectionEvent) -> Unit,
    listState: LazyListState,
    gridState: LazyGridState
) {
    when (state.viewMode) {
        CollectionViewMode.GRID -> CollectionGrid(state, onEvent, gridState)
        CollectionViewMode.LIST -> CollectionList(state, onEvent, listState)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CollectionGrid(
    state: CollectionUiState.Content,
    onEvent: (CollectionEvent) -> Unit,
    gridState: LazyGridState
) {
    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        state.sections.forEach { section ->
            item(key = section.key, span = { GridItemSpan(2) }) {
                SectionHeader(section.key)
            }

            items(section.games, key = { it.gameId }) { game ->
                GameGridCard(
                    game = game,
                    onClick = {
                        onEvent(CollectionEvent.GameClicked(game.gameId))
                    },
                    onLogPlay = {
                        onEvent(CollectionEvent.LogPlayClicked(game.gameId))
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CollectionList(
    state: CollectionUiState.Content,
    onEvent: (CollectionEvent) -> Unit,
    listState: LazyListState
) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        state.sections.forEach { section ->
            stickyHeader(key = section.key) {
                SectionHeader(section.key)
            }

            items(section.games, key = { it.gameId }) { game ->
                GameListRow(
                    game = game,
                    onClick = {
                        onEvent(CollectionEvent.GameClicked(game.gameId))
                    },
                    onLogPlay = {
                        onEvent(CollectionEvent.LogPlayClicked(game.gameId))
                    }
                )
            }
        }
    }
}

/* ---------- GAME CARDS ---------- */

@Composable
private fun GameGridCard(
    game: CollectionGameItem,
    onClick: () -> Unit,
    onLogPlay: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Box(
                modifier = Modifier
                    .height(120.dp)
                    .fillMaxWidth()
                    .gameImageClip(),
            ) {
                AsyncImage(
                    model = game.thumbnailUrl,
                    contentDescription = game.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )

                if (game.isUnplayed) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(6.dp)
                            .align(Alignment.TopStart)
                            .background(
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(2.dp)
                    )
                }
            }

            Text(
                game.name,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                listOfNotNull(
                    game.yearPublished?.toString(),
                    game.playersSubtitle
                ).joinToString(separator = " • ")
            )

            IconButton(
                onClick = onLogPlay,
                modifier = Modifier.align(Alignment.End)
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(id = R.string.collection_log_play))
            }
        }
    }
}

@Composable
private fun GameListRow(
    game: CollectionGameItem,
    onClick: () -> Unit,
    onLogPlay: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .gameImageClip(),
        ) {
            AsyncImage(
                model = game.thumbnailUrl,
                contentDescription = game.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = game.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = listOfNotNull(
                    game.yearPublished?.toString(),
                    game.playersSubtitle,
                    game.playTimeSubtitle
                ).joinToString(separator = " • "),
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = game.playsSubtitle,
                style = MaterialTheme.typography.bodySmall
            )
        }

        IconButton(onClick = onLogPlay) {
            Icon(Icons.Default.Add, contentDescription = stringResource(id = R.string.collection_log_play))
        }
    }
}

/* ---------- MISC ---------- */

@Composable
private fun SectionHeader(letter: Char) {
    Text(
        text = letter.toString(),
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = 4.dp)
            .testTag("sectionHeader_${letter}"),
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun BoxScope.AlphabetJumpBar(
    onLetterSelected: (Char) -> Unit
) {
    val letters = listOf('#') + ('A'..'Z').toList()

    Column(
        modifier = Modifier
            .align(Alignment.CenterEnd)
            .padding(end = 4.dp),
        verticalArrangement = Arrangement.Center
    ) {
        letters.forEach {
            Text(
                it.toString(),
                modifier = Modifier
                    .padding(2.dp)
                    .clickable { onLetterSelected(it) }
            )
        }
    }
}

/* ---------- SORT SHEET ---------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SortBottomSheet(
    uiState: CollectionUiState.Content,
    onDismiss: () -> Unit,
    onSortSelected: (CollectionSort) -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        val current = uiState.sort
        uiState.availableSortOptions.forEach { option ->
            ListItem(
                headlineContent = { Text(option.name) },
                trailingContent = {
                    if (option == current) {
                        Icon(Icons.Default.Star, null)
                    }
                },
                modifier = Modifier.clickable {
                    onSortSelected(option)
                    onDismiss()
                }
            )
        }
    }
}

/**
 * Provides preview parameter states for [CollectionScreenContent]:
 * 1. Default content state with sample data
 * 2. Empty state
 * 3. Loading state
 * 4. Content state with refreshing flag set
 */
class CollectionUiStatePreviewParameterProvider : PreviewParameterProvider<CollectionUiState> {
    override val values: Sequence<CollectionUiState> = sequenceOf(
        sampleContentState(),
        sampleContentState(viewMode = CollectionViewMode.LIST, isSortSheetVisible = true),
        CollectionUiState.Empty(
            reason = EmptyReason.NO_SEARCH_RESULTS,
            searchQuery = "search term",
            activeQuickFilter = QuickFilter.ALL,
            totalGameCount = 100,
            unplayedGameCount = 27,
            isRefreshing = false
        ),
        CollectionUiState.Loading,
        sampleContentState(isRefreshing = true),
        CollectionUiState.Error(
            R.string.sync_collections_failed_error,
            searchQuery = "azul",
            activeQuickFilter = QuickFilter.ALL,
            totalGameCount = 100,
            unplayedGameCount = 27,
            isRefreshing = false
        )
    )

    private fun sampleContentState(
        viewMode: CollectionViewMode = CollectionViewMode.GRID,
        isRefreshing: Boolean = false,
        isSortSheetVisible: Boolean = false
    ): CollectionUiState.Content {
        val games = sampleGames()
        return CollectionUiState.Content(
            searchQuery = "",
            viewMode = viewMode,
            sort = CollectionSort.ALPHABETICAL,
            activeQuickFilter = QuickFilter.ALL,
            availableSortOptions = CollectionSort.entries,
            sections = buildSections(games),
            sectionIndices = LinkedHashMap(),
            totalGameCount = games.size.toLong(),
            unplayedGameCount = games.size - 3L,
            isRefreshing = isRefreshing,
            showAlphabetJump = true,
            isSortSheetVisible = isSortSheetVisible
        )
    }

    private fun sampleGames(): List<CollectionGameItem> = listOf(
        CollectionGameItem(
            gameId = 1,
            name = "Catan",
            yearPublished = 1995,
            thumbnailUrl = null,
            playsSubtitle = "42 plays",
            playersSubtitle = "3–4p",
            playTimeSubtitle = "75 min",
            isUnplayed = false
        ),
        CollectionGameItem(
            gameId = 2,
            name = "Wingspan",
            yearPublished = 2019,
            thumbnailUrl = null,
            playsSubtitle = "18 plays",
            playersSubtitle = "1–5p",
            playTimeSubtitle = "90 min",
            isUnplayed = false
        ),
        CollectionGameItem(
            gameId = 3,
            name = "Abyss",
            yearPublished = 2015,
            thumbnailUrl = null,
            playsSubtitle = "25 plays",
            playersSubtitle = "2p",
            playTimeSubtitle = "30 min",
            isUnplayed = false
        ),
        CollectionGameItem(
            gameId = 4,
            name = "Azul",
            yearPublished = 2017,
            thumbnailUrl = null,
            playsSubtitle = "0 plays",
            playersSubtitle = "2–4p",
            playTimeSubtitle = "45 min",
            isUnplayed = true
        ),
        CollectionGameItem(
            gameId = 40,
            name = "Azul Duel",
            yearPublished = 2017,
            thumbnailUrl = null,
            playsSubtitle = "0 plays",
            playersSubtitle = "2–4p",
            playTimeSubtitle = "45 min",
            isUnplayed = true
        ),
        CollectionGameItem(
            gameId = 5,
            name = "Ticket to Ride",
            yearPublished = 2004,
            thumbnailUrl = null,
            playsSubtitle = "33 plays",
            playersSubtitle = "2–5p",
            playTimeSubtitle = "60 min",
            isUnplayed = false
        )
    )

    private fun buildSections(
        games: List<CollectionGameItem>
    ): List<CollectionSection> =
        games.groupBy { it.name.firstOrNull()?.uppercaseChar() ?: '#' }
            .map { (key, items) ->
                CollectionSection(key, items.sortedBy(CollectionGameItem::name))
            }
            .sortedBy(CollectionSection::key)
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun CollectionScreenPreview(
    @PreviewParameter(CollectionUiStatePreviewParameterProvider::class) uiState: CollectionUiState
) {
    MeepleBookTheme {
        CollectionScreenRoot(
            uiState = uiState,
            onEvent = {},
            rememberLazyListState(),
            rememberLazyGridState()
        )
    }
}