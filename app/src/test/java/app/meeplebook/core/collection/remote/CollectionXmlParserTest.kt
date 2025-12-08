package app.meeplebook.core.collection.remote

import app.meeplebook.core.collection.model.GameSubtype
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CollectionXmlParserTest {

    @Test
    fun `parse empty items returns empty list`() {
        val xml = """
            <?xml version="1.0" encoding="utf-8"?>
            <items totalitems="0" termsofuse="https://boardgamegeek.com/xmlapi/termsofuse">
            </items>
        """.trimIndent()

        val result = CollectionXmlParser.parse(xml.reader())

        assertTrue(result.isEmpty())
    }

    @Test
    fun `parse single boardgame item`() {
        val xml = """
            <?xml version="1.0" encoding="utf-8"?>
            <items totalitems="1" termsofuse="https://boardgamegeek.com/xmlapi/termsofuse">
                <item objecttype="thing" objectid="174430" subtype="boardgame" collid="12345">
                    <name sortindex="1">Gloomhaven</name>
                    <yearpublished>2017</yearpublished>
                    <thumbnail>https://cf.geekdo-images.com/thumb.jpg</thumbnail>
                </item>
            </items>
        """.trimIndent()

        val result = CollectionXmlParser.parse(xml.reader())

        assertEquals(1, result.size)
        val item = result[0]
        assertEquals(174430, item.gameId)
        assertEquals(GameSubtype.BOARDGAME, item.subtype)
        assertEquals("Gloomhaven", item.name)
        assertEquals(2017, item.yearPublished)
        assertEquals("https://cf.geekdo-images.com/thumb.jpg", item.thumbnail)
    }

    @Test
    fun `parse expansion item`() {
        val xml = """
            <?xml version="1.0" encoding="utf-8"?>
            <items totalitems="1" termsofuse="https://boardgamegeek.com/xmlapi/termsofuse">
                <item objecttype="thing" objectid="220308" subtype="boardgameexpansion" collid="67890">
                    <name sortindex="1">Gloomhaven: Forgotten Circles</name>
                    <yearpublished>2019</yearpublished>
                    <thumbnail>https://cf.geekdo-images.com/expansion.jpg</thumbnail>
                </item>
            </items>
        """.trimIndent()

        val result = CollectionXmlParser.parse(xml.reader())

        assertEquals(1, result.size)
        val item = result[0]
        assertEquals(220308, item.gameId)
        assertEquals(GameSubtype.BOARDGAME_EXPANSION, item.subtype)
        assertEquals("Gloomhaven: Forgotten Circles", item.name)
        assertEquals(2019, item.yearPublished)
    }

    @Test
    fun `parse multiple items`() {
        val xml = """
            <?xml version="1.0" encoding="utf-8"?>
            <items totalitems="3" termsofuse="https://boardgamegeek.com/xmlapi/termsofuse">
                <item objecttype="thing" objectid="1" subtype="boardgame" collid="1">
                    <name sortindex="1">Game 1</name>
                    <yearpublished>2020</yearpublished>
                </item>
                <item objecttype="thing" objectid="2" subtype="boardgame" collid="2">
                    <name sortindex="1">Game 2</name>
                    <yearpublished>2021</yearpublished>
                </item>
                <item objecttype="thing" objectid="3" subtype="boardgame" collid="3">
                    <name sortindex="1">Game 3</name>
                    <yearpublished>2022</yearpublished>
                </item>
            </items>
        """.trimIndent()

        val result = CollectionXmlParser.parse(xml.reader())

        assertEquals(3, result.size)
        assertEquals(1, result[0].gameId)
        assertEquals(2, result[1].gameId)
        assertEquals(3, result[2].gameId)
    }

    @Test
    fun `parse item without year published`() {
        val xml = """
            <?xml version="1.0" encoding="utf-8"?>
            <items totalitems="1" termsofuse="https://boardgamegeek.com/xmlapi/termsofuse">
                <item objecttype="thing" objectid="1" subtype="boardgame" collid="1">
                    <name sortindex="1">Game Without Year</name>
                    <thumbnail>https://example.com/thumb.jpg</thumbnail>
                </item>
            </items>
        """.trimIndent()

        val result = CollectionXmlParser.parse(xml.reader())

        assertEquals(1, result.size)
        assertNull(result[0].yearPublished)
    }

    @Test
    fun `parse item without thumbnail`() {
        val xml = """
            <?xml version="1.0" encoding="utf-8"?>
            <items totalitems="1" termsofuse="https://boardgamegeek.com/xmlapi/termsofuse">
                <item objecttype="thing" objectid="1" subtype="boardgame" collid="1">
                    <name sortindex="1">Game Without Thumbnail</name>
                    <yearpublished>2020</yearpublished>
                </item>
            </items>
        """.trimIndent()

        val result = CollectionXmlParser.parse(xml.reader())

        assertEquals(1, result.size)
        assertNull(result[0].thumbnail)
    }

    @Test
    fun `parse skips item without name`() {
        val xml = """
            <?xml version="1.0" encoding="utf-8"?>
            <items totalitems="2" termsofuse="https://boardgamegeek.com/xmlapi/termsofuse">
                <item objecttype="thing" objectid="1" subtype="boardgame" collid="1">
                    <yearpublished>2020</yearpublished>
                </item>
                <item objecttype="thing" objectid="2" subtype="boardgame" collid="2">
                    <name sortindex="1">Valid Game</name>
                    <yearpublished>2021</yearpublished>
                </item>
            </items>
        """.trimIndent()

        val result = CollectionXmlParser.parse(xml.reader())

        assertEquals(1, result.size)
        assertEquals(2, result[0].gameId)
        assertEquals("Valid Game", result[0].name)
    }

    @Test
    fun `parse skips item without valid game id`() {
        val xml = """
            <?xml version="1.0" encoding="utf-8"?>
            <items totalitems="2" termsofuse="https://boardgamegeek.com/xmlapi/termsofuse">
                <item objecttype="thing" objectid="invalid" subtype="boardgame" collid="1">
                    <name sortindex="1">Invalid Game</name>
                    <yearpublished>2020</yearpublished>
                </item>
                <item objecttype="thing" objectid="2" subtype="boardgame" collid="2">
                    <name sortindex="1">Valid Game</name>
                    <yearpublished>2021</yearpublished>
                </item>
            </items>
        """.trimIndent()

        val result = CollectionXmlParser.parse(xml.reader())

        assertEquals(1, result.size)
        assertEquals(2, result[0].gameId)
    }
}
