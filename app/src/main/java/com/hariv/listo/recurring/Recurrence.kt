package com.hariv.listo.recurring

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

enum class RepeatUnit(val key: String) {
    WEEK("week"), MONTH("month"), YEAR("year");

    companion object {
        fun of(key: String): RepeatUnit = entries.firstOrNull { it.key == key } ?: MONTH
    }
}

/**
 * A repeat schedule.
 * weekday: 1 = Monday .. 7 = Sunday (weekly rules)
 * day: 1..31 (monthly and yearly rules; clamped to the month's last day)
 * month: 1..12 (yearly rules)
 * The first due date is the first matching date on or after [startDate].
 */
data class Schedule(
    val unit: RepeatUnit,
    val every: Int,
    val weekday: Int?,
    val day: Int?,
    val month: Int?,
    val startDate: LocalDate,
)

object Recurrence {

    private fun step(s: Schedule): Long = s.every.coerceIn(1, 999).toLong()

    fun clampDay(ym: YearMonth, day: Int): LocalDate = ym.atDay(day.coerceIn(1, ym.lengthOfMonth()))

    /** The k-th due date (k = 0 is the first one on or after the start date). */
    fun occurrence(s: Schedule, k: Long): LocalDate = when (s.unit) {
        RepeatUnit.WEEK -> {
            val weekday = DayOfWeek.of((s.weekday ?: s.startDate.dayOfWeek.value).coerceIn(1, 7))
            s.startDate.with(TemporalAdjusters.nextOrSame(weekday)).plusWeeks(k * step(s))
        }
        RepeatUnit.MONTH -> {
            val day = s.day ?: s.startDate.dayOfMonth
            var first = YearMonth.from(s.startDate)
            if (clampDay(first, day).isBefore(s.startDate)) first = first.plusMonths(1)
            clampDay(first.plusMonths(k * step(s)), day)
        }
        RepeatUnit.YEAR -> {
            val month = (s.month ?: s.startDate.monthValue).coerceIn(1, 12)
            val day = s.day ?: s.startDate.dayOfMonth
            var firstYear = s.startDate.year
            if (clampDay(YearMonth.of(firstYear, month), day).isBefore(s.startDate)) firstYear += 1
            clampDay(YearMonth.of(firstYear, month).plusYears(k * step(s)), day)
        }
    }

    fun occurrences(s: Schedule): Sequence<LocalDate> =
        generateSequence(0L) { it + 1 }.map { occurrence(s, it) }

    /** Latest due date after [after] (exclusive) and up to [upTo] (inclusive), or null. One catch-up item at most. */
    fun latestDue(s: Schedule, after: LocalDate?, upTo: LocalDate): LocalDate? {
        var result: LocalDate? = null
        for (date in occurrences(s)) {
            if (date.isAfter(upTo)) break
            if (after == null || date.isAfter(after)) result = date
        }
        return result
    }

    /** Next due date after [after] (exclusive), or the first one if [after] is null. */
    fun nextDue(s: Schedule, after: LocalDate?): LocalDate =
        occurrences(s).first { after == null || it.isAfter(after) }

    fun upcoming(s: Schedule, after: LocalDate?, count: Int): List<LocalDate> =
        occurrences(s).filter { after == null || it.isAfter(after) }.take(count).toList()

    fun describe(s: Schedule): String {
        val n = step(s)
        return when (s.unit) {
            RepeatUnit.WEEK -> {
                val name = DayOfWeek.of((s.weekday ?: 1).coerceIn(1, 7)).getDisplayName(TextStyle.FULL, Locale.ENGLISH)
                if (n == 1L) "Every week on $name" else "Every $n weeks on $name"
            }
            RepeatUnit.MONTH -> {
                val day = s.day ?: 1
                val last = if (day >= 29) " (or the month's last day)" else ""
                if (n == 1L) "Every month on the ${ordinal(day)}$last" else "Every $n months on the ${ordinal(day)}$last"
            }
            RepeatUnit.YEAR -> {
                val name = Month.of((s.month ?: 1).coerceIn(1, 12)).getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
                val day = s.day ?: 1
                if (n == 1L) "Every year on $day $name" else "Every $n years on $day $name"
            }
        }
    }

    fun ordinal(d: Int): String = d.toString() + when {
        d % 100 in 11..13 -> "th"
        d % 10 == 1 -> "st"
        d % 10 == 2 -> "nd"
        d % 10 == 3 -> "rd"
        else -> "th"
    }
}
