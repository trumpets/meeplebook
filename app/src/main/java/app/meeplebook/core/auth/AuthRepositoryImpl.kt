package app.meeplebook.core.auth

import app.meeplebook.core.auth.local.AuthLocalDataSource
import app.meeplebook.core.auth.model.AuthError
import app.meeplebook.core.auth.remote.AuthenticationException
import app.meeplebook.core.auth.remote.BggAuthRemoteDataSource
import app.meeplebook.core.model.AuthCredentials
import app.meeplebook.core.result.AppResult
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.net.UnknownHostException

class AuthRepositoryImpl @Inject constructor(
    private val local: AuthLocalDataSource,
    private val remote: BggAuthRemoteDataSource
) : AuthRepository {

    override fun observeCurrentUser(): Flow<AuthCredentials?> {
        return local.observeCredentials()
    }

    override suspend fun getCurrentUser(): AuthCredentials? {
        return local.getCredentials()
    }

    override suspend fun login(username: String, password: String): AppResult<AuthCredentials, AuthError> {
        try {
            val authCredentials = remote.login(username, password)
            local.saveCredentials(authCredentials)

            return AppResult.Success(authCredentials)
        } catch (e: Exception) {
            return when (e) {
                is IllegalArgumentException -> AppResult.Failure(AuthError.EmptyCredentials)
                is IOException, is IllegalStateException -> AppResult.Failure(AuthError.NetworkError)
                is AuthenticationException -> AppResult.Failure(AuthError.InvalidCredentials)
                else -> AppResult.Failure(AuthError.Unknown(e))
            }
        }
    }

    override suspend fun logout() {
        local.clear()
    }

    override fun isLoggedIn(): Flow<Boolean> {
        return local.observeCredentials().map { it != null }
    }
}