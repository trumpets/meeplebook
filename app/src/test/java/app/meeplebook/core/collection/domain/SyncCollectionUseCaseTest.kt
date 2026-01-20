package app.meeplebook.core.collection.domain

import app.meeplebook.core.auth.FakeAuthRepository
import app.meeplebook.core.collection.FakeCollectionRepository
import app.meeplebook.core.collection.model.CollectionError
import app.meeplebook.core.collection.model.CollectionItem
import app.meeplebook.core.collection.model.GameSubtype
import app.meeplebook.core.model.AuthCredentials
import app.meeplebook.core.result.AppResult
import app.meeplebook.core.sync.FakeSyncTimeRepository
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
 * Unit tests for [SyncCollectionUseCase].
 */
class SyncCollectionUseCaseTest {

    private lateinit var fakeAuthRepository: FakeAuthRepository
    private lateinit var fakeCollectionRepository: FakeCollectionRepository
    private lateinit var fakeSyncTimeRepository: FakeSyncTimeRepository
    private lateinit var useCase: SyncCollectionUseCase

    // Fixed clock for predictable testing
    private val testClock = Clock.fixed(
        Instant.parse("2024-01-15T12:00:00Z"),
        ZoneOffset.UTC
    )

    @Before
    fun setUp() {
        fakeAuthRepository = FakeAuthRepository()
        fakeCollectionRepository = FakeCollectionRepository()
        fakeSyncTimeRepository = FakeSyncTimeRepository()
        useCase = SyncCollectionUseCase(
            authRepository = fakeAuthRepository,
            collectionRepository = fakeCollectionRepository,
            syncTimeRepository = fakeSyncTimeRepository,
            clock = testClock
        )
    }

    @Test
    fun `invoke succeeds when collection syncs successfully`() = runTest {
        // Given
        val user = AuthCredentials(
            username = "testuser",
            password = "password"
        )
        fakeAuthRepository.setCurrentUser(user)

        val collectionItems = listOf(
            CollectionItem(
                gameId = 1,
                subtype = GameSubtype.BOARDGAME,
                name = "Catan",
                yearPublished = 1995,
                thumbnail = null,
                lastModifiedDate = Instant.now(),
                minPlayers = null,
                maxPlayers = null,
                minPlayTimeMinutes = null,
                maxPlayTimeMinutes = null,
                numPlays = 0
            )
        )
        fakeCollectionRepository.syncCollectionResult = AppResult.Success(collectionItems)

        // When
        val result = useCase()

        // Then
        assertTrue(result is AppResult.Success)
        assertEquals(1, fakeCollectionRepository.syncCallCount)
        assertEquals("testuser", fakeCollectionRepository.lastSyncUsername)

        // Verify sync time was updated
        assertEquals(Instant.now(testClock), fakeSyncTimeRepository.getLastCollectionSync())
    }

    @Test
    fun `invoke returns NotLoggedIn error when no user is logged in`() = runTest {
        // Given - no user logged in

        // When
        val result = useCase()

        // Then
        assertTrue(result is AppResult.Failure)
        assertEquals(CollectionError.NotLoggedIn, (result as AppResult.Failure).error)
        assertEquals(0, fakeCollectionRepository.syncCallCount)

        // Verify sync time was not updated
        assertNull(fakeSyncTimeRepository.getLastCollectionSync())
    }

    @Test
    fun `invoke returns CollectionError when collection sync fails`() = runTest {
        // Given
        val user = AuthCredentials(
            username = "testuser",
            password = "password"
        )
        fakeAuthRepository.setCurrentUser(user)

        val collectionError = CollectionError.NetworkError
        fakeCollectionRepository.syncCollectionResult = AppResult.Failure(collectionError)

        // When
        val result = useCase()

        // Then
        assertTrue(result is AppResult.Failure)
        val error = (result as AppResult.Failure).error
        assertEquals(collectionError, error)

        // Verify sync time was not updated
        assertNull(fakeSyncTimeRepository.getLastCollectionSync())
    }

    @Test
    fun `invoke updates sync time correctly on success`() = runTest {
        // Given
        val user = AuthCredentials(
            username = "testuser",
            password = "password"
        )
        fakeAuthRepository.setCurrentUser(user)

        fakeCollectionRepository.syncCollectionResult = AppResult.Success(emptyList())

        // When
        useCase()

        // Then - sync time should be set to the test clock time
        val expectedTime = Instant.now(testClock)
        assertEquals(expectedTime, fakeSyncTimeRepository.getLastCollectionSync())
    }
}
