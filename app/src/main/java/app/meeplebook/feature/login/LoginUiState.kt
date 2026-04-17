package app.meeplebook.feature.login

import app.meeplebook.core.ui.UiText
import app.meeplebook.core.ui.uiTextEmpty

/**
 * Reducer-owned UI state for the Login screen.
 */
data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: UiText = uiTextEmpty()
)
