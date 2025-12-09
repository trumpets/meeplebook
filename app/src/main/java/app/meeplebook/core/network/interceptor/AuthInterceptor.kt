package app.meeplebook.core.network.interceptor

import android.net.Uri
import android.util.Log
import app.meeplebook.core.auth.AuthRepository
import app.meeplebook.core.model.AuthCredentials
import dagger.Lazy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/**
 * Interceptor that adds BGG authentication cookies to requests.
 * Uses Lazy<AuthRepository> to prevent circular dependency:
 * - AuthRepository needs OkHttpClient for remote data source
 * - OkHttpClient needs AuthInterceptor
 * - Lazy breaks the cycle by deferring AuthRepository resolution
 * 
 * The interceptor maintains an in-memory cache of credentials that's updated
 * via Flow to avoid blocking network threads with runBlocking calls.
 */
class AuthInterceptor(
    private val repository: Lazy<AuthRepository>
) : Interceptor {

    private val tag = "AuthInterceptor"
    
    // Cache credentials in memory to avoid blocking on every request
    @Volatile
    private var cachedCredentials: AuthCredentials? = null
    
    // Scope for observing credential changes
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    init {
        // Start observing credentials and cache them in memory
        // This eliminates the need for runBlocking on every request
        scope.launch {
            try {
                repository.get().observeCurrentUser().collect { credentials ->
                    cachedCredentials = credentials
                    Log.d(tag, "Credentials updated: ${if (credentials != null) "logged in" else "logged out"}")
                }
            } catch (e: Exception) {
                Log.e(tag, "Error observing credentials", e)
                // On error, clear cached credentials to fail safe
                cachedCredentials = null
            }
        }
    }

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // Use cached credentials instead of blocking to fetch them
        val currentUser = cachedCredentials

        if (currentUser == null) {
            return chain.proceed(originalRequest)
        }

        val username = Uri.encode(currentUser.username, "UTF-8")
        val password = Uri.encode(currentUser.password, "UTF-8")

        val cookieValue = "bggusername=$username; bggpassword=$password"

        val requestWithCookie = originalRequest.newBuilder()
            .addHeader("Cookie", cookieValue)
            .build()

        return chain.proceed(requestWithCookie)
    }
    
    /**
     * Cleanup method to cancel the coroutine scope.
     * Should be called when the interceptor is no longer needed.
     * In practice, since this is a singleton, it lives for the app lifetime.
     */
    fun cleanup() {
        scope.cancel()
    }
}