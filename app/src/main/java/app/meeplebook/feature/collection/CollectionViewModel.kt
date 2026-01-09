package app.meeplebook.feature.collection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.meeplebook.core.ui.StringProvider
import app.meeplebook.core.collection.model.CollectionDataQuery
import app.meeplebook.core.collection.model.CollectionSort
import app.meeplebook.core.collection.model.QuickFilter
import app.meeplebook.feature.collection.domain.ObserveCollectionDomainSectionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
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
    private val observeCollectionDomainSectionsUseCase: ObserveCollectionDomainSectionsUseCase,
    private val stringProvider: StringProvider
) : ViewModel() {

    private val rawSearchQuery = MutableStateFlow("")
    @OptIn(FlowPreview::class)
    private val debouncedSearchQuery: StateFlow<String> =
        rawSearchQuery
            .debounce(300)
            .distinctUntilChanged()
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                ""
            )

    private val quickFilter = MutableStateFlow(QuickFilter.ALL)
    private val sort = MutableStateFlow(CollectionSort.ALPHABETICAL)
    private val viewMode = MutableStateFlow(CollectionViewMode.LIST)

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
        collectionDataQuery
            .flatMapLatest { query ->
                observeCollectionDomainSectionsUseCase(query)
            }
            .map { domainSections ->
                val uiSections = domainSections.map { it.toCollectionSection(stringProvider) }

                if (uiSections.isEmpty()) {
                    CollectionUiState.Empty(emptyReason(
                        searchQuery = rawSearchQuery.value,
                        quickFilter = quickFilter.value
                    ))
                } else {
                    val sectionIndices = buildSectionIndices(uiSections)

                    CollectionUiState.Content(
                        searchQuery = rawSearchQuery.value,
                        viewMode = viewMode.value,
                        sort = sort.value,
                        activeQuickFilter = quickFilter.value,
                        availableSortOptions = CollectionSort.entries,
                        sections = uiSections,
                        sectionIndices = sectionIndices,
                        totalGameCount = uiSections.sumOf { it.games.size },
                        isRefreshing = false,
                        showAlphabetJump = uiSections.size > 1,
                        isSortSheetVisible = false
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
            rawSearchQuery
        ) { state, viewMode, rawQuery ->
            when (state) {
                is CollectionUiState.Content ->
                    state.copy(
                        viewMode = viewMode,
                        searchQuery = rawQuery
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

            else -> Unit
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