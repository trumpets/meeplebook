package app.meeplebook.feature.addplay.effect

import app.meeplebook.R
import app.meeplebook.core.ui.UiText
import app.meeplebook.feature.addplay.AddPlayEffect
import app.meeplebook.feature.addplay.AddPlayEffects
import app.meeplebook.feature.addplay.AddPlayEvent
import app.meeplebook.feature.addplay.AddPlayUiEffect
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
     * Derive effects for the transition [oldState] → [newState] triggered by [event].
     *
     * @param oldState State before the reducer ran.
     * @param newState State after the reducer ran.
     * @param event    The user-driven event that caused the transition.
     * @return An [AddPlayEffects] holder that may contain zero or more effects of each kind.
     */
    fun produce(
        oldState: AddPlayUiState,
        newState: AddPlayUiState,
        event: AddPlayEvent
    ): AddPlayEffects {

        val effects = mutableListOf<AddPlayEffect>()
        val uiEffects = mutableListOf<AddPlayUiEffect>()

        when (event) {

            is AddPlayEvent.MetadataEvent.LocationChanged -> {
                effects += AddPlayEffect.LoadPlayerSuggestions(
                    gameId = newState.gameId,
                    location = event.value
                )
            }

            is AddPlayEvent.SuggestionEvent.RefreshPlayerSuggestions -> {
                effects += AddPlayEffect.LoadPlayerSuggestions(
                    gameId = newState.gameId,
                    location = newState.location.value
                )
            }

            is AddPlayEvent.ActionEvent.SaveClicked -> {
                if (newState.canSave) {
                    effects += AddPlayEffect.SavePlay(newState.toDomain())
                } else {
                    uiEffects += AddPlayUiEffect.ShowError(
                        UiText.Res(R.string.add_play_error_missing_required_fields)
                    )
                }
            }

            is AddPlayEvent.ActionEvent.CancelClicked -> {
                uiEffects += AddPlayUiEffect.NavigateBack
            }

            else -> Unit
        }

        return AddPlayEffects(effects = effects, uiEffects = uiEffects)
    }
}
