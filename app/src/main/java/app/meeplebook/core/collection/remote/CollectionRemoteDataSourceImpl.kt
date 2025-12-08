package app.meeplebook.core.collection.remote

import app.meeplebook.core.collection.model.CollectionItem
import app.meeplebook.core.network.BggApi
import app.meeplebook.core.network.RetryException
import app.meeplebook.core.network.RetrySignal
import app.meeplebook.core.network.retryWithBackoff
import kotlinx.coroutines.delay
import java.io.IOException
import javax.inject.Inject

/**
 * Implementation of [CollectionRemoteDataSource] that fetches collections from BGG.
 *
 * Handles:
 * - 202 responses with retry logic (BGG queuing)
 * - Rate limiting with delays between requests
 * - Fetching both boardgames and expansions separately
 */
class CollectionRemoteDataSourceImpl @Inject constructor(
    private val api: BggApi
) : CollectionRemoteDataSource {

    companion object {
        /** Delay between requests to avoid rate limiting */
        private const val RATE_LIMIT_DELAY_MS = 5000L
    }

    override suspend fun fetchCollection(username: String): List<CollectionItem> {
        if (username.isBlank()) {
            throw IllegalArgumentException("Username must not be empty")
        }

        // Fetch boardgames (excluding expansions)
        val boardgames = fetchWithRetry(username, excludeSubtype = "boardgameexpansion")

        // Wait between requests to avoid rate limiting
        delay(RATE_LIMIT_DELAY_MS)

        // Fetch expansions
        val expansions = fetchWithRetry(
            username = username,
            subtype = "boardgameexpansion"
        )

        return boardgames + expansions
    }

    /**
     * Fetches collection with retry logic for 202 responses.
     */
    private suspend fun fetchWithRetry(
        username: String,
        excludeSubtype: String? = null,
        subtype: String? = null
    ): List<CollectionItem> {

        return retryWithBackoff(
            username = username
        ) { attempt ->
            val response = api.getCollection(
                username = username,
                excludeSubtype = excludeSubtype,
                subtype = subtype
            )

            val code = response.code()

            if (code == 202 || code == 429 || code in 500..599) {
                response.body()?.close()
                throw RetrySignal(code)
            }

            if (code != 200) {
                response.body()?.close()
                throw RetryException(
                    message = "Unexpected HTTP $code",
                    username = username,
                    lastHttpCode = code,
                    attempts = attempt,
                    lastDelayMs = 0L
                )
            }

            val body = response.body()
                ?: throw IOException("Empty response body on HTTP $code")

            // STREAM reader instead of body.string()
            body.charStream().use { reader ->

                // Check for disguised queued response BEFORE parsing
                val peek = reader.buffered(2048).readText()

                if (peek.contains("totalitems=\"0\"") && !peek.contains("<item ")) {
                    throw RetrySignal(code)
                }

                // Need a fresh Reader for actual parsing
                return@retryWithBackoff CollectionXmlParser.parse(peek.reader())
            }
        }
    }
}
