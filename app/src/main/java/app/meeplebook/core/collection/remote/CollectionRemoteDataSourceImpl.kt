package app.meeplebook.core.collection.remote

import app.meeplebook.core.collection.model.CollectionItem
import app.meeplebook.core.collection.model.GameSubtype
import app.meeplebook.core.network.BggBaseUrl
import kotlinx.coroutines.delay
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
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
    okHttpClient: OkHttpClient,
    @BggBaseUrl bggBaseUrl: String
) : CollectionRemoteDataSource {

    private val api: BggApi = Retrofit.Builder()
        .baseUrl(bggBaseUrl)
        .client(okHttpClient)
        .build()
        .create(BggApi::class.java)

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
            subtype = "boardgameexpansion",
            subtypeOverride = GameSubtype.BOARDGAME_EXPANSION
        )

        return boardgames + expansions
    }

    /**
     * Fetches collection with retry logic for 202 responses.
     */
    private suspend fun fetchWithRetry(
        username: String,
        excludeSubtype: String? = null,
        subtype: String? = null,
        subtypeOverride: GameSubtype? = null
    ): List<CollectionItem> {
        var attempts = 0
        var delayMs = INITIAL_RETRY_DELAY_MS

        while (attempts < MAX_RETRY_ATTEMPTS) {
            val response = api.getCollection(
                username = username,
                excludeSubtype = excludeSubtype,
                subtype = subtype
            )

            when (response.code()) {
                200 -> {
                    val body = response.body()
                        ?: throw CollectionFetchException("Empty response body")
                    // Use 'use' to ensure the body is closed after reading
                    val xml = body.use { it.string() }
                    return CollectionXmlParser.parse(xml, subtypeOverride)
                }
                202 -> {
                    // BGG is preparing the data, retry after delay
                    attempts++
                    delay(delayMs)
                    delayMs = (delayMs * BACKOFF_MULTIPLIER).toLong().coerceAtMost(MAX_RETRY_DELAY_MS)
                }
                in 500..599 -> {
                    // Server error, likely rate limiting
                    throw CollectionFetchException("Server error: ${response.code()}")
                }
                else -> {
                    throw CollectionFetchException("Unexpected response: ${response.code()}")
                }
            }
        }

        throw CollectionFetchException("Max retry attempts exceeded while waiting for collection")
    }

    companion object {
        /** Initial delay between retry attempts in milliseconds */
        const val INITIAL_RETRY_DELAY_MS = 5000L

        /** Maximum delay between retry attempts in milliseconds */
        const val MAX_RETRY_DELAY_MS = 30000L

        /** Backoff multiplier for retry delays */
        const val BACKOFF_MULTIPLIER = 1.5

        /** Maximum number of retry attempts */
        const val MAX_RETRY_ATTEMPTS = 10

        /** Delay between requests to avoid rate limiting */
        const val RATE_LIMIT_DELAY_MS = 5000L
    }
}
