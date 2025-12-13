package app.meeplebook.core.util

import java.time.Instant
import java.time.YearMonth
import java.time.ZoneOffset

// Usage example:
// val (start, end) = monthRangeFor("2025-09")
fun monthRangeFor(yearMonth: String): Pair<Instant, Instant> {
    // yearMonth must be "yyyy-MM"
    val ym = YearMonth.parse(yearMonth)
    val start = ym.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant()
    val end = ym.plusMonths(1).atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant()
    return start to end
}