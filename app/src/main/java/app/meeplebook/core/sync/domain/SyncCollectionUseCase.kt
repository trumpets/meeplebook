package app.meeplebook.core.sync.domain

import app.meeplebook.core.auth.AuthRepository
import app.meeplebook.core.collection.CollectionRepository
import app.meeplebook.core.result.AppResult
import app.meeplebook.core.result.fold
import app.meeplebook.core.sync.SyncTimeRepository
import app.meeplebook.core.sync.model.SyncUserDataError
import java.time.Clock
import java.time.Instant
import javax.inject.Inject

/**
 * Auth-gated entry point for collection background sync.
 *
 * This use case sits between background orchestration (manual refresh today, workers later) and the
 * repository-owned collection pull sync. It handles user/session preconditions and maps repository
 * failures into app-level sync errors.
 */
class SyncCollectionUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val collectionRepository: CollectionRepository,
    private val syncTimeRepository: SyncTimeRepository,
    private val clock: Clock
) {
    /**
     * Runs the collection pull sync for the currently logged-in user.
     *
     * Returns [SyncUserDataError.NotLoggedIn] when no user is authenticated and
     * [SyncUserDataError.CollectionSyncFailed] when the repository pull sync fails. Successful runs
     * update the collection sync timestamp.
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

        return AppResult.Success(Unit)
    }
}
