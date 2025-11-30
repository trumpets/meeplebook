package app.meeplebook.core.network.interceptor

import app.meeplebook.core.network.token.TokenProviding
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/**
 * Interceptor that adds the BGG bearer token to requests.
 * The token is retrieved from TokenProvider which deobfuscates it from BuildConfig.
 *
 * @param tokenProvider The token provider to use, injected by Hilt.
 */
class BearerInterceptor @Inject constructor(
    private val tokenProvider: TokenProviding
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = tokenProvider.getBggToken()
        val originalRequest = chain.request()

        // Skip adding header if token is not configured
        if (token.isEmpty()) {
            return chain.proceed(originalRequest)
        }

        val requestWithBearer = originalRequest.newBuilder()
            .header("Authorization", "Bearer $token")
            .build()
        return chain.proceed(requestWithBearer)
    }
}