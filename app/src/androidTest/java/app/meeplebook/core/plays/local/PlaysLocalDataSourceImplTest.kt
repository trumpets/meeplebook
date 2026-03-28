package app.meeplebook.core.plays.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.meeplebook.core.database.MeepleBookDatabase
import app.meeplebook.core.database.dao.PlayDao
import app.meeplebook.core.database.dao.PlayerDao
import app.meeplebook.core.database.entity.PlayEntity
import app.meeplebook.core.plays.model.PlaySyncStatus
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

/**
 * Integration tests for [PlaysLocalDataSourceImpl] using an in-memory Room database.
 * Focused on [PlaysLocalDataSourceImpl.retainByRemoteIds].
 */
@RunWith(AndroidJUnit4::class)
class PlaysLocalDataSourceImplTest {

    private lateinit var database: MeepleBookDatabase
    private lateinit var playDao: PlayDao
    private lateinit var playerDao: PlayerDao
    private lateinit var dataSource: PlaysLocalDataSourceImpl

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            MeepleBookDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()

        playDao = database.playDao()
        playerDao = database.playerDao()
        dataSource = PlaysLocalDataSourceImpl(database, playDao, playerDao)
    }

    @After
    fun tearDown() {
        database.close()
    }

    // --- retainByRemoteIds ---

    @Test
    fun retainByRemoteIds_keepsPlaysInList_deletesOthers() = runTest {
        val plays = listOf(
            makeRemotePlay(localId = 1, remoteId = 1001),
            makeRemotePlay(localId = 2, remoteId = 1002),
            makeRemotePlay(localId = 3, remoteId = 1003),
        )
        playDao.insertAll(plays)

        dataSource.retainByRemoteIds(listOf(1001, 1003))

        val remaining = playDao.getPlays()
        assertEquals(2, remaining.size)
        assertEquals(setOf(1001L, 1003L), remaining.map { it.remoteId }.toSet())
    }

    @Test
    fun retainByRemoteIds_emptyList_deletesAllRemotePlays() = runTest {
        val plays = listOf(
            makeRemotePlay(localId = 1, remoteId = 1001),
            makeRemotePlay(localId = 2, remoteId = 1002),
        )
        playDao.insertAll(plays)

        dataSource.retainByRemoteIds(emptyList())

        val remaining = playDao.getPlays()
        assertEquals(0, remaining.size)
    }

    @Test
    fun retainByRemoteIds_neverDeletesLocalOnlyPlays() = runTest {
        val plays = listOf(
            makeRemotePlay(localId = 1, remoteId = 1001),
            makeLocalPlay(localId = 2),
        )
        playDao.insertAll(plays)

        dataSource.retainByRemoteIds(emptyList())

        val remaining = playDao.getPlays()
        assertEquals(1, remaining.size)
        assertNull(remaining[0].remoteId)
    }

    @Test
    fun retainByRemoteIds_allRetained_nothingDeleted() = runTest {
        val plays = listOf(
            makeRemotePlay(localId = 1, remoteId = 1001),
            makeRemotePlay(localId = 2, remoteId = 1002),
        )
        playDao.insertAll(plays)

        dataSource.retainByRemoteIds(listOf(1001, 1002))

        assertEquals(2, playDao.getPlays().size)
    }

    @Test
    fun retainByRemoteIds_largeListExceedingSqliteLimit_correctlyDeletesMissing() = runTest {
        // Insert SYNC_CHUNK_SIZE*2 + 1 = 1001 remote plays so the toDelete list
        // spans more than two 500-item chunks.
        val count = 1001
        val keepId = 999_001L
        val plays = (1..count).map { i ->
            makeRemotePlay(localId = i.toLong(), remoteId = (i + 999_000).toLong())
        }
        playDao.insertAll(plays)

        // Retain only one play — toDelete will be 1000 items (needs 2 chunks of 500)
        dataSource.retainByRemoteIds(listOf(keepId))

        val remaining = playDao.getPlays()
        assertEquals(1, remaining.size)
        assertEquals(keepId, remaining[0].remoteId)
    }

    // --- Helpers ---

    private fun makeRemotePlay(localId: Long, remoteId: Long): PlayEntity =
        PlayEntity(
            localId = localId,
            remoteId = remoteId,
            date = Instant.parse("2024-06-01T12:00:00Z"),
            quantity = 1,
            length = null,
            incomplete = false,
            location = null,
            gameId = 1L,
            gameName = "Test Game",
            comments = null,
            syncStatus = PlaySyncStatus.SYNCED,
        )

    private fun makeLocalPlay(localId: Long): PlayEntity =
        PlayEntity(
            localId = localId,
            remoteId = null,
            date = Instant.parse("2024-06-01T12:00:00Z"),
            quantity = 1,
            length = null,
            incomplete = false,
            location = null,
            gameId = 1L,
            gameName = "Local Game",
            comments = null,
            syncStatus = PlaySyncStatus.PENDING,
        )
}
