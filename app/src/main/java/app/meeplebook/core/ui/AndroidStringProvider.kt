package app.meeplebook.core.ui

import android.content.Context
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes

class AndroidStringProvider(
    private val context: Context
) : StringProvider {

    override fun get(@StringRes resId: Int, vararg args: Any): String {
        return context.getString(resId, *args)
    }

    override fun getPlural(
        @PluralsRes resId: Int,
        quantity: Int,
        vararg args: Any
    ): String {
        return context.resources.getQuantityString(resId, quantity, *args)
    }
}