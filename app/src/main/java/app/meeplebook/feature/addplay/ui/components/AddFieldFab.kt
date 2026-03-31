package app.meeplebook.feature.addplay.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import app.meeplebook.R
import app.meeplebook.feature.addplay.AddPlayEvent
import app.meeplebook.feature.addplay.OptionalField

@Composable
fun AddFieldFab(
    hiddenFields: List<OptionalField>,
    onEvent: (AddPlayEvent) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        FloatingActionButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = stringResource(R.string.add_play_add_optional_field)
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            hiddenFields.forEach { field ->
                val label = when (field) {
                    OptionalField.QUANTITY -> stringResource(R.string.add_play_optional_quantity)
                    OptionalField.INCOMPLETE -> stringResource(R.string.add_play_optional_incomplete)
                    OptionalField.COMMENTS -> stringResource(R.string.add_play_optional_comments)
                }
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        onEvent(AddPlayEvent.MetadataEvent.ShowOptionalField(field))
                        expanded = false
                    }
                )
            }
        }
    }
}