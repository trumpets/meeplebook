package app.meeplebook.core.util

import android.content.Context
import app.meeplebook.R
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private const val MINUTES_IN_HOUR = 60L
private const val MINUTES_IN_TWO_HOURS = MINUTES_IN_HOUR * 2
private const val MINUTES_IN_DAY = MINUTES_IN_HOUR * 24

// Usage example:
// val (start, end) = monthRangeFor("2025-09")
fun monthRangeFor(yearMonth: String): Pair<Instant, Instant> {
    // yearMonth must be "yyyy-MM"
    val ym = YearMonth.parse(yearMonth)
    val start = ym.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant()
    val end = ym.plusMonths(1).atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant()
    return start to end
}

fun parseBggDateTime(value: String?): Instant? {
    if (value.isNullOrBlank()) return null
    return try {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val ldt = LocalDateTime.parse(value, formatter)
        ldt.toInstant(ZoneOffset.UTC)
    } catch (_: Exception) {
        null
    }
}

fun parseBggDate(value: String?): Instant? {
    if (value.isNullOrBlank()) return null
    return try {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val ldt = LocalDate.parse(value, formatter)
        ldt.atStartOfDay(ZoneOffset.UTC).toInstant()
    } catch (_: Exception) {
        null
    }
}

/**
 * Formats an Instant into a human-readable date text (relative).
 */
fun formatRelativeDate(context: Context, dateInstant: Instant): String {
    val zone = ZoneId.systemDefault()
    val playDate = dateInstant.atZone(zone).toLocalDate()
    val today = LocalDate.now(zone)
    val daysDiff = ChronoUnit.DAYS.between(playDate, today)

    return when {
        daysDiff == 0L -> context.getString(R.string.date_today)
        daysDiff == 1L -> context.getString(R.string.date_yesterday)
        daysDiff < 7L -> context.getString(R.string.date_days_ago, daysDiff)
        else -> playDate.format(DateTimeFormatter.ofPattern("d MMM"))
    }
}

/**
 * Formats a timestamp into a human-readable "time ago" string.
 */
fun formatTimeAgo(context: Context, time: Instant?): String {
    if (time == null) return context.getString(R.string.sync_never)

    val now = Instant.now()
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
fun formatLastSynced(context: Context, time: Instant?): String {
    return if (time == null) {
        context.getString(R.string.sync_never)
    } else {
        context.getString(R.string.sync_last_synced, formatTimeAgo(context, time))
    }
}