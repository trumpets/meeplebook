package app.meeplebook.core.timer.service

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PlayTimerServiceModule {

    @Binds
    @Singleton
    abstract fun bindPlayTimerServiceController(
        impl: AndroidPlayTimerServiceController,
    ): PlayTimerServiceController
}