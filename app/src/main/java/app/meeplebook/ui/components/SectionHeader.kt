package app.meeplebook.ui.components

import android.content.res.Configuration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import app.meeplebook.ui.theme.MeepleBookTheme

/**
 * Reusable section header for lists or grouped content.
 *
 * @param title The text to display
 */
@Composable
fun SectionHeader(
    title: String
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
    )
}

@Preview(showBackground = true)
@Preview(
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun SectionHeaderPreview() {
    MeepleBookTheme {
        SectionHeader(title = "Recent Activity")
    }
}