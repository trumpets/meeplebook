package app.meeplebook.core.collection.domain

import app.meeplebook.core.collection.FakeCollectionRepository
import app.meeplebook.core.collection.model.CollectionError
import app.meeplebook.core.collection.model.CollectionItem
import app.meeplebook.core.result.AppResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GetCollectionUseCaseTest {

    private lateinit var fakeRepository: FakeCollectionRepository
    private lateinit var useCase: GetCollectionUseCase

    @Before
    fun setUp() {
        fakeRepository = FakeCollectionRepository()
        useCase = GetCollectionUseCase(fakeRepository)
    }

    @Test
    fun `invoke with blank username returns NotLoggedIn error`() = runTest {
        val result = useCase("")

        assertTrue(result is AppResult.Failure)
        assertTrue((result as AppResult.Failure).error is CollectionError.NotLoggedIn)
        assertEquals(0, fakeRepository.getCollectionCallCount)
    }

    @Test
    fun `invoke with whitespace username returns NotLoggedIn error`() = runTest {
        val result = useCase("   ")

        assertTrue(result is AppResult.Failure)
        assertTrue((result as AppResult.Failure).error is CollectionError.NotLoggedIn)
        assertEquals(0, fakeRepository.getCollectionCallCount)
    }

    @Test
    fun `invoke with valid username calls repository`() = runTest {
        val items = listOf(
            CollectionItem(
                objectId = 1L,
                name = "Catan",
                yearPublished = 1995,
                thumbnailUrl = null,
                imageUrl = null,
                numPlays = 5,
                owned = true,
                rating = null,
                averageRating = null
            )
        )
        fakeRepository.getCollectionResult = AppResult.Success(items)

        val result = useCase("testUser")

        assertTrue(result is AppResult.Success)
        assertEquals(items, (result as AppResult.Success).data)
        assertEquals(1, fakeRepository.getCollectionCallCount)
        assertEquals("testUser", fakeRepository.lastUsername)
    }

    @Test
    fun `invoke with repository error returns error`() = runTest {
        fakeRepository.getCollectionResult = AppResult.Failure(CollectionError.NetworkError)

        val result = useCase("testUser")

        assertTrue(result is AppResult.Failure)
        assertTrue((result as AppResult.Failure).error is CollectionError.NetworkError)
    }
}
