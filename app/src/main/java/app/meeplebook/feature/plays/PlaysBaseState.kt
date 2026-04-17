package app.meeplebook.feature.plays

/**
 * Reducer-owned internal state for the Plays screen.
 *
 * This is the single source of truth for synchronous UI-visible inputs that are mutated directly
 * in response to [PlaysEvent]s. The display-facing [PlaysUiState] is derived later by combining
 * this base state with observed plays data from the domain layer.
 *
 * @property searchQuery Current raw search query entered by the user.
 * @property isRefreshing Whether a refresh/sync operation is currently in progress.
 */
data class PlaysBaseState(
    val searchQuery: String = "",
    val isRefreshing: Boolean = false,
)
