package app.meeplebook.core.collection

import app.meeplebook.core.collection.remote.BggCollectionRemoteDataSource
import app.meeplebook.core.collection.remote.BggCollectionRemoteDataSourceImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CollectionModule {

    @Binds
    @Singleton
    abstract fun bindCollectionRemote(impl: BggCollectionRemoteDataSourceImpl): BggCollectionRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindCollectionRepository(impl: CollectionRepositoryImpl): CollectionRepository
}
