package app.meeplebook.feature.addplay.ui.dialogs

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import app.meeplebook.core.util.LuminanceUtils
import app.meeplebook.feature.addplay.PlayerEntryUi
import app.meeplebook.ui.theme.MeepleBookTheme

/**
 * Returns [colorsHistory] sorted by [PlayerColor.ordinal].
 */
internal fun sortedHistoryColors(colorsHistory: List<PlayerColor>): List<PlayerColor> =
    colorsHistory.sortedBy { it.ordinal }

/**
 * Returns all [PlayerColor] entries NOT in [colorsHistory], sorted by [PlayerColor.ordinal].
 */
internal fun remainingColors(colorsHistory: List<PlayerColor>): List<PlayerColor> =
    PlayerColor.entries.filter { it !in colorsHistory }.sortedBy { it.ordinal }

/**
 * A dialog that shows all available [PlayerColor] values as labelled coloured circles,
 * allowing the user to pick a colour for a player.
 *
 * **Smart expand behaviour:**
 * - If [colorsHistory] is **non-empty**: the dialog opens showing only the history colours
 *   (sorted by enum ordinal) plus a **More** button. Tapping More reveals the remaining
 *   colours (also sorted by ordinal) below a divider.
 * - If [colorsHistory] is **empty**: all colours are shown immediately (expanded form).
 *
 * Tapping outside the dialog or pressing back dismisses it without making a selection.
 *
 * @param player          The player whose colour is being chosen.
 * @param colorsHistory   Distinct colours already used for this game (may be empty).
 * @param onColorSelected Called with the chosen [PlayerColor] when the user taps a circle.
 * @param onDismiss       Called when the dialog is dismissed without a selection.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorPickerDialog(
    player: PlayerEntryUi,
    colorsHistory: List<PlayerColor>,
    onColorSelected: (PlayerColor) -> Unit,
    onDismiss: () -> Unit,
) {
    val currentColor = PlayerColor.fromString(player.color)
    val startExpanded = colorsHistory.isEmpty()
    var expanded by remember { mutableStateOf(startExpanded) }

    // History sorted by enum ordinal; remaining (not in history) also sorted by ordinal.
    val historySorted = sortedHistoryColors(colorsHistory)
    val remaining = remainingColors(colorsHistory)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true),
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 6.dp,
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // ── Title ────────────────────────────────────────────────────
                Text(
                    text = stringResource(R.string.color_picker_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (startExpanded) {
                    // All colours at once — no history to separate.
                    ColorCircleGrid(
                        colors = PlayerColor.entries,
                        currentColor = currentColor,
                        onColorSelected = onColorSelected,
                    )
                } else {
                    // History colours.
                    ColorCircleGrid(
                        colors = historySorted,
                        currentColor = currentColor,
                        onColorSelected = onColorSelected,
                    )

                    if (remaining.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))

                        if (!expanded) {
                            OutlinedButton(
                                onClick = { expanded = true },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ExpandMore,
                                    contentDescription = null,
                                )
                                Text(
                                    text = stringResource(R.string.color_picker_more),
                                    modifier = Modifier.padding(start = 4.dp),
                                )
                            }
                        }

                        AnimatedVisibility(
                            visible = expanded,
                            enter = expandVertically() + fadeIn(),
                        ) {
                            Column {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                                ColorCircleGrid(
                                    colors = remaining,
                                    currentColor = currentColor,
                                    onColorSelected = onColorSelected,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColorCircleGrid(
    colors: List<PlayerColor>,
    currentColor: PlayerColor?,
    onColorSelected: (PlayerColor) -> Unit,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        maxItemsInEachRow = 5,
    ) {
        colors.forEach { color ->
            ColorCircleItem(
                color = color,
                isSelected = color == currentColor,
                onSelected = { onColorSelected(color) },
            )
        }
    }
}

@Composable
private fun ColorCircleItem(
    color: PlayerColor,
    isSelected: Boolean,
    onSelected: () -> Unit,
) {
    val circleColor = Color(color.hexColor.toColorInt())
    val borderColor = if (isSelected)
        MaterialTheme.colorScheme.primary
    else
        MaterialTheme.colorScheme.outline
    val borderWidth = if (isSelected) 2.5.dp else 1.dp
    val contentDesc = color.colorString

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .semantics { contentDescription = contentDesc }
            .clickable(onClick = onSelected),
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(circleColor)
                .border(borderWidth, borderColor, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (isSelected) {
                // Checkmark — use contrasting colour for visibility.
                val checkTint = LuminanceUtils.contrastColorFor(circleColor)
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = checkTint,
                    modifier = Modifier.size(24.dp),
                )
            }
        }

        Text(
            text = color.colorString,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(width = 48.dp, height = 16.dp),
        )
    }
}

// ── Previews ──────────────────────────────────────────────────────────────────

private data class ColorPickerDialogPreviewState(
    val player: PlayerEntryUi,
    val colorsHistory: List<PlayerColor>,
)

private class ColorPickerDialogPreviewProvider :
    PreviewParameterProvider<ColorPickerDialogPreviewState> {
    override val values = sequenceOf(
        // No history — opens expanded showing all colours.
        ColorPickerDialogPreviewState(
            player = PlayerEntryUi(
                playerIdentity = PlayerIdentity(
                    name = "Alice",
                    username = "alicebgg",
                    userId = null
                ),
                startPosition = 1,
                color = PlayerColor.BLUE.colorString,
                score = null,
                isWinner = false,
            ),
            colorsHistory = emptyList(),
        ),
        // Has history — opens compact with MORE button.
        ColorPickerDialogPreviewState(
            player = PlayerEntryUi(
                playerIdentity = PlayerIdentity(name = "Bob", username = null, userId = null),
                startPosition = 2,
                color = PlayerColor.RED.colorString,
                score = null,
                isWinner = false,
            ),
            colorsHistory = listOf(PlayerColor.RED, PlayerColor.BLUE, PlayerColor.GREEN),
        ),
    )
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ColorPickerDialogPreview(
    @PreviewParameter(ColorPickerDialogPreviewProvider::class) state: ColorPickerDialogPreviewState,
) {
    MeepleBookTheme {
        ColorPickerDialog(
            player = state.player,
            colorsHistory = state.colorsHistory,
            onColorSelected = {},
            onDismiss = {},
        )
    }
}
