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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaysViewModel @Inject constructor(
    private val observePlaysScreenData: ObservePlaysScreenDataUseCase,
    private val syncPlays: SyncPlaysUseCase
) : ViewModel() {

    private val rawSearchQuery = MutableStateFlow("")
    private val isRefreshing = MutableStateFlow(false)

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    private val searchResults =
        searchableFlow(
            queryFlow = rawSearchQuery,
            debounceMillis = DebounceDurations.SearchQuery.inWholeMilliseconds
        ) { query ->
            observePlaysScreenData(query)
        }

    val uiState: StateFlow<PlaysUiState> =
        combine(
            rawSearchQuery,
            searchResults,
            isRefreshing
        ) { rawQuery, screenData, refreshing ->

            val sections = screenData.sections.map { it.toPlaysSection() }
            val common = PlaysCommonState(
                searchQuery = rawQuery,
                playStats = screenData.stats.toPlayStats(),
                isRefreshing = refreshing
            )

            when {
                sections.isEmpty() -> PlaysUiState.Empty(
                    reason = emptyReason(rawQuery),
                    common = common
                )
                else -> PlaysUiState.Content(
                    sections = sections,
                    common = common
                )
            }
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            PlaysUiState.Loading
        )

    private val _uiEffect = MutableSharedFlow<PlaysUiEffects>()
    val uiEffect = _uiEffect.asSharedFlow()

    private var refreshJob: Job? = null

    fun onEvent(event: PlaysEvent) {
        when (event) {
            is PlaysEvent.SearchChanged -> {
                rawSearchQuery.value = event.query
            }

            is PlaysEvent.PlayClicked -> {
                emitEffect(PlaysUiEffects.NavigateToPlay(event.playId))
            }

            is PlaysEvent.Refresh -> {
                refreshJob?.cancel()
                refreshJob = viewModelScope.launch {
                    isRefreshing.value = true
                    try {
                        syncPlays().fold(
                            onSuccess = {
                                // Sync successful, data will update automatically via flows
                            },
                            onFailure = { _ ->
                                emitEffect(
                                    PlaysUiEffects.ShowSnackbar(
                                        messageUiText = uiTextRes(R.string.sync_plays_failed_error)
                                    )
                                )
                            }
                        )
                    } finally {
                        isRefreshing.value = false
                    }
                }
            }

            PlaysEvent.LogPlayClicked -> {
                // TODO: Implement navigation or handling for logging a play.
            }
        }
    }

    private fun emitEffect(effect: PlaysUiEffects) {
        viewModelScope.launch {
            _uiEffect.emit(effect)
        }
    }

    private fun emptyReason(
        searchQuery: String,
    ): EmptyReason =
        when {
            searchQuery.isNotBlank() ->
                EmptyReason.NO_SEARCH_RESULTS

            else ->
                EmptyReason.NO_PLAYS
        }
}