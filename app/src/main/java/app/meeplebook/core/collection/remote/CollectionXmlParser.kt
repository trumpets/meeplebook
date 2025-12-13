package app.meeplebook.core.collection.remote

import app.meeplebook.core.collection.model.CollectionItem
import app.meeplebook.core.collection.model.GameSubtype
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.Reader
import java.time.Instant

/**
 * Parses BGG collection XML responses into domain models.
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
                            val gameId = parser.getAttributeValue(null, "objectid")?.toIntOrNull()
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
                        "status" -> {
                            val lastModifiedStr = parser.getAttributeValue(null, "lastmodified")
                            currentItem?.lastModifiedDate = lastModifiedStr?.toLongOrNull()?.let {
                                Instant.ofEpochSecond(it)
                            }
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

    private class CollectionItemBuilder(
        val gameId: Int,
        val subtype: GameSubtype
    ) {
        var name: String? = null
        var yearPublished: Int? = null
        var thumbnail: String? = null
        var lastModifiedDate: Instant? = null

        fun build(): CollectionItem? {
            val itemName = name ?: return null
            return CollectionItem(
                gameId = gameId,
                subtype = subtype,
                name = itemName,
                yearPublished = yearPublished,
                thumbnail = thumbnail,
                lastModifiedDate = lastModifiedDate
            )
        }
    }
}
