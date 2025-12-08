package app.meeplebook.core.plays.local

import app.meeplebook.core.plays.model.Play
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Fake implementation of [PlaysLocalDataSource] for testing.
 */
class FakePlaysLocalDataSource : PlaysLocalDataSource {

    private val playsFlow = MutableStateFlow<List<Play>>(emptyList())

    override fun observePlays(): Flow<List<Play>> = playsFlow

    override suspend fun getPlays(): List<Play> = playsFlow.value

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

    override suspend fun clearPlays() {
        playsFlow.value = emptyList()
    }
}
