package app.meeplebook.feature.addplay.ui.sections

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import app.meeplebook.R
import app.meeplebook.feature.addplay.AddPlayEvent
import app.meeplebook.feature.addplay.AddPlayUiState
import app.meeplebook.feature.addplay.ui.previewGameSelectedState
import app.meeplebook.ui.components.ScreenPadding
import app.meeplebook.ui.theme.MeepleBookTheme

@Composable
fun QuantityIncompleteRow(
    state: AddPlayUiState.GameSelected,
    onEvent: (AddPlayEvent) -> Unit
) {
    // Local raw string so the field can be empty mid-edit without the reducer snapping
    // it back to "1". We only emit an event when the value is a valid positive integer.
    var rawInput by remember(state.showQuantity) { mutableStateOf(state.quantity.toString()) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ScreenPadding.Horizontal, vertical = ScreenPadding.Small),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (state.showQuantity) {
            OutlinedTextField(
                value = rawInput,
                onValueChange = { raw ->
                    // Accept only digits (no sign, no decimal).
                    if (raw.all { it.isDigit() }) {
                        rawInput = raw
                        val parsed = raw.toIntOrNull()
                        if (parsed != null && parsed > 0) {
                            onEvent(AddPlayEvent.MetadataEvent.QuantityChanged(parsed))
                        }
                    }
                },
                label = { Text(stringResource(R.string.add_play_quantity_label)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .width(96.dp)
                    .onFocusChanged { focusState ->
                        // Snap back to 1 when the field loses focus
                        // so it never stays blank after the user taps away.
                        if (!focusState.isFocused && rawInput.toIntOrNull().let { it == null || it < 1 }) {
                            rawInput = "1"
                        }
                    }
                    .testTag("quantityField")
            )
        }

        if (state.showIncomplete) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = stringResource(R.string.add_play_optional_incomplete),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = state.incomplete,
                    onCheckedChange = { onEvent(AddPlayEvent.MetadataEvent.IncompleteToggled(it)) },
                    modifier = Modifier.testTag("incompleteToggle")
                )
            }
        }
    }
}

// ── Previews ──────────────────────────────────────────────────────────────────

private class QuantityIncompleteSectionPreviewProvider :
    PreviewParameterProvider<AddPlayUiState.GameSelected> {
    override val values = sequenceOf(
        previewGameSelectedState(showQuantity = true, showIncomplete = false),
        previewGameSelectedState(showQuantity = false, showIncomplete = true),
        previewGameSelectedState(showQuantity = true, showIncomplete = true),
    )
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun QuantityIncompleteRowPreview(
    @PreviewParameter(QuantityIncompleteSectionPreviewProvider::class)
    state: AddPlayUiState.GameSelected,
) {
    MeepleBookTheme {
        QuantityIncompleteRow(state = state, onEvent = {})
    }
}
