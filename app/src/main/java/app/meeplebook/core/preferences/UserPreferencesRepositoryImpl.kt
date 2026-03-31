package app.meeplebook.core.preferences

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import app.meeplebook.core.collection.model.CollectionViewMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class UserPreferencesRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : UserPreferencesRepository {

    private companion object {
        private const val TAG = "UserPreferencesRepo"
        val KEY_STARTING_SCREEN = stringPreferencesKey("pref_starting_screen")
        val KEY_COLLECTION_VIEW_MODE = stringPreferencesKey("pref_collection_view_mode")
        val KEY_COLLECTION_ALPHABET_JUMP_VISIBLE = booleanPreferencesKey("pref_collection_alphabet_jump_visible")
    }

    override fun getPreferences(): Flow<UserPreferences> =
        dataStore.data.map { prefs ->
            val startingScreen = prefs[KEY_STARTING_SCREEN]
                ?.let { raw ->
                    runCatching { StartingScreen.valueOf(raw) }
                        .onFailure { Log.w(TAG, "Unknown StartingScreen value '$raw', falling back to default") }
                        .getOrNull()
                }
                ?: StartingScreen.OVERVIEW

            val collectionViewMode = prefs[KEY_COLLECTION_VIEW_MODE]
                ?.let { raw ->
                    runCatching { CollectionViewMode.valueOf(raw) }
                        .onFailure { Log.w(TAG, "Unknown CollectionViewMode value '$raw', falling back to default") }
                        .getOrNull()
                }
                ?: CollectionViewMode.LIST

            val alphabetJumpVisible = prefs[KEY_COLLECTION_ALPHABET_JUMP_VISIBLE] ?: true

            UserPreferences(
                startingScreen = startingScreen,
                collectionViewMode = collectionViewMode,
                collectionAlphabetJumpVisible = alphabetJumpVisible
            )
        }

    override suspend fun setStartingScreen(screen: StartingScreen) {
        dataStore.edit { prefs ->
            prefs[KEY_STARTING_SCREEN] = screen.name
        }
    }

    override suspend fun setCollectionViewMode(viewMode: CollectionViewMode) {
        dataStore.edit { prefs ->
            prefs[KEY_COLLECTION_VIEW_MODE] = viewMode.name
        }
    }

    override suspend fun setCollectionAlphabetJumpVisible(visible: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_COLLECTION_ALPHABET_JUMP_VISIBLE] = visible
        }
    }
}
