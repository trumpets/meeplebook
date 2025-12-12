package app.meeplebook.core.collection.remote

import app.meeplebook.core.collection.model.CollectionItem
import app.meeplebook.core.collection.model.GameSubtype
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.Reader
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Parses BGG collection XML responses into domain models.
 */
object CollectionXmlParser {

    private val parserFactory: XmlPullParserFactory by lazy {
        XmlPullParserFactory.newInstance()
    }
    
    // BGG uses ISO 8601 format for dates
    private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ISO_DATE_TIME

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
                            val gameId = parser.getAttributeValue(null, "objectid")?.toIntOrNull()
                            val subtype = parser.getAttributeValue(null, "subtype")
                            val lastModified = parser.getAttributeValue(null, "lastmodified")
                            if (gameId != null) {
                                currentItem = CollectionItemBuilder(
                                    gameId = gameId,
                                    subtype = parseSubtype(subtype),
                                    lastModified = parseLastModified(lastModified)
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
    
    /**
     * Parses the lastmodified date string from BGG XML.
     * Expected format: YYYY-MM-DD HH:MM:SS
     */
    private fun parseLastModified(lastModified: String?): LocalDateTime? {
        if (lastModified.isNullOrBlank()) return null
        return try {
            LocalDateTime.parse(lastModified, dateFormatter)
        } catch (_: Exception) {
            null
        }
    }

    private class CollectionItemBuilder(
        val gameId: Int,
        val subtype: GameSubtype,
        val lastModified: LocalDateTime?
    ) {
        var name: String? = null
        var yearPublished: Int? = null
        var thumbnail: String? = null

        fun build(): CollectionItem? {
            val itemName = name ?: return null
            return CollectionItem(
                gameId = gameId,
                subtype = subtype,
                name = itemName,
                yearPublished = yearPublished,
                thumbnail = thumbnail,
                lastModified = lastModified
            )
        }
    }
}
