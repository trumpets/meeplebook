package app.meeplebook.core.sync.domain

import app.meeplebook.core.auth.AuthRepository
import app.meeplebook.core.plays.PlaysRepository
import app.meeplebook.core.result.AppResult
import app.meeplebook.core.result.fold
import app.meeplebook.core.sync.SyncTimeRepository
import app.meeplebook.core.sync.model.SyncUserDataError
import java.time.Clock
import java.time.Instant
import javax.inject.Inject

/**
 * Auth-gated entry point for remote plays pull sync.
 *
 * This use case sits between background orchestration (manual refresh today, workers later) and the
 * repository-owned plays pull sync. It handles user/session preconditions and maps repository
 * failures into app-level sync errors.
 */
class SyncPlaysUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val playsRepository: PlaysRepository,
    private val syncTimeRepository: SyncTimeRepository,
    private val clock: Clock
) {
    /**
     * Runs the remote plays pull sync for the currently logged-in user.
     *
     * Returns [SyncUserDataError.NotLoggedIn] when no user is authenticated and
     * [SyncUserDataError.PlaysSyncFailed] when the repository pull sync fails. Successful runs
     * update the plays sync timestamp.
     */
    suspend operator fun invoke(): AppResult<Unit, SyncUserDataError> {
        // Get current user
        val user = authRepository.getCurrentUser()
            ?: return AppResult.Failure(SyncUserDataError.NotLoggedIn)

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

        return AppResult.Success(Unit)
    }
}
