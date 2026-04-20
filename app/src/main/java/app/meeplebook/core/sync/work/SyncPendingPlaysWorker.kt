package app.meeplebook.core.sync.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import app.meeplebook.core.plays.PlaysRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Thin WorkManager worker that uploads locally queued pending or failed plays.
 */
@HiltWorker
class SyncPendingPlaysWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val playsRepository: PlaysRepository
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result =
        playsRepository.syncPendingPlays().toPendingPlaysWorkerResult()
}
