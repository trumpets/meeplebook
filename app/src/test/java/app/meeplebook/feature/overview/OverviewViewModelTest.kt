package app.meeplebook.feature.overview

import app.meeplebook.R
import app.meeplebook.core.auth.FakeAuthRepository
import app.meeplebook.core.collection.FakeCollectionRepository
import app.meeplebook.core.collection.domain.ObserveCollectionHighlightsUseCase
import app.meeplebook.core.collection.domain.ObserveCollectionSummaryUseCase
import app.meeplebook.core.collection.model.CollectionError
import app.meeplebook.core.collection.model.CollectionItem
import app.meeplebook.core.collection.model.GameSubtype
import app.meeplebook.core.model.AuthCredentials
import app.meeplebook.core.plays.FakePlaysRepository
import app.meeplebook.core.plays.PlayTestFactory.createPlay
import app.meeplebook.core.plays.domain.ObserveRecentPlaysUseCase
import app.meeplebook.core.plays.model.PlayError
import app.meeplebook.core.plays.model.PlayId
import app.meeplebook.core.result.AppResult
import app.meeplebook.core.sync.FakeSyncTimeRepository
import app.meeplebook.core.sync.SyncRunner
import app.meeplebook.core.sync.domain.ObserveLastFullSyncUseCase
import app.meeplebook.core.sync.domain.SyncCollectionUseCase
import app.meeplebook.core.sync.domain.SyncPlaysUseCase
import app.meeplebook.core.sync.domain.SyncUserDataUseCase
import app.meeplebook.core.ui.uiTextRes
import app.meeplebook.feature.overview.domain.ObserveOverviewUseCase
import app.meeplebook.feature.overview.effect.OverviewEffectProducer
import app.meeplebook.feature.overview.effect.OverviewUiEffect
import app.meeplebook.feature.overview.reducer.OverviewReducer
import app.meeplebook.testutils.assertState
import app.meeplebook.testutils.awaitUiStateMatching
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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

@OptIn(ExperimentalCoroutinesApi::class)
class OverviewViewModelTest {

    private lateinit var fakeAuthRepository: FakeAuthRepository
    private lateinit var fakeCollectionRepository: FakeCollectionRepository
    private lateinit var fakePlaysRepository: FakePlaysRepository
    private lateinit var fakeSyncTimeRepository: FakeSyncTimeRepository
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

        val user = AuthCredentials(username = "testuser", password = "password")
        fakeAuthRepository.setCurrentUser(user)

        val observeStats = app.meeplebook.core.stats.domain.ObserveCollectionPlayStatsUseCase(
            observeCollectionSummary = ObserveCollectionSummaryUseCase(fakeCollectionRepository),
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

        val syncCollectionUseCase = SyncCollectionUseCase(
            authRepository = fakeAuthRepository,
            collectionRepository = fakeCollectionRepository,
            syncRunner = SyncRunner(
                syncTimeRepository = fakeSyncTimeRepository,
                clock = testClock
            )
        )
        val syncPlaysUseCase = SyncPlaysUseCase(
            authRepository = fakeAuthRepository,
            playsRepository = fakePlaysRepository,
            syncRunner = SyncRunner(
                syncTimeRepository = fakeSyncTimeRepository,
                clock = testClock
            )
        )
        val syncUserDataUseCase = SyncUserDataUseCase(
            syncCollection = syncCollectionUseCase,
            syncPlays = syncPlaysUseCase
        )

        viewModel = OverviewViewModel(
            reducer = OverviewReducer(),
            effectProducer = OverviewEffectProducer(),
            observeOverviewUseCase = observeOverviewUseCase,
            syncUserDataUseCase = syncUserDataUseCase
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is loading`() {
        assertEquals(OverviewUiState.Loading, viewModel.uiState.value)
    }

    @Test
    fun `uiState displays overview data correctly`() = runTest {
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

        val recentPlays = listOf(createPlay(localPlayId = 1, gameName = "Catan"))
        fakePlaysRepository.setRecentPlays(recentPlays)

        val state = awaitContentUiState(viewModel)

        assertEquals(50L, state.stats.gamesCount)
        assertEquals(100L, state.stats.totalPlays)
        assertEquals(15L, state.stats.playsThisMonth)
        assertEquals(10L, state.stats.unplayedCount)
        assertEquals(1, state.recentPlays.size)
        assertEquals("Azul", state.recentlyAddedGame?.gameName)
        assertFalse(state.isRefreshing)
    }

    @Test
    fun `refresh succeeds and returns content state`() = runTest {
        fakeCollectionRepository.syncCollectionResult = AppResult.Success(emptyList())
        fakePlaysRepository.syncPlaysResult = AppResult.Success(Unit)

        viewModel.onEvent(OverviewEvent.ActionEvent.Refresh)
        advanceUntilIdle()

        val state = awaitContentUiState(viewModel)
        assertFalse(state.isRefreshing)
    }

    @Test
    fun `refresh shows refreshing state while syncing`() = runTest {
        val gate = CompletableDeferred<Unit>()

        fakeCollectionRepository.syncCollectionResult = AppResult.Success(emptyList())
        fakePlaysRepository.syncPlaysResult = AppResult.Success(Unit)
        fakeCollectionRepository.beforeSync = { gate.await() }

        viewModel.onEvent(OverviewEvent.ActionEvent.Refresh)

        val state = awaitContentUiState(viewModel) { it.isRefreshing }
        assertTrue(state.isRefreshing)

        gate.complete(Unit)
        advanceUntilIdle()
    }

    @Test
    fun `refresh shows full screen error when not logged in`() = runTest {
        fakeAuthRepository.setCurrentUser(null)

        viewModel.onEvent(OverviewEvent.ActionEvent.Refresh)
        advanceUntilIdle()

        val state = awaitErrorUiState(viewModel)
        assertEquals(uiTextRes(R.string.sync_not_logged_in_error), state.errorMessageUiText)
    }

    @Test
    fun `refresh shows full screen error when collection sync fails`() = runTest {
        fakeCollectionRepository.syncCollectionResult = AppResult.Failure(CollectionError.NetworkError)

        viewModel.onEvent(OverviewEvent.ActionEvent.Refresh)
        advanceUntilIdle()

        val state = awaitErrorUiState(viewModel)
        assertEquals(uiTextRes(R.string.sync_collections_failed_error), state.errorMessageUiText)
    }

    @Test
    fun `refresh shows full screen error when plays sync fails`() = runTest {
        fakeCollectionRepository.syncCollectionResult = AppResult.Success(emptyList())
        fakePlaysRepository.syncPlaysResult = AppResult.Failure(PlayError.NetworkError)

        viewModel.onEvent(OverviewEvent.ActionEvent.Refresh)
        advanceUntilIdle()

        val state = awaitErrorUiState(viewModel)
        assertEquals(uiTextRes(R.string.sync_plays_failed_error), state.errorMessageUiText)
    }

    @Test
    fun `successful refresh clears previous error and returns content`() = runTest {
        fakeAuthRepository.setCurrentUser(null)
        viewModel.onEvent(OverviewEvent.ActionEvent.Refresh)
        advanceUntilIdle()
        awaitErrorUiState(viewModel)

        val user = AuthCredentials(username = "testuser", password = "password")
        fakeAuthRepository.setCurrentUser(user)
        fakeCollectionRepository.syncCollectionResult = AppResult.Success(emptyList())
        fakePlaysRepository.syncPlaysResult = AppResult.Success(Unit)

        viewModel.onEvent(OverviewEvent.ActionEvent.Refresh)
        advanceUntilIdle()

        val state = awaitContentUiState(viewModel)
        assertFalse(state.isRefreshing)
    }

    @Test
    fun `log play event emits open add play effect`() = runTest {
        val effects = mutableListOf<OverviewUiEffect>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiEffect.collect { effects.add(it) }
        }

        viewModel.onEvent(OverviewEvent.ActionEvent.LogPlayClicked)
        advanceUntilIdle()

        assertEquals(listOf(OverviewUiEffect.OpenAddPlay), effects)

        job.cancel()
    }

    @Test
    fun `recent play click emits navigate to play effect`() = runTest {
        val effects = mutableListOf<OverviewUiEffect>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiEffect.collect { effects.add(it) }
        }

        viewModel.onEvent(OverviewEvent.ActionEvent.RecentPlayClicked(PlayId.Local(42L)))
        advanceUntilIdle()

        assertEquals(
            listOf(OverviewUiEffect.NavigateToPlay(PlayId.Local(42L))),
            effects
        )

        job.cancel()
    }

    @Test
    fun `highlight click emits navigate to game effect`() = runTest {
        val effects = mutableListOf<OverviewUiEffect>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiEffect.collect { effects.add(it) }
        }

        viewModel.onEvent(OverviewEvent.ActionEvent.RecentlyAddedClicked(7L))
        advanceUntilIdle()

        assertEquals(
            listOf(OverviewUiEffect.NavigateToGame(7L)),
            effects
        )

        job.cancel()
    }

    private suspend fun TestScope.awaitContentUiState(
        viewModel: OverviewViewModel,
        predicate: (OverviewUiState.Content) -> Boolean = { true }
    ): OverviewUiState.Content {
        return awaitUiStateMatching(viewModel.uiState) {
            (it as? OverviewUiState.Content)?.let(predicate) == true
        }.assertState()
    }

    private suspend fun TestScope.awaitErrorUiState(
        viewModel: OverviewViewModel
    ): OverviewUiState.Error {
        return awaitUiStateMatching(viewModel.uiState) { it is OverviewUiState.Error }
            .assertState()
    }
}
