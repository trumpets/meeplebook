package app.meeplebook.core.plays.remote

import app.meeplebook.core.util.xml.PlaysXmlParser

/**
 * Fake implementation of [BggPlaysRemoteDataSource] for testing.
 */
class FakeBggPlaysRemoteDataSource : BggPlaysRemoteDataSource {

    var playsResult: PlaysXmlParser.PlaysResponse? = null
    var exception: Exception? = null
    var getPlaysCallCount = 0
        private set
    var lastUsername: String? = null
        private set
    var lastPage: Int? = null
        private set

    override suspend fun getPlays(username: String, page: Int): PlaysXmlParser.PlaysResponse {
        getPlaysCallCount++
        lastUsername = username
        lastPage = page

        exception?.let { throw it }
        return playsResult
            ?: throw IllegalStateException("FakeBggPlaysRemoteDataSource not configured")
    }
}
