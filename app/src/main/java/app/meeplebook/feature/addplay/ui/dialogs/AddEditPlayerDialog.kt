package app.meeplebook.feature.addplay.ui.dialogs

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.graphics.toColorInt
import app.meeplebook.R
import app.meeplebook.core.plays.domain.PlayerIdentity
import app.meeplebook.core.plays.model.PlayerColor
import app.meeplebook.core.ui.UiText
import app.meeplebook.core.ui.uiText
import app.meeplebook.core.ui.uiTextRes
import app.meeplebook.feature.addplay.AddEditPlayerDialogState
import app.meeplebook.ui.components.UiTextText
import app.meeplebook.ui.theme.MeepleBookTheme

/**
 * Dialog for adding a completely new player or editing the details of an existing one.
 *
 * Shows three text fields: Player Name, BGG Username, and Team/Color. Both name and
 * username fields show an autocomplete dropdown populated with matching players from the
 * local database. Selecting a suggestion from either dropdown fills both fields.
 *
 * When the color field contains a valid [PlayerColor] string a filled color swatch circle
 * is shown inline next to the field.
 *
 * The dialog is in "add" mode when [state]'s `editingIdentity` is `null`, or in "edit" mode
 * when it is pre-filled with an existing player's data.
 *
 * @param state Current dialog state (field values and autocomplete suggestions).
 * @param colorsHistory History of recently used colors to pre-populate the color picker.
 * @param onNameChanged Called with the updated name string when the user types in the name field.
 * @param onUsernameChanged Called with the updated username string when the user types in the username field.
 * @param onColorChanged Called with the updated color string when the user types in the color field.
 * @param onConfirm Called when the user taps Confirm.
 * @param onDismiss Called when the user taps Cancel or dismisses the dialog.
 */
@Composable
fun AddEditPlayerDialog(
    state: AddEditPlayerDialogState,
    colorsHistory: List<PlayerColor>,
    onNameChanged: (String) -> Unit,
    onUsernameChanged: (String) -> Unit,
    onColorChanged: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val title = if (state.editingIdentity == null) {
        stringResource(R.string.add_edit_player_title_add)
    } else {
        stringResource(R.string.add_edit_player_title_edit)
    }

    val resolvedColor = PlayerColor.fromString(state.color)

    var showColorPicker by remember { mutableStateOf(false) }

    if (showColorPicker) {
        ColorPickerDialog(
            currentColor = resolvedColor,
            colorsHistory = colorsHistory,
            onColorSelected = { color ->
                onColorChanged(color.colorString)
                showColorPicker = false
            },
            onDismiss = { showColorPicker = false },
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true),
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )

                Spacer(modifier = Modifier.height(20.dp))

                // ── Name field with autocomplete ────────────────────────────
                PlayerAutocompleteField(
                    value = state.name,
                    onValueChange = onNameChanged,
                    label = stringResource(R.string.add_edit_player_name_label),
                    suggestions = state.nameSuggestions,
                    onSuggestionSelected = { suggestion ->
                        onNameChanged(suggestion.name)
                        onUsernameChanged(suggestion.username.orEmpty())
                    },
                    suggestionText = { s ->
                        s.username?.let {
                            uiTextRes(
                                R.string.player_name_with_username,
                                s.name,
                                it,
                            )
                        } ?: uiText(s.name)
                    },
                )

                Spacer(modifier = Modifier.height(12.dp))

                // ── Username field with autocomplete ────────────────────────
                PlayerAutocompleteField(
                    value = state.username,
                    onValueChange = onUsernameChanged,
                    label = stringResource(R.string.add_edit_player_username_label),
                    suggestions = state.usernameSuggestions,
                    onSuggestionSelected = { suggestion ->
                        onNameChanged(suggestion.name)
                        onUsernameChanged(suggestion.username.orEmpty())
                    },
                    suggestionText = { s ->
                        s.username?.let {
                            uiTextRes(
                                R.string.player_name_with_username,
                                s.name,
                                it,
                            )
                        } ?: uiText(s.name)
                    },
                )

                Spacer(modifier = Modifier.height(12.dp))

                // ── Color field with always-visible clickable swatch ────────
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = state.color,
                        onValueChange = onColorChanged,
                        label = { Text(stringResource(R.string.add_edit_player_color_label)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    val swatchColor = resolvedColor?.let { Color(it.hexColor.toColorInt()) }
                    val colorDesc = resolvedColor?.let {
                        stringResource(R.string.add_edit_player_color_preview_description, it.colorString)
                    } ?: stringResource(R.string.add_edit_player_color_pick_description)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(48.dp)
                            .clickable { showColorPicker = true }
                            .semantics { contentDescription = colorDesc },
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .then(
                                    if (swatchColor != null) {
                                        Modifier.background(swatchColor)
                                    } else {
                                        Modifier
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                            .border(
                                                width = 1.dp,
                                                color = MaterialTheme.colorScheme.outline,
                                                shape = CircleShape,
                                            )
                                    }
                                ),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // ── Buttons ─────────────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.add_edit_player_cancel))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = onConfirm,
                        enabled = state.name.isNotBlank(),
                    ) {
                        Text(stringResource(R.string.add_edit_player_confirm))
                    }
                }
            }
        }
    }
}

/**
 * An [OutlinedTextField] paired with a [DropdownMenu] that shows [suggestions] while the
 * field is focused.
 *
 * Selecting a suggestion via [onSuggestionSelected] collapses the dropdown and allows the
 * caller to fill related fields.
 */
@Composable
private fun <T> PlayerAutocompleteField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    suggestions: List<T>,
    onSuggestionSelected: (T) -> Unit,
    suggestionText: (T) -> UiText,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val showDropdown = expanded && suggestions.isNotEmpty()

    Box(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focusState -> expanded = focusState.isFocused },
        )
        DropdownMenu(
            expanded = showDropdown,
            onDismissRequest = { expanded = false },
        ) {
            suggestions.forEach { suggestion ->
                DropdownMenuItem(
                    text = { UiTextText(suggestionText(suggestion)) },
                    onClick = {
                        expanded = false
                        onSuggestionSelected(suggestion)
                    },
                )
            }
        }
    }
}

// ── Previews ──────────────────────────────────────────────────────────────────

private class AddEditPlayerDialogPreviewProvider :
    PreviewParameterProvider<AddEditPlayerDialogState> {
    override val values = sequenceOf(
        // Add mode, empty
        AddEditPlayerDialogState(),
        // Add mode with suggestions
        AddEditPlayerDialogState(
            name = "Ali",
            nameSuggestions = listOf(
                PlayerIdentity("Alice", username = "alicebgg", userId = null),
                PlayerIdentity("Alicia", username = null, userId = null),
            ),
        ),
        // Add mode with valid color swatch
        AddEditPlayerDialogState(
            name = "Bob",
            username = "bobbgg",
            color = "Red",
        ),
        // Edit mode
        AddEditPlayerDialogState(
            editingIdentity = PlayerIdentity("Charlie", username = "charlie99", userId = null),
            name = "Charlie",
            username = "charlie99",
            color = "Blue",
        ),
    )
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AddEditPlayerDialogPreview(
    @PreviewParameter(AddEditPlayerDialogPreviewProvider::class) state: AddEditPlayerDialogState,
) {
    MeepleBookTheme {
        AddEditPlayerDialog(
            state = state,
            colorsHistory = emptyList(),
            onNameChanged = {},
            onUsernameChanged = {},
            onColorChanged = {},
            onConfirm = {},
            onDismiss = {},
        )
    }
}
