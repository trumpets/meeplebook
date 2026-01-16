package app.meeplebook.feature.collection

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.meeplebook.R
import app.meeplebook.core.collection.model.CollectionSort
import app.meeplebook.core.collection.model.QuickFilter
import app.meeplebook.ui.theme.MeepleBookTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI Compose tests for [CollectionScreenRoot].
 * Tests UI rendering and interaction behavior for different collection screen states.
 *
 * Test structure:
 * - State Testing (5 tests): Loading, Empty (3 variants), Error
 * - Content Testing (3 tests): Grid mode, List mode, Multiple sections
 * - Interaction Testing (4 tests): Search, Quick filter, View mode, Sort
 * - Integration Testing (2 tests): Search/filters in Empty/Error states
 */
@RunWith(AndroidJUnit4::class)
class CollectionScreenRootTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun collectionScreenRoot_loadingState_displaysLoadingIndicator() {
        composeTestRule.setContent {
            MeepleBookTheme {
                CollectionScreenRoot(
                    uiState = CollectionUiState.Loading,
                    onEvent = {},
                    listState = LazyListState(),
                    gridState = LazyGridState()
                )
            }
        }

        // Verify loading indicator is displayed
        composeTestRule.onNodeWithTag("loadingIndicator").assertIsDisplayed()
        composeTestRule.onNodeWithText("Loading collection…").assertIsDisplayed()

        // Verify collection screen container is present
        composeTestRule.onNodeWithTag("collectionScreen").assertIsDisplayed()
    }

    @Test
    fun collectionScreenRoot_emptyState_noGames_displaysMessage() {
        composeTestRule.setContent {
            MeepleBookTheme {
                CollectionScreenRoot(
                    uiState = CollectionUiState.Empty(
                        reason = EmptyReason.NO_GAMES,
                        searchQuery = "",
                        activeQuickFilter = QuickFilter.ALL,
                        totalGameCount = 0,
                        unplayedGameCount = 0,
                        isRefreshing = false
                    ),
                    onEvent = {},
                    listState = LazyListState(),
                    gridState = LazyGridState()
                )
            }
        }

        // Verify empty state is displayed
        composeTestRule.onNodeWithTag("emptyState").assertIsDisplayed()
        composeTestRule.onNodeWithText("Your collection is empty. Add games to your BGG collection to see them here.")
            .assertIsDisplayed()
    }

    @Test
    fun collectionScreenRoot_emptyState_noSearchResults_displaysMessage() {
        composeTestRule.setContent {
            MeepleBookTheme {
                CollectionScreenRoot(
                    uiState = CollectionUiState.Empty(
                        reason = EmptyReason.NO_SEARCH_RESULTS,
                        searchQuery = "azul",
                        activeQuickFilter = QuickFilter.ALL,
                        totalGameCount = 100,
                        unplayedGameCount = 27,
                        isRefreshing = false
                    ),
                    onEvent = {},
                    listState = LazyListState(),
                    gridState = LazyGridState()
                )
            }
        }

        // Verify empty state is displayed
        composeTestRule.onNodeWithTag("emptyState").assertIsDisplayed()
        composeTestRule.onNodeWithText("No games match your search").assertIsDisplayed()
    }

    @Test
    fun collectionScreenRoot_emptyState_noFilterResults_displaysMessage() {
        composeTestRule.setContent {
            MeepleBookTheme {
                CollectionScreenRoot(
                    uiState = CollectionUiState.Empty(
                        reason = EmptyReason.NO_FILTER_RESULTS,
                        searchQuery = "",
                        activeQuickFilter = QuickFilter.UNPLAYED,
                        totalGameCount = 100,
                        unplayedGameCount = 0,
                        isRefreshing = false
                    ),
                    onEvent = {},
                    listState = LazyListState(),
                    gridState = LazyGridState()
                )
            }
        }

        // Verify empty state is displayed
        composeTestRule.onNodeWithTag("emptyState").assertIsDisplayed()
        composeTestRule.onNodeWithText("No games match your filter").assertIsDisplayed()
    }

    @Test
    fun collectionScreenRoot_errorState_displaysErrorMessage() {
        composeTestRule.setContent {
            MeepleBookTheme {
                CollectionScreenRoot(
                    uiState = CollectionUiState.Error(
                        errorMessageResId = R.string.sync_collections_failed_error,
                        searchQuery = "",
                        activeQuickFilter = QuickFilter.ALL,
                        totalGameCount = 0,
                        unplayedGameCount = 0,
                        isRefreshing = false
                    ),
                    onEvent = {},
                    listState = LazyListState(),
                    gridState = LazyGridState()
                )
            }
        }

        // Verify error state is displayed
        composeTestRule.onNodeWithTag("errorState").assertIsDisplayed()
        composeTestRule.onNodeWithText("Failed to sync collections").assertIsDisplayed()
    }

    @Test
    fun collectionScreenRoot_contentState_displaysSearchAndFilters() {
        val sampleGames = listOf(
            CollectionGameItem(
                gameId = 1,
                name = "Catan",
                yearPublished = 1995,
                thumbnailUrl = null,
                playsSubtitle = "42 plays",
                playersSubtitle = "3–4p",
                playTimeSubtitle = "75 min",
                isNew = false
            )
        )

        val sections = listOf(
            CollectionSection('C', sampleGames)
        )

        composeTestRule.setContent {
            MeepleBookTheme {
                CollectionScreenRoot(
                    uiState = CollectionUiState.Content(
                        searchQuery = "",
                        viewMode = CollectionViewMode.GRID,
                        sort = CollectionSort.ALPHABETICAL,
                        activeQuickFilter = QuickFilter.ALL,
                        availableSortOptions = CollectionSort.entries,
                        sections = sections,
                        sectionIndices = mapOf('C' to 0),
                        totalGameCount = 1,
                        unplayedGameCount = 0,
                        isRefreshing = false,
                        showAlphabetJump = true,
                        isSortSheetVisible = false
                    ),
                    onEvent = {},
                    listState = LazyListState(),
                    gridState = LazyGridState()
                )
            }
        }

        // Verify search bar is displayed
        composeTestRule.onNodeWithText("Search games").assertIsDisplayed()

        // Verify quick filters are displayed
        composeTestRule.onNodeWithText("All (1)").assertIsDisplayed()
        composeTestRule.onNodeWithText("Unplayed (0)").assertIsDisplayed()

        // Verify game name is displayed
        composeTestRule.onNodeWithText("Catan").assertIsDisplayed()
    }

    @Test
    fun collectionScreenRoot_contentState_listMode_displaysCorrectly() {
        val sampleGames = listOf(
            CollectionGameItem(
                gameId = 1,
                name = "Wingspan",
                yearPublished = 2019,
                thumbnailUrl = null,
                playsSubtitle = "18 plays",
                playersSubtitle = "1–5p",
                playTimeSubtitle = "90 min",
                isNew = false
            )
        )

        val sections = listOf(
            CollectionSection('W', sampleGames)
        )

        composeTestRule.setContent {
            MeepleBookTheme {
                CollectionScreenRoot(
                    uiState = CollectionUiState.Content(
                        searchQuery = "",
                        viewMode = CollectionViewMode.LIST,
                        sort = CollectionSort.ALPHABETICAL,
                        activeQuickFilter = QuickFilter.ALL,
                        availableSortOptions = CollectionSort.entries,
                        sections = sections,
                        sectionIndices = mapOf('W' to 0),
                        totalGameCount = 1,
                        unplayedGameCount = 0,
                        isRefreshing = false,
                        showAlphabetJump = true,
                        isSortSheetVisible = false
                    ),
                    onEvent = {},
                    listState = LazyListState(),
                    gridState = LazyGridState()
                )
            }
        }

        // Verify game name is displayed in list mode
        composeTestRule.onNodeWithText("Wingspan").assertIsDisplayed()
    }

    @Test
    fun collectionScreenRoot_searchInput_triggersCallback() {
        var capturedQuery = ""

        composeTestRule.setContent {
            MeepleBookTheme {
                CollectionScreenRoot(
                    uiState = CollectionUiState.Empty(
                        reason = EmptyReason.NO_GAMES,
                        searchQuery = "",
                        activeQuickFilter = QuickFilter.ALL,
                        totalGameCount = 0,
                        unplayedGameCount = 0,
                        isRefreshing = false
                    ),
                    onEvent = { event ->
                        if (event is CollectionEvent.SearchChanged) {
                            capturedQuery = event.query
                        }
                    },
                    listState = LazyListState(),
                    gridState = LazyGridState()
                )
            }
        }

        // Type in the search field
        composeTestRule.onNodeWithText("Search games").performTextInput("azul")

        // Verify callback was triggered with the correct value
        assertEquals("azul", capturedQuery)
    }

    @Test
    fun collectionScreenRoot_quickFilterClick_triggersCallback() {
        var capturedFilter: QuickFilter? = null

        composeTestRule.setContent {
            MeepleBookTheme {
                CollectionScreenRoot(
                    uiState = CollectionUiState.Empty(
                        reason = EmptyReason.NO_GAMES,
                        searchQuery = "",
                        activeQuickFilter = QuickFilter.ALL,
                        totalGameCount = 100,
                        unplayedGameCount = 27,
                        isRefreshing = false
                    ),
                    onEvent = { event ->
                        if (event is CollectionEvent.QuickFilterSelected) {
                            capturedFilter = event.filter
                        }
                    },
                    listState = LazyListState(),
                    gridState = LazyGridState()
                )
            }
        }

        // Click on the Unplayed filter
        composeTestRule.onNodeWithText("Unplayed (27)").performClick()

        // Verify callback was triggered with the correct filter
        assertEquals(QuickFilter.UNPLAYED, capturedFilter)
    }

    @Test
    fun collectionScreenRoot_viewModeToggle_triggersCallback() {
        var capturedViewMode: CollectionViewMode? = null

        val sampleGames = listOf(
            CollectionGameItem(
                gameId = 1,
                name = "Catan",
                yearPublished = 1995,
                thumbnailUrl = null,
                playsSubtitle = "42 plays",
                playersSubtitle = "3–4p",
                playTimeSubtitle = "75 min",
                isNew = false
            )
        )

        val sections = listOf(
            CollectionSection('C', sampleGames)
        )

        composeTestRule.setContent {
            MeepleBookTheme {
                CollectionScreenRoot(
                    uiState = CollectionUiState.Content(
                        searchQuery = "",
                        viewMode = CollectionViewMode.GRID,
                        sort = CollectionSort.ALPHABETICAL,
                        activeQuickFilter = QuickFilter.ALL,
                        availableSortOptions = CollectionSort.entries,
                        sections = sections,
                        sectionIndices = mapOf('C' to 0),
                        totalGameCount = 1,
                        unplayedGameCount = 0,
                        isRefreshing = false,
                        showAlphabetJump = true,
                        isSortSheetVisible = false
                    ),
                    onEvent = { event ->
                        if (event is CollectionEvent.ViewModeSelected) {
                            capturedViewMode = event.viewMode
                        }
                    },
                    listState = LazyListState(),
                    gridState = LazyGridState()
                )
            }
        }

        // Click on the List view button
        composeTestRule.onNodeWithText("List").performClick()

        // Verify callback was triggered with correct view mode
        assertEquals(CollectionViewMode.LIST, capturedViewMode)
    }

    @Test
    fun collectionScreenRoot_sortButtonClick_triggersCallback() {
        var capturedEvent: CollectionEvent? = null

        val sampleGames = listOf(
            CollectionGameItem(
                gameId = 1,
                name = "Catan",
                yearPublished = 1995,
                thumbnailUrl = null,
                playsSubtitle = "42 plays",
                playersSubtitle = "3–4p",
                playTimeSubtitle = "75 min",
                isNew = false
            )
        )

        val sections = listOf(
            CollectionSection('C', sampleGames)
        )

        composeTestRule.setContent {
            MeepleBookTheme {
                CollectionScreenRoot(
                    uiState = CollectionUiState.Content(
                        searchQuery = "",
                        viewMode = CollectionViewMode.GRID,
                        sort = CollectionSort.ALPHABETICAL,
                        activeQuickFilter = QuickFilter.ALL,
                        availableSortOptions = CollectionSort.entries,
                        sections = sections,
                        sectionIndices = mapOf('C' to 0),
                        totalGameCount = 1,
                        unplayedGameCount = 0,
                        isRefreshing = false,
                        showAlphabetJump = true,
                        isSortSheetVisible = false
                    ),
                    onEvent = { event ->
                        capturedEvent = event
                    },
                    listState = LazyListState(),
                    gridState = LazyGridState()
                )
            }
        }

        // Click on the Sort button
        composeTestRule.onNodeWithText("Sort").performClick()

        // Verify callback was triggered with correct event type
        assertTrue(capturedEvent is CollectionEvent.OpenSortSheet)
    }

    @Test
    fun collectionScreenRoot_emptyState_displaysSearchAndFilters() {
        composeTestRule.setContent {
            MeepleBookTheme {
                CollectionScreenRoot(
                    uiState = CollectionUiState.Empty(
                        reason = EmptyReason.NO_SEARCH_RESULTS,
                        searchQuery = "test",
                        activeQuickFilter = QuickFilter.ALL,
                        totalGameCount = 100,
                        unplayedGameCount = 27,
                        isRefreshing = false
                    ),
                    onEvent = {},
                    listState = LazyListState(),
                    gridState = LazyGridState()
                )
            }
        }

        // Verify search bar is still visible in empty state
        composeTestRule.onNodeWithText("Search games").assertIsDisplayed()

        // Verify quick filters are still visible
        composeTestRule.onNodeWithText("All (100)").assertIsDisplayed()
        composeTestRule.onNodeWithText("Unplayed (27)").assertIsDisplayed()
    }

    @Test
    fun collectionScreenRoot_errorState_displaysSearchAndFilters() {
        composeTestRule.setContent {
            MeepleBookTheme {
                CollectionScreenRoot(
                    uiState = CollectionUiState.Error(
                        errorMessageResId = R.string.sync_collections_failed_error,
                        searchQuery = "",
                        activeQuickFilter = QuickFilter.ALL,
                        totalGameCount = 100,
                        unplayedGameCount = 27,
                        isRefreshing = false
                    ),
                    onEvent = {},
                    listState = LazyListState(),
                    gridState = LazyGridState()
                )
            }
        }

        // Verify search bar is still visible in error state
        composeTestRule.onNodeWithText("Search games").assertIsDisplayed()

        // Verify quick filters are still visible
        composeTestRule.onNodeWithText("All (100)").assertIsDisplayed()
        composeTestRule.onNodeWithText("Unplayed (27)").assertIsDisplayed()
    }

    @Test
    fun collectionScreenRoot_contentState_multipleSections_displaysCorrectly() {
        val sampleGames = listOf(
            CollectionGameItem(
                gameId = 1,
                name = "Azul",
                yearPublished = 2017,
                thumbnailUrl = null,
                playsSubtitle = "25 plays",
                playersSubtitle = "2–4p",
                playTimeSubtitle = "30 min",
                isNew = false
            ),
            CollectionGameItem(
                gameId = 2,
                name = "Catan",
                yearPublished = 1995,
                thumbnailUrl = null,
                playsSubtitle = "42 plays",
                playersSubtitle = "3–4p",
                playTimeSubtitle = "75 min",
                isNew = false
            ),
            CollectionGameItem(
                gameId = 3,
                name = "Wingspan",
                yearPublished = 2019,
                thumbnailUrl = null,
                playsSubtitle = "18 plays",
                playersSubtitle = "1–5p",
                playTimeSubtitle = "90 min",
                isNew = false
            )
        )

        val sections = listOf(
            CollectionSection('A', listOf(sampleGames[0])),
            CollectionSection('C', listOf(sampleGames[1])),
            CollectionSection('W', listOf(sampleGames[2]))
        )

        composeTestRule.setContent {
            MeepleBookTheme {
                CollectionScreenRoot(
                    uiState = CollectionUiState.Content(
                        searchQuery = "",
                        viewMode = CollectionViewMode.GRID,
                        sort = CollectionSort.ALPHABETICAL,
                        activeQuickFilter = QuickFilter.ALL,
                        availableSortOptions = CollectionSort.entries,
                        sections = sections,
                        sectionIndices = mapOf('A' to 0, 'C' to 1, 'W' to 2),
                        totalGameCount = 3,
                        unplayedGameCount = 0,
                        isRefreshing = false,
                        showAlphabetJump = true,
                        isSortSheetVisible = false
                    ),
                    onEvent = {},
                    listState = LazyListState(),
                    gridState = LazyGridState()
                )
            }
        }

        // Verify all game names are displayed
        composeTestRule.onNodeWithText("Azul").assertIsDisplayed()
        composeTestRule.onNodeWithText("Catan").assertIsDisplayed()
        composeTestRule.onNodeWithText("Wingspan").assertIsDisplayed()

        // Verify section headers are displayed
        composeTestRule.onNodeWithText("A").assertIsDisplayed()
        composeTestRule.onNodeWithText("C").assertIsDisplayed()
        composeTestRule.onNodeWithText("W").assertIsDisplayed()
    }
}
