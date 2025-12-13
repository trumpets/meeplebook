package app.meeplebook.core.security

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import app.meeplebook.core.di.AuthDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    private const val AUTH_DATASTORE_NAME = "meeplebook_secure_prefs"
    private const val DATASTORE_NAME = "meeplebook_prefs"

    private val Context.authDataStore: DataStore<Preferences>
            by preferencesDataStore(AUTH_DATASTORE_NAME)

    private val Context.dataStore: DataStore<Preferences>
            by preferencesDataStore(DATASTORE_NAME)

    @Provides
    @Singleton
    @AuthDataStore
    fun provideAuthDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return context.authDataStore
    }

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return context.dataStore
    }

    @Provides
    @Singleton
    fun provideAeadProvider(@ApplicationContext context: Context): TinkAeadProvider {
        return TinkAeadProvider(context)
    }

    @Provides
    @Singleton
    fun provideEncryptedPreferences(@AuthDataStore dataStore: DataStore<Preferences>, provider: TinkAeadProvider): EncryptedPreferencesDataStore {
        return EncryptedPreferencesDataStore(dataStore, provider.getAead())
    }
}