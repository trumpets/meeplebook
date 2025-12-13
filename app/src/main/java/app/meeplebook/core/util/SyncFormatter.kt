package app.meeplebook.core.util

import android.content.Context
import app.meeplebook.R
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject

private const val MINUTES_IN_HOUR = 60L
private const val MINUTES_IN_TWO_HOURS = 120L
private const val MINUTES_IN_DAY = 1440L

/**
 * Utility class for formatting sync times using string resources.
 */
class SyncFormatter @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /**
     * Formats a timestamp into a human-readable "time ago" string.
     */
    fun formatTimeAgo(time: LocalDateTime?): String {
        if (time == null) return context.getString(R.string.sync_never)
        
        val now = LocalDateTime.now()
        val minutesAgo = ChronoUnit.MINUTES.between(time, now)
        
        return when {
            minutesAgo < 1L -> context.getString(R.string.sync_just_now)
            minutesAgo < MINUTES_IN_HOUR -> context.getString(R.string.sync_minutes_ago, minutesAgo)
            minutesAgo < MINUTES_IN_TWO_HOURS -> context.getString(R.string.sync_one_hour_ago)
            minutesAgo < MINUTES_IN_DAY -> context.getString(R.string.sync_hours_ago, minutesAgo / MINUTES_IN_HOUR)
            else -> {
                val daysAgo = minutesAgo / MINUTES_IN_DAY
                if (daysAgo == 1L) {
                    context.getString(R.string.sync_one_day_ago)
                } else {
                    context.getString(R.string.sync_days_ago, daysAgo)
                }
            }
        }
    }
    
    /**
     * Formats sync time with "Last synced:" prefix.
     */
    fun formatLastSynced(time: LocalDateTime?): String {
        return if (time == null) {
            context.getString(R.string.sync_never)
        } else {
            context.getString(R.string.sync_last_synced, formatTimeAgo(time))
        }
    }
}
