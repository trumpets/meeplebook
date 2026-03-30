package app.meeplebook.feature.addplay

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.meeplebook.core.collection.domain.ObserveCollectionUseCase
import app.meeplebook.core.collection.model.CollectionDataQuery
import app.meeplebook.core.plays.domain.CreatePlayUseCase
import app.meeplebook.core.plays.domain.ObservePlayerSuggestionsUseCase
import app.meeplebook.core.plays.domain.ObserveRecentLocationsUseCase
import app.meeplebook.core.plays.domain.SearchLocationsUseCase
import app.meeplebook.core.result.fold
import app.meeplebook.core.ui.flow.searchableFlow
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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Add Play screen.
 *
 * Orchestrates the [AddPlayReducer] + [AddPlayEffectProducer] pipeline and manages
 * the reactive data flows (location suggestions, recent locations, game search).
 *
 * ## Entry paths
 * - **Path 1 — no game pre-selected**: The screen starts with [AddPlayUiState.GameSearch.gameId] `null`.
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

    private val rawGameSearchQuery = MutableStateFlow("")
    private val rawLocationQuery = MutableStateFlow("")

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    private val searchResultsLocations =
        searchableFlow(
            queryFlow = rawLocationQuery,
            debounceMillis = DebounceDurations.SearchQuery.inWholeMilliseconds
        ) { query ->
            searchLocations(query)
        }

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    private val searchResultsGames =
        searchableFlow(
            queryFlow = rawGameSearchQuery,
            debounceMillis = DebounceDurations.SearchQuery.inWholeMilliseconds
        ) { query ->
            observeCollection(CollectionDataQuery(searchQuery = query))
        }

    private val _uiState = MutableStateFlow<AddPlayUiState>(AddPlayUiState.GameSearch())

    val combinedUiState: StateFlow<AddPlayUiState> =
        combine(
            _uiState,
            rawGameSearchQuery,
            rawLocationQuery,
            searchResultsLocations,
            searchResultsGames
        ) { state, rawGameQuery, rawLocationQuery, locationSuggestions, gameSuggestions ->

            when (state) {
                is AddPlayUiState.GameSearch -> state.copy(
                    gameSearchQuery = rawGameQuery,
                    gameSearchResults = gameSuggestions.map { it.toSearchResultGameItem() },
                )

                is AddPlayUiState.GameSelected -> state.copy(
                    location = state.location.copy(
                        value = rawLocationQuery,
                        suggestions = locationSuggestions
                    )
                )
            }
        }.combine(
            observeRecentLocations()
        ) { state, recentLocations ->

            when (state) {
                is AddPlayUiState.GameSearch -> state
                is AddPlayUiState.GameSelected -> state.copy(
                    location = state.location.copy(
                        recentLocations = recentLocations
                    )
                )
            }
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            AddPlayUiState.GameSearch()
        )

    private val _uiEffect = MutableSharedFlow<AddPlayUiEffect>()
    val uiEffect = _uiEffect.asSharedFlow()

    private var suggestionsJob: Job? = null
    private var saveJob: Job? = null

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
                .map { identity -> PlayerSuggestion(playerIdentity = identity) }

            _uiState.value = _uiState.value.updateGameSelected { copy(playersByLocation = suggestions) }
        }
    }

    private fun savePlay(effect: AddPlayEffect.SavePlay) {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            _uiState.value = _uiState.value.updateGameSelected { copy(isSaving = true) }
            createPlay(effect.play).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.updateGameSelected { copy(isSaving = false) }
                    _uiEffect.emit(AddPlayUiEffect.NavigateBack)
                },
                onFailure = {
                    _uiState.value = _uiState.value.updateGameSelected { copy(isSaving = false) }
                }
            )
        }
    }

    // endregion
}
