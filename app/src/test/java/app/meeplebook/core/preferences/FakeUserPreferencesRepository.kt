package app.meeplebook.core.preferences

import app.meeplebook.core.collection.model.CollectionViewMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeUserPreferencesRepository : UserPreferencesRepository {

    private val _preferences = MutableStateFlow(UserPreferences())
    val preferences = _preferences.asStateFlow()

    override fun getPreferences(): Flow<UserPreferences> = _preferences

    override suspend fun setStartingScreen(screen: StartingScreen) {
        _preferences.value = _preferences.value.copy(startingScreen = screen)
    }

    override suspend fun setCollectionViewMode(viewMode: CollectionViewMode) {
        _preferences.value = _preferences.value.copy(collectionViewMode = viewMode)
    }

    override suspend fun setCollectionAlphabetJumpVisible(visible: Boolean) {
        _preferences.value = _preferences.value.copy(collectionAlphabetJumpVisible = visible)
    }

    fun setPreferences(prefs: UserPreferences) {
        _preferences.value = prefs
    }
}
