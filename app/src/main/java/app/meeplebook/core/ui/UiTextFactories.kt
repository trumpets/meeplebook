package app.meeplebook.core.ui

import androidx.annotation.PluralsRes
import androidx.annotation.StringRes

fun uiText(value: String?): UiText =
    if (value.isNullOrEmpty()) UiText.Empty else UiText.Plain(value)

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

fun uiTextEmpty(): UiText = UiText.Empty

fun uiTextCombine(vararg parts: UiText) = UiText.Composite(parts.toList())

fun uiTextJoin(vararg parts: UiText, separator: String = ", "): UiText {

    // Flatten non-empty parts
    val nonEmptyParts = parts.filter { it.isNotEmpty() }

    if (nonEmptyParts.isEmpty()) return UiText.Empty
    if (nonEmptyParts.size == 1) return nonEmptyParts.first()

    // Insert separator between elements
    val compositeParts = mutableListOf<UiText>()
    nonEmptyParts.forEachIndexed { index, part ->
        if (index > 0) {
            compositeParts += UiText.Plain(separator)
        }

        compositeParts += part
    }

    return UiText.Composite(compositeParts)
}