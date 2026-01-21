package app.meeplebook.feature.collection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.meeplebook.R
import app.meeplebook.core.collection.domain.ObserveCollectionSummaryUseCase
import app.meeplebook.core.collection.model.CollectionDataQuery
import app.meeplebook.core.collection.model.CollectionSort
import app.meeplebook.core.collection.model.QuickFilter
import app.meeplebook.core.result.fold
import app.meeplebook.core.sync.domain.SyncCollectionUseCase
import app.meeplebook.core.ui.StringProvider
import app.meeplebook.core.util.DebounceDurations
import app.meeplebook.feature.collection.domain.ObserveCollectionDomainSectionsUseCase
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
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CollectionViewModel @Inject constructor(
    private val observeCollectionDomainSections: ObserveCollectionDomainSectionsUseCase,
    private val observeCollectionSummary: ObserveCollectionSummaryUseCase,
    private val syncCollection: SyncCollectionUseCase,
    private val stringProvider: StringProvider
) : ViewModel() {

    private val rawSearchQuery = MutableStateFlow("")
    @OptIn(FlowPreview::class)
    private val debouncedSearchQuery: StateFlow<String> =
        rawSearchQuery
            .map { it.trim() }
            .debounce(DebounceDurations.SearchQuery.inWholeMilliseconds)
            .distinctUntilChanged()
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                ""
            )

    private val quickFilter = MutableStateFlow(QuickFilter.ALL)
    private val sort = MutableStateFlow(CollectionSort.ALPHABETICAL)
    private val viewMode = MutableStateFlow(CollectionViewMode.LIST)
    private val isRefreshing = MutableStateFlow(false)

    private val collectionDataQuery: StateFlow<CollectionDataQuery> =
        combine(
            debouncedSearchQuery,
            quickFilter,
            sort
        ) { query, filter, sort ->
            CollectionDataQuery(
                searchQuery = query,
                quickFilter = filter,
                sort = sort
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            CollectionDataQuery(
                searchQuery = "",
                quickFilter = QuickFilter.ALL,
                sort = CollectionSort.ALPHABETICAL
            )
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    private val contentState: StateFlow<CollectionUiState> =
        combine(
            collectionDataQuery.flatMapLatest {
                observeCollectionDomainSections(it)
            },
            observeCollectionSummary(),
            isRefreshing
        ) { domainSections, summary, refreshing ->

                val uiSections = domainSections.map { it.toCollectionSection(stringProvider) }

                if (uiSections.isEmpty()) {
                    CollectionUiState.Empty(
                        reason = emptyReason(
                            searchQuery = rawSearchQuery.value,
                            quickFilter = quickFilter.value
                        ),

                        searchQuery = rawSearchQuery.value,
                        activeQuickFilter = quickFilter.value,
                        totalGameCount = summary.totalGames,
                        unplayedGameCount = summary.unplayedGames,
                        isRefreshing = refreshing
                    )
                } else {
                    val sectionIndices = buildSectionIndices(uiSections)

                    CollectionUiState.Content(
                        viewMode = viewMode.value,
                        sort = sort.value,
                        availableSortOptions = CollectionSort.entries,
                        sections = uiSections,
                        sectionIndices = sectionIndices,
                        showAlphabetJump = uiSections.size > 1,
                        isSortSheetVisible = false,

                        searchQuery = rawSearchQuery.value,
                        activeQuickFilter = quickFilter.value,
                        totalGameCount = summary.totalGames,
                        unplayedGameCount = summary.unplayedGames,
                        isRefreshing = refreshing
                    )
                }
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                CollectionUiState.Loading
            )

    val uiState: StateFlow<CollectionUiState> =
        combine(
            contentState,
            viewMode,
            rawSearchQuery,
            quickFilter
        ) { state, viewMode, rawQuery, filter ->
            when (state) {
                is CollectionUiState.Content ->
                    state.copy(
                        viewMode = viewMode,
                        searchQuery = rawQuery,
                        activeQuickFilter = filter
                    )

                is CollectionUiState.Empty ->
                    state.copy(
                        searchQuery = rawQuery,
                        activeQuickFilter = filter
                    )

                else -> state
            }
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            CollectionUiState.Loading
        )

    private val _uiEffect = MutableSharedFlow<CollectionUiEffects>()
    val uiEffect = _uiEffect.asSharedFlow()

    private var refreshJob: Job? = null

    fun onEvent(event: CollectionEvent) {
        when (event) {
            is CollectionEvent.SearchChanged -> {
                rawSearchQuery.value = event.query
            }

            is CollectionEvent.QuickFilterSelected -> {
                quickFilter.value = event.filter
            }

            is CollectionEvent.SortSelected -> {
                sort.value = event.sort
            }

            is CollectionEvent.ViewModeSelected -> {
                viewMode.value = event.viewMode
            }

            is CollectionEvent.JumpToLetter -> {
                emitEffect(CollectionUiEffects.ScrollToLetter(event.letter))
            }

            is CollectionEvent.GameClicked -> {
                emitEffect(CollectionUiEffects.NavigateToGame(event.gameId))
            }

            CollectionEvent.OpenSortSheet -> {
                emitEffect(CollectionUiEffects.OpenSortSheet)
            }

            CollectionEvent.DismissSortSheet -> {
                emitEffect(CollectionUiEffects.DismissSortSheet)
            }

            is CollectionEvent.Refresh -> {
                refreshJob?.cancel()
                refreshJob = viewModelScope.launch {
                    isRefreshing.value = true
                    try {
                        syncCollection().fold(
                            onSuccess = {
                                // Sync successful, data will update automatically via flows
                            },
                            onFailure = { _ ->
                                emitEffect(
                                    CollectionUiEffects.ShowSnackbar(
                                        stringProvider.get(R.string.sync_collections_failed_error)
                                    )
                                )
                            }
                        )
                    } finally {
                        isRefreshing.value = false
                    }
                }
            }

            is CollectionEvent.LogPlayClicked -> {
                // TODO: Implement navigation or handling for logging a play.
            }
        }
    }

    private fun emitEffect(effect: CollectionUiEffects) {
        viewModelScope.launch {
            _uiEffect.emit(effect)
        }
    }

    private fun buildSectionIndices(
        sections: List<CollectionSection>
    ): Map<Char, Int> {
        var index = 0
        val result = LinkedHashMap<Char, Int>(sections.size)

        sections.forEach { section ->
            result[section.key] = index
            index += 1 // sticky header
            index += section.games.size
        }

        return result
    }

    private fun emptyReason(
        searchQuery: String,
        quickFilter: QuickFilter
    ): EmptyReason =
        when {
            searchQuery.isNotBlank() ->
                EmptyReason.NO_SEARCH_RESULTS

            quickFilter != QuickFilter.ALL ->
                EmptyReason.NO_FILTER_RESULTS

            else ->
                EmptyReason.NO_GAMES
        }
}