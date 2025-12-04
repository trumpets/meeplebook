package app.meeplebook.core.collection.domain

import app.meeplebook.core.collection.CollectionRepository
import app.meeplebook.core.collection.model.CollectionError
import app.meeplebook.core.collection.model.CollectionItem
import app.meeplebook.core.result.AppResult
import javax.inject.Inject

/**
 * Use case that fetches the user's board game collection.
 */
class GetCollectionUseCase @Inject constructor(
    private val collectionRepository: CollectionRepository
) {

    /**
     * Fetches the collection for the given username.
     *
     * @param username BGG username
     * @return Result containing list of collection items or error
     */
    suspend operator fun invoke(username: String): AppResult<List<CollectionItem>, CollectionError> {
        if (username.isBlank()) {
            return AppResult.Failure(CollectionError.NotLoggedIn)
        }
        return collectionRepository.getCollection(username)
    }
}
