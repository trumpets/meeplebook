package app.meeplebook.core.network

import com.squareup.moshi.JsonClass
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface BggAuthApi {

    @POST("login/api/v1")
    suspend fun login(@Body payload: LoginPayload): Response<ResponseBody>
}

@JsonClass(generateAdapter = true)
data class LoginPayload(
    val credentials: CredentialsPayload
)

@JsonClass(generateAdapter = true)
data class CredentialsPayload(
    val username: String,
    val password: String
)