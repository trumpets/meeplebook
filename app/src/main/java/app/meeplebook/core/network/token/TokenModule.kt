package app.meeplebook.core.network.token

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module that provides token-related dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class TokenModule {

    /**
     * Provides the [TokenProvider] implementation.
     * Uses [TokenProvider] which deobfuscates the token from BuildConfig.
     */
    @Binds
    @Singleton
    abstract fun bindTokenProviding(impl: TokenProviderImpl): TokenProvider
}
