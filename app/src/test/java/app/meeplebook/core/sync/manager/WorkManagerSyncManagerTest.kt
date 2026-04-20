package app.meeplebook.core.sync.manager

import androidx.work.ExistingWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.Operation
import androidx.work.WorkContinuation
import androidx.work.WorkManager
import app.meeplebook.core.sync.work.SyncCollectionWorker
import app.meeplebook.core.sync.work.SyncPendingPlaysWorker
import app.meeplebook.core.sync.work.SyncPlaysWorker
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class WorkManagerSyncManagerTest {

    private val workManager = mockk<WorkManager>()
    private lateinit var subject: SyncManager

    @Before
    fun setUp() {
        subject = WorkManagerSyncManager(workManager)
    }

    @Test
    fun enqueuePendingPlaysSync_enqueuesUniquePendingPlaysWork() {
        val requestSlot = slot<OneTimeWorkRequest>()
        val operation = mockk<Operation>()
        every {
            workManager.enqueueUniqueWork(
                SyncWorkNames.PENDING_PLAYS,
                ExistingWorkPolicy.KEEP,
                capture(requestSlot)
            )
        } returns operation

        val result = subject.enqueuePendingPlaysSync()

        assertSame(operation, result)
        assertWorkRequest<SyncPendingPlaysWorker>(
            request = requestSlot.captured,
            expectedTag = SyncWorkNames.PENDING_PLAYS
        )
    }

    @Test
    fun enqueuePlaysSync_enqueuesUniquePlaysWork() {
        val requestSlot = slot<OneTimeWorkRequest>()
        val operation = mockk<Operation>()
        every {
            workManager.enqueueUniqueWork(
                SyncWorkNames.PLAYS,
                ExistingWorkPolicy.KEEP,
                capture(requestSlot)
            )
        } returns operation

        val result = subject.enqueuePlaysSync()

        assertSame(operation, result)
        assertWorkRequest<SyncPlaysWorker>(
            request = requestSlot.captured,
            expectedTag = SyncWorkNames.PLAYS
        )
    }

    @Test
    fun enqueueCollectionSync_enqueuesUniqueCollectionWork() {
        val requestSlot = slot<OneTimeWorkRequest>()
        val operation = mockk<Operation>()
        every {
            workManager.enqueueUniqueWork(
                SyncWorkNames.COLLECTION,
                ExistingWorkPolicy.KEEP,
                capture(requestSlot)
            )
        } returns operation

        val result = subject.enqueueCollectionSync()

        assertSame(operation, result)
        assertWorkRequest<SyncCollectionWorker>(
            request = requestSlot.captured,
            expectedTag = SyncWorkNames.COLLECTION
        )
    }

    @Test
    fun enqueueFullSync_chainsPendingPlaysThenPlaysThenCollection() {
        val pendingSlot = slot<OneTimeWorkRequest>()
        val playsSlot = slot<OneTimeWorkRequest>()
        val collectionSlot = slot<OneTimeWorkRequest>()
        val firstContinuation = mockk<WorkContinuation>()
        val secondContinuation = mockk<WorkContinuation>()
        val thirdContinuation = mockk<WorkContinuation>()
        val operation = mockk<Operation>()

        every {
            workManager.beginUniqueWork(
                SyncWorkNames.FULL_SYNC,
                ExistingWorkPolicy.KEEP,
                capture(pendingSlot)
            )
        } returns firstContinuation
        every { firstContinuation.then(capture(playsSlot)) } returns secondContinuation
        every { secondContinuation.then(capture(collectionSlot)) } returns thirdContinuation
        every { thirdContinuation.enqueue() } returns operation

        val result = subject.enqueueFullSync()

        assertSame(operation, result)
        assertWorkRequest<SyncPendingPlaysWorker>(
            request = pendingSlot.captured,
            expectedTag = SyncWorkNames.PENDING_PLAYS
        )
        assertWorkRequest<SyncPlaysWorker>(
            request = playsSlot.captured,
            expectedTag = SyncWorkNames.PLAYS
        )
        assertWorkRequest<SyncCollectionWorker>(
            request = collectionSlot.captured,
            expectedTag = SyncWorkNames.COLLECTION
        )
    }
}

private inline fun <reified T : ListenableWorker> assertWorkRequest(
    request: OneTimeWorkRequest,
    expectedTag: String
) {
    assertEquals(T::class.java.name, request.workSpec.workerClassName)
    assertEquals(NetworkType.CONNECTED, request.workSpec.constraints.requiredNetworkType)
    assertTrue(request.tags.contains(expectedTag))
}
