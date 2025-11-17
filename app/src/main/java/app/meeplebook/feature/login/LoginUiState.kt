package app.meeplebook.feature.login

import androidx.annotation.StringRes

data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    @StringRes val errorMessageResId: Int? = null,
    val isLoggedIn: Boolean = false
)