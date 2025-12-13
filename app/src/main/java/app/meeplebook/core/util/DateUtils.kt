package app.meeplebook.core.util

import java.time.Instant
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

// Usage example:
// val (start, end) = monthRangeFor("2025-09")
fun monthRangeFor(yearMonth: String): Pair<Instant, Instant> {
    // yearMonth must be "yyyy-MM"
    val ym = YearMonth.parse(yearMonth)
    val start = ym.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant()
    val end = ym.plusMonths(1).atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant()
    return start to end
}

fun parseBggDateToInstant(value: String?): Instant? {
    if (value.isNullOrBlank()) return null
    return try {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val ldt = LocalDateTime.parse(value, formatter)
        ldt.toInstant(ZoneOffset.UTC)
    } catch (_: Exception) {
        null
    }
}