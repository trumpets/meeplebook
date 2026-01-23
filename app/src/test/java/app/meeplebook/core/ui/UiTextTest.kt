package app.meeplebook.core.ui

import androidx.annotation.StringRes
import app.meeplebook.R
import org.junit.Assert.assertEquals
import org.junit.Test

class UiTextTest {

    private val strings = FakeStringProvider()

    @Test
    fun `plain text returns value as is`() {
        val uiText = uiText("Something went wrong")

        val resolved = uiText.asString(strings)

        assertEquals(
            "Something went wrong",
            resolved
        )
    }

    @Test
    fun `string resource without args resolves correctly`() {
        strings.setString(
            R.string.sync_never,
            "Never synced"
        )

        val uiText = uiTextRes(R.string.sync_never)

        val resolved = uiText.asString(strings)

        assertEquals(
            "Never synced",
            resolved
        )
    }

    @Test
    fun `string resource with args resolves correctly`() {
        strings.setString(
            R.string.collection_filter_all,
            "All (%d)"
        )

        val uiText = uiTextRes(
            R.string.collection_filter_all,
            3
        )

        val resolved = uiText.asString(strings)

        assertEquals(
            "All (3)",
            resolved
        )
    }

    @Test
    fun `plural resource uses singular form`() {
        @StringRes val fakePluralResId = 157

        strings.setPlural(
            resId = fakePluralResId,
            quantity = 1,
            value = "%d game"
        )
        strings.setPlural(
            resId = fakePluralResId,
            quantity = -1, // fallback / other
            value = "%d games"
        )

        val uiText = uiTextPlural(
            resId = fakePluralResId,
            quantity = 1
        )

        val resolved = uiText.asString(strings)

        assertEquals(
            "1 game",
            resolved
        )
    }

    @Test
    fun `plural resource uses plural form`() {
        @StringRes val fakePluralResId = 157

        strings.setPlural(
            resId = fakePluralResId,
            quantity = 1,
            value = "%d game"
        )
        strings.setPlural(
            resId = fakePluralResId,
            quantity = -1,
            value = "%d games"
        )

        val uiText = uiTextPlural(
            resId = fakePluralResId,
            quantity = 5
        )

        val resolved = uiText.asString(strings)

        assertEquals(
            "5 games",
            resolved
        )
    }

    @Test
    fun `plural resource respects explicit args`() {
        @StringRes val fakePluralResId = 157

        strings.setPlural(
            resId = fakePluralResId,
            quantity = -1,
            value = "%d total games"
        )

        val uiText = uiTextPlural(
            resId = fakePluralResId,
            quantity = 7,
            42 // explicit arg overrides default quantity insertion
        )

        val resolved = uiText.asString(strings)

        assertEquals(
            "42 total games",
            resolved
        )
    }
}
