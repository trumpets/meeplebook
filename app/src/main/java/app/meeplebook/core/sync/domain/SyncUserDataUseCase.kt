package app.meeplebook.core.sync.domain

import app.meeplebook.core.result.AppResult
import app.meeplebook.core.sync.SyncTimeRepository
import app.meeplebook.core.sync.model.SyncUserDataError
import java.time.Clock
import java.time.Instant
import javax.inject.Inject

/**
 * Auth-gated full-sync entry point that composes the narrower sync use cases.
 *
 * This keeps repository-specific pull-sync details inside [SyncCollectionUseCase] and
 * [SyncPlaysUseCase] while giving manual refresh today, and workers later, a single full-sync
 * orchestration boundary.
 */
class SyncUserDataUseCase @Inject constructor(
    private val syncCollection: SyncCollectionUseCase,
    private val syncPlays: SyncPlaysUseCase,
    private val syncTimeRepository: SyncTimeRepository,
    private val clock: Clock
) {
    /**
     * Runs the current full-sync pipeline.
     *
     * This slice keeps the existing sequence of collection pull followed by plays pull while moving
     * the per-domain sync responsibility into the narrower use cases. The full-sync timestamp is
     * updated only after both steps succeed.
     */
    suspend operator fun invoke(): AppResult<Unit, SyncUserDataError> {
        when (val collectionResult = syncCollection()) {
            is AppResult.Failure -> return collectionResult
            is AppResult.Success -> Unit
        }

        when (val playsResult = syncPlays()) {
            is AppResult.Failure -> return playsResult
            is AppResult.Success -> Unit
        }

        syncTimeRepository.updateFullSyncTime(Instant.now(clock))
        return AppResult.Success(Unit)
    }
}
