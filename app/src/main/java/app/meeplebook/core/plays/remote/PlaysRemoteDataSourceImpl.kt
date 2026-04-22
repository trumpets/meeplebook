package app.meeplebook.core.plays.remote

import app.meeplebook.core.network.BggApi
import app.meeplebook.core.network.RetrySignal
import app.meeplebook.core.network.retryWithBackoff
import app.meeplebook.core.plays.model.Play
import app.meeplebook.core.plays.model.Player
import app.meeplebook.core.plays.model.onRemote
import app.meeplebook.core.plays.remote.dto.RemotePlayDto
import app.meeplebook.core.util.formatBggDate
import okhttp3.FormBody
import java.io.IOException
import java.math.BigDecimal
import javax.inject.Inject

/**
 * Implementation of [PlaysRemoteDataSource] that fetches and uploads plays using BGG endpoints.
 */
class PlaysRemoteDataSourceImpl @Inject constructor(
    private val api: BggApi
) : PlaysRemoteDataSource {

    override suspend fun fetchPlays(username: String, page: Int?): List<RemotePlayDto> {
        if (username.isBlank()) {
            throw IllegalArgumentException("Username must not be empty")
        }

        return fetchWithRetry(username, page)
    }

    override suspend fun uploadPlay(play: Play): Long {

        val response = api.savePlay(play.toFormBody())

        try {
            val code = response.code()

            if (code == 401 || code == 403) {
                throw IllegalArgumentException("Authenticated session required to upload plays")
            }

            if (code == 429 || code in 500..599) {
                throw IOException("Upload failed with HTTP $code")
            }

            if (code !in 200..299) {
                throw PlayUploadException("Unexpected HTTP $code")
            }

            val body = response.body()
                ?: throw IOException("Empty response body on HTTP $code")

            val bodyText = body.string()

            extractErrorMessage(bodyText)?.let { message ->
                if (isAuthErrorMessage(message)) {
                    throw IllegalArgumentException(message)
                }
                throw PlayUploadException(message)
            }

            if (looksLikeAuthFailure(bodyText)) {
                throw IllegalArgumentException("Authenticated session required to upload plays")
            }

            return extractRemoteId(bodyText, play)?.also { remoteId ->
                if (remoteId <= 0L) {
                    throw PlayUploadException("Invalid remote play id returned")
                }
            } ?: throw PlayUploadException("Missing remote play id in upload response")
        } finally {
            response.body()?.close()
            response.errorBody()?.close()
        }
    }

    /**
     * Fetches plays with retry logic for 202 responses.
     */
    private suspend fun fetchWithRetry(
        username: String,
        page: Int?
    ): List<RemotePlayDto> {

        return retryWithBackoff(
            username = username
        ) { attempt ->
            val response = api.getPlays(
                username = username,
                type = "thing",
                page = page
            )

            val code = response.code()

            if (code == 202 || code == 429 || code in 500..599) {
                response.body()?.close()
                throw RetrySignal(code)
            }

            if (code != 200) {
                response.body()?.close()
                throw PlaysFetchException(message = "Unexpected HTTP $code")
            }

            val body = response.body()
                ?: throw IOException("Empty response body on HTTP $code")

            body.charStream().use { reader ->
                return@retryWithBackoff PlaysXmlParser.parse(reader)
            }
        }
    }
}

private val quotedErrorRegex = Regex(""""error"\s*:\s*"([^"]+)"""")
private val plainErrorRegex = Regex("""\berror\b[^A-Za-z0-9]+([^\n<]+)""", RegexOption.IGNORE_CASE)
private val playIdRegexes = listOf(
    Regex("\"playid\"\\s*:\\s*\"?(\\d+)\"?", setOf(RegexOption.IGNORE_CASE)),
    Regex("""\bplayid\b[^0-9]+(\d+)""", setOf(RegexOption.IGNORE_CASE)),
    Regex("""/play/(\d+)""")
)

private fun Play.toFormBody(): FormBody {
    val dateValue = formatBggDate(date)

    return FormBody.Builder()
        .add("ajax", "1")
        .add("action", "save")
        .add("version", "2")
        .add("objecttype", "thing")
        .add("objectid", gameId.toString())
        .add("playdate", dateValue)
        .add("dateinput", dateValue)
        .add("length", length?.toString().orEmpty())
        .add("location", location.orEmpty())
        .add("quantity", quantity.toString())
        .add("comments", comments.orEmpty())
        .add("incomplete", if (incomplete) "1" else "0")
        .add("nowinstats", "0") // TODO Ivo has forgotten what this does, need to verify if we should set it to 1 for certain plays
        .apply {
            playId.onRemote { remoteId ->
                add("playid", remoteId.toString())
            }

            players.forEachIndexed { index, player ->
                addPlayer(index, player)
            }
        }
        .build()
}

private fun FormBody.Builder.addPlayer(index: Int, player: Player): FormBody.Builder {
    val prefix = "players[$index]"
    return this
        .add("$prefix[name]", player.name)
        .add("$prefix[username]", player.username.orEmpty())
        .add("$prefix[color]", player.color.orEmpty())
        .add("$prefix[position]", player.startPosition?.toString().orEmpty())
        .add("$prefix[score]", player.score.toBggScoreString())
        .add("$prefix[win]", if (player.win) "1" else "0")
}

private fun Double?.toBggScoreString(): String {
    if (this == null) return ""
    return BigDecimal.valueOf(this).stripTrailingZeros().toPlainString()
}

private fun looksLikeAuthFailure(body: String): Boolean {
    val normalized = body.lowercase()
    return ("bggpassword" in normalized && "bggusername" in normalized) ||
        ("sign in" in normalized && "boardgamegeek" in normalized) ||
        ("not logged in" in normalized) ||
        ("must login to save plays" in normalized) ||
        ("must log in to save plays" in normalized)
}

private fun isAuthErrorMessage(message: String): Boolean {
    val normalized = message.lowercase()
    return "must login to save plays" in normalized ||
        "must log in to save plays" in normalized ||
        "not logged in" in normalized
}

private fun extractErrorMessage(body: String): String? {
    return quotedErrorRegex.find(body)?.groupValues?.getOrNull(1)?.takeIf { it.isNotBlank() }
        ?: plainErrorRegex.find(body)?.groupValues?.getOrNull(1)?.trim()?.takeIf {
            it.isNotBlank() && !it.contains("playid", ignoreCase = true)
        }
}

private fun extractRemoteId(body: String, play: Play): Long? {
    playIdRegexes.forEach { regex ->
        regex.find(body)?.groupValues?.getOrNull(1)?.toLongOrNull()?.let { return it }
    }
    return play.playId.remoteIdOrNull()
}
