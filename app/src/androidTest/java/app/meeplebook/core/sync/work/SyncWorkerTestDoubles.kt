package app.meeplebook.core.sync.work

import androidx.work.Operation
import app.meeplebook.core.auth.AuthRepository
import app.meeplebook.core.auth.local.AuthLocalDataSource
import app.meeplebook.core.auth.model.AuthError
import app.meeplebook.core.collection.CollectionRepository
import app.meeplebook.core.collection.model.CollectionDataQuery
import app.meeplebook.core.collection.model.CollectionError
import app.meeplebook.core.collection.model.CollectionItem
import app.meeplebook.core.model.AuthCredentials
import app.meeplebook.core.plays.PlaysRepository
import app.meeplebook.core.plays.domain.CreatePlayCommand
import app.meeplebook.core.plays.domain.PlayerIdentity
import app.meeplebook.core.plays.model.Play
import app.meeplebook.core.plays.model.PlayError
import app.meeplebook.core.result.AppResult
import app.meeplebook.core.sync.manager.SyncManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.time.Instant

internal class FakeWorkerAuthRepository : AuthRepository {
    private val currentUserFlow = MutableStateFlow<AuthCredentials?>(null)

    var currentUser: AuthCredentials?
        get() = currentUserFlow.value
        set(value) {
            currentUserFlow.value = value
        }

    override fun observeCurrentUser(): Flow<AuthCredentials?> = currentUserFlow

    override suspend fun getCurrentUser(): AuthCredentials? = currentUser

    override suspend fun login(
        username: String,
        password: String
    ): AppResult<AuthCredentials, AuthError> = AppResult.Failure(AuthError.InvalidCredentials)

    override suspend fun logout() {
        currentUser = null
    }

    override fun isLoggedIn(): Flow<Boolean> = currentUserFlow.map { it != null }
}

internal class FakeWorkerAuthLocalDataSource : AuthLocalDataSource {
    private val credentialsFlow = MutableStateFlow<AuthCredentials?>(null)

    override fun observeCredentials(): Flow<AuthCredentials?> = credentialsFlow

    override suspend fun saveCredentials(creds: AuthCredentials) {
        credentialsFlow.value = creds
    }

    override suspend fun getCredentials(): AuthCredentials? = credentialsFlow.value

    override suspend fun clear() {
        credentialsFlow.value = null
    }
}

internal class FakeWorkerCollectionRepository : CollectionRepository {
    var syncResult: AppResult<List<CollectionItem>, CollectionError> = AppResult.Success(emptyList())
    var syncCallCount: Int = 0
        private set

    override fun observeCollection(query: CollectionDataQuery?): Flow<List<CollectionItem>> =
        flowOf(emptyList())

    override suspend fun getCollection(): List<CollectionItem> = emptyList()

    override suspend fun syncCollection(username: String): AppResult<List<CollectionItem>, CollectionError> {
        syncCallCount++
        return syncResult
    }

    override suspend fun clearCollection() = Unit

    override fun observeCollectionCount(): Flow<Long> = flowOf(0L)

    override fun observeUnplayedGamesCount(): Flow<Long> = flowOf(0L)

    override fun observeMostRecentlyAddedItem(): Flow<CollectionItem?> = flowOf(null)

    override fun observeFirstUnplayedGame(): Flow<CollectionItem?> = flowOf(null)
}

internal class FakeWorkerPlaysRepository : PlaysRepository {
    var syncPlaysResult: AppResult<Unit, PlayError> = AppResult.Success(Unit)
    var syncPendingResult: AppResult<Unit, PlayError> = AppResult.Success(Unit)
    var syncPlaysCallCount: Int = 0
        private set
    var syncPendingCallCount: Int = 0
        private set

    override fun observePlays(gameNameOrLocationQuery: String?): Flow<List<Play>> = flowOf(emptyList())

    override fun observePlaysForGame(gameId: Long): Flow<List<Play>> = flowOf(emptyList())

    override suspend fun getPlays(): List<Play> = emptyList()

    override suspend fun getPlaysForGame(gameId: Long): List<Play> = emptyList()

    override suspend fun syncPlays(username: String): AppResult<Unit, PlayError> {
        syncPlaysCallCount++
        return syncPlaysResult
    }

    override suspend fun syncPendingPlays(): AppResult<Unit, PlayError> {
        syncPendingCallCount++
        return syncPendingResult
    }

    override suspend fun createPlay(command: CreatePlayCommand) = Unit

    override suspend fun clearPlays() = Unit

    override fun observeTotalPlaysCount(): Flow<Long> = flowOf(0L)

    override fun observePlaysCountForPeriod(start: Instant, end: Instant): Flow<Long> = flowOf(0L)

    override fun observeRecentPlays(limit: Int): Flow<List<Play>> = flowOf(emptyList())

    override fun observeUniqueGamesCount(): Flow<Long> = flowOf(0L)

    override fun observeLocations(query: String): Flow<List<String>> = flowOf(emptyList())

    override fun observeRecentLocations(): Flow<List<String>> = flowOf(emptyList())

    override fun observePlayersByLocation(location: String): Flow<List<PlayerIdentity>> = flowOf(emptyList())

    override fun observeColorsUsedForGame(gameId: Long): Flow<List<String>> = flowOf(emptyList())

    override fun searchPlayersByName(query: String): Flow<List<PlayerIdentity>> = flowOf(emptyList())

    override fun searchPlayersByUsername(query: String): Flow<List<PlayerIdentity>> = flowOf(emptyList())
}

internal class FakeWorkerSyncManager : SyncManager {
    var fullSyncEnqueueCount: Int = 0
        private set

    private val operation = object : Operation {
        override fun getResult() = throw UnsupportedOperationException("Not used in this test")

        override fun getState() = throw UnsupportedOperationException("Not used in this test")
    }

    override fun observeFullSyncRunning(): Flow<Boolean> = flowOf(false)

    override fun enqueuePendingPlaysSync() = throw UnsupportedOperationException("Not used in this test")

    override fun enqueuePlaysSync() = throw UnsupportedOperationException("Not used in this test")

    override fun enqueueCollectionSync() = throw UnsupportedOperationException("Not used in this test")

    override fun enqueueFullSync() =
        operation.also {
            fullSyncEnqueueCount += 1
        }

    override fun schedulePeriodicFullSync() = throw UnsupportedOperationException("Not used in this test")
}
