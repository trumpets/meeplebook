package app.meeplebook.feature.overview

import app.meeplebook.core.ui.UiText

/**
 * Reducer-owned base state for Overview.
 *
 * This keeps only transient UI bookkeeping that is not part of the observed domain overview data:
 * whether a manual refresh is in flight and whether the screen should currently surface a
 * full-screen error.
 */
data class OverviewBaseState(
    val errorMessageUiText: UiText? = null
)
