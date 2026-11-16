package com.f11.app

import com.f11.app.data.DayStat
import com.f11.app.data.FrictionLanguage
import com.f11.app.data.FrictionLines
import com.f11.app.data.HindiForm
import com.f11.app.data.History
import com.f11.app.service.SwipeDebouncer
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryAndLinesTest {

    @Test
    fun historyRoundTripsAndSkipsGarbage() {
        val days = listOf(DayStat(100, 12, 1), DayStat(101, 0, 0))
        assertEquals(days, History.decode(History.encode(days)))
        assertEquals(listOf(DayStat(5, 3, 0)), History.decode("junk;5:3:0;7:x:1;"))
        assertTrue(History.decode(null).isEmpty())
    }

    @Test
    fun upsertReplacesTodayAndCapsLength() {
        val updated = History.upsert(listOf(DayStat(10, 1, 0)), DayStat(10, 5, 2))
        assertEquals(listOf(DayStat(10, 5, 2)), updated)
        val many = (1L..200L).fold(emptyList<DayStat>()) { acc, d -> History.upsert(acc, DayStat(d, 1, 0)) }
        assertEquals(History.MAX_DAYS, many.size)
        assertEquals(200L, many.last().epochDay)
    }

    @Test
    fun windowZeroFillsGaps() {
        val w = History.window(listOf(DayStat(8, 4, 0)), today = 9, count = 3)
        assertEquals(listOf(DayStat(7, 0, 0), DayStat(8, 4, 0), DayStat(9, 0, 0)), w)
    }

    @Test
    fun focusStreakStopsAtShieldDayAndInstallDay() {
        val days = listOf(DayStat(5, 30, 2), DayStat(6, 10, 0), DayStat(8, 3, 0))
        assertEquals(3, History.focusStreak(days, today = 8, firstDay = 1)) // 8, 7 (no data), 6
        assertEquals(2, History.focusStreak(days, today = 8, firstDay = 7))
        assertEquals(0, History.focusStreak(days, today = 5, firstDay = 1))
    }

    @Test
    fun typedLineIgnoresCaseAndPunctuationOnly() {
        val line = "I am sacrificing discipline for dopamine."
        assertTrue(FrictionLines.typedMatches(line, "i am sacrificing   discipline for dopamine"))
        assertFalse(FrictionLines.typedMatches(line, "I am sacrificing discipline"))
        assertFalse(FrictionLines.typedMatches(line, ""))
    }

    @Test
    fun hindiLinesRenderChosenGenderAndMatchDanda() {
        val m = FrictionLines.all(FrictionLanguage.HINDI, HindiForm.MASCULINE)
        val f = FrictionLines.all(FrictionLanguage.HINDI, HindiForm.FEMININE)
        assertTrue(m.none { '{' in it })
        assertTrue(m[0].contains("रहा हूँ"))
        assertTrue(f[0].contains("रही हूँ"))
        assertTrue(FrictionLines.typedMatches(m[4], m[4].removeSuffix("।")))
    }

    @Test
    fun pickReturnsDistinctLines() {
        val picked = FrictionLines.pick(2, FrictionLanguage.ENGLISH, random = Random(7))
        assertEquals(2, picked.size)
        assertNotEquals(picked[0], picked[1])
    }

    @Test
    fun debouncerCountsGesturesNotEvents() {
        val d = SwipeDebouncer(debounceMs = 500)
        assertTrue(d.isNewSwipe(0))
        assertFalse(d.isNewSwipe(200))
        assertFalse(d.isNewSwipe(600))
        assertTrue(d.isNewSwipe(1_200))
    }
}
