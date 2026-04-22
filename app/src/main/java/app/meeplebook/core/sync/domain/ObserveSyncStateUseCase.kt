package app.meeplebook.core.sync.domain

import app.meeplebook.core.sync.SyncTimeRepository
import app.meeplebook.core.sync.model.SyncState
import app.meeplebook.core.sync.model.SyncType
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Observes the persisted sync execution state for a single sync domain without additional mapping.
 */
class ObserveSyncStateUseCase @Inject constructor(
    private val syncTimeRepository: SyncTimeRepository
) {
    operator fun invoke(type: SyncType): Flow<SyncState> {
        return syncTimeRepository.observeSyncState(type)
    }
}
