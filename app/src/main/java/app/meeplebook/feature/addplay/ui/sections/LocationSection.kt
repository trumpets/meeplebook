package app.meeplebook.feature.addplay.ui.sections

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import app.meeplebook.R
import app.meeplebook.feature.addplay.AddPlayEvent
import app.meeplebook.feature.addplay.LocationState
import app.meeplebook.feature.addplay.ui.previewLocationState
import app.meeplebook.ui.components.ScreenPadding
import app.meeplebook.ui.theme.MeepleBookTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationSection(
    locationState: LocationState,
    onEvent: (AddPlayEvent) -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val showSuggestions = isFocused && locationState.suggestions.isNotEmpty()
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = ScreenPadding.Horizontal,
                vertical = ScreenPadding.Small
            )
    ) {
        ExposedDropdownMenuBox(
            expanded = showSuggestions,
            onExpandedChange = {}
        ) {
            OutlinedTextField(
                value = locationState.value ?: "",
                onValueChange = { onEvent(AddPlayEvent.MetadataEvent.LocationChanged(it)) },
                label = { Text(stringResource(R.string.add_play_location_label)) },
                placeholder = { Text(stringResource(R.string.add_play_location_placeholder)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        locationState.suggestions.firstOrNull()?.let {
                            onEvent(AddPlayEvent.MetadataEvent.LocationChanged(it))
                        }
                        focusManager.clearFocus(true)
                    }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable)
                    .onFocusChanged { isFocused = it.isFocused }
                    .testTag("locationField")
            )

            ExposedDropdownMenu(
                expanded = showSuggestions,
                onDismissRequest = { focusManager.clearFocus() },
                modifier = Modifier.testTag("locationSuggestions")
            ) {
                locationState.suggestions.forEach { suggestion ->
                    DropdownMenuItem(
                        text = { Text(suggestion) },
                        onClick = {
                            onEvent(AddPlayEvent.MetadataEvent.LocationChanged(suggestion))
                            focusManager.clearFocus()
                        }
                    )
                }
            }
        }

        // Recent / top locations chips (always visible when available)
        val topLocations = locationState.recentLocations.take(10)
        if (topLocations.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 4.dp),
                modifier = Modifier.testTag("recentLocationChips")
            ) {
                items(topLocations) { loc ->
                    FilterChip(
                        selected = locationState.value == loc,
                        onClick = { onEvent(AddPlayEvent.MetadataEvent.LocationChanged(loc)) },
                        label = { Text(loc) }
                    )
                }
            }
        }
    }
}
// ── Previews ──────────────────────────────────────────────────────────────────

private class LocationSectionPreviewProvider : PreviewParameterProvider<LocationState> {
    override val values = sequenceOf(
        previewLocationState(),
        previewLocationState(
            value = "Home",
            recentLocations = listOf("Home", "Game Café", "Bob's place"),
        ),
        previewLocationState(
            value = "Gam",
            isFocused = true,
            suggestions = listOf("Game Café", "Game Vault"),
        ),
    )
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun LocationSectionPreview(
    @PreviewParameter(LocationSectionPreviewProvider::class) state: LocationState,
) {
    MeepleBookTheme {
        LocationSection(locationState = state, onEvent = {})
    }
}
