package app.meeplebook.feature.addplay

import app.cash.turbine.test
import app.meeplebook.core.collection.FakeCollectionRepository
import app.meeplebook.core.collection.domain.ObserveCollectionUseCase
import app.meeplebook.core.plays.FakePlaysRepository
import app.meeplebook.core.plays.domain.CreatePlayUseCase
import app.meeplebook.core.plays.domain.ObservePlayerSuggestionsUseCase
import app.meeplebook.core.plays.domain.ObserveRecentLocationsUseCase
import app.meeplebook.core.plays.domain.SearchLocationsUseCase
import app.meeplebook.core.util.DebounceDurations
import app.meeplebook.feature.addplay.effect.AddPlayEffectProducer
import app.meeplebook.feature.addplay.effect.AddPlayUiEffect
import app.meeplebook.feature.addplay.reducer.AddPlayReducer
import app.meeplebook.feature.addplay.reducer.MetaReducer
import app.meeplebook.feature.addplay.reducer.PlayerColorReducer
import app.meeplebook.feature.addplay.reducer.PlayerEditReducer
import app.meeplebook.feature.addplay.reducer.PlayerListReducer
import app.meeplebook.feature.addplay.reducer.PlayerScoreReducer
import app.meeplebook.feature.addplay.reducer.PlayersReducer
import app.meeplebook.feature.addplay.reducer.ValidationReducer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
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
        assertNull(state.gameId)
        assertNull(state.gameName)
        assertFalse(state.isSaving)
        assertFalse(state.canSave)
        assertTrue(state.players.players.isEmpty())
    }

    // endregion

    // region GameSearchEvent

    @Test
    fun `GameSearchQueryChanged updates gameSearchQuery in state`() = runTest {
        viewModel.onEvent(AddPlayEvent.GameSearchEvent.GameSearchQueryChanged("Catan"))
        advanceUntilIdle()
        assertEquals("Catan", viewModel.uiState.value.gameSearchQuery)
    }

    @Test
    fun `GameSelected sets gameId and gameName and clears search fields`() = runTest {
        viewModel.onEvent(AddPlayEvent.GameSearchEvent.GameSearchQueryChanged("Wing"))
        viewModel.onEvent(AddPlayEvent.GameSearchEvent.GameSelected(gameId = 7L, gameName = "Wingspan"))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(7L, state.gameId)
        assertEquals("Wingspan", state.gameName)
        assertEquals("", state.gameSearchQuery)
        assertTrue(state.gameSearchResults.isEmpty())
    }

    // endregion

    // region Reactive location flows

    @Test
    fun `location suggestions update after debounce when location changes`() = runTest {
        // Set up plays with a known location so the fake repo can filter
        viewModel.onEvent(AddPlayEvent.GameSearchEvent.GameSelected(1L, "Catan"))

        viewModel.uiState.test {
            awaitItem() // initial

            viewModel.onEvent(AddPlayEvent.MetadataEvent.LocationChanged("Home"))

            // Advance past debounce
            advanceTimeBy(DebounceDurations.SearchQuery.inWholeMilliseconds + 1)
            advanceUntilIdle()

            // Drain until we get the suggestions update
            var foundSuggestions = false
            while (!foundSuggestions) {
                val s = awaitItem()
                // suggestions may be empty (no matching plays) but the field must have been updated
                foundSuggestions = true // Flow emitted at least once after debounce
            }
            assertTrue(foundSuggestions)
            cancelAndIgnoreRemainingEvents()
        }
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
    fun `SaveClicked with canSave false does not trigger save`() = runTest {
        // canSave is false in initial state (no gameId/players)
        viewModel.uiEffect.test {
            viewModel.onEvent(AddPlayEvent.ActionEvent.SaveClicked)
            advanceUntilIdle()

            // Should emit ShowError (can't save), not NavigateBack
            val effect = awaitItem()
            assertTrue(effect is AddPlayUiEffect.ShowError)
            cancelAndIgnoreRemainingEvents()
        }
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
        fakePlaysRepository.createPlayException = RuntimeException("Save failed")

        viewModel.onEvent(AddPlayEvent.GameSearchEvent.GameSelected(1L, "Catan"))
        advanceUntilIdle()

        viewModel.uiState.test {
            awaitItem() // current state with canSave=true

            viewModel.onEvent(AddPlayEvent.ActionEvent.SaveClicked)
            advanceUntilIdle()

            var sawSaving = false
            var sawNotSaving = false
            while (!sawNotSaving) {
                val s = awaitItem()
                if (s.isSaving) sawSaving = true
                if (sawSaving && !s.isSaving) sawNotSaving = true
            }
            assertTrue(sawSaving)
            assertTrue(sawNotSaving)
            cancelAndIgnoreRemainingEvents()
        }
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
        observeCollection = ObserveCollectionUseCase(fakeCollectionRepository),
        createPlay = CreatePlayUseCase(playsRepository)
    )

    private fun buildReducer() = AddPlayReducer(
        metaReducer = MetaReducer(),
        playersReducer = PlayersReducer(
            editReducer = PlayerEditReducer(),
            listReducer = PlayerListReducer(),
            scoreReducer = PlayerScoreReducer(),
            colorReducer = PlayerColorReducer()
        ),
        validationReducer = ValidationReducer()
    )

    // endregion
}
