package app.meeplebook.core.network.interceptor

import app.meeplebook.core.auth.CurrentCredentialsStore
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import java.net.URLEncoder

class AuthInterceptor(
    private val credsStore: CurrentCredentialsStore
) : Interceptor {

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        val currentUser = credsStore.current

        if (currentUser == null) {
            return chain.proceed(originalRequest)
        }

        // Use URLEncoder for JVM-friendly percent-encoding and convert '+' (space) to '%20'
        val username = URLEncoder.encode(currentUser.username, "UTF-8").replace("+", "%20")
        val password = URLEncoder.encode(currentUser.password, "UTF-8").replace("+", "%20")

        val cookieValue = "bggusername=$username; bggpassword=$password"

        val requestWithCookie = originalRequest.newBuilder()
            .addHeader("Cookie", cookieValue)
            .build()

        return chain.proceed(requestWithCookie)
    }
}