package app.meeplebook.core.ui

import androidx.annotation.PluralsRes
import androidx.annotation.StringRes

/**
 * Fake implementation of [StringProvider] for testing purposes.
 * Returns string resource IDs as strings (e.g., "R.string.test_string" → "test_string")
 * or custom mappings for specific resources.
 */
class FakeStringProvider : StringProvider {

    private val customStrings = mutableMapOf<Int, String>()
    private val plurals = mutableMapOf<Int, Map<Int, String>>()

    override fun get(@StringRes resId: Int, vararg args: Any): String {
        val customString = customStrings[resId]
        if (customString != null) {
            return if (args.isEmpty()) {
                customString
            } else {
                String.format(customString, *args)
            }
        }

        // Default behavior: return resource ID as string
        return "string_$resId"
    }

    override fun getPlural(
        resId: Int,
        quantity: Int,
        vararg args: Any
    ): String {
        val quantityMap = plurals[resId]
        val template =
            quantityMap?.get(quantity)
                ?: quantityMap?.get(-1) // fallback
                ?: return "plural_$resId($quantity)" // early return if no template found

        return if (args.isEmpty()) {
            String.format(template, quantity) // use quantity as default arg
        } else {
            String.format(template, *args) // use explicit args
        }
    }

    /**
     * Sets a custom string for a specific resource ID.
     * Useful for testing specific string formatting scenarios.
     */
    fun setString(@StringRes resId: Int, value: String) {
        customStrings[resId] = value
    }

    /**
     * quantity = exact match
     * quantity = -1 -> fallback (e.g. "other")
     */
    fun setPlural(
        @PluralsRes resId: Int,
        quantity: Int,
        value: String
    ) {
        val map = plurals.getOrPut(resId) { mutableMapOf() } as MutableMap
        map[quantity] = value
    }

    /**
     * Clears all custom string mappings.
     */
    fun clearStrings() {
        customStrings.clear()
    }

    /**
     * Clears all custom plural string mappings.
     */
    fun clearPlurals() {
        plurals.clear()
    }
}
