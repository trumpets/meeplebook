package app.meeplebook.feature.addplay.effect

import app.meeplebook.R
import app.meeplebook.core.ui.architecture.EffectProducer
import app.meeplebook.core.ui.architecture.ProducedEffects
import app.meeplebook.core.ui.uiTextRes
import app.meeplebook.feature.addplay.AddPlayEvent
import app.meeplebook.feature.addplay.AddPlayUiState
import app.meeplebook.feature.addplay.asGameSelected
import javax.inject.Inject

/**
 * Produces [ProducedEffects] (domain + UI side effects) from a state transition.
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
class AddPlayEffectProducer @Inject constructor() :
    EffectProducer<AddPlayUiState, AddPlayEvent, AddPlayEffect, AddPlayUiEffect>() {

    /**
     * Derives side effects for the [newState] reached by processing [event].
     *
     * @param newState State after the reducer ran.
     * @param event    The user-driven event that caused the transition.
     * @return An [ProducedEffects] holder; [ProducedEffects.none] when no effects apply.
     */
    override fun produceEffects(
        newState: AddPlayUiState,
        event: AddPlayEvent
    ): ProducedEffects<AddPlayEffect, AddPlayUiEffect> {
        val effects = mutableListOf<AddPlayEffect>()
        val uiEffects = mutableListOf<AddPlayUiEffect>()

        when (event) {

            is AddPlayEvent.MetadataEvent.LocationChanged -> {
                newState.asGameSelected {
                    effects += AddPlayEffect.LoadPlayerSuggestions(
                        location = event.value
                    )
                }
            }

            is AddPlayEvent.ActionEvent.SaveClicked -> {
                newState.asGameSelected {
                    if (canSave) {
                        effects += AddPlayEffect.SavePlay(toCreatePlayCommand())
                    } else {
                        uiEffects += AddPlayUiEffect.ShowError(
                            uiTextRes(R.string.add_play_cant_save)
                        )
                    }
                }
            }

            is AddPlayEvent.ActionEvent.CancelClicked -> {
                uiEffects += AddPlayUiEffect.NavigateBack
            }

            else -> Unit
        }

        return ProducedEffects(effects, uiEffects)
    }
}
