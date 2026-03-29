package app.meeplebook.feature.addplay

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.meeplebook.core.collection.model.CollectionDataQuery
import app.meeplebook.core.collection.domain.ObserveCollectionUseCase
import app.meeplebook.core.plays.domain.CreatePlayUseCase
import app.meeplebook.core.plays.domain.ObservePlayerSuggestionsUseCase
import app.meeplebook.core.plays.domain.ObserveRecentLocationsUseCase
import app.meeplebook.core.plays.domain.SearchLocationsUseCase
import app.meeplebook.core.util.DebounceDurations
import app.meeplebook.feature.addplay.effect.AddPlayEffect
import app.meeplebook.feature.addplay.effect.AddPlayEffectProducer
import app.meeplebook.feature.addplay.effect.AddPlayUiEffect
import app.meeplebook.feature.addplay.reducer.AddPlayReducer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Add Play screen.
 *
 * Orchestrates the [AddPlayReducer] + [AddPlayEffectProducer] pipeline and manages
 * the reactive data flows (location suggestions, recent locations, game search).
 *
 * ## Entry paths
 * - **Path 1 — no game pre-selected**: The screen starts with [AddPlayUiState.gameId] `null`.
 *   The screen drives game search via [AddPlayEvent.GameSearchEvent] events and then fires
 *   [AddPlayEvent.GameSearchEvent.GameSelected] (typically from a `LaunchedEffect`) once a
 *   game is resolved.
 * - **Path 2 — game pre-selected**: The screen fires [AddPlayEvent.GameSearchEvent.GameSelected]
 *   immediately (via `LaunchedEffect`) so the full form is shown from the start.
 *
 * ## Event flow
 * ```
 * onEvent(event)
 *   ├─ update rawGameSearchQuery / rawLocationQuery  (drive reactive debounce pipes)
 *   └─ reducer.reduce(state, event) → newState
 *        └─ effectProducer.produce(newState, event) → effects
 *             ├─ domain effects → handled here (async, job-cancellable)
 *             └─ ui effects    → emitted on [uiEffect]
 * ```
 */
@HiltViewModel
class AddPlayViewModel @Inject constructor(
    private val reducer: AddPlayReducer,
    private val effectProducer: AddPlayEffectProducer,
    private val observeRecentLocations: ObserveRecentLocationsUseCase,
    private val searchLocations: SearchLocationsUseCase,
    private val observePlayerSuggestions: ObservePlayerSuggestionsUseCase,
    private val observeCollection: ObserveCollectionUseCase,
    private val createPlay: CreatePlayUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddPlayUiState.initial())
    val uiState: StateFlow<AddPlayUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<AddPlayUiEffect>()
    val uiEffect: SharedFlow<AddPlayUiEffect> = _uiEffect.asSharedFlow()

    // Raw query inputs — updated in onEvent() to drive debounced reactive flows
    private val rawGameSearchQuery = MutableStateFlow("")
    private val rawLocationQuery = MutableStateFlow("")

    private var suggestionsJob: Job? = null
    private var saveJob: Job? = null

    init {
        observeRecentLocationsFlow()
        observeLocationSuggestionsFlow()
        observeGameSearchResultsFlow()
    }

    // region Public API

    fun onEvent(event: AddPlayEvent) {
        // Feed raw query flows so debounce pipes run alongside the reducer
        when (event) {
            is AddPlayEvent.GameSearchEvent.GameSearchQueryChanged ->
                rawGameSearchQuery.value = event.query

            is AddPlayEvent.MetadataEvent.LocationChanged ->
                rawLocationQuery.value = event.value

            else -> Unit
        }

        val oldState = _uiState.value
        val newState = reducer.reduce(oldState, event)
        _uiState.value = newState

        val effects = effectProducer.produce(newState, event)
        handleDomainEffects(effects.effects)
        handleUiEffects(effects.uiEffects)
    }

    // endregion

    // region Reactive flows

    private fun observeRecentLocationsFlow() {
        observeRecentLocations()
            .onEach { recent ->
                _uiState.value = _uiState.value.copy(
                    location = _uiState.value.location.copy(recentLocations = recent)
                )
            }
            .launchIn(viewModelScope)
    }

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    private fun observeLocationSuggestionsFlow() {
        rawLocationQuery
            .debounce(DebounceDurations.SearchQuery.inWholeMilliseconds)
            .distinctUntilChanged()
            .flatMapLatest { query -> searchLocations(query) }
            .onEach { suggestions ->
                _uiState.value = _uiState.value.copy(
                    location = _uiState.value.location.copy(suggestions = suggestions)
                )
            }
            .launchIn(viewModelScope)
    }

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    private fun observeGameSearchResultsFlow() {
        rawGameSearchQuery
            .debounce(DebounceDurations.SearchQuery.inWholeMilliseconds)
            .distinctUntilChanged()
            .flatMapLatest { query ->
                if (query.isBlank()) flowOf(emptyList())
                else observeCollection(CollectionDataQuery(searchQuery = query))
            }
            .onEach { results ->
                _uiState.value = _uiState.value.copy(gameSearchResults = results)
            }
            .launchIn(viewModelScope)
    }

    // endregion

    // region Effect handling

    private fun handleDomainEffects(effects: List<AddPlayEffect>) {
        effects.forEach { effect ->
            when (effect) {
                is AddPlayEffect.LoadPlayerSuggestions -> loadPlayerSuggestions(effect)
                is AddPlayEffect.SavePlay -> savePlay(effect)
            }
        }
    }

    private fun handleUiEffects(uiEffects: List<AddPlayUiEffect>) {
        uiEffects.forEach { effect ->
            viewModelScope.launch { _uiEffect.emit(effect) }
        }
    }

    private fun loadPlayerSuggestions(effect: AddPlayEffect.LoadPlayerSuggestions) {
        suggestionsJob?.cancel()
        suggestionsJob = viewModelScope.launch {
            val suggestions = observePlayerSuggestions(effect.location)
                .first()
                .map { identity -> PlayerSuggestion(playerIdentity = identity, playCount = 0) }
            _uiState.value = _uiState.value.copy(playersByLocation = suggestions)
        }
    }

    private fun savePlay(effect: AddPlayEffect.SavePlay) {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            runCatching { createPlay(effect.play) }
                .onSuccess { _uiEffect.emit(AddPlayUiEffect.NavigateBack) }
                .onFailure { _uiState.value = _uiState.value.copy(isSaving = false) }
        }
    }

    // endregion
}
