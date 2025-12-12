package app.meeplebook.core.util

import android.content.Context
import app.meeplebook.R
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/**
 * Utility class for formatting dates and player lists using string resources.
 */
class DateFormatter @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /**
     * Formats a date string (YYYY-MM-DD) into a human-readable text.
     */
    fun formatDateText(dateString: String): String {
        val playDate = LocalDate.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE)
        val today = LocalDate.now()
        val daysDiff = ChronoUnit.DAYS.between(playDate, today)
        
        return when {
            daysDiff == 0L -> context.getString(R.string.date_today)
            daysDiff == 1L -> context.getString(R.string.date_yesterday)
            daysDiff < 7L -> context.getString(R.string.date_days_ago, daysDiff)
            else -> playDate.format(DateTimeFormatter.ofPattern("MMM d"))
        }
    }
    
    /**
     * Formats a list of player names into a comma-separated string.
     */
    fun formatPlayerNames(names: List<String>): String {
        return when {
            names.isEmpty() -> context.getString(R.string.players_none)
            names.size <= 3 -> names.joinToString(", ")
            else -> "${names.take(3).joinToString(", ")}, ${context.getString(R.string.players_more, names.size - 3)}"
        }
    }
}
