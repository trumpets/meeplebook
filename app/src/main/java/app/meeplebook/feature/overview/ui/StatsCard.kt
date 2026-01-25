package app.meeplebook.feature.overview.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.meeplebook.R
import app.meeplebook.core.ui.UiText
import app.meeplebook.core.ui.isNotEmpty
import app.meeplebook.feature.overview.OverviewStats
import app.meeplebook.ui.components.UiTextText

@Composable
fun StatsCard(
    stats: OverviewStats,
    lastSyncedUiText: UiText,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("statsCard"),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.your_stats_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem(
                    value = stats.gamesCount.toString(),
                    label = stringResource(R.string.stat_games)
                )
                StatItem(
                    value = stats.totalPlays.toString(),
                    label = stringResource(R.string.stat_total_plays)
                )
                StatItem(
                    value = stats.playsThisMonth.toString(),
                    label = stringResource(R.string.stat_this_month)
                )
                StatItem(
                    value = stats.unplayedCount.toString(),
                    label = stringResource(R.string.stat_unplayed)
                )
            }
            if (lastSyncedUiText.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                UiTextText(
                    text = lastSyncedUiText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}