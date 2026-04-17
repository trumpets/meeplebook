package app.meeplebook.core.ui.architecture

/**
 * Bundle of domain and UI effects produced by handling a single event.
 *
 * Domain effects are handled by the ViewModel, while UI effects are emitted to the UI layer as
 * one-shot events.
 */
data class ProducedEffects<DomainEffect, UiEffect>(
    val effects: List<DomainEffect> = emptyList(),
    val uiEffects: List<UiEffect> = emptyList()
) {

    /**
     * Returns `true` when this transition produced no domain or UI work.
     */
    fun isEmpty(): Boolean = effects.isEmpty() && uiEffects.isEmpty()

    companion object {
        /**
         * Convenience factory for a transition that produced no side effects.
         */
        fun <DomainEffect, UiEffect> none(): ProducedEffects<DomainEffect, UiEffect> =
            ProducedEffects()
    }
}
