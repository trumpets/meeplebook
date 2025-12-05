package app.meeplebook.core.collection

import app.meeplebook.core.collection.local.CollectionLocalDataSource
import app.meeplebook.core.collection.local.CollectionLocalDataSourceImpl
import app.meeplebook.core.collection.remote.CollectionRemoteDataSource
import app.meeplebook.core.collection.remote.CollectionRemoteDataSourceImpl
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
    abstract fun bindCollectionLocal(impl: CollectionLocalDataSourceImpl): CollectionLocalDataSource

    @Binds
    @Singleton
    abstract fun bindCollectionRemote(impl: CollectionRemoteDataSourceImpl): CollectionRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindCollectionRepository(impl: CollectionRepositoryImpl): CollectionRepository
}
