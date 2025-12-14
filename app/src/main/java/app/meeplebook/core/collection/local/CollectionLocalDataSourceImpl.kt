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

    override fun observeCollectionCount(): Flow<Long> {
        return dao.observeCollectionCount()
    }

    override fun observeUnplayedGamesCount(): Flow<Long> {
        return dao.observeUnplayedGamesCount()
    }

    override fun observeMostRecentlyAddedItem(): Flow<CollectionItem> {
        return dao.observeMostRecentlyAddedItem().map { itemEntity ->
            itemEntity.toCollectionItem()
        }
    }

    override fun observeFirstUnplayedGame(): Flow<CollectionItem> {
        return dao.observeFirstUnplayedGame().map { itemEntity ->
            itemEntity.toCollectionItem()
        }
    }
}
