package app.meeplebook.core.timer.domain

import app.meeplebook.core.timer.TimerRepository
import app.meeplebook.core.timer.model.ActivePlayTimer
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveActivePlayTimerUseCase @Inject constructor(
    private val timerRepository: TimerRepository,
) {
    operator fun invoke(): Flow<ActivePlayTimer> = timerRepository.observe()
}
