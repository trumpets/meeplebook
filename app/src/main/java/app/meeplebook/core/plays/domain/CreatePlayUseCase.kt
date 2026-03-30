package app.meeplebook.core.plays.domain

import app.meeplebook.core.plays.PlaysRepository
import app.meeplebook.core.plays.model.PlayError
import app.meeplebook.core.result.AppResult
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
    suspend operator fun invoke(command: CreatePlayCommand): AppResult<Unit, PlayError> {
        return try {
            playsRepository.createPlay(command)
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Failure(PlayError.Unknown(e))
        }
    }
}
