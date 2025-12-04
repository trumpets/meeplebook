package app.meeplebook.core.collection

import app.meeplebook.core.collection.model.CollectionError
import app.meeplebook.core.collection.model.CollectionItem
import app.meeplebook.core.collection.remote.CollectionNotReadyException
import app.meeplebook.core.collection.remote.FakeBggCollectionRemoteDataSource
import app.meeplebook.core.result.AppResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

class CollectionRepositoryImplTest {

    private lateinit var fakeRemoteDataSource: FakeBggCollectionRemoteDataSource
    private lateinit var repository: CollectionRepositoryImpl

    @Before
    fun setUp() {
        fakeRemoteDataSource = FakeBggCollectionRemoteDataSource()
        repository = CollectionRepositoryImpl(fakeRemoteDataSource)
    }

    @Test
    fun `getCollection success returns items and updates cache`() = runTest {
        val items = listOf(
            CollectionItem(
                objectId = 1L,
                name = "Catan",
                yearPublished = 1995,
                thumbnailUrl = "https://example.com/thumb.jpg",
                imageUrl = "https://example.com/image.jpg",
                numPlays = 10,
                owned = true,
                rating = 8.5f,
                averageRating = 7.2f
            )
        )
        fakeRemoteDataSource.collectionResult = items

        val result = repository.getCollection("testUser")

        assertTrue(result is AppResult.Success)
        assertEquals(items, (result as AppResult.Success).data)
        assertEquals(items, repository.observeCollection().first())
        assertEquals("testUser", fakeRemoteDataSource.lastUsername)
    }

    @Test
    fun `getCollection with IOException returns NetworkError`() = runTest {
        fakeRemoteDataSource.exception = IOException("Network error")

        val result = repository.getCollection("testUser")

        assertTrue(result is AppResult.Failure)
        assertTrue((result as AppResult.Failure).error is CollectionError.NetworkError)
    }

    @Test
    fun `getCollection with IllegalArgumentException returns NotLoggedIn`() = runTest {
        fakeRemoteDataSource.exception = IllegalArgumentException("Empty username")

        val result = repository.getCollection("")

        assertTrue(result is AppResult.Failure)
        assertTrue((result as AppResult.Failure).error is CollectionError.NotLoggedIn)
    }

    @Test
    fun `getCollection with CollectionNotReadyException returns Timeout`() = runTest {
        fakeRemoteDataSource.exception = CollectionNotReadyException("Timed out")

        val result = repository.getCollection("testUser")

        assertTrue(result is AppResult.Failure)
        assertTrue((result as AppResult.Failure).error is CollectionError.Timeout)
    }

    @Test
    fun `getCollection with unknown exception returns Unknown error`() = runTest {
        val unexpectedException = RuntimeException("Unexpected")
        fakeRemoteDataSource.exception = unexpectedException

        val result = repository.getCollection("testUser")

        assertTrue(result is AppResult.Failure)
        val error = (result as AppResult.Failure).error
        assertTrue(error is CollectionError.Unknown)
        assertEquals(unexpectedException, (error as CollectionError.Unknown).throwable)
    }

    @Test
    fun `observeCollection returns empty list initially`() = runTest {
        val result = repository.observeCollection().first()

        assertTrue(result.isEmpty())
    }
}
