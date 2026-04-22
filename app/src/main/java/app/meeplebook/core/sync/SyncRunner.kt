package app.meeplebook.core.sync

import app.meeplebook.core.result.AppResult
import app.meeplebook.core.sync.model.SyncType
import kotlinx.coroutines.CancellationException
import java.time.Clock
import javax.inject.Inject

/**
 * Shared lifecycle wrapper for running a sync unit of work.
 *
 * It persists started, successful, and failed states around the supplied [block] and returns the
 * original [AppResult] untouched so feature-specific callers can map errors at their own boundary.
 */
class SyncRunner @Inject constructor(
    private val syncTimeRepository: SyncTimeRepository,
    private val clock: Clock
) {
    /**
     * Runs [block] while persisting sync lifecycle state for [type].
     */
    suspend fun <SyncResult, SyncError> run(
        type: SyncType,
        parseStorageError: (SyncError) -> String = { error -> error.toString() },
        block: suspend () -> AppResult<SyncResult, SyncError>
    ): AppResult<SyncResult, SyncError> {
        syncTimeRepository.markStarted(type)

        return try {
            val result = block()
            when (result) {
                is AppResult.Success -> syncTimeRepository.markCompleted(type, clock.instant())
                is AppResult.Failure -> syncTimeRepository.markFailed(
                    type = type,
                    errorMessage = parseStorageError(result.error)
                )
            }
            result
        } catch (e: CancellationException) {
            // IMPORTANT: do not treat as failure. Normal cancellation due to OS cancelling Job for whatever reason
            syncTimeRepository.markIdle(type)
            throw e // NEVER swallow cancellation
        } catch (throwable: Throwable) {
            syncTimeRepository.markFailed(type, throwable.message)
            throw throwable
        }
    }
}
