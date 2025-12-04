package app.meeplebook.core.plays

import app.meeplebook.core.plays.remote.BggPlaysRemoteDataSource
import app.meeplebook.core.plays.remote.BggPlaysRemoteDataSourceImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PlaysModule {

    @Binds
    @Singleton
    abstract fun bindPlaysRemote(impl: BggPlaysRemoteDataSourceImpl): BggPlaysRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindPlaysRepository(impl: PlaysRepositoryImpl): PlaysRepository
}
