package app.meeplebook.feature.plays.effect

import app.meeplebook.core.ui.architecture.EffectProducer
import app.meeplebook.core.ui.architecture.ProducedEffects
import app.meeplebook.feature.plays.PlaysBaseState
import app.meeplebook.feature.plays.PlaysEvent
import javax.inject.Inject

/**
 * Produces Plays domain and UI effects from a state transition.
 *
 * The reducer updates only [PlaysBaseState]. This producer decides what asynchronous work or
 * one-shot UI action should happen after that state change, mirroring the AddPlay architecture in a
 * simpler feature.
 */
class PlaysEffectProducer @Inject constructor() :
    EffectProducer<PlaysBaseState, PlaysEvent, PlaysEffect, PlaysUiEffect>() {

    /**
     * Derives effects for the given [event] after the reducer produced [newState].
     *
     * @param newState Reducer-owned state after the current event has been applied.
     * @param event The event being processed.
     * @return The domain and UI effects to execute for this transition.
     */
    override fun produceEffects(
        newState: PlaysBaseState,
        event: PlaysEvent
    ): ProducedEffects<PlaysEffect, PlaysUiEffect> {
        val effects = mutableListOf<PlaysEffect>()
        val uiEffects = mutableListOf<PlaysUiEffect>()

        when (event) {
            is PlaysEvent.ActionEvent.PlayClicked ->
                uiEffects += PlaysUiEffect.NavigateToPlay(event.playId)

            PlaysEvent.ActionEvent.Refresh ->
                effects += PlaysEffect.Refresh

            else -> Unit
        }

        return ProducedEffects(effects, uiEffects)
    }
}
