package app.meeplebook.core.collection

import app.meeplebook.core.collection.local.FakeCollectionLocalDataSource
import app.meeplebook.core.collection.model.CollectionError
import app.meeplebook.core.collection.model.CollectionItem
import app.meeplebook.core.collection.model.GameSubtype
import app.meeplebook.core.collection.remote.FakeCollectionRemoteDataSource
import app.meeplebook.core.network.RetryException
import app.meeplebook.core.result.AppResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.time.Instant

class CollectionRepositoryImplTest {

    private lateinit var fakeLocalDataSource: FakeCollectionLocalDataSource
    private lateinit var fakeRemoteDataSource: FakeCollectionRemoteDataSource
    private lateinit var repository: CollectionRepositoryImpl

    @Before
    fun setUp() {
        fakeLocalDataSource = FakeCollectionLocalDataSource()
        fakeRemoteDataSource = FakeCollectionRemoteDataSource()
        repository = CollectionRepositoryImpl(fakeLocalDataSource, fakeRemoteDataSource)
    }

    // --- observeCollection tests ---

    @Test
    fun `observeCollection returns flow from local data source`() = runTest {
        val items = listOf(createTestItem(1, "Game 1"))
        fakeLocalDataSource.setCollection(items)

        val result = repository.observeCollection().first()

        assertEquals(items, result)
    }

    @Test
    fun `observeCollection returns empty list when no collection`() = runTest {
        val result = repository.observeCollection().first()

        assertTrue(result.isEmpty())
    }

    // --- getCollection tests ---

    @Test
    fun `getCollection returns items from local data source`() = runTest {
        val items = listOf(createTestItem(1, "Game 1"), createTestItem(2, "Game 2"))
        fakeLocalDataSource.setCollection(items)

        val result = repository.getCollection()

        assertEquals(items, result)
    }

    // --- syncCollection tests ---

    @Test
    fun `syncCollection success fetches from remote and saves locally`() = runTest {
        val items = listOf(createTestItem(1, "Game 1"))
        fakeRemoteDataSource.fetchResult = items

        val result = repository.syncCollection("testuser")

        assertTrue(result is AppResult.Success)
        assertEquals(items, (result as AppResult.Success).data)
        assertEquals(1, fakeRemoteDataSource.fetchCallCount)
        assertEquals("testuser", fakeRemoteDataSource.lastFetchUsername)
        assertEquals(1, fakeLocalDataSource.saveCollectionCallCount)
        assertEquals(items, fakeLocalDataSource.lastSaveItems)
    }

    @Test
    fun `syncCollection with IllegalArgumentException returns NotLoggedIn error`() = runTest {
        fakeRemoteDataSource.fetchException = IllegalArgumentException("Empty username")

        val result = repository.syncCollection("")

        assertTrue(result is AppResult.Failure)
        assertTrue((result as AppResult.Failure).error is CollectionError.NotLoggedIn)
        assertEquals(0, fakeLocalDataSource.saveCollectionCallCount)
    }

    @Test
    fun `syncCollection with IOException returns NetworkError`() = runTest {
        fakeRemoteDataSource.fetchException = IOException("Network error")

        val result = repository.syncCollection("testuser")

        assertTrue(result is AppResult.Failure)
        assertTrue((result as AppResult.Failure).error is CollectionError.NetworkError)
        assertEquals(0, fakeLocalDataSource.saveCollectionCallCount)
    }

    @Test
    fun `syncCollection with max retry CollectionFetchException returns MaxRetriesExceeded error`() = runTest {
        fakeRemoteDataSource.fetchException = RetryException(
            message = "Retry attempts exceeded",
            username = "testuser",
            lastHttpCode = 202,
            attempts = 10,
            lastDelayMs = 15000L
        )

        val result = repository.syncCollection("testuser")

        assertTrue(result is AppResult.Failure)
        assertTrue((result as AppResult.Failure).error is CollectionError.MaxRetriesExceeded)
        assertEquals(0, fakeLocalDataSource.saveCollectionCallCount)
        assertEquals((result.error as CollectionError.MaxRetriesExceeded).exception.username, "testuser")
    }

    @Test
    fun `syncCollection with unknown exception returns Unknown error`() = runTest {
        val unexpectedException = RuntimeException("Unexpected error")
        fakeRemoteDataSource.fetchException = unexpectedException

        val result = repository.syncCollection("testuser")

        assertTrue(result is AppResult.Failure)
        val error = (result as AppResult.Failure).error
        assertTrue(error is CollectionError.Unknown)
        assertEquals(unexpectedException, (error as CollectionError.Unknown).throwable)
        assertEquals(0, fakeLocalDataSource.saveCollectionCallCount)
    }

    // --- clearCollection tests ---

    @Test
    fun `clearCollection clears local data source`() = runTest {
        val items = listOf(createTestItem(1, "Game 1"))
        fakeLocalDataSource.setCollection(items)

        repository.clearCollection()

        assertEquals(1, fakeLocalDataSource.clearCollectionCallCount)
    }

    // --- Helper functions ---

    private fun createTestItem(
        gameId: Long,
        name: String,
        subtype: GameSubtype = GameSubtype.BOARDGAME,
        yearPublished: Int? = 2020,
        thumbnail: String? = "https://example.com/thumb.jpg",
        lastModifiedDate: Instant = Instant.now()
    ) = CollectionItem(
        gameId = gameId,
        subtype = subtype,
        name = name,
        yearPublished = yearPublished,
        thumbnail = thumbnail,
        lastModifiedDate = lastModifiedDate
    )
}
