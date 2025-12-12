package app.meeplebook.core.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * Fake DateFormatter for testing that doesn't require Android Context.
 */
class FakeDateFormatter {
    fun formatDateText(dateString: String): String {
        val playDate = LocalDate.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE)
        val today = LocalDate.now()
        val daysDiff = ChronoUnit.DAYS.between(playDate, today)
        
        return when {
            daysDiff == 0L -> "Today"
            daysDiff == 1L -> "Yesterday"
            daysDiff < 7L -> "$daysDiff days ago"
            else -> playDate.format(DateTimeFormatter.ofPattern("MMM d"))
        }
    }
    
    fun formatPlayerNames(names: List<String>): String {
        return when {
            names.isEmpty() -> "No players"
            names.size <= 3 -> names.joinToString(", ")
            else -> "${names.take(3).joinToString(", ")}, +${names.size - 3}"
        }
    }
}
