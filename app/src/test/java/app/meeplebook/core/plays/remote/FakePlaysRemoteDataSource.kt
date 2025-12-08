package app.meeplebook.core.plays.remote

import app.meeplebook.core.plays.model.Play

/**
 * Fake implementation of [PlaysRemoteDataSource] for testing.
 */
class FakePlaysRemoteDataSource : PlaysRemoteDataSource {

    var playsToReturn: List<Play> = emptyList()
    var playsToReturnByPage: Map<Int, List<Play>> = emptyMap()
    var shouldThrowException: Exception? = null
    var fetchPlaysCalled = false
    var lastFetchUsername: String? = null
    var lastFetchPage: Int? = null

    override suspend fun fetchPlays(username: String, page: Int?): List<Play> {
        fetchPlaysCalled = true
        lastFetchUsername = username
        lastFetchPage = page

        shouldThrowException?.let { throw it }

        // If page-specific responses are configured, use them
        if (playsToReturnByPage.isNotEmpty() && page != null) {
            return playsToReturnByPage[page] ?: emptyList()
        }

        return playsToReturn
    }
}
