package app.meeplebook.core.plays.remote

import app.meeplebook.core.util.xml.PlaysXmlParser

/**
 * Remote data source interface for fetching BGG plays.
 */
interface BggPlaysRemoteDataSource {

    /**
     * Fetches plays for a user with pagination.
     *
     * @param username BGG username
     * @param page Page number (1-indexed)
     * @return Response containing plays and pagination info
     * @throws java.io.IOException if network error occurs
     */
    suspend fun getPlays(username: String, page: Int = 1): PlaysXmlParser.PlaysResponse
}
