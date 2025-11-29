package app.meeplebook.feature.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.meeplebook.R
import app.meeplebook.core.domain.AuthError
import app.meeplebook.core.domain.LoginUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onUsernameChange(newUsername: String) {
        _uiState.update { it.copy(username = newUsername) }
    }

    fun onPasswordChange(newPassword: String) {
        _uiState.update { it.copy(password = newPassword) }
    }

    fun login() {
        val currentState = _uiState.value

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessageResId = null) }

            loginUseCase(currentState.username, currentState.password).fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false, isLoggedIn = true) }
                },
                onFailure = { error ->
                    val resId = when (error) {
                        is AuthError.EmptyCredentials -> R.string.msg_empty_credentials_error
                        is AuthError.NetworkError -> R.string.msg_login_failed_error
                        is AuthError.InvalidCredentials -> R.string.msg_invalid_credentials_error
                        is AuthError.Unknown -> R.string.msg_login_failed_error
                        else -> R.string.msg_login_failed_error
                    }
                    _uiState.update { it.copy(isLoading = false, errorMessageResId = resId) }
                }
            )
        }
    }
}