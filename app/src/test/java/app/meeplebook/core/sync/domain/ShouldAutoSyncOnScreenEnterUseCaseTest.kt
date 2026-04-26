package app.meeplebook.core.sync.domain

import app.meeplebook.core.sync.FakeSyncTimeRepository
import app.meeplebook.core.sync.model.SyncType
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class ShouldAutoSyncOnScreenEnterUseCaseTest {

    private lateinit var fakeSyncTimeRepository: FakeSyncTimeRepository
    private lateinit var useCase: ShouldAutoSyncOnScreenEnterUseCase

    private val testClock = Clock.fixed(
        Instant.parse("2024-01-15T12:00:00Z"),
        ZoneOffset.UTC
    )

    @Before
    fun setUp() {
        fakeSyncTimeRepository = FakeSyncTimeRepository()
        useCase = ShouldAutoSyncOnScreenEnterUseCase(fakeSyncTimeRepository, testClock)
    }

    @Test
    fun `invoke returns true when domain never synced`() = runTest {
        assertTrue(useCase(SyncType.COLLECTION))
    }

    @Test
    fun `invoke returns false when domain is already syncing`() = runTest {
        fakeSyncTimeRepository.markStarted(SyncType.COLLECTION)

        assertFalse(useCase(SyncType.COLLECTION))
    }

    @Test
    fun `invoke returns false when last sync is within min interval`() = runTest {
        fakeSyncTimeRepository.markCompleted(
            SyncType.COLLECTION,
            testClock.instant().minusSeconds(ShouldAutoSyncOnScreenEnterUseCase.AUTO_SYNC_MIN_INTERVAL.seconds - 60)
        )

        assertFalse(useCase(SyncType.COLLECTION))
    }

    @Test
    fun `invoke returns true when last sync is older than min interval`() = runTest {
        fakeSyncTimeRepository.markCompleted(
            SyncType.COLLECTION,
            testClock.instant().minusSeconds(ShouldAutoSyncOnScreenEnterUseCase.AUTO_SYNC_MIN_INTERVAL.seconds + 60)
        )

        assertTrue(useCase(SyncType.COLLECTION))
    }

    @Test
    fun `invoke with multiple types returns false when all domains are recent`() = runTest {
        val recentSync = testClock.instant().minusSeconds(5 * 60)
        fakeSyncTimeRepository.markCompleted(SyncType.COLLECTION, recentSync)
        fakeSyncTimeRepository.markCompleted(SyncType.PLAYS, recentSync)

        assertFalse(useCase(SyncType.COLLECTION, SyncType.PLAYS))
    }

    @Test
    fun `invoke with multiple types returns true when one domain is stale`() = runTest {
        fakeSyncTimeRepository.markCompleted(
            SyncType.COLLECTION,
            testClock.instant().minusSeconds(5 * 60)
        )
        fakeSyncTimeRepository.markCompleted(
            SyncType.PLAYS,
            testClock.instant().minusSeconds(ShouldAutoSyncOnScreenEnterUseCase.AUTO_SYNC_MIN_INTERVAL.seconds + 60)
        )

        assertTrue(useCase(SyncType.COLLECTION, SyncType.PLAYS))
    }

    @Test
    fun `invoke with multiple types returns false when one domain is syncing`() = runTest {
        fakeSyncTimeRepository.markCompleted(
            SyncType.COLLECTION,
            testClock.instant().minusSeconds(ShouldAutoSyncOnScreenEnterUseCase.AUTO_SYNC_MIN_INTERVAL.seconds + 60)
        )
        fakeSyncTimeRepository.markStarted(SyncType.PLAYS)

        assertFalse(useCase(SyncType.COLLECTION, SyncType.PLAYS))
    }
}
