package app.meeplebook.core.plays.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaysXmlParserTest {

    @Test
    fun `parse empty plays returns empty list`() {
        val xml = """
            <?xml version="1.0" encoding="utf-8"?>
            <plays username="testuser" userid="123" total="0" page="1" termsofuse="https://boardgamegeek.com/xmlapi/termsofuse">
            </plays>
        """.trimIndent()

        val result = PlaysXmlParser.parse(xml.reader())

        assertTrue(result.isEmpty())
    }

    @Test
    fun `parse single play with basic info`() {
        val xml = """
            <?xml version="1.0" encoding="utf-8"?>
            <plays username="testuser" userid="123" total="1" page="1" termsofuse="https://boardgamegeek.com/xmlapi/termsofuse">
                <play id="12345" date="2024-01-15" quantity="1" length="120" incomplete="0" location="Home">
                    <item name="Gloomhaven" objecttype="thing" objectid="174430">
                    </item>
                    <comments>Great game!</comments>
                </play>
            </plays>
        """.trimIndent()

        val result = PlaysXmlParser.parse(xml.reader())

        assertEquals(1, result.size)
        val play = result[0]
        assertEquals(12345, play.id)
        assertEquals("2024-01-15", play.date)
        assertEquals(1, play.quantity)
        assertEquals(120, play.length)
        assertFalse(play.incomplete)
        assertEquals("Home", play.location)
        assertEquals(174430, play.gameId)
        assertEquals("Gloomhaven", play.gameName)
        assertEquals("thing", play.gameSubtype)
        assertEquals("Great game!", play.comments)
        assertTrue(play.players.isEmpty())
    }

    @Test
    fun `parse play with players`() {
        val xml = """
            <?xml version="1.0" encoding="utf-8"?>
            <plays username="testuser" userid="123" total="1" page="1" termsofuse="https://boardgamegeek.com/xmlapi/termsofuse">
                <play id="12345" date="2024-01-15" quantity="1" length="90" incomplete="0" location="">
                    <item name="Catan" objecttype="thing" objectid="13">
                    </item>
                    <players>
                        <player username="player1" userid="111" name="Alice" startposition="1" color="Red" score="10" win="1" />
                        <player username="player2" userid="222" name="Bob" startposition="2" color="Blue" score="8" win="0" />
                        <player username="" userid="" name="Charlie" startposition="3" color="Green" score="7" win="0" />
                    </players>
                </play>
            </plays>
        """.trimIndent()

        val result = PlaysXmlParser.parse(xml.reader())

        assertEquals(1, result.size)
        val play = result[0]
        assertEquals(3, play.players.size)
        
        val player1 = play.players[0]
        assertEquals(12345, player1.playId)
        assertEquals("player1", player1.username)
        assertEquals(111, player1.userId)
        assertEquals("Alice", player1.name)
        assertEquals("1", player1.startPosition)
        assertEquals("Red", player1.color)
        assertEquals("10", player1.score)
        assertTrue(player1.win)

        val player2 = play.players[1]
        assertEquals("Bob", player2.name)
        assertFalse(player2.win)

        val player3 = play.players[2]
        assertEquals("Charlie", player3.name)
        assertNull(player3.username)
        assertNull(player3.userId)
    }

    @Test
    fun `parse play without optional fields`() {
        val xml = """
            <?xml version="1.0" encoding="utf-8"?>
            <plays username="testuser" userid="123" total="1" page="1" termsofuse="https://boardgamegeek.com/xmlapi/termsofuse">
                <play id="54321" date="2024-02-20" quantity="1" length="0" incomplete="0" location="">
                    <item name="Simple Game" objecttype="thing" objectid="999">
                    </item>
                </play>
            </plays>
        """.trimIndent()

        val result = PlaysXmlParser.parse(xml.reader())

        assertEquals(1, result.size)
        val play = result[0]
        assertEquals(54321, play.id)
        assertEquals("2024-02-20", play.date)
        assertNull(play.length)
        assertNull(play.location)
        assertNull(play.comments)
        assertTrue(play.players.isEmpty())
    }

    @Test
    fun `parse incomplete play`() {
        val xml = """
            <?xml version="1.0" encoding="utf-8"?>
            <plays username="testuser" userid="123" total="1" page="1" termsofuse="https://boardgamegeek.com/xmlapi/termsofuse">
                <play id="99999" date="2024-03-10" quantity="1" length="45" incomplete="1" location="Office">
                    <item name="Quick Game" objecttype="thing" objectid="555">
                    </item>
                </play>
            </plays>
        """.trimIndent()

        val result = PlaysXmlParser.parse(xml.reader())

        assertEquals(1, result.size)
        assertTrue(result[0].incomplete)
    }

    @Test
    fun `parse multiple plays`() {
        val xml = """
            <?xml version="1.0" encoding="utf-8"?>
            <plays username="testuser" userid="123" total="3" page="1" termsofuse="https://boardgamegeek.com/xmlapi/termsofuse">
                <play id="1" date="2024-01-01" quantity="1" length="60" incomplete="0" location="">
                    <item name="Game 1" objecttype="thing" objectid="1">
                    </item>
                </play>
                <play id="2" date="2024-01-02" quantity="1" length="90" incomplete="0" location="">
                    <item name="Game 2" objecttype="thing" objectid="2">
                    </item>
                </play>
                <play id="3" date="2024-01-03" quantity="2" length="120" incomplete="0" location="">
                    <item name="Game 3" objecttype="thing" objectid="3">
                    </item>
                </play>
            </plays>
        """.trimIndent()

        val result = PlaysXmlParser.parse(xml.reader())

        assertEquals(3, result.size)
        assertEquals(1, result[0].id)
        assertEquals(2, result[1].id)
        assertEquals(3, result[2].id)
        assertEquals(2, result[2].quantity)
    }

    @Test
    fun `parse skips play without game id`() {
        val xml = """
            <?xml version="1.0" encoding="utf-8"?>
            <plays username="testuser" userid="123" total="2" page="1" termsofuse="https://boardgamegeek.com/xmlapi/termsofuse">
                <play id="1" date="2024-01-01" quantity="1" length="60" incomplete="0" location="">
                    <item name="Invalid Game" objecttype="thing" objectid="">
                    </item>
                </play>
                <play id="2" date="2024-01-02" quantity="1" length="90" incomplete="0" location="">
                    <item name="Valid Game" objecttype="thing" objectid="999">
                    </item>
                </play>
            </plays>
        """.trimIndent()

        val result = PlaysXmlParser.parse(xml.reader())

        assertEquals(1, result.size)
        assertEquals(2, result[0].id)
        assertEquals("Valid Game", result[0].gameName)
    }

    @Test
    fun `parse skips play without game name`() {
        val xml = """
            <?xml version="1.0" encoding="utf-8"?>
            <plays username="testuser" userid="123" total="2" page="1" termsofuse="https://boardgamegeek.com/xmlapi/termsofuse">
                <play id="1" date="2024-01-01" quantity="1" length="60" incomplete="0" location="">
                    <item name="" objecttype="thing" objectid="100">
                    </item>
                </play>
                <play id="2" date="2024-01-02" quantity="1" length="90" incomplete="0" location="">
                    <item name="Valid Game" objecttype="thing" objectid="999">
                    </item>
                </play>
            </plays>
        """.trimIndent()

        val result = PlaysXmlParser.parse(xml.reader())

        assertEquals(1, result.size)
        assertEquals(2, result[0].id)
    }

    @Test
    fun `parse skips player without name`() {
        val xml = """
            <?xml version="1.0" encoding="utf-8"?>
            <plays username="testuser" userid="123" total="1" page="1" termsofuse="https://boardgamegeek.com/xmlapi/termsofuse">
                <play id="12345" date="2024-01-15" quantity="1" length="90" incomplete="0" location="">
                    <item name="Test Game" objecttype="thing" objectid="100">
                    </item>
                    <players>
                        <player username="user1" userid="111" name="" startposition="1" color="Red" score="10" win="1" />
                        <player username="user2" userid="222" name="Bob" startposition="2" color="Blue" score="8" win="0" />
                    </players>
                </play>
            </plays>
        """.trimIndent()

        val result = PlaysXmlParser.parse(xml.reader())

        assertEquals(1, result.size)
        val play = result[0]
        assertEquals(1, play.players.size)
        assertEquals("Bob", play.players[0].name)
    }
}
