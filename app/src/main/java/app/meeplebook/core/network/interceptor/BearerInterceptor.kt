package app.meeplebook.core.network.interceptor

import okhttp3.Interceptor
import okhttp3.Response

class BearerInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = "INSERT_TOKEN_HERE_SAFELY" // Replace with secure token retrieval
        val originalRequest = chain.request()
        val requestWithBearer = originalRequest.newBuilder()
            .header("Authorization", "Bearer $token")
            .build()
        return chain.proceed(requestWithBearer)
    }
}