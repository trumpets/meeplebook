package app.meeplebook.feature.collection

import app.cash.turbine.test
import app.meeplebook.R
import app.meeplebook.core.collection.FakeCollectionRepository
import app.meeplebook.core.collection.domain.ObserveCollectionUseCase
import app.meeplebook.core.collection.model.CollectionItem
import app.meeplebook.core.collection.model.CollectionSort
import app.meeplebook.core.collection.model.GameSubtype
import app.meeplebook.core.collection.model.QuickFilter
import app.meeplebook.core.ui.FakeStringProvider
import app.meeplebook.core.util.DebounceDurations
import app.meeplebook.feature.collection.domain.BuildCollectionSectionsUseCase
import app.meeplebook.feature.collection.domain.ObserveCollectionDomainSectionsUseCase
import app.meeplebook.testutils.assertState
import app.meeplebook.testutils.awaitUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

/**
 * Unit tests for [CollectionViewModel].
 *
 * Tests the ViewModel's reactive state management, including search debouncing,
 * filtering, sorting, and section building logic.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CollectionViewModelTest {

    private lateinit var viewModel: CollectionViewModel
    private lateinit var fakeCollectionRepository: FakeCollectionRepository
    private lateinit var observeCollectionUseCase: ObserveCollectionUseCase
    private lateinit var buildSectionsUseCase: BuildCollectionSectionsUseCase
    private lateinit var observeCollectionDomainSectionsUseCase: ObserveCollectionDomainSectionsUseCase
    private lateinit var fakeStringProvider: FakeStringProvider
    
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        
        // Set up dependencies
        fakeCollectionRepository = FakeCollectionRepository()
        observeCollectionUseCase = ObserveCollectionUseCase(fakeCollectionRepository)
        buildSectionsUseCase = BuildCollectionSectionsUseCase()
        observeCollectionDomainSectionsUseCase = ObserveCollectionDomainSectionsUseCase(
            observeCollection = observeCollectionUseCase,
            sectionBuilder = buildSectionsUseCase
        )
        fakeStringProvider = FakeStringProvider()
        
        // Configure string provider with expected formats
        fakeStringProvider.setString(R.string.collection_plays_count, "%d plays")
        fakeStringProvider.setString(R.string.collection_player_count, "%d-%dp")
        fakeStringProvider.setString(R.string.collection_play_time, "%d-%dm")
        
        // Create ViewModel
        viewModel = CollectionViewModel(
            observeCollectionDomainSectionsUseCase = observeCollectionDomainSectionsUseCase,
            stringProvider = fakeStringProvider
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is Loading`() {
        val state = viewModel.uiState.value
        assertEquals(CollectionUiState.Loading, state)
    }

    @Test
    fun `empty collection shows Empty state with NO_GAMES reason`() = runTest {
        // Given - empty collection
        fakeCollectionRepository.setCollection(emptyList())

        // Then
        val state = awaitUiStateAfterDebounce<CollectionUiState.Empty>(viewModel)
        assertEquals(EmptyReason.NO_GAMES, state.reason)
    }

    @Test
    fun `collection with games shows Content state`() = runTest {
        // Given
        val items = listOf(
            createCollectionItem(gameId = 1, name = "Azul"),
            createCollectionItem(gameId = 2, name = "Brass: Birmingham")
        )
        fakeCollectionRepository.setCollection(items)


        // Then
        val state = awaitUiStateAfterDebounce<CollectionUiState.Content>(viewModel)
        assertEquals(2, state.totalGameCount)
        assertEquals(2, state.sections.size)
    }

    @Test
    fun `SearchChanged event updates search query immediately in UI state`() = runTest {
        // Given
        val items = listOf(createCollectionItem(gameId = 1, name = "Azul"))
        fakeCollectionRepository.setCollection(items)

        // When
        viewModel.onEvent(CollectionEvent.SearchChanged("test query"))

        // Then - search query should be updated immediately
        val state = awaitUiStateAfterDebounce<CollectionUiState.Content>(viewModel)
        assertEquals("test query", state.searchQuery)
    }

    @Test
    fun `search query is debounced before filtering`() = runTest {
        // Given
        val items = listOf(
            createCollectionItem(gameId = 1, name = "Azul"),
            createCollectionItem(gameId = 2, name = "Brass: Birmingham")
        )
        fakeCollectionRepository.setCollection(items)

        viewModel.uiState.test {
            advanceUntilIdle()

            // Drain initial Loading states to avoid flakiness from assuming a fixed number of emissions
            var current: CollectionUiState
            do {
                current = awaitItem()
            } while (current is CollectionUiState.Loading)

            val initial = current.assertState<CollectionUiState.Content>()
            assertEquals("", initial.searchQuery)
            assertEquals(2, initial.totalGameCount)

            // Initial content state
            // When - send search query
            viewModel.onEvent(CollectionEvent.SearchChanged("Azul"))

            // Then: UI state updates immediately with raw query, but not filtered yet.
            val immediate = awaitItem().assertState<CollectionUiState.Content>()
            assertEquals("Azul", immediate.searchQuery)
            assertEquals(2, immediate.totalGameCount)

            // And: before debounce expires, nothing else should be emitted.
            advanceTimeBy(DebounceDurations.SearchQuery.inWholeMilliseconds - 1)
            expectNoEvents()

            // After debounce: advance past boundary and flush.
            advanceTimeBy(2)
            advanceUntilIdle()

            expectNoEvents()

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `empty search result shows Empty state with NO_SEARCH_RESULTS reason`() = runTest {
        // Given - collection with games
        val items = listOf(createCollectionItem(gameId = 1, name = "Azul"))
        fakeCollectionRepository.setCollection(items)

        // When - search returns no results (simulated by empty collection)
        viewModel.onEvent(CollectionEvent.SearchChanged("NonexistentGame"))
        fakeCollectionRepository.setCollection(emptyList())

        // Then
        val state = awaitUiStateAfterDebounce<CollectionUiState.Empty>(viewModel)
        assertEquals(EmptyReason.NO_SEARCH_RESULTS, state.reason)
    }

    @Test
    fun `QuickFilterSelected updates filter and shows appropriate empty state`() = runTest {
        // Given - empty collection
        fakeCollectionRepository.setCollection(emptyList())

        // When - select non-ALL filter
        viewModel.onEvent(CollectionEvent.QuickFilterSelected(QuickFilter.UNPLAYED))

        // Then - should show NO_FILTER_RESULTS
        val state = awaitUiStateAfterDebounce<CollectionUiState.Empty>(viewModel)
        assertEquals(EmptyReason.NO_FILTER_RESULTS, state.reason)
    }

    @Test
    fun `SortSelected updates sort in content state`() = runTest {
        // Given
        val items = listOf(
            createCollectionItem(gameId = 1, name = "Azul", yearPublished = 2017),
            createCollectionItem(gameId = 2, name = "Brass: Birmingham", yearPublished = 2018)
        )
        fakeCollectionRepository.setCollection(items)

        // When
        viewModel.onEvent(CollectionEvent.SortSelected(CollectionSort.YEAR_PUBLISHED_NEWEST))

        // Then
        val state = awaitUiStateAfterDebounce<CollectionUiState.Content>(viewModel)
        assertEquals(CollectionSort.YEAR_PUBLISHED_NEWEST, state.sort)
    }

    @Test
    fun `ViewModeSelected updates view mode in content state`() = runTest {
        // Given
        val items = listOf(createCollectionItem(gameId = 1, name = "Azul"))
        fakeCollectionRepository.setCollection(items)

        // When
        viewModel.onEvent(CollectionEvent.ViewModeSelected(CollectionViewMode.GRID))

        // Then
        val state = awaitUiStateAfterDebounce<CollectionUiState.Content>(viewModel)
        assertEquals(CollectionViewMode.GRID, state.viewMode)
    }

    @Test
    fun `content state builds sections correctly`() = runTest {
        // Given
        val items = listOf(
            createCollectionItem(gameId = 1, name = "Azul"),
            createCollectionItem(gameId = 2, name = "Ark Nova"),
            createCollectionItem(gameId = 3, name = "Brass: Birmingham"),
            createCollectionItem(gameId = 4, name = "Catan")
        )
        fakeCollectionRepository.setCollection(items)


        // Then
        val state = awaitUiStateAfterDebounce<CollectionUiState.Content>(viewModel)

        assertEquals(3, state.sections.size)
        assertEquals('A', state.sections[0].key)
        assertEquals(2, state.sections[0].games.size)
        assertEquals('B', state.sections[1].key)
        assertEquals(1, state.sections[1].games.size)
        assertEquals('C', state.sections[2].key)
        assertEquals(1, state.sections[2].games.size)
    }

    @Test
    fun `content state builds section indices correctly`() = runTest {
        // Given
        val items = listOf(
            createCollectionItem(gameId = 1, name = "Azul"),
            createCollectionItem(gameId = 2, name = "Brass: Birmingham"),
            createCollectionItem(gameId = 3, name = "Catan")
        )
        fakeCollectionRepository.setCollection(items)

        // Then
        val state = awaitUiStateAfterDebounce<CollectionUiState.Content>(viewModel)
        
        // Section indices should account for headers + items
        // A: index 0 (header), items at 1
        // B: index 2 (header), items at 3
        // C: index 4 (header), items at 5
        assertEquals(0, state.sectionIndices['A'])
        assertEquals(2, state.sectionIndices['B'])
        assertEquals(4, state.sectionIndices['C'])
    }

    @Test
    fun `content state shows alphabet jump when multiple sections exist`() = runTest {
        // Given - multiple sections
        val items = listOf(
            createCollectionItem(gameId = 1, name = "Azul"),
            createCollectionItem(gameId = 2, name = "Brass: Birmingham")
        )
        fakeCollectionRepository.setCollection(items)

        // Then
        val state = awaitUiStateAfterDebounce<CollectionUiState.Content>(viewModel)
        assertTrue(state.showAlphabetJump)
    }

    @Test
    fun `content state hides alphabet jump when single section exists`() = runTest {
        // Given - single section
        val items = listOf(
            createCollectionItem(gameId = 1, name = "Azul"),
            createCollectionItem(gameId = 2, name = "Ark Nova")
        )
        fakeCollectionRepository.setCollection(items)

        // Then
        val state = awaitUiStateAfterDebounce<CollectionUiState.Content>(viewModel)
        assertFalse(state.showAlphabetJump)
    }

    @Test
    fun `JumpToLetter emits ScrollToLetter effect`() = runTest {
        // Given
        val effects = mutableListOf<CollectionUiEffects>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiEffect.collect { effects.add(it) }
        }

        // When
        viewModel.onEvent(CollectionEvent.JumpToLetter('A'))
        advanceUntilIdle()

        // Then
        assertEquals(1, effects.size)
        assertTrue(effects[0] is CollectionUiEffects.ScrollToLetter)
        assertEquals('A', (effects[0] as CollectionUiEffects.ScrollToLetter).letter)
        
        job.cancel()
    }

    @Test
    fun `GameClicked emits NavigateToGame effect`() = runTest {
        // Given
        val effects = mutableListOf<CollectionUiEffects>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiEffect.collect { effects.add(it) }
        }

        // When
        viewModel.onEvent(CollectionEvent.GameClicked(123))
        advanceUntilIdle()

        // Then
        assertEquals(1, effects.size)
        assertTrue(effects[0] is CollectionUiEffects.NavigateToGame)
        assertEquals(123L, (effects[0] as CollectionUiEffects.NavigateToGame).gameId)
        
        job.cancel()
    }

    @Test
    fun `OpenSortSheet emits OpenSortSheet effect`() = runTest {
        // Given
        val effects = mutableListOf<CollectionUiEffects>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiEffect.collect { effects.add(it) }
        }

        // When
        viewModel.onEvent(CollectionEvent.OpenSortSheet)
        advanceUntilIdle()

        // Then
        assertEquals(1, effects.size)
        assertTrue(effects[0] is CollectionUiEffects.OpenSortSheet)
        
        job.cancel()
    }

    @Test
    fun `DismissSortSheet emits DismissSortSheet effect`() = runTest {
        // Given
        val effects = mutableListOf<CollectionUiEffects>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiEffect.collect { effects.add(it) }
        }

        // When
        viewModel.onEvent(CollectionEvent.DismissSortSheet)
        advanceUntilIdle()

        // Then
        assertEquals(1, effects.size)
        assertTrue(effects[0] is CollectionUiEffects.DismissSortSheet)
        
        job.cancel()
    }

    @Test
    fun `state updates reactively when repository data changes`() = runTest {
        // Given - initial state with one game
        val initialItems = listOf(createCollectionItem(gameId = 1, name = "Azul"))
        fakeCollectionRepository.setCollection(initialItems)

        val initialState = awaitUiStateAfterDebounce<CollectionUiState.Content>(viewModel)
        assertEquals(1, initialState.totalGameCount)

        // When - repository data changes
        val updatedItems = listOf(
            createCollectionItem(gameId = 1, name = "Azul"),
            createCollectionItem(gameId = 2, name = "Brass: Birmingham")
        )
        fakeCollectionRepository.setCollection(updatedItems)

        // Then - state should update
        val updatedState = awaitUiStateAfterDebounce<CollectionUiState.Content>(viewModel)
        assertEquals(2, updatedState.totalGameCount)
    }

    @Test
    fun `content state includes correct game metadata`() = runTest {
        // Given
        val item = createCollectionItem(
            gameId = 123,
            name = "Wingspan",
            yearPublished = 2019,
            thumbnail = "https://example.com/wingspan.jpg",
            numPlays = 15,
            minPlayers = 1,
            maxPlayers = 5,
            minPlayTimeMinutes = 40,
            maxPlayTimeMinutes = 70
        )
        fakeCollectionRepository.setCollection(listOf(item))

        // Then
        val state = awaitUiStateAfterDebounce<CollectionUiState.Content>(viewModel)
        val game = state.sections[0].games[0]
        
        assertEquals(123L, game.gameId)
        assertEquals("Wingspan", game.name)
        assertEquals(2019, game.yearPublished)
        assertEquals("https://example.com/wingspan.jpg", game.thumbnailUrl)
        assertEquals("15 plays", game.playsSubtitle)
        assertEquals("1-5p", game.playersSubtitle)
        assertEquals("40-70m", game.playTimeSubtitle)
    }

    @Test
    fun `available sort options include all CollectionSort entries`() = runTest {
        // Given
        val items = listOf(createCollectionItem(gameId = 1, name = "Azul"))
        fakeCollectionRepository.setCollection(items)

        // Then
        val state = awaitUiStateAfterDebounce<CollectionUiState.Content>(viewModel)
        assertEquals(CollectionSort.entries, state.availableSortOptions)
    }

    @Test
    fun `isRefreshing is false in content state`() = runTest {
        // Given
        val items = listOf(createCollectionItem(gameId = 1, name = "Azul"))
        fakeCollectionRepository.setCollection(items)

        // Then
        val state = awaitUiStateAfterDebounce<CollectionUiState.Content>(viewModel)
        assertFalse(state.isRefreshing)
    }

    @Test
    fun `isSortSheetVisible is false in content state`() = runTest {
        // Given
        val items = listOf(createCollectionItem(gameId = 1, name = "Azul"))
        fakeCollectionRepository.setCollection(items)

        // Then
        val state = awaitUiStateAfterDebounce<CollectionUiState.Content>(viewModel)
        assertFalse(state.isSortSheetVisible)
    }

    // Helper function to create test collection items
    private fun createCollectionItem(
        gameId: Long,
        name: String,
        yearPublished: Int? = null,
        thumbnail: String? = null,
        numPlays: Int = 0,
        minPlayers: Int? = null,
        maxPlayers: Int? = null,
        minPlayTimeMinutes: Int? = null,
        maxPlayTimeMinutes: Int? = null
    ): CollectionItem {
        return CollectionItem(
            gameId = gameId,
            subtype = GameSubtype.BOARDGAME,
            name = name,
            yearPublished = yearPublished,
            thumbnail = thumbnail,
            lastModifiedDate = Instant.parse("2024-01-01T00:00:00Z"),
            minPlayers = minPlayers,
            maxPlayers = maxPlayers,
            minPlayTimeMinutes = minPlayTimeMinutes,
            maxPlayTimeMinutes = maxPlayTimeMinutes,
            numPlays = numPlays
        )
    }

    suspend inline fun <reified T: CollectionUiState> TestScope.awaitUiStateAfterDebounce(
        viewModel: CollectionViewModel
    ): T {
        return awaitUiState(viewModel.uiState, DebounceDurations.SearchQuery, skipWhile = { it is CollectionUiState.Loading })
    }
}
