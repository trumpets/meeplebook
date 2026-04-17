package app.meeplebook.feature.login

import androidx.lifecycle.viewModelScope
import app.meeplebook.R
import app.meeplebook.core.auth.domain.LoginUseCase
import app.meeplebook.core.auth.model.AuthError
import app.meeplebook.core.result.fold
import app.meeplebook.core.ui.architecture.ReducerViewModel
import app.meeplebook.core.ui.uiTextEmpty
import app.meeplebook.core.ui.uiTextRes
import app.meeplebook.feature.login.effect.LoginEffect
import app.meeplebook.feature.login.effect.LoginEffectProducer
import app.meeplebook.feature.login.effect.LoginUiEffect
import app.meeplebook.feature.login.reducer.LoginReducer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Login screen using the shared reducer/effect pipeline.
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    reducer: LoginReducer,
    effectProducer: LoginEffectProducer,
    private val loginUseCase: LoginUseCase
) : ReducerViewModel<LoginUiState, LoginEvent, LoginEffect, LoginUiEffect>(
    initialState = LoginUiState(),
    reducer = reducer,
    effectProducer = effectProducer
) {

    val uiState: StateFlow<LoginUiState> = baseState

    private var loginJob: Job? = null

    fun onEvent(event: LoginEvent) {
        dispatchEvent(event)
    }

    override fun handleDomainEffect(effect: LoginEffect) {
        when (effect) {
            is LoginEffect.Login -> login(effect.username, effect.password)
        }
    }

    private fun login(
        username: String,
        password: String
    ) {
        loginJob?.cancel()
        loginJob = viewModelScope.launch {
            updateBaseState {
                it.copy(
                    isLoading = true,
                    errorMessage = uiTextEmpty()
                )
            }

            loginUseCase(username, password).fold(
                onSuccess = {
                    updateBaseState {
                        it.copy(
                            isLoading = false,
                            errorMessage = uiTextEmpty()
                        )
                    }
                    postUiEffect(LoginUiEffect.LoginSucceeded)
                },
                onFailure = { error ->
                    updateBaseState {
                        it.copy(
                            isLoading = false,
                            errorMessage = mapError(error)
                        )
                    }
                }
            )
        }
    }

    private fun mapError(error: AuthError) =
        when (error) {
            is AuthError.EmptyCredentials -> uiTextRes(R.string.msg_empty_credentials_error)
            is AuthError.NetworkError -> uiTextRes(R.string.msg_login_failed_error)
            is AuthError.InvalidCredentials -> uiTextRes(R.string.msg_invalid_credentials_error)
            is AuthError.Unknown -> uiTextRes(R.string.msg_login_failed_error)
        }
}
