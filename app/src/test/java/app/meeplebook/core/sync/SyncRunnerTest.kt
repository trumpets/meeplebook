package app.meeplebook.core.sync

import app.meeplebook.core.result.AppResult
import app.meeplebook.core.sync.model.SyncType
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

/**
 * Unit tests for [SyncRunner].
 */
class SyncRunnerTest {

    private lateinit var fakeSyncTimeRepository: FakeSyncTimeRepository
    private lateinit var syncRunner: SyncRunner

    private val testClock = Clock.fixed(
        Instant.parse("2024-01-15T12:00:00Z"),
        ZoneOffset.UTC
    )

    @Before
    fun setUp() {
        fakeSyncTimeRepository = FakeSyncTimeRepository()
        syncRunner = SyncRunner(
            syncTimeRepository = fakeSyncTimeRepository,
            clock = testClock
        )
    }

    @Test
    fun `run marks started and completed for successful work`() = runTest {
        val result = syncRunner.run(
            type = SyncType.COLLECTION,
            block = { AppResult.Success(Unit) }
        )

        assertTrue(result is AppResult.Success)
        assertEquals(
            listOf(
                FakeSyncTimeRepository.Operation(
                    kind = FakeSyncTimeRepository.Operation.Kind.STARTED,
                    type = SyncType.COLLECTION
                ),
                FakeSyncTimeRepository.Operation(
                    kind = FakeSyncTimeRepository.Operation.Kind.COMPLETED,
                    type = SyncType.COLLECTION,
                    time = Instant.now(testClock)
                )
            ),
            fakeSyncTimeRepository.operations
        )
    }

    @Test
    fun `run marks failed when work returns failure`() = runTest {
        val result = syncRunner.run(
            type = SyncType.PLAYS,
            parseStorageError = { _: Int -> "boom" },
            block = { AppResult.Failure(42) }
        )

        assertTrue(result is AppResult.Failure)
        assertEquals(
            listOf(
                FakeSyncTimeRepository.Operation(
                    kind = FakeSyncTimeRepository.Operation.Kind.STARTED,
                    type = SyncType.PLAYS
                ),
                FakeSyncTimeRepository.Operation(
                    kind = FakeSyncTimeRepository.Operation.Kind.FAILED,
                    type = SyncType.PLAYS,
                    errorMessage = "boom"
                )
            ),
            fakeSyncTimeRepository.operations
        )
    }

    @Test
    fun `run marks failed and rethrows when work throws`() = runTest {
        val throwable = IOException("network down")

        try {
            syncRunner.run<Unit, Unit>(
                type = SyncType.COLLECTION,
                block = { throw throwable }
            )
        } catch (actual: IOException) {
            assertEquals(throwable, actual)
        }

        assertEquals(
            listOf(
                FakeSyncTimeRepository.Operation(
                    kind = FakeSyncTimeRepository.Operation.Kind.STARTED,
                    type = SyncType.COLLECTION
                ),
                FakeSyncTimeRepository.Operation(
                    kind = FakeSyncTimeRepository.Operation.Kind.FAILED,
                    type = SyncType.COLLECTION,
                    errorMessage = "network down"
                )
            ),
            fakeSyncTimeRepository.operations
        )
    }
}
