package app.meeplebook.core.sync.domain

import app.meeplebook.core.sync.FakeSyncTimeRepository
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
    fun `invoke returns last full sync time when available`() = runTest {
        // Given
        val syncTime = Instant.parse("2024-01-15T12:00:00Z")
        fakeSyncTimeRepository.updateFullSyncTime(syncTime)

        // When
        val result = useCase().first()

        // Then
        assertEquals(syncTime, result)
    }

    @Test
    fun `invoke returns null when no sync has occurred`() = runTest {
        // Given - no sync time set

        // When
        val result = useCase().first()

        // Then
        assertNull(result)
    }

    @Test
    fun `invoke updates when sync time changes`() = runTest {
        // Given - initial sync time
        val initialTime = Instant.parse("2024-01-15T12:00:00Z")
        fakeSyncTimeRepository.updateFullSyncTime(initialTime)

        // When - first observation
        val result1 = useCase().first()

        // Then
        assertEquals(initialTime, result1)

        // When - sync time is updated
        val updatedTime = Instant.parse("2024-01-15T14:00:00Z")
        fakeSyncTimeRepository.updateFullSyncTime(updatedTime)
        val result2 = useCase().first()

        // Then - new time is observed
        assertEquals(updatedTime, result2)
    }

    @Test
    fun `invoke returns null after sync times are cleared`() = runTest {
        // Given - sync time exists
        val syncTime = Instant.parse("2024-01-15T12:00:00Z")
        fakeSyncTimeRepository.updateFullSyncTime(syncTime)

        // When - sync times are cleared
        fakeSyncTimeRepository.clearSyncTimes()
        val result = useCase().first()

        // Then
        assertNull(result)
    }
}
