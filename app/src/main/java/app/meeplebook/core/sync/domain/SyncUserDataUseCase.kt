package app.meeplebook.core.sync.domain

import app.meeplebook.core.auth.AuthRepository
import app.meeplebook.core.collection.CollectionRepository
import app.meeplebook.core.plays.PlaysRepository
import app.meeplebook.core.result.AppResult
import app.meeplebook.core.result.fold
import app.meeplebook.core.sync.SyncTimeRepository
import app.meeplebook.core.sync.model.SyncUserDataError
import java.time.Clock
import java.time.Instant
import javax.inject.Inject

/**
 * Synchronizes collection and plays data for the currently logged-in user.
 *
 * Updates sync timestamps on successful synchronization. Both collection and plays
 * must sync successfully for the operation to be considered complete.
 */
class SyncUserDataUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val collectionRepository: CollectionRepository,
    private val playsRepository: PlaysRepository,
    private val syncTimeRepository: SyncTimeRepository,
    private val clock: Clock
) {
    /**
     * Performs a full sync of collection and plays data from BGG.
     *
     * Syncs collection first, then plays. If either sync fails, the operation stops
     * and returns the error. Sync timestamps are updated after each successful sync.
     */
    suspend operator fun invoke(): AppResult<Unit, SyncUserDataError> {
        // Get current user
        val user = authRepository.getCurrentUser()
            ?: return AppResult.Failure(SyncUserDataError.NotLoggedIn)

        // Sync collection
        val collectionResult = collectionRepository.syncCollection(user.username)
        collectionResult.fold(
            onSuccess = {
                // Record collection sync time
                syncTimeRepository.updateCollectionSyncTime(Instant.now(clock))
            },
            onFailure = { error ->
                return AppResult.Failure(SyncUserDataError.CollectionSyncFailed(error))
            }
        )

        // Sync plays
        val playsResult = playsRepository.syncPlays(user.username)
        playsResult.fold(
            onSuccess = {
                // Record plays sync time
                syncTimeRepository.updatePlaysSyncTime(Instant.now(clock))
            },
            onFailure = { error ->
                return AppResult.Failure(SyncUserDataError.PlaysSyncFailed(error))
            }
        )

        // Record full sync time
        syncTimeRepository.updateFullSyncTime(Instant.now(clock))

        return AppResult.Success(Unit)
    }
}