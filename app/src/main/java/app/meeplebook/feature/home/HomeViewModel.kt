package app.meeplebook.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.meeplebook.feature.home.domain.GetCollectionHighlightsUseCase
import app.meeplebook.feature.home.domain.GetHomeStatsUseCase
import app.meeplebook.feature.home.domain.GetRecentPlaysUseCase
import app.meeplebook.feature.home.domain.SyncHomeDataUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import javax.inject.Inject

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
        // Load initial data when ViewModel is created
        loadHomeData()
    }

    /**
     * Refreshes home data by syncing with BGG and reloading.
     * Should be called on pull-to-refresh and after login.
     */
    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            
            // Sync data from BGG
            syncHomeDataUseCase()
            
            // Record sync time
            lastSyncTime = LocalDateTime.now()
            
            // Reload data
            loadHomeData()
            
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    /**
     * Loads home screen data from local storage.
     */
    private fun loadHomeData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            // Load all data in parallel
            val stats = getHomeStatsUseCase()
            val recentPlays = getRecentPlaysUseCase()
            val (recentlyAdded, suggested) = getCollectionHighlightsUseCase()
            
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
            minutesAgo < 60L -> "Last synced: $minutesAgo min ago"
            minutesAgo < 120L -> "Last synced: 1 hour ago"
            minutesAgo < 1440L -> "Last synced: ${minutesAgo / 60} hours ago"
            else -> {
                val daysAgo = minutesAgo / 1440
                "Last synced: $daysAgo day${if (daysAgo > 1) "s" else ""} ago"
            }
        }
    }
}
