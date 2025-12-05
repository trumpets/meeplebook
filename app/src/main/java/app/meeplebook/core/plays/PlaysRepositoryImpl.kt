package app.meeplebook.core.plays

import app.meeplebook.core.plays.model.Play
import app.meeplebook.core.plays.model.PlaysError
import app.meeplebook.core.plays.remote.BggPlaysRemoteDataSource
import app.meeplebook.core.result.AppResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.IOException
import javax.inject.Inject

/**
 * Implementation of [PlaysRepository] that fetches from remote
 * and caches in memory.
 */
class PlaysRepositoryImpl @Inject constructor(
    private val remote: BggPlaysRemoteDataSource
) : PlaysRepository {

    private val _cachedPlays = MutableStateFlow<List<Play>>(emptyList())

    companion object {
        private const val PLAYS_PER_PAGE = 100
    }

    override suspend fun getPlays(username: String, page: Int): AppResult<PlaysResponse, PlaysError> {
        return try {
            val response = remote.getPlays(username, page)

            // Update cache - for first page replace, otherwise append
            if (page == 1) {
                _cachedPlays.value = response.plays
            } else {
                _cachedPlays.value = _cachedPlays.value + response.plays
            }

            val hasMore = response.total > page * PLAYS_PER_PAGE

            AppResult.Success(
                PlaysResponse(
                    plays = response.plays,
                    totalPlays = response.total,
                    currentPage = response.page,
                    hasMorePages = hasMore
                )
            )
        } catch (e: Exception) {
            when (e) {
                is IllegalArgumentException -> AppResult.Failure(PlaysError.NotLoggedIn)
                is IOException -> AppResult.Failure(PlaysError.NetworkError)
                else -> AppResult.Failure(PlaysError.Unknown(e))
            }
        }
    }

    override fun observePlays(): Flow<List<Play>> {
        return _cachedPlays.asStateFlow()
    }
}
