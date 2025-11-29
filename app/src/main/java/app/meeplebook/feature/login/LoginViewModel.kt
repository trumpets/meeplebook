package app.meeplebook.feature.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.meeplebook.R
import app.meeplebook.core.auth.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.net.UnknownHostException

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
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
        if (currentState.username.isBlank() || currentState.password.isBlank()) {
            _uiState.update { it.copy(errorMessageResId = R.string.msg_empty_credentials_error) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessageResId = null) }

            try {
                val result = authRepository.login(currentState.username, currentState.password)

                result.fold(
                    onSuccess = {
                        _uiState.update { it.copy(isLoading = false, isLoggedIn = true) }
                    },
                    onFailure = { throwable ->
                        val resId = when (throwable) {
                            is UnknownHostException, is IllegalStateException -> R.string.msg_login_failed_error
                            else -> R.string.msg_invalid_credentials_error
                        }
                        _uiState.update { it.copy(isLoading = false, errorMessageResId = resId) }
                    }
                )
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessageResId = R.string.msg_login_failed_error
                    )
                }
            }
        }
    }
}