package app.meeplebook.core.sync.work

import androidx.hilt.work.HiltWorkerFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import app.meeplebook.core.sync.manager.SyncManager
import app.meeplebook.core.sync.manager.SyncManagerModule
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
@UninstallModules(SyncManagerModule::class)
@RunWith(AndroidJUnit4::class)
class SyncPeriodicFullSyncWorkerTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    private val fakeSyncManager = FakeWorkerSyncManager()

    @BindValue
    @JvmField
    val syncManager: SyncManager = fakeSyncManager

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun doWork_enqueuesFullSyncAndReturnsSuccess() = runTest {
        val result = buildWorker().doWork()

        assertTrue(result is ListenableWorker.Result.Success)
        assertEquals(1, fakeSyncManager.fullSyncEnqueueCount)
    }

    private fun buildWorker(): SyncPeriodicFullSyncWorker =
        TestListenableWorkerBuilder<SyncPeriodicFullSyncWorker>(
            ApplicationProvider.getApplicationContext()
        )
            .setWorkerFactory(workerFactory)
            .build()
}
