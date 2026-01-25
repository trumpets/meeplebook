package app.meeplebook.core.ui

import androidx.annotation.PluralsRes
import androidx.annotation.StringRes

sealed interface UiText {

    data class Plain(
        val value: String
    ) : UiText

    data class Res(
        @StringRes val resId: Int,
        val args: List<Any> = emptyList()
    ) : UiText

    data class PluralRes(
        @PluralsRes val resId: Int,
        val quantity: Int,
        val args: List<Any> = emptyList()
    ) : UiText

    object Empty : UiText

    data class Composite(val parts: List<UiText>) : UiText
}