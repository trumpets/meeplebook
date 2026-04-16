package app.meeplebook.feature.plays

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.meeplebook.R
import app.meeplebook.core.result.fold
import app.meeplebook.core.sync.domain.SyncPlaysUseCase
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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
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
    private val reducer: PlaysReducer,
    private val effectProducer: PlaysEffectProducer,
    private val observePlaysScreenData: ObservePlaysScreenDataUseCase,
    private val syncPlays: SyncPlaysUseCase
) : ViewModel() {

    private val baseState = MutableStateFlow(PlaysBaseState())

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

    private val _uiEffect = MutableSharedFlow<PlaysUiEffect>()
    val uiEffect = _uiEffect.asSharedFlow()

    private var refreshJob: Job? = null

    /**
     * Entry point for UI events.
     *
     * Each event goes through the reducer first, then through the effect producer so that state
     * mutation and side-effect routing remain explicit and testable.
     */
    fun onEvent(event: PlaysEvent) {
        val oldState = baseState.value
        val newState = reducer.reduce(oldState, event)
        baseState.value = newState

        val effects = effectProducer.produce(newState, event)
        handleDomainEffects(effects.effects)
        handleUiEffects(effects.uiEffects)
    }

    private fun handleDomainEffects(effects: List<PlaysEffect>) {
        effects.forEach { effect ->
            when (effect) {
                PlaysEffect.Refresh -> refresh()
            }
        }
    }

    private fun handleUiEffects(uiEffects: List<PlaysUiEffect>) {
        uiEffects.forEach { effect ->
            viewModelScope.launch {
                _uiEffect.emit(effect)
            }
        }
    }

    private fun refresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            baseState.update { state ->
                state.copy(isRefreshing = true)
            }
            try {
                syncPlays().fold(
                    onSuccess = {
                        // Sync successful, data will update automatically via flows.
                    },
                    onFailure = { _ ->
                        _uiEffect.emit(PlaysUiEffect.ShowSnackbar(uiTextRes(R.string.sync_plays_failed_error)))
                    }
                )
            } finally {
                baseState.update { state ->
                    state.copy(isRefreshing = false)
                }
            }
        }
    }
}
