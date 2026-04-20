package app.meeplebook.core.sync.manager

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt bindings for background sync orchestration.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class SyncManagerBindingsModule {

    @Binds
    @Singleton
    abstract fun bindSyncManager(
        impl: WorkManagerSyncManager
    ): SyncManager
}
