package app.meeplebook.feature.overview

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.meeplebook.R
import app.meeplebook.core.collection.CollectionRepository
import app.meeplebook.core.plays.PlaysRepository
import app.meeplebook.core.result.AppResult
import app.meeplebook.core.sync.SyncTimeRepository
import app.meeplebook.core.util.SyncFormatter
import app.meeplebook.feature.home.domain.GetCollectionHighlightsUseCase
import app.meeplebook.feature.home.domain.GetHomeStatsUseCase
import app.meeplebook.feature.home.domain.GetRecentPlaysUseCase
import app.meeplebook.feature.home.domain.SyncHomeDataUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OverviewViewModel @Inject constructor(
    private val getHomeStatsUseCase: GetHomeStatsUseCase,
    private val getRecentPlaysUseCase: GetRecentPlaysUseCase,
    private val getCollectionHighlightsUseCase: GetCollectionHighlightsUseCase,
    private val syncHomeDataUseCase: SyncHomeDataUseCase,
    private val collectionRepository: CollectionRepository,
    private val playsRepository: PlaysRepository,
    private val syncTimeRepository: SyncTimeRepository,
    private val syncFormatter: SyncFormatter,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(OverviewUiState(isLoading = true))
    val uiState: StateFlow<OverviewUiState> = _uiState.asStateFlow()
    
    private val refreshOnLogin: Boolean = savedStateHandle.get<Boolean>("refreshOnLogin") ?: false

    init {
        // Observe data changes reactively
        observeDataChanges()
        
        // Only refresh data on initialization if coming from login
        if (refreshOnLogin) {
            refresh()
        }
    }

    /**
     * Observes collection, plays, and sync time changes, automatically updating UI state.
     */
    private fun observeDataChanges() {
        viewModelScope.launch {
            combine(
                collectionRepository.observeCollection(),
                playsRepository.observePlays(),
                syncTimeRepository.observeLastFullSync()
            ) { _, _, lastFullSync ->
                // Calculate all data using use cases
                val stats = getHomeStatsUseCase()
                val recentPlays = getRecentPlaysUseCase()
                val (recentlyAdded, suggested) = getCollectionHighlightsUseCase()
                
                // Format sync time
                val syncText = syncFormatter.formatLastSynced(lastFullSync)

                // Return new state data
                Triple(
                    Triple(stats, recentPlays, Pair(recentlyAdded, suggested)),
                    syncText,
                    lastFullSync
                )
            }.collect { (dataTriple, syncText, _) ->
                val (stats, recentPlays, highlights) = dataTriple
                val (recentlyAdded, suggested) = highlights
                
                // Update state with new values, preserving isRefreshing and errorMessage
                _uiState.update { current ->
                    current.copy(
                        stats = stats,
                        recentPlays = recentPlays,
                        recentlyAddedGame = recentlyAdded,
                        suggestedGame = suggested,
                        lastSyncedText = syncText,
                        isLoading = false
                    )
                }
            }
        }
    }

    /**
     * Triggers a refresh by syncing data from BGG.
     */
    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, errorMessageResId = null) }
            
            // Sync data from BGG and handle result
            val syncResult = syncHomeDataUseCase()
            when (syncResult) {
                is AppResult.Success -> {
                    // Data will be automatically updated through observeDataChanges
                    _uiState.update { it.copy(isRefreshing = false) }
                }
                is AppResult.Failure -> {
                    _uiState.update { 
                        it.copy(
                            isRefreshing = false,
                            errorMessageResId = R.string.sync_failed_error
                        )
                    }
                }
            }
        }
    }
}
