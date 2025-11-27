package app.meeplebook.core.auth.remote

import app.meeplebook.core.model.AuthCredentials

interface BggAuthRemoteDataSource {
    suspend fun login(username: String, password: String): AuthCredentials
}