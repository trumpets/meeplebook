package app.meeplebook.core.sync.domain

import app.meeplebook.core.sync.FakeSyncTimeRepository
import app.meeplebook.core.sync.model.SyncType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.time.Instant

/**
 * Unit tests for [ObserveLastFullSyncUseCase].
 */
class ObserveLastFullSyncUseCaseTest {

    private lateinit var fakeSyncTimeRepository: FakeSyncTimeRepository
    private lateinit var useCase: ObserveLastFullSyncUseCase

    @Before
    fun setUp() {
        fakeSyncTimeRepository = FakeSyncTimeRepository()
        useCase = ObserveLastFullSyncUseCase(fakeSyncTimeRepository)
    }

    @Test
    fun `invoke returns last full sync time when both syncs have completed`() = runTest {
        val syncTime = Instant.parse("2024-01-15T12:00:00Z")
        fakeSyncTimeRepository.markCompleted(SyncType.COLLECTION, syncTime)
        fakeSyncTimeRepository.markCompleted(SyncType.PLAYS, syncTime)

        val result = useCase().first()

        assertEquals(syncTime, result)
    }

    @Test
    fun `invoke returns null when only one sync has completed`() = runTest {
        fakeSyncTimeRepository.markCompleted(
            SyncType.COLLECTION,
            Instant.parse("2024-01-15T12:00:00Z")
        )

        val result = useCase().first()

        assertNull(result)
    }

    @Test
    fun `invoke returns the older domain sync time when they differ`() = runTest {
        fakeSyncTimeRepository.markCompleted(
            SyncType.COLLECTION,
            Instant.parse("2024-01-15T14:00:00Z")
        )
        fakeSyncTimeRepository.markCompleted(
            SyncType.PLAYS,
            Instant.parse("2024-01-15T12:00:00Z")
        )

        val result = useCase().first()

        assertEquals(Instant.parse("2024-01-15T12:00:00Z"), result)
    }

    @Test
    fun `invoke returns null after sync times are cleared`() = runTest {
        val syncTime = Instant.parse("2024-01-15T12:00:00Z")
        fakeSyncTimeRepository.markCompleted(SyncType.COLLECTION, syncTime)
        fakeSyncTimeRepository.markCompleted(SyncType.PLAYS, syncTime)

        fakeSyncTimeRepository.clearSyncTimes()
        val result = useCase().first()

        assertNull(result)
    }
}
