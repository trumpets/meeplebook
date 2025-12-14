package app.meeplebook.feature.overview

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.meeplebook.R
import app.meeplebook.core.result.AppResult
import app.meeplebook.core.sync.domain.SyncUserDataUseCase
import app.meeplebook.core.sync.model.SyncUserDataError
import app.meeplebook.core.ui.StringProvider
import app.meeplebook.core.util.formatLastSynced
import app.meeplebook.feature.overview.domain.ObserveOverviewUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OverviewViewModel @Inject constructor(
    private val observeOverviewUseCase: ObserveOverviewUseCase,
    private val syncUserDataUseCase: SyncUserDataUseCase,
    private val stringProvider: StringProvider,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiEffects = MutableStateFlow(OverviewUiEffects(isRefreshing = false, errorMessageResId = null))

    val uiState: StateFlow<OverviewUiState> =
        combine(
            observeOverviewUseCase()
                .map { domain ->
                    OverviewUiState(
                        stats = domain.stats.toOverviewStats(),
                        recentPlays = domain.recentPlays.map { it.toRecentPlay(stringProvider) },
                        recentlyAddedGame = domain.recentlyAddedGame?.toGameHighlight(stringProvider),
                        suggestedGame = domain.suggestedGame?.toGameHighlight(stringProvider),
                        lastSyncedText = formatLastSynced(stringProvider, domain.lastSyncedDate),
                        isLoading = false
                    )
                },
            _uiEffects
        ) { data, effects ->
            data.copy(
                isRefreshing = effects.isRefreshing,
                errorMessageResId = effects.errorMessageResId
            )
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            OverviewUiState(isLoading = false)
        )

    private val refreshOnLogin: Boolean = savedStateHandle.get<Boolean>("refreshOnLogin") ?: false
    private var refreshJob: Job? = null

    init {
        // Only refresh data on initialization if coming from login
        if (refreshOnLogin) {
            refresh()
        }
    }

    /**
     * Triggers a refresh by syncing data from BGG.
     */
    fun refresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            _uiEffects.update {
                it.copy(isRefreshing = true, errorMessageResId = null)
            }

            when (val result = syncUserDataUseCase()) {
                is AppResult.Success -> {
                    _uiEffects.update { it.copy(isRefreshing = false) }
                }
                is AppResult.Failure -> {
                    _uiEffects.update {
                        it.copy(
                            isRefreshing = false,
                            errorMessageResId = mapSyncError(result.error)
                        )
                    }
                }
            }
        }
    }

    @StringRes
    private fun mapSyncError(error: SyncUserDataError): Int =
        when (error) {
            SyncUserDataError.NotLoggedIn ->
                R.string.sync_not_logged_in_error

            is SyncUserDataError.CollectionSyncFailed ->
                R.string.sync_collections_failed_error

            is SyncUserDataError.PlaysSyncFailed ->
                R.string.sync_plays_failed_error
        }
}