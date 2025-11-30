package app.meeplebook.core.auth.remote

import app.meeplebook.core.model.AuthCredentials
import javax.inject.Inject
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class BggAuthRemoteDataSourceImpl @Inject constructor(
    private val okHttpClient: OkHttpClient
) : BggAuthRemoteDataSource {

    override suspend fun login(username: String, password: String): AuthCredentials {
        if (username.isBlank() || password.isBlank()) {
            throw IllegalArgumentException("Username and password must not be empty")
        }

        var capturedToken: String? = null

        // Build a temp client that intercepts ONLY THIS CALL
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
            .baseUrl("https://boardgamegeek.com/")
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
