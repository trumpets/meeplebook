package app.meeplebook.core.ui

import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource

@Composable
fun UiText.asString(): String = when (this) {
    is UiText.Plain -> value

    is UiText.Res -> {
        // Resolve any UiText arguments recursively (rarely happens, but possible)
        val resolvedArgs = args.map { arg ->
            when (arg) {
                is UiText -> arg.asString()
                else -> arg
            }
        }.toTypedArray()
        stringResource(resId, *resolvedArgs)
    }

    is UiText.PluralRes -> {
        val resolvedArgs = args.map { arg ->
            when (arg) {
                is UiText -> arg.asString()
                else -> arg
            }
        }.toTypedArray()
        pluralStringResource(resId, quantity, *resolvedArgs)
    }

    UiText.Empty -> ""

    is UiText.Composite -> buildString {
        for (part in parts) {
            append(part.asString()) // OK: inside @Composable function
        }
    }
}

fun UiText.asString(stringProvider: StringProvider): String = when (this) {
    is UiText.Plain -> value

    is UiText.Res -> stringProvider.get(resId, *args.map {
        if (it is UiText) it.asString(stringProvider) else it
    }.toTypedArray())

    is UiText.PluralRes -> stringProvider.getPlural(resId, quantity, *args.map {
        if (it is UiText) it.asString(stringProvider) else it
    }.toTypedArray())

    UiText.Empty -> ""

    is UiText.Composite -> parts.joinToString("") { it.asString(stringProvider) }
}

fun UiText.asString(resources: Resources): String = when (this) {
    is UiText.Plain -> value

    is UiText.Res -> resources.getString(resId, *args.map {
        if (it is UiText) it.asString(resources) else it
    }.toTypedArray())

    is UiText.PluralRes -> resources.getQuantityString(resId, quantity, *args.map {
        if (it is UiText) it.asString(resources) else it
    }.toTypedArray())

    UiText.Empty -> ""

    is UiText.Composite -> parts.joinToString("") { it.asString(resources) }
}

fun UiText.isEmpty(): Boolean = when (this) {
    UiText.Empty -> true
    is UiText.Composite -> parts.all { it.isEmpty() }
    else -> false
}

fun UiText.isNotEmpty(): Boolean = !isEmpty()

fun UiText.wrap(prefix: String, suffix: String): UiText =
    uiTextCombine(uiText(prefix), this, uiText(suffix))