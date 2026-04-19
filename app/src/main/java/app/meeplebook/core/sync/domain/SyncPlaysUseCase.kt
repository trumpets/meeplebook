package app.meeplebook.core.sync.domain

import app.meeplebook.core.auth.AuthRepository
import app.meeplebook.core.plays.PlaysRepository
import app.meeplebook.core.plays.model.PlayError
import app.meeplebook.core.result.AppResult
import app.meeplebook.core.result.fold
import app.meeplebook.core.sync.SyncRunner
import app.meeplebook.core.sync.model.SyncType
import app.meeplebook.core.sync.model.SyncUserDataError
import javax.inject.Inject

/**
 * Auth-gated entry point for remote plays pull sync.
 *
 * This use case sits between background orchestration (manual refresh today, workers later) and the
 * repository-owned plays pull sync. It handles user/session preconditions, persists plays sync
 * execution state, and maps repository failures into app-level sync errors.
 */
class SyncPlaysUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val playsRepository: PlaysRepository,
    private val syncRunner: SyncRunner
) {
    /**
     * Runs the remote plays pull sync for the currently logged-in user.
     *
     * Returns [SyncUserDataError.NotLoggedIn] when no user is authenticated and
     * [SyncUserDataError.PlaysSyncFailed] when the repository pull sync fails. Successful runs
     * clear any previous error and update the plays sync timestamp.
     */
    suspend operator fun invoke(): AppResult<Unit, SyncUserDataError> {
        val user = authRepository.getCurrentUser()
            ?: return AppResult.Failure(SyncUserDataError.NotLoggedIn)

        return syncRunner.run(
            type = SyncType.PLAYS,
            parseStorageError = { error -> error.toStorageMessage() },
            block = { playsRepository.syncPlays(user.username) }
        ).fold(
            onSuccess = {
                AppResult.Success(Unit)
            },
            onFailure = { error ->
                AppResult.Failure(SyncUserDataError.PlaysSyncFailed(error))
            }
        )
    }
}

private fun PlayError.toStorageMessage(): String =
    when (this) {
        PlayError.NetworkError -> "NetworkError"
        PlayError.NotLoggedIn -> "NotLoggedIn"
        is PlayError.MaxRetriesExceeded -> "MaxRetriesExceeded"
        is PlayError.Unknown -> throwable.message ?: "Unknown"
    }
