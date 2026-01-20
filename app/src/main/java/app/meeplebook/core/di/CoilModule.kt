package app.meeplebook.core.di

import android.content.Context
import app.meeplebook.R
import coil3.ImageLoader
import coil3.asImage
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton

/**
 * Hilt module that provides a singleton [ImageLoader] for Coil.
 *
 * This module configures:
 * - Memory cache (25% of available memory)
 * - Disk cache (50 MB in app cache directory)
 * - Cross-fade animation for smooth image loading
 * - Uses the app's OkHttpClient for network requests (sharing interceptors, auth, etc.)
 */
@Module
@InstallIn(SingletonComponent::class)
object CoilModule {

    @Provides
    @Singleton
    fun provideImageLoader(
        @ApplicationContext context: Context,
        okHttpClient: OkHttpClient
    ): ImageLoader {
        return ImageLoader.Builder(context)
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, 0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("image_cache"))
                    .maxSizeBytes(50L * 1024 * 1024) // 50 MB
                    .build()
            }
            .crossfade(true)
            .placeholder { request ->
                request.context.resources
                    .getDrawable(R.drawable.ic_launcher_background, request.context.theme)
                    .asImage()
            }
            .error { request ->
                request.context.resources
                    .getDrawable(R.drawable.ic_launcher_background, request.context.theme)
                    .asImage()
            }
            .fallback { request ->
                request.context.resources
                    .getDrawable(R.drawable.ic_launcher_background, request.context.theme)
                    .asImage()
            }
            .components {
                add(
                    OkHttpNetworkFetcherFactory(
                        callFactory = okHttpClient
                    )
                )
            }
            .build()
    }
}