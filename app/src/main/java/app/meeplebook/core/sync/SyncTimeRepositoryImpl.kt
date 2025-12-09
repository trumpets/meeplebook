package app.meeplebook.core.sync

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncTimeRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : SyncTimeRepository {
    
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    
    companion object {
        private val LAST_COLLECTION_SYNC = stringPreferencesKey("last_collection_sync")
        private val LAST_PLAYS_SYNC = stringPreferencesKey("last_plays_sync")
        private val LAST_FULL_SYNC = stringPreferencesKey("last_full_sync")
    }
    
    override fun observeLastCollectionSync(): Flow<LocalDateTime?> =
        dataStore.data.map { prefs ->
            prefs[LAST_COLLECTION_SYNC]?.let { LocalDateTime.parse(it, formatter) }
        }
    
    override fun observeLastPlaysSync(): Flow<LocalDateTime?> =
        dataStore.data.map { prefs ->
            prefs[LAST_PLAYS_SYNC]?.let { LocalDateTime.parse(it, formatter) }
        }
    
    override fun observeLastFullSync(): Flow<LocalDateTime?> =
        dataStore.data.map { prefs ->
            prefs[LAST_FULL_SYNC]?.let { LocalDateTime.parse(it, formatter) }
        }
    
    override suspend fun updateCollectionSyncTime(time: LocalDateTime) {
        dataStore.edit { prefs ->
            prefs[LAST_COLLECTION_SYNC] = time.format(formatter)
        }
    }
    
    override suspend fun updatePlaysSyncTime(time: LocalDateTime) {
        dataStore.edit { prefs ->
            prefs[LAST_PLAYS_SYNC] = time.format(formatter)
        }
    }
    
    override suspend fun updateFullSyncTime(time: LocalDateTime) {
        dataStore.edit { prefs ->
            prefs[LAST_FULL_SYNC] = time.format(formatter)
        }
    }
    
    override suspend fun clearSyncTimes() {
        dataStore.edit { prefs ->
            prefs.remove(LAST_COLLECTION_SYNC)
            prefs.remove(LAST_PLAYS_SYNC)
            prefs.remove(LAST_FULL_SYNC)
        }
    }
}
