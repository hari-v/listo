package com.hariv.simpletodo.recurring

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class RecurrenceTest {

    private fun d(s: String) = LocalDate.parse(s)

    private fun monthly(day: Int, start: String, every: Int = 1) =
        Schedule(RepeatUnit.MONTH, every, null, day, null, d(start))

    @Test fun monthlyOnTheFirst() {
        assertEquals(
            listOf(d("2026-11-01"), d("2026-12-01"), d("2027-01-01")),
            Recurrence.upcoming(monthly(1, "2026-10-25"), null, 3),
        )
    }

    @Test fun day31FallsOnLastDayOfShortMonths() {
        assertEquals(
            listOf(d("2026-01-31"), d("2026-02-28"), d("2026-03-31"), d("2026-04-30")),
            Recurrence.upcoming(monthly(31, "2026-01-15"), null, 4),
        )
    }

    @Test fun quarterlyFromChosenStart() {
        assertEquals(
            listOf(d("2027-01-01"), d("2027-04-01"), d("2027-07-01"), d("2027-10-01")),
            Recurrence.upcoming(monthly(1, "2027-01-01", every = 3), null, 4),
        )
    }

    @Test fun quarterlyStartsAtNextMatchingDay() {
        assertEquals(
            listOf(d("2026-11-01"), d("2027-02-01")),
            Recurrence.upcoming(monthly(1, "2026-10-25", every = 3), null, 2),
        )
    }

    @Test fun leapDayBirthday() {
        val s = Schedule(RepeatUnit.YEAR, 1, null, 29, 2, d("2026-01-01"))
        assertEquals(
            listOf(d("2026-02-28"), d("2027-02-28"), d("2028-02-29")),
            Recurrence.upcoming(s, null, 3),
        )
    }

    @Test fun yearlyAlreadyPassedThisYear() {
        val s = Schedule(RepeatUnit.YEAR, 1, null, 5, 3, d("2026-10-25"))
        assertEquals(d("2027-03-05"), Recurrence.nextDue(s, null))
    }

    @Test fun everyTwoWeeksOnMonday() {
        // 25 Oct 2026 is a Sunday.
        val s = Schedule(RepeatUnit.WEEK, 2, 1, null, null, d("2026-10-25"))
        assertEquals(listOf(d("2026-10-26"), d("2026-11-09")), Recurrence.upcoming(s, null, 2))
    }

    @Test fun showsOneDayEarly() {
        val s = monthly(1, "2026-10-25")
        assertEquals(d("2026-11-01"), Recurrence.latestDue(s, null, d("2026-10-31").plusDays(1)))
        assertNull(Recurrence.latestDue(s, null, d("2026-10-30").plusDays(1)))
    }

    @Test fun onlyOneCatchUpItem() {
        val s = monthly(1, "2026-01-01")
        assertEquals(d("2026-04-01"), Recurrence.latestDue(s, d("2026-01-01"), d("2026-04-02")))
    }

    @Test fun nothingNewAfterAlreadyCreated() {
        val s = monthly(1, "2026-01-01")
        assertNull(Recurrence.latestDue(s, d("2026-11-01"), d("2026-11-02")))
    }

    @Test fun nextDueAfterLast() {
        val s = monthly(15, "2026-01-01", every = 3)
        assertEquals(d("2026-04-15"), Recurrence.nextDue(s, d("2026-01-15")))
    }

    @Test fun describesSchedules() {
        assertEquals("Every 3 months on the 1st", Recurrence.describe(monthly(1, "2026-01-01", every = 3)))
        assertEquals("Every month on the 31st (or the month's last day)", Recurrence.describe(monthly(31, "2026-01-01")))
        assertEquals("Every year on 29 Feb", Recurrence.describe(Schedule(RepeatUnit.YEAR, 1, null, 29, 2, d("2026-01-01"))))
        assertEquals("Every 2 weeks on Monday", Recurrence.describe(Schedule(RepeatUnit.WEEK, 2, 1, null, null, d("2026-01-01"))))
        assertEquals("11th", Recurrence.ordinal(11))
        assertEquals("22nd", Recurrence.ordinal(22))
    }
}
