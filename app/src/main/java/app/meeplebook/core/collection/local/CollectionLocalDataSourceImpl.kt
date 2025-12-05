package app.meeplebook.core.collection.local

import app.meeplebook.core.collection.model.CollectionItem
import app.meeplebook.core.database.CollectionItemDao
import app.meeplebook.core.database.toCollectionItem
import app.meeplebook.core.database.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Implementation of [CollectionLocalDataSource] using Room database.
 */
class CollectionLocalDataSourceImpl @Inject constructor(
    private val dao: CollectionItemDao
) : CollectionLocalDataSource {

    override fun observeCollection(username: String): Flow<List<CollectionItem>> {
        return dao.observeCollectionByUsername(username).map { entities ->
            entities.map { it.toCollectionItem() }
        }
    }

    override suspend fun getCollection(username: String): List<CollectionItem> {
        return dao.getCollectionByUsername(username).map { it.toCollectionItem() }
    }

    override suspend fun saveCollection(username: String, items: List<CollectionItem>) {
        val entities = items.map { it.toEntity(username) }
        dao.replaceCollection(username, entities)
    }

    override suspend fun clearCollection(username: String) {
        dao.deleteByUsername(username)
    }
}
