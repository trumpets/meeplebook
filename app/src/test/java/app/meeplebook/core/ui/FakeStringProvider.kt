package app.meeplebook.core.ui

import androidx.annotation.StringRes

/**
 * Fake implementation of [StringProvider] for testing purposes.
 * Returns string resource IDs as strings (e.g., "R.string.test_string" → "test_string")
 * or custom mappings for specific resources.
 */
class FakeStringProvider : StringProvider {

    private val customStrings = mutableMapOf<Int, String>()

    override fun get(@StringRes resId: Int, vararg args: Any): String {
        // Check custom mappings first
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

    /**
     * Sets a custom string for a specific resource ID.
     * Useful for testing specific string formatting scenarios.
     */
    fun setString(@StringRes resId: Int, value: String) {
        customStrings[resId] = value
    }

    /**
     * Clears all custom string mappings.
     */
    fun clear() {
        customStrings.clear()
    }
}
