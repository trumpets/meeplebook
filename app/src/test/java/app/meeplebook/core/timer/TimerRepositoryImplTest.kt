package app.meeplebook.core.timer

import app.meeplebook.core.database.dao.PlayTimerDao
import app.meeplebook.core.database.entity.ActivePlayTimerEntity
import app.meeplebook.core.database.entity.toModel
import app.meeplebook.core.timer.model.ActivePlayTimer
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TimerRepositoryImplTest {

    private lateinit var fakePlayTimerDao: FakePlayTimerDao
    private lateinit var clock: MutableClock
    private lateinit var repository: TimerRepositoryImpl

    @Before
    fun setUp() {
        fakePlayTimerDao = FakePlayTimerDao()
        clock = MutableClock(Instant.parse("2026-03-01T10:00:00Z"))
        repository = TimerRepositoryImpl(fakePlayTimerDao, clock)
    }

    @Test
    fun `observe returns default timer when no row exists`() = runTest {
        assertEquals(ActivePlayTimer(), repository.observe().first())
    }

    @Test
    fun `get returns default timer when no row exists`() = runTest {
        assertEquals(ActivePlayTimer(), repository.get())
    }

    @Test
    fun `start persists running timer row`() = runTest {
        repository.start(playId = 42L)

        val persisted = fakePlayTimerDao.currentEntity
        requireNotNull(persisted)

        assertEquals(42L, persisted.playId)
        assertEquals(clock.instant(), persisted.startedAt)
        assertEquals(0L, persisted.accumulatedMillis)
        assertTrue(persisted.isRunning)
        assertTrue(persisted.hasStarted)
    }

    @Test
    fun `pause persists accumulated elapsed time`() = runTest {
        repository.start(playId = 7L)
        clock.currentInstant = clock.instant().plusSeconds(185)

        repository.pause()

        val persisted = repository.get()
        assertEquals(7L, persisted.playId)
        assertEquals(Duration.ofSeconds(185), persisted.accumulated)
        assertEquals(false, persisted.isRunning)
        assertNull(persisted.startedAt)
    }

    @Test
    fun `resume restarts paused timer from current instant`() = runTest {
        fakePlayTimerDao.currentEntity = ActivePlayTimerEntity(
            playId = 7L,
            startedAt = null,
            accumulatedMillis = Duration.ofMinutes(8).toMillis(),
            isRunning = false,
            hasStarted = true,
        )
        fakePlayTimerDao.emitCurrent()
        clock.currentInstant = clock.instant().plusSeconds(75)

        repository.resume()

        val persisted = repository.get()
        assertEquals(7L, persisted.playId)
        assertEquals(clock.instant(), persisted.startedAt)
        assertEquals(Duration.ofMinutes(8), persisted.accumulated)
        assertTrue(persisted.isRunning)
    }

    @Test
    fun `reset clears accumulated time and starts immediately`() = runTest {
        fakePlayTimerDao.currentEntity = ActivePlayTimerEntity(
            playId = 15L,
            startedAt = null,
            accumulatedMillis = Duration.ofMinutes(14).toMillis(),
            isRunning = false,
            hasStarted = true,
        )
        fakePlayTimerDao.emitCurrent()

        repository.reset()

        val persisted = repository.get()
        assertEquals(15L, persisted.playId)
        assertEquals(Duration.ZERO, persisted.accumulated)
        assertEquals(clock.instant(), persisted.startedAt)
        assertTrue(persisted.isRunning)
    }

    @Test
    fun `pause before start does not write a row`() = runTest {
        repository.pause()

        assertNull(fakePlayTimerDao.currentEntity)
    }

    @Test
    fun `observe maps persisted row to model`() = runTest {
        val entity = ActivePlayTimerEntity(
            playId = 3L,
            startedAt = Instant.parse("2026-03-01T10:00:00Z"),
            accumulatedMillis = 123_000L,
            isRunning = true,
            hasStarted = true,
        )
        fakePlayTimerDao.currentEntity = entity
        fakePlayTimerDao.emitCurrent()

        assertEquals(entity.toModel(), repository.observe().first())
    }

    private class FakePlayTimerDao : PlayTimerDao {
        val flow = MutableStateFlow<ActivePlayTimerEntity?>(null)
        var currentEntity: ActivePlayTimerEntity? = null

        override fun observeTimer(): Flow<ActivePlayTimerEntity?> = flow

        override suspend fun getTimer(): ActivePlayTimerEntity? = currentEntity

        override suspend fun upsertTimer(timer: ActivePlayTimerEntity) {
            currentEntity = timer
            emitCurrent()
        }

        fun emitCurrent() {
            flow.value = currentEntity
        }
    }

    private class MutableClock(
        var currentInstant: Instant,
    ) : Clock() {
        override fun instant(): Instant = currentInstant

        override fun getZone(): ZoneId = ZoneOffset.UTC

        override fun withZone(zone: ZoneId): Clock = this
    }
}
