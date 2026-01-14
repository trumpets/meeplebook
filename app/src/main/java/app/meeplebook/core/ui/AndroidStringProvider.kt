package app.meeplebook.core.ui

import android.content.Context
import androidx.annotation.StringRes

class AndroidStringProvider(
    private val context: Context
) : StringProvider {
    override fun get(@StringRes resId: Int, vararg args: Any): String =
        context.getString(resId, *args)
}