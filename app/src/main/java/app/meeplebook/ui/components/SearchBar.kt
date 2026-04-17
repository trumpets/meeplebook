package app.meeplebook.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

@Composable
fun SearchBar(
    query: String,
    @StringRes placeholderResId: Int,
    onQueryChanged: (String) -> Unit
) {
    TextField(
        value = query,
        onValueChange = onQueryChanged,
        leadingIcon = { Icon(Icons.Default.Search, null) },
        placeholder = { Text(stringResource(placeholderResId)) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
            .testTag("searchField"),
        singleLine = true,
        shape = RoundedCornerShape(12.dp)
    )
}