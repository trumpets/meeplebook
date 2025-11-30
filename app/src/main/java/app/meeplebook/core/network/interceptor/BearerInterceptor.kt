package app.meeplebook.core.network.interceptor

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Interceptor that adds the BGG bearer token to requests.
 */
class BearerInterceptor (
    private val token: String?
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // Skip adding header if token is not configured
        if (token.isNullOrBlank()) {
            return chain.proceed(originalRequest)
        }

        val requestWithBearer = originalRequest.newBuilder()
            .header("Authorization", "Bearer $token")
            .build()
        return chain.proceed(requestWithBearer)
    }
}