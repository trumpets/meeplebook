package app.meeplebook.core.network

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import okhttp3.OkHttpClient
import retrofit2.Retrofit

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val BASE_URL = "https://boardgamegeek.com/"

    @Provides
    @BggBaseUrl
    fun provideBggBaseUrl(): String = BASE_URL

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttp: OkHttpClient
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttp)
            .build()
    }

    @Provides
    @Singleton
    fun provideBggApi(
        retrofit: Retrofit
    ): BggApi {
        return retrofit.create(BggApi::class.java)
    }

}