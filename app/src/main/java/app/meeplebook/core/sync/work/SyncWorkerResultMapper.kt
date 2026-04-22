package app.meeplebook.core.sync.work

import androidx.work.ListenableWorker
import app.meeplebook.core.collection.model.CollectionError
import app.meeplebook.core.plays.model.PlayError
import app.meeplebook.core.result.AppResult
import app.meeplebook.core.sync.model.SyncUserDataError

/**
 * Maps repository/use-case sync results to WorkManager outcomes while keeping workers thin.
 */
internal fun AppResult<Unit, SyncUserDataError>.toSyncWorkerResult(): ListenableWorker.Result =
    when (this) {
        is AppResult.Success -> ListenableWorker.Result.success()
        is AppResult.Failure -> error.toWorkerResult()
    }

/**
 * Maps pending-play repository results to WorkManager outcomes.
 */
internal fun AppResult<Unit, PlayError>.toPendingPlaysWorkerResult(): ListenableWorker.Result =
    when (this) {
        is AppResult.Success -> ListenableWorker.Result.success()
        is AppResult.Failure -> error.toWorkerResult()
    }

private fun SyncUserDataError.toWorkerResult(): ListenableWorker.Result =
    when (this) {
        SyncUserDataError.NotLoggedIn -> ListenableWorker.Result.success()
        is SyncUserDataError.CollectionSyncFailed -> error.toWorkerResult()
        is SyncUserDataError.PlaysSyncFailed -> error.toWorkerResult()
    }

private fun CollectionError.toWorkerResult(): ListenableWorker.Result =
    when (this) {
        CollectionError.NetworkError -> ListenableWorker.Result.retry()
        is CollectionError.MaxRetriesExceeded -> ListenableWorker.Result.failure()
        CollectionError.NotLoggedIn -> ListenableWorker.Result.success()
        is CollectionError.Unknown -> ListenableWorker.Result.failure()
    }

private fun PlayError.toWorkerResult(): ListenableWorker.Result =
    when (this) {
        PlayError.NetworkError -> ListenableWorker.Result.retry()
        is PlayError.MaxRetriesExceeded -> ListenableWorker.Result.failure()
        PlayError.NotLoggedIn -> ListenableWorker.Result.success()
        is PlayError.Unknown -> ListenableWorker.Result.failure()
    }
