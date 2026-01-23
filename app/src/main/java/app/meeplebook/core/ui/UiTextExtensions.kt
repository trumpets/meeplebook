package app.meeplebook.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource

@Composable
fun UiText.asString(): String = when (this) {
    is UiText.Plain -> value
    is UiText.Res -> stringResource(resId, *args.toTypedArray())
    is UiText.PluralRes -> pluralStringResource(resId, quantity, *args.toTypedArray())
}

fun UiText.asString(stringProvider: StringProvider): String = when (this) {
    is UiText.Plain -> value
    is UiText.Res -> stringProvider.get(resId, *args.toTypedArray())
    is UiText.PluralRes -> stringProvider.getPlural(resId, quantity, *args.toTypedArray())
}