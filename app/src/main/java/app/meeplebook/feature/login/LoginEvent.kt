package app.meeplebook.feature.login

/**
 * User-driven events for the Login screen.
 */
sealed interface LoginEvent {
    /**
     * Updates the username field.
     */
    data class UsernameChanged(val username: String) : LoginEvent

    /**
     * Updates the password field.
     */
    data class PasswordChanged(val password: String) : LoginEvent

    /**
     * Attempts to submit the current credentials.
     */
    data object Submit : LoginEvent
}
