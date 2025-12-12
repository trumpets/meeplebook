package app.meeplebook.feature.home

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
class HomeViewModel @Inject constructor(
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

    private val _uiState = MutableStateFlow(HomeUiState(isLoading = true))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
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
     * Refreshes home data by syncing with BGG and reloading.
     * Should be called on pull-to-refresh and after login.
     */
    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, errorMessage = null) }
            
            // Sync data from BGG
            val syncResult = syncHomeDataUseCase()
            
            when (syncResult) {
                is AppResult.Success -> {
                    // Data will be updated reactively through observeDataChanges
                    _uiState.update { it.copy(isRefreshing = false, errorMessage = null) }
                }
                is AppResult.Failure -> {
                    // Show error to user via UI state
                    _uiState.update { 
                        it.copy(
                            isRefreshing = false,
                            errorMessage = "Failed to sync data. Please try again."
                        )
                    }
                }
            }
        }
    }

    /**
     * Observes data changes and updates UI state reactively.
     */
    private fun observeDataChanges() {
        viewModelScope.launch {
            // Combine all data sources - only observe last full sync
            combine(
                collectionRepository.observeCollection(),
                playsRepository.observePlays(),
                syncTimeRepository.observeLastFullSync()
            ) { collection, plays, lastFullSync ->
                // Calculate stats
                val stats = getHomeStatsUseCase()
                
                // Get recent plays
                val recentPlays = getRecentPlaysUseCase()
                
                // Get highlights
                val (recentlyAdded, suggested) = getCollectionHighlightsUseCase()
                
                // Format sync time
                val syncText = syncFormatter.formatLastSynced(lastFullSync)
                
                // Return updated values without creating new state object
                Triple(stats, recentPlays, Pair(recentlyAdded, suggested) to syncText)
            }.collect { (stats, recentPlays, highlightsAndSync) ->
                val (highlights, syncText) = highlightsAndSync
                val (recentlyAdded, suggested) = highlights
                
                // Update state preserving isRefreshing and errorMessage
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
}
    
    /**
     * Formats sync time information for display.
     */
    private fun formatSyncText(
        lastFullSync: LocalDateTime?,
        lastCollectionSync: LocalDateTime?,
        lastPlaysSync: LocalDateTime?
    ): String {
        return when {
            lastFullSync != null -> {
                val timeAgo = formatTimeAgo(lastFullSync)
                buildString {
                    append("Full sync: $timeAgo")
                    if (lastCollectionSync != null) {
                        append("\nCollection: ${formatTimeAgo(lastCollectionSync)}")
                    }
                    if (lastPlaysSync != null) {
                        append("\nPlays: ${formatTimeAgo(lastPlaysSync)}")
                    }
                }
            }
            lastCollectionSync != null || lastPlaysSync != null -> {
                buildString {
                    if (lastCollectionSync != null) {
                        append("Collection: ${formatTimeAgo(lastCollectionSync)}")
                    }
                    if (lastPlaysSync != null) {
                        if (isNotEmpty()) append("\n")
                        append("Plays: ${formatTimeAgo(lastPlaysSync)}")
                    }
                }
            }
            else -> "Never synced"
        }
    }
    
    /**
     * Formats a timestamp into a human-readable "time ago" string.
     */
    private fun formatTimeAgo(time: LocalDateTime): String {
        val now = LocalDateTime.now()
        val minutesAgo = ChronoUnit.MINUTES.between(time, now)
        
        return when {
            minutesAgo < 1L -> "just now"
            minutesAgo < MINUTES_IN_HOUR -> "$minutesAgo min ago"
            minutesAgo < MINUTES_IN_TWO_HOURS -> "1 hour ago"
            minutesAgo < MINUTES_IN_DAY -> "${minutesAgo / MINUTES_IN_HOUR} hours ago"
            else -> {
                val daysAgo = minutesAgo / MINUTES_IN_DAY
                "$daysAgo day${if (daysAgo > 1) "s" else ""} ago"
            }
        }
    }
    
}
