package app.meeplebook.core.collection.domain

import app.meeplebook.core.collection.FakeCollectionRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [ObserveCollectionSummaryUseCase].
 */
class ObserveCollectionSummaryUseCaseTest {

    private lateinit var fakeCollectionRepository: FakeCollectionRepository
    private lateinit var useCase: ObserveCollectionSummaryUseCase

    @Before
    fun setUp() {
        fakeCollectionRepository = FakeCollectionRepository()
        useCase = ObserveCollectionSummaryUseCase(fakeCollectionRepository)
    }

    @Test
    fun `invoke returns correct summary with both counts`() = runTest {
        // Given
        fakeCollectionRepository.setCollectionCount(25L)
        fakeCollectionRepository.setUnplayedCount(10L)

        // When
        val summary = useCase().first()

        // Then
        assertEquals(
            DomainCollectionSummary(
                totalGames = 25L,
                unplayedGames = 10L
            ),
            summary
        )
    }

    @Test
    fun `invoke returns zero counts when collection is empty`() = runTest {
        // Given - no data set, defaults to zero

        // When
        val summary = useCase().first()

        // Then
        assertEquals(0L, summary.totalGames)
        assertEquals(0L, summary.unplayedGames)
    }

    @Test
    fun `invoke returns correct summary when all games are unplayed`() = runTest {
        // Given
        fakeCollectionRepository.setCollectionCount(15L)
        fakeCollectionRepository.setUnplayedCount(15L)

        // When
        val summary = useCase().first()

        // Then
        assertEquals(15L, summary.totalGames)
        assertEquals(15L, summary.unplayedGames)
    }

    @Test
    fun `invoke returns correct summary when no games are unplayed`() = runTest {
        // Given
        fakeCollectionRepository.setCollectionCount(20L)
        fakeCollectionRepository.setUnplayedCount(0L)

        // When
        val summary = useCase().first()

        // Then
        assertEquals(20L, summary.totalGames)
        assertEquals(0L, summary.unplayedGames)
    }

    @Test
    fun `invoke updates when repository data changes`() = runTest {
        // Given - initial state
        fakeCollectionRepository.setCollectionCount(10L)
        fakeCollectionRepository.setUnplayedCount(5L)

        // When - first observation
        val summary1 = useCase().first()

        // Then
        assertEquals(10L, summary1.totalGames)
        assertEquals(5L, summary1.unplayedGames)

        // When - data changes
        fakeCollectionRepository.setCollectionCount(30L)
        fakeCollectionRepository.setUnplayedCount(12L)
        val summary2 = useCase().first()

        // Then - summary is updated
        assertEquals(30L, summary2.totalGames)
        assertEquals(12L, summary2.unplayedGames)
    }

    @Test
    fun `invoke updates when only total count changes`() = runTest {
        // Given - initial state
        fakeCollectionRepository.setCollectionCount(10L)
        fakeCollectionRepository.setUnplayedCount(5L)

        // When - first observation
        val summary1 = useCase().first()

        // Then
        assertEquals(10L, summary1.totalGames)
        assertEquals(5L, summary1.unplayedGames)

        // When - only total changes
        fakeCollectionRepository.setCollectionCount(15L)
        val summary2 = useCase().first()

        // Then - summary reflects the change
        assertEquals(15L, summary2.totalGames)
        assertEquals(5L, summary2.unplayedGames)
    }

    @Test
    fun `invoke updates when only unplayed count changes`() = runTest {
        // Given - initial state
        fakeCollectionRepository.setCollectionCount(10L)
        fakeCollectionRepository.setUnplayedCount(5L)

        // When - first observation
        val summary1 = useCase().first()

        // Then
        assertEquals(10L, summary1.totalGames)
        assertEquals(5L, summary1.unplayedGames)

        // When - only unplayed changes
        fakeCollectionRepository.setUnplayedCount(3L)
        val summary2 = useCase().first()

        // Then - summary reflects the change
        assertEquals(10L, summary2.totalGames)
        assertEquals(3L, summary2.unplayedGames)
    }
}
