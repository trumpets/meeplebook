package app.meeplebook.core.ui.architecture

/**
 * Pure side-effect derivation contract for reducer-driven screens.
 *
 * Implementations decide what should happen *after* a reducer has produced [newState] for an
 * [event]. This keeps reducers synchronous and testable while still making follow-up work explicit.
 */
abstract class EffectProducer<State, Event, DomainEffect, UiEffect> {

    /**
     * Returns the side effects that should be handled after [event] produced [newState].
     */
    fun produce(
        newState: State,
        event: Event
    ): ProducedEffects<DomainEffect, UiEffect> {

        val producedEffects = produceEffects(newState, event)
        return if (producedEffects.isEmpty()) {
            ProducedEffects.none()
        }else {
            producedEffects
        }
    }

    protected abstract fun produceEffects(
        newState: State,
        event: Event
    ): ProducedEffects<DomainEffect, UiEffect>
}
