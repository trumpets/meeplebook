package app.meeplebook.feature.overview

import app.meeplebook.R
import app.meeplebook.core.auth.FakeAuthRepository
import app.meeplebook.core.collection.FakeCollectionRepository
import app.meeplebook.core.collection.domain.ObserveCollectionHighlightsUseCase
import app.meeplebook.core.collection.model.CollectionError
import app.meeplebook.core.collection.model.CollectionItem
import app.meeplebook.core.collection.model.GameSubtype
import app.meeplebook.core.model.AuthCredentials
import app.meeplebook.core.plays.FakePlaysRepository
import app.meeplebook.core.plays.PlayTestFactory.createPlay
import app.meeplebook.core.plays.domain.ObserveRecentPlaysUseCase
import app.meeplebook.core.plays.model.PlayError
import app.meeplebook.core.result.AppResult
import app.meeplebook.core.stats.domain.ObserveCollectionPlayStatsUseCase
import app.meeplebook.core.sync.FakeSyncTimeRepository
import app.meeplebook.core.sync.domain.ObserveLastFullSyncUseCase
import app.meeplebook.core.sync.domain.SyncUserDataUseCase
import app.meeplebook.core.ui.FakeStringProvider
import app.meeplebook.feature.overview.domain.ObserveOverviewUseCase
import app.meeplebook.testutils.awaitUiState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

/**
 * Unit tests for [OverviewViewModel].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class OverviewViewModelTest {

    private lateinit var fakeAuthRepository: FakeAuthRepository
    private lateinit var fakeCollectionRepository: FakeCollectionRepository
    private lateinit var fakePlaysRepository: FakePlaysRepository
    private lateinit var fakeSyncTimeRepository: FakeSyncTimeRepository
    private lateinit var fakeStringProvider: FakeStringProvider
    private lateinit var viewModel: OverviewViewModel

    private val testDispatcher = StandardTestDispatcher()
    private val testClock = Clock.fixed(
        Instant.parse("2024-01-15T12:00:00Z"),
        ZoneOffset.UTC
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        fakeAuthRepository = FakeAuthRepository()
        fakeCollectionRepository = FakeCollectionRepository()
        fakePlaysRepository = FakePlaysRepository()
        fakeSyncTimeRepository = FakeSyncTimeRepository()
        fakeStringProvider = FakeStringProvider()

        // Set a default user to ensure a consistent state for most tests.
        // Tests requiring no user can clear it.
        val user = AuthCredentials(username = "testuser", password = "password")
        fakeAuthRepository.setCurrentUser(user)

        // Setup string resources
        fakeStringProvider.setString(R.string.game_highlight_recently_added, "Recently Added")
        fakeStringProvider.setString(R.string.game_highlight_try_tonight, "Try Tonight?")
        fakeStringProvider.setString(R.string.sync_never, "Never synced")

        val observeStats = ObserveCollectionPlayStatsUseCase(
            collectionRepository = fakeCollectionRepository,
            playsRepository = fakePlaysRepository,
            clock = testClock
        )
        val observeRecentPlays = ObserveRecentPlaysUseCase(fakePlaysRepository)
        val observeHighlights = ObserveCollectionHighlightsUseCase(fakeCollectionRepository)
        val observeLastSync = ObserveLastFullSyncUseCase(fakeSyncTimeRepository)

        val observeOverviewUseCase = ObserveOverviewUseCase(
            observeStats = observeStats,
            observeRecentPlays = observeRecentPlays,
            observeHighlights = observeHighlights,
            observeLastSync = observeLastSync
        )

        val syncUserDataUseCase = SyncUserDataUseCase(
            authRepository = fakeAuthRepository,
            collectionRepository = fakeCollectionRepository,
            playsRepository = fakePlaysRepository,
            syncTimeRepository = fakeSyncTimeRepository,
            clock = testClock
        )

        viewModel = OverviewViewModel(
            observeOverviewUseCase = observeOverviewUseCase,
            syncUserDataUseCase = syncUserDataUseCase,
            stringProvider = fakeStringProvider
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is loading`() = runTest {
        // When
        val state = viewModel.uiState.value

        // Then
        assertTrue(state.isLoading)
        assertFalse(state.isRefreshing)
        assertNull(state.errorMessageResId)
    }

    @Test
    fun `uiState displays overview data correctly`() = runTest {
        // Given
        fakeCollectionRepository.setCollectionCount(50)
        fakeCollectionRepository.setUnplayedCount(10)
        fakePlaysRepository.setTotalPlaysCount(100)
        fakePlaysRepository.setPlaysCountForPeriod(15)

        val recentlyAdded = CollectionItem(
            gameId = 1,
            subtype = GameSubtype.BOARDGAME,
            name = "Azul",
            yearPublished = 2017,
            thumbnail = null,
            lastModifiedDate = Instant.parse("2024-01-10T10:00:00Z"),
            minPlayers = null,
            maxPlayers = null,
            minPlayTimeMinutes = null,
            maxPlayTimeMinutes = null,
            numPlays = 0
        )
        fakeCollectionRepository.setMostRecentlyAdded(recentlyAdded)

        val recentPlays = listOf(createPlay(id = 1, gameName = "Catan"))
        fakePlaysRepository.setRecentPlays(recentPlays)

        // When
        val state = awaitLoadedUiState(viewModel)

        // Then
        assertFalse(state.isLoading)
        assertEquals(50L, state.stats.gamesCount)
        assertEquals(100L, state.stats.totalPlays)
        assertEquals(15L, state.stats.playsThisMonth)
        assertEquals(10L, state.stats.unplayedCount)
        assertEquals(1, state.recentPlays.size)
        assertEquals("Azul", state.recentlyAddedGame?.gameName)
    }

    @Test
    fun `refresh succeeds and updates state`() = runTest {
        // Given
        val user = AuthCredentials(
            username = "testuser",
            password = "password"
        )
        fakeAuthRepository.setCurrentUser(user)
        fakeCollectionRepository.syncCollectionResult = AppResult.Success(emptyList())
        fakePlaysRepository.syncPlaysResult = AppResult.Success(emptyList())

        // When
        viewModel.refresh()

        // Then
        val state = awaitLoadedUiState(viewModel)
        assertFalse(state.isRefreshing)
        assertNull(state.errorMessageResId)
    }

    @Test
    fun `refresh shows refreshing state while syncing`() = runTest {
        // Given
        val gate = CompletableDeferred<Unit>()

        val user = AuthCredentials(
            username = "testuser",
            password = "password"
        )
        fakeAuthRepository.setCurrentUser(user)
        fakeCollectionRepository.syncCollectionResult = AppResult.Success(emptyList())
        fakePlaysRepository.syncPlaysResult = AppResult.Success(emptyList())

        fakeCollectionRepository.beforeSync = { gate.await() }

        // When
        viewModel.refresh()
        // Don't advance - check intermediate state

        // Then
        val state = awaitLoadedUiState(viewModel)
        assertTrue(state.isRefreshing)
        assertNull(state.errorMessageResId)

        // Cleanup: let refresh complete
        gate.complete(Unit)
    }

    @Test
    fun `refresh shows error when not logged in`() = runTest {
        // Given - no user logged in
        fakeAuthRepository.setCurrentUser(null)

        // When
        viewModel.refresh()

        // Then
        val state = awaitLoadedUiState(viewModel)
        assertFalse(state.isRefreshing)
        assertEquals(R.string.sync_not_logged_in_error, state.errorMessageResId)
    }

    @Test
    fun `refresh shows error when collection sync fails`() = runTest {
        // Given
        val user = AuthCredentials(
            username = "testuser",
            password = "password"
        )
        fakeAuthRepository.setCurrentUser(user)
        fakeCollectionRepository.syncCollectionResult = AppResult.Failure(CollectionError.NetworkError)

        // When
        viewModel.refresh()

        // Then
        val state = awaitLoadedUiState(viewModel)
        assertFalse(state.isRefreshing)
        assertEquals(R.string.sync_collections_failed_error, state.errorMessageResId)
    }

    @Test
    fun `refresh shows error when plays sync fails`() = runTest {
        // Given
        val user = AuthCredentials(
            username = "testuser",
            password = "password"
        )
        fakeAuthRepository.setCurrentUser(user)
        fakeCollectionRepository.syncCollectionResult = AppResult.Success(emptyList())
        fakePlaysRepository.syncPlaysResult = AppResult.Failure(PlayError.NetworkError)

        // When
        viewModel.refresh()

        // Then
        val state = awaitLoadedUiState(viewModel)
        assertFalse(state.isRefreshing)
        assertEquals(R.string.sync_plays_failed_error, state.errorMessageResId)
    }

    @Test
    fun `clearError removes error message`() = runTest {
        // Given - trigger an error (no user logged in)
        fakeAuthRepository.setCurrentUser(null)
        viewModel.refresh()

        // Verify error exists
        var state = awaitLoadedUiState(viewModel)
        assertEquals(R.string.sync_not_logged_in_error, state.errorMessageResId)

        // When
        viewModel.clearError()

        // Then
        state = awaitLoadedUiState(viewModel)
        assertNull(state.errorMessageResId)
    }

    @Test
    fun `refresh clears previous error`() = runTest {
        // Given - trigger an error first by logging out
        fakeAuthRepository.setCurrentUser(null)

        viewModel.refresh()

        // Verify error exists
        var state = awaitLoadedUiState(viewModel)
        assertEquals(R.string.sync_not_logged_in_error, state.errorMessageResId)

        // When - setup successful refresh
        val user = AuthCredentials(
            username = "testuser",
            password = "password"
        )
        fakeAuthRepository.setCurrentUser(user)

        fakeCollectionRepository.syncCollectionResult = AppResult.Success(emptyList())
        fakePlaysRepository.syncPlaysResult = AppResult.Success(emptyList())

        viewModel.refresh()

        // Then - error is cleared
        state = awaitLoadedUiState(viewModel)
        assertNull(state.errorMessageResId)
    }

    suspend fun TestScope.awaitLoadedUiState(
        viewModel: OverviewViewModel
    ): OverviewUiState {
        return awaitUiState(viewModel.uiState, skipWhile = { it.isLoading })
    }
}
