package app.meeplebook.core.database

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MeepleBookDatabase {
        return Room.databaseBuilder(
            context,
            MeepleBookDatabase::class.java,
            "meeplebook.db"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideCollectionItemDao(database: MeepleBookDatabase): CollectionItemDao {
        return database.collectionItemDao()
    }

    @Provides
    fun providePlayDao(database: MeepleBookDatabase): PlayDao {
        return database.playDao()
    }

    @Provides
    fun providePlayerDao(database: MeepleBookDatabase): PlayerDao {
        return database.playerDao()
    }
}
