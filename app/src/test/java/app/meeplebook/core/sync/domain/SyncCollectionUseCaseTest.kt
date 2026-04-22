package app.meeplebook.core.sync.domain

import app.meeplebook.core.auth.FakeAuthRepository
import app.meeplebook.core.collection.FakeCollectionRepository
import app.meeplebook.core.collection.model.CollectionError
import app.meeplebook.core.collection.model.CollectionItem
import app.meeplebook.core.collection.model.GameSubtype
import app.meeplebook.core.model.AuthCredentials
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
 * Unit tests for [SyncCollectionUseCase].
 */
class SyncCollectionUseCaseTest {

    private lateinit var fakeAuthRepository: FakeAuthRepository
    private lateinit var fakeCollectionRepository: FakeCollectionRepository
    private lateinit var fakeSyncTimeRepository: FakeSyncTimeRepository
    private lateinit var useCase: SyncCollectionUseCase

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
            syncRunner = SyncRunner(
                syncTimeRepository = fakeSyncTimeRepository,
                clock = testClock
            )
        )
    }

    @Test
    fun `invoke succeeds when collection syncs successfully`() = runTest {
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

        val result = useCase()

        assertTrue(result is AppResult.Success)
        assertEquals(1, fakeCollectionRepository.syncCallCount)
        assertEquals("testuser", fakeCollectionRepository.lastSyncUsername)
        assertEquals(Instant.now(testClock), fakeSyncTimeRepository.getLastCollectionSync())
        assertEquals(
            SyncState(
                isSyncing = false,
                lastSyncedAt = Instant.now(testClock),
                errorMessage = null
            ),
            fakeSyncTimeRepository.getSyncState(SyncType.COLLECTION)
        )
    }

    @Test
    fun `invoke returns NotLoggedIn error when no user is logged in`() = runTest {
        val result = useCase()

        assertTrue(result is AppResult.Failure)
        assertEquals(SyncUserDataError.NotLoggedIn, (result as AppResult.Failure).error)
        assertEquals(0, fakeCollectionRepository.syncCallCount)
        assertNull(fakeSyncTimeRepository.getLastCollectionSync())
        assertEquals(SyncState(), fakeSyncTimeRepository.getSyncState(SyncType.COLLECTION))
    }

    @Test
    fun `invoke returns CollectionError when collection sync fails`() = runTest {
        val user = AuthCredentials(
            username = "testuser",
            password = "password"
        )
        fakeAuthRepository.setCurrentUser(user)

        val collectionError = CollectionError.NetworkError
        fakeCollectionRepository.syncCollectionResult = AppResult.Failure(collectionError)

        val result = useCase()

        assertTrue(result is AppResult.Failure)
        val error = (result as AppResult.Failure).error
        assertEquals(SyncUserDataError.CollectionSyncFailed(collectionError), error)
        assertNull(fakeSyncTimeRepository.getLastCollectionSync())
        assertEquals(
            SyncState(
                isSyncing = false,
                lastSyncedAt = null,
                errorMessage = "NetworkError"
            ),
            fakeSyncTimeRepository.getSyncState(SyncType.COLLECTION)
        )
    }
}
