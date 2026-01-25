package app.meeplebook.core.ui

import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import app.meeplebook.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class UiTextTest {

    private lateinit var fakeStringProvider: FakeStringProvider

    @Before
    fun setUp() {
        fakeStringProvider = FakeStringProvider()
    }

    @Test
    fun `plain text returns value as is`() {
        val uiText = uiText("Something went wrong")

        val resolved = uiText.asString(fakeStringProvider)

        assertEquals(
            "Something went wrong",
            resolved
        )
    }

    @Test
    fun `string resource without args resolves correctly`() {
        fakeStringProvider.setString(
            R.string.sync_never,
            "Never synced"
        )

        val uiText = uiTextRes(R.string.sync_never)

        val resolved = uiText.asString(fakeStringProvider)

        assertEquals(
            "Never synced",
            resolved
        )
    }

    @Test
    fun `string resource with args resolves correctly`() {
        fakeStringProvider.setString(
            R.string.collection_filter_all,
            "All (%d)"
        )

        val uiText = uiTextRes(
            R.string.collection_filter_all,
            3
        )

        val resolved = uiText.asString(fakeStringProvider)

        assertEquals(
            "All (3)",
            resolved
        )
    }

    @Test
    fun `plural resource uses singular form`() {
        @PluralsRes val fakePluralResId = 157

        fakeStringProvider.setPlural(
            resId = fakePluralResId,
            quantity = 1,
            value = "%d game"
        )
        fakeStringProvider.setPlural(
            resId = fakePluralResId,
            quantity = -1, // fallback / other
            value = "%d games"
        )

        val uiText = uiTextPlural(
            resId = fakePluralResId,
            quantity = 1
        )

        val resolved = uiText.asString(fakeStringProvider)

        assertEquals(
            "1 game",
            resolved
        )
    }

    @Test
    fun `plural resource uses plural form`() {
        @PluralsRes val fakePluralResId = 157

        fakeStringProvider.setPlural(
            resId = fakePluralResId,
            quantity = 1,
            value = "%d game"
        )
        fakeStringProvider.setPlural(
            resId = fakePluralResId,
            quantity = -1,
            value = "%d games"
        )

        val uiText = uiTextPlural(
            resId = fakePluralResId,
            quantity = 5
        )

        val resolved = uiText.asString(fakeStringProvider)

        assertEquals(
            "5 games",
            resolved
        )
    }

    @Test
    fun `plural resource respects explicit args`() {
        @PluralsRes val fakePluralResId = 157

        fakeStringProvider.setPlural(
            resId = fakePluralResId,
            quantity = -1,
            value = "%d total games"
        )

        val uiText = uiTextPlural(
            resId = fakePluralResId,
            quantity = 7,
            42 // explicit arg overrides default quantity insertion
        )

        val resolved = uiText.asString(fakeStringProvider)

        assertEquals(
            "42 total games",
            resolved
        )
    }

    @Test
    fun `Empty UiText resolves to empty string`() {
        val empty = UiText.Empty
        assertTrue(empty.isEmpty())
        assertFalse(empty.isNotEmpty())
        assertEquals("", empty.asString(fakeStringProvider))
    }

    @Test
    fun `Composite with only empty parts resolves to empty string`() {
        val composite = UiText.Composite(listOf(UiText.Empty, UiText.Empty))
        assertTrue(composite.isEmpty())
        assertFalse(composite.isNotEmpty())
        assertEquals("", composite.asString(fakeStringProvider))
    }

    @Test
    fun `Composite with mixed Plain and Empty resolves correctly`() {
        val composite = UiText.Composite(listOf(UiText.Plain("Hello"), UiText.Empty, UiText.Plain("World")))
        assertFalse(composite.isEmpty())
        assertTrue(composite.isNotEmpty())
        assertEquals("HelloWorld", composite.asString(fakeStringProvider))
    }

    @Test
    fun `Composite with nested Composite of only Empty isEmpty is true`() {
        val nestedComposite = UiText.Composite(
            listOf(
                UiText.Composite(listOf(UiText.Empty, UiText.Empty)),
                UiText.Empty
            )
        )
        assertTrue(nestedComposite.isEmpty())
        assertFalse(nestedComposite.isNotEmpty())
        assertEquals("", nestedComposite.asString(fakeStringProvider))
    }

    @Test
    fun `Composite with nested Composite with Plain isNotEmpty is true`() {
        val nestedComposite = UiText.Composite(
            listOf(
                UiText.Composite(listOf(UiText.Empty, UiText.Plain("A"))),
                UiText.Empty
            )
        )
        assertFalse(nestedComposite.isEmpty())
        assertTrue(nestedComposite.isNotEmpty())
        assertEquals("A", nestedComposite.asString(fakeStringProvider))
    }

    @Test
    fun `Composite with nested Composite resolves recursively`() {
        val nestedComposite = UiText.Composite(
            listOf(
                UiText.Plain("A"),
                UiText.Composite(listOf(UiText.Plain("B"), UiText.Empty, UiText.Plain("C"))),
                UiText.Plain("D")
            )
        )
        assertFalse(nestedComposite.isEmpty())
        assertTrue(nestedComposite.isNotEmpty())
        assertEquals("ABCD", nestedComposite.asString(fakeStringProvider))
    }

    @Test
    fun `Composite with resource and plural resolves via resolver`() {
        // setup fake mapping
        @StringRes val fakeStringRes = 1
        @PluralsRes val fakePluralRes = 2

        val resText = UiText.Res(fakeStringRes, listOf("X"))
        val pluralText = UiText.PluralRes(fakePluralRes, 3, listOf(3, "Y"))

        fakeStringProvider.setString(fakeStringRes, "Res:%s")
        fakeStringProvider.setPlural(fakePluralRes, 3, "Plural:%d %s")

        val composite = UiText.Composite(listOf(UiText.Plain("Start "), resText, UiText.Plain(" "), pluralText))
        val resolved = composite.asString(fakeStringProvider)

        assertEquals("Start Res:X Plural:3 Y", resolved)
        assertFalse(composite.isEmpty())
        assertTrue(composite.isNotEmpty())
    }

    @Test
    fun `UiText as argument inside resource resolves correctly`() {
        @StringRes val fakeStringRes = 10
        @StringRes val timeAgoRes = 11

        fakeStringProvider.setString(fakeStringRes, "Last synced: %s")
        fakeStringProvider.setString(timeAgoRes, "5 minutes ago")

        val timeAgoText = UiText.Res(timeAgoRes) // simulates formatTimeAgo returning UiText
        val mainText = UiText.Res(fakeStringRes, listOf(timeAgoText))

        val resolved = mainText.asString(fakeStringProvider)
        assertEquals("Last synced: 5 minutes ago", resolved)
    }

    @Test
    fun `Nested UiText arguments in plural resolves correctly`() {
        @PluralsRes val fakeStringRes = 20
        @StringRes val itemRes = 21

        fakeStringProvider.setPlural(fakeStringRes, 3, "%s items")
        fakeStringProvider.setString(itemRes, "Item %d")

        val innerText = UiText.Res(itemRes, listOf(5))
        val pluralText = UiText.PluralRes(fakeStringRes, 3, listOf(innerText))

        val resolved = pluralText.asString(fakeStringProvider)
        assertEquals("Item 5 items", resolved)
    }
}
