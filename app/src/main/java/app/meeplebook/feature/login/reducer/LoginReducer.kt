package app.meeplebook.feature.login.reducer

import app.meeplebook.core.ui.architecture.Reducer
import app.meeplebook.core.ui.uiTextEmpty
import app.meeplebook.feature.login.LoginEvent
import app.meeplebook.feature.login.LoginUiState
import javax.inject.Inject

/**
 * Synchronous reducer for Login form state.
 */
class LoginReducer @Inject constructor() : Reducer<LoginUiState, LoginEvent> {
    override fun reduce(
        state: LoginUiState,
        event: LoginEvent
    ): LoginUiState {
        return when (event) {
            is LoginEvent.UsernameChanged ->
                state.copy(
                    username = event.username,
                    errorMessage = uiTextEmpty()
                )

            is LoginEvent.PasswordChanged ->
                state.copy(
                    password = event.password,
                    errorMessage = uiTextEmpty()
                )

            LoginEvent.Submit ->
                state.copy(errorMessage = uiTextEmpty())
        }
    }
}
