package app.meeplebook.core.sync.domain

import app.meeplebook.core.sync.SyncTimeRepository
import app.meeplebook.core.sync.model.SyncType
import java.time.Clock
import java.time.Duration
import java.time.Instant
import javax.inject.Inject

/**
 * Decides whether a screen-entry auto sync should run for [type].
 *
 * Manual pull-to-refresh and periodic/full-sync scheduling are intentionally outside this guard.
 * The safeguard applies only to screen navigation refreshes so quick tab switches do not enqueue
 * redundant work when the same domain synced recently.
 */
class ShouldAutoSyncOnScreenEnterUseCase @Inject constructor(
    private val syncTimeRepository: SyncTimeRepository,
    private val clock: Clock
) {
    suspend operator fun invoke(type: SyncType): Boolean {
        return invoke(listOf(type))
    }

    suspend operator fun invoke(vararg types: SyncType): Boolean {
        return invoke(types.toList())
    }

    private suspend fun invoke(types: List<SyncType>): Boolean {
        val states = types.map { type -> syncTimeRepository.getSyncState(type) }

        if (states.any { it.isSyncing }) {
            return false
        }

        val now = Instant.now(clock)
        return states.any { state ->
            val lastSyncedAt = state.lastSyncedAt ?: return@any true
            if (lastSyncedAt.isAfter(now)) {
                return@any true
            }

            Duration.between(lastSyncedAt, now) >= AUTO_SYNC_MIN_INTERVAL
        }
    }

    companion object {
        internal val AUTO_SYNC_MIN_INTERVAL: Duration = Duration.ofMinutes(15)
    }
}
