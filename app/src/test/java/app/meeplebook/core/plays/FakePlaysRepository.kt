package app.meeplebook.core.plays

import app.meeplebook.core.plays.model.Play
import app.meeplebook.core.plays.model.PlaysError
import app.meeplebook.core.result.AppResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Fake implementation of [PlaysRepository] for testing.
 */
class FakePlaysRepository : PlaysRepository {

    private val _plays = MutableStateFlow<List<Play>>(emptyList())

    var getPlaysResult: AppResult<PlaysResponse, PlaysError> =
        AppResult.Failure(PlaysError.Unknown(IllegalStateException("Not configured")))

    var getPlaysCallCount = 0
        private set
    var lastUsername: String? = null
        private set
    var lastPage: Int? = null
        private set

    override suspend fun getPlays(username: String, page: Int): AppResult<PlaysResponse, PlaysError> {
        getPlaysCallCount++
        lastUsername = username
        lastPage = page

        when (val result = getPlaysResult) {
            is AppResult.Success -> {
                if (page == 1) {
                    _plays.value = result.data.plays
                } else {
                    _plays.value = _plays.value + result.data.plays
                }
            }
            is AppResult.Failure -> { /* no-op */ }
        }

        return getPlaysResult
    }

    override fun observePlays(): Flow<List<Play>> = _plays

    fun setPlays(plays: List<Play>) {
        _plays.value = plays
    }
}
