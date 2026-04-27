package app.meeplebook.core.timer

import app.meeplebook.core.timer.domain.PlayTimerStateMachine
import app.meeplebook.core.timer.model.ActivePlayTimer
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Fake implementation of [TimerRepository] for unit tests.
 */
class FakeTimerRepository(
    private var nowProvider: () -> Instant = { Instant.parse("2026-03-01T10:00:00Z") },
) : TimerRepository {
    private val state = MutableStateFlow(ActivePlayTimer())

    var startCallCount: Int = 0
        private set
    var pauseCallCount: Int = 0
        private set
    var resumeCallCount: Int = 0
        private set
    var resetCallCount: Int = 0
        private set

    override fun observe(): Flow<ActivePlayTimer> = state

    override suspend fun get(): ActivePlayTimer = state.value

    override suspend fun start(playId: Long?) {
        startCallCount++
        state.value = PlayTimerStateMachine.start(playId, nowProvider())
    }

    override suspend fun pause() {
        pauseCallCount++
        state.value = PlayTimerStateMachine.pause(state.value, nowProvider())
    }

    override suspend fun resume() {
        resumeCallCount++
        state.value = PlayTimerStateMachine.resume(state.value, nowProvider())
    }

    override suspend fun reset() {
        resetCallCount++
        state.value = PlayTimerStateMachine.reset(state.value, nowProvider())
    }

    fun setTimer(timer: ActivePlayTimer) {
        state.value = timer
    }

    fun setNowProvider(nowProvider: () -> Instant) {
        this.nowProvider = nowProvider
    }
}
