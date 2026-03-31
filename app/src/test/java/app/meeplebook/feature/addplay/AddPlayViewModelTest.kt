package app.meeplebook.feature.addplay

import app.cash.turbine.test
import app.meeplebook.core.collection.FakeCollectionRepository
import app.meeplebook.core.collection.domain.ObserveCollectionUseCase
import app.meeplebook.core.plays.FakePlaysRepository
import app.meeplebook.core.plays.PlayTestFactory
import app.meeplebook.core.plays.domain.CreatePlayUseCase
import app.meeplebook.core.plays.domain.ObserveColorsUsedForGameUseCase
import app.meeplebook.core.plays.domain.ObservePlayerSuggestionsUseCase
import app.meeplebook.core.plays.domain.ObserveRecentLocationsUseCase
import app.meeplebook.core.plays.domain.SearchLocationsUseCase
import app.meeplebook.core.util.DebounceDurations
import app.meeplebook.feature.addplay.effect.AddPlayEffectProducer
import app.meeplebook.feature.addplay.effect.AddPlayUiEffect
import app.meeplebook.feature.addplay.reducer.AddPlayReducer
import app.meeplebook.feature.addplay.reducer.GameSearchReducer
import app.meeplebook.feature.addplay.reducer.MetaReducer
import app.meeplebook.feature.addplay.reducer.PlayerColorReducer
import app.meeplebook.feature.addplay.reducer.PlayerEditReducer
import app.meeplebook.feature.addplay.reducer.PlayerListReducer
import app.meeplebook.feature.addplay.reducer.PlayerScoreReducer
import app.meeplebook.feature.addplay.reducer.PlayersReducer
import app.meeplebook.testutils.awaitUiStateMatching
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [AddPlayViewModel].
 *
 * Verifies state transitions through the reducer pipeline, reactive data flows,
 * domain effect handling (save/player suggestions) and UI effect emission.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AddPlayViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var fakePlaysRepository: FakePlaysRepository
    private lateinit var fakeCollectionRepository: FakeCollectionRepository
    private lateinit var viewModel: AddPlayViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        fakePlaysRepository = FakePlaysRepository()
        fakeCollectionRepository = FakeCollectionRepository()

        viewModel = buildViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // region Initial state

    @Test
    fun `initial state has no game selected`() {
        val state = viewModel.uiState.value
        assertTrue(state is AddPlayUiState.GameSearch)
        val search = state as AddPlayUiState.GameSearch
        assertNull(search.gameId)
        assertNull(search.gameName)
    }

    // endregion

    // region GameSearchEvent

    @Test
    fun `GameSearchQueryChanged updates gameSearchQuery in state`() = runTest {
        viewModel.onEvent(AddPlayEvent.GameSearchEvent.GameSearchQueryChanged("Catan"))
        val state = awaitUiStateAfterDebounce<AddPlayUiState.GameSearch>(
            viewModel
        ) { (it as? AddPlayUiState.GameSearch)?.gameSearchQuery == "Catan" }
        assertEquals("Catan", state.gameSearchQuery)
    }

    @Test
    fun `GameSelected sets gameId and gameName and transitions to GameSelected state`() = runTest {
        viewModel.onEvent(AddPlayEvent.GameSearchEvent.GameSearchQueryChanged("Wing"))
        viewModel.onEvent(AddPlayEvent.GameSearchEvent.GameSelected(gameId = 7L, gameName = "Wingspan"))
        val state = awaitUiStateAfterDebounce<AddPlayUiState.GameSelected>(
            viewModel
        ) { it is AddPlayUiState.GameSelected }
        assertEquals(7L, state.gameId)
        assertEquals("Wingspan", state.gameName)
    }

    // endregion

    // region Reactive location flows

    @Test
    fun `location suggestions update after debounce when location changes`() = runTest {
        // Seed a play at "Home" so observeLocations("Home") returns ["Home"]
        fakePlaysRepository.setPlays(
            listOf(PlayTestFactory.createPlay(localPlayId = 1L, gameName = "Catan", location = "Home"))
        )

        viewModel.onEvent(AddPlayEvent.GameSearchEvent.GameSelected(1L, "Catan"))
        viewModel.onEvent(AddPlayEvent.MetadataEvent.LocationChanged("Home"))

        val state = awaitUiStateAfterDebounce<AddPlayUiState.GameSelected>(viewModel) { s ->
            (s as? AddPlayUiState.GameSelected)?.location?.let {
                it.value == "Home" && it.suggestions == listOf("Home")
            } == true
        }

        assertEquals("Home", state.location.value)
        assertEquals(listOf("Home"), state.location.suggestions)
    }

    // endregion

    // region ActionEvent — CancelClicked

    @Test
    fun `CancelClicked emits NavigateBack ui effect`() = runTest {
        viewModel.uiEffect.test {
            viewModel.onEvent(AddPlayEvent.ActionEvent.CancelClicked)
            advanceUntilIdle()

            assertEquals(AddPlayUiEffect.NavigateBack, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // endregion

    // region ActionEvent — SaveClicked

    @Test
    fun `SaveClicked with no game selected does not trigger any effect`() = runTest {
        // In GameSearch state, SaveClicked produces no effects (neither save nor error)
        val effects = mutableListOf<AddPlayUiEffect>()
        val job = launch { viewModel.uiEffect.collect { effects.add(it) } }

        viewModel.onEvent(AddPlayEvent.ActionEvent.SaveClicked)
        advanceUntilIdle()

        job.cancel()
        assertTrue("Expected no effects but got: $effects", effects.isEmpty())
    }

    @Test
    fun `SaveClicked with saveable state emits NavigateBack after successful save`() = runTest {
        // Make the VM saveable by selecting a game
        viewModel.onEvent(AddPlayEvent.GameSearchEvent.GameSelected(1L, "Catan"))
        advanceUntilIdle()

        viewModel.uiEffect.test {
            viewModel.onEvent(AddPlayEvent.ActionEvent.SaveClicked)
            advanceUntilIdle()

            assertEquals(AddPlayUiEffect.NavigateBack, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `SaveClicked sets isSaving true while saving then false on failure`() = runTest {
        val gate = CompletableDeferred<Unit>()
        fakePlaysRepository.beforeCreatePlay = { gate.await() }
        fakePlaysRepository.createPlayException = RuntimeException("Save failed")

        viewModel.onEvent(AddPlayEvent.GameSearchEvent.GameSelected(1L, "Catan"))
        awaitUiStateAfterDebounce<AddPlayUiState.GameSelected>(viewModel) { it is AddPlayUiState.GameSelected }

        viewModel.onEvent(AddPlayEvent.ActionEvent.SaveClicked)
        val savingState = awaitUiStateMatching<AddPlayUiState, AddPlayUiState.GameSelected>(
            viewModel.uiState
        ) { state ->
            (state as? AddPlayUiState.GameSelected)?.isSaving == true
        }
        assertTrue(savingState.isSaving)

        gate.complete(Unit)

        val failedState = awaitUiStateMatching<AddPlayUiState, AddPlayUiState.GameSelected>(
            viewModel.uiState
        ) { state ->
            (state as? AddPlayUiState.GameSelected)?.isSaving == false
        }
        assertTrue(!failedState.isSaving)
    }

    // endregion

    // region Helpers

    private fun buildViewModel(
        playsRepository: FakePlaysRepository = fakePlaysRepository
    ) = AddPlayViewModel(
        reducer = buildReducer(),
        effectProducer = AddPlayEffectProducer(),
        observeRecentLocations = ObserveRecentLocationsUseCase(playsRepository),
        searchLocations = SearchLocationsUseCase(playsRepository),
        observePlayerSuggestions = ObservePlayerSuggestionsUseCase(playsRepository),
        observeColorsUsedForGame = ObserveColorsUsedForGameUseCase(playsRepository),
        observeCollection = ObserveCollectionUseCase(fakeCollectionRepository),
        createPlay = CreatePlayUseCase(playsRepository)
    )

    private fun buildReducer() = AddPlayReducer(
        gameSearchReducer = GameSearchReducer(),
        metaReducer = MetaReducer(),
        playersReducer = PlayersReducer(
            editReducer = PlayerEditReducer(),
            listReducer = PlayerListReducer(),
            scoreReducer = PlayerScoreReducer(),
            colorReducer = PlayerColorReducer()
        )
    )

    suspend inline fun <reified T : AddPlayUiState> TestScope.awaitUiStateAfterDebounce(
        viewModel: AddPlayViewModel,
        crossinline predicate: (AddPlayUiState) -> Boolean = { true }
    ): T {
        return awaitUiStateMatching(
            viewModel.uiState,
            DebounceDurations.SearchQuery,
            predicate = predicate
        )
    }

    // endregion
}
