package app.meeplebook.core.auth

import app.meeplebook.core.auth.local.AuthLocalDataSource
import app.meeplebook.core.auth.remote.BggAuthRemoteDataSource
import app.meeplebook.core.model.AuthCredentials
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AuthRepositoryImpl @Inject constructor(
    private val local: AuthLocalDataSource,
    private val remote: BggAuthRemoteDataSource
) : AuthRepository {

    override fun currentUser(): Flow<AuthCredentials?> {
        return local.observeCredentials()
    }

    override suspend fun login(username: String, password: String): Result<AuthCredentials> {
        try {
            val authCredentials = remote.login(username, password)
            local.saveCredentials(authCredentials)
            return Result.success(authCredentials)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    override suspend fun logout() {
        local.clear()
    }

    override suspend fun refreshUser(): Result<Unit> {
        TODO("Not yet implemented")
    }

    override fun isLoggedIn(): Flow<Boolean> {
        return local.observeCredentials().map { it != null }
    }
}