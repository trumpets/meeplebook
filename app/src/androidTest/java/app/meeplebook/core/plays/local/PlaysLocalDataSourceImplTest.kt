package app.meeplebook.core.plays.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.meeplebook.core.database.MeepleBookDatabase
import app.meeplebook.core.database.dao.PlayDao
import app.meeplebook.core.database.dao.PlayerDao
import app.meeplebook.core.database.entity.PlayEntity
import app.meeplebook.core.database.entity.PlayerEntity
import app.meeplebook.core.plays.model.PlaySyncStatus
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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

    @Test
    fun getPendingOrFailedPlays_returnsOnlyRetryablePlays() = runTest {
        playDao.insertAll(
            listOf(
                makePlay(localId = 1, remoteId = null, syncStatus = PlaySyncStatus.PENDING),
                makePlay(localId = 2, remoteId = 2002, syncStatus = PlaySyncStatus.FAILED),
                makePlay(localId = 3, remoteId = 3003, syncStatus = PlaySyncStatus.SYNCED)
            )
        )
        playerDao.insertAll(
            listOf(
                makePlayer(playId = 1, name = "Pending Player"),
                makePlayer(playId = 2, name = "Failed Player"),
                makePlayer(playId = 3, name = "Synced Player")
            )
        )

        val result = dataSource.getPendingOrFailedPlays()

        assertEquals(listOf(1L, 2L), result.map { it.playId.localId })
        assertEquals(
            listOf(PlaySyncStatus.PENDING, PlaySyncStatus.FAILED),
            result.map { it.syncStatus }
        )
    }

    @Test
    fun markPlayAsSynced_updatesStatusAndRemoteId() = runTest {
        playDao.insert(makeLocalPlay(localId = 1))

        dataSource.markPlayAsSynced(localPlayId = 1, remotePlayId = 9001)

        val updated = playDao.getPlayById(1)!!
        assertEquals(PlaySyncStatus.SYNCED, updated.syncStatus)
        assertEquals(9001L, updated.remoteId)
    }

    @Test
    fun markPlayAsFailed_preservesRemoteIdAndMarksFailed() = runTest {
        playDao.insert(makePlay(localId = 1, remoteId = 8001, syncStatus = PlaySyncStatus.PENDING))

        dataSource.markPlayAsFailed(localPlayId = 1)

        val updated = playDao.getPlayById(1)!!
        assertEquals(PlaySyncStatus.FAILED, updated.syncStatus)
        assertEquals(8001L, updated.remoteId)
    }

    // --- Helpers ---

    private fun makeRemotePlay(localId: Long, remoteId: Long): PlayEntity =
        makePlay(localId = localId, remoteId = remoteId, syncStatus = PlaySyncStatus.SYNCED)

    private fun makeLocalPlay(localId: Long): PlayEntity =
        makePlay(localId = localId, remoteId = null, syncStatus = PlaySyncStatus.PENDING)

    private fun makePlay(
        localId: Long,
        remoteId: Long?,
        syncStatus: PlaySyncStatus
    ): PlayEntity =
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
            syncStatus = syncStatus,
        )

    private fun makePlayer(playId: Long, name: String): PlayerEntity =
        PlayerEntity(
            id = 0,
            playId = playId,
            username = null,
            userId = null,
            name = name,
            startPosition = null,
            color = null,
            score = null,
            win = false
        )
}
