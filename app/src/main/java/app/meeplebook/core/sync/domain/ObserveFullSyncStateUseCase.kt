package app.meeplebook.core.sync.domain

import app.meeplebook.core.sync.SyncTimeRepository
import app.meeplebook.core.sync.manager.SyncManager
import app.meeplebook.core.sync.model.SyncState
import app.meeplebook.core.sync.model.SyncType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

/**
 * Observes an app-level full-sync state derived from the collection and plays sync rows.
 *
 * `isSyncing` comes from the full-sync unique-work state exposed by [SyncManager], while
 * `lastSyncedAt` and `errorMessage` are still derived from the collection/plays persisted rows.
 * This keeps manual pull-to-refresh control separate from background sync status while preserving
 * collection-first error precedence and the "both timestamps required" full-sync time rule.
 */
class ObserveFullSyncStateUseCase @Inject constructor(
    private val syncTimeRepository: SyncTimeRepository,
    private val syncManager: SyncManager
) {
    operator fun invoke(): Flow<SyncState> {
        return combine(
            syncManager.observeFullSyncRunning(),
            syncTimeRepository.observeSyncState(SyncType.COLLECTION),
            syncTimeRepository.observeSyncState(SyncType.PLAYS)
        ) { isSyncing, collectionState, playsState ->
            SyncState(
                isSyncing = isSyncing,
                lastSyncedAt = listOfNotNull(
                    collectionState.lastSyncedAt,
                    playsState.lastSyncedAt
                ).takeIf { it.size == 2 }?.minOrNull(),
                errorMessage = collectionState.errorMessage ?: playsState.errorMessage
            )
        }
    }
}
