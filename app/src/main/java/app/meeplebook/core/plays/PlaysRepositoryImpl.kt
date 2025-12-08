package app.meeplebook.core.plays

import app.meeplebook.core.network.RetryException
import app.meeplebook.core.plays.local.PlaysLocalDataSource
import app.meeplebook.core.plays.model.Play
import app.meeplebook.core.plays.model.PlayError
import app.meeplebook.core.plays.remote.PlaysRemoteDataSource
import app.meeplebook.core.result.AppResult
import kotlinx.coroutines.flow.Flow
import java.io.IOException
import javax.inject.Inject

/**
 * Implementation of [PlaysRepository].
 */
class PlaysRepositoryImpl @Inject constructor(
    private val local: PlaysLocalDataSource,
    private val remote: PlaysRemoteDataSource
) : PlaysRepository {

    override fun observePlays(): Flow<List<Play>> {
        return local.observePlays()
    }

    override suspend fun getPlays(): List<Play> {
        return local.getPlays()
    }

    override suspend fun syncPlays(username: String, page: Int?): AppResult<List<Play>, PlayError> {
        try {
            val plays = remote.fetchPlays(username, page)
            local.savePlays(plays)
            return AppResult.Success(plays)
        } catch (e: Exception) {
            return when (e) {
                is IllegalArgumentException -> AppResult.Failure(PlayError.NotLoggedIn)
                is IOException -> AppResult.Failure(PlayError.NetworkError)
                is RetryException -> AppResult.Failure(PlayError.MaxRetriesExceeded(e))
                else -> AppResult.Failure(PlayError.Unknown(e))
            }
        }
    }

    override suspend fun clearPlays() {
        local.clearPlays()
    }
}
