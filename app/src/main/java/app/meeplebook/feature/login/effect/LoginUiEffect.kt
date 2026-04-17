package app.meeplebook.feature.login.effect

/**
 * One-shot UI effects emitted by the Login feature.
 */
sealed interface LoginUiEffect {
    /**
     * Signals that login succeeded and the screen should navigate away.
     */
    data object LoginSucceeded : LoginUiEffect
}
