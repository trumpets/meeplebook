package app.meeplebook.feature.addplay.ui.dialogs

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.graphics.toColorInt
import app.meeplebook.R
import app.meeplebook.core.plays.domain.PlayerIdentity
import app.meeplebook.core.plays.model.PlayerColor
import app.meeplebook.feature.addplay.PlayerEntryUi
import app.meeplebook.ui.theme.MeepleBookTheme

/**
 * Converts a nullable [Double] score to a clean display/input string.
 * Strips trailing ".0" from whole numbers (e.g. `42.0` → `"42"`, `42.5` → `"42.5"`).
 */
internal fun Double?.toScoreInputString(): String = when {
    this == null -> ""
    this == toLong().toDouble() -> toLong().toString()
    else -> toBigDecimal().stripTrailingZeros().toPlainString()
}

/**
 * Parses the input string to a [Double] score, or `null` if blank or just a minus sign.
 */
internal fun String.toScoreOrNull(): Double? = when {
    isBlank() || this == "-" -> null
    else -> toDoubleOrNull()
}

/**
 * A numpad dialog for entering a player's score.
 *
 * The header is tinted with the player's assigned [PlayerColor] when one is set.
 * Dismissing by tapping outside or pressing back does **not** change the score.
 * Confirming with an empty input clears the score to null.
 *
 * @param player       The player whose score is being entered.
 * @param onConfirm    Called with the parsed score (null = no score) when the user confirms.
 * @param onDismiss    Called when the user dismisses without confirming.
 */
@Composable
fun ScoreInputDialog(
    player: PlayerEntryUi,
    onConfirm: (Double?) -> Unit,
    onDismiss: () -> Unit,
) {
    var input by remember(player) { mutableStateOf(player.score.toScoreInputString()) }
    val haptic = LocalHapticFeedback.current

    val colorEnum = PlayerColor.fromString(player.color)
    val headerColor = colorEnum?.let { Color(it.hexColor.toColorInt()) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true),
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 6.dp,
        ) {
            Column {
                // ── Header ──────────────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = headerColor?.copy(alpha = 0.25f)
                                ?: MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                        )
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                ) {
                    Column {
                        if (headerColor != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(headerColor)
                                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
                                )
                                Spacer(modifier = Modifier.size(6.dp))
                                Text(
                                    text = player.playerIdentity.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        } else {
                            Text(
                                text = player.playerIdentity.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        player.playerIdentity.username?.let { uname ->
                            Text(
                                text = "@$uname",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                // ── Score display + backspace ────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = input,
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontSize = 36.sp,
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = if (input.isEmpty())
                            MaterialTheme.colorScheme.onSurfaceVariant
                        else
                            MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(modifier = Modifier.size(12.dp))
                    FilledIconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            input = input.dropLast(1)
                        },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        ),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Backspace,
                            contentDescription = stringResource(R.string.score_input_delete_content_description),
                        )
                    }
                }

                // ── Numpad ──────────────────────────────────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val rows = listOf(
                        listOf("7", "8", "9"),
                        listOf("4", "5", "6"),
                        listOf("1", "2", "3"),
                        listOf("+/-", "0", "."),
                    )
                    rows.forEach { rowKeys ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            rowKeys.forEach { key ->
                                NumpadKey(
                                    label = key,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(56.dp),
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        input = handleNumpadKey(key, input)
                                    },
                                )
                            }
                        }
                    }
                }

                // ── Confirm button ───────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    FilledIconButton(
                        onClick = { onConfirm(input.toScoreOrNull()) },
                        modifier = Modifier.size(56.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = stringResource(R.string.score_input_confirm_content_description),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Processes a numpad key press and returns the updated input string.
 */
internal fun handleNumpadKey(key: String, current: String): String {
    return when (key) {
        "+/-" -> when {
            current.isEmpty() -> current
            current.startsWith("-") -> current.removePrefix("-")
            else -> "-$current"
        }
        "." -> if (current.contains(".")) current else "$current."
        else -> {
            // Prevent leading zeros (e.g. "00" → "0" is fine but "01" should become "1")
            when (current) {
                "0" -> key
                "-0" -> "-$key"
                else -> "$current$key"
            }
        }
    }
}

@Composable
private fun NumpadKey(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
        )
    }
}

// ── Previews ──────────────────────────────────────────────────────────────────

private class ScoreInputDialogPreviewProvider : PreviewParameterProvider<PlayerEntryUi> {
    override val values = sequenceOf(
        PlayerEntryUi(
            playerIdentity = PlayerIdentity(name = "Alice", username = "alicebgg", userId = null),
            startPosition = 1,
            color = PlayerColor.BLUE.colorString,
            score = 42.0,
            isWinner = false,
        ),
        PlayerEntryUi(
            playerIdentity = PlayerIdentity(name = "Bob", username = null, userId = null),
            startPosition = 2,
            color = null,
            score = null,
            isWinner = false,
        ),
    )
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ScoreInputDialogPreview(
    @PreviewParameter(ScoreInputDialogPreviewProvider::class) player: PlayerEntryUi,
) {
    MeepleBookTheme {
        ScoreInputDialog(
            player = player,
            onConfirm = {},
            onDismiss = {},
        )
    }
}
