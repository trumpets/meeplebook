package app.meeplebook.core.sync.domain

import app.meeplebook.core.auth.FakeAuthRepository
import app.meeplebook.core.collection.FakeCollectionRepository
import app.meeplebook.core.collection.model.CollectionError
import app.meeplebook.core.collection.model.CollectionItem
import app.meeplebook.core.collection.model.GameSubtype
import app.meeplebook.core.model.AuthCredentials
import app.meeplebook.core.plays.FakePlaysRepository
import app.meeplebook.core.plays.PlayTestFactory.createPlay
import app.meeplebook.core.plays.model.PlayError
import app.meeplebook.core.result.AppResult
import app.meeplebook.core.sync.FakeSyncTimeRepository
import app.meeplebook.core.sync.model.SyncUserDataError
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

/**
 * Unit tests for [SyncUserDataUseCase].
 */
class SyncUserDataUseCaseTest {

    private lateinit var fakeAuthRepository: FakeAuthRepository
    private lateinit var fakeCollectionRepository: FakeCollectionRepository
    private lateinit var fakePlaysRepository: FakePlaysRepository
    private lateinit var fakeSyncTimeRepository: FakeSyncTimeRepository
    private lateinit var useCase: SyncUserDataUseCase

    // Fixed clock for predictable testing
    private val testClock = Clock.fixed(
        Instant.parse("2024-01-15T12:00:00Z"),
        ZoneOffset.UTC
    )

    @Before
    fun setUp() {
        fakeAuthRepository = FakeAuthRepository()
        fakeCollectionRepository = FakeCollectionRepository()
        fakePlaysRepository = FakePlaysRepository()
        fakeSyncTimeRepository = FakeSyncTimeRepository()
        useCase = SyncUserDataUseCase(
            authRepository = fakeAuthRepository,
            collectionRepository = fakeCollectionRepository,
            playsRepository = fakePlaysRepository,
            syncTimeRepository = fakeSyncTimeRepository,
            clock = testClock
        )
    }

    @Test
    fun `invoke succeeds when both collection and plays sync successfully`() = runTest {
        // Given
        val user = AuthCredentials(
            username = "testuser",
            password = "password",
            bearerToken = "token"
        )
        fakeAuthRepository.setCurrentUser(user)

        val collectionItems = listOf(
            CollectionItem(
                gameId = 1,
                subtype = GameSubtype.BOARDGAME,
                name = "Catan",
                yearPublished = 1995,
                thumbnail = null,
                lastModifiedDate = Instant.now()
            )
        )
        fakeCollectionRepository.syncCollectionResult = AppResult.Success(collectionItems)

        val plays = listOf(
            createPlay(id = 1, gameName = "Catan")
        )
        fakePlaysRepository.syncPlaysResult = AppResult.Success(plays)

        // When
        val result = useCase()

        // Then
        assertTrue(result is AppResult.Success)
        assertEquals(1, fakeCollectionRepository.syncCallCount)
        assertEquals("testuser", fakeCollectionRepository.lastSyncUsername)
        assertEquals(1, fakePlaysRepository.syncCallCount)
        assertEquals("testuser", fakePlaysRepository.lastSyncUsername)

        // Verify sync times were updated
        assertEquals(Instant.now(testClock), fakeSyncTimeRepository.getLastCollectionSync())
        assertEquals(Instant.now(testClock), fakeSyncTimeRepository.getLastPlaysSync())
        assertEquals(Instant.now(testClock), fakeSyncTimeRepository.getLastFullSync())
    }

    @Test
    fun `invoke returns NotLoggedIn error when no user is logged in`() = runTest {
        // Given - no user logged in

        // When
        val result = useCase()

        // Then
        assertTrue(result is AppResult.Failure)
        assertEquals(SyncUserDataError.NotLoggedIn, (result as AppResult.Failure).error)
        assertEquals(0, fakeCollectionRepository.syncCallCount)
        assertEquals(0, fakePlaysRepository.syncCallCount)

        // Verify sync times were not updated
        assertNull(fakeSyncTimeRepository.getLastFullSync())
    }

    @Test
    fun `invoke returns CollectionSyncFailed when collection sync fails`() = runTest {
        // Given
        val user = AuthCredentials(
            username = "testuser",
            password = "password",
            bearerToken = "token"
        )
        fakeAuthRepository.setCurrentUser(user)

        val collectionError = CollectionError.NetworkError
        fakeCollectionRepository.syncCollectionResult = AppResult.Failure(collectionError)

        // When
        val result = useCase()

        // Then
        assertTrue(result is AppResult.Failure)
        val error = (result as AppResult.Failure).error
        assertTrue(error is SyncUserDataError.CollectionSyncFailed)
        assertEquals(collectionError, (error as SyncUserDataError.CollectionSyncFailed).error)

        // Verify plays sync was not attempted
        assertEquals(0, fakePlaysRepository.syncCallCount)

        // Verify sync times were not updated
        assertNull(fakeSyncTimeRepository.getLastFullSync())
    }

    @Test
    fun `invoke returns PlaysSyncFailed when plays sync fails but collection succeeds`() = runTest {
        // Given
        val user = AuthCredentials(
            username = "testuser",
            password = "password",
            bearerToken = "token"
        )
        fakeAuthRepository.setCurrentUser(user)

        val collectionItems = listOf(
            CollectionItem(
                gameId = 1,
                subtype = GameSubtype.BOARDGAME,
                name = "Catan",
                yearPublished = 1995,
                thumbnail = null,
                lastModifiedDate = Instant.now()
            )
        )
        fakeCollectionRepository.syncCollectionResult = AppResult.Success(collectionItems)

        val playError = PlayError.NetworkError
        fakePlaysRepository.syncPlaysResult = AppResult.Failure(playError)

        // When
        val result = useCase()

        // Then
        assertTrue(result is AppResult.Failure)
        val error = (result as AppResult.Failure).error
        assertTrue(error is SyncUserDataError.PlaysSyncFailed)
        assertEquals(playError, (error as SyncUserDataError.PlaysSyncFailed).error)

        // Verify both syncs were attempted
        assertEquals(1, fakeCollectionRepository.syncCallCount)
        assertEquals(1, fakePlaysRepository.syncCallCount)

        // Verify collection sync time was updated but not full sync
        assertNotNull(fakeSyncTimeRepository.getLastCollectionSync())
        assertNull(fakeSyncTimeRepository.getLastFullSync())
    }

    @Test
    fun `invoke updates all sync times correctly`() = runTest {
        // Given
        val user = AuthCredentials(
            username = "testuser",
            password = "password",
            bearerToken = "token"
        )
        fakeAuthRepository.setCurrentUser(user)

        fakeCollectionRepository.syncCollectionResult = AppResult.Success(emptyList())
        fakePlaysRepository.syncPlaysResult = AppResult.Success(emptyList())

        // When
        useCase()

        // Then - all sync times should be set to the test clock time
        val expectedTime = Instant.now(testClock)
        assertEquals(expectedTime, fakeSyncTimeRepository.getLastCollectionSync())
        assertEquals(expectedTime, fakeSyncTimeRepository.getLastPlaysSync())
        assertEquals(expectedTime, fakeSyncTimeRepository.getLastFullSync())
    }
}
