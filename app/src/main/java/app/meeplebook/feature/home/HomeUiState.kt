package app.meeplebook.feature.home

import androidx.annotation.StringRes
import app.meeplebook.core.collection.model.CollectionItem
import app.meeplebook.core.plays.model.Play

/**
 * UI state for the Home screen.
 */
data class HomeUiState(
    val isLoadingCollection: Boolean = false,
    val isLoadingPlays: Boolean = false,
    val collection: List<CollectionItem> = emptyList(),
    val recentPlays: List<Play> = emptyList(),
    val totalPlays: Int = 0,
    val playsCurrentPage: Int = 0,
    val hasMorePlays: Boolean = false,
    @StringRes val errorMessageResId: Int? = null,
    val username: String = ""
) {
    val isLoading: Boolean
        get() = isLoadingCollection || isLoadingPlays

    val hasData: Boolean
        get() = collection.isNotEmpty() || recentPlays.isNotEmpty()
}
