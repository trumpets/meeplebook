package app.meeplebook.core.util

import app.meeplebook.R
import app.meeplebook.core.ui.UiText
import app.meeplebook.core.ui.uiText
import app.meeplebook.core.ui.uiTextRes
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Year
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private const val MINUTES_IN_HOUR = 60L
private const val MINUTES_IN_TWO_HOURS = MINUTES_IN_HOUR * 2
private const val MINUTES_IN_DAY = MINUTES_IN_HOUR * 24

data class Range(
    val start: Instant,
    val end: Instant
)

fun monthRangeFor(yearMonth: YearMonth, zoneId: ZoneId = ZoneOffset.UTC): Range {
    val start = yearMonth.atDay(1).atStartOfDay(zoneId).toInstant()
    val end = yearMonth.plusMonths(1).atDay(1).atStartOfDay(zoneId).toInstant()
    return Range(start, end)
}

fun yearRangeFor(year: Year, zoneId: ZoneId = ZoneOffset.UTC): Range {
    val start = year.atDay(1).atStartOfDay(zoneId).toInstant()
    val end = year.plusYears(1).atDay(1).atStartOfDay(zoneId).toInstant()
    return Range(start, end)
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
fun formatRelativeDate(dateInstant: Instant): UiText {
    val zone = ZoneId.systemDefault()
    val playDate = dateInstant.atZone(zone).toLocalDate()
    val today = LocalDate.now(zone)
    val daysDiff = ChronoUnit.DAYS.between(playDate, today)

    return when {
        daysDiff < 0L -> uiTextRes(R.string.date_in_future)
        daysDiff == 0L -> uiTextRes(R.string.date_today)
        daysDiff == 1L -> uiTextRes(R.string.date_yesterday)
        daysDiff < 7L -> uiTextRes(R.string.date_days_ago, daysDiff)
        else -> uiText(playDate.format(DateTimeFormatter.ofPattern("d MMM")))
    }
}

/**
 * Formats a timestamp into a human-readable "time ago" string.
 */
fun formatTimeAgo(time: Instant?): UiText {
    if (time == null) return uiTextRes(R.string.sync_never)

    val now = Instant.now()
    val minutesAgo = ChronoUnit.MINUTES.between(time, now)

    return when {
        minutesAgo < 1L -> uiTextRes(R.string.sync_just_now)
        minutesAgo < MINUTES_IN_HOUR -> uiTextRes(R.string.sync_minutes_ago, minutesAgo)
        minutesAgo < MINUTES_IN_TWO_HOURS -> uiTextRes(R.string.sync_one_hour_ago)
        minutesAgo < MINUTES_IN_DAY -> uiTextRes(R.string.sync_hours_ago, minutesAgo / MINUTES_IN_HOUR)
        else -> {
            val daysAgo = minutesAgo / MINUTES_IN_DAY
            if (daysAgo == 1L) {
                uiTextRes(R.string.sync_one_day_ago)
            } else {
                uiTextRes(R.string.sync_days_ago, daysAgo)
            }
        }
    }
}

/**
 * Formats sync time with "Last synced:" prefix.
 */
fun formatLastSynced(time: Instant?): UiText {
    return if (time == null) {
        uiTextRes(R.string.sync_never)
    } else {
        uiTextRes(R.string.sync_last_synced, formatTimeAgo(time))
    }
}

/**
 * Parses a date string in the format "yyyy-MM-dd" into an [Instant] at midnight UTC.
 *
 * @param dateString the date string to parse, expected in "yyyy-MM-dd" format (e.g., "2024-01-01")
 * @return the corresponding [Instant] at 00:00:00 UTC on the given date
 */
fun parseDateString(dateString: String): Instant {
    return Instant.parse("${dateString}T00:00:00Z")
}