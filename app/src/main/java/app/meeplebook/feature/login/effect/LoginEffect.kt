package app.meeplebook.feature.login.effect

/**
 * Domain work triggered by Login events.
 */
sealed interface LoginEffect {
    /**
     * Attempts to authenticate with the supplied credentials.
     */
    data class Login(
        val username: String,
        val password: String
    ) : LoginEffect
}
