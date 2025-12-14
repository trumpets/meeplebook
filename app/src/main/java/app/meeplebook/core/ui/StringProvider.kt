package app.meeplebook.core.ui

import androidx.annotation.StringRes

interface StringProvider {
    fun get(@StringRes resId: Int, vararg args: Any): String
}