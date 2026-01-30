package app.meeplebook.feature.plays

import app.cash.turbine.test
import app.meeplebook.R
import app.meeplebook.core.auth.FakeAuthRepository
import app.meeplebook.core.model.AuthCredentials
import app.meeplebook.core.plays.FakePlaysRepository
import app.meeplebook.core.plays.PlayTestFactory.createPlay
import app.meeplebook.core.plays.domain.ObservePlayStatsUseCase
import app.meeplebook.core.plays.domain.ObservePlaysUseCase
import app.meeplebook.core.plays.model.Play
import app.meeplebook.core.plays.model.PlayError
import app.meeplebook.core.result.AppResult
import app.meeplebook.core.sync.FakeSyncTimeRepository
import app.meeplebook.core.sync.domain.SyncPlaysUseCase
import app.meeplebook.core.ui.asString
import app.meeplebook.core.util.DebounceDurations
import app.meeplebook.feature.plays.domain.BuildPlaysSectionsUseCase
import app.meeplebook.feature.plays.domain.ObservePlaysScreenDataUseCase
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
    private lateinit var fakeAuthRepository: FakeAuthRepository
    private lateinit var fakePlaysRepository: FakePlaysRepository
    private lateinit var fakeSyncTimeRepository: FakeSyncTimeRepository
    private lateinit var observePlaysScreenDataUseCase: ObservePlaysScreenDataUseCase
    private lateinit var syncPlaysUseCase: SyncPlaysUseCase

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
        fakeAuthRepository = FakeAuthRepository()
        fakePlaysRepository = FakePlaysRepository()
        fakeSyncTimeRepository = FakeSyncTimeRepository()

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

        syncPlaysUseCase = SyncPlaysUseCase(
            authRepository = fakeAuthRepository,
            playsRepository = fakePlaysRepository,
            syncTimeRepository = fakeSyncTimeRepository,
            clock = testClock
        )

        // Create ViewModel
        viewModel = PlaysViewModel(
            observePlaysScreenData = observePlaysScreenDataUseCase,
            syncPlays = syncPlaysUseCase
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
    fun `empty plays shows Empty state with NO_PLAYS reason`() = runTest {
        // Given - empty plays
        fakePlaysRepository.setPlays(emptyList())
        fakePlaysRepository.setTotalPlaysCount(0)
        fakePlaysRepository.setUniqueGamesCount(0)

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
            createPlay(id = 1, gameName = "Azul", date = Instant.parse("2024-01-15T20:00:00Z")),
            createPlay(id = 2, gameName = "Brass: Birmingham", date = Instant.parse("2024-01-10T18:00:00Z"))
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
        val plays = listOf(createPlay(id = 1, gameName = "Azul"))
        fakePlaysRepository.setPlays(plays)
        fakePlaysRepository.setTotalPlaysCount(1)
        fakePlaysRepository.setUniqueGamesCount(1)

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
            createPlay(id = 1, gameName = "Azul"),
            createPlay(id = 2, gameName = "Brass: Birmingham")
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
        val plays = listOf(createPlay(id = 1, gameName = "Azul"))
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
        val effects = mutableListOf<PlaysUiEffects>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiEffect.collect { effects.add(it) }
        }

        // When
        viewModel.onEvent(PlaysEvent.PlayClicked(123))
        advanceUntilIdle()

        // Then
        assertEquals(1, effects.size)
        assertTrue(effects[0] is PlaysUiEffects.NavigateToPlay)
        assertEquals(123L, (effects[0] as PlaysUiEffects.NavigateToPlay).playId)

        job.cancel()
    }

    @Test
    fun `content state builds sections correctly by month`() = runTest {
        // Given - plays from different months
        val plays = listOf(
            createPlay(id = 1, gameName = "Azul", date = Instant.parse("2024-01-15T20:00:00Z")),
            createPlay(id = 2, gameName = "Ark Nova", date = Instant.parse("2024-01-10T18:00:00Z")),
            createPlay(id = 3, gameName = "Brass: Birmingham", date = Instant.parse("2023-12-20T15:00:00Z")),
            createPlay(id = 4, gameName = "Catan", date = Instant.parse("2023-12-10T12:00:00Z"))
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
        val initialPlays = listOf(createPlay(id = 1, gameName = "Azul"))
        fakePlaysRepository.setPlays(initialPlays)
        fakePlaysRepository.setTotalPlaysCount(1)
        fakePlaysRepository.setUniqueGamesCount(1)

        val initialState = awaitUiStateAfterDebounce<PlaysUiState.Content>(viewModel)
        assertEquals(1L, initialState.common.playStats.totalPlays)

        // When - repository data changes
        val updatedPlays = listOf(
            createPlay(id = 1, gameName = "Azul"),
            createPlay(id = 2, gameName = "Brass: Birmingham")
        )
        fakePlaysRepository.setPlays(updatedPlays)
        fakePlaysRepository.setTotalPlaysCount(2)
        fakePlaysRepository.setUniqueGamesCount(2)

        // Then - state should update
        val updatedState = awaitUiStateAfterDebounce<PlaysUiState.Content>(viewModel)
        assertEquals(2L, updatedState.common.playStats.totalPlays)
    }

    @Test
    fun `content state includes correct play stats`() = runTest {
        // Given - plays in current year and previous year
        val plays = listOf(
            createPlay(id = 1, gameName = "Azul", date = Instant.parse("2024-01-15T20:00:00Z")),
            createPlay(id = 2, gameName = "Wingspan", date = Instant.parse("2024-01-10T18:00:00Z")),
            createPlay(id = 3, gameName = "Catan", date = Instant.parse("2023-12-20T15:00:00Z"))
        )
        fakePlaysRepository.setPlays(plays)
        fakePlaysRepository.setTotalPlaysCount(3)
        fakePlaysRepository.setUniqueGamesCount(3)

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
        val plays = listOf(createPlay(id = 1, gameName = "Azul"))
        fakePlaysRepository.setPlays(plays)
        fakePlaysRepository.setTotalPlaysCount(1)
        fakePlaysRepository.setUniqueGamesCount(1)

        // Then
        val state = awaitUiStateAfterDebounce<PlaysUiState.Content>(viewModel)
        assertFalse(state.common.isRefreshing)
    }

    @Test
    fun `Refresh event triggers sync and sets isRefreshing to true then false`() = runTest {
        // Given - logged in user and plays
        val user = AuthCredentials(username = "testuser", password = "password")
        fakeAuthRepository.setCurrentUser(user)
        val plays = listOf(createPlay(id = 1, gameName = "Azul"))
        fakePlaysRepository.setPlays(plays)
        fakePlaysRepository.setTotalPlaysCount(1)
        fakePlaysRepository.setUniqueGamesCount(1)
        fakePlaysRepository.syncPlaysResult = AppResult.Success(plays)

        val initialState = awaitUiStateAfterDebounce<PlaysUiState.Content>(viewModel)
        assertFalse(initialState.common.isRefreshing)

        // When - Refresh event is triggered
        viewModel.onEvent(PlaysEvent.Refresh)
        advanceUntilIdle()

        // Then - sync was called
        assertEquals(1, fakePlaysRepository.syncCallCount)
        assertEquals("testuser", fakePlaysRepository.lastSyncUsername)

        // And - isRefreshing returns to false
        val finalState = viewModel.uiState.value as PlaysUiState.Content
        assertFalse(finalState.common.isRefreshing)
    }

    @Test
    fun `Refresh event handles sync failure gracefully`() = runTest {
        // Given - logged in user and plays
        val user = AuthCredentials(username = "testuser", password = "password")
        fakeAuthRepository.setCurrentUser(user)
        val plays = listOf(createPlay(id = 1, gameName = "Azul"))
        fakePlaysRepository.setPlays(plays)
        fakePlaysRepository.setTotalPlaysCount(1)
        fakePlaysRepository.setUniqueGamesCount(1)
        fakePlaysRepository.syncPlaysResult = AppResult.Failure(PlayError.NetworkError)

        val initialState = awaitUiStateAfterDebounce<PlaysUiState.Content>(viewModel)
        assertFalse(initialState.common.isRefreshing)

        // Given - collect effects
        val effects = mutableListOf<PlaysUiEffects>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiEffect.collect { effects.add(it) }
        }

        // When - Refresh event is triggered
        viewModel.onEvent(PlaysEvent.Refresh)
        advanceUntilIdle()

        // Then - sync was attempted
        assertEquals(1, fakePlaysRepository.syncCallCount)

        // And - isRefreshing returns to false even after error
        val finalState = viewModel.uiState.value as PlaysUiState.Content
        assertFalse(finalState.common.isRefreshing)

        // And - ShowSnackbar effect was emitted
        assertEquals(1, effects.size)
        assertTrue(effects[0] is PlaysUiEffects.ShowSnackbar)

        job.cancel()
    }

    @Test
    fun `Refresh event when not logged in does not call sync`() = runTest {
        // Given - no logged in user but plays exist
        val plays = listOf(createPlay(id = 1, gameName = "Azul"))
        fakePlaysRepository.setPlays(plays)
        fakePlaysRepository.setTotalPlaysCount(1)
        fakePlaysRepository.setUniqueGamesCount(1)

        awaitUiStateAfterDebounce<PlaysUiState.Content>(viewModel)

        // When - Refresh event is triggered
        viewModel.onEvent(PlaysEvent.Refresh)
        advanceUntilIdle()

        // Then - sync was not called (use case returned NotLoggedIn error before attempting repository sync)
        assertEquals(0, fakePlaysRepository.syncCallCount)
    }

    @Test
    fun `LogPlayClicked event does not crash`() = runTest {
        // Given
        val plays = listOf(createPlay(id = 1, gameName = "Azul"))
        fakePlaysRepository.setPlays(plays)
        fakePlaysRepository.setTotalPlaysCount(1)
        fakePlaysRepository.setUniqueGamesCount(1)

        // When - LogPlayClicked is triggered (currently a no-op)
        viewModel.onEvent(PlaysEvent.LogPlayClicked)
        advanceUntilIdle()

        // Then - no crash, state remains consistent
        val state = awaitUiStateAfterDebounce<PlaysUiState.Content>(viewModel)
        assertEquals(1L, state.common.playStats.totalPlays)
    }

    @Test
    fun `plays are sorted in reverse chronological order within sections`() = runTest {
        // Given - plays from same month but different days
        val plays = listOf(
            createPlay(id = 1, gameName = "Azul", date = Instant.parse("2024-01-05T20:00:00Z")),
            createPlay(id = 2, gameName = "Wingspan", date = Instant.parse("2024-01-15T18:00:00Z")),
            createPlay(id = 3, gameName = "Catan", date = Instant.parse("2024-01-10T12:00:00Z"))
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
            createPlay(id = 1, gameName = "Azul"),
            createPlay(id = 2, gameName = "Catan"),
            createPlay(id = 3, gameName = "Wingspan")
        )
        fakePlaysRepository.setPlays(plays)
        fakePlaysRepository.setTotalPlaysCount(3)
        fakePlaysRepository.setUniqueGamesCount(3)

        // When - search returns no results
        viewModel.onEvent(PlaysEvent.SearchChanged("NonexistentGame"))
        fakePlaysRepository.setPlays(emptyList())

        // Then - stats should still reflect total data (not filtered)
        val state = awaitUiStateAfterDebounce<PlaysUiState.Empty>(viewModel)
        assertEquals(EmptyReason.NO_SEARCH_RESULTS, state.reason)
        // Note: Stats come from ObservePlayStatsUseCase which observes total counts,
        // not the filtered plays
        assertEquals(3L, state.common.playStats.totalPlays)
        assertEquals(3L, state.common.playStats.uniqueGamesCount)
    }

    suspend inline fun <reified T : PlaysUiState> TestScope.awaitUiStateAfterDebounce(
        viewModel: PlaysViewModel
    ): T {
        return awaitUiState(
            viewModel.uiState,
            DebounceDurations.SearchQuery,
            skipWhile = { it is PlaysUiState.Loading }
        )
    }
}
