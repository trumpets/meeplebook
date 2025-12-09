package app.meeplebook.core.network.interceptor

import app.meeplebook.core.auth.AuthRepository
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import dagger.Lazy
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class AuthInterceptor(
    private val repository: Lazy<AuthRepository> // to prevent circular dependency as repo needs OkHttp which needs this
) : Interceptor {

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // blocks the OkHttp request thread to get the current user synchronously
        // this is acceptable here since the auth data is stored locally
        // and the operation should be quick
        // alternatively, we could cache the current user in memory to avoid blocking
        // but that would add complexity to keep the cache in sync
        // with the repository
        val currentUser = runBlocking { repository.get().getCurrentUser() }

        if (currentUser == null) {
            return chain.proceed(originalRequest)
        }

        // Use URLEncoder for JVM-friendly percent-encoding and convert '+' (space) to '%20'
        val username = URLEncoder.encode(currentUser.username, StandardCharsets.UTF_8.name()).replace("+", "%20")
        val password = URLEncoder.encode(currentUser.password, StandardCharsets.UTF_8.name()).replace("+", "%20")

        val cookieValue = "bggusername=$username; bggpassword=$password"

        val requestWithCookie = originalRequest.newBuilder()
            .addHeader("Cookie", cookieValue)
            .build()

        return chain.proceed(requestWithCookie)
    }
}