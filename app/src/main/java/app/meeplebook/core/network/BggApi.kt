package app.meeplebook.core.network

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit API interface for BGG collection endpoints.
 *
 * Note: BGG returns XML, so we use ResponseBody and parse manually.
 */
interface BggApi {

    /**
     * Fetches a user's collection from BGG.
     *
     * @param username The BGG username.
     * @param own Whether to include owned items (1 = yes).
     * @param showPrivate Whether to show private info (1 = yes).
     * @param excludeSubtype Subtype to exclude (e.g., "boardgameexpansion").
     * @param subtype Subtype to include (e.g., "boardgameexpansion").
     * @return Response containing XML body. Note: May return 202 if BGG is queuing the request.
     */
    @GET("xmlapi2/collection")
    suspend fun getCollection(
        @Query("username") username: String,
        @Query("own") own: Int = 1,
        @Query("showprivate") showPrivate: Int = 1,
        @Query("excludesubtype") excludeSubtype: String? = null,
        @Query("subtype") subtype: String? = null
    ): Response<ResponseBody>

    /**
     * Fetches a user's plays from BGG.
     *
     * @param username The BGG username.
     * @param type The type of item (typically "thing" for board games).
     * @param subtype The subtype to filter by (e.g., "boardgame", "boardgameexpansion").
     * @param page The page number (page size is 100 records).
     * @return Response containing XML body. Note: May return 202 if BGG is queuing the request.
     */
    @GET("xmlapi2/plays")
    suspend fun getPlays(
        @Query("username") username: String,
        @Query("type") type: String? = null,
        @Query("subtype") subtype: String? = null,
        @Query("page") page: Int? = null
    ): Response<ResponseBody>
}