package app.meeplebook.core.network.token

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module that provides token-related dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object TokenModule {

    /**
     * Provides the [TokenProviding] implementation.
     * Uses [TokenProvider] which deobfuscates the token from BuildConfig.
     */
    @Provides
    @Singleton
    fun provideTokenProviding(): TokenProviding = TokenProvider
}
