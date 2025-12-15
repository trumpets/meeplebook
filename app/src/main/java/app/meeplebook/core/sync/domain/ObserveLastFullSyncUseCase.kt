package app.meeplebook.core.sync.domain

import app.meeplebook.core.sync.SyncTimeRepository
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import javax.inject.Inject

/**
 * Observes the timestamp of the last successful full data synchronization.
 *
 * Emits null if no full sync has been performed yet.
 */
class ObserveLastFullSyncUseCase @Inject constructor(
    private val syncTimeRepository: SyncTimeRepository
) {

    operator fun invoke(): Flow<Instant?> {
        return syncTimeRepository.observeLastFullSync()
    }
}