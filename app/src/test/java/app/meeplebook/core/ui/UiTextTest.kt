package app.meeplebook.core.ui

import androidx.annotation.PluralsRes
import app.meeplebook.R
import org.junit.Assert.assertEquals
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
}
