package app.meeplebook.core.collection.local

import app.meeplebook.core.collection.model.CollectionItem
import app.meeplebook.core.database.dao.CollectionItemDao
import app.meeplebook.core.database.entity.toCollectionItem
import app.meeplebook.core.database.entity.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Implementation of [CollectionLocalDataSource] using Room database.
 */
class CollectionLocalDataSourceImpl @Inject constructor(
    private val dao: CollectionItemDao
) : CollectionLocalDataSource {

    override fun observeCollection(): Flow<List<CollectionItem>> {
        return dao.observeCollection().map { entities ->
            entities.map { it.toCollectionItem() }
        }
    }

    override suspend fun getCollection(): List<CollectionItem> {
        return dao.getCollection().map { it.toCollectionItem() }
    }

    override suspend fun saveCollection(items: List<CollectionItem>) {
        val entities = items.map { it.toEntity() }
        dao.replaceCollection(entities)
    }

    override suspend fun clearCollection() {
        dao.deleteAll()
    }

    override suspend fun getCollectionCount(): Long {
        return dao.getCollectionCount()
    }

    override suspend fun getUnplayedGamesCount(): Long {
        return dao.getUnplayedGamesCount()
    }

    override suspend fun getMostRecentlyAddedItem(): CollectionItem? {
        return dao.getMostRecentlyAddedItem()?.toCollectionItem()
    }

    override suspend fun getFirstUnplayedGame(): CollectionItem? {
        return dao.getFirstUnplayedGame()?.toCollectionItem()
    }
}
