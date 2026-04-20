package app.meeplebook.core.sync.work

import androidx.work.ListenableWorker
import app.meeplebook.core.collection.model.CollectionError
import app.meeplebook.core.network.RetryException
import app.meeplebook.core.plays.model.PlayError
import app.meeplebook.core.result.AppResult
import app.meeplebook.core.sync.model.SyncUserDataError
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncWorkerResultMapperTest {

    @Test
    fun `sync worker result retries on collection network error`() {
        val result = AppResult.Failure(
            SyncUserDataError.CollectionSyncFailed(CollectionError.NetworkError)
        ).toSyncWorkerResult()

        assertTrue(result is ListenableWorker.Result.Retry)
    }

    @Test
    fun `sync worker result fails on collection max retries exceeded`() {
        val result = AppResult.Failure(
            SyncUserDataError.CollectionSyncFailed(
                CollectionError.MaxRetriesExceeded(
                    RetryException("retry", "user", 202, 10, 1_000L)
                )
            )
        ).toSyncWorkerResult()

        assertTrue(result is ListenableWorker.Result.Failure)
    }

    @Test
    fun `pending plays worker result fails on max retries exceeded`() {
        val result = AppResult.Failure(
            PlayError.MaxRetriesExceeded(
                RetryException("retry", "user", 202, 10, 1_000L)
            )
        ).toPendingPlaysWorkerResult()

        assertTrue(result is ListenableWorker.Result.Failure)
    }

    @Test
    fun `sync worker result succeeds when not logged in`() {
        val result = AppResult.Failure(SyncUserDataError.NotLoggedIn).toSyncWorkerResult()

        assertTrue(result is ListenableWorker.Result.Success)
    }
}
