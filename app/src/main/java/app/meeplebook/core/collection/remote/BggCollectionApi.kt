package app.meeplebook.core.collection.remote

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit API interface for BGG Collection XML API2.
 *
 * API docs: https://boardgamegeek.com/wiki/page/BGG_XML_API2#toc4
 *
 * Note: Returns 202 Accepted when data is being prepared. Caller should
 * implement backoff and retry logic.
 */
interface BggCollectionApi {

    /**
     * Fetches the user's collection.
     *
     * @param username BGG username
     * @param own Filter to owned games (1 = owned, 0 = not owned)
     * @param showPrivate Include private collection info (1 = include)
     * @return Response with XML body, may return 202 if data is being prepared
     */
    @GET("xmlapi2/collection")
    suspend fun getCollection(
        @Query("username") username: String,
        @Query("own") own: Int = 1,
        @Query("showprivate") showPrivate: Int = 1
    ): Response<ResponseBody>
}
