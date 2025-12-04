package app.meeplebook.core.plays.remote

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit API interface for BGG Plays XML API2.
 *
 * API docs: https://boardgamegeek.com/wiki/page/BGG_XML_API2#toc13
 */
interface BggPlaysApi {

    /**
     * Fetches plays for a user.
     *
     * @param username BGG username
     * @param type Type of plays to fetch (thing = board game plays)
     * @param page Page number for pagination (1-indexed)
     * @return Response with XML body containing plays
     */
    @GET("xmlapi2/plays")
    suspend fun getPlays(
        @Query("username") username: String,
        @Query("type") type: String = "thing",
        @Query("page") page: Int = 1
    ): Response<ResponseBody>
}
