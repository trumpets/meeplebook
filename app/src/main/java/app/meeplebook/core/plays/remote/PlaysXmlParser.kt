package app.meeplebook.core.plays.remote

import app.meeplebook.core.plays.model.Play
import app.meeplebook.core.plays.model.Player
import app.meeplebook.core.util.parseBggDate
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.Reader
import java.time.Instant

/**
 * Parses BGG plays XML responses into domain models.
 */
object PlaysXmlParser {

    private val parserFactory: XmlPullParserFactory by lazy {
        XmlPullParserFactory.newInstance()
    }

    /**
     * Parses the BGG plays XML response.
     *
     * @param reader The Reader containing XML from BGG.
     * @return List of [Play]s parsed from the XML.
     */
    fun parse(reader: Reader): List<Play> {
        val plays = mutableListOf<Play>()

        val parser = parserFactory.newPullParser()
        parser.setInput(reader)

        var eventType = parser.eventType
        var currentPlay: PlayBuilder? = null

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "play" -> {
                            val id = parser.getAttributeValue(null, "id")?.toLongOrNull()
                            val date = parseBggDate(parser.getAttributeValue(null, "date"))
                            val quantity = parser.getAttributeValue(null, "quantity")?.toIntOrNull() ?: 1
                            val length = parser.getAttributeValue(null, "length")?.toIntOrNull()?.takeIf { it > 0 }
                            val incomplete = parser.getAttributeValue(null, "incomplete")?.toIntOrNull() == 1
                            val location = parser.getAttributeValue(null, "location")?.takeIf { it.isNotBlank() }

                            // TODO: log that a date is invalid or missing
                            if (id != null && date != null) {
                                currentPlay = PlayBuilder(
                                    id = id,
                                    date = date,
                                    quantity = quantity,
                                    length = length,
                                    incomplete = incomplete,
                                    location = location
                                )
                            }
                        }
                        "item" -> {
                            currentPlay?.let { play ->
                                play.gameId = parser.getAttributeValue(null, "objectid")?.toLongOrNull()
                                play.gameName = parser.getAttributeValue(null, "name")
                            }
                        }
                        "comments" -> {
                            currentPlay?.comments = safeNextText(parser)?.takeIf { it.isNotBlank() }
                        }
                        "player" -> {
                            currentPlay?.let { play ->
                                val username = parser.getAttributeValue(null, "username")
                                val userId = parser.getAttributeValue(null, "userid")?.toLongOrNull()
                                val name = parser.getAttributeValue(null, "name")
                                val startPosition = parser.getAttributeValue(null, "startposition")
                                val color = parser.getAttributeValue(null, "color")
                                val score = parser.getAttributeValue(null, "score")
                                val win = parser.getAttributeValue(null, "win")?.toIntOrNull() == 1

                                if (!name.isNullOrBlank()) {
                                    val player = Player(
                                        playId = play.id,
                                        username = username?.takeIf { it.isNotBlank() },
                                        userId = userId,
                                        name = name,
                                        startPosition = startPosition?.takeIf { it.isNotBlank() },
                                        color = color?.takeIf { it.isNotBlank() },
                                        score = score?.takeIf { it.isNotBlank() },
                                        win = win
                                    )
                                    play.players.add(player)
                                }
                            }
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name == "play" && currentPlay != null) {
                        currentPlay.build()?.let { plays.add(it) }
                        currentPlay = null
                    }
                }
            }

            eventType = parser.next()
        }

        return plays
    }

    /**
     * Safely extracts text content from the current XML element.
     * Returns null if the text cannot be extracted or an error occurs.
     */
    private fun safeNextText(parser: XmlPullParser): String? {
        return try {
            parser.nextText()
        } catch (_: Exception) {
            null
        }
    }

    private class PlayBuilder(
        val id: Long,
        val date: Instant,
        val quantity: Int,
        val length: Int?,
        val incomplete: Boolean,
        val location: String?
    ) {
        var gameId: Long? = null
        var gameName: String? = null
        var comments: String? = null
        val players: MutableList<Player> = mutableListOf()

        fun build(): Play? {
            val validGameId = gameId ?: return null
            val validGameName = gameName?.takeIf { it.isNotBlank() } ?: return null

            return Play(
                id = this.id,
                date = date,
                quantity = quantity,
                length = length,
                incomplete = incomplete,
                location = location,
                gameId = validGameId,
                gameName = validGameName,
                comments = comments,
                players = players
            )
        }
    }
}
