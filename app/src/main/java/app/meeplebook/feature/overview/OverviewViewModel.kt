package app.meeplebook.feature.overview

import androidx.lifecycle.viewModelScope
import app.meeplebook.core.sync.manager.SyncManager
import app.meeplebook.core.ui.architecture.ReducerViewModel
import app.meeplebook.feature.overview.domain.ObserveOverviewUseCase
import app.meeplebook.feature.overview.effect.OverviewEffect
import app.meeplebook.feature.overview.effect.OverviewEffectProducer
import app.meeplebook.feature.overview.effect.OverviewUiEffect
import app.meeplebook.feature.overview.reducer.OverviewReducer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
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
    private val syncManager: SyncManager
) : ReducerViewModel<OverviewBaseState, OverviewEvent, OverviewEffect, OverviewUiEffect>(
    initialState = OverviewBaseState(),
    reducer = reducer,
    effectProducer = effectProducer
) {

    init {
        syncManager.schedulePeriodicFullSync()
        syncManager.enqueueFullSync()
    }

    /**
     * Renderable screen state derived from reducer-owned base state and the observed domain data.
     */
    val uiState: StateFlow<OverviewUiState> =
        combine(
            baseState,
            observeOverviewUseCase()
        ) { state, domainOverview ->
            state.errorMessageUiText?.let { errorMessageUiText ->
                OverviewUiState.Error(errorMessageUiText)
            } ?: domainOverview.toContentState()
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
            OverviewEffect.Refresh -> refresh()
        }
    }

    private fun refresh() {
        syncManager.enqueueFullSync()
    }
}
