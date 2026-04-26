package app.meeplebook.feature.overview

import androidx.lifecycle.viewModelScope
import app.meeplebook.core.sync.domain.ObserveFullSyncStateUseCase
import app.meeplebook.core.sync.domain.ShouldAutoSyncOnScreenEnterUseCase
import app.meeplebook.core.sync.manager.SyncManager
import app.meeplebook.core.sync.model.SyncState
import app.meeplebook.core.sync.model.SyncType
import app.meeplebook.core.sync.model.observeRefreshCompletion
import app.meeplebook.core.ui.architecture.ReducerViewModel
import app.meeplebook.feature.overview.domain.ObserveOverviewUseCase
import app.meeplebook.feature.overview.effect.OverviewEffect
import app.meeplebook.feature.overview.effect.OverviewEffectProducer
import app.meeplebook.feature.overview.effect.OverviewUiEffect
import app.meeplebook.feature.overview.reducer.OverviewReducer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Overview screen.
 *
 * The screen follows the shared reducer architecture: [OverviewBaseState] owns refresh/error
 * bookkeeping, [OverviewEvent] flows through the reducer/effect producer pipeline, and the exposed
 * [uiState] is derived by combining base state with [ObserveOverviewUseCase].
 */
@HiltViewModel
class OverviewViewModel @Inject constructor(
    reducer: OverviewReducer,
    effectProducer: OverviewEffectProducer,
    observeOverviewUseCase: ObserveOverviewUseCase,
    observeFullSyncState: ObserveFullSyncStateUseCase,
    private val shouldAutoSyncOnScreenEnter: ShouldAutoSyncOnScreenEnterUseCase,
    private val syncManager: SyncManager
) : ReducerViewModel<OverviewBaseState, OverviewEvent, OverviewEffect, OverviewUiEffect>(
    initialState = OverviewBaseState(),
    reducer = reducer,
    effectProducer = effectProducer
) {

    private val syncState: StateFlow<SyncState> = observeFullSyncState()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            SyncState()
        )

    /**
     * Renderable screen state derived from reducer-owned base state and the observed domain data.
     */
    val uiState: StateFlow<OverviewUiState> =
        combine(
            baseState,
            observeOverviewUseCase(),
            syncState
        ) { state, domainOverview, syncState ->
            state.errorMessageUiText?.let { errorMessageUiText ->
                OverviewUiState.Error(errorMessageUiText)
            } ?: domainOverview.toContentState(state, syncState)
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            OverviewUiState.Loading
        )

    fun onEvent(event: OverviewEvent) {
        dispatchEvent(event)
    }

    override fun handleDomainEffect(effect: OverviewEffect) {
        when (effect) {
            OverviewEffect.ScreenOpened -> onScreenOpened()
            OverviewEffect.Refresh -> refresh()
        }
    }

    private fun onScreenOpened() {
        syncManager.schedulePeriodicFullSync()
        viewModelScope.launch {
            if (shouldAutoSyncOnScreenEnter(SyncType.COLLECTION, SyncType.PLAYS)) {
                syncManager.enqueueFullSync()
            }
        }
    }

    private var refreshJob : Job? = null

    /**
     * Enqueues full sync through the app-level sync manager.
     *
     * Manual refresh always runs immediately; only the screen-entry auto sync triggered by
     * [OverviewEffect.ScreenOpened] in [onScreenOpened] is guarded by
     * [ShouldAutoSyncOnScreenEnterUseCase].
     */
    private fun refresh() {
        updateBaseState { it.copy(isRefreshing = true) }

        refreshJob?.cancel()
        refreshJob = syncState
            .observeRefreshCompletion(viewModelScope) {
                updateBaseState { it.copy(isRefreshing = false) }
            }
        syncManager.enqueueFullSync()
    }
}
