package app.meeplebook.feature.collection.effect

import app.meeplebook.core.ui.architecture.EffectProducer
import app.meeplebook.core.ui.architecture.ProducedEffects
import app.meeplebook.feature.collection.CollectionBaseState
import app.meeplebook.feature.collection.CollectionEvent
import javax.inject.Inject

/**
 * Produces Collection domain effects and one-shot UI effects from state transitions.
 *
 * The reducer mutates only [CollectionBaseState]. This producer decides what asynchronous work or
 * transient UI action should happen next.
 */
class CollectionEffectProducer @Inject constructor() :
    EffectProducer<CollectionBaseState, CollectionEvent, CollectionEffect, CollectionUiEffect>() {

    /**
     * Derives side effects for [event] after the reducer produced [newState].
     */
    override fun produceEffects(
        newState: CollectionBaseState,
        event: CollectionEvent
    ): ProducedEffects<CollectionEffect, CollectionUiEffect> {
        val effects = mutableListOf<CollectionEffect>()
        val uiEffects = mutableListOf<CollectionUiEffect>()

        when (event) {
            CollectionEvent.ActionEvent.ScreenOpened ->
                effects += CollectionEffect.ScreenOpened

            is CollectionEvent.ActionEvent.GameClicked ->
                uiEffects += CollectionUiEffect.NavigateToGame(event.gameId)

            is CollectionEvent.ActionEvent.JumpToLetter ->
                effects += CollectionEffect.ResolveJumpToLetter(event.letter)

            CollectionEvent.ActionEvent.Refresh ->
                effects += CollectionEffect.Refresh

            else -> Unit
        }

        return ProducedEffects(effects, uiEffects)
    }
}
