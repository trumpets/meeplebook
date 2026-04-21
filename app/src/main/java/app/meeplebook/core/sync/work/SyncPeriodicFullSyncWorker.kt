package app.meeplebook.core.sync.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import app.meeplebook.core.sync.manager.SyncManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Periodic orchestration worker that re-enqueues the full sync chain through [SyncManager].
 *
 * The actual sync execution remains in the existing per-domain workers. This worker only provides
 * a periodic WorkManager entry point for the already-centralized manager policy.
 */
@HiltWorker
class SyncPeriodicFullSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val syncManager: SyncManager
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        syncManager.enqueueFullSync()
        return Result.success()
    }
}
