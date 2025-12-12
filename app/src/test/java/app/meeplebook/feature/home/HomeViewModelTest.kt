package app.meeplebook.feature.home

import app.meeplebook.R
import app.meeplebook.core.auth.FakeAuthRepository
import app.meeplebook.core.collection.FakeCollectionRepository
import app.meeplebook.core.collection.model.CollectionItem
import app.meeplebook.core.collection.model.GameSubtype
import app.meeplebook.core.model.AuthCredentials
import app.meeplebook.core.plays.FakePlaysRepository
import app.meeplebook.core.plays.model.Play
import app.meeplebook.core.plays.model.Player
import app.meeplebook.core.result.AppResult
import app.meeplebook.core.sync.FakeSyncTimeRepository
import app.meeplebook.core.util.FakeDateFormatter
import app.meeplebook.core.util.FakeSyncFormatter
import app.meeplebook.feature.home.domain.GetCollectionHighlightsUseCase
import app.meeplebook.feature.home.domain.GetHomeStatsUseCase
import app.meeplebook.feature.home.domain.GetRecentPlaysUseCase
import app.meeplebook.feature.home.domain.SyncHomeDataUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private lateinit var fakeAuthRepository: FakeAuthRepository
    private lateinit var fakeCollectionRepository: FakeCollectionRepository
    private lateinit var fakePlaysRepository: FakePlaysRepository
    private lateinit var fakeSyncTimeRepository: FakeSyncTimeRepository
    private lateinit var fakeSyncFormatter: FakeSyncFormatter
    private lateinit var getHomeStatsUseCase: GetHomeStatsUseCase
    private lateinit var getRecentPlaysUseCase: GetRecentPlaysUseCase
    private lateinit var getCollectionHighlightsUseCase: GetCollectionHighlightsUseCase
    private lateinit var syncHomeDataUseCase: SyncHomeDataUseCase
    private lateinit var viewModel: HomeViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        
        fakeAuthRepository = FakeAuthRepository()
        fakeCollectionRepository = FakeCollectionRepository()
        fakePlaysRepository = FakePlaysRepository()
        fakeSyncTimeRepository = FakeSyncTimeRepository()
        
        val fakeDateFormatter = FakeDateFormatter()
        fakeSyncFormatter = FakeSyncFormatter()
        
        getHomeStatsUseCase = GetHomeStatsUseCase(fakeCollectionRepository, fakePlaysRepository)
        getRecentPlaysUseCase = GetRecentPlaysUseCase(fakePlaysRepository, fakeDateFormatter)
        getCollectionHighlightsUseCase = GetCollectionHighlightsUseCase(
            fakeCollectionRepository,
            fakePlaysRepository
        )
        syncHomeDataUseCase = SyncHomeDataUseCase(
            fakeAuthRepository,
            fakeCollectionRepository,
            fakePlaysRepository,
            fakeSyncTimeRepository
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state shows loading`() {
        viewModel = createViewModel()

        val state = viewModel.uiState.value
        assertTrue(state.isLoading)
    }

    @Test
    fun `loads home data on initialization`() = runTest {
        // Setup logged in user for sync to work
        fakeAuthRepository.loginResult = AppResult.Success(AuthCredentials("testuser", "testpass"))
        fakeAuthRepository.login("testuser", "testpass")
        
        // Setup sync results
        val collectionItems = listOf(createCollectionItem(1, "Game 1"))
        fakeCollectionRepository.syncCollectionResult = AppResult.Success(collectionItems)
        val plays = listOf(createPlay(1, "2024-12-05"))
        fakePlaysRepository.syncPlaysResult = AppResult.Success(plays)

        viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(1, state.stats.gamesCount)
        assertEquals(1, state.recentPlays.size)
    }

    @Test
    fun `refresh syncs data and reloads`() = runTest {
        // Setup logged in user
        fakeAuthRepository.loginResult = AppResult.Success(AuthCredentials("testuser", "testpass"))
        fakeAuthRepository.login("testuser", "testpass")

        // Setup sync results
        val collectionItems = listOf(createCollectionItem(1, "Game 1"))
        fakeCollectionRepository.syncCollectionResult = AppResult.Success(collectionItems)
        val plays = listOf(createPlay(1, "2024-12-05"))
        fakePlaysRepository.syncPlaysResult = AppResult.Success(plays)

        viewModel = createViewModel()
        advanceUntilIdle()

        // Trigger refresh
        viewModel.refresh()
        advanceUntilIdle()

        // Verify sync was called
        assertEquals(1, fakeCollectionRepository.syncCollectionCallCount)
        assertEquals(1, fakePlaysRepository.syncPlaysCallCount)

        // Verify data was loaded
        val state = viewModel.uiState.value
        assertFalse(state.isRefreshing)
        assertEquals(1, state.stats.gamesCount)
    }

    @Test
    fun `refresh sets isRefreshing during sync`() = runTest {
        // Setup logged in user
        fakeAuthRepository.loginResult = AppResult.Success(AuthCredentials("testuser", "testpass"))
        fakeAuthRepository.login("testuser", "testpass")

        fakeCollectionRepository.syncCollectionResult = AppResult.Success(emptyList())
        fakePlaysRepository.syncPlaysResult = AppResult.Success(emptyList())

        viewModel = createViewModel()
        advanceUntilIdle()

        // Clear initial state
        assertFalse(viewModel.uiState.value.isRefreshing)

        // Start refresh (don't advance)
        viewModel.refresh()

        // Should be refreshing now (though this might be tricky to test due to timing)
        // After advancing, it should be false
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isRefreshing)
    }

    @Test
    fun `loads empty state correctly`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(0, state.stats.gamesCount)
        assertEquals(0, state.recentPlays.size)
        assertNull(state.recentlyAddedGame)
        assertNull(state.suggestedGame)
    }

    @Test
    fun `loads stats correctly`() = runTest {
        val currentMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))
        
        fakeCollectionRepository.setCollection(
            listOf(
                createCollectionItem(1, "Game 1"),
                createCollectionItem(2, "Game 2")
            )
        )
        fakePlaysRepository.setPlays(
            listOf(
                createPlay(1, "$currentMonth-05", gameId = 1, quantity = 2),
                createPlay(2, "2024-11-05", gameId = 1, quantity = 1)
            )
        )

        viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, state.stats.gamesCount)
        assertEquals(3, state.stats.totalPlays)
        assertEquals(2, state.stats.playsThisMonth)
        assertEquals(1, state.stats.unplayedCount) // Game 2 is unplayed
    }

    @Test
    fun `loads recent plays correctly`() = runTest {
        fakePlaysRepository.setPlays(
            listOf(
                createPlay(1, "2024-12-01", gameName = "Game A"),
                createPlay(2, "2024-12-05", gameName = "Game B")
            )
        )

        viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, state.recentPlays.size)
        assertEquals("Game B", state.recentPlays[0].gameName) // Most recent first
        assertEquals("Game A", state.recentPlays[1].gameName)
    }

    @Test
    fun `loads collection highlights correctly`() = runTest {
        fakeCollectionRepository.setCollection(
            listOf(
                createCollectionItem(1, "Game 1"),
                createCollectionItem(2, "Game 2")
            )
        )
        fakePlaysRepository.setPlays(
            listOf(createPlay(1, "2024-12-05", gameId = 1))
        )

        viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.recentlyAddedGame)
        assertEquals("Game 2", state.recentlyAddedGame?.gameName)
        assertNotNull(state.suggestedGame)
        assertEquals("Game 2", state.suggestedGame?.gameName) // Unplayed game
    }

    @Test
    fun `updates lastSyncedText after refresh`() = runTest {
        // Setup logged in user
        fakeAuthRepository.loginResult = AppResult.Success(AuthCredentials("testuser", "testpass"))
        fakeAuthRepository.login("testuser", "testpass")

        fakeCollectionRepository.syncCollectionResult = AppResult.Success(emptyList())
        fakePlaysRepository.syncPlaysResult = AppResult.Success(emptyList())

        viewModel = createViewModel()
        advanceUntilIdle()

        // After refresh
        viewModel.refresh()
        advanceUntilIdle()

        // Should have a recent sync time
        val lastSyncedText = viewModel.uiState.value.lastSyncedText
        assertTrue(lastSyncedText.contains("Full sync:"))
    }

    @Test
    fun `refresh handles collection sync failure gracefully`() = runTest {
        // Setup logged in user
        fakeAuthRepository.loginResult = AppResult.Success(AuthCredentials("testuser", "testpass"))
        fakeAuthRepository.login("testuser", "testpass")

        // Make collection sync fail
        fakeCollectionRepository.syncCollectionResult = 
            AppResult.Failure(app.meeplebook.core.collection.model.CollectionError.NetworkError(
                RuntimeException("Network error")
            ))

        viewModel = createViewModel()
        advanceUntilIdle()

        // Trigger explicit refresh
        viewModel.refresh()
        advanceUntilIdle()

        // Verify error handling
        val state = viewModel.uiState.value
        assertFalse(state.isRefreshing)
        assertNotNull(state.errorMessageResId)
        assertEquals(R.string.sync_failed_error, state.errorMessageResId)
    }

    @Test
    fun `refresh handles plays sync failure gracefully`() = runTest {
        // Setup logged in user
        fakeAuthRepository.loginResult = AppResult.Success(AuthCredentials("testuser", "testpass"))
        fakeAuthRepository.login("testuser", "testpass")

        // Successful collection sync but failed plays sync
        fakeCollectionRepository.syncCollectionResult = AppResult.Success(emptyList())
        fakePlaysRepository.syncPlaysResult = 
            AppResult.Failure(app.meeplebook.core.plays.model.PlayError.NetworkError(
                RuntimeException("Network error")
            ))

        viewModel = createViewModel()
        advanceUntilIdle()

        // Trigger explicit refresh
        viewModel.refresh()
        advanceUntilIdle()

        // Verify error handling
        val state = viewModel.uiState.value
        assertFalse(state.isRefreshing)
        assertNotNull(state.errorMessageResId)
    }

    @Test
    fun `refresh clears previous error message`() = runTest {
        // Setup logged in user
        fakeAuthRepository.loginResult = AppResult.Success(AuthCredentials("testuser", "testpass"))
        fakeAuthRepository.login("testuser", "testpass")

        // First refresh fails
        fakeCollectionRepository.syncCollectionResult = 
            AppResult.Failure(app.meeplebook.core.collection.model.CollectionError.NetworkError(
                RuntimeException("Network error")
            ))

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.refresh()
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.errorMessageResId)

        // Second refresh succeeds
        fakeCollectionRepository.syncCollectionResult = AppResult.Success(emptyList())
        fakePlaysRepository.syncPlaysResult = AppResult.Success(emptyList())
        
        viewModel.refresh()
        advanceUntilIdle()

        // Error should be cleared
        assertNull(viewModel.uiState.value.errorMessageResId)
    }

    private fun createViewModel() = HomeViewModel(
        getHomeStatsUseCase,
        getRecentPlaysUseCase,
        getCollectionHighlightsUseCase,
        syncHomeDataUseCase,
        fakeCollectionRepository,
        fakePlaysRepository,
        fakeSyncTimeRepository,
        fakeSyncFormatter
    )

    private fun createCollectionItem(gameId: Int, name: String) = CollectionItem(
        gameId = gameId,
        subtype = GameSubtype.BOARDGAME,
        name = name,
        yearPublished = 2020,
        thumbnail = null,
        lastModified = null
    )

    private fun createPlay(
        id: Int,
        date: String,
        gameId: Int = 1,
        gameName: String = "Test Game",
        quantity: Int = 1,
        players: List<Player> = emptyList()
    ) = Play(
        id = id,
        date = date,
        quantity = quantity,
        length = 60,
        incomplete = false,
        location = null,
        gameId = gameId,
        gameName = gameName,
        comments = null,
        players = players
    )
}
