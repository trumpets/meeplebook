package app.meeplebook.core.sync.model

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncExtensionsTest {

    @Test
    fun `observeRefreshCompletion waits for syncing to return false`() = runTest {
        val states = MutableStateFlow(SyncState(isSyncing = false))
        var completionCount = 0

        val job = states.observeRefreshCompletion(this, timeoutMs = 1_000) {
            completionCount++
        }

        runCurrent()
        assertEquals(0, completionCount)

        states.value = SyncState(isSyncing = true)
        runCurrent()
        assertEquals(0, completionCount)

        states.value = SyncState(isSyncing = false)
        runCurrent()

        assertEquals(1, completionCount)
        assertTrue(job.isCompleted)
    }

    @Test
    fun `observeRefreshCompletion completes on timeout when sync never starts`() = runTest {
        val states = MutableStateFlow(SyncState(isSyncing = false))
        var completionCount = 0

        val job = states.observeRefreshCompletion(this, timeoutMs = 1_000) {
            completionCount++
        }

        runCurrent()
        assertEquals(0, completionCount)

        advanceTimeBy(1_000)
        runCurrent()

        assertEquals(1, completionCount)
        assertTrue(job.isCompleted)
    }
}
