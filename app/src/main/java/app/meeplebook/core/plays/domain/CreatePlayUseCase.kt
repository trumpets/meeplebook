package app.meeplebook.core.plays.domain

import app.meeplebook.core.plays.PlaysRepository
import javax.inject.Inject

/**
 * Creates (persists) a new play locally and schedules a BGG sync.
 *
 * Delegates directly to [PlaysRepository.createPlay]; the repository owns
 * the local-first write and sync scheduling logic.
 *
 * @param command The fully-constructed play command.
 */
class CreatePlayUseCase @Inject constructor(
    private val playsRepository: PlaysRepository
) {
    suspend operator fun invoke(command: CreatePlayCommand) =
        playsRepository.createPlay(command)
}
