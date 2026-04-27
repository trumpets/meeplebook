package app.meeplebook.core.timer.service

import app.meeplebook.core.timer.FakeTimerRepository
import app.meeplebook.core.timer.domain.ObserveActivePlayTimerUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class PlayTimerServiceCoordinatorTest {
    @Test
    fun `coordinator stops service when timer is inactive`() = runTest {
        val timerRepository = FakeTimerRepository { Instant.parse("2026-03-01T10:00:00Z") }
        val serviceController = FakePlayTimerServiceController()
        val scope = TestScope(StandardTestDispatcher(testScheduler))

        PlayTimerServiceCoordinator(
            observeActivePlayTimer = ObserveActivePlayTimerUseCase(timerRepository),
            serviceController = serviceController,
            applicationScope = scope,
        )
        scope.advanceUntilIdle()

        Assert.assertEquals(1, serviceController.stopCount)
        scope.cancel()
    }

    @Test
    fun `coordinator starts service when timer becomes active`() = runTest {
        val timerRepository = FakeTimerRepository { Instant.parse("2026-03-01T10:00:00Z") }
        val serviceController = FakePlayTimerServiceController()
        val scope = TestScope(StandardTestDispatcher(testScheduler))

        PlayTimerServiceCoordinator(
            observeActivePlayTimer = ObserveActivePlayTimerUseCase(timerRepository),
            serviceController = serviceController,
            applicationScope = scope,
        )
        scope.advanceUntilIdle()

        timerRepository.start(playId = null)
        scope.advanceUntilIdle()

        Assert.assertEquals(1, serviceController.startCount)
        scope.cancel()
    }

    private class FakePlayTimerServiceController : PlayTimerServiceController {
        var startCount: Int = 0
        var stopCount: Int = 0

        override fun ensureServiceRunning() {
            startCount++
        }

        override fun stopService() {
            stopCount++
        }
    }
}