package app.meeplebook.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.meeplebook.core.collection.CollectionRepository
import app.meeplebook.core.plays.PlaysRepository
import app.meeplebook.core.result.AppResult
import app.meeplebook.core.sync.SyncTimeRepository
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
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject

private const val MINUTES_IN_HOUR = 60L
private const val MINUTES_IN_TWO_HOURS = 120L
private const val MINUTES_IN_DAY = 1440L

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getHomeStatsUseCase: GetHomeStatsUseCase,
    private val getRecentPlaysUseCase: GetRecentPlaysUseCase,
    private val getCollectionHighlightsUseCase: GetCollectionHighlightsUseCase,
    private val syncHomeDataUseCase: SyncHomeDataUseCase,
    private val collectionRepository: CollectionRepository,
    private val playsRepository: PlaysRepository,
    private val syncTimeRepository: SyncTimeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState(isLoading = true))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        // Observe data changes reactively
        observeDataChanges()
        
        // Refresh data on initialization (syncs from BGG)
        // This ensures fresh data after login
        refresh()
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
                    _uiState.update { it.copy(isRefreshing = false) }
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
            // Combine all data sources
            combine(
                collectionRepository.observeCollection(),
                playsRepository.observePlays(),
                syncTimeRepository.observeLastFullSync(),
                syncTimeRepository.observeLastCollectionSync(),
                syncTimeRepository.observeLastPlaysSync()
            ) { collection, plays, lastFullSync, lastCollectionSync, lastPlaysSync ->
                // Calculate stats
                val stats = getHomeStatsUseCase()
                
                // Get recent plays
                val recentPlays = getRecentPlaysUseCase()
                
                // Get highlights
                val (recentlyAdded, suggested) = getCollectionHighlightsUseCase()
                
                // Format sync times
                val syncText = formatSyncText(lastFullSync, lastCollectionSync, lastPlaysSync)
                
                HomeUiState(
                    stats = stats,
                    recentPlays = recentPlays,
                    recentlyAddedGame = recentlyAdded,
                    suggestedGame = suggested,
                    lastSyncedText = syncText,
                    isLoading = false,
                    isRefreshing = _uiState.value.isRefreshing,
                    errorMessage = _uiState.value.errorMessage
                )
            }.collect { newState ->
                _uiState.value = newState
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
