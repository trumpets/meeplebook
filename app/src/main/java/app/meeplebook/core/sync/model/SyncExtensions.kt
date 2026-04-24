package app.meeplebook.core.sync.model

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Watches a user-initiated refresh against an existing sync-state flow and invokes
 * [onRefreshComplete] once the flow transitions from syncing back to idle.
 *
 * The helper intentionally ignores leading `false` emissions so background/app-start syncs do not
 * auto-complete a refresh indicator that the user never started.
 *
 * Callers should start collecting before enqueuing work to avoid missing the transition,
 * though the timeout guards against that race.
 *
 * If no true→false transition is observed within [timeoutMs],
 * [onRefreshComplete] is invoked anyway to avoid leaving the refresh indicator stuck.
 */
fun Flow<SyncState>.observeRefreshCompletion(
    scope: CoroutineScope,
    timeoutMs: Long = 60_000 * 2,
    onRefreshComplete: () -> Unit
): Job {
    return scope.launch {
        withTimeoutOrNull(timeoutMs) {
            map { it.isSyncing }
                .distinctUntilChanged()
                .dropWhile { !it }      // skip leading false (background/app-start syncs)
                .first { !it }          // suspend until the single true→false transition, then done
        }
        onRefreshComplete()
    }
}
