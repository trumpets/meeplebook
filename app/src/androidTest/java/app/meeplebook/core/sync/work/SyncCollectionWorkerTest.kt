package app.meeplebook.core.sync.work

import androidx.hilt.work.HiltWorkerFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import app.meeplebook.core.auth.AuthModule
import app.meeplebook.core.auth.AuthRepository
import app.meeplebook.core.auth.local.AuthLocalDataSource
import app.meeplebook.core.collection.CollectionModule
import app.meeplebook.core.collection.CollectionRepository
import app.meeplebook.core.collection.model.CollectionError
import app.meeplebook.core.model.AuthCredentials
import app.meeplebook.core.result.AppResult
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import javax.inject.Inject
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@UninstallModules(AuthModule::class, CollectionModule::class)
@RunWith(AndroidJUnit4::class)
class SyncCollectionWorkerTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    private val fakeAuthRepository = FakeWorkerAuthRepository()
    private val fakeAuthLocalDataSource = FakeWorkerAuthLocalDataSource()
    private val fakeCollectionRepository = FakeWorkerCollectionRepository()

    @BindValue
    @JvmField
    val authRepository: AuthRepository = fakeAuthRepository

    @BindValue
    @JvmField
    val authLocalDataSource: AuthLocalDataSource = fakeAuthLocalDataSource

    @BindValue
    @JvmField
    val collectionRepository: CollectionRepository = fakeCollectionRepository

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun doWork_returnsSuccess_whenCollectionSyncSucceeds() = runTest {
        fakeAuthRepository.currentUser = AuthCredentials("testuser", "password")
        fakeCollectionRepository.syncResult = AppResult.Success(emptyList())

        val result = buildWorker().doWork()

        assertTrue(result is ListenableWorker.Result.Success)
        assertEquals(1, fakeCollectionRepository.syncCallCount)
    }

    @Test
    fun doWork_returnsFailure_whenCollectionMaxRetriesExceeded() = runTest {
        fakeAuthRepository.currentUser = AuthCredentials("testuser", "password")
        fakeCollectionRepository.syncResult =
            AppResult.Failure(
                CollectionError.MaxRetriesExceeded(
                    app.meeplebook.core.network.RetryException("retry", "user", 202, 10, 1_000L)
                )
            )

        val result = buildWorker().doWork()

        assertTrue(result is ListenableWorker.Result.Failure)
    }

    @Test
    fun doWork_returnsRetry_whenCollectionSyncFailsWithNetworkError() = runTest {
        fakeAuthRepository.currentUser = AuthCredentials("testuser", "password")
        fakeCollectionRepository.syncResult = AppResult.Failure(CollectionError.NetworkError)

        val result = buildWorker().doWork()

        assertTrue(result is ListenableWorker.Result.Retry)
    }

    @Test
    fun doWork_returnsSuccess_whenNotLoggedIn() = runTest {
        val result = buildWorker().doWork()

        assertTrue(result is ListenableWorker.Result.Success)
        assertEquals(0, fakeCollectionRepository.syncCallCount)
    }

    private fun buildWorker(): SyncCollectionWorker =
        TestListenableWorkerBuilder<SyncCollectionWorker>(
            ApplicationProvider.getApplicationContext()
        )
            .setWorkerFactory(workerFactory)
            .build()
}
