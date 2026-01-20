package app.meeplebook.core.collection.domain

import app.meeplebook.core.auth.AuthRepository
import app.meeplebook.core.collection.CollectionRepository
import app.meeplebook.core.collection.model.CollectionError
import app.meeplebook.core.result.AppResult
import app.meeplebook.core.result.fold
import app.meeplebook.core.sync.SyncTimeRepository
import java.time.Clock
import java.time.Instant
import javax.inject.Inject

/**
 * Synchronizes collection data for the currently logged-in user.
 *
 * Updates sync timestamp on successful synchronization.
 * Returns CollectionError.NotLoggedIn if no user is currently authenticated.
 */
class SyncCollectionUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val collectionRepository: CollectionRepository,
    private val syncTimeRepository: SyncTimeRepository,
    private val clock: Clock
) {
    /**
     * Performs a sync of collection data from BGG.
     *
     * Returns CollectionError.NotLoggedIn if no user is authenticated.
     * Returns the specific CollectionError if sync fails.
     * Sync timestamp is updated only after successful sync.
     */
    suspend operator fun invoke(): AppResult<Unit, CollectionError> {
        // Get current user
        val user = authRepository.getCurrentUser()
            ?: return AppResult.Failure(CollectionError.NotLoggedIn)

        // Sync collection
        val collectionResult = collectionRepository.syncCollection(user.username)
        collectionResult.fold(
            onSuccess = {
                // Record collection sync time
                syncTimeRepository.updateCollectionSyncTime(Instant.now(clock))
            },
            onFailure = { error ->
                return AppResult.Failure(error)
            }
        )

        return AppResult.Success(Unit)
    }
}
