package app.meeplebook.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.meeplebook.core.result.AppResult
import app.meeplebook.feature.home.domain.GetCollectionHighlightsUseCase
import app.meeplebook.feature.home.domain.GetHomeStatsUseCase
import app.meeplebook.feature.home.domain.GetRecentPlaysUseCase
import app.meeplebook.feature.home.domain.SyncHomeDataUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    private val syncHomeDataUseCase: SyncHomeDataUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState(isLoading = true))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    private var lastSyncTime: LocalDateTime? = null

    init {
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
                    // Record sync time
                    lastSyncTime = LocalDateTime.now()
                    
                    // Reload data
                    loadHomeData()
                    
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
     * Loads home screen data from local storage.
     */
    private fun loadHomeData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            // Load all data in parallel
            val statsDeferred = async { getHomeStatsUseCase() }
            val recentPlaysDeferred = async { getRecentPlaysUseCase() }
            val highlightsDeferred = async { getCollectionHighlightsUseCase() }
            
            val stats = statsDeferred.await()
            val recentPlays = recentPlaysDeferred.await()
            val (recentlyAdded, suggested) = highlightsDeferred.await()
            
            _uiState.update {
                it.copy(
                    stats = stats,
                    recentPlays = recentPlays,
                    recentlyAddedGame = recentlyAdded,
                    suggestedGame = suggested,
                    lastSyncedText = formatLastSyncedText(),
                    isLoading = false
                )
            }
        }
    }
    
    /**
     * Formats the last synced time into a human-readable string.
     */
    private fun formatLastSyncedText(): String {
        val syncTime = lastSyncTime ?: return "Never synced"
        
        val now = LocalDateTime.now()
        val minutesAgo = ChronoUnit.MINUTES.between(syncTime, now)
        
        return when {
            minutesAgo < 1L -> "Last synced: just now"
            minutesAgo < MINUTES_IN_HOUR -> "Last synced: $minutesAgo min ago"
            minutesAgo < MINUTES_IN_TWO_HOURS -> "Last synced: 1 hour ago"
            minutesAgo < MINUTES_IN_DAY -> "Last synced: ${minutesAgo / MINUTES_IN_HOUR} hours ago"
            else -> {
                val daysAgo = minutesAgo / MINUTES_IN_DAY
                "Last synced: $daysAgo day${if (daysAgo > 1) "s" else ""} ago"
            }
        }
    }
}
