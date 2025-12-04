package app.meeplebook.core.plays.domain

import app.meeplebook.core.plays.PlaysRepository
import app.meeplebook.core.plays.PlaysResponse
import app.meeplebook.core.plays.model.PlaysError
import app.meeplebook.core.result.AppResult
import javax.inject.Inject

/**
 * Use case that fetches the user's board game plays.
 */
class GetPlaysUseCase @Inject constructor(
    private val playsRepository: PlaysRepository
) {

    /**
     * Fetches plays for the given username with pagination.
     *
     * @param username BGG username
     * @param page Page number (1-indexed)
     * @return Result containing plays response or error
     */
    suspend operator fun invoke(username: String, page: Int = 1): AppResult<PlaysResponse, PlaysError> {
        if (username.isBlank()) {
            return AppResult.Failure(PlaysError.NotLoggedIn)
        }
        return playsRepository.getPlays(username, page)
    }
}
