package app.meeplebook.core.plays

import app.meeplebook.core.collection.remote.CollectionRemoteDataSourceImpl
import app.meeplebook.core.network.RetryException
import app.meeplebook.core.plays.local.PlaysLocalDataSource
import app.meeplebook.core.plays.model.Play
import app.meeplebook.core.plays.model.PlayError
import app.meeplebook.core.plays.remote.PlaysFetchException
import app.meeplebook.core.plays.remote.PlaysRemoteDataSource
import app.meeplebook.core.result.AppResult
import kotlinx.coroutines.delay
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

    override fun observePlaysForGame(gameId: Int): Flow<List<Play>> {
        return local.observePlaysForGame(gameId)
    }

    override suspend fun getPlays(): List<Play> {
        return local.getPlays()
    }

    override suspend fun getPlaysForGame(gameId: Int): List<Play> {
        return local.getPlaysForGame(gameId)
    }

    companion object {
        /** Delay between requests to avoid rate limiting */
        private const val RATE_LIMIT_DELAY_MS = 5000L
    }

    override suspend fun syncPlays(username: String): AppResult<List<Play>, PlayError> {
        try {
            val allPlays = mutableListOf<Play>()
            var currentPage = 1
            var hasMorePages = true

            // Loop through all pages until no more plays are returned
            while (hasMorePages) {
                val plays = remote.fetchPlays(username, currentPage)
                
                if (plays.isEmpty()) {
                    hasMorePages = false
                } else {
                    allPlays.addAll(plays)
                    local.savePlays(plays)
                    
                    // BGG returns 100 plays per page
                    // If we got fewer than 100, we're on the last page
                    if (plays.size < 100) {
                        hasMorePages = false
                    } else {
                        currentPage++

                        // Wait between requests to avoid rate limiting
                        delay(RATE_LIMIT_DELAY_MS)
                    }
                }
            }
            
            return AppResult.Success(allPlays)
        } catch (e: Exception) {
            return when (e) {
                is IllegalArgumentException -> AppResult.Failure(PlayError.NotLoggedIn)
                is IOException -> AppResult.Failure(PlayError.NetworkError)
                is RetryException -> AppResult.Failure(PlayError.MaxRetriesExceeded(e))
                is PlaysFetchException -> AppResult.Failure(PlayError.Unknown(e))
                else -> AppResult.Failure(PlayError.Unknown(e))
            }
        }
    }

    override suspend fun clearPlays() {
        local.clearPlays()
    }
}
