package app.meeplebook.core.database.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.meeplebook.core.database.MeepleBookDatabase
import app.meeplebook.core.database.entity.ActivePlayTimerEntity
import java.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Room DAO tests for [PlayTimerDao].
 */
@RunWith(AndroidJUnit4::class)
class PlayTimerDaoTest {

    private lateinit var database: MeepleBookDatabase
    private lateinit var playTimerDao: PlayTimerDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            MeepleBookDatabase::class.java,
        )
            .allowMainThreadQueries()
            .build()

        playTimerDao = database.playTimerDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun getTimerReturnsNullWhenNoRowExists() = runTest {
        assertNull(playTimerDao.getTimer())
    }

    @Test
    fun upsertAndReadTimer() = runTest {
        val timer = ActivePlayTimerEntity(
            playId = 77L,
            startedAt = Instant.parse("2026-03-01T10:00:00Z"),
            accumulatedMillis = 90_000L,
            isRunning = true,
            hasStarted = true,
        )

        playTimerDao.upsertTimer(timer)

        assertEquals(timer, playTimerDao.getTimer())
    }

    @Test
    fun observeTimerEmitsUpdatedSingletonRow() = runTest {
        val initial = ActivePlayTimerEntity(
            playId = null,
            startedAt = null,
            accumulatedMillis = 30_000L,
            isRunning = false,
            hasStarted = true,
        )
        val updated = initial.copy(
            playId = 12L,
            startedAt = Instant.parse("2026-03-01T11:00:00Z"),
            accumulatedMillis = 0L,
            isRunning = true,
        )

        playTimerDao.upsertTimer(initial)
        playTimerDao.upsertTimer(updated)

        assertEquals(updated, playTimerDao.observeTimer().first())
    }
}
