package app.meeplebook.core.sync.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import app.meeplebook.core.sync.domain.SyncPlaysUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Thin WorkManager worker that runs the auth-gated remote plays pull sync.
 */
@HiltWorker
class SyncPlaysWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val syncPlaysUseCase: SyncPlaysUseCase
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result =
        syncPlaysUseCase.invoke().toSyncWorkerResult()
}
