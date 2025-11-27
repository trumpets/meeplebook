package app.meeplebook.core.security

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import jakarta.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    private const val DATASTORE_NAME = "meeplebook_secure_prefs"

    private val Context.dataStore: DataStore<Preferences>
            by preferencesDataStore(DATASTORE_NAME)

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
    fun provideEncryptedPreferences(dataStore: DataStore<Preferences>, provider: TinkAeadProvider): EncryptedPreferencesDataStore {
        return EncryptedPreferencesDataStore(dataStore, provider.getAead())
    }
}