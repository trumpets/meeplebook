package app.meeplebook.core.network

import android.content.Context
import app.meeplebook.BuildConfig
import app.meeplebook.core.network.interceptor.BearerInterceptor
import app.meeplebook.core.network.interceptor.UserAgentInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

@Module
@InstallIn(SingletonComponent::class)
object OkHttpModule {

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
    }

    @Provides
    @Singleton
    fun provideBearerInterceptor(): BearerInterceptor {
        return BearerInterceptor(BuildConfig.BGG_TOKEN)
    }

    @Provides
    @Singleton
    fun provideUserAgentInterceptor(@ApplicationContext context: Context?): UserAgentInterceptor {
        return UserAgentInterceptor(context)
    }

    @Provides
    @Singleton
    fun provideOkHttp(logging: HttpLoggingInterceptor, bearer: BearerInterceptor, userAgent: UserAgentInterceptor): OkHttpClient {
        return OkHttpClient.Builder()
            .addNetworkInterceptor(logging)
            .addInterceptor(bearer)
            .addInterceptor(userAgent)
            .build()
    }
}