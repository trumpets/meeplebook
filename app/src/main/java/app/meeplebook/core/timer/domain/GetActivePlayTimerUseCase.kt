package app.meeplebook.core.timer.domain

import app.meeplebook.core.timer.TimerRepository
import app.meeplebook.core.timer.model.ActivePlayTimer
import javax.inject.Inject

class GetActivePlayTimerUseCase @Inject constructor(
    private val timerRepository: TimerRepository,
) {
    suspend operator fun invoke(): ActivePlayTimer = timerRepository.get()
}
