package com.f11.app

import com.f11.app.data.FocusSchedule
import com.f11.app.data.FocusSchedules
import java.time.DayOfWeek
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.SATURDAY
import java.time.DayOfWeek.SUNDAY
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FocusScheduleTest {

    // 2026-11-16 is a Monday.
    private fun at(day: Int, h: Int, m: Int = 0) = LocalDateTime.of(2026, 11, day, h, m)

    private val weekdays = setOf(MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, FRIDAY)
    private val work = FocusSchedule(1, "Work", 9 * 60, 18 * 60, weekdays)
    private val sleep = FocusSchedule(2, "Sleep", 23 * 60, 7 * 60, setOf(SUNDAY))

    @Test
    fun daytimeWindowRespectsDaysAndBounds() {
        assertTrue(work.isActive(at(16, 9)))
        assertTrue(work.isActive(at(16, 17, 59)))
        assertFalse(work.isActive(at(16, 18)))
        assertFalse(work.isActive(at(16, 8, 59)))
        assertFalse(work.isActive(at(21, 10))) // Saturday
    }

    @Test
    fun overnightWindowBelongsToTheDayItStarts() {
        assertTrue(sleep.overnight)
        assertTrue(sleep.isActive(at(15, 23, 30))) // Sunday night
        assertTrue(sleep.isActive(at(16, 6, 59)))  // early Monday, started Sunday
        assertFalse(sleep.isActive(at(16, 7)))
        assertFalse(sleep.isActive(at(16, 23, 30))) // Monday night not selected
        assertFalse(sleep.isActive(at(15, 6, 0)))   // Sunday morning: Saturday not selected
    }

    @Test
    fun endsAtRollsPastMidnight() {
        assertEquals(at(16, 18), work.endsAt(at(16, 10)))
        assertEquals(at(16, 7), sleep.endsAt(at(15, 23, 30)))
    }

    @Test
    fun disabledOrDaylessNeverRuns() {
        assertFalse(work.copy(enabled = false).isActive(at(16, 10)))
        assertFalse(work.copy(days = emptySet()).isActive(at(16, 10)))
    }

    @Test
    fun encodingRoundTripsAndSanitisesNames() {
        val list = listOf(work, sleep.copy(name = "Night|time;", enabled = false))
        val decoded = FocusSchedules.decode(FocusSchedules.encode(list))
        assertEquals(work, decoded[0])
        assertEquals("Night time", decoded[1].name)
        assertFalse(decoded[1].enabled)
        assertEquals(setOf(SUNDAY), decoded[1].days)
        assertTrue(FocusSchedules.decode("garbage;1|x|9999|0|1|1").isEmpty())
    }

    @Test
    fun activeAtPicksTheLongestRunningWindow() {
        val evening = FocusSchedule(3, "Evening", 17 * 60, 21 * 60, setOf(MONDAY, SATURDAY))
        assertEquals(evening, FocusSchedules.activeAt(listOf(work, evening), at(16, 17, 30)))
        assertNull(FocusSchedules.activeAt(listOf(work, evening), at(16, 22)))
    }
}
