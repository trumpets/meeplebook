package app.meeplebook.core.auth.local

import app.meeplebook.core.model.AuthCredentials
import kotlinx.coroutines.flow.Flow
import java.io.Closeable

interface AuthLocalDataSource : Closeable {
    fun observeCredentials(): Flow<AuthCredentials?>
    suspend fun saveCredentials(creds: AuthCredentials)
    suspend fun getCredentials(): AuthCredentials?
    suspend fun clear()
}