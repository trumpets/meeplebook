package app.meeplebook.feature.overview

import app.meeplebook.R
import app.meeplebook.core.collection.FakeCollectionRepository
import app.meeplebook.core.collection.domain.ObserveCollectionHighlightsUseCase
import app.meeplebook.core.collection.domain.ObserveCollectionSummaryUseCase
import app.meeplebook.core.collection.model.CollectionItem
import app.meeplebook.core.collection.model.GameSubtype
import app.meeplebook.core.plays.FakePlaysRepository
import app.meeplebook.core.plays.PlayTestFactory.createPlay
import app.meeplebook.core.plays.domain.ObserveRecentPlaysUseCase
import app.meeplebook.core.plays.model.PlayId
import app.meeplebook.core.sync.FakeSyncTimeRepository
import app.meeplebook.core.sync.domain.ObserveFullSyncStateUseCase
import app.meeplebook.core.sync.manager.FakeSyncManager
import app.meeplebook.core.sync.model.SyncType
import app.meeplebook.core.ui.FakeStringProvider
import app.meeplebook.core.ui.asString
import app.meeplebook.feature.overview.domain.ObserveOverviewUseCase
import app.meeplebook.feature.overview.effect.OverviewEffectProducer
import app.meeplebook.feature.overview.effect.OverviewUiEffect
import app.meeplebook.feature.overview.reducer.OverviewReducer
import app.meeplebook.testutils.assertState
import app.meeplebook.testutils.awaitUiStateMatching
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

    private lateinit var fakeCollectionRepository: FakeCollectionRepository
    private lateinit var fakePlaysRepository: FakePlaysRepository
    private lateinit var fakeSyncTimeRepository: FakeSyncTimeRepository
    private lateinit var fakeSyncManager: FakeSyncManager
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

        fakeCollectionRepository = FakeCollectionRepository()
        fakePlaysRepository = FakePlaysRepository()
        fakeSyncTimeRepository = FakeSyncTimeRepository()
        fakeSyncManager = FakeSyncManager()
        fakeStringProvider = FakeStringProvider().apply {
            setString(R.string.sync_in_progress, "Syncing…")
            setString(R.string.sync_failed_error, "Failed to sync data. Please try again.")
            setString(R.string.sync_never, "Never synced")
            setString(R.string.sync_last_synced, "Last synced: %s")
            setString(R.string.sync_minutes_ago, "%d min ago")
            setString(R.string.sync_one_hour_ago, "1 hour ago")
            setString(R.string.sync_hours_ago, "%d hours ago")
            setString(R.string.sync_one_day_ago, "1 day ago")
            setString(R.string.sync_days_ago, "%d days ago")
            setString(R.string.sync_just_now, "just now")
        }

        val observeStats = app.meeplebook.core.stats.domain.ObserveCollectionPlayStatsUseCase(
            observeCollectionSummary = ObserveCollectionSummaryUseCase(fakeCollectionRepository),
            playsRepository = fakePlaysRepository,
            clock = testClock
        )
        val observeRecentPlays = ObserveRecentPlaysUseCase(fakePlaysRepository)
        val observeHighlights = ObserveCollectionHighlightsUseCase(fakeCollectionRepository)
        val observeFullSyncState = ObserveFullSyncStateUseCase(fakeSyncTimeRepository)

        val observeOverviewUseCase = ObserveOverviewUseCase(
            observeStats = observeStats,
            observeRecentPlays = observeRecentPlays,
            observeHighlights = observeHighlights,
            observeFullSyncState = observeFullSyncState
        )

        viewModel = OverviewViewModel(
            reducer = OverviewReducer(),
            effectProducer = OverviewEffectProducer(),
            observeOverviewUseCase = observeOverviewUseCase,
            syncManager = fakeSyncManager
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
    fun `refresh event enqueues full sync`() = runTest {
        viewModel.onEvent(OverviewEvent.ActionEvent.Refresh)
        advanceUntilIdle()

        assertEquals(1, fakeSyncManager.fullSyncEnqueueCount)
    }

    @Test
    fun `persisted full sync state drives refreshing`() = runTest {
        fakeSyncTimeRepository.markStarted(SyncType.COLLECTION)

        val state = awaitContentUiState(viewModel) { it.isRefreshing }
        assertTrue(state.isRefreshing)
    }

    @Test
    fun `failed full sync updates overview sync status text`() = runTest {
        fakeSyncTimeRepository.markFailed(SyncType.COLLECTION, "NetworkError")
        advanceUntilIdle()

        val state = awaitContentUiState(viewModel)
        assertEquals(
            "Failed to sync data. Please try again.",
            state.syncStatusUiText.asString(fakeStringProvider)
        )
    }

    @Test
    fun `completed full sync updates overview sync status text`() = runTest {
        fakeSyncTimeRepository.updateFullSyncTime(Instant.now())
        advanceUntilIdle()

        val state = awaitContentUiState(viewModel)
        assertEquals(
            "Last synced: just now",
            state.syncStatusUiText.asString(fakeStringProvider)
        )
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

}
