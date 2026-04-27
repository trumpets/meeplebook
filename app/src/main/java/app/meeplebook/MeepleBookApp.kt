package app.meeplebook

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import app.meeplebook.core.timer.service.PlayTimerServiceCoordinator
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class MeepleBookApp : Application(), SingletonImageLoader.Factory, Configuration.Provider {

    @Inject
    lateinit var customImageLoader: ImageLoader

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var playTimerServiceCoordinator: PlayTimerServiceCoordinator

    override fun newImageLoader(context: PlatformContext): ImageLoader {
        return customImageLoader
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
