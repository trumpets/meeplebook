package app.meeplebook.feature.collection

import androidx.lifecycle.viewModelScope
import app.meeplebook.R
import app.meeplebook.core.collection.domain.ObserveCollectionSummaryUseCase
import app.meeplebook.core.collection.model.CollectionDataQuery
import app.meeplebook.core.collection.model.CollectionSort
import app.meeplebook.core.collection.model.QuickFilter
import app.meeplebook.core.result.fold
import app.meeplebook.core.sync.domain.SyncCollectionUseCase
import app.meeplebook.core.ui.architecture.ReducerViewModel
import app.meeplebook.core.ui.flow.searchableFlow
import app.meeplebook.core.ui.uiTextRes
import app.meeplebook.core.util.DebounceDurations
import app.meeplebook.feature.collection.domain.ObserveCollectionDomainSectionsUseCase
import app.meeplebook.feature.collection.effect.CollectionEffect
import app.meeplebook.feature.collection.effect.CollectionEffectProducer
import app.meeplebook.feature.collection.effect.CollectionUiEffect
import app.meeplebook.feature.collection.reducer.CollectionReducer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Collection screen.
 *
 * The feature uses the shared reducer/effect pipeline:
 * - [CollectionReducer] owns synchronous mutations of [CollectionBaseState]
 * - debounced query/search flows are derived from [baseState]
 * - observed domain data is fetched from [ObserveCollectionDomainSectionsUseCase] and
 *   [ObserveCollectionSummaryUseCase]
 * - [CollectionUiState] is derived by combining reducer state with those external flows
 * - [CollectionUiEffect] is used only for one-shot UI work such as navigation, scrolling, and
 *   snackbars
 *
 * Persistent UI chrome such as sort-sheet visibility remains in [CollectionBaseState], not in UI
 * effects.
 */
@HiltViewModel
class CollectionViewModel @Inject constructor(
    reducer: CollectionReducer,
    effectProducer: CollectionEffectProducer,
    observeCollectionSummary: ObserveCollectionSummaryUseCase,
    private val observeCollectionDomainSections: ObserveCollectionDomainSectionsUseCase,
    private val syncCollection: SyncCollectionUseCase
) : ReducerViewModel<CollectionBaseState, CollectionEvent, CollectionEffect, CollectionUiEffect>(
    initialState = CollectionBaseState(),
    reducer = reducer,
    effectProducer = effectProducer
) {
    /**
     * Raw search query derived from reducer state.
     *
     * This flow emits immediately so the UI can reflect typing without waiting for debounce.
     */
    private val searchQueryFlow =
        baseState
            .map { it.searchQuery }
            .distinctUntilChanged()

    private val quickFilterFlow =
        baseState
            .map { it.quickFilter }
            .distinctUntilChanged()

    private val sortFlow =
        baseState
            .map { it.sort }
            .distinctUntilChanged()

    private data class FilterAndSort(
        val quickFilter: QuickFilter,
        val sort: CollectionSort
    )

    private val filterAndSortFlow =
        combine(quickFilterFlow, sortFlow) { quickFilter, sort ->
            FilterAndSort(
                quickFilter = quickFilter,
                sort = sort
            )
        }.distinctUntilChanged()

    /**
     * Debounced collection query used to drive repository/use-case observation.
     *
     * The raw search text stays in [CollectionBaseState.searchQuery], while the actual data fetch is
     * debounced through [searchableFlow].
     */
    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    private val collectionDataQuery: StateFlow<CollectionDataQuery> =
        searchableFlow(
            queryFlow = searchQueryFlow,
            debounceMillis = DebounceDurations.SearchQuery.inWholeMilliseconds
        ) { query ->
            filterAndSortFlow.map { filterAndSort ->
                CollectionDataQuery(
                    searchQuery = query,
                    quickFilter = filterAndSort.quickFilter,
                    sort = filterAndSort.sort
                )
            }
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
    private val domainSectionsFlow =
        collectionDataQuery.flatMapLatest { query ->
            observeCollectionDomainSections(query)
        }

    val uiState: StateFlow<CollectionUiState> =
        combine(
            baseState,
            domainSectionsFlow,
            observeCollectionSummary()
        ) { state, domainSections, summary ->
            val uiSections = domainSections.map { it.toCollectionSection() }

            val common = CollectionCommonState(
                searchQuery = state.searchQuery,
                activeQuickFilter = state.quickFilter,
                totalGameCount = summary.totalGames,
                unplayedGameCount = summary.unplayedGames,
                isRefreshing = state.isRefreshing
            )

            if (uiSections.isEmpty()) {
                CollectionUiState.Empty(
                    reason = emptyReason(
                        searchQuery = state.searchQuery,
                        quickFilter = state.quickFilter
                    ),
                    common = common
                )
            } else {
                CollectionUiState.Content(
                    viewMode = state.viewMode,
                    sort = state.sort,
                    availableSortOptions = CollectionSort.entries,
                    sections = uiSections,
                    sectionIndices = buildSectionIndices(uiSections),
                    showAlphabetJump = uiSections.size > 1,
                    isSortSheetVisible = state.isSortSheetVisible,
                    common = common
                )
            }
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            CollectionUiState.Loading
        )

    private var refreshJob: Job? = null

    fun onEvent(event: CollectionEvent) {
        dispatchEvent(event)
    }

    override fun handleDomainEffect(effect: CollectionEffect) {
        when (effect) {
            CollectionEffect.Refresh -> refresh()
        }
    }

    /**
     * Executes a collection sync and mirrors its progress into reducer state.
     *
     * Successful syncs update the screen indirectly via the observed flows. Failures emit a
     * one-shot snackbar effect.
     */
    private fun refresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            updateBaseState { state ->
                state.copy(isRefreshing = true)
            }
            try {
                syncCollection().fold(
                    onSuccess = {
                        // Sync successful, data will update automatically via flows.
                    },
                    onFailure = {
                        tryEmitUiEffect(CollectionUiEffect.ShowSnackbar(uiTextRes(R.string.sync_collections_failed_error)))
                    }
                )
            } finally {
                updateBaseState { state ->
                    state.copy(isRefreshing = false)
                }
            }
        }
    }

    private fun buildSectionIndices(sections: List<CollectionSection>): Map<Char, Int> {
        var index = 0
        val result = LinkedHashMap<Char, Int>(sections.size)

        sections.forEach { section ->
            result[section.key] = index
            index += 1
            index += section.games.size
        }

        return result
    }

    private fun emptyReason(
        searchQuery: String,
        quickFilter: QuickFilter
    ): EmptyReason =
        when {
            searchQuery.trim().isNotEmpty() -> EmptyReason.NO_SEARCH_RESULTS
            quickFilter != QuickFilter.ALL -> EmptyReason.NO_FILTER_RESULTS
            else -> EmptyReason.NO_GAMES
        }
}
