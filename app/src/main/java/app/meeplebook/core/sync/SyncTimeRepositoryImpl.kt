package app.meeplebook.core.sync

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class SyncTimeRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : SyncTimeRepository {

    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    private val LAST_COLLECTION_SYNC = longPreferencesKey("last_collection_sync")
    private val LAST_PLAYS_SYNC = longPreferencesKey("last_plays_sync")
    private val LAST_FULL_SYNC = longPreferencesKey("last_full_sync")

    override fun observeLastCollectionSync(): Flow<Instant?> =
        dataStore.data.map { prefs ->
            prefs[LAST_COLLECTION_SYNC]?.let { Instant.ofEpochMilli(it) }
        }

    override fun observeLastPlaysSync(): Flow<Instant?> =
        dataStore.data.map { prefs ->
            prefs[LAST_PLAYS_SYNC]?.let { Instant.ofEpochMilli(it) }
        }

    override fun observeLastFullSync(): Flow<Instant?> =
        dataStore.data.map { prefs ->
            prefs[LAST_FULL_SYNC]?.let { Instant.ofEpochMilli(it) }
        }

    override suspend fun updateCollectionSyncTime(time: Instant) {
        dataStore.edit { prefs ->
            prefs[LAST_COLLECTION_SYNC] = time.toEpochMilli()
        }
    }

    override suspend fun updatePlaysSyncTime(time: Instant) {
        dataStore.edit { prefs ->
            prefs[LAST_PLAYS_SYNC] = time.toEpochMilli()
        }
    }

    override suspend fun updateFullSyncTime(time: Instant) {
        dataStore.edit { prefs ->
            prefs[LAST_FULL_SYNC] = time.toEpochMilli()
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