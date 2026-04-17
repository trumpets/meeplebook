package app.meeplebook.feature.overview.reducer

import app.meeplebook.core.ui.architecture.Reducer
import app.meeplebook.feature.overview.OverviewBaseState
import app.meeplebook.feature.overview.OverviewEvent
import javax.inject.Inject

/**
 * Synchronous reducer for Overview base state.
 *
 * Overview currently keeps synchronous transitions minimal, so events leave [OverviewBaseState]
 * unchanged and delegate work to [app.meeplebook.feature.overview.effect.OverviewEffectProducer].
 * Async refresh results update state through [app.meeplebook.core.ui.architecture.ReducerViewModel.updateBaseState].
 */
class OverviewReducer @Inject constructor() : Reducer<OverviewBaseState, OverviewEvent> {
    /**
     * Returns the current state unchanged because Overview has no immediate reducer-only updates.
     */
    override fun reduce(
        state: OverviewBaseState,
        event: OverviewEvent
    ): OverviewBaseState = state
}
