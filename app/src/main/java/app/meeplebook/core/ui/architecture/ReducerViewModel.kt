package app.meeplebook.core.ui.architecture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Lightweight base [ViewModel] for screens that follow the reducer/effect pipeline.
 *
 * This abstraction intentionally standardizes only the shared orchestration:
 * - reducer-owned mutable base state
 * - event dispatch through reducer then effect producer
 * - domain-effect handling hook
 * - one-shot UI effect emission through [uiEffect]
 *
 * It intentionally does **not** own feature-specific `combine(...)` chains that derive renderable
 * UI state from base state plus external flows. Screens keep that composition logic locally.
 */
abstract class ReducerViewModel<State, Event, DomainEffect, UiEffect>(
    initialState: State,
    private val reducer: Reducer<State, Event>,
    private val effectProducer: EffectProducer<State, Event, DomainEffect, UiEffect>
) : ViewModel() {

    /**
     * Reducer-owned mutable state. Features derive query flows and UI state from this source.
     */
    private val _baseState = MutableStateFlow(initialState)

    private val _uiEffect = MutableSharedFlow<UiEffect>()

    /**
     * One-shot UI effects emitted by [dispatchEvent] or by subclass effect handlers.
     */
    val uiEffect: SharedFlow<UiEffect> = _uiEffect.asSharedFlow()
    val baseState: StateFlow<State> = _baseState

    /**
     * Current reducer-owned state snapshot.
     */
    protected val currentBaseState: State
        get() = _baseState.value

    /**
     * Runs a single [event] through the standard reducer/effect pipeline.
     */
    protected fun dispatchEvent(event: Event) {
        val oldState = _baseState.value
        val newState = reducer.reduce(oldState, event)
        _baseState.value = newState

        val producedEffects = effectProducer.produce(newState, event)
        handleDomainEffects(producedEffects.effects)
        handleUiEffects(producedEffects.uiEffects)
    }

    /**
     * Updates reducer-owned state for async effect results without replacing the flow instance.
     */
    protected fun updateBaseState(transform: (State) -> State) {
        _baseState.update(transform)
    }

    /**
     * Emits a one-shot [effect] to the UI layer.
     */
    protected suspend fun emitUiEffect(effect: UiEffect) {
        _uiEffect.emit(effect)
    }

    /**
     * Emits a one-shot [effect] to the UI layer without suspending.
     */
    protected fun postUiEffect(effect: UiEffect) {
        viewModelScope.launch {
            emitUiEffect(effect)
        }
    }

    /**
     * Handles domain effects produced for the last transition.
     *
     * The default behavior routes each effect to [handleDomainEffect] in order.
     */
    private fun handleDomainEffects(effects: List<DomainEffect>) {
        effects.forEach(::handleDomainEffect)
    }

    /**
     * Handles a single domain [effect].
     */
    protected abstract fun handleDomainEffect(effect: DomainEffect)

    /**
     * Emits produced UI effects through [uiEffect] in order.
     */
    private fun handleUiEffects(uiEffects: List<UiEffect>) {
        if (uiEffects.isEmpty()) return
        viewModelScope.launch {
            uiEffects.forEach{
                emitUiEffect(it)
            }
        }
    }
}
