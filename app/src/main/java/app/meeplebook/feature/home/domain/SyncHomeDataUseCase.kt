package app.meeplebook.feature.home.domain

import app.meeplebook.core.auth.AuthRepository
import app.meeplebook.core.collection.CollectionRepository
import app.meeplebook.core.collection.model.CollectionError
import app.meeplebook.core.plays.PlaysRepository
import app.meeplebook.core.plays.model.PlayError
import app.meeplebook.core.result.AppResult
import app.meeplebook.core.result.fold
import javax.inject.Inject

/**
 * Error types for home data sync operations.
 */
sealed class SyncHomeDataError {
    data object NotLoggedIn : SyncHomeDataError()
    data class CollectionSyncFailed(val error: CollectionError) : SyncHomeDataError()
    data class PlaysSyncFailed(val error: PlayError) : SyncHomeDataError()
}

/**
 * Use case that syncs both collection and plays data for the home screen.
 * This ensures the home screen has fresh data from BGG.
 */
class SyncHomeDataUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val collectionRepository: CollectionRepository,
    private val playsRepository: PlaysRepository
) {
    /**
     * Syncs collection and plays data for the currently logged-in user.
     *
     * @return Success if both syncs succeed, Failure with the first error encountered
     */
    suspend operator fun invoke(): AppResult<Unit, SyncHomeDataError> {
        // Get current user
        val user = authRepository.getCurrentUser()
            ?: return AppResult.Failure(SyncHomeDataError.NotLoggedIn)
        
        // Sync collection
        val collectionResult = collectionRepository.syncCollection(user.username)
        collectionResult.fold(
            onSuccess = { /* Continue to sync plays */ },
            onFailure = { error ->
                return AppResult.Failure(SyncHomeDataError.CollectionSyncFailed(error))
            }
        )
        
        // Sync plays
        val playsResult = playsRepository.syncPlays(user.username)
        playsResult.fold(
            onSuccess = { /* Success */ },
            onFailure = { error ->
                return AppResult.Failure(SyncHomeDataError.PlaysSyncFailed(error))
            }
        )
        
        return AppResult.Success(Unit)
    }
}
