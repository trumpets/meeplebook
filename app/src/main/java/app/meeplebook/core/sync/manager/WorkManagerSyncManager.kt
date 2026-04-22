package app.meeplebook.core.sync.manager

import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.Operation
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import app.meeplebook.core.sync.work.SyncCollectionWorker
import app.meeplebook.core.sync.work.SyncPendingPlaysWorker
import app.meeplebook.core.sync.work.SyncPeriodicFullSyncWorker
import app.meeplebook.core.sync.work.SyncPlaysWorker
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * WorkManager-backed implementation of [SyncManager].
 *
 * This class owns the unique work names, constraints, and full-sync chain ordering so future
 * trigger points can enqueue sync without reconstructing WorkManager details.
 */
class WorkManagerSyncManager @Inject constructor(
    private val workManager: WorkManager
) : SyncManager {

    override fun enqueuePendingPlaysSync(): Operation =
        enqueueUniqueWork(
            uniqueWorkName = SyncWorkNames.PENDING_PLAYS,
            request = buildOneTimeRequest<SyncPendingPlaysWorker>(SyncWorkNames.PENDING_PLAYS)
        )

    override fun enqueuePlaysSync(): Operation =
        enqueueUniqueWork(
            uniqueWorkName = SyncWorkNames.PLAYS,
            request = buildOneTimeRequest<SyncPlaysWorker>(SyncWorkNames.PLAYS)
        )

    override fun enqueueCollectionSync(): Operation =
        enqueueUniqueWork(
            uniqueWorkName = SyncWorkNames.COLLECTION,
            request = buildOneTimeRequest<SyncCollectionWorker>(SyncWorkNames.COLLECTION)
        )

    override fun enqueueFullSync(): Operation {
        val pendingPlays = buildOneTimeRequest<SyncPendingPlaysWorker>(SyncWorkNames.PENDING_PLAYS)
        val plays = buildOneTimeRequest<SyncPlaysWorker>(SyncWorkNames.PLAYS)
        val collection = buildOneTimeRequest<SyncCollectionWorker>(SyncWorkNames.COLLECTION)

        return workManager
            .beginUniqueWork(
                SyncWorkNames.FULL_SYNC,
                ExistingWorkPolicy.KEEP,
                pendingPlays
            )
            .then(plays)
            .then(collection)
            .enqueue()
    }

    override fun schedulePeriodicFullSync(): Operation =
        workManager.enqueueUniquePeriodicWork(
            SyncWorkNames.PERIODIC_FULL_SYNC,
            ExistingPeriodicWorkPolicy.KEEP,
            buildPeriodicRequest<SyncPeriodicFullSyncWorker>(SyncWorkNames.PERIODIC_FULL_SYNC)
        )

    private fun enqueueUniqueWork(
        uniqueWorkName: String,
        request: OneTimeWorkRequest
    ): Operation =
        workManager.enqueueUniqueWork(uniqueWorkName, ExistingWorkPolicy.KEEP, request)
}

internal object SyncWorkNames {
    const val FULL_SYNC = "sync-full"
    const val PERIODIC_FULL_SYNC = "sync-full-periodic"
    const val PENDING_PLAYS = "sync-pending-plays"
    const val PLAYS = "sync-plays"
    const val COLLECTION = "sync-collection"
}

private const val FULL_SYNC_REPEAT_INTERVAL_HOURS = 24L

private val syncConstraints: Constraints =
    Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

private inline fun <reified T : ListenableWorker> buildOneTimeRequest(
    tag: String
): OneTimeWorkRequest =
    OneTimeWorkRequestBuilder<T>()
        .setConstraints(syncConstraints)
        .addTag(tag)
        .build()

private inline fun <reified T : ListenableWorker> buildPeriodicRequest(
    tag: String
): PeriodicWorkRequest =
    PeriodicWorkRequestBuilder<T>(
        FULL_SYNC_REPEAT_INTERVAL_HOURS,
        TimeUnit.HOURS
    )
        .setConstraints(syncConstraints)
        .addTag(tag)
        .build()
