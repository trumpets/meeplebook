package app.meeplebook.feature.addplay

import androidx.lifecycle.viewModelScope
import app.meeplebook.core.collection.domain.DomainCollectionItem
import app.meeplebook.core.collection.domain.ObserveCollectionUseCase
import app.meeplebook.core.collection.model.CollectionDataQuery
import app.meeplebook.core.plays.domain.CreatePlayUseCase
import app.meeplebook.core.plays.domain.ObserveColorsUsedForGameUseCase
import app.meeplebook.core.plays.domain.ObservePlayerSuggestionsUseCase
import app.meeplebook.core.plays.domain.ObserveRecentLocationsUseCase
import app.meeplebook.core.plays.domain.PlayerIdentity
import app.meeplebook.core.plays.domain.SearchLocationsUseCase
import app.meeplebook.core.plays.domain.SearchPlayersByNameUseCase
import app.meeplebook.core.plays.domain.SearchPlayersByUsernameUseCase
import app.meeplebook.core.plays.model.PlayerColor
import app.meeplebook.core.result.fold
import app.meeplebook.core.ui.architecture.ReducerViewModel
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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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
 * Orchestrates the [AddPlayReducer] + [AddPlayEffectProducer] pipeline and merges reactive
 * external data (search results, suggestions, color history) into the exposed [uiState].
 *
 * ## Entry paths
 * - **Path 1 — no game pre-selected**: The screen starts with [AddPlayUiState.GameSearch.gameId]
 *   `null`. The user searches and then fires [AddPlayEvent.GameSearchEvent.GameSelected] to
 *   transition into the full play form.
 * - **Path 2 — game pre-selected**: The screen fires [AddPlayEvent.GameSearchEvent.GameSelected]
 *   immediately (via `LaunchedEffect`) so the full form is shown from the start.
 *
 * ## State architecture
 * State is split into two layers that are `combine`d into [uiState]:
 *
 * ```
 * baseState  (MutableStateFlow — mutated synchronously by the reducer on every event)
 *     +
 * externalData  (StateFlow — reactive, driven by debounced search pipes and DB observers)
 *     │
 *     ├─ searchResultsGames       ← debounced collection search (from baseState.gameSearchQuery)
 *     ├─ searchResultsLocations   ← debounced location autocomplete (from baseState.location.value)
 *     ├─ searchResultsPlayerNames ← debounced player-name search (from baseState.addEditPlayerDialog.name)
 *     ├─ searchResultsPlayerUsernames ← debounced player-username search (from ...dialog.username)
 *     ├─ recentLocationsFlow      ← DB observer (all-time recent locations)
 *     └─ usedColorsForGameFlow    ← DB observer (colors used in past plays for the selected game)
 * ```
 *
 * Query values for all debounce pipes are derived directly from `baseState` via
 * `map + distinctUntilChanged`, so no separate raw query flows are needed.
 *
 * ## Event flow
 * ```
 * onEvent(event)
 *   └─ reducer.reduce(baseState, event) → newState  (synchronous, updates baseState)
 *        └─ effectProducer.produce(newState, event) → AddPlayEffects
 *             ├─ domain effects → handled here (async, job-cancellable)
 *             │    ├─ LoadPlayerSuggestions → observePlayerSuggestions → baseState update
 *             │    └─ SavePlay → createPlay → NavigateBack ui effect on success
 *             └─ ui effects → emitted on [uiEffect]
 * ```
 */
@HiltViewModel
class AddPlayViewModel @Inject constructor(
    reducer: AddPlayReducer,
    effectProducer: AddPlayEffectProducer,
    observeRecentLocations: ObserveRecentLocationsUseCase,
    private val searchLocations: SearchLocationsUseCase,
    private val observePlayerSuggestions: ObservePlayerSuggestionsUseCase,
    private val observeColorsUsedForGame: ObserveColorsUsedForGameUseCase,
    private val observeCollection: ObserveCollectionUseCase,
    private val createPlay: CreatePlayUseCase,
    private val searchPlayersByName: SearchPlayersByNameUseCase,
    private val searchPlayersByUsername: SearchPlayersByUsernameUseCase,
) : ReducerViewModel<AddPlayUiState, AddPlayEvent, AddPlayEffect, AddPlayUiEffect>(
    initialState = AddPlayUiState.GameSearch(),
    reducer = reducer,
    effectProducer = effectProducer
) {

    private val gameSearchQueryFlow =
        baseState
            .map {
                it.asGameSearch { gameSearchQuery } ?: ""
            }
            .distinctUntilChanged()
    private val locationQueryFlow =
        baseState
            .map {
                it.asGameSelected { location.value } ?: ""
            }
            .distinctUntilChanged()
    private val addEditNameQueryFlow =
        baseState
            .map {
                it.asGameSelected { addEditPlayerDialog?.name } ?: ""
            }
            .distinctUntilChanged()
    private val addEditUsernameQueryFlow =
        baseState
            .map {
                it.asGameSelected { addEditPlayerDialog?.username } ?: ""
            }
            .distinctUntilChanged()

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    private val searchResultsLocations =
        searchableFlow(
            queryFlow = locationQueryFlow,
            debounceMillis = DebounceDurations.SearchQuery.inWholeMilliseconds
        ) { query ->
            if (query.isBlank()) flowOf(emptyList())
            else searchLocations(query)
        }

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    private val searchResultsGames =
        searchableFlow(
            queryFlow = gameSearchQueryFlow,
            debounceMillis = DebounceDurations.SearchQuery.inWholeMilliseconds
        ) { query ->
            observeCollection(CollectionDataQuery(searchQuery = query))
        }

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    private val searchResultsPlayerNames =
        searchableFlow(
            queryFlow = addEditNameQueryFlow,
            debounceMillis = DebounceDurations.SearchQuery.inWholeMilliseconds
        ) { query ->
            if (query.isBlank()) flowOf(emptyList())
            else searchPlayersByName(query)
        }

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    private val searchResultsPlayerUsernames =
        searchableFlow(
            queryFlow = addEditUsernameQueryFlow,
            debounceMillis = DebounceDurations.SearchQuery.inWholeMilliseconds
        ) { query ->
            if (query.isBlank()) flowOf(emptyList())
            else searchPlayersByUsername(query)
        }

    private data class SearchResults(
        val games: List<DomainCollectionItem>,
        val locations: List<String>,
        val names: List<PlayerIdentity>,
        val usernames: List<PlayerIdentity>
    )

    private val searchResultsFlow =
        combine(searchResultsGames, searchResultsLocations, searchResultsPlayerNames, searchResultsPlayerUsernames) { games, locations, names, usernames ->
            SearchResults(games, locations, names, usernames)
        }


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
        val search: SearchResults,
        val recentLocations: List<String>,
        val usedColors: List<PlayerColor>
    )

    private val externalData =
        combine(
            searchResultsFlow,
            recentLocationsFlow,
            usedColorsForGameFlow
        ) { search, recentLocations, usedColors ->

            ExternalData(
                search = search,
                recentLocations = recentLocations,
                usedColors = usedColors
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            ExternalData(
                search = SearchResults(games = emptyList(), locations = emptyList(), names = emptyList(), usernames = emptyList()),
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
                    gameSearchResults = data.search.games.map { it.toSearchResultGameItem() },
                )

                is AddPlayUiState.GameSelected -> state.copy(
                    location = state.location.copy(
                        suggestions = data.search.locations,
                        recentLocations = data.recentLocations
                    ),
                    players = state.players.copy(
                        colorsHistory = data.usedColors
                    ),
                    addEditPlayerDialog = state.addEditPlayerDialog?.copy(
                        nameSuggestions = data.search.names,
                        usernameSuggestions = data.search.usernames
                    )
                )
            }
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            AddPlayUiState.GameSearch()
        )

    private var suggestionsJob: Job? = null
    private var saveJob: Job? = null

    fun onEvent(event: AddPlayEvent) {
        dispatchEvent(event)
    }

    override fun handleDomainEffect(effect: AddPlayEffect) {
        when (effect) {
            is AddPlayEffect.LoadPlayerSuggestions -> loadPlayerSuggestions(effect)
            is AddPlayEffect.SavePlay -> savePlay(effect)
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
                    emitUiEffect(AddPlayUiEffect.NavigateBack)
                },
                onFailure = {
                    baseState.value = baseState.value.updateGameSelected { copy(isSaving = false) }
                }
            )
        }
    }
}
