package app.meeplebook.core.ui

import androidx.annotation.PluralsRes
import androidx.annotation.StringRes

interface StringProvider {
    fun get(@StringRes resId: Int, vararg args: Any): String

    fun getPlural(@PluralsRes resId: Int, quantity: Int, vararg args: Any): String
}