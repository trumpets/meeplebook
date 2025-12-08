package app.meeplebook.core.plays.remote

import app.meeplebook.core.plays.model.Play

/**
 * Fake implementation of [PlaysRemoteDataSource] for testing.
 */
class FakePlaysRemoteDataSource : PlaysRemoteDataSource {

    var playsToReturn: List<Play> = emptyList()
    var shouldThrowException: Exception? = null
    var fetchPlaysCalled = false
    var lastFetchUsername: String? = null
    var lastFetchPage: Int? = null

    override suspend fun fetchPlays(username: String, page: Int?): List<Play> {
        fetchPlaysCalled = true
        lastFetchUsername = username
        lastFetchPage = page

        shouldThrowException?.let { throw it }

        return playsToReturn
    }
}
