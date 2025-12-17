package app.meeplebook.core.plays.local

import app.meeplebook.core.plays.model.Play
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.Instant

/**
 * Fake implementation of [PlaysLocalDataSource] for testing.
 */
class FakePlaysLocalDataSource : PlaysLocalDataSource {

    private val playsFlow = MutableStateFlow<List<Play>>(emptyList())
    private val totalPlaysCount = MutableStateFlow(0L)
    private val playsCountForMonth = MutableStateFlow<Map<Pair<Instant, Instant>, Long>>(emptyMap())
    private val recentPlays = MutableStateFlow<Map<Int, List<Play>>>(emptyMap())

    override fun observePlays(): Flow<List<Play>> = playsFlow
    override fun observePlaysForGame(gameId: Long): Flow<List<Play>> {
        return playsFlow.map { list -> list.filter { it.gameId == gameId } }
    }

    override suspend fun getPlays(): List<Play> = playsFlow.value
    override suspend fun getPlaysForGame(gameId: Long): List<Play> {
        return playsFlow.value.filter { it.gameId == gameId }
    }

    override suspend fun savePlays(plays: List<Play>) {
        // Merge plays - replace existing by ID, add new ones
        val existingPlays = playsFlow.value.toMutableList()
        plays.forEach { newPlay ->
            val index = existingPlays.indexOfFirst { it.id == newPlay.id }
            if (index >= 0) {
                existingPlays[index] = newPlay
            } else {
                existingPlays.add(newPlay)
            }
        }
        playsFlow.value = existingPlays
    }

    override suspend fun savePlay(play: Play) {
        savePlays(listOf(play))
    }

    override suspend fun clearPlays() {
        playsFlow.value = emptyList()
    }

    override fun observeTotalPlaysCount(): Flow<Long> {
        return totalPlaysCount
    }

    override fun observePlaysCountForMonth(
        start: Instant,
        end: Instant
    ): Flow<Long> {
        return playsCountForMonth.map { map -> map[Pair(start, end)] ?: 0L }
    }

    override fun observeRecentPlays(limit: Int): Flow<List<Play>> {
        return recentPlays.map { map -> map[limit] ?: emptyList() }
    }

    /**
     * Sets the total plays count for testing.
     */
    fun setTotalPlaysCount(count: Long) {
        totalPlaysCount.value = count
    }

    /**
     * Sets the plays count for a specific month for testing.
     */
    fun setPlaysCountForMonth(start: Instant, end: Instant, count: Long) {
        val currentMap = playsCountForMonth.value.toMutableMap()
        currentMap[Pair(start, end)] = count
        playsCountForMonth.value = currentMap
    }

    /**
     * Sets recent plays for a specific limit for testing.
     */
    fun setRecentPlays(limit: Int, plays: List<Play>) {
        val currentMap = recentPlays.value.toMutableMap()
        currentMap[limit] = plays
        recentPlays.value = currentMap
    }
}
