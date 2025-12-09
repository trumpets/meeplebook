package app.meeplebook.feature.home.domain

import app.meeplebook.core.auth.FakeAuthRepository
import app.meeplebook.core.collection.FakeCollectionRepository
import app.meeplebook.core.collection.model.CollectionError
import app.meeplebook.core.collection.model.CollectionItem
import app.meeplebook.core.collection.model.GameSubtype
import app.meeplebook.core.model.AuthCredentials
import app.meeplebook.core.plays.FakePlaysRepository
import app.meeplebook.core.plays.model.Play
import app.meeplebook.core.plays.model.PlayError
import app.meeplebook.core.result.AppResult
import app.meeplebook.core.sync.FakeSyncTimeRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SyncHomeDataUseCaseTest {

    private lateinit var fakeAuthRepository: FakeAuthRepository
    private lateinit var fakeCollectionRepository: FakeCollectionRepository
    private lateinit var fakePlaysRepository: FakePlaysRepository
    private lateinit var fakeSyncTimeRepository: FakeSyncTimeRepository
    private lateinit var useCase: SyncHomeDataUseCase

    @Before
    fun setUp() {
        fakeAuthRepository = FakeAuthRepository()
        fakeCollectionRepository = FakeCollectionRepository()
        fakePlaysRepository = FakePlaysRepository()
        fakeSyncTimeRepository = FakeSyncTimeRepository()
        useCase = SyncHomeDataUseCase(
            fakeAuthRepository,
            fakeCollectionRepository,
            fakePlaysRepository,
            fakeSyncTimeRepository
        )
    }

    @Test
    fun `returns NotLoggedIn error when user is not logged in`() = runTest {
        // No user logged in

        val result = useCase()

        assertTrue(result is AppResult.Failure)
        val error = (result as AppResult.Failure).error
        assertTrue(error is SyncHomeDataError.NotLoggedIn)
    }

    @Test
    fun `syncs collection and plays successfully when logged in`() = runTest {
        // Setup logged in user
        fakeAuthRepository.loginResult = AppResult.Success(AuthCredentials("testuser", "testpass"))
        fakeAuthRepository.login("testuser", "testpass")

        // Setup successful sync results
        val collectionItems = listOf(
            CollectionItem(1, GameSubtype.BOARDGAME, "Game 1", 2020, null)
        )
        fakeCollectionRepository.syncCollectionResult = AppResult.Success(collectionItems)

        val plays = listOf(
            Play(1, "2024-12-05", 1, 60, false, null, 1, "Game 1", null, emptyList())
        )
        fakePlaysRepository.syncPlaysResult = AppResult.Success(plays)

        val result = useCase()

        assertTrue(result is AppResult.Success)
        assertEquals(1, fakeCollectionRepository.syncCollectionCallCount)
        assertEquals("testuser", fakeCollectionRepository.lastSyncUsername)
        assertEquals(1, fakePlaysRepository.syncPlaysCallCount)
        assertEquals("testuser", fakePlaysRepository.lastSyncUsername)
    }

    @Test
    fun `returns CollectionSyncFailed when collection sync fails`() = runTest {
        // Setup logged in user
        fakeAuthRepository.loginResult = AppResult.Success(AuthCredentials("testuser", "testpass"))
        fakeAuthRepository.login("testuser", "testpass")

        // Setup failed collection sync
        fakeCollectionRepository.syncCollectionResult = 
            AppResult.Failure(CollectionError.NetworkError(RuntimeException("Network error")))

        val result = useCase()

        assertTrue(result is AppResult.Failure)
        val error = (result as AppResult.Failure).error
        assertTrue(error is SyncHomeDataError.CollectionSyncFailed)
        // Plays sync should not be attempted
        assertEquals(0, fakePlaysRepository.syncPlaysCallCount)
    }

    @Test
    fun `returns PlaysSyncFailed when plays sync fails`() = runTest {
        // Setup logged in user
        fakeAuthRepository.loginResult = AppResult.Success(AuthCredentials("testuser", "testpass"))
        fakeAuthRepository.login("testuser", "testpass")

        // Setup successful collection sync
        val collectionItems = listOf(
            CollectionItem(1, GameSubtype.BOARDGAME, "Game 1", 2020, null)
        )
        fakeCollectionRepository.syncCollectionResult = AppResult.Success(collectionItems)

        // Setup failed plays sync
        fakePlaysRepository.syncPlaysResult = 
            AppResult.Failure(PlayError.NetworkError(RuntimeException("Network error")))

        val result = useCase()

        assertTrue(result is AppResult.Failure)
        val error = (result as AppResult.Failure).error
        assertTrue(error is SyncHomeDataError.PlaysSyncFailed)
        // Collection sync should have been attempted
        assertEquals(1, fakeCollectionRepository.syncCollectionCallCount)
    }

    @Test
    fun `uses current user's username for syncing`() = runTest {
        // Setup logged in user with specific username
        fakeAuthRepository.loginResult = AppResult.Success(AuthCredentials("myusername", "password"))
        fakeAuthRepository.login("myusername", "password")

        // Setup successful sync results
        fakeCollectionRepository.syncCollectionResult = AppResult.Success(emptyList())
        fakePlaysRepository.syncPlaysResult = AppResult.Success(emptyList())

        useCase()

        assertEquals("myusername", fakeCollectionRepository.lastSyncUsername)
        assertEquals("myusername", fakePlaysRepository.lastSyncUsername)
    }

    @Test
    fun `handles queued response error from collection`() = runTest {
        // Setup logged in user
        fakeAuthRepository.loginResult = AppResult.Success(AuthCredentials("testuser", "testpass"))
        fakeAuthRepository.login("testuser", "testpass")

        // Setup queued response error
        fakeCollectionRepository.syncCollectionResult = 
            AppResult.Failure(CollectionError.QueuedResponse)

        val result = useCase()

        assertTrue(result is AppResult.Failure)
        val error = (result as AppResult.Failure).error
        assertTrue(error is SyncHomeDataError.CollectionSyncFailed)
    }

    @Test
    fun `handles queued response error from plays`() = runTest {
        // Setup logged in user
        fakeAuthRepository.loginResult = AppResult.Success(AuthCredentials("testuser", "testpass"))
        fakeAuthRepository.login("testuser", "testpass")

        // Setup successful collection sync
        fakeCollectionRepository.syncCollectionResult = AppResult.Success(emptyList())

        // Setup queued response error from plays
        fakePlaysRepository.syncPlaysResult = 
            AppResult.Failure(PlayError.QueuedResponse)

        val result = useCase()

        assertTrue(result is AppResult.Failure)
        val error = (result as AppResult.Failure).error
        assertTrue(error is SyncHomeDataError.PlaysSyncFailed)
    }
}
