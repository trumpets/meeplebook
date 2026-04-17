package app.meeplebook.feature.overview

import app.meeplebook.R
import app.meeplebook.core.result.AppResult
import app.meeplebook.core.sync.domain.SyncUserDataUseCase
import app.meeplebook.core.sync.model.SyncUserDataError
import app.meeplebook.core.ui.architecture.ReducerViewModel
import app.meeplebook.core.ui.uiTextRes
import app.meeplebook.feature.overview.domain.ObserveOverviewUseCase
import app.meeplebook.feature.overview.effect.OverviewEffect
import app.meeplebook.feature.overview.effect.OverviewEffectProducer
import app.meeplebook.feature.overview.effect.OverviewUiEffect
import app.meeplebook.feature.overview.reducer.OverviewReducer
import androidx.lifecycle.viewModelScope
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
    private val syncUserDataUseCase: SyncUserDataUseCase
) : ReducerViewModel<OverviewBaseState, OverviewEvent, OverviewEffect, OverviewUiEffect>(
    initialState = OverviewBaseState(),
    reducer = reducer,
    effectProducer = effectProducer
) {
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
            } ?: domainOverview.toContentState(isRefreshing = state.isRefreshing)
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            OverviewUiState.Loading
        )

    private var refreshJob: Job? = null

    fun onEvent(event: OverviewEvent) {
        dispatchEvent(event)
    }

    override fun handleDomainEffect(effect: OverviewEffect) {
        when (effect) {
            OverviewEffect.Refresh -> refresh()
        }
    }

    private fun refresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            updateBaseState {
                it.copy(
                    isRefreshing = true,
                    errorMessageUiText = null
                )
            }

            when (val result = syncUserDataUseCase()) {
                is AppResult.Success -> {
                    updateBaseState { it.copy(isRefreshing = false) }
                }

                is AppResult.Failure -> {
                    updateBaseState {
                        it.copy(
                            isRefreshing = false,
                            errorMessageUiText = mapSyncError(result.error)
                        )
                    }
                }
            }
        }
    }

    private fun mapSyncError(error: SyncUserDataError) =
        when (error) {
            SyncUserDataError.NotLoggedIn ->
                uiTextRes(R.string.sync_not_logged_in_error)

            is SyncUserDataError.CollectionSyncFailed ->
                uiTextRes(R.string.sync_collections_failed_error)

            is SyncUserDataError.PlaysSyncFailed ->
                uiTextRes(R.string.sync_plays_failed_error)
        }
}
