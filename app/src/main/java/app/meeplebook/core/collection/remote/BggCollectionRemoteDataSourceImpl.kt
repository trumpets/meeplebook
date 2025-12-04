package app.meeplebook.core.collection.remote

import app.meeplebook.core.collection.model.CollectionItem
import app.meeplebook.core.network.BggBaseUrl
import app.meeplebook.core.util.xml.CollectionXmlParser
import kotlinx.coroutines.delay
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.io.IOException
import javax.inject.Inject

/**
 * Implementation of [BggCollectionRemoteDataSource] that fetches collection
 * from BGG XML API2 with retry logic for 202 responses.
 */
class BggCollectionRemoteDataSourceImpl @Inject constructor(
    private val okHttpClient: OkHttpClient,
    @BggBaseUrl private val baseUrl: String
) : BggCollectionRemoteDataSource {

    private val api: BggCollectionApi by lazy {
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .build()
            .create(BggCollectionApi::class.java)
    }

    companion object {
        private const val MAX_RETRIES = 5
        private const val INITIAL_DELAY_MS = 5000L
        private const val BACKOFF_MULTIPLIER = 1.5
    }

    override suspend fun getCollection(username: String): List<CollectionItem> {
        if (username.isBlank()) {
            throw IllegalArgumentException("Username must not be empty")
        }

        var currentDelay = INITIAL_DELAY_MS
        var retryCount = 0

        while (retryCount < MAX_RETRIES) {
            val response = api.getCollection(username)

            when (response.code()) {
                200 -> {
                    val body = response.body()
                        ?: throw IOException("Empty response body")
                    return body.byteStream().use { inputStream ->
                        CollectionXmlParser.parse(inputStream)
                    }
                }
                202 -> {
                    // Data is being prepared, wait and retry
                    retryCount++
                    if (retryCount < MAX_RETRIES) {
                        delay(currentDelay)
                        currentDelay = (currentDelay * BACKOFF_MULTIPLIER).toLong()
                    }
                }
                else -> {
                    throw IOException("Unexpected response code: ${response.code()}")
                }
            }
        }

        throw CollectionNotReadyException(
            "Collection data not ready after $MAX_RETRIES retries"
        )
    }
}
