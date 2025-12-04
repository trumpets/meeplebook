package app.meeplebook.core.util.xml

import android.util.Xml
import app.meeplebook.core.plays.model.Play
import app.meeplebook.core.plays.model.PlayGame
import app.meeplebook.core.plays.model.PlayPlayer
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream

/**
 * Parser for BGG Plays XML API2 responses.
 */
object PlaysXmlParser {

    /**
     * Response data from parsing plays XML.
     *
     * @param plays List of parsed plays
     * @param total Total number of plays available
     * @param page Current page number
     */
    data class PlaysResponse(
        val plays: List<Play>,
        val total: Int,
        val page: Int
    )

    /**
     * Parses the XML response from BGG Plays API.
     *
     * @param inputStream The XML response input stream
     * @return Parsed plays response with pagination info
     */
    fun parse(inputStream: InputStream): PlaysResponse {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(inputStream, null)
        parser.nextTag()
        return readPlays(parser)
    }

    private fun readPlays(parser: XmlPullParser): PlaysResponse {
        val plays = mutableListOf<Play>()

        parser.require(XmlPullParser.START_TAG, null, "plays")

        val total = parser.getAttributeValue(null, "total")?.toIntOrNull() ?: 0
        val page = parser.getAttributeValue(null, "page")?.toIntOrNull() ?: 1

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) continue
            if (parser.name == "play") {
                readPlay(parser)?.let { plays.add(it) }
            } else {
                skip(parser)
            }
        }
        return PlaysResponse(plays, total, page)
    }

    private fun readPlay(parser: XmlPullParser): Play? {
        parser.require(XmlPullParser.START_TAG, null, "play")

        val playId = parser.getAttributeValue(null, "id")?.toLongOrNull() ?: return null
        val date = parser.getAttributeValue(null, "date") ?: ""
        val quantity = parser.getAttributeValue(null, "quantity")?.toIntOrNull() ?: 1
        val length = parser.getAttributeValue(null, "length")?.toIntOrNull() ?: 0
        val incomplete = parser.getAttributeValue(null, "incomplete") == "1"
        val noWinStats = parser.getAttributeValue(null, "nowinstats") == "1"
        val location = parser.getAttributeValue(null, "location")

        var game: PlayGame? = null
        var comments: String? = null
        val players = mutableListOf<PlayPlayer>()

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) continue

            when (parser.name) {
                "item" -> game = readPlayGame(parser)
                "comments" -> comments = readText(parser)
                "players" -> players.addAll(readPlayers(parser))
                else -> skip(parser)
            }
        }

        return game?.let {
            Play(
                playId = playId,
                date = date,
                quantity = quantity,
                length = length,
                incomplete = incomplete,
                noWinStats = noWinStats,
                location = location,
                comments = comments,
                game = it,
                players = players
            )
        }
    }

    private fun readPlayGame(parser: XmlPullParser): PlayGame {
        parser.require(XmlPullParser.START_TAG, null, "item")

        val objectId = parser.getAttributeValue(null, "objectid")?.toLongOrNull() ?: 0L
        val name = parser.getAttributeValue(null, "name") ?: ""

        // Skip to end tag
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType == XmlPullParser.START_TAG) {
                skip(parser)
            }
        }

        return PlayGame(objectId, name)
    }

    private fun readPlayers(parser: XmlPullParser): List<PlayPlayer> {
        val players = mutableListOf<PlayPlayer>()

        parser.require(XmlPullParser.START_TAG, null, "players")

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) continue
            if (parser.name == "player") {
                players.add(readPlayer(parser))
            } else {
                skip(parser)
            }
        }

        return players
    }

    private fun readPlayer(parser: XmlPullParser): PlayPlayer {
        parser.require(XmlPullParser.START_TAG, null, "player")

        val player = PlayPlayer(
            username = parser.getAttributeValue(null, "username")?.takeIf { it.isNotBlank() },
            name = parser.getAttributeValue(null, "name") ?: "",
            startPosition = parser.getAttributeValue(null, "startposition")?.takeIf { it.isNotBlank() },
            color = parser.getAttributeValue(null, "color")?.takeIf { it.isNotBlank() },
            score = parser.getAttributeValue(null, "score")?.takeIf { it.isNotBlank() },
            new = parser.getAttributeValue(null, "new") == "1",
            win = parser.getAttributeValue(null, "win") == "1"
        )

        // Skip to end tag
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType == XmlPullParser.START_TAG) {
                skip(parser)
            }
        }

        return player
    }

    private fun readText(parser: XmlPullParser): String {
        var result = ""
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.text
            parser.nextTag()
        }
        return result
    }

    private fun skip(parser: XmlPullParser) {
        if (parser.eventType != XmlPullParser.START_TAG) {
            throw IllegalStateException()
        }
        var depth = 1
        while (depth != 0) {
            when (parser.next()) {
                XmlPullParser.END_TAG -> depth--
                XmlPullParser.START_TAG -> depth++
            }
        }
    }
}
