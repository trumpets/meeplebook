package app.meeplebook.core.timer.domain

import app.meeplebook.core.timer.TimerRepository
import javax.inject.Inject

class ResumePlayTimerUseCase @Inject constructor(
    private val timerRepository: TimerRepository,
) {
    suspend operator fun invoke() {
        timerRepository.resume()
    }
}
