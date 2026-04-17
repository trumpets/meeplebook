package app.meeplebook.feature.login.effect

import app.meeplebook.core.ui.architecture.EffectProducer
import app.meeplebook.core.ui.architecture.ProducedEffects
import app.meeplebook.feature.login.LoginEvent
import app.meeplebook.feature.login.LoginUiState
import javax.inject.Inject

/**
 * Maps Login events to domain work and one-shot UI effects.
 */
class LoginEffectProducer @Inject constructor() :
    EffectProducer<LoginUiState, LoginEvent, LoginEffect, LoginUiEffect>() {

    override fun produceEffects(
        newState: LoginUiState,
        event: LoginEvent
    ): ProducedEffects<LoginEffect, LoginUiEffect> {
        return when (event) {
            is LoginEvent.Submit ->
                if (newState.isLoading) {
                    ProducedEffects.none()
                } else {
                    ProducedEffects(
                        effects = listOf(LoginEffect.Login(newState.username, newState.password))
                    )
                }

            is LoginEvent.PasswordChanged,
            is LoginEvent.UsernameChanged -> ProducedEffects.none()
        }
    }
}
