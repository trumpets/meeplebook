package app.meeplebook.feature.addplay

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.meeplebook.core.collection.domain.DomainCollectionItem
import app.meeplebook.core.collection.domain.ObserveCollectionUseCase
import app.meeplebook.core.collection.model.CollectionDataQuery
import app.meeplebook.core.plays.domain.CreatePlayUseCase
import app.meeplebook.core.plays.domain.ObserveColorsUsedForGameUseCase
import app.meeplebook.core.plays.domain.ObservePlayerSuggestionsUseCase
import app.meeplebook.core.plays.domain.ObserveRecentLocationsUseCase
import app.meeplebook.core.plays.domain.SearchLocationsUseCase
import app.meeplebook.core.plays.model.PlayerColor
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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
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
    private val observeColorsUsedForGame: ObserveColorsUsedForGameUseCase,
    private val observeCollection: ObserveCollectionUseCase,
    private val createPlay: CreatePlayUseCase
) : ViewModel() {

    private val rawGameSearchQuery = MutableStateFlow("")
    private val rawLocationQuery = MutableStateFlow("")

    private data class Queries(
        val gameQuery: String,
        val locationQuery: String
    )

    private val queriesFlow =
        combine(rawGameSearchQuery, rawLocationQuery) { game, location ->
            Queries(game, location)
        }

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

    private data class SearchResults(
        val games: List<DomainCollectionItem>,
        val locations: List<String>
    )

    private val searchResultsFlow =
        combine(searchResultsGames, searchResultsLocations) { games, locations ->
            SearchResults(games, locations)
        }

    private val baseState = MutableStateFlow<AddPlayUiState>(AddPlayUiState.GameSearch())

    // Observe distinct player colors for the currently selected game.
    private val selectedGameIdFlow =
        baseState
            .map { (it as? AddPlayUiState.GameSelected)?.gameId }
            .distinctUntilChanged()
    @OptIn(ExperimentalCoroutinesApi::class)
    private val usedColorsForGameFlow = selectedGameIdFlow
        .flatMapLatest { gameId ->
            if (gameId != null) observeColorsUsedForGame(gameId)
            else flowOf(emptyList())
        }

    val recentLocationsFlow = observeRecentLocations()

    private data class ExternalData(
        val queries: Queries,
        val search: SearchResults,
        val recentLocations: List<String>,
        val usedColors: List<PlayerColor>
    )

    private val externalData =
        combine(
            queriesFlow,
            searchResultsFlow,
            recentLocationsFlow,
            usedColorsForGameFlow
        ) { queries, search, recentLocations, usedColors ->

            ExternalData(
                queries = queries,
                search = search,
                recentLocations = recentLocations,
                usedColors = usedColors
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            ExternalData(
                queries = Queries(gameQuery = "", locationQuery = ""),
                search = SearchResults(games = emptyList(), locations = emptyList()),
                recentLocations = emptyList(),
                usedColors = emptyList()
            )
        )

    val uiState: StateFlow<AddPlayUiState> =
        combine(
            baseState,
            externalData
        ) { state, data ->

            when (state) {
                is AddPlayUiState.GameSearch -> state.copy(
                    gameSearchQuery = data.queries.gameQuery,
                    gameSearchResults = data.search.games.map { it.toSearchResultGameItem() },
                )

                is AddPlayUiState.GameSelected -> state.copy(
                    location = state.location.copy(
                        value = data.queries.locationQuery,
                        suggestions = data.search.locations,
                        recentLocations = data.recentLocations
                    ),
                    players = state.players.copy(
                        colorsHistory = data.usedColors
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

        val oldState = baseState.value
        val newState = reducer.reduce(oldState, event)
        baseState.value = newState

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

            baseState.value = baseState.value.updateGameSelected { copy(playersByLocation = suggestions) }
        }
    }

    private fun savePlay(effect: AddPlayEffect.SavePlay) {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            baseState.value = baseState.value.updateGameSelected { copy(isSaving = true) }
            createPlay(effect.play).fold(
                onSuccess = {
                    baseState.value = baseState.value.updateGameSelected { copy(isSaving = false) }
                    _uiEffect.emit(AddPlayUiEffect.NavigateBack)
                },
                onFailure = {
                    baseState.value = baseState.value.updateGameSelected { copy(isSaving = false) }
                }
            )
        }
    }

    // endregion
}
