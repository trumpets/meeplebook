package app.meeplebook.core.timer

import app.meeplebook.core.timer.model.ActivePlayTimer
import kotlinx.coroutines.flow.Flow

/**
 * Repository for the single persisted global play timer.
 */
interface TimerRepository {
    /**
     * Observes the current timer state.
     */
    fun observe(): Flow<ActivePlayTimer>

    /**
     * Reads the current timer state.
     */
    suspend fun get(): ActivePlayTimer

    /**
     * Starts a new timer from zero for [playId].
     */
    suspend fun start(playId: Long?)

    /**
     * Pauses the running timer.
     */
    suspend fun pause()

    /**
     * Resumes a paused timer.
     */
    suspend fun resume()

    /**
     * Resets the timer to zero and starts it immediately.
     */
    suspend fun reset()
}
