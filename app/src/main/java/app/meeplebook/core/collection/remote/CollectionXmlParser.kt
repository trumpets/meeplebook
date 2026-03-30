package app.meeplebook.core.collection.remote

import app.meeplebook.core.collection.model.CollectionItem
import app.meeplebook.core.collection.model.GameRank
import app.meeplebook.core.collection.model.GameSubtype
import app.meeplebook.core.collection.model.RankType
import app.meeplebook.core.util.parseBggDateTime
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.Reader
import java.time.Instant

/**
 * Parses BGG collection XML responses into domain models.
 *
 * The BGG collection response includes stats > rating information:
 * ```xml
 * <stats ...>
 *   <rating value="7">
 *     <ranks>
 *       <rank type="subtype" name="boardgame" friendlyname="Board Game Rank" value="123" .../>
 *       <rank type="family" name="strategygames" friendlyname="Strategy Game Rank" value="45" .../>
 *     </ranks>
 *   </rating>
 * </stats>
 * ```
 */
object CollectionXmlParser {

    private val parserFactory: XmlPullParserFactory by lazy {
        XmlPullParserFactory.newInstance()
    }

    /**
     * Parses the BGG collection XML response.
     *
     * @param reader The Reader containing XML from BGG.
     * @return List of [CollectionItem]s parsed from the XML.
     */
    fun parse(reader: Reader): List<CollectionItem> {
        val items = mutableListOf<CollectionItem>()

        val parser = parserFactory.newPullParser()
        parser.setInput(reader)

        var eventType = parser.eventType
        var currentItem: CollectionItemBuilder? = null

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "item" -> {
                            val gameId = parser.getAttributeValue(null, "objectid")?.toLongOrNull()
                            val subtype = parser.getAttributeValue(null, "subtype")
                            if (gameId != null) {
                                currentItem = CollectionItemBuilder(
                                    gameId = gameId,
                                    subtype = parseSubtype(subtype)
                                )
                            }
                        }
                        "name" -> {
                            currentItem?.name = safeNextText(parser)
                        }
                        "yearpublished" -> {
                            currentItem?.yearPublished = safeNextText(parser)?.toIntOrNull()
                        }
                        "thumbnail" -> {
                            currentItem?.thumbnail = safeNextText(parser)
                        }
                        "image" -> {
                            currentItem?.image = safeNextText(parser)
                        }
                        "status" -> {
                            val lastModifiedStr = parser.getAttributeValue(null, "lastmodified")
                            currentItem?.lastModifiedDate = parseBggDateTime(lastModifiedStr)
                        }
                        "stats" -> {
                            currentItem?.minPlayers =
                                parser.getAttributeValue(null, "minplayers")?.toIntOrNull()
                            currentItem?.maxPlayers =
                                parser.getAttributeValue(null, "maxplayers")?.toIntOrNull()
                            currentItem?.minPlayTimeMinutes =
                                parser.getAttributeValue(null, "minplaytime")?.toIntOrNull()
                            currentItem?.maxPlayTimeMinutes =
                                parser.getAttributeValue(null, "maxplaytime")?.toIntOrNull()
                        }
                        "rating" -> {
                            val ratingStr = parser.getAttributeValue(null, "value")
                            currentItem?.userRating = ratingStr?.toFloatOrNull()
                        }
                        "rank" -> {
                            currentItem?.let { item ->
                                val typeStr = parser.getAttributeValue(null, "type")
                                val name = parser.getAttributeValue(null, "name")
                                val friendlyName = parser.getAttributeValue(null, "friendlyname")
                                val valueStr = parser.getAttributeValue(null, "value")
                                val rankType = parseRankType(typeStr)
                                if (name != null && friendlyName != null && rankType != null) {
                                    item.ranks.add(
                                        GameRank(
                                            type = rankType,
                                            name = name,
                                            friendlyName = friendlyName,
                                            value = valueStr?.toIntOrNull()
                                        )
                                    )
                                }
                            }
                        }
                        "numplays" -> {
                            currentItem?.numPlays = safeNextText(parser)?.toIntOrNull() ?: 0
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name == "item" && currentItem != null) {
                        currentItem.build()?.let { items.add(it) }
                        currentItem = null
                    }
                }
            }

            eventType = parser.next()
        }

        return items
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

    private fun parseSubtype(subtype: String?): GameSubtype {
        return when (subtype) {
            "boardgameexpansion" -> GameSubtype.BOARDGAME_EXPANSION
            else -> GameSubtype.BOARDGAME
        }
    }

    private fun parseRankType(type: String?): RankType? {
        return when (type) {
            "subtype" -> RankType.SUBTYPE
            "family" -> RankType.FAMILY
            else -> null
        }
    }

    private class CollectionItemBuilder(
        val gameId: Long,
        val subtype: GameSubtype
    ) {
        var name: String? = null
        var yearPublished: Int? = null
        var thumbnail: String? = null
        var image: String? = null
        var lastModifiedDate: Instant? = null
        var minPlayers: Int? = null
        var maxPlayers: Int? = null
        var minPlayTimeMinutes: Int? = null
        var maxPlayTimeMinutes: Int? = null
        var numPlays: Int = 0
        var userRating: Float? = null
        val ranks: MutableList<GameRank> = mutableListOf()

        fun build(): CollectionItem? {
            val itemName = name ?: return null
            return CollectionItem(
                gameId = gameId,
                subtype = subtype,
                name = itemName,
                yearPublished = yearPublished,
                thumbnail = thumbnail,
                image = image,
                lastModifiedDate = lastModifiedDate,
                minPlayers = minPlayers,
                maxPlayers = maxPlayers,
                minPlayTimeMinutes = minPlayTimeMinutes,
                maxPlayTimeMinutes = maxPlayTimeMinutes,
                numPlays = numPlays,
                userRating = userRating,
                ranks = ranks
            )
        }
    }
}
