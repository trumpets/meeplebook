package app.meeplebook.core.auth

import app.meeplebook.core.auth.local.AuthLocalDataSource
import app.meeplebook.core.di.ApplicationScope
import app.meeplebook.core.model.AuthCredentials
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Always-available in-memory cache of auth credentials.
 * Never blocks, observed from ApplicationScope.
 *
 * Interceptor reads only this, synchronously.
 */
@Singleton
class CurrentCredentialsStore @Inject constructor(
    private val local: AuthLocalDataSource,
    @ApplicationScope private val scope: CoroutineScope
) {
    @Volatile
    var current: AuthCredentials? = null
        private set

    init {
        scope.launch {
            local.observeCredentials().collect { creds ->
                current = creds
            }
        }
    }
}
