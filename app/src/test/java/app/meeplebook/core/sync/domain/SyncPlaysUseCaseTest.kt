package app.meeplebook.core.sync.domain

import app.meeplebook.core.auth.FakeAuthRepository
import app.meeplebook.core.model.AuthCredentials
import app.meeplebook.core.plays.FakePlaysRepository
import app.meeplebook.core.plays.model.Play
import app.meeplebook.core.plays.model.PlayError
import app.meeplebook.core.result.AppResult
import app.meeplebook.core.sync.FakeSyncTimeRepository
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

    // Fixed clock for predictable testing
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
            syncTimeRepository = fakeSyncTimeRepository,
            clock = testClock
        )
    }

    @Test
    fun `invoke succeeds when plays sync successfully`() = runTest {
        // Given
        val user = AuthCredentials(
            username = "testuser",
            password = "password"
        )
        fakeAuthRepository.setCurrentUser(user)

        val plays = listOf(
            Play(
                id = 1,
                gameId = 100,
                gameName = "Catan",
                date = Instant.parse("2024-01-10T12:00:00Z"),
                quantity = 1,
                location = "Home",
                incomplete = false,
                length = 60,
                comments = null,
                players = emptyList()
            )
        )
        fakePlaysRepository.syncPlaysResult = AppResult.Success(plays)

        // When
        val result = useCase()

        // Then
        assertTrue(result is AppResult.Success)
        assertEquals(1, fakePlaysRepository.syncCallCount)
        assertEquals("testuser", fakePlaysRepository.lastSyncUsername)

        // Verify sync time was updated
        assertEquals(Instant.now(testClock), fakeSyncTimeRepository.getLastPlaysSync())
    }

    @Test
    fun `invoke returns NotLoggedIn error when no user is logged in`() = runTest {
        // Given - no user logged in

        // When
        val result = useCase()

        // Then
        assertTrue(result is AppResult.Failure)
        assertEquals(SyncUserDataError.NotLoggedIn, (result as AppResult.Failure).error)
        assertEquals(0, fakePlaysRepository.syncCallCount)

        // Verify sync time was not updated
        assertNull(fakeSyncTimeRepository.getLastPlaysSync())
    }

    @Test
    fun `invoke returns PlaysSyncFailed when plays sync fails with NetworkError`() = runTest {
        // Given
        val user = AuthCredentials(
            username = "testuser",
            password = "password"
        )
        fakeAuthRepository.setCurrentUser(user)

        val playError = PlayError.NetworkError
        fakePlaysRepository.syncPlaysResult = AppResult.Failure(playError)

        // When
        val result = useCase()

        // Then
        assertTrue(result is AppResult.Failure)
        val error = (result as AppResult.Failure).error
        assertEquals(SyncUserDataError.PlaysSyncFailed(playError), error)

        // Verify sync time was not updated
        assertNull(fakeSyncTimeRepository.getLastPlaysSync())
    }

    @Test
    fun `invoke returns PlaysSyncFailed when plays sync fails with Unknown error`() = runTest {
        // Given
        val user = AuthCredentials(
            username = "testuser",
            password = "password"
        )
        fakeAuthRepository.setCurrentUser(user)

        val throwable = RuntimeException("Test exception")
        val playError = PlayError.Unknown(throwable)
        fakePlaysRepository.syncPlaysResult = AppResult.Failure(playError)

        // When
        val result = useCase()

        // Then
        assertTrue(result is AppResult.Failure)
        val error = (result as AppResult.Failure).error
        assertTrue(error is SyncUserDataError.PlaysSyncFailed)
        assertTrue((error as SyncUserDataError.PlaysSyncFailed).error is PlayError.Unknown)

        // Verify sync time was not updated
        assertNull(fakeSyncTimeRepository.getLastPlaysSync())
    }

    @Test
    fun `invoke updates sync time correctly on success`() = runTest {
        // Given
        val user = AuthCredentials(
            username = "testuser",
            password = "password"
        )
        fakeAuthRepository.setCurrentUser(user)

        fakePlaysRepository.syncPlaysResult = AppResult.Success(emptyList())

        // When
        useCase()

        // Then - sync time should be set to the test clock time
        val expectedTime = Instant.now(testClock)
        assertEquals(expectedTime, fakeSyncTimeRepository.getLastPlaysSync())
    }
}
