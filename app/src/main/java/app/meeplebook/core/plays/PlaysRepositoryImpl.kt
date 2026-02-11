package app.meeplebook.core.plays

import app.meeplebook.core.database.entity.toEntity
import app.meeplebook.core.network.RetryException
import app.meeplebook.core.plays.domain.CreatePlayCommand
import app.meeplebook.core.plays.local.PlaysLocalDataSource
import app.meeplebook.core.plays.model.Play
import app.meeplebook.core.plays.model.PlayError
import app.meeplebook.core.plays.remote.PlaysFetchException
import app.meeplebook.core.plays.remote.PlaysRemoteDataSource
import app.meeplebook.core.result.AppResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import java.io.IOException
import java.time.Instant
import javax.inject.Inject

/**
 * Implementation of [PlaysRepository].
 */
class PlaysRepositoryImpl @Inject constructor(
    private val local: PlaysLocalDataSource,
    private val remote: PlaysRemoteDataSource
) : PlaysRepository {

    override fun observePlays(gameNameOrLocationQuery: String?): Flow<List<Play>> {
        val query = gameNameOrLocationQuery?.trim()
        return if (query.isNullOrEmpty()) {
            local.observePlays()
        } else {
            local.observePlaysByGameNameOrLocation(query)
        }
    }

    override fun observePlaysForGame(gameId: Long): Flow<List<Play>> {
        return local.observePlaysForGame(gameId)
    }

    override suspend fun getPlays(): List<Play> {
        return local.getPlays()
    }

    override suspend fun getPlaysForGame(gameId: Long): List<Play> {
        return local.getPlaysForGame(gameId)
    }

    companion object {
        /** Delay between requests to avoid rate limiting */
        private const val RATE_LIMIT_DELAY_MS = 5000L
    }

    override suspend fun syncPlays(username: String): AppResult<Unit, PlayError> {
        try {
            val allRemoteIds = mutableListOf<Long>()
            var currentPage = 1
            var hasMorePages = true

            // Loop through all pages until no more plays are returned
            while (hasMorePages) {
                val plays = remote.fetchPlays(username, currentPage)
                
                if (plays.isEmpty()) {
                    hasMorePages = false
                } else {
                    local.saveRemotePlays(remotePlays = plays)

                    allRemoteIds += plays.map { it.remoteId }
                    
                    // BGG returns 100 plays per page
                    // If we got fewer than 100, we're on the last page
                    if (plays.size < 100) {
                        hasMorePages = false
                    } else {
                        currentPage++

                        // Wait between requests to avoid rate limiting
                        delay(timeMillis = RATE_LIMIT_DELAY_MS)
                    }
                }
            }

            // After all pages fetched -> reconcile deletions
            local.retainByRemoteIds(allRemoteIds)

            return AppResult.Success(Unit)
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

    override suspend fun createPlay(command: CreatePlayCommand) {
        local.insertPlay(
            playEntity = command.toEntity(),
            playerEntities = command.players.map { it.toEntity() }
        )
    }

    override suspend fun clearPlays() {
        local.clearPlays()
    }

    override fun observeTotalPlaysCount(): Flow<Long> {
        return local.observeTotalPlaysCount()
    }

    override fun observePlaysCountForPeriod(start: Instant, end: Instant): Flow<Long> {
        return local.observePlaysCountForMonth(start, end)
    }

    override fun observeRecentPlays(limit: Int): Flow<List<Play>> {
        return local.observeRecentPlays(limit)
    }

    override fun observeUniqueGamesCount(): Flow<Long> {
        return local.observeUniqueGamesCount()
    }

    override fun observeLocations(query: String): Flow<List<String>> {
        return local.observeLocations(query)
    }

    override fun observeRecentLocations(): Flow<List<String>> {
        return local.observeRecentLocations()
    }
}
