package app.meeplebook.core.sync

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt bindings for sync persistence components.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class SyncTimeModule {

    @Binds
    @Singleton
    abstract fun bindSyncTimeRepository(
        impl: SyncTimeRepositoryImpl
    ): SyncTimeRepository
}
