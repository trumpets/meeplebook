package app.meeplebook.core.database.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.meeplebook.core.database.MeepleBookDatabase
import app.meeplebook.core.database.entity.SyncStateEntity
import app.meeplebook.core.sync.model.SyncType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

/**
 * Room DAO tests for [SyncDao].
 */
@RunWith(AndroidJUnit4::class)
class SyncDaoTest {

    private lateinit var database: MeepleBookDatabase
    private lateinit var syncDao: SyncDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            MeepleBookDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()

        syncDao = database.syncDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun observeSyncState_emitsPersistedState() = runTest {
        val state = SyncStateEntity(
            type = SyncType.COLLECTION,
            isSyncing = true,
            lastSyncedAt = Instant.parse("2024-01-15T12:00:00Z"),
            errorMessage = "NetworkError"
        )

        syncDao.upsertSyncState(state)

        assertEquals(state, syncDao.observeSyncState(SyncType.COLLECTION).first())
    }

    @Test
    fun markStarted_preservesLastSyncedAtAndClearsError() = runTest {
        val lastSyncedAt = Instant.parse("2024-01-15T12:00:00Z")
        syncDao.upsertSyncState(
            SyncStateEntity(
                type = SyncType.PLAYS,
                isSyncing = false,
                lastSyncedAt = lastSyncedAt,
                errorMessage = "Old"
            )
        )

        syncDao.markStarted(SyncType.PLAYS)

        assertEquals(
            SyncStateEntity(
                type = SyncType.PLAYS,
                isSyncing = true,
                lastSyncedAt = lastSyncedAt,
                errorMessage = null
            ),
            syncDao.getSyncState(SyncType.PLAYS)
        )
    }

    @Test
    fun markFailed_preservesLastSyncedAt() = runTest {
        val lastSyncedAt = Instant.parse("2024-01-15T12:00:00Z")
        syncDao.upsertSyncState(
            SyncStateEntity(
                type = SyncType.COLLECTION,
                lastSyncedAt = lastSyncedAt
            )
        )

        syncDao.markFailed(SyncType.COLLECTION, "NetworkError")

        assertEquals(
            SyncStateEntity(
                type = SyncType.COLLECTION,
                isSyncing = false,
                lastSyncedAt = lastSyncedAt,
                errorMessage = "NetworkError"
            ),
            syncDao.getSyncState(SyncType.COLLECTION)
        )
    }

    @Test
    fun markCompleted_updatesExistingRow() = runTest {
        syncDao.markStarted(SyncType.PLAYS)
        val completedAt = Instant.parse("2024-01-15T13:00:00Z")

        syncDao.markCompleted(SyncType.PLAYS, completedAt)

        assertEquals(
            SyncStateEntity(
                type = SyncType.PLAYS,
                isSyncing = false,
                lastSyncedAt = completedAt,
                errorMessage = null
            ),
            syncDao.getSyncState(SyncType.PLAYS)
        )
    }

    @Test
    fun markIdle_preservesLastSyncedAtAndClearsError() = runTest {
        val lastSyncedAt = Instant.parse("2024-01-15T12:00:00Z")
        syncDao.upsertSyncState(
            SyncStateEntity(
                type = SyncType.COLLECTION,
                isSyncing = true,
                lastSyncedAt = lastSyncedAt,
                errorMessage = "OldError"
            )
        )

        syncDao.markIdle(SyncType.COLLECTION)

        assertEquals(
            SyncStateEntity(
                type = SyncType.COLLECTION,
                isSyncing = false,
                lastSyncedAt = lastSyncedAt,
                errorMessage = null
            ),
            syncDao.getSyncState(SyncType.COLLECTION)
        )
    }

    @Test
    fun markIdle_insertsIdleRowWhenStateDoesNotExist() = runTest {
        syncDao.markIdle(SyncType.PLAYS)

        assertEquals(
            SyncStateEntity(
                type = SyncType.PLAYS,
                isSyncing = false,
                lastSyncedAt = null,
                errorMessage = null
            ),
            syncDao.getSyncState(SyncType.PLAYS)
        )
    }
}
