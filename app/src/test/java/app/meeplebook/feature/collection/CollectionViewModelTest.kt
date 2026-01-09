package app.meeplebook.feature.collection

import app.meeplebook.R
import app.meeplebook.core.collection.FakeCollectionRepository
import app.meeplebook.core.collection.domain.ObserveCollectionUseCase
import app.meeplebook.core.collection.model.CollectionItem
import app.meeplebook.core.collection.model.CollectionSort
import app.meeplebook.core.collection.model.GameSubtype
import app.meeplebook.core.collection.model.QuickFilter
import app.meeplebook.core.ui.FakeStringProvider
import app.meeplebook.feature.collection.domain.BuildCollectionSectionsUseCase
import app.meeplebook.feature.collection.domain.ObserveCollectionDomainSectionsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
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

        // When - allow state to update
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertTrue(state is CollectionUiState.Empty)
        assertEquals(EmptyReason.NO_GAMES, (state as CollectionUiState.Empty).reason)
    }

    @Test
    fun `collection with games shows Content state`() = runTest {
        // Given
        val items = listOf(
            createCollectionItem(gameId = 1, name = "Azul"),
            createCollectionItem(gameId = 2, name = "Brass: Birmingham")
        )
        fakeCollectionRepository.setCollection(items)

        // When
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertTrue(state is CollectionUiState.Content)
        val contentState = state as CollectionUiState.Content
        assertEquals(2, contentState.totalGameCount)
        assertEquals(2, contentState.sections.size)
    }

    @Test
    fun `SearchChanged event updates search query immediately in UI state`() = runTest {
        // Given
        val items = listOf(createCollectionItem(gameId = 1, name = "Azul"))
        fakeCollectionRepository.setCollection(items)
        advanceUntilIdle()

        // When
        viewModel.onEvent(CollectionEvent.SearchChanged("test query"))
        advanceUntilIdle()

        // Then - search query should be updated immediately
        val state = viewModel.uiState.value
        assertTrue(state is CollectionUiState.Content)
        assertEquals("test query", (state as CollectionUiState.Content).searchQuery)
    }

    @Test
    fun `search query is debounced before filtering`() = runTest {
        // Given
        val items = listOf(
            createCollectionItem(gameId = 1, name = "Azul"),
            createCollectionItem(gameId = 2, name = "Brass: Birmingham")
        )
        fakeCollectionRepository.setCollection(items)
        advanceUntilIdle()

        // When - send search query
        viewModel.onEvent(CollectionEvent.SearchChanged("Azul"))
        
        // Then - before debounce time, no filtering should occur
        advanceTimeBy(200) // Less than 300ms debounce
        val stateBeforeDebounce = viewModel.uiState.value
        assertTrue(stateBeforeDebounce is CollectionUiState.Content)
        assertEquals(2, (stateBeforeDebounce as CollectionUiState.Content).totalGameCount)

        // When - debounce time passes
        advanceTimeBy(150) // Total 350ms > 300ms debounce
        advanceUntilIdle()

        // Then - filtering should occur (note: actual filtering happens in repository query)
        val stateAfterDebounce = viewModel.uiState.value
        assertTrue(stateAfterDebounce is CollectionUiState.Content)
    }

    @Test
    fun `empty search result shows Empty state with NO_SEARCH_RESULTS reason`() = runTest {
        // Given - collection with games
        val items = listOf(createCollectionItem(gameId = 1, name = "Azul"))
        fakeCollectionRepository.setCollection(items)
        advanceUntilIdle()

        // When - search returns no results (simulated by empty collection)
        viewModel.onEvent(CollectionEvent.SearchChanged("NonexistentGame"))
        fakeCollectionRepository.setCollection(emptyList())
        advanceTimeBy(350)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertTrue(state is CollectionUiState.Empty)
        assertEquals(EmptyReason.NO_SEARCH_RESULTS, (state as CollectionUiState.Empty).reason)
    }

    @Test
    fun `QuickFilterSelected updates filter and shows appropriate empty state`() = runTest {
        // Given - empty collection
        fakeCollectionRepository.setCollection(emptyList())
        advanceUntilIdle()

        // When - select non-ALL filter
        viewModel.onEvent(CollectionEvent.QuickFilterSelected(QuickFilter.UNPLAYED))
        advanceUntilIdle()

        // Then - should show NO_FILTER_RESULTS
        val state = viewModel.uiState.value
        assertTrue(state is CollectionUiState.Empty)
        assertEquals(EmptyReason.NO_FILTER_RESULTS, (state as CollectionUiState.Empty).reason)
    }

    @Test
    fun `SortSelected updates sort in content state`() = runTest {
        // Given
        val items = listOf(
            createCollectionItem(gameId = 1, name = "Azul", yearPublished = 2017),
            createCollectionItem(gameId = 2, name = "Brass: Birmingham", yearPublished = 2018)
        )
        fakeCollectionRepository.setCollection(items)
        advanceUntilIdle()

        // When
        viewModel.onEvent(CollectionEvent.SortSelected(CollectionSort.YEAR_PUBLISHED_NEWEST))
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertTrue(state is CollectionUiState.Content)
        assertEquals(CollectionSort.YEAR_PUBLISHED_NEWEST, (state as CollectionUiState.Content).sort)
    }

    @Test
    fun `ViewModeSelected updates view mode in content state`() = runTest {
        // Given
        val items = listOf(createCollectionItem(gameId = 1, name = "Azul"))
        fakeCollectionRepository.setCollection(items)
        advanceUntilIdle()

        // When
        viewModel.onEvent(CollectionEvent.ViewModeSelected(CollectionViewMode.GRID))
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertTrue(state is CollectionUiState.Content)
        assertEquals(CollectionViewMode.GRID, (state as CollectionUiState.Content).viewMode)
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

        // When
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertTrue(state is CollectionUiState.Content)
        val contentState = state as CollectionUiState.Content
        
        assertEquals(3, contentState.sections.size)
        assertEquals('A', contentState.sections[0].key)
        assertEquals(2, contentState.sections[0].games.size)
        assertEquals('B', contentState.sections[1].key)
        assertEquals(1, contentState.sections[1].games.size)
        assertEquals('C', contentState.sections[2].key)
        assertEquals(1, contentState.sections[2].games.size)
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

        // When
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertTrue(state is CollectionUiState.Content)
        val contentState = state as CollectionUiState.Content
        
        // Section indices should account for headers + items
        // A: index 0 (header), items at 1
        // B: index 2 (header), items at 3
        // C: index 4 (header), items at 5
        assertEquals(0, contentState.sectionIndices['A'])
        assertEquals(2, contentState.sectionIndices['B'])
        assertEquals(4, contentState.sectionIndices['C'])
    }

    @Test
    fun `content state shows alphabet jump when multiple sections exist`() = runTest {
        // Given - multiple sections
        val items = listOf(
            createCollectionItem(gameId = 1, name = "Azul"),
            createCollectionItem(gameId = 2, name = "Brass: Birmingham")
        )
        fakeCollectionRepository.setCollection(items)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertTrue(state is CollectionUiState.Content)
        assertTrue((state as CollectionUiState.Content).showAlphabetJump)
    }

    @Test
    fun `content state hides alphabet jump when single section exists`() = runTest {
        // Given - single section
        val items = listOf(
            createCollectionItem(gameId = 1, name = "Azul"),
            createCollectionItem(gameId = 2, name = "Ark Nova")
        )
        fakeCollectionRepository.setCollection(items)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertTrue(state is CollectionUiState.Content)
        assertFalse((state as CollectionUiState.Content).showAlphabetJump)
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
        advanceUntilIdle()

        val initialState = viewModel.uiState.value
        assertTrue(initialState is CollectionUiState.Content)
        assertEquals(1, (initialState as CollectionUiState.Content).totalGameCount)

        // When - repository data changes
        val updatedItems = listOf(
            createCollectionItem(gameId = 1, name = "Azul"),
            createCollectionItem(gameId = 2, name = "Brass: Birmingham")
        )
        fakeCollectionRepository.setCollection(updatedItems)
        advanceUntilIdle()

        // Then - state should update
        val updatedState = viewModel.uiState.value
        assertTrue(updatedState is CollectionUiState.Content)
        assertEquals(2, (updatedState as CollectionUiState.Content).totalGameCount)
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

        // When
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertTrue(state is CollectionUiState.Content)
        val contentState = state as CollectionUiState.Content
        val game = contentState.sections[0].games[0]
        
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

        // When
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertTrue(state is CollectionUiState.Content)
        val contentState = state as CollectionUiState.Content
        assertEquals(CollectionSort.entries, contentState.availableSortOptions)
    }

    @Test
    fun `isRefreshing is false in content state`() = runTest {
        // Given
        val items = listOf(createCollectionItem(gameId = 1, name = "Azul"))
        fakeCollectionRepository.setCollection(items)

        // When
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertTrue(state is CollectionUiState.Content)
        assertFalse((state as CollectionUiState.Content).isRefreshing)
    }

    @Test
    fun `isSortSheetVisible is false in content state`() = runTest {
        // Given
        val items = listOf(createCollectionItem(gameId = 1, name = "Azul"))
        fakeCollectionRepository.setCollection(items)

        // When
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertTrue(state is CollectionUiState.Content)
        assertFalse((state as CollectionUiState.Content).isSortSheetVisible)
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
}
