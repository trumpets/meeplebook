package app.meeplebook.core.sync.domain

import app.meeplebook.core.sync.SyncTimeRepository
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import javax.inject.Inject

/**
 * Observes the timestamp of the last successful full data synchronization.
 *
 * Emits null until both collection and plays have completed successfully at least once.
 */
class ObserveLastFullSyncUseCase @Inject constructor(
    private val syncTimeRepository: SyncTimeRepository
) {
    operator fun invoke(): Flow<Instant?> {
        return syncTimeRepository.observeLastFullSync()
    }
}
