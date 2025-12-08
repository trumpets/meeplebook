package app.meeplebook.core.plays

import app.meeplebook.core.plays.local.PlaysLocalDataSource
import app.meeplebook.core.plays.local.PlaysLocalDataSourceImpl
import app.meeplebook.core.plays.remote.PlaysRemoteDataSource
import app.meeplebook.core.plays.remote.PlaysRemoteDataSourceImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for plays-related dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class PlaysModule {

    @Binds
    @Singleton
    abstract fun bindPlaysRepository(
        impl: PlaysRepositoryImpl
    ): PlaysRepository

    @Binds
    @Singleton
    abstract fun bindPlaysLocalDataSource(
        impl: PlaysLocalDataSourceImpl
    ): PlaysLocalDataSource

    @Binds
    @Singleton
    abstract fun bindPlaysRemoteDataSource(
        impl: PlaysRemoteDataSourceImpl
    ): PlaysRemoteDataSource
}
