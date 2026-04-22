package app.meeplebook.core.sync.domain

import app.meeplebook.core.auth.AuthRepository
import app.meeplebook.core.collection.CollectionRepository
import app.meeplebook.core.collection.model.CollectionError
import app.meeplebook.core.result.AppResult
import app.meeplebook.core.result.fold
import app.meeplebook.core.sync.SyncRunner
import app.meeplebook.core.sync.model.SyncType
import app.meeplebook.core.sync.model.SyncUserDataError
import javax.inject.Inject

/**
 * Auth-gated entry point for collection background sync.
 *
 * This use case sits between background orchestration (manual refresh today, workers later) and the
 * repository-owned collection pull sync. It handles user/session preconditions, persists collection
 * sync execution state, and maps repository failures into app-level sync errors.
 */
class SyncCollectionUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val collectionRepository: CollectionRepository,
    private val syncRunner: SyncRunner
) {
    /**
     * Runs the collection pull sync for the currently logged-in user.
     *
     * Returns [SyncUserDataError.NotLoggedIn] when no user is authenticated and
     * [SyncUserDataError.CollectionSyncFailed] when the repository pull sync fails. Successful runs
     * clear any previous error and update the collection sync timestamp.
     */
    suspend operator fun invoke(): AppResult<Unit, SyncUserDataError> {
        val user = authRepository.getCurrentUser()
            ?: return AppResult.Failure(SyncUserDataError.NotLoggedIn)

        return syncRunner.run(
            type = SyncType.COLLECTION,
            parseStorageError = { error -> error.toStorageMessage() },
            block = { collectionRepository.syncCollection(user.username) }
        ).fold(
            onSuccess = {
                AppResult.Success(Unit)
            },
            onFailure = { error ->
                AppResult.Failure(SyncUserDataError.CollectionSyncFailed(error))
            }
        )
    }
}

private fun CollectionError.toStorageMessage(): String =
    when (this) {
        CollectionError.NetworkError -> "NetworkError"
        CollectionError.NotLoggedIn -> "NotLoggedIn"
        is CollectionError.MaxRetriesExceeded -> "MaxRetriesExceeded"
        is CollectionError.Unknown -> throwable.message ?: "Unknown"
    }
