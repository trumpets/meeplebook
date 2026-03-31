package app.meeplebook.feature.addplay.ui.sections

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.meeplebook.R
import app.meeplebook.core.util.toEuFormattedString
import app.meeplebook.feature.addplay.AddPlayEvent
import app.meeplebook.ui.components.ScreenPadding
import app.meeplebook.ui.theme.MeepleBookTheme
import java.time.Instant
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddPlaySections.DateDuration(
    date: Instant,
    durationMinutes: Int?,
    onEvent: (AddPlayEvent) -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }

    val formatted = remember(date) {
        date.atZone(ZoneId.systemDefault()).toLocalDate().toEuFormattedString()
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ScreenPadding.Horizontal, vertical = ScreenPadding.Small),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        OutlinedTextField(
            value = formatted,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.add_play_date_label)) },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            modifier = Modifier
                .weight(1f)
                .clickable { showDatePicker = true }
                .testTag("dateField"),
            enabled = false,
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledBorderColor = MaterialTheme.colorScheme.outline,
                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledTrailingIconColor = MaterialTheme.colorScheme.primary,
            )
        )

        OutlinedTextField(
            value = durationMinutes?.toString() ?: "",
            onValueChange = { raw ->
                val parsed = if (raw.isEmpty()) null else raw.toIntOrNull()
                if (raw.isEmpty() || parsed != null) {
                    onEvent(AddPlayEvent.MetadataEvent.DurationChanged(parsed))
                }
            },
            label = { Text(stringResource(R.string.add_play_duration_label)) },
            placeholder = { Text(stringResource(R.string.add_play_duration_placeholder)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier
                .width(112.dp)
                .testTag("durationField")
        )
    }

    if (showDatePicker) {
        val initialMillis = remember(date) {
            date.atZone(ZoneId.systemDefault())
                .toLocalDate()
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        }
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val selectedInstant = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                            .atStartOfDay(ZoneId.systemDefault())
                            .toInstant()
                        onEvent(AddPlayEvent.MetadataEvent.DateChanged(selectedInstant))
                    }
                    showDatePicker = false
                }) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
// ── Previews ──────────────────────────────────────────────────────────────────

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DateDurationSectionNoDurationPreview() {
    MeepleBookTheme {
        AddPlaySections.DateDuration(
            date = Instant.parse("2026-03-30T18:00:00Z"),
            durationMinutes = null,
            onEvent = {},
        )
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DateDurationSectionWithDurationPreview() {
    MeepleBookTheme {
        AddPlaySections.DateDuration(
            date = Instant.parse("2026-03-30T18:00:00Z"),
            durationMinutes = 90,
            onEvent = {},
        )
    }
}
