package app.meeplebook.core.auth.remote.integration

import app.meeplebook.core.auth.remote.AuthenticationException
import app.meeplebook.core.auth.remote.BggAuthApi
import app.meeplebook.core.auth.remote.BggAuthRemoteDataSource
import app.meeplebook.core.auth.remote.CredentialsPayload
import app.meeplebook.core.auth.remote.LoginPayload
import app.meeplebook.core.model.AuthCredentials
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

/**
 * A testable version of BggAuthRemoteDataSourceImpl that allows injecting a custom base URL
 * for integration testing with MockWebServer.
 */
class BggAuthRemoteDataSourceTestable(
    private val okHttpClient: OkHttpClient,
    private val baseUrl: String
) : BggAuthRemoteDataSource {

    override suspend fun login(username: String, password: String): AuthCredentials {
        if (username.isBlank() || password.isBlank()) {
            throw IllegalArgumentException("Username and password must not be empty")
        }

        var capturedToken: String? = null

        val client = okHttpClient.newBuilder()
            .addInterceptor { chain ->
                val response = chain.proceed(chain.request())
                response.headers("Set-Cookie").forEach { cookie ->
                    if (cookie.startsWith("bggpassword=") &&
                        !cookie.contains("deleted", ignoreCase = true)
                    ) {
                        capturedToken = cookie.substringAfter("bggpassword=").substringBefore(";")
                    }
                }
                response
            }
            .build()

        val tempApi = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(BggAuthApi::class.java)

        val res = tempApi.login(
            LoginPayload(
                credentials = CredentialsPayload(username, password)
            )
        )

        if (!res.isSuccessful || res.code() != 204) {
            throw AuthenticationException("Login failed: HTTP ${res.code()}")
        }

        if (capturedToken == null) {
            throw AuthenticationException("Login failed: bggpassword cookie not found")
        }

        return AuthCredentials(username, password)
    }
}
