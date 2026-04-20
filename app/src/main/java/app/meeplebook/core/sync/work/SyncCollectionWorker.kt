package app.meeplebook.core.sync.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import app.meeplebook.core.sync.domain.SyncCollectionUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Thin WorkManager worker that runs the auth-gated collection pull sync.
 */
@HiltWorker
class SyncCollectionWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val syncCollectionUseCase: SyncCollectionUseCase
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result =
        syncCollectionUseCase.invoke().toSyncWorkerResult()
}
