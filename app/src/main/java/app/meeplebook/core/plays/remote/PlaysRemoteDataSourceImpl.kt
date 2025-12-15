package app.meeplebook.core.plays.remote

import app.meeplebook.core.network.BggApi
import app.meeplebook.core.network.RetrySignal
import app.meeplebook.core.network.retryWithBackoff
import app.meeplebook.core.plays.model.Play
import java.io.IOException
import javax.inject.Inject

/**
 * Implementation of [PlaysRemoteDataSource] that fetches plays from BGG.
 *
 * Handles:
 * - 202 responses with retry logic (BGG queuing)
 * - Rate limiting with delays between requests
 * - Pagination support
 */
class PlaysRemoteDataSourceImpl @Inject constructor(
    private val api: BggApi
) : PlaysRemoteDataSource {

    override suspend fun fetchPlays(username: String, page: Int?): List<Play> {
        if (username.isBlank()) {
            throw IllegalArgumentException("Username must not be empty")
        }

        return fetchWithRetry(username, page)
    }

    /**
     * Fetches plays with retry logic for 202 responses.
     */
    private suspend fun fetchWithRetry(
        username: String,
        page: Int?
    ): List<Play> {

        return retryWithBackoff(
            username = username
        ) { attempt ->
            val response = api.getPlays(
                username = username,
                type = "thing",
                page = page
            )

            val code = response.code()

            if (code == 202 || code == 429 || code in 500..599) {
                response.body()?.close()
                throw RetrySignal(code)
            }

            if (code != 200) {
                response.body()?.close()
                throw PlaysFetchException(message = "Unexpected HTTP $code")
            }

            val body = response.body()
                ?: throw IOException("Empty response body on HTTP $code")

            // STREAM reader instead of body.string()
            body.charStream().use { reader ->
                return@retryWithBackoff PlaysXmlParser.parse(reader)
            }
        }
    }
}
