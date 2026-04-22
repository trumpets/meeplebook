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
 * Unit tests for [ObserveFullSyncStateUseCase].
 */
class ObserveFullSyncStateUseCaseTest {

    private lateinit var fakeSyncTimeRepository: FakeSyncTimeRepository
    private lateinit var useCase: ObserveFullSyncStateUseCase

    @Before
    fun setUp() {
        fakeSyncTimeRepository = FakeSyncTimeRepository()
        useCase = ObserveFullSyncStateUseCase(fakeSyncTimeRepository)
    }

    @Test
    fun `invoke reports syncing when either domain is syncing`() = runTest {
        fakeSyncTimeRepository.markStarted(SyncType.COLLECTION)

        val result = useCase().first()

        assertEquals(
            SyncState(
                isSyncing = true,
                lastSyncedAt = null,
                errorMessage = null
            ),
            result
        )
    }

    @Test
    fun `invoke reports older last synced time when both domains completed`() = runTest {
        fakeSyncTimeRepository.markCompleted(
            SyncType.COLLECTION,
            Instant.parse("2024-01-15T14:00:00Z")
        )
        fakeSyncTimeRepository.markCompleted(
            SyncType.PLAYS,
            Instant.parse("2024-01-15T12:00:00Z")
        )

        val result = useCase().first()

        assertEquals(
            SyncState(
                isSyncing = false,
                lastSyncedAt = Instant.parse("2024-01-15T12:00:00Z"),
                errorMessage = null
            ),
            result
        )
    }

    @Test
    fun `invoke prioritizes collection error when both domains failed`() = runTest {
        fakeSyncTimeRepository.markFailed(SyncType.COLLECTION, "CollectionError")
        fakeSyncTimeRepository.markFailed(SyncType.PLAYS, "PlaysError")

        val result = useCase().first()

        assertEquals(
            SyncState(
                isSyncing = false,
                lastSyncedAt = null,
                errorMessage = "CollectionError"
            ),
            result
        )
    }
}
