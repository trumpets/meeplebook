package app.meeplebook.feature.plays

import androidx.lifecycle.viewModelScope
import app.meeplebook.R
import app.meeplebook.core.result.fold
import app.meeplebook.core.sync.domain.SyncPlaysUseCase
import app.meeplebook.core.ui.architecture.ReducerViewModel
import app.meeplebook.core.ui.flow.searchableFlow
import app.meeplebook.core.ui.uiTextRes
import app.meeplebook.core.util.DebounceDurations
import app.meeplebook.feature.plays.domain.ObservePlaysScreenDataUseCase
import app.meeplebook.feature.plays.effect.PlaysEffect
import app.meeplebook.feature.plays.effect.PlaysEffectProducer
import app.meeplebook.feature.plays.effect.PlaysUiEffect
import app.meeplebook.feature.plays.reducer.PlaysReducer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Plays screen.
 *
 * The Plays feature uses the same high-level architecture as AddPlay, but with a smaller internal
 * state surface:
 *
 * - reducer-owned [PlaysBaseState] for synchronous UI inputs
 * - debounced query flow derived from [PlaysBaseState.searchQuery]
 * - domain observer output from [ObservePlaysScreenDataUseCase]
 * - derived [PlaysUiState] for rendering
 * - [PlaysUiEffect] for one-shot UI work
 *
 * The reducer mutates only [baseState]. Display-state derivation happens by combining `baseState`
 * with `searchResults`, and refresh/navigation work is triggered through
 * [PlaysEffectProducer]-produced effects.
 */
@HiltViewModel
class PlaysViewModel @Inject constructor(
    reducer: PlaysReducer,
    effectProducer: PlaysEffectProducer,
    private val observePlaysScreenData: ObservePlaysScreenDataUseCase,
    private val syncPlays: SyncPlaysUseCase
) : ReducerViewModel<PlaysBaseState, PlaysEvent, PlaysEffect, PlaysUiEffect>(
    initialState = PlaysBaseState(),
    reducer = reducer,
    effectProducer = effectProducer
) {

    private val searchQueryFlow =
        baseState
            .map { it.searchQuery }
            .distinctUntilChanged()

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    private val searchResults =
        searchableFlow(
            queryFlow = searchQueryFlow,
            debounceMillis = DebounceDurations.SearchQuery.inWholeMilliseconds
        ) { query ->
            observePlaysScreenData(query)
        }

    val uiState: StateFlow<PlaysUiState> =
        combine(
            baseState,
            searchResults
        ) { state, screenData ->
            screenData.toUiState(state)
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            PlaysUiState.Loading
        )

    private var refreshJob: Job? = null

    /**
     * Entry point for UI events.
     *
     * Each event goes through the reducer first, then through the effect producer so that state
     * mutation and side-effect routing remain explicit and testable.
     */
    fun onEvent(event: PlaysEvent) {
        dispatchEvent(event)
    }

    override fun handleDomainEffect(effect: PlaysEffect) {
        when (effect) {
            PlaysEffect.Refresh -> refresh()
        }
    }

    private fun refresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            updateBaseState { state ->
                state.copy(isRefreshing = true)
            }
            try {
                syncPlays().fold(
                    onSuccess = {
                        // Sync successful, data will update automatically via flows.
                    },
                    onFailure = { _ ->
                        tryEmitUiEffect(PlaysUiEffect.ShowSnackbar(uiTextRes(R.string.sync_plays_failed_error)))
                    }
                )
            } finally {
                updateBaseState { state ->
                    state.copy(isRefreshing = false)
                }
            }
        }
    }
}
