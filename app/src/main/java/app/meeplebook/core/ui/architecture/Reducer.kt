package app.meeplebook.core.ui.architecture

/**
 * Pure state-transition contract for reducer-driven screens.
 *
 * Implementations must be deterministic and side-effect free: given the same [state] and [event],
 * they must always return the same next state. Asynchronous work belongs in an
 * [EffectProducer], not in the reducer.
 */
fun interface Reducer<State, Event> {

    /**
     * Returns the next [State] for a single [event].
     */
    fun reduce(
        state: State,
        event: Event
    ): State
}
