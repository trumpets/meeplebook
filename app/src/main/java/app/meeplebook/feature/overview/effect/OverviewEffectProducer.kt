package app.meeplebook.feature.overview.effect

import app.meeplebook.core.ui.architecture.EffectProducer
import app.meeplebook.core.ui.architecture.ProducedEffects
import app.meeplebook.feature.overview.OverviewBaseState
import app.meeplebook.feature.overview.OverviewEvent
import javax.inject.Inject

/**
 * Maps Overview events to domain effects and one-shot UI effects.
 *
 * Overview currently has no synchronous reducer transitions, so action handling mostly routes
 * events either to refresh work or to navigation-style UI effects.
 */
class OverviewEffectProducer @Inject constructor() :
    EffectProducer<OverviewBaseState, OverviewEvent, OverviewEffect, OverviewUiEffect>() {

    /**
     * Produces domain work and transient UI effects for a single Overview event.
     */
    override fun produceEffects(
        newState: OverviewBaseState,
        event: OverviewEvent
    ): ProducedEffects<OverviewEffect, OverviewUiEffect> {
        val effects = mutableListOf<OverviewEffect>()
        val uiEffects = mutableListOf<OverviewUiEffect>()

        when (event) {
            OverviewEvent.ActionEvent.ScreenOpened ->
                effects += OverviewEffect.ScreenOpened

            OverviewEvent.ActionEvent.Refresh ->
                effects += OverviewEffect.Refresh

            OverviewEvent.ActionEvent.LogPlayClicked ->
                uiEffects += OverviewUiEffect.OpenAddPlay

            is OverviewEvent.ActionEvent.RecentPlayClicked ->
                uiEffects += OverviewUiEffect.NavigateToPlay(event.playId)

            is OverviewEvent.ActionEvent.RecentlyAddedClicked ->
                uiEffects += OverviewUiEffect.NavigateToGame(event.gameId)

            is OverviewEvent.ActionEvent.SuggestedGameClicked ->
                uiEffects += OverviewUiEffect.NavigateToGame(event.gameId)
        }

        return ProducedEffects(
            effects = effects,
            uiEffects = uiEffects
        )
    }
}
