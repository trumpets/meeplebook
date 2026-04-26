package app.meeplebook.feature.plays

import app.cash.turbine.test
import app.meeplebook.core.plays.FakePlaysRepository
import app.meeplebook.core.plays.PlayTestFactory.createPlay
import app.meeplebook.core.plays.domain.ObservePlayStatsUseCase
import app.meeplebook.core.plays.domain.ObservePlaysUseCase
import app.meeplebook.core.plays.model.PlayId
import app.meeplebook.core.sync.FakeSyncTimeRepository
import app.meeplebook.core.sync.domain.ObserveSyncStateUseCase
import app.meeplebook.core.sync.domain.ShouldAutoSyncOnScreenEnterUseCase
import app.meeplebook.core.sync.manager.FakeSyncManager
import app.meeplebook.core.sync.model.SyncType
import app.meeplebook.core.util.DebounceDurations
import app.meeplebook.feature.plays.domain.BuildPlaysSectionsUseCase
import app.meeplebook.feature.plays.domain.ObservePlaysScreenDataUseCase
import app.meeplebook.feature.plays.effect.PlaysEffectProducer
import app.meeplebook.feature.plays.effect.PlaysUiEffect
import app.meeplebook.feature.plays.reducer.PlaysReducer
import app.meeplebook.testutils.assertState
import app.meeplebook.testutils.awaitUiStateMatching
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
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

/**
 * Unit tests for [PlaysViewModel].
 *
 * Tests the ViewModel's reactive state management, including search debouncing,
 * play statistics, and section building logic.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PlaysViewModelTest {

    private lateinit var viewModel: PlaysViewModel
    private lateinit var fakePlaysRepository: FakePlaysRepository
    private lateinit var fakeSyncTimeRepository: FakeSyncTimeRepository
    private lateinit var fakeSyncManager: FakeSyncManager
    private lateinit var observePlaysScreenDataUseCase: ObservePlaysScreenDataUseCase
    private val testDispatcher = StandardTestDispatcher()

    // Fixed clock for predictable testing
    private val testClock = Clock.fixed(
        Instant.parse("2024-01-15T12:00:00Z"),
        ZoneOffset.UTC
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        // Set up dependencies
        fakePlaysRepository = FakePlaysRepository()
        fakeSyncTimeRepository = FakeSyncTimeRepository()
        fakeSyncManager = FakeSyncManager()

        val observePlaysUseCase = ObservePlaysUseCase(fakePlaysRepository)
        val buildPlaysSectionsUseCase = BuildPlaysSectionsUseCase()
        val observePlayStatsUseCase = ObservePlayStatsUseCase(
            playsRepository = fakePlaysRepository,
            clock = testClock
        )

        observePlaysScreenDataUseCase = ObservePlaysScreenDataUseCase(
            observePlays = observePlaysUseCase,
            sectionBuilder = buildPlaysSectionsUseCase,
            observePlayStats = observePlayStatsUseCase
        )

        // Create ViewModel
        viewModel = PlaysViewModel(
            reducer = PlaysReducer(),
            effectProducer = PlaysEffectProducer(),
            observePlaysScreenData = observePlaysScreenDataUseCase,
            observeSyncState = ObserveSyncStateUseCase(fakeSyncTimeRepository),
            shouldAutoSyncOnScreenEnter = ShouldAutoSyncOnScreenEnterUseCase(fakeSyncTimeRepository, testClock),
            syncManager = fakeSyncManager
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is Loading`() {
        val state = viewModel.uiState.value
        assertEquals(PlaysUiState.Loading, state)
    }

    @Test
    fun `init enqueues plays screen-open sync when stale`() = runTest {
        advanceUntilIdle()

        assertEquals(1, fakeSyncManager.playsSyncEnqueueCount)
    }

    @Test
    fun `init skips plays screen-open sync when last sync is recent`() = runTest {
        fakeSyncTimeRepository.markCompleted(
            SyncType.PLAYS,
            testClock.instant().minusSeconds(5 * 60)
        )

        viewModel = PlaysViewModel(
            reducer = PlaysReducer(),
            effectProducer = PlaysEffectProducer(),
            observePlaysScreenData = observePlaysScreenDataUseCase,
            observeSyncState = ObserveSyncStateUseCase(fakeSyncTimeRepository),
            shouldAutoSyncOnScreenEnter = ShouldAutoSyncOnScreenEnterUseCase(fakeSyncTimeRepository, testClock),
            syncManager = fakeSyncManager
        )

        advanceUntilIdle()

        assertEquals(1, fakeSyncManager.playsSyncEnqueueCount)
    }

    @Test
    fun `empty plays shows Empty state with NO_PLAYS reason`() = runTest {
        // Given - empty plays
        fakePlaysRepository.setPlays(emptyList())

        // Then
        val state = awaitUiStateAfterDebounce<PlaysUiState.Empty>(viewModel)
        assertEquals(EmptyReason.NO_PLAYS, state.reason)
        assertEquals("", state.common.searchQuery)
        assertFalse(state.common.isRefreshing)
    }

    @Test
    fun `plays with data shows Content state`() = runTest {
        // Given
        val plays = listOf(
            createPlay(localPlayId = 1, gameName = "Azul", date = Instant.parse("2024-01-15T20:00:00Z")),
            createPlay(localPlayId = 2, gameName = "Brass: Birmingham", date = Instant.parse("2024-01-10T18:00:00Z"))
        )
        fakePlaysRepository.setPlays(plays)
        fakePlaysRepository.setTotalPlaysCount(2)
        fakePlaysRepository.setUniqueGamesCount(2)

        // Then
        val state = awaitUiStateAfterDebounce<PlaysUiState.Content>(viewModel)
        assertEquals(1, state.sections.size) // Both plays in January 2024
        assertEquals(2, state.sections[0].plays.size)
        assertEquals(2L, state.common.playStats.totalPlays)
        assertEquals(2L, state.common.playStats.uniqueGamesCount)
    }

    @Test
    fun `SearchChanged event updates search query immediately in UI state`() = runTest {
        // Given
        val plays = listOf(createPlay(localPlayId = 1, gameName = "Azul"))
        fakePlaysRepository.setPlays(plays)

        // When
        viewModel.onEvent(PlaysEvent.SearchChanged("test query"))

        // Then - search query should be updated immediately
        val state = awaitUiStateAfterDebounce<PlaysUiState.Content>(viewModel)
        assertEquals("test query", state.common.searchQuery)
    }

    @Test
    fun `search query is debounced before filtering`() = runTest {
        // Given
        val plays = listOf(
            createPlay(localPlayId = 1, gameName = "Azul"),
            createPlay(localPlayId = 2, gameName = "Brass: Birmingham")
        )
        fakePlaysRepository.setPlays(plays)
        fakePlaysRepository.setTotalPlaysCount(2)
        fakePlaysRepository.setUniqueGamesCount(2)

        viewModel.uiState.test {
            advanceUntilIdle()

            // Drain initial Loading states
            var current: PlaysUiState
            do {
                current = awaitItem()
            } while (current is PlaysUiState.Loading)

            val initial = current.assertState<PlaysUiState.Content>()
            assertEquals("", initial.common.searchQuery)

            // When - send search query
            viewModel.onEvent(PlaysEvent.SearchChanged("Azul"))

            // Then: UI state updates immediately with raw query
            val immediate = awaitItem().assertState<PlaysUiState.Content>()
            assertEquals("Azul", immediate.common.searchQuery)

            // And: before debounce expires, nothing else should be emitted
            advanceTimeBy(DebounceDurations.SearchQuery.inWholeMilliseconds - 1)
            expectNoEvents()

            // After debounce: advance past boundary and flush
            advanceTimeBy(2)
            advanceUntilIdle()

            expectNoEvents()

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `empty search result shows Empty state with NO_SEARCH_RESULTS reason`() = runTest {
        // Given - plays with games
        val plays = listOf(createPlay(localPlayId = 1, gameName = "Azul"))
        fakePlaysRepository.setPlays(plays)
        fakePlaysRepository.setTotalPlaysCount(1)
        fakePlaysRepository.setUniqueGamesCount(1)

        // When - search returns no results (simulated by empty plays)
        viewModel.onEvent(PlaysEvent.SearchChanged("NonexistentGame"))
        fakePlaysRepository.setPlays(emptyList())
        fakePlaysRepository.setTotalPlaysCount(0)
        fakePlaysRepository.setUniqueGamesCount(0)

        // Then
        val state = awaitUiStateAfterDebounce<PlaysUiState.Empty>(viewModel)
        assertEquals(EmptyReason.NO_SEARCH_RESULTS, state.reason)
    }

    @Test
    fun `PlayClicked emits NavigateToPlay effect`() = runTest {
        // Given
        val effects = mutableListOf<PlaysUiEffect>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiEffect.collect { effects.add(it) }
        }

        // When
        viewModel.onEvent(PlaysEvent.ActionEvent.PlayClicked(PlayId.Local(123L)))
        advanceUntilIdle()

        // Then
        assertEquals(1, effects.size)
        assertTrue(effects[0] is PlaysUiEffect.NavigateToPlay)
        assertEquals(123L, (effects[0] as PlaysUiEffect.NavigateToPlay).playId.localId)

        job.cancel()
    }

    @Test
    fun `content state builds sections correctly by month`() = runTest {
        // Given - plays from different months
        val plays = listOf(
            createPlay(localPlayId = 1, gameName = "Azul", date = Instant.parse("2024-01-15T20:00:00Z")),
            createPlay(localPlayId = 2, gameName = "Ark Nova", date = Instant.parse("2024-01-10T18:00:00Z")),
            createPlay(localPlayId = 3, gameName = "Brass: Birmingham", date = Instant.parse("2023-12-20T15:00:00Z")),
            createPlay(localPlayId = 4, gameName = "Catan", date = Instant.parse("2023-12-10T12:00:00Z"))
        )
        fakePlaysRepository.setPlays(plays)
        fakePlaysRepository.setTotalPlaysCount(4)
        fakePlaysRepository.setUniqueGamesCount(4)

        // Then
        val state = awaitUiStateAfterDebounce<PlaysUiState.Content>(viewModel)

        // Should have 2 sections (January 2024 and December 2023)
        assertEquals(2, state.sections.size)

        // Most recent month first (January 2024)
        assertEquals(2024, state.sections[0].monthYearDate.year)
        assertEquals(1, state.sections[0].monthYearDate.monthValue)
        assertEquals(2, state.sections[0].plays.size)

        // Then December 2023
        assertEquals(2023, state.sections[1].monthYearDate.year)
        assertEquals(12, state.sections[1].monthYearDate.monthValue)
        assertEquals(2, state.sections[1].plays.size)
    }

    @Test
    fun `state updates reactively when repository data changes`() = runTest {
        // Given - initial state with one play
        val initialPlays = listOf(createPlay(localPlayId = 1, gameName = "Azul"))
        fakePlaysRepository.setPlays(initialPlays)
        fakePlaysRepository.setTotalPlaysCount(1)
        fakePlaysRepository.setUniqueGamesCount(1)

        val initialState = awaitUiStateAfterDebounce<PlaysUiState.Content>(viewModel)
        assertEquals(1L, initialState.common.playStats.totalPlays)

        // When - repository data changes
        val updatedPlays = listOf(
            createPlay(localPlayId = 1, gameName = "Azul"),
            createPlay(localPlayId = 2, gameName = "Brass: Birmingham")
        )
        fakePlaysRepository.setPlays(updatedPlays)
        fakePlaysRepository.setTotalPlaysCount(2)
        fakePlaysRepository.setUniqueGamesCount(2)

        // Then - state should update
        val updatedState = awaitUiStateAfterDebounce<PlaysUiState.Content>(viewModel) {
            it is PlaysUiState.Content && it.common.playStats.totalPlays == 2L
        }
        assertEquals(2L, updatedState.common.playStats.totalPlays)
    }

    @Test
    fun `content state includes correct play stats`() = runTest {
        // Given - plays in current year and previous year
        val plays = listOf(
            createPlay(localPlayId = 1, gameName = "Azul", date = Instant.parse("2024-01-15T20:00:00Z")),
            createPlay(localPlayId = 2, gameName = "Wingspan", date = Instant.parse("2024-01-10T18:00:00Z")),
            createPlay(localPlayId = 3, gameName = "Catan", date = Instant.parse("2023-12-20T15:00:00Z"))
        )
        fakePlaysRepository.setPlays(plays)
        fakePlaysRepository.setTotalPlaysCount(3)
        fakePlaysRepository.setUniqueGamesCount(3)
        fakePlaysRepository.setPlaysCountForPeriod(2) // 2 plays in 2024

        // Then
        val state = awaitUiStateAfterDebounce<PlaysUiState.Content>(viewModel)

        assertEquals(3L, state.common.playStats.totalPlays)
        assertEquals(3L, state.common.playStats.uniqueGamesCount)
        assertEquals(2L, state.common.playStats.playsThisYear) // Only 2024 plays
        assertEquals(2024, state.common.playStats.currentYear)
    }

    @Test
    fun `isRefreshing is false in content state`() = runTest {
        // Given
        val plays = listOf(createPlay(localPlayId = 1, gameName = "Azul"))
        fakePlaysRepository.setPlays(plays)
        fakePlaysRepository.setTotalPlaysCount(1)
        fakePlaysRepository.setUniqueGamesCount(1)

        // Then
        val state = awaitUiStateAfterDebounce<PlaysUiState.Content>(viewModel)
        assertFalse(state.common.isRefreshing)
    }

    @Test
    fun `Refresh event enqueues plays sync`() = runTest {
        val plays = listOf(createPlay(localPlayId = 1, gameName = "Azul"))
        fakePlaysRepository.setPlays(plays)
        fakePlaysRepository.setTotalPlaysCount(1)
        fakePlaysRepository.setUniqueGamesCount(1)
        awaitUiStateAfterDebounce<PlaysUiState.Content>(viewModel)
        advanceUntilIdle()

        viewModel.onEvent(PlaysEvent.ActionEvent.Refresh)
        advanceUntilIdle()

        assertEquals(2, fakeSyncManager.playsSyncEnqueueCount)
    }

    @Test
    fun `Refresh event enqueues plays sync even when recent auto sync is skipped`() = runTest {
        fakeSyncTimeRepository.markCompleted(
            SyncType.PLAYS,
            testClock.instant().minusSeconds(5 * 60)
        )
        fakeSyncManager = FakeSyncManager()
        viewModel = PlaysViewModel(
            reducer = PlaysReducer(),
            effectProducer = PlaysEffectProducer(),
            observePlaysScreenData = observePlaysScreenDataUseCase,
            observeSyncState = ObserveSyncStateUseCase(fakeSyncTimeRepository),
            shouldAutoSyncOnScreenEnter = ShouldAutoSyncOnScreenEnterUseCase(fakeSyncTimeRepository, testClock),
            syncManager = fakeSyncManager
        )
        val plays = listOf(createPlay(localPlayId = 1, gameName = "Azul"))
        fakePlaysRepository.setPlays(plays)
        fakePlaysRepository.setTotalPlaysCount(1)
        fakePlaysRepository.setUniqueGamesCount(1)
        awaitUiStateAfterDebounce<PlaysUiState.Content>(viewModel)
        advanceUntilIdle()

        assertEquals(0, fakeSyncManager.playsSyncEnqueueCount)

        viewModel.onEvent(PlaysEvent.ActionEvent.Refresh)
        advanceUntilIdle()

        assertEquals(1, fakeSyncManager.playsSyncEnqueueCount)
    }

    @Test
    fun `background plays sync does not auto show refresh indicator`() = runTest {
        val plays = listOf(createPlay(localPlayId = 1, gameName = "Azul"))
        fakePlaysRepository.setPlays(plays)
        fakePlaysRepository.setTotalPlaysCount(1)
        fakePlaysRepository.setUniqueGamesCount(1)
        awaitUiStateAfterDebounce<PlaysUiState.Content>(viewModel)

        fakeSyncTimeRepository.markStarted(SyncType.PLAYS)
        advanceUntilIdle()

        val state = viewModel.uiState.value as PlaysUiState.Content
        assertFalse(state.common.isRefreshing)
    }

    @Test
    fun `manual refresh shows plays indicator until sync completes`() = runTest {
        val plays = listOf(createPlay(localPlayId = 1, gameName = "Azul"))
        fakePlaysRepository.setPlays(plays)
        fakePlaysRepository.setTotalPlaysCount(1)
        fakePlaysRepository.setUniqueGamesCount(1)
        
        viewModel.uiState.test {
            advanceUntilIdle()

            var state: PlaysUiState
            do {
                state = awaitItem()
            } while (state is PlaysUiState.Loading)

            viewModel.onEvent(PlaysEvent.ActionEvent.Refresh)
            advanceUntilIdle()

            var refreshingState: PlaysUiState.Content
            do {
                refreshingState = awaitItem().assertState()
            } while (!refreshingState.common.isRefreshing)
            assertTrue(refreshingState.common.isRefreshing)

            fakeSyncTimeRepository.markStarted(SyncType.PLAYS)
            advanceUntilIdle()

            fakeSyncTimeRepository.markCompleted(SyncType.PLAYS, testClock.instant())
            advanceUntilIdle()

            var finalState: PlaysUiState.Content
            do {
                finalState = awaitItem().assertState()
            } while (finalState.common.isRefreshing)
            assertFalse(finalState.common.isRefreshing)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `LogPlayClicked event does not crash`() = runTest {
        // Given
        val plays = listOf(createPlay(localPlayId = 1, gameName = "Azul"))
        fakePlaysRepository.setPlays(plays)
        fakePlaysRepository.setTotalPlaysCount(1)
        fakePlaysRepository.setUniqueGamesCount(1)

        // When - LogPlayClicked is triggered (currently a no-op)
        viewModel.onEvent(PlaysEvent.ActionEvent.LogPlayClicked)
        advanceUntilIdle()

        // Then - no crash, state remains consistent
        val state = awaitUiStateAfterDebounce<PlaysUiState.Content>(viewModel)
        assertEquals(1L, state.common.playStats.totalPlays)
    }

    @Test
    fun `plays are sorted in reverse chronological order within sections`() = runTest {
        // Given - plays from same month but different days
        val plays = listOf(
            createPlay(localPlayId = 1, gameName = "Azul", date = Instant.parse("2024-01-05T20:00:00Z")),
            createPlay(localPlayId = 2, gameName = "Wingspan", date = Instant.parse("2024-01-15T18:00:00Z")),
            createPlay(localPlayId = 3, gameName = "Catan", date = Instant.parse("2024-01-10T12:00:00Z"))
        )
        fakePlaysRepository.setPlays(plays)
        fakePlaysRepository.setTotalPlaysCount(3)
        fakePlaysRepository.setUniqueGamesCount(3)

        // Then
        val state = awaitUiStateAfterDebounce<PlaysUiState.Content>(viewModel)

        // All in same section (January 2024)
        assertEquals(1, state.sections.size)
        val section = state.sections[0]
        assertEquals(3, section.plays.size)

        // Most recent first
        assertEquals("Wingspan", section.plays[0].gameName) // Jan 15
        assertEquals("Catan", section.plays[1].gameName)    // Jan 10
        assertEquals("Azul", section.plays[2].gameName)     // Jan 5
    }

    @Test
    fun `empty state with NO_PLAYS shows correct stats`() = runTest {
        // Given - empty plays
        fakePlaysRepository.setPlays(emptyList())
        fakePlaysRepository.setTotalPlaysCount(0)
        fakePlaysRepository.setUniqueGamesCount(0)

        // Then
        val state = awaitUiStateAfterDebounce<PlaysUiState.Empty>(viewModel)
        assertEquals(EmptyReason.NO_PLAYS, state.reason)
        assertEquals(0L, state.common.playStats.totalPlays)
        assertEquals(0L, state.common.playStats.uniqueGamesCount)
    }

    @Test
    fun `empty state with NO_SEARCH_RESULTS shows correct stats from before search`() = runTest {
        // Given - plays with games
        val plays = listOf(
            createPlay(localPlayId = 1, gameName = "Azul"),
            createPlay(localPlayId = 2, gameName = "Catan"),
            createPlay(localPlayId = 3, gameName = "Wingspan")
        )
        fakePlaysRepository.setPlays(plays)
        fakePlaysRepository.setTotalPlaysCount(3)
        fakePlaysRepository.setUniqueGamesCount(3)

        // When - search returns no results
        viewModel.onEvent(PlaysEvent.SearchChanged("NonexistentGame"))
        fakePlaysRepository.setPlays(emptyList())
        fakePlaysRepository.setTotalPlaysCount(3)
        fakePlaysRepository.setUniqueGamesCount(3)

        // Then - stats should still reflect total data (not filtered)
        val state = awaitUiStateAfterDebounce<PlaysUiState.Empty>(viewModel)
        assertEquals(EmptyReason.NO_SEARCH_RESULTS, state.reason)
        // Note: Stats come from ObservePlayStatsUseCase which observes total counts,
        // not the filtered plays
        assertEquals(3L, state.common.playStats.totalPlays)
        assertEquals(3L, state.common.playStats.uniqueGamesCount)
    }

    suspend inline fun <reified T : PlaysUiState> TestScope.awaitUiStateAfterDebounce(
        viewModel: PlaysViewModel,
        crossinline predicate: (PlaysUiState) -> Boolean = { it !is PlaysUiState.Loading }
    ): T {
        return awaitUiStateMatching(
            viewModel.uiState,
            DebounceDurations.SearchQuery,
            predicate = predicate
        )
    }
}
