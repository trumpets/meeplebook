package app.meeplebook.core.ui

import androidx.annotation.PluralsRes
import androidx.annotation.StringRes

fun uiText(value: String): UiText =
    UiText.Plain(value)

fun uiTextRes(@StringRes resId: Int, vararg args: Any): UiText =
    UiText.Res(
        resId = resId,
        args = if (args.isEmpty()) emptyList() else args.toList()
    )

fun uiTextPlural(@PluralsRes resId: Int, quantity: Int, vararg args: Any): UiText =
    UiText.PluralRes(
        resId = resId,
        quantity = quantity,
        args = if (args.isEmpty()) listOf(quantity) else args.toList()
    )