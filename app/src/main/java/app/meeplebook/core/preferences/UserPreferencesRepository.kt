package app.meeplebook.core.preferences

import app.meeplebook.core.collection.model.CollectionViewMode
import kotlinx.coroutines.flow.Flow

interface UserPreferencesRepository {
    fun getPreferences(): Flow<UserPreferences>
    suspend fun setStartingScreen(screen: StartingScreen)
    suspend fun setCollectionViewMode(viewMode: CollectionViewMode)
    suspend fun setCollectionAlphabetJumpVisible(visible: Boolean)
}
