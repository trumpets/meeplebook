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
 * Synchronizes plays data for the currently logged-in user.
 *
 * Updates sync timestamp on successful synchronization.
 * Returns SyncUserDataError.NotLoggedIn if no user is currently authenticated.
 */
class SyncPlaysUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val playsRepository: PlaysRepository,
    private val syncTimeRepository: SyncTimeRepository,
    private val clock: Clock
) {
    /**
     * Performs a sync of plays data from BGG.
     *
     * Returns SyncUserDataError.NotLoggedIn if no user is authenticated.
     * Returns the specific SyncUserDataError.PlaysSyncFailed(PlayError) if sync fails.
     * Sync timestamp is updated only after successful sync.
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