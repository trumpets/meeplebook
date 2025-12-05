package app.meeplebook.core.collection.remote

import app.meeplebook.core.collection.model.CollectionItem
import app.meeplebook.core.collection.model.GameSubtype
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader

/**
 * Parses BGG collection XML responses into domain models.
 */
object CollectionXmlParser {

    /**
     * Parses the BGG collection XML response.
     *
     * @param xml The XML string from BGG.
     * @param subtypeOverride Optional subtype to override the one in XML (useful for expansion calls).
     * @return List of [CollectionItem]s parsed from the XML.
     */
    fun parse(xml: String, subtypeOverride: GameSubtype? = null): List<CollectionItem> {
        val items = mutableListOf<CollectionItem>()

        val factory = XmlPullParserFactory.newInstance()
        val parser = factory.newPullParser()
        parser.setInput(StringReader(xml))

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
                                    subtype = subtypeOverride ?: parseSubtype(subtype)
                                )
                            }
                        }
                        "name" -> {
                            currentItem?.name = parser.nextText()
                        }
                        "yearpublished" -> {
                            currentItem?.yearPublished = parser.nextText().toIntOrNull()
                        }
                        "thumbnail" -> {
                            currentItem?.thumbnail = parser.nextText()
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

        fun build(): CollectionItem? {
            val itemName = name ?: return null
            return CollectionItem(
                gameId = gameId,
                subtype = subtype,
                name = itemName,
                yearPublished = yearPublished,
                thumbnail = thumbnail
            )
        }
    }
}
