package app.meeplebook.core.sync.manager

import androidx.work.Operation
import kotlinx.coroutines.flow.Flow

/**
 * App-level orchestration boundary for WorkManager-backed sync.
 *
 * UI and lifecycle triggers should enqueue sync through this abstraction instead of constructing
 * worker requests directly. The actual trigger policy stays outside this interface.
 */
interface SyncManager {

    fun observeFullSyncRunning(): Flow<Boolean>

    /**
     * Enqueues the pending-play upload worker if one is not already queued or running.
     */
    fun enqueuePendingPlaysSync(): Operation

    /**
     * Enqueues the remote plays pull worker if one is not already queued or running.
     */
    fun enqueuePlaysSync(): Operation

    /**
     * Enqueues the collection pull worker if one is not already queued or running.
     */
    fun enqueueCollectionSync(): Operation

    /**
     * Enqueues the full sync chain in push-before-pull order:
     * pending plays -> plays pull -> collection pull.
     */
    fun enqueueFullSync(): Operation

    /**
     * Ensures a periodic full-sync trigger is scheduled.
     */
    fun schedulePeriodicFullSync(): Operation
}
