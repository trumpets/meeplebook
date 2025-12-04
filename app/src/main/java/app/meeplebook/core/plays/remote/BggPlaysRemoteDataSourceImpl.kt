package app.meeplebook.core.plays.remote

import app.meeplebook.core.network.BggBaseUrl
import app.meeplebook.core.util.xml.PlaysXmlParser
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.io.IOException
import javax.inject.Inject

/**
 * Implementation of [BggPlaysRemoteDataSource] that fetches plays
 * from BGG XML API2.
 */
class BggPlaysRemoteDataSourceImpl @Inject constructor(
    private val okHttpClient: OkHttpClient,
    @BggBaseUrl private val baseUrl: String
) : BggPlaysRemoteDataSource {

    private val api: BggPlaysApi by lazy {
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .build()
            .create(BggPlaysApi::class.java)
    }

    override suspend fun getPlays(username: String, page: Int): PlaysXmlParser.PlaysResponse {
        if (username.isBlank()) {
            throw IllegalArgumentException("Username must not be empty")
        }

        val response = api.getPlays(username, page = page)

        if (!response.isSuccessful) {
            throw IOException("Failed to fetch plays: HTTP ${response.code()}")
        }

        val body = response.body()
            ?: throw IOException("Empty response body")

        return body.byteStream().use { inputStream ->
            PlaysXmlParser.parse(inputStream)
        }
    }
}
