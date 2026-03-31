package app.meeplebook.feature.profile

/**
 * One-time UI effects emitted by ProfileViewModel and consumed by ProfileScreen.
 */
sealed interface ProfileUiEffect {
    data object NavigateToLogin : ProfileUiEffect
    data object OpenSourceLicenses : ProfileUiEffect
}
