package app.meeplebook.core.sync.domain

import app.meeplebook.core.sync.FakeSyncTimeRepository
import app.meeplebook.core.sync.model.SyncState
import app.meeplebook.core.sync.model.SyncType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.Instant

/**
 * Unit tests for [ObserveSyncStateUseCase].
 */
class ObserveSyncStateUseCaseTest {

    private lateinit var fakeSyncTimeRepository: FakeSyncTimeRepository
    private lateinit var useCase: ObserveSyncStateUseCase

    @Before
    fun setUp() {
        fakeSyncTimeRepository = FakeSyncTimeRepository()
        useCase = ObserveSyncStateUseCase(fakeSyncTimeRepository)
    }

    @Test
    fun `invoke returns current sync state for requested type`() = runTest {
        val syncTime = Instant.parse("2024-01-15T12:00:00Z")
        fakeSyncTimeRepository.markCompleted(SyncType.PLAYS, syncTime)

        val result = useCase(SyncType.PLAYS).first()

        assertEquals(
            SyncState(
                isSyncing = false,
                lastSyncedAt = syncTime,
                errorMessage = null
            ),
            result
        )
    }

    @Test
    fun `invoke isolates state per sync type`() = runTest {
        fakeSyncTimeRepository.markFailed(SyncType.COLLECTION, "NetworkError")

        val result = useCase(SyncType.PLAYS).first()

        assertEquals(SyncState(), result)
    }
}
