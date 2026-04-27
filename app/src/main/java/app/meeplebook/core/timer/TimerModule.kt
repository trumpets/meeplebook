package app.meeplebook.core.timer

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt bindings for timer persistence components.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class TimerModule {

    @Binds
    @Singleton
    abstract fun bindTimerRepository(
        impl: TimerRepositoryImpl,
    ): TimerRepository
}
