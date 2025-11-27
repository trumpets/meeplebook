package app.meeplebook.core.auth

import app.meeplebook.core.auth.local.AuthLocalDataSource
import app.meeplebook.core.auth.local.AuthLocalDataSourceImpl
import app.meeplebook.core.auth.remote.BggAuthRemoteDataSource
import app.meeplebook.core.auth.remote.BggAuthRemoteDataSourceImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AuthModule {

    @Binds
    @Singleton
    abstract fun bindAuthLocal(impl: AuthLocalDataSourceImpl): AuthLocalDataSource

    @Binds
    @Singleton
    abstract fun bindAuthRemote(impl: BggAuthRemoteDataSourceImpl): BggAuthRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository
}