package app.meeplebook.core.sync.domain

import app.meeplebook.core.auth.FakeAuthRepository
import app.meeplebook.core.model.AuthCredentials
import app.meeplebook.core.plays.FakePlaysRepository
import app.meeplebook.core.plays.model.PlayError
import app.meeplebook.core.result.AppResult
import app.meeplebook.core.sync.FakeSyncTimeRepository
import app.meeplebook.core.sync.SyncRunner
import app.meeplebook.core.sync.model.SyncState
import app.meeplebook.core.sync.model.SyncType
import app.meeplebook.core.sync.model.SyncUserDataError
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

/**
 * Unit tests for [SyncPlaysUseCase].
 */
class SyncPlaysUseCaseTest {

    private lateinit var fakeAuthRepository: FakeAuthRepository
    private lateinit var fakePlaysRepository: FakePlaysRepository
    private lateinit var fakeSyncTimeRepository: FakeSyncTimeRepository
    private lateinit var useCase: SyncPlaysUseCase

    private val testClock = Clock.fixed(
        Instant.parse("2024-01-15T12:00:00Z"),
        ZoneOffset.UTC
    )

    @Before
    fun setUp() {
        fakeAuthRepository = FakeAuthRepository()
        fakePlaysRepository = FakePlaysRepository()
        fakeSyncTimeRepository = FakeSyncTimeRepository()
        useCase = SyncPlaysUseCase(
            authRepository = fakeAuthRepository,
            playsRepository = fakePlaysRepository,
            syncRunner = SyncRunner(
                syncTimeRepository = fakeSyncTimeRepository,
                clock = testClock
            )
        )
    }

    @Test
    fun `invoke succeeds when plays sync successfully`() = runTest {
        val user = AuthCredentials(
            username = "testuser",
            password = "password"
        )
        fakeAuthRepository.setCurrentUser(user)
        fakePlaysRepository.syncPlaysResult = AppResult.Success(Unit)

        val result = useCase()

        assertTrue(result is AppResult.Success)
        assertEquals(1, fakePlaysRepository.syncCallCount)
        assertEquals("testuser", fakePlaysRepository.lastSyncUsername)
        assertEquals(Instant.now(testClock), fakeSyncTimeRepository.getLastPlaysSync())
        assertEquals(
            SyncState(
                isSyncing = false,
                lastSyncedAt = Instant.now(testClock),
                errorMessage = null
            ),
            fakeSyncTimeRepository.getSyncState(SyncType.PLAYS)
        )
    }

    @Test
    fun `invoke returns NotLoggedIn error when no user is logged in`() = runTest {
        val result = useCase()

        assertTrue(result is AppResult.Failure)
        assertEquals(SyncUserDataError.NotLoggedIn, (result as AppResult.Failure).error)
        assertEquals(0, fakePlaysRepository.syncCallCount)
        assertNull(fakeSyncTimeRepository.getLastPlaysSync())
        assertEquals(SyncState(), fakeSyncTimeRepository.getSyncState(SyncType.PLAYS))
    }

    @Test
    fun `invoke returns PlaysSyncFailed when plays sync fails`() = runTest {
        val user = AuthCredentials(
            username = "testuser",
            password = "password"
        )
        fakeAuthRepository.setCurrentUser(user)

        val playError = PlayError.NetworkError
        fakePlaysRepository.syncPlaysResult = AppResult.Failure(playError)

        val result = useCase()

        assertTrue(result is AppResult.Failure)
        val error = (result as AppResult.Failure).error
        assertEquals(SyncUserDataError.PlaysSyncFailed(playError), error)
        assertNull(fakeSyncTimeRepository.getLastPlaysSync())
        assertEquals(
            SyncState(
                isSyncing = false,
                lastSyncedAt = null,
                errorMessage = "NetworkError"
            ),
            fakeSyncTimeRepository.getSyncState(SyncType.PLAYS)
        )
    }
}
