package app.meeplebook.core.network.interceptor

import android.content.Context
import app.meeplebook.core.util.versionName
import okhttp3.Interceptor
import okhttp3.Response

class UserAgentInterceptor (
    private val context: Context?
) : Interceptor {

    val userAgent: String = constructUserAgent()

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val requestWithUserAgent = originalRequest.newBuilder()
            .header("User-Agent", userAgent)
            .build()
        return chain.proceed(requestWithUserAgent)
    }

    private fun constructUserAgent(): String {
        val userAgent = "MeepleBook"
        return if (context == null) {
            userAgent
        } else {
            "$userAgent/${context.versionName()}"
        }
    }
}