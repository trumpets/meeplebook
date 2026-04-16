package app.meeplebook.feature.plays.effect

import app.meeplebook.feature.plays.PlaysBaseState
import app.meeplebook.feature.plays.PlaysEvent
import javax.inject.Inject

class PlaysEffectProducer @Inject constructor() {

    fun produce(
        newState: PlaysBaseState,
        event: PlaysEvent
    ): PlaysEffects {
        val effects = mutableListOf<PlaysEffect>()
        val uiEffects = mutableListOf<PlaysUiEffect>()

        when (event) {
            is PlaysEvent.ActionEvent.PlayClicked ->
                uiEffects += PlaysUiEffect.NavigateToPlay(event.playId)

            PlaysEvent.ActionEvent.Refresh ->
                effects += PlaysEffect.Refresh

            else -> Unit
        }

        return if (effects.isEmpty() && uiEffects.isEmpty()) {
            PlaysEffects.None
        } else {
            PlaysEffects(
                effects = effects.toList(),
                uiEffects = uiEffects.toList()
            )
        }
    }
}
