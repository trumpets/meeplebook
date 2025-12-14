package app.meeplebook.feature.overview.domain

import app.meeplebook.core.auth.AuthRepository
import app.meeplebook.core.collection.CollectionRepository
import app.meeplebook.core.collection.model.CollectionError
import app.meeplebook.core.plays.PlaysRepository
import app.meeplebook.core.plays.model.PlayError
import app.meeplebook.core.result.AppResult
import app.meeplebook.core.result.fold
import app.meeplebook.core.sync.SyncTimeRepository
import java.time.Instant
import javax.inject.Inject

/**
 * Error types for overview data sync operations.
 */
sealed class SyncOverviewDataError {
    data object NotLoggedIn : SyncOverviewDataError()
    data class CollectionSyncFailed(val error: CollectionError) : SyncOverviewDataError()
    data class PlaysSyncFailed(val error: PlayError) : SyncOverviewDataError()
}

/**
 * Use case that syncs both collection and plays data for the overview screen.
 * This ensures the overview screen has fresh data from BGG.
 */
class SyncOverviewDataUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val collectionRepository: CollectionRepository,
    private val playsRepository: PlaysRepository,
    private val syncTimeRepository: SyncTimeRepository
) {
    /**
     * Syncs collection and plays data for the currently logged-in user.
     *
     * @return Success if both syncs succeed, Failure with the first error encountered
     */
    suspend operator fun invoke(): AppResult<Unit, SyncOverviewDataError> {
        // Get current user
        val user = authRepository.getCurrentUser()
            ?: return AppResult.Failure(SyncOverviewDataError.NotLoggedIn)

        // Sync collection
        val collectionResult = collectionRepository.syncCollection(user.username)
        collectionResult.fold(
            onSuccess = {
                // Record collection sync time
                syncTimeRepository.updateCollectionSyncTime(Instant.now())
            },
            onFailure = { error ->
                return AppResult.Failure(SyncOverviewDataError.CollectionSyncFailed(error))
            }
        )

        // Sync plays
        val playsResult = playsRepository.syncPlays(user.username)
        playsResult.fold(
            onSuccess = {
                // Record plays sync time
                syncTimeRepository.updatePlaysSyncTime(Instant.now())
            },
            onFailure = { error ->
                return AppResult.Failure(SyncOverviewDataError.PlaysSyncFailed(error))
            }
        )

        // Record full sync time
        syncTimeRepository.updateFullSyncTime(Instant.now())

        return AppResult.Success(Unit)
    }
}