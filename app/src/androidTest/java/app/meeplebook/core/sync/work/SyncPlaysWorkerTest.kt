package app.meeplebook.core.sync.work

import androidx.hilt.work.HiltWorkerFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import app.meeplebook.core.auth.AuthModule
import app.meeplebook.core.auth.AuthRepository
import app.meeplebook.core.model.AuthCredentials
import app.meeplebook.core.plays.PlaysModule
import app.meeplebook.core.plays.PlaysRepository
import app.meeplebook.core.plays.model.PlayError
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
@UninstallModules(AuthModule::class, PlaysModule::class)
@RunWith(AndroidJUnit4::class)
class SyncPlaysWorkerTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    private val fakeAuthRepository = FakeWorkerAuthRepository()
    private val fakePlaysRepository = FakeWorkerPlaysRepository()

    @BindValue
    @JvmField
    val authRepository: AuthRepository = fakeAuthRepository

    @BindValue
    @JvmField
    val playsRepository: PlaysRepository = fakePlaysRepository

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun doWork_returnsSuccess_whenPlaysSyncSucceeds() = runTest {
        fakeAuthRepository.currentUser = AuthCredentials("testuser", "password")
        fakePlaysRepository.syncPlaysResult = AppResult.Success(Unit)

        val result = buildWorker().doWork()

        assertTrue(result is ListenableWorker.Result.Success)
        assertEquals(1, fakePlaysRepository.syncPlaysCallCount)
    }

    @Test
    fun doWork_returnsSuccess_whenNotLoggedIn() = runTest {
        val result = buildWorker().doWork()

        assertTrue(result is ListenableWorker.Result.Success)
    }

    @Test
    fun doWork_returnsFailure_whenPlaysMaxRetriesExceeded() = runTest {
        fakeAuthRepository.currentUser = AuthCredentials("testuser", "password")
        fakePlaysRepository.syncPlaysResult =
            AppResult.Failure(
                PlayError.MaxRetriesExceeded(
                    app.meeplebook.core.network.RetryException("retry", "user", 202, 10, 1_000L)
                )
            )

        val result = buildWorker().doWork()

        assertTrue(result is ListenableWorker.Result.Failure)
    }

    private fun buildWorker(): SyncPlaysWorker =
        TestListenableWorkerBuilder.from(
            ApplicationProvider.getApplicationContext(),
            SyncPlaysWorker::class.java
        )
            .setWorkerFactory(workerFactory)
            .build()
}
