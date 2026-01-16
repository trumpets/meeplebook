package app.meeplebook

import android.app.Application
import coil3.ImageLoader
import coil3.SingletonImageLoader
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class MeepleBookApp : Application(), SingletonImageLoader.Factory {
    
    @Inject
    lateinit var imageLoader: ImageLoader
    
    override fun newImageLoader(): ImageLoader = imageLoader
}