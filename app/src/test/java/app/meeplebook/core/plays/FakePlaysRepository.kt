package app.meeplebook.core.plays

import app.meeplebook.core.plays.model.Play
import app.meeplebook.core.plays.model.PlayError
import app.meeplebook.core.result.AppResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Fake implementation of [PlaysRepository] for testing.
 */
class FakePlaysRepository : PlaysRepository {

    private val playsFlow = MutableStateFlow<List<Play>>(emptyList())
    
    var syncPlaysResult: AppResult<List<Play>, PlayError>? = null
    var syncPlaysCalled = false
    var lastSyncUsername: String? = null
    var lastSyncPage: Int? = null

    override fun observePlays(): Flow<List<Play>> = playsFlow

    override suspend fun getPlays(): List<Play> = playsFlow.value

    override suspend fun syncPlays(username: String, page: Int?): AppResult<List<Play>, PlayError> {
        syncPlaysCalled = true
        lastSyncUsername = username
        lastSyncPage = page
        
        return syncPlaysResult ?: AppResult.Success(emptyList())
    }

    override suspend fun clearPlays() {
        playsFlow.value = emptyList()
    }

    fun setPlays(plays: List<Play>) {
        playsFlow.value = plays
    }
}
