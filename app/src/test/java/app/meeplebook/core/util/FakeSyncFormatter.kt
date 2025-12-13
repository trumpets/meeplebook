package app.meeplebook.core.util

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/**
 * Fake SyncFormatter for testing that doesn't require Android Context.
 */
class FakeSyncFormatter {
    fun formatLastSynced(lastSyncTime: LocalDateTime?): String {
        if (lastSyncTime == null) {
            return "Never synced"
        }
        
        val now = LocalDateTime.now()
        val minutesAgo = ChronoUnit.MINUTES.between(lastSyncTime, now)
        
        return when {
            minutesAgo < 1L -> "Last synced: just now"
            minutesAgo < 60L -> "Last synced: $minutesAgo min ago"
            minutesAgo < 120L -> "Last synced: 1 hour ago"
            minutesAgo < 1440L -> "Last synced: ${minutesAgo / 60L} hours ago"
            else -> {
                val daysAgo = minutesAgo / 1440L
                "Last synced: $daysAgo day${if (daysAgo > 1L) "s" else ""} ago"
            }
        }
    }
}
