package app.meeplebook.ui.components

import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import app.meeplebook.core.ui.UiText
import app.meeplebook.core.ui.asString

@Composable
fun Text(
    text: UiText,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = style.color,
) {
    androidx.compose.material3.Text(
        text = text.asString(),
        modifier = modifier,
        style = style,
        color = color
    )
}