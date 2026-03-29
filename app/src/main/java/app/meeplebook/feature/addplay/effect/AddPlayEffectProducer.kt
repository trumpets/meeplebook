package app.meeplebook.feature.addplay.effect

import app.meeplebook.R
import app.meeplebook.core.ui.uiTextRes
import app.meeplebook.feature.addplay.AddPlayEvent
import app.meeplebook.feature.addplay.AddPlayUiState

/**
 * Produces [AddPlayEffects] (domain + UI side effects) from a state transition.
 *
 * This class is the single source of truth for *what should happen* after an event
 * is processed by the reducer.  It is intentionally free of ViewModel or Android
 * dependencies so that it can be unit-tested without instrumentation.
 *
 * ## Effect types
 * | Effect type            | Interface          | Handler       |
 * |------------------------|--------------------|---------------|
 * | Data / domain effects  | [AddPlayEffect]    | ViewModel     |
 * | UI / navigation effects| [AddPlayUiEffect]  | Composable    |
 */
class AddPlayEffectProducer {

    /**
     * Derives side effects for the [newState] reached by processing [event].
     *
     * Callers must ensure [AddPlayUiState.gameId] is non-null before a
     * [AddPlayEvent.MetadataEvent.LocationChanged] event is dispatched; the
     * implementation asserts this with `!!`.
     *
     * @param newState State after the reducer ran.
     * @param event    The user-driven event that caused the transition.
     * @return An [AddPlayEffects] holder; [AddPlayEffects.None] when no effects apply.
     */
    fun produce(
        newState: AddPlayUiState,
        event: AddPlayEvent
    ): AddPlayEffects {

        val effects = mutableListOf<AddPlayEffect>()
        val uiEffects = mutableListOf<AddPlayUiEffect>()

        when (event) {

            is AddPlayEvent.MetadataEvent.LocationChanged -> {
                effects += AddPlayEffect.LoadPlayerSuggestions(
                    gameId = newState.gameId!!,
                    location = event.value
                )
            }

            is AddPlayEvent.ActionEvent.SaveClicked -> {
                if (newState.canSave) {
                    effects += AddPlayEffect.SavePlay(newState.toCreatePlayCommand())
                } else {
                    uiEffects += AddPlayUiEffect.ShowError(
                        uiTextRes(R.string.add_play_cant_save)
                    )
                }
            }

            is AddPlayEvent.ActionEvent.CancelClicked -> {
                uiEffects += AddPlayUiEffect.NavigateBack
            }

            else -> Unit
        }

        return if (effects.isEmpty() && uiEffects.isEmpty()) {
            AddPlayEffects.None
        } else {
            AddPlayEffects(
                effects = effects.toList(),
                uiEffects = uiEffects.toList()
            )
        }
    }
}
