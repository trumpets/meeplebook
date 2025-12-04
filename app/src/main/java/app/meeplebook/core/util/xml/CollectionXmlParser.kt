package app.meeplebook.core.util.xml

import android.util.Xml
import app.meeplebook.core.collection.model.CollectionItem
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream

/**
 * Parser for BGG Collection XML API2 responses.
 */
object CollectionXmlParser {

    /**
     * Parses the XML response from BGG Collection API.
     *
     * @param inputStream The XML response input stream
     * @return List of parsed collection items
     */
    fun parse(inputStream: InputStream): List<CollectionItem> {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(inputStream, null)
        parser.nextTag()
        return readItems(parser)
    }

    private fun readItems(parser: XmlPullParser): List<CollectionItem> {
        val items = mutableListOf<CollectionItem>()

        parser.require(XmlPullParser.START_TAG, null, "items")
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) continue
            if (parser.name == "item") {
                readItem(parser)?.let { items.add(it) }
            } else {
                skip(parser)
            }
        }
        return items
    }

    private fun readItem(parser: XmlPullParser): CollectionItem? {
        parser.require(XmlPullParser.START_TAG, null, "item")

        val objectId = parser.getAttributeValue(null, "objectid")?.toLongOrNull() ?: return null

        var name: String? = null
        var yearPublished: Int? = null
        var thumbnailUrl: String? = null
        var imageUrl: String? = null
        var numPlays = 0
        var owned = false
        var rating: Float? = null
        var averageRating: Float? = null

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) continue

            when (parser.name) {
                "name" -> name = readText(parser)
                "yearpublished" -> yearPublished = readText(parser).toIntOrNull()
                "thumbnail" -> thumbnailUrl = readText(parser)
                "image" -> imageUrl = readText(parser)
                "numplays" -> numPlays = readText(parser).toIntOrNull() ?: 0
                "status" -> {
                    owned = parser.getAttributeValue(null, "own") == "1"
                    skip(parser)
                }
                "stats" -> {
                    val stats = readStats(parser)
                    rating = stats.first
                    averageRating = stats.second
                }
                else -> skip(parser)
            }
        }

        return name?.let {
            CollectionItem(
                objectId = objectId,
                name = it,
                yearPublished = yearPublished,
                thumbnailUrl = thumbnailUrl,
                imageUrl = imageUrl,
                numPlays = numPlays,
                owned = owned,
                rating = rating,
                averageRating = averageRating
            )
        }
    }

    private fun readStats(parser: XmlPullParser): Pair<Float?, Float?> {
        parser.require(XmlPullParser.START_TAG, null, "stats")

        var rating: Float? = null
        var averageRating: Float? = null

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) continue

            when (parser.name) {
                "rating" -> {
                    val value = parser.getAttributeValue(null, "value")
                    rating = value?.toFloatOrNull()
                    // Read nested rating elements
                    while (parser.next() != XmlPullParser.END_TAG) {
                        if (parser.eventType != XmlPullParser.START_TAG) continue
                        if (parser.name == "average") {
                            averageRating = parser.getAttributeValue(null, "value")?.toFloatOrNull()
                        }
                        skip(parser)
                    }
                }
                else -> skip(parser)
            }
        }

        return Pair(rating, averageRating)
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
