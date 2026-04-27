package app.meeplebook.core.timer.domain

import app.meeplebook.core.timer.TimerRepository
import javax.inject.Inject

class StartPlayTimerUseCase @Inject constructor(
    private val timerRepository: TimerRepository,
) {
    suspend operator fun invoke(playId: Long?) {
        timerRepository.start(playId)
    }
}
