package app.meeplebook.core.auth.local

import app.meeplebook.core.model.AuthCredentials
import kotlinx.coroutines.flow.Flow

interface AuthLocalDataSource {
    fun observeCredentials(): Flow<AuthCredentials?>
    suspend fun saveCredentials(creds: AuthCredentials)
    suspend fun getCredentials(): AuthCredentials?
    suspend fun clear()
}